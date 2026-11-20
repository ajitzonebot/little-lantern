package org.littlelantern.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple stroke icons drawn in code, so the app ships no icon set. Coordinates use a 24-unit box. */
enum class Glyph { BACK, HOME, SPEAKER, PAUSE, PLAY, LANTERN, PLUS }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            Glyph.BACK -> drawPath(Path().apply { moveTo(15 * s, 5 * s); lineTo(8 * s, 12 * s); lineTo(15 * s, 19 * s) }, color, style = stroke)
            Glyph.HOME -> {
                drawPath(Path().apply { moveTo(3 * s, 11 * s); lineTo(12 * s, 4 * s); lineTo(21 * s, 11 * s) }, color, style = stroke)
                drawPath(Path().apply { moveTo(5 * s, 10 * s); lineTo(5 * s, 20 * s); lineTo(19 * s, 20 * s); lineTo(19 * s, 10 * s) }, color, style = stroke)
            }
            Glyph.SPEAKER -> {
                drawPath(Path().apply { moveTo(4 * s, 9 * s); lineTo(8 * s, 9 * s); lineTo(13 * s, 5 * s); lineTo(13 * s, 19 * s); lineTo(8 * s, 15 * s); lineTo(4 * s, 15 * s); close() }, color, style = stroke)
                drawPath(Path().apply { moveTo(16 * s, 9 * s); quadraticBezierTo(18.5f * s, 12 * s, 16 * s, 15 * s) }, color, style = stroke)
            }
            Glyph.PAUSE -> {
                drawRect(color, topLeft = p(6f, 5f), size = androidx.compose.ui.geometry.Size(4 * s, 14 * s))
                drawRect(color, topLeft = p(14f, 5f), size = androidx.compose.ui.geometry.Size(4 * s, 14 * s))
            }
            Glyph.PLAY -> drawPath(Path().apply { moveTo(8 * s, 5 * s); lineTo(19 * s, 12 * s); lineTo(8 * s, 19 * s); close() }, color)
            Glyph.LANTERN -> {
                drawPath(Path().apply { moveTo(9 * s, 3 * s); lineTo(15 * s, 3 * s); moveTo(12 * s, 3 * s); lineTo(12 * s, 5 * s) }, color, style = stroke)
                drawPath(Path().apply { moveTo(7 * s, 8 * s); lineTo(17 * s, 8 * s); lineTo(16 * s, 19 * s); lineTo(8 * s, 19 * s); close() }, color, style = stroke)
            }
            Glyph.PLUS -> {
                drawLine(color, p(12f, 5f), p(12f, 19f), strokeWidth = 2f * s, cap = StrokeCap.Round)
                drawLine(color, p(5f, 12f), p(19f, 12f), strokeWidth = 2f * s, cap = StrokeCap.Round)
            }
        }
    }
}

/** Round icon button, 56 dp: big enough for small fingers. */
@Composable
fun RoundButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (dark) Color(0xFF4A4A44) else Palette.Surface)
            .border(1.dp, if (dark) Color.Transparent else Palette.Border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, if (dark) Palette.Bg else Palette.Sage, 26.dp)
    }
}
