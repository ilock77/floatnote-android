package com.example.floatnote.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.floatnote.data.Repository
import com.example.floatnote.data.TimerStatus

/**
 * Membungkus AlarmManager. Alarm adalah jaring pengaman utama: tetap berbunyi walau
 * aplikasi ditutup, proses dimatikan sistem, atau HP masuk Doze.
 */
object Scheduler {
    const val ACTION_TIMER = "com.example.floatnote.action.TIMER_DONE"
    const val ACTION_REMINDER = "com.example.floatnote.action.REMINDER"
    const val EXTRA_ID = "id"

    private fun pending(ctx: Context, action: String, kind: String, id: String): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("floatnote://$kind/$id"))   // data unik = PendingIntent unik per item
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun set(ctx: Context, atMs: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (Permissions.canScheduleExactAlarms(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            } else {
                // Tanpa izin alarm tepat: sistem boleh menunda (bisa beberapa menit saat Doze).
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            }
        } catch (e: SecurityException) {
            // Izin dicabut tepat di antara pengecekan dan pemasangan.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun scheduleTimer(ctx: Context, id: String, endAt: Long) =
        set(ctx, endAt, pending(ctx, ACTION_TIMER, "timer", id))

    fun cancelTimer(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pending(ctx, ACTION_TIMER, "timer", id))
    }

    fun scheduleReminder(ctx: Context, id: String, atMs: Long) =
        set(ctx, atMs, pending(ctx, ACTION_REMINDER, "reminder", id))

    fun cancelReminder(ctx: Context, id: String) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pending(ctx, ACTION_REMINDER, "reminder", id))
    }

    /**
     * Sinkronkan alarm dengan data tersimpan. Dipanggil saat aplikasi dibuka, HP restart,
     * aplikasi diperbarui, atau izin alarm berubah (alarm hilang pada Force Stop dan restart).
     */
    fun syncAll(ctx: Context) {
        val now = System.currentTimeMillis()
        val data = Repository.state.value

        data.timers.forEach { t ->
            if (t.status == TimerStatus.RUNNING) {
                if (t.endAt <= now) TimerEngine.complete(ctx, t.id) else scheduleTimer(ctx, t.id, t.endAt)
            }
        }
        data.notes.forEach { n ->
            val at = n.reminderAt
            if (at != null && !n.reminderFired) {
                if (at <= now) ReminderEngine.fire(ctx, n.id) else scheduleReminder(ctx, n.id, at)
            }
        }
    }
}
