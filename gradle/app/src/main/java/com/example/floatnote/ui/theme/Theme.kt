package com.example.floatnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.floatnote.data.ThemeMode

val LocalIsDark = staticCompositionLocalOf { false }

/** Warna pastel untuk kartu catatan/timer. Indeks disimpan di data, warna dihitung sesuai tema. */
object Pastel {
    private val light = listOf(
        Color(0xFFFFE1DC), // persik
        Color(0xFFD9F2E3), // mint
        Color(0xFFDAE7FF), // langit
        Color(0xFFFFF0C4), // mentega
        Color(0xFFE9DCFF), // lavender
        Color(0xFFFFDCEB), // permen
    )
    private val dark = listOf(
        Color(0xFF4B3837),
        Color(0xFF2D4639),
        Color(0xFF2E3D5A),
        Color(0xFF504628),
        Color(0xFF41385A),
        Color(0xFF543849),
    )
    val count: Int get() = light.size

    fun card(index: Int, isDark: Boolean): Color {
        val list = if (isDark) dark else light
        return list[((index % list.size) + list.size) % list.size]
    }

    fun onCard(isDark: Boolean): Color = if (isDark) Color(0xFFF3F0FB) else Color(0xFF1F1D2B)
    fun onCardSoft(isDark: Boolean): Color = if (isDark) Color(0xFFC9C4DA) else Color(0xFF5B5870)
    fun accent(isDark: Boolean): Color = if (isDark) Color(0xFFB9ADFF) else Color(0xFF6C5CE7)
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF2A1F7A),
    secondary = Color(0xFF3FA37A),
    secondaryContainer = Color(0xFFD9F2E3),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF1F1D2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1D2B),
    surfaceVariant = Color(0xFFEDEBF5),
    onSurfaceVariant = Color(0xFF5B5870),
    outline = Color(0xFFC9C5DA),
    error = Color(0xFFD64550),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB9ADFF),
    onPrimary = Color(0xFF2A1F7A),
    primaryContainer = Color(0xFF3F3590),
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = Color(0xFF8FD9B8),
    secondaryContainer = Color(0xFF2D4639),
    background = Color(0xFF121116),
    onBackground = Color(0xFFF3F0FB),
    surface = Color(0xFF1C1B22),
    onSurface = Color(0xFFF3F0FB),
    surfaceVariant = Color(0xFF2A2833),
    onSurfaceVariant = Color(0xFFC9C4DA),
    outline = Color(0xFF4A4758),
    error = Color(0xFFFF8A93),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FloatNoteTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            shapes = AppShapes,
            content = content,
        )
    }
}
