package com.example.floatnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MAX_MS = 99L * 3600_000L

@Composable
fun TimerDialog(
    initialTitle: String,
    initialDurationMs: Long,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, durationMs: Long) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    val total0 = initialDurationMs / 1000
    var h by remember { mutableStateOf((total0 / 3600).toString()) }
    var m by remember { mutableStateOf(((total0 % 3600) / 60).toString()) }
    var s by remember { mutableStateOf((total0 % 60).toString()) }

    fun digits(v: String) = v.filter { it.isDigit() }.take(2)
    val durationMs = ((h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (s.toLongOrNull() ?: 0)) * 1000
    val valid = durationMs in 1000..MAX_MS

    fun preset(minutes: Int) { h = "0"; m = minutes.toString(); s = "0" }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (isEdit) "Ubah timer" else "Timer baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(60) },
                    label = { Text("Judul") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Jam", h, Modifier.weight(1f)) { h = digits(it) }
                    NumberField("Menit", m, Modifier.weight(1f)) { m = digits(it) }
                    NumberField("Detik", s, Modifier.weight(1f)) { s = digits(it) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10, 25).forEach { min ->
                        AssistChip(onClick = { preset(min) }, label = { Text("$min m") })
                    }
                }
                if (!valid) {
                    Spacer(Modifier.height(6.dp))
                    Text("Durasi minimal 1 detik.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(title.trim(), durationMs) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier,
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
    )
}
