package com.example.floatnote.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.floatnote.data.Repository
import com.example.floatnote.overlay.OverlayService
import com.example.floatnote.system.PermissionStatus
import com.example.floatnote.system.Permissions
import com.example.floatnote.ui.theme.LocalIsDark

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Editor(val noteId: String) : Screen
}

@Composable
fun AppRoot(perms: PermissionStatus, actions: PermissionActions) {
    val data by Repository.state.collectAsState()
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showPermissions by remember { mutableStateOf(false) }
    var afterPermissions by remember { mutableStateOf<(() -> Unit)?>(null) }

    StatusBarSync()

    fun askPermissions(then: (() -> Unit)? = null) {
        afterPermissions = then
        showPermissions = true
    }

    val toggleFloat: (String, Boolean) -> Unit = { id, on -> setFloating(ctx, id, on) { askPermissions() } }

    BackHandler(enabled = screen is Screen.Settings) { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            data = data,
            perms = perms,
            onOpenNote = { screen = Screen.Editor(it) },
            onOpenSettings = { screen = Screen.Settings },
            onAskPermission = { askPermissions() },
            onToggleFloat = toggleFloat,
        )
        Screen.Settings -> SettingsScreen(data, perms, actions, onBack = { screen = Screen.Home })
        is Screen.Editor -> NoteEditorScreen(
            noteId = s.noteId,
            perms = perms,
            onBack = { screen = Screen.Home },
            onAskPermission = { then -> askPermissions(then) },
            onToggleFloat = toggleFloat,
        )
    }

    if (!data.settings.onboardingDone) {
        OnboardingDialog(perms, actions, onDone = { Repository.updateSettings { it.copy(onboardingDone = true) } })
    } else if (showPermissions) {
        PermissionDialog(perms, actions, onClose = {
            showPermissions = false
            val next = afterPermissions
            afterPermissions = null
            next?.invoke()
        })
    }
}

/** Menyalakan/mematikan kartu melayang. Tanpa izin overlay, tampilkan penjelasan izin lebih dulu. */
private fun setFloating(ctx: Context, id: String, on: Boolean, needPermission: () -> Unit) {
    if (!on) {
        Repository.updateOverlay(id) { it.copy(enabled = false) }
        return
    }
    if (!Permissions.canDrawOverlays(ctx)) {
        needPermission()
        return
    }
    // Simpan status DULU, baru jalankan service: service membaca status dari Repository.
    Repository.updateOverlay(id) { it.copy(enabled = true, collapsed = false) }
    OverlayService.start(ctx)
}

/** Ikon status bar mengikuti tema aplikasi (bukan hanya tema sistem). */
@Composable
private fun StatusBarSync() {
    val view = LocalView.current
    val dark = LocalIsDark.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
}
