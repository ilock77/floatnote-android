package com.example.floatnote.overlay

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatnote.MainActivity
import com.example.floatnote.data.AppSettings
import com.example.floatnote.data.Note
import com.example.floatnote.data.Repository
import com.example.floatnote.data.TimerItem
import com.example.floatnote.data.TimerStatus
import com.example.floatnote.system.TimerEngine
import com.example.floatnote.ui.TimerControls
import com.example.floatnote.ui.TimerProgress
import com.example.floatnote.ui.rememberNow
import com.example.floatnote.ui.theme.FloatNoteTheme
import com.example.floatnote.ui.theme.LocalIsDark
import com.example.floatnote.ui.theme.Pastel
import com.example.floatnote.util.Format

/** Isi satu jendela melayang. Membaca Repository sendiri, jadi selalu mengikuti data terbaru. */
@Composable
fun OverlayContent(id: String) {
    val data by Repository.state.collectAsState()
    FloatNoteTheme(data.settings.themeMode) {
        val note = data.notes.firstOrNull { it.id == id }
        val timer = data.timers.firstOrNull { it.id == id }
        when {
            note != null -> NoteOverlay(note, data.settings)
            timer != null -> TimerOverlay(timer, data.settings)
        }
    }
}

@Composable
private fun NoteOverlay(note: Note, settings: AppSettings) {
    val dark = LocalIsDark.current
    val ctx = LocalContext.current
    val bg = Pastel.card(note.colorIndex, dark).copy(alpha = settings.overlayAlpha)
    val text = Pastel.onCard(dark)
    val soft = Pastel.onCardSoft(dark)

    if (note.overlay.collapsed) {
        Bubble(bg, onClick = { setCollapsed(note.id, false) }) {
            Text(
                note.title.ifBlank { note.body }.trim().take(1).uppercase().ifEmpty { "N" },
                color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    Card(bg) {
        HeaderRow(
            title = note.title.ifBlank { "Catatan" },
            textColor = text,
            onCollapse = { setCollapsed(note.id, true) },
            onClose = { close(note.id) },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            note.body.ifBlank { "(kosong)" },
            color = if (note.body.isBlank()) soft else text,
            fontSize = 14.sp, lineHeight = 20.sp,
            maxLines = 8, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Buka untuk mengedit",
            color = Pastel.accent(dark), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    ctx.startActivity(
                        Intent(ctx, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun TimerOverlay(timer: TimerItem, settings: AppSettings) {
    val dark = LocalIsDark.current
    val ctx = LocalContext.current.applicationContext
    val now = rememberNow(timer)
    val bg = Pastel.card(timer.colorIndex, dark).copy(alpha = settings.overlayAlpha)
    val text = Pastel.onCard(dark)
    val accent = Pastel.accent(dark)
    val remaining = timer.remainingAt(now)

    if (timer.overlay.collapsed) {
        Bubble(bg, onClick = { setCollapsed(timer.id, false) }, size = 68) {
            Text(
                Format.clock(remaining),
                color = if (timer.status == TimerStatus.FINISHED) accent else text,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    Card(bg) {
        HeaderRow(
            title = timer.title.ifBlank { "Timer" },
            textColor = text,
            onCollapse = { setCollapsed(timer.id, true) },
            onClose = { close(timer.id) },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (timer.status == TimerStatus.FINISHED) "Selesai!" else Format.clock(remaining),
            color = if (timer.status == TimerStatus.FINISHED) accent else text,
            fontSize = 34.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        TimerProgress(timer.progressAt(now), accent)
        Spacer(Modifier.height(10.dp))
        TimerControls(
            timer = timer,
            compact = true,
            onStart = { TimerEngine.start(ctx, timer.id) },
            onPause = { TimerEngine.pause(ctx, timer.id) },
            onReset = { TimerEngine.reset(ctx, timer.id) },
        )
    }
}

@Composable
private fun Card(bg: Color, content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(6.dp) // ruang untuk bayangan
            .width(264.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) { content() }
}

@Composable
private fun Bubble(bg: Color, onClick: () -> Unit, size: Int = 56, content: @Composable () -> Unit) {
    Box(
        Modifier
            .padding(6.dp)
            .size(size.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun HeaderRow(title: String, textColor: Color, onCollapse: () -> Unit, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        HeaderButton("–", textColor, onCollapse)  // ciutkan jadi gelembung
        Spacer(Modifier.width(2.dp))
        HeaderButton("✕", textColor, onClose)     // sembunyikan kartu
    }
}

@Composable
private fun HeaderButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

private fun setCollapsed(id: String, collapsed: Boolean) =
    Repository.updateOverlay(id) { it.copy(collapsed = collapsed) }

private fun close(id: String) =
    Repository.updateOverlay(id) { it.copy(enabled = false) }
