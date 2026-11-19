package org.littlelantern.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.littlelantern.data.AppEntry

/** Desaturated icons: calm visuals by default. */
private val calmIconFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.5f) })

@Composable
fun ChildHome(games: List<AppEntry>, hasVideos: Boolean, onOpenVideos: () -> Unit, onOpenGame: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        Text(
            "Little Lantern",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "videos") {
                Tile(
                    label = "Videos",
                    subtitle = if (hasVideos) null else "Ask a grown-up to add some",
                    icon = null,
                    enabled = hasVideos,
                    onClick = onOpenVideos,
                )
            }
            items(games, key = { it.packageName }) { app ->
                Tile(label = app.label, subtitle = null, icon = app.icon, enabled = true) {
                    onOpenGame(app.packageName)
                }
            }
        }
    }
}

@Composable
private fun Tile(
    label: String,
    subtitle: String?,
    icon: Drawable?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
    Column(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            val bitmap = remember(icon) { icon.toBitmap(128, 128).asImageBitmap() }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                colorFilter = calmIconFilter,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = textColor, textAlign = TextAlign.Center, maxLines = 2)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textColor)
        }
    }
}
