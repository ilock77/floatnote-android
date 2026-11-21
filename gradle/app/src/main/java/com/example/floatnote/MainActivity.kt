package com.example.floatnote

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.floatnote.data.Repository
import com.example.floatnote.overlay.OverlayService
import com.example.floatnote.system.PermissionStatus
import com.example.floatnote.system.Permissions
import com.example.floatnote.system.Scheduler
import com.example.floatnote.ui.AppRoot
import com.example.floatnote.ui.PermissionActions
import com.example.floatnote.ui.theme.FloatNoteTheme

class MainActivity : ComponentActivity() {

    private var perms by mutableStateOf(PermissionStatus(false, false, false, false))

    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

    private val actions by lazy {
        PermissionActions(
            requestNotifications = ::askNotifications,
            openOverlay = { Permissions.open(this, Permissions.overlaySettingsIntent(this)) },
            openExactAlarm = { Permissions.open(this, Permissions.exactAlarmSettingsIntent(this)) },
            openBattery = { Permissions.open(this, Permissions.batterySettingsIntent()) },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissions()
        setContent {
            val data by Repository.state.collectAsState()
            FloatNoteTheme(data.settings.themeMode) { AppRoot(perms, actions) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pengguna baru kembali dari layar Pengaturan: baca ulang status izin.
        refreshPermissions()
        // Alarm bisa hilang (Force Stop, restart, izin berubah) dan service bisa dimatikan sistem.
        Scheduler.syncAll(this)
        if (Repository.state.value.floatingCount > 0) {
            if (Permissions.canDrawOverlays(this)) OverlayService.start(this)
            else Repository.disableAllFloating() // izin dicabut: jangan biarkan status "aktif" palsu
        }
    }

    override fun onStop() {
        Repository.flush()
        super.onStop()
    }

    private fun refreshPermissions() {
        perms = Permissions.snapshot(this)
    }

    private fun askNotifications() {
        val asked = Repository.state.value.settings.notificationAsked
        if (Permissions.needsRuntimeNotificationPermission() && !asked) {
            Repository.updateSettings { it.copy(notificationAsked = true) }
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Sudah pernah ditanya (atau Android < 13 dengan notifikasi dimatikan): buka pengaturan notifikasi.
            Permissions.open(this, Permissions.notificationSettingsIntent(this))
        }
    }
}
