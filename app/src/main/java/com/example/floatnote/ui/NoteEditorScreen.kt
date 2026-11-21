package com.example.floatnote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.floatnote.data.Repository
import com.example.floatnote.system.PermissionStatus
import com.example.floatnote.system.ReminderEngine
import com.example.floatnote.system.Scheduler
import com.example.floatnote.ui.theme.LocalIsDark
import com.example.floatnote.ui.theme.Pastel
import com.example.floatnote.util.Format
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String,
    perms: PermissionStatus,
    onBack: () -> Unit,
    onAskPermission: (then: (() -> Unit)?) -> Unit,
    onToggleFloat: (String, Boolean) -> Unit,
) {
    val ctx = LocalContext.current.applicationContext
    val data by Repository.state.collectAsState()
    val note = data.notes.firstOrNull { it.id == noteId }
    if (note == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val dark = LocalIsDark.current
    var title by remember(noteId) { mutableStateOf(note.title) }
    var body by remember(noteId) { mutableStateOf(note.body) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun leave() {
        val n = Repository.note(noteId)
        // Catatan kosong tanpa pengingat tidak disimpan, supaya daftar tidak penuh sampah.
        if (n != null && n.title.isBlank() && n.body.isBlank() && n.reminderAt == null) {
            Repository.deleteNote(noteId)
        } else {
            Repository.flush()
        }
        onBack()
    }
    BackHandler(onBack = ::leave)

    fun startPicking() { error = null; pickDate = true }

    val bg = Pastel.card(note.colorIndex, dark)
    val text = Pastel.onCard(dark)
    val soft = Pastel.onCardSoft(dark)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = text, unfocusedTextColor = text, cursorColor = Pastel.accent(dark),
        focusedPlaceholderColor = soft, unfocusedPlaceholderColor = soft,
    )

    Scaffold(containerColor = bg) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::leave) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = text)
                }
                Text("Tersimpan otomatis", fontSize = 12.sp, color = soft)
            }
            TextField(
                value = title,
                onValueChange = {
                    title = it.take(80)
                    Repository.updateNote(noteId, debounced = true) { n -> n.copy(title = title, updatedAt = System.currentTimeMillis()) }
                },
                placeholder = { Text("Judul", fontSize = 22.sp) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = fieldColors, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            TextField(
                value = body,
                onValueChange = {
                    body = it
                    Repository.updateNote(noteId, debounced = true) { n -> n.copy(body = body, updatedAt = System.currentTimeMillis()) }
                },
                placeholder = { Text("Tulis catatanmu di sini…") },
                colors = fieldColors, modifier = Modifier.fillMaxWidth().height(220.dp),
            )

            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Warna", fontSize = 13.sp, color = soft)
                Spacer(Modifier.height(6.dp))
                ColorDots(note.colorIndex, onSelect = { i -> Repository.updateNote(noteId) { it.copy(colorIndex = i) } })

                Spacer(Modifier.height(20.dp))
                Text("Pengingat", fontSize = 13.sp, color = soft)
                Spacer(Modifier.height(4.dp))
                Text(
                    note.reminderAt?.let { Format.reminder(it) + if (note.reminderFired) " (sudah berbunyi)" else "" }
                        ?: "Belum diatur",
                    color = text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = {
                        if (!perms.notifications || !perms.exactAlarm) onAskPermission { startPicking() } else startPicking()
                    }) { Text(if (note.reminderAt == null) "Atur tanggal & jam" else "Ubah") }
                    if (note.reminderAt != null) {
                        TextButton(onClick = { ReminderEngine.set(ctx, noteId, null) }) { Text("Hapus") }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tampilkan sebagai kartu melayang", color = text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = note.overlay.enabled, onCheckedChange = { onToggleFloat(noteId, it) })
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (pickDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initial = note.reminderAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } ?: todayUtc
        val state = rememberDatePickerState(
            initialSelectedDateMillis = maxOf(initial, todayUtc),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        pickDate = false
                        pickTime = true
                    }
                }) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Batal") } },
        ) { DatePicker(state = state) }
    }

    if (pickTime) {
        val base = note.reminderAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
            ?: LocalTime.now().plusHours(1)
        val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Pilih jam") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedDate
                    if (date != null) {
                        val at = LocalDateTime.of(date, LocalTime.of(state.hour, state.minute))
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        if (at <= System.currentTimeMillis() + 5_000L) {
                            error = "Pilih waktu di masa depan."
                        } else {
                            error = null
                            ReminderEngine.set(ctx, noteId, at)
                        }
                    }
                    pickTime = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Batal") } },
        )
    }
}
