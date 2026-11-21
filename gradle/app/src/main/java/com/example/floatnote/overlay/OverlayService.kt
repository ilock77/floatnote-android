package com.example.floatnote.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.floatnote.data.AppData
import com.example.floatnote.data.OverlayState
import com.example.floatnote.data.Repository
import com.example.floatnote.system.Notifications
import com.example.floatnote.system.Permissions
import com.example.floatnote.system.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service yang memegang semua jendela melayang. Statusnya mengikuti Repository:
 * item dengan overlay.enabled = true punya jendela, yang lain tidak. Service berhenti sendiri
 * saat tidak ada lagi kartu yang aktif.
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_STOP_ALL = "com.example.floatnote.action.STOP_ALL"
        private const val TAG = "OverlayService"

        /** Panggil SETELAH menyimpan overlay.enabled = true di Repository. */
        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val windows = LinkedHashMap<String, OverlayWindow>()
    private var collectJob: Job? = null
    private lateinit var wm: WindowManager
    private var lastNotifiedCount = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Notifications.createChannels(this)
        wm = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALL) {
            Repository.disableAllFloating()
            shutdown()
            return START_NOT_STICKY
        }
        if (!enterForeground(Repository.state.value.floatingCount)) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Service bisa dihidupkan ulang sistem setelah dimatikan: pastikan alarm tetap terpasang.
        Scheduler.syncAll(this)
        if (collectJob == null) {
            collectJob = scope.launch { Repository.state.collect { reconcile(it) } }
        }
        return START_STICKY
    }

    private fun enterForeground(count: Int): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        ServiceCompat.startForeground(this, Notifications.SERVICE_NOTIFICATION_ID, Notifications.serviceNotification(this, count), type)
        true
    } catch (e: Exception) {
        // Contoh: Android 12+ menolak start dari latar belakang. Aman: kartu muncul saat aplikasi dibuka.
        Log.w(TAG, "Gagal masuk mode foreground", e)
        false
    }

    private fun reconcile(data: AppData) {
        if (data.floatingCount == 0) {
            shutdown()
            return
        }
        if (!Permissions.canDrawOverlays(this)) {
            // Izin dicabut saat service jalan. Matikan flag; perubahan state memicu reconcile -> shutdown.
            Repository.disableAllFloating()
            return
        }

        val wanted = LinkedHashMap<String, OverlayState>()
        data.notes.forEach { if (it.overlay.enabled) wanted[it.id] = it.overlay }
        data.timers.forEach { if (it.overlay.enabled) wanted[it.id] = it.overlay }

        windows.keys.filter { it !in wanted }.toList().forEach { windows.remove(it)?.dispose() }

        wanted.forEach { (id, state) ->
            if (id !in windows) {
                val w = OverlayWindow(this, wm, id, state, windows.size)
                try {
                    w.show()
                    windows[id] = w
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal menampilkan kartu $id", e)
                    w.dispose()
                    Repository.updateOverlay(id) { it.copy(enabled = false) }
                }
            }
        }

        if (windows.size != lastNotifiedCount) {
            lastNotifiedCount = windows.size
            Notifications.updateServiceNotification(this, windows.size)
        }
    }

    private fun shutdown() {
        windows.values.forEach { it.dispose() }
        windows.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        windows.values.forEach { it.dispose() }
        windows.clear()
        scope.cancel()
        Repository.flush()   // pastikan posisi kartu terakhir tersimpan
        super.onDestroy()
    }
}
