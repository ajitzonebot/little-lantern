package org.littlelantern.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.littlelantern.ui.Glyph
import org.littlelantern.ui.Palette
import org.littlelantern.ui.RoundButton

@Composable
fun GamesMenu(games: List<GameInfo>, onPlay: (GameInfo) -> Unit, onHome: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Glyph.HOME, "Back to home", onHome)
                Spacer(Modifier.width(14.dp))
                Text("Games", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Sage)
            }
        }
        items(games, key = { it.id }) { g ->
            Column(
                Modifier
                    .height(124.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { onPlay(g) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShapeView(g.tileShape, g.tileColor, 46.dp)
                Spacer(Modifier.height(8.dp))
                Text(g.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
            }
        }
        item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(20.dp)) }
    }
}
