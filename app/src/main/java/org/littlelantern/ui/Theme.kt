package org.littlelantern.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Muted, low-saturation palette by design (see spec: calm visuals by default).
private val CalmColors = lightColorScheme(
    primary = Color(0xFF5B6B5D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8A7F6B),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF2F2F2B),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF2F2F2B),
    outline = Color(0xFFB9B3A6),
    error = Color(0xFF9A4A3F),
)

@Composable
fun LittleLanternTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CalmColors, content = content)
}
