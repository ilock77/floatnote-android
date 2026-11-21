package com.example.floatnote.system

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class PermissionStatus(
    val overlay: Boolean,
    val notifications: Boolean,
    val exactAlarm: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** Tiga izin inti. Hemat baterai hanya opsional. */
    val essentialOk: Boolean get() = overlay && notifications && exactAlarm

    fun missingText(): String {
        val list = buildList {
            if (!overlay) add("kartu melayang")
            if (!notifications) add("notifikasi")
            if (!exactAlarm) add("alarm tepat waktu")
        }
        return list.joinToString(", ").replaceFirstChar { it.uppercase() }
    }
}

object Permissions {

    fun canDrawOverlays(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    /** Android 13+ (API 33) meminta izin notifikasi lewat dialog sistem. */
    fun needsRuntimeNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** False bila izin ditolak ATAU notifikasi aplikasi dimatikan di Pengaturan. */
    fun hasNotifications(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** Android 12+ (API 31) mewajibkan izin khusus untuk alarm presisi; di bawahnya otomatis boleh. */
    fun canScheduleExactAlarms(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun isBatteryUnrestricted(ctx: Context): Boolean {
        val pm = ctx.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun snapshot(ctx: Context) = PermissionStatus(
        overlay = canDrawOverlays(ctx),
        notifications = hasNotifications(ctx),
        exactAlarm = canScheduleExactAlarms(ctx),
        batteryUnrestricted = isBatteryUnrestricted(ctx),
    )

    // ---- intent layar pengaturan ----

    private fun pkgUri(ctx: Context): Uri = Uri.parse("package:${ctx.packageName}")

    fun appDetailsIntent(ctx: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx))

    fun overlaySettingsIntent(ctx: Context) = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri(ctx))

    fun exactAlarmSettingsIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri(ctx))
        } else {
            appDetailsIntent(ctx)
        }

    fun notificationSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)

    /** Daftar aplikasi hemat baterai (tidak butuh izin khusus di manifest). */
    fun batterySettingsIntent() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Buka layar pengaturan; bila tidak ada di HP ini, jatuh ke layar info aplikasi. */
    fun open(ctx: Context, intent: Intent) {
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                ctx.startActivity(appDetailsIntent(ctx))
            } catch (e2: ActivityNotFoundException) {
                // HP tanpa layar pengaturan yang dibutuhkan; tidak ada yang bisa dilakukan.
            }
        }
    }
}
