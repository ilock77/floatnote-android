package com.example.floatnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatnote.data.AppData
import com.example.floatnote.data.Note
import com.example.floatnote.data.Repository
import com.example.floatnote.data.TimerItem
import com.example.floatnote.data.TimerStatus
import com.example.floatnote.system.PermissionStatus
import com.example.floatnote.system.Scheduler
import com.example.floatnote.system.TimerEngine
import com.example.floatnote.ui.theme.LocalIsDark
import com.example.floatnote.ui.theme.Pastel
import com.example.floatnote.util.Format
import java.util.UUID

@Composable
fun HomeScreen(
    data: AppData,
    perms: PermissionStatus,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAskPermission: () -> Unit,
    onToggleFloat: (String, Boolean) -> Unit,
) {
    val ctx = LocalContext.current.applicationContext
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var timerDialogOpen by remember { mutableStateOf(false) }
    var editingTimerId by remember { mutableStateOf<String?>(null) }
    var deleteNoteId by remember { mutableStateOf<String?>(null) }
    var deleteTimerId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (tab == 0) {
                        val n = Note(
                            id = UUID.randomUUID().toString(),
                            colorIndex = (data.notes.size + data.timers.size) % Pastel.count,
                            updatedAt = System.currentTimeMillis(),
                        )
                        Repository.addNote(n)
                        onOpenNote(n.id)
                    } else {
                        editingTimerId = null
                        timerDialogOpen = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (tab == 0) "Catatan baru" else "Timer baru") },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("FloatNote", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (data.floatingCount > 0) "${data.floatingCount} kartu sedang melayang" else "Belum ada kartu melayang",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }
                }
            }
            if (!perms.essentialOk) {
                item { PermissionBanner(perms, onAskPermission) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Catatan (${data.notes.size})") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Timer (${data.timers.size})") })
                }
            }

            if (tab == 0) {
                if (data.notes.isEmpty()) item { EmptyHint("Belum ada catatan. Ketuk “Catatan baru” untuk mulai.") }
                items(data.notes, key = { it.id }) { n ->
                    NoteCard(n, onClick = { onOpenNote(n.id) }, onToggleFloat = { onToggleFloat(n.id, it) }, onDelete = { deleteNoteId = n.id })
                }
            } else {
                if (data.timers.isEmpty()) item { EmptyHint("Belum ada timer. Ketuk “Timer baru” untuk membuat hitung mundur.") }
                items(data.timers, key = { it.id }) { t ->
                    TimerCard(
                        t, onToggleFloat = { onToggleFloat(t.id, it) },
                        onEdit = { editingTimerId = t.id; timerDialogOpen = true },
                        onDelete = { deleteTimerId = t.id },
                    )
                }
            }
        }
    }

    if (timerDialogOpen) {
        val editing = editingTimerId?.let { Repository.timer(it) }
        TimerDialog(
            initialTitle = editing?.title ?: "",
            initialDurationMs = editing?.durationMs ?: 300_000L,
            isEdit = editing != null,
            onDismiss = { timerDialogOpen = false },
            onSave = { title, ms ->
                if (editing == null) {
                    Repository.addTimer(
                        TimerItem(
                            id = UUID.randomUUID().toString(), title = title, durationMs = ms, remainingMs = ms,
                            colorIndex = (data.notes.size + data.timers.size) % Pastel.count,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                } else {
                    Repository.updateTimer(editing.id) { it.copy(title = title) }
                    if (ms != editing.durationMs) TimerEngine.setDuration(ctx, editing.id, ms)
                }
                timerDialogOpen = false
            },
        )
    }

    deleteNoteId?.let { id ->
        ConfirmDelete("Hapus catatan ini?", onDismiss = { deleteNoteId = null }) {
            Scheduler.cancelReminder(ctx, id)
            Repository.deleteNote(id)
            deleteNoteId = null
        }
    }
    deleteTimerId?.let { id ->
        ConfirmDelete("Hapus timer ini?", onDismiss = { deleteTimerId = null }) {
            TimerEngine.delete(ctx, id)
            deleteTimerId = null
        }
    }
}

@Composable
private fun PermissionBanner(perms: PermissionStatus, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Izin belum lengkap", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${perms.missingText()}. Ketuk untuk mengatur.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@Composable
private fun NoteCard(n: Note, onClick: () -> Unit, onToggleFloat: (Boolean) -> Unit, onDelete: () -> Unit) {
    val dark = LocalIsDark.current
    val text = Pastel.onCard(dark)
    val soft = Pastel.onCardSoft(dark)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Pastel.card(n.colorIndex, dark), contentColor = text),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                n.title.ifBlank { "Tanpa judul" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (n.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(n.body, fontSize = 14.sp, lineHeight = 20.sp, color = soft, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (n.reminderAt != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Pastel.accent(dark), modifier = Modifier.height(16.dp))
                    Text(
                        " " + Format.reminder(n.reminderAt) + if (n.reminderFired) " (sudah berbunyi)" else "",
                        fontSize = 12.sp, color = soft,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FooterRow(n.overlay.enabled, onToggleFloat, onDelete, extra = null)
        }
    }
}

@Composable
private fun TimerCard(t: TimerItem, onToggleFloat: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dark = LocalIsDark.current
    val ctx = LocalContext.current.applicationContext
    val text = Pastel.onCard(dark)
    val soft = Pastel.onCardSoft(dark)
    val accent = Pastel.accent(dark)
    val now = rememberNow(t)
    val remaining = t.remainingAt(now)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Pastel.card(t.colorIndex, dark), contentColor = text),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.title.ifBlank { "Timer" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(statusLabel(t.status), fontSize = 12.sp, color = soft)
            }
            Text(
                Format.clock(remaining), fontSize = 44.sp, fontWeight = FontWeight.Bold,
                color = if (t.status == TimerStatus.FINISHED) accent else text,
            )
            Text("Durasi ${Format.duration(t.durationMs)}", fontSize = 12.sp, color = soft)
            Spacer(Modifier.height(10.dp))
            TimerProgress(t.progressAt(now), accent)
            Spacer(Modifier.height(12.dp))
            TimerControls(
                timer = t,
                onStart = { TimerEngine.start(ctx, t.id) },
                onPause = { TimerEngine.pause(ctx, t.id) },
                onReset = { TimerEngine.reset(ctx, t.id) },
            )
            Spacer(Modifier.height(4.dp))
            FooterRow(
                t.overlay.enabled, onToggleFloat, onDelete,
                extra = if (t.status != TimerStatus.RUNNING) ({ TextButton(onClick = onEdit) { Text("Ubah") } }) else null,
            )
        }
    }
}

@Composable
private fun FooterRow(floating: Boolean, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, extra: (@Composable () -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = floating, onCheckedChange = onToggle)
        Text("  Melayang", fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        extra?.invoke()
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
    }
}

@Composable
fun ConfirmDelete(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(message) },
        text = { Text("Tindakan ini tidak bisa dibatalkan.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Hapus", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
