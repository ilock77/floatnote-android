package com.example.floatnote.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Pembungkus yang menggeser jendela overlay. Ketukan biasa diteruskan ke Compose (tombol, switch);
 * hanya saat jari bergeser melewati ambang, sentuhan "diambil alih" untuk menyeret kartu.
 * Memakai koordinat layar (rawX/rawY) supaya tidak melompat saat jendela ikut bergerak.
 */
class DragLayout(context: Context) : FrameLayout(context) {
    var onDragStart: (() -> Unit)? = null
    var onDragTo: ((dx: Int, dy: Int) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    val isDragging: Boolean get() = dragging

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && (abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop)) {
                    dragging = true
                    onDragStart?.invoke()
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Sentuh di area kosong kartu: ambil alih supaya MOVE berikutnya sampai ke sini.
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && (abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop)) {
                    dragging = true
                    onDragStart?.invoke()
                }
                if (dragging) onDragTo?.invoke((ev.rawX - downX).toInt(), (ev.rawY - downY).toInt())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) onDragEnd?.invoke()
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }
}
