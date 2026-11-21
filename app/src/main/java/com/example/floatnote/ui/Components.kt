package com.example.floatnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shape
import com.example.floatnote.data.TimerItem
import com.example.floatnote.data.TimerStatus
import com.example.floatnote.system.TimerEngine
import com.example.floatnote.ui.theme.LocalIsDark
import com.example.floatnote.ui.theme.Pastel
import kotlinx.coroutines.delay

/**
 * Jam tampilan. HANYA untuk menggambar ulang layar: nilai sebenarnya selalu dihitung dari
 * endAt (tersimpan di disk). Bila waktu habis saat layar terlihat, memicu penyelesaian idempoten.
 */
@Composable
fun rememberNow(timer: TimerItem?): Long {
    val appCtx = LocalContext.current.applicationContext
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val id = timer?.id
    val status = timer?.status
    val endAt = timer?.endAt ?: 0L
    LaunchedEffect(id, status, endAt) {
        now = System.currentTimeMillis()
        if (id != null && status == TimerStatus.RUNNING) {
            while (true) {
                if (endAt <= now) {
                    TimerEngine.complete(appCtx, id)
                    break
                }
                delay(200)
                now = System.currentTimeMillis()
            }
        }
    }
    return now
}

@Composable
fun TimerProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = color,
        trackColor = color.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Tombol Mulai / Jeda / Lanjutkan / Reset sesuai status. Dipakai di halaman utama dan kartu melayang. */
@Composable
fun TimerControls(
    timer: TimerItem,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pad = if (compact) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
    val minH = if (compact) 36.dp else 40.dp
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (timer.status) {
            TimerStatus.IDLE -> Button(onClick = onStart, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Mulai") }
            TimerStatus.RUNNING -> {
                Button(onClick = onPause, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Jeda") }
                OutlinedButton(onClick = onReset, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Reset") }
            }
            TimerStatus.PAUSED -> {
                Button(onClick = onStart, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Lanjutkan") }
                OutlinedButton(onClick = onReset, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Reset") }
            }
            TimerStatus.FINISHED -> {
                Button(onClick = onStart, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Ulangi") }
                OutlinedButton(onClick = onReset, contentPadding = pad, modifier = Modifier.height(minH)) { Text("Reset") }
            }
        }
    }
}

@Composable
fun ColorDots(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(Pastel.count) { i ->
            val ring = if (i == selected) Pastel.accent(dark) else Color.Transparent
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Pastel.card(i, dark))
                    .border(2.dp, ring, CircleShape)
                    .clickable { onSelect(i) },
            )
        }
    }
}

fun statusLabel(s: TimerStatus): String = when (s) {
    TimerStatus.IDLE -> "Siap"
    TimerStatus.RUNNING -> "Berjalan"
    TimerStatus.PAUSED -> "Dijeda"
    TimerStatus.FINISHED -> "Selesai"
}
