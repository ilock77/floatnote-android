package com.example.floatnote.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.floatnote.data.OverlayState
import com.example.floatnote.data.Repository
import kotlin.math.max

/** Satu jendela melayang (satu catatan atau satu timer) yang bisa digeser dan diciutkan. */
class OverlayWindow(
    private val ctx: Context,
    private val wm: WindowManager,
    private val id: String,
    saved: OverlayState,
    slotIndex: Int,
) {
    private val density = ctx.resources.displayMetrics.density
    private val owner = OverlayLifecycleOwner()
    private val root = DragLayout(ctx)
    private val composeView = ComposeView(ctx)
    private var attached = false

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // NOT_FOCUSABLE: tidak mencuri keyboard/tombol back dari aplikasi di belakangnya.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = if (saved.x >= 0) saved.x else (16 * density).toInt()
        y = if (saved.y >= 0) saved.y else ((120 + 64 * slotIndex) * density).toInt()
    }

    private var startX = 0
    private var startY = 0

    fun show() {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent { OverlayContent(id) }
        root.addView(composeView)

        root.onDragStart = { startX = params.x; startY = params.y }
        root.onDragTo = { dx, dy ->
            params.x = startX + dx
            params.y = startY + dy
            clampAndApply()
        }
        root.onDragEnd = {
            Repository.updateOverlay(id, debounced = true) { it.copy(x = params.x, y = params.y) }
        }
        // Ukuran berubah (ciut/lebar) atau layar diputar: pastikan kartu tetap terlihat penuh.
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> clampAndApply() }

        owner.attachTo(root)
        owner.start()
        wm.addView(root, params)   // bisa melempar bila izin overlay dicabut; ditangani OverlayService
        attached = true
    }

    private fun clampAndApply() {
        if (!attached) return
        val dm = ctx.resources.displayMetrics
        val maxX = max(0, dm.widthPixels - root.width)
        val maxY = max(0, dm.heightPixels - root.height)
        val nx = params.x.coerceIn(0, maxX)
        val ny = params.y.coerceIn(0, maxY)
        val changed = nx != params.x || ny != params.y
        params.x = nx
        params.y = ny
        // Selalu update saat menyeret; saat layout, hanya bila posisi harus dikoreksi.
        if (changed || root.isDragging) {
            try {
                wm.updateViewLayout(root, params)
            } catch (e: IllegalArgumentException) {
                // jendela sudah dilepas
            }
        }
    }

    fun dispose() {
        attached = false
        composeView.disposeComposition()
        try {
            wm.removeView(root)
        } catch (e: IllegalArgumentException) {
            // sudah tidak terpasang
        }
        owner.stop()
    }
}
