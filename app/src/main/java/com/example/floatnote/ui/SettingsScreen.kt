package com.example.floatnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatnote.data.AppData
import com.example.floatnote.data.Repository
import com.example.floatnote.data.ThemeMode
import com.example.floatnote.system.PermissionStatus
import com.example.floatnote.ui.theme.LocalIsDark
import com.example.floatnote.ui.theme.Pastel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(data: AppData, perms: PermissionStatus, actions: PermissionActions, onBack: () -> Unit) {
    val settings = data.settings
    var alpha by remember { mutableFloatStateOf(settings.overlayAlpha) }
    val dark = LocalIsDark.current

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                Text("Pengaturan", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Section("Tema")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "Ikuti sistem", ThemeMode.LIGHT to "Terang", ThemeMode.DARK to "Gelap").forEach { (m, label) ->
                    FilterChip(
                        selected = settings.themeMode == m,
                        onClick = { Repository.updateSettings { it.copy(themeMode = m) } },
                        label = { Text(label) },
                    )
                }
            }

            Section("Transparansi kartu melayang: ${(alpha * 100).roundToInt()}%")
            Slider(
                value = alpha,
                onValueChange = {
                    alpha = it
                    Repository.updateSettings(debounced = true) { s -> s.copy(overlayAlpha = it) }
                },
                valueRange = 0.3f..1f,
            )
            // Pratinjau di atas latar bergaris supaya efek transparansinya terlihat.
            Box(
                Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7C6CF0), Color(0xFFFFB5A7), Color(0xFF8FD9B8)))),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth(0.7f).height(44.dp).clip(RoundedCornerShape(16.dp))
                        .background(Pastel.card(2, dark).copy(alpha = alpha)),
                    contentAlignment = Alignment.Center,
                ) { Text("Contoh kartu", color = Pastel.onCard(dark), fontWeight = FontWeight.Medium) }
            }

            Section("Izin")
            PermissionPanel(perms, actions)

            Section("Kartu melayang")
            Text(
                "Menyembunyikan kartu tidak menghapus catatan atau timer.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                enabled = data.floatingCount > 0,
                onClick = { Repository.disableAllFloating() },
            ) { Text("Sembunyikan semua kartu (${data.floatingCount})") }

            Spacer(Modifier.height(12.dp))
            Text(
                "Semua data tersimpan hanya di HP ini. Tanpa akun, iklan, atau sinkronisasi cloud.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}
