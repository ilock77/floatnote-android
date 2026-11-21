package com.example.floatnote.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val idLocale: Locale = Locale.forLanguageTag("id-ID")
    private val reminderFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'pukul' HH.mm", idLocale)

    /** 04:59 atau 1:02:03. Dibulatkan ke atas supaya "00:00" hanya tampil saat benar-benar selesai. */
    fun clock(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** "5 menit", "1 jam 30 menit", "45 detik". */
    fun duration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        val parts = buildList {
            if (h > 0) add("$h jam")
            if (m > 0) add("$m menit")
            if (s > 0) add("$s detik")
        }
        return if (parts.isEmpty()) "0 detik" else parts.joinToString(" ")
    }

    fun reminder(epochMs: Long): String =
        reminderFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}
