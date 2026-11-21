package com.example.floatnote.system

import android.content.Context
import com.example.floatnote.data.Repository
import com.example.floatnote.data.TimerItem
import com.example.floatnote.data.TimerStatus

/**
 * Logika timer. Sumber kebenaran adalah [TimerItem.endAt] (jam dinding) yang disimpan ke disk,
 * bukan penghitung di layar. Tampilan hanya menghitung "endAt - sekarang" tiap detik.
 */
object TimerEngine {
    private const val TOLERANCE_MS = 1_500L

    fun start(ctx: Context, id: String) {
        val now = System.currentTimeMillis()
        val t = Repository.modifyTimer(id) { cur ->
            when (cur.status) {
                TimerStatus.IDLE, TimerStatus.FINISHED ->
                    if (cur.durationMs <= 0L) null
                    else cur.copy(status = TimerStatus.RUNNING, endAt = now + cur.durationMs, remainingMs = cur.durationMs)
                TimerStatus.PAUSED ->
                    cur.copy(status = TimerStatus.RUNNING, endAt = now + cur.remainingMs.coerceAtLeast(1L))
                TimerStatus.RUNNING -> null
            }
        } ?: return
        Notifications.cancelTimerDone(ctx, id)
        Scheduler.scheduleTimer(ctx, id, t.endAt)
    }

    fun pause(ctx: Context, id: String) {
        val now = System.currentTimeMillis()
        val t = Repository.modifyTimer(id) { cur ->
            if (cur.status != TimerStatus.RUNNING) null
            else cur.copy(status = TimerStatus.PAUSED, remainingMs = (cur.endAt - now).coerceAtLeast(0L))
        } ?: return
        Scheduler.cancelTimer(ctx, t.id)
    }

    fun reset(ctx: Context, id: String) {
        val t = Repository.modifyTimer(id) { cur ->
            cur.copy(status = TimerStatus.IDLE, endAt = 0L, remainingMs = cur.durationMs)
        } ?: return
        Scheduler.cancelTimer(ctx, t.id)
        Notifications.cancelTimerDone(ctx, t.id)
    }

    /** Ubah durasi; hanya boleh saat tidak berjalan. Otomatis mereset. */
    fun setDuration(ctx: Context, id: String, durationMs: Long) {
        val t = Repository.modifyTimer(id) { cur ->
            if (cur.status == TimerStatus.RUNNING) null
            else cur.copy(durationMs = durationMs, status = TimerStatus.IDLE, endAt = 0L, remainingMs = durationMs)
        } ?: return
        Scheduler.cancelTimer(ctx, t.id)
    }

    fun delete(ctx: Context, id: String) {
        Scheduler.cancelTimer(ctx, id)
        Notifications.cancelTimerDone(ctx, id)
        Repository.deleteTimer(id)
    }

    /**
     * Tandai selesai. Aman dipanggil berkali-kali dan dari mana saja (alarm, tampilan, sinkronisasi):
     * transisi RUNNING -> FINISHED hanya terjadi sekali, sehingga notifikasi tidak dobel.
     */
    fun complete(ctx: Context, id: String): TimerItem? {
        val now = System.currentTimeMillis()
        val done = Repository.modifyTimer(id) { cur ->
            if (cur.status == TimerStatus.RUNNING && cur.endAt - now <= TOLERANCE_MS)
                cur.copy(status = TimerStatus.FINISHED, remainingMs = 0L)
            else null
        } ?: return null
        Scheduler.cancelTimer(ctx, id)
        Notifications.timerDone(ctx, done)
        return done
    }
}

object ReminderEngine {
    fun set(ctx: Context, noteId: String, atMs: Long?) {
        Scheduler.cancelReminder(ctx, noteId)
        Repository.updateNote(noteId) { it.copy(reminderAt = atMs, reminderFired = false) }
        if (atMs != null) Scheduler.scheduleReminder(ctx, noteId, atMs)
    }

    fun fire(ctx: Context, noteId: String) {
        val n = Repository.note(noteId) ?: return
        if (n.reminderAt == null || n.reminderFired) return
        Repository.updateNote(noteId) { it.copy(reminderFired = true) }
        Scheduler.cancelReminder(ctx, noteId)
        Notifications.reminder(ctx, n)
    }
}
