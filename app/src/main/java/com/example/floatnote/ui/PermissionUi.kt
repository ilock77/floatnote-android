package com.example.floatnote.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatnote.system.PermissionStatus

/** Tindakan pembuka layar izin. Diisi oleh MainActivity (butuh Activity untuk dialog sistem). */
class PermissionActions(
    val requestNotifications: () -> Unit,
    val openOverlay: () -> Unit,
    val openExactAlarm: () -> Unit,
    val openBattery: () -> Unit,
)

@Composable
fun PermissionPanel(perms: PermissionStatus, actions: PermissionActions, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionRow(
            title = "Tampil di atas aplikasi lain",
            why = "Supaya catatan dan timer bisa melayang di atas aplikasi lain. " +
                "Kamu akan dibawa ke Pengaturan. Pilih FloatNote, lalu aktifkan “Izinkan tampil di atas aplikasi lain”.",
            granted = perms.overlay,
            buttonLabel = "Buka pengaturan",
            onClick = actions.openOverlay,
        )
        PermissionRow(
            title = "Notifikasi",
            why = "Untuk membunyikan timer yang selesai dan pengingat catatan. " +
                "Juga menampilkan penanda kecil selama kartu melayang aktif.",
            granted = perms.notifications,
            buttonLabel = "Izinkan",
            onClick = actions.requestNotifications,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionRow(
                title = "Alarm tepat waktu",
                why = "Supaya timer dan pengingat berbunyi persis waktunya, walau layar mati atau aplikasi ditutup. " +
                    "Tanpa izin ini, bunyi bisa terlambat beberapa menit.",
                granted = perms.exactAlarm,
                buttonLabel = "Buka pengaturan",
                onClick = actions.openExactAlarm,
            )
        }
        PermissionRow(
            title = "Hemat baterai (opsional)",
            why = "Beberapa HP (Xiaomi, Oppo, Vivo, Samsung, dll.) menghentikan aplikasi di latar belakang. " +
                "Pilih “Tanpa batasan” untuk FloatNote agar kartu melayang tidak hilang sendiri.",
            granted = perms.batteryUnrestricted,
            buttonLabel = "Buka pengaturan",
            onClick = actions.openBattery,
        )
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, buttonLabel: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                if (granted) "✓ Aktif" else "Belum aktif",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(why, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!granted) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onClick) { Text(buttonLabel) }
        }
    }
}

@Composable
fun PermissionDialog(perms: PermissionStatus, actions: PermissionActions, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Izin yang dibutuhkan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PermissionPanel(perms, actions)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(if (perms.essentialOk) "Selesai" else "Nanti saja") } },
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun OnboardingDialog(perms: PermissionStatus, actions: PermissionActions, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Selamat datang di FloatNote") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Catatan dan timer kamu bisa tampil sebagai kartu kecil di atas aplikasi lain. " +
                        "Agar berfungsi, FloatNote butuh beberapa izin. Semua data hanya tersimpan di HP ini.",
                    fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                PermissionPanel(perms, actions)
            }
        },
        confirmButton = { Button(onClick = onDone) { Text("Mulai") } },
        shape = RoundedCornerShape(28.dp),
    )
}
