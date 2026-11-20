package org.littlelantern.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Palette from the colour research (see docs/SPEC.md):
 * light, low-saturation screens; stronger colour only on small game pieces;
 * piece colours from the Okabe-Ito colour-blind-safe set, softened 15%, always with an outline.
 */
object Palette {
    val Bg = Color(0xFFF4F1EA)
    val Surface = Color(0xFFFBF9F4)
    val Ink = Color(0xFF2F2F2B)
    val InkSoft = Color(0xFF5F5A50)
    val Sage = Color(0xFF5B6B5D)
    val Sand = Color(0xFF8A7F6B)
    val Line = Color(0xFFDCD6C9)
    val Border = Color(0xFFB9B3A6)
    val EndScreen = Color(0xFF57524A)
    val Rest = Color(0xFFD9D3C7)

    // Game pieces.
    val Blue = Color(0xFF2585BA)
    val Orange = Color(0xFFDA7423)
    val Sky = Color(0xFF6EBDE9)
    val Pink = Color(0xFFD28BB1)
    val Outline = InkSoft
    val Skin = Color(0xFFF3DDB3)
    val Water = Color(0xFFBFDDEB)
    val Grass = Color(0xFF9FC3A8)
    val SkyBg = Color(0xFFCFE4F2)
    val StartDot = Color(0xFF3F7A55)

    val pieces = listOf(Blue, Orange, Sky, Pink)
}

private val CalmColors = lightColorScheme(
    primary = Palette.Sage,
    onPrimary = Color(0xFFFFFFFF),
    secondary = Palette.Sand,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    outline = Palette.Border,
    error = Color(0xFF9A4A3F),
)

@Composable
fun LittleLanternTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CalmColors, content = content)
}
