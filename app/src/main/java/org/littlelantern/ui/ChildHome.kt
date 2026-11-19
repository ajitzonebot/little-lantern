package org.littlelantern.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.littlelantern.ui.games.GameKind

/** Child home: Videos plus the four in-app games. Nothing here opens another app. */
@Composable
fun ChildHome(hasVideos: Boolean, onOpenVideos: () -> Unit, onOpenGame: (GameKind) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Little Lantern", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Column(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFDDE3DA))
                .clickable(enabled = hasVideos, onClick = onOpenVideos),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(Modifier.size(52.dp)) {
                val s = size.minDimension
                drawRoundRect(Color(0xFF4A584C), topLeft = Offset(s * 0.1f, s * 0.2f), size = androidx.compose.ui.geometry.Size(s * 0.8f, s * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f), style = Stroke(s * 0.06f))
                val tri = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * 0.42f, s * 0.38f); lineTo(s * 0.62f, s * 0.5f); lineTo(s * 0.42f, s * 0.62f); close()
                }
                drawPath(tri, Color(0xFF4A584C))
            }
            Text("Videos", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasVideos) 1f else 0.4f))
            if (!hasVideos) Text("Ask a grown-up to add some", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        GameKind.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { game ->
                    Column(
                        Modifier.weight(1f).height(150.dp).clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpenGame(game) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GameIcon(game)
                        Spacer(Modifier.height(10.dp))
                        Text(game.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameIcon(game: GameKind) {
    val brown = Color(0xFF8A6F4E)
    val blue = Color(0xFF6E7A8A)
    Canvas(Modifier.size(48.dp)) {
        val s = size.minDimension
        val line = Stroke(s * 0.05f)
        when (game) {
            GameKind.COUNT -> {
                drawCircle(brown, s * 0.1f, Offset(s * 0.3f, s * 0.33f), style = line)
                drawCircle(brown, s * 0.1f, Offset(s * 0.7f, s * 0.33f), style = line)
                drawCircle(brown, s * 0.1f, Offset(s * 0.5f, s * 0.67f), style = line)
            }
            GameKind.SHAPES -> {
                drawCircle(blue, s * 0.16f, Offset(s * 0.3f, s * 0.3f), style = line)
                drawRect(blue, Offset(s * 0.56f, s * 0.14f), androidx.compose.ui.geometry.Size(s * 0.3f, s * 0.3f), style = line)
                val t = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * 0.5f, s * 0.55f); lineTo(s * 0.72f, s * 0.9f); lineTo(s * 0.28f, s * 0.9f); close()
                }
                drawPath(t, blue, style = line)
            }
            GameKind.PAIRS -> {
                drawRoundRect(brown, Offset(s * 0.1f, s * 0.12f), androidx.compose.ui.geometry.Size(s * 0.36f, s * 0.46f), androidx.compose.ui.geometry.CornerRadius(s * 0.06f), style = line)
                drawRoundRect(brown, Offset(s * 0.54f, s * 0.42f), androidx.compose.ui.geometry.Size(s * 0.36f, s * 0.46f), androidx.compose.ui.geometry.CornerRadius(s * 0.06f), style = line)
            }
            GameKind.PAINT -> {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * 0.15f, s * 0.8f); quadraticBezierTo(s * 0.35f, s * 0.4f, s * 0.55f, s * 0.6f); quadraticBezierTo(s * 0.75f, s * 0.8f, s * 0.88f, s * 0.2f)
                }
                drawPath(p, blue, style = Stroke(s * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}
