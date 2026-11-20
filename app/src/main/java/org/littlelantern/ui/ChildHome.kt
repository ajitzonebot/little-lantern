package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.littlelantern.data.Video

/**
 * Child home: the parent's videos and a Games tile. No timer is shown; the screen itself calms down.
 * Videos that would run past the daily hour fade out and cannot start.
 */
@Composable
fun ChildHome(
    videos: List<Video>,
    canStart: (Video) -> Boolean,
    onWatch: (Video) -> Unit,
    onGames: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.LANTERN, Palette.Sand, 28.dp)
                Spacer(Modifier.padding(start = 10.dp))
                Text("Little Lantern", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Sage)
            }
        }
        item(span = { GridItemSpan(2) }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.Sage)
                    .clickable(role = Role.Button, onClick = onGames),
                contentAlignment = Alignment.Center,
            ) {
                Text("Games", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (videos.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text("Watch", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.InkSoft)
            }
        }
        items(videos, key = { it.id }) { video ->
            val enabled = canStart(video)
            Column(
                Modifier
                    .alpha(if (enabled) 1f else 0.25f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = enabled, role = Role.Button) { onWatch(video) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Palette.Line),
                )
                Text(
                    video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(24.dp)) }
    }
}

/** Shown when the day's hour is used up. Same screen every day, no countdown before it. */
@Composable
fun EndScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.EndScreen)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphIcon(Glyph.LANTERN, Palette.Border, 80.dp)
        Spacer(Modifier.height(20.dp))
        Text("The lantern is sleeping", color = Palette.Bg, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("See you tomorrow", color = Color(0xFFE6E1D6), fontSize = 15.sp)
    }
}
