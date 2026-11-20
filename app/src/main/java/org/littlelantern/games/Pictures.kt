package org.littlelantern.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.littlelantern.ui.Palette

enum class Mood { HAPPY, SAD, CROSS, SCARED }

/** A simple drawn face. */
@Composable
fun FaceView(mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val line = w * 0.05f
        drawCircle(Palette.Skin, radius = w * 0.44f)
        drawCircle(Palette.Outline, radius = w * 0.44f, style = Stroke(width = w * 0.04f))
        drawCircle(Palette.Ink, radius = w * 0.05f, center = Offset(w * 0.35f, w * 0.43f))
        drawCircle(Palette.Ink, radius = w * 0.05f, center = Offset(w * 0.65f, w * 0.43f))
        when (mood) {
            Mood.HAPPY -> drawPath(
                Path().apply { moveTo(w * 0.3f, w * 0.6f); quadraticBezierTo(w * 0.5f, w * 0.8f, w * 0.7f, w * 0.6f) },
                Palette.Ink, style = Stroke(width = line, cap = StrokeCap.Round),
            )
            Mood.SAD -> {
                drawPath(
                    Path().apply { moveTo(w * 0.3f, w * 0.72f); quadraticBezierTo(w * 0.5f, w * 0.56f, w * 0.7f, w * 0.72f) },
                    Palette.Ink, style = Stroke(width = line, cap = StrokeCap.Round),
                )
                drawCircle(Palette.Sky, radius = w * 0.035f, center = Offset(w * 0.33f, w * 0.53f))
            }
            Mood.CROSS -> {
                drawLine(Palette.Ink, Offset(w * 0.25f, w * 0.3f), Offset(w * 0.42f, w * 0.36f), strokeWidth = line, cap = StrokeCap.Round)
                drawLine(Palette.Ink, Offset(w * 0.75f, w * 0.3f), Offset(w * 0.58f, w * 0.36f), strokeWidth = line, cap = StrokeCap.Round)
                drawLine(Palette.Ink, Offset(w * 0.34f, w * 0.7f), Offset(w * 0.66f, w * 0.7f), strokeWidth = line, cap = StrokeCap.Round)
            }
            Mood.SCARED -> {
                drawCircle(Palette.Ink, radius = w * 0.08f, center = Offset(w * 0.5f, w * 0.68f), style = Stroke(width = line))
            }
        }
    }
}

/** A balloon in one colour. */
@Composable
fun Balloon(color: Color, size: Dp) {
    Canvas(Modifier.size(size * 0.7f, size)) {
        val w = this.size.width
        val h = this.size.height
        drawOval(color, topLeft = Offset(w * 0.1f, 0f), size = Size(w * 0.8f, h * 0.68f))
        drawOval(Palette.Outline, topLeft = Offset(w * 0.1f, 0f), size = Size(w * 0.8f, h * 0.68f), style = Stroke(width = w * 0.05f))
        drawPath(
            Path().apply { moveTo(w * 0.5f, h * 0.68f); quadraticBezierTo(w * 0.35f, h * 0.84f, w * 0.5f, h) },
            Palette.Outline, style = Stroke(width = w * 0.03f),
        )
    }
}

/** A ten-frame (2 x 5) with [count] dots. */
@Composable
fun TenFrame(count: Int) {
    Column(Modifier.border(3.dp, Palette.Outline, RoundedCornerShape(6.dp))) {
        (0 until 2).forEach { row ->
            Row {
                (0 until 5).forEach { col ->
                    val i = row * 5 + col
                    Box(
                        Modifier.size(48.dp).border(1.dp, Palette.Border).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (i < count) {
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(Palette.Blue).border(2.dp, Palette.Outline, RoundedCornerShape(50)))
                        }
                    }
                }
            }
        }
    }
}

/** A sorting bin: shows what belongs in it and how many are inside. */
@Composable
fun SortBin(count: Int, label: @Composable () -> Unit) {
    Column(
        Modifier
            .size(130.dp, 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .border(3.dp, Palette.Outline, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        label()
        Dots(count, 12.dp)
    }
}

enum class FacePart { EYES, NOSE, MOUTH }

@Composable
fun FacePartView(part: FacePart, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        when (part) {
            FacePart.EYES -> {
                drawCircle(Palette.Ink, radius = w * 0.12f, center = Offset(w * 0.25f, w * 0.5f))
                drawCircle(Palette.Ink, radius = w * 0.12f, center = Offset(w * 0.75f, w * 0.5f))
            }
            FacePart.NOSE -> {
                val p = Path().apply { moveTo(w * 0.5f, w * 0.15f); lineTo(w * 0.75f, w * 0.8f); lineTo(w * 0.25f, w * 0.8f); close() }
                drawPath(p, Color(0xFFE2B98F))
                drawPath(p, Palette.Outline, style = Stroke(width = w * 0.05f))
            }
            FacePart.MOUTH -> drawPath(
                Path().apply { moveTo(w * 0.1f, w * 0.35f); quadraticBezierTo(w * 0.5f, w * 0.95f, w * 0.9f, w * 0.35f) },
                Palette.Ink, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun FaceBase(size: Dp, showEyes: Boolean) {
    Canvas(Modifier.size(size * 0.86f, size)) {
        val w = this.size.width
        val h = this.size.height
        drawOval(Palette.Skin, size = Size(w, h))
        drawOval(Palette.Outline, size = Size(w, h), style = Stroke(width = w * 0.02f))
        if (showEyes) {
            drawCircle(Palette.Ink, radius = w * 0.045f, center = Offset(w * 0.34f, h * 0.38f))
            drawCircle(Palette.Ink, radius = w * 0.045f, center = Offset(w * 0.66f, h * 0.38f))
        }
    }
}
