package com.example.floatnote.system

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.floatnote.MainActivity
import com.example.floatnote.R
import com.example.floatnote.data.Note
import com.example.floatnote.data.TimerItem
import com.example.floatnote.overlay.OverlayService
import com.example.floatnote.util.Format

object Notifications {
    const val CHANNEL_SERVICE = "overlay_service"
    const val CHANNEL_TIMER = "timer_done"
    const val CHANNEL_REMINDER = "reminders"

    const val SERVICE_NOTIFICATION_ID = 1001

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, "Kartu melayang aktif", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notifikasi tetap yang menjaga kartu melayang tetap hidup."
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Timer selesai", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Berbunyi saat hitung mundur selesai."
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Pengingat catatan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat sesuai tanggal dan jam yang kamu pilih."
                enableVibration(true)
            },
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Notifikasi wajib untuk foreground service. */
    fun serviceNotification(ctx: Context, floatingCount: Int): Notification {
        val stop = PendingIntent.getService(
            ctx, 1,
            Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_STOP_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(ctx, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_floatnote)
            .setContentTitle("FloatNote aktif")
            .setContentText("$floatingCount kartu melayang ditampilkan")
            .setContentIntent(openAppIntent(ctx))
            .addAction(0, "Tutup semua", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun updateServiceNotification(ctx: Context, floatingCount: Int) {
        post(ctx, SERVICE_NOTIFICATION_ID, serviceNotification(ctx, floatingCount))
    }

    fun timerDone(ctx: Context, t: TimerItem) {
        val title = t.title.ifBlank { "Timer" }
        val n = NotificationCompat.Builder(ctx, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_floatnote)
            .setContentTitle("$title selesai")
            .setContentText("Hitung mundur ${Format.duration(t.durationMs)} sudah habis.")
            .setContentIntent(openAppIntent(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        post(ctx, notificationId(t.id, 1), n)
    }

    fun reminder(ctx: Context, note: Note) {
        val title = note.title.ifBlank { "Pengingat" }
        val n = NotificationCompat.Builder(ctx, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_floatnote)
            .setContentTitle(title)
            .setContentText(note.body.ifBlank { "Waktunya melihat catatanmu." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.body.ifBlank { "Waktunya melihat catatanmu." }))
            .setContentIntent(openAppIntent(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        post(ctx, notificationId(note.id, 2), n)
    }

    fun cancelTimerDone(ctx: Context, timerId: String) {
        NotificationManagerCompat.from(ctx).cancel(notificationId(timerId, 1))
    }

    private fun notificationId(id: String, kind: Int): Int = (id.hashCode() * 31 + kind) and 0x7FFFFFFF

    private fun post(ctx: Context, id: Int, n: Notification) {
        // Android 13+: tanpa izin, notify() diabaikan diam-diam. Cek dulu agar tidak lint-error.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
