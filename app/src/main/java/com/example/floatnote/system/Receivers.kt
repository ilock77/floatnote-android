package com.example.floatnote.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.floatnote.data.Repository
import com.example.floatnote.overlay.OverlayService

/** Dipanggil AlarmManager saat timer habis atau pengingat jatuh tempo. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Repository.init(app)
        val id = intent.getStringExtra(Scheduler.EXTRA_ID) ?: return
        when (intent.action) {
            Scheduler.ACTION_TIMER -> TimerEngine.complete(app, id)
            Scheduler.ACTION_REMINDER -> ReminderEngine.fire(app, id)
        }
    }
}

/** HP restart, aplikasi diperbarui, atau izin alarm tepat berubah: pasang ulang semua alarm. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Repository.init(app)
        Scheduler.syncAll(app)

        val restoresFloating = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (restoresFloating && Repository.state.value.floatingCount > 0 && Permissions.canDrawOverlays(app)) {
            try {
                OverlayService.start(app)
            } catch (e: Exception) {
                // Android 12+ bisa menolak start service latar depan dari latar belakang. Bukan fatal:
                // kartu akan muncul lagi saat aplikasi dibuka.
                Log.w("SystemEvents", "Tidak bisa memulihkan kartu melayang", e)
            }
        }
    }
}
