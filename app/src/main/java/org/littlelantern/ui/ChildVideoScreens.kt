package org.littlelantern.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

private val calmThumbFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.6f) })

data class PickerItem(val id: String, val title: String, val thumb: File)

@Composable
private fun Thumb(file: File, modifier: Modifier) {
    val bitmap = remember(file.path) { if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, colorFilter = calmThumbFilter, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(Color(0xFFC9D3C4)))
    }
}

/** A few big choices, the same set all week. No search, no "more". */
@Composable
fun VideoPicker(items: List<PickerItem>, onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("<", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.size(16.dp))
            Text("Pick one", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = { it.id }) { item ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onPick(item.id) },
                ) {
                    Thumb(item.thumb, Modifier.fillMaxWidth().height(170.dp))
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

/** Still screen between videos. Choices appear only after the pause. No countdown. */
@Composable
fun PauseScreen(thumb: File, pauseMs: Long, onOneMore: () -> Unit, onAllDone: () -> Unit) {
    var showChoices by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(pauseMs)
        showChoices = true
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFECE8DF)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Thumb(thumb, Modifier.size(220.dp).clip(CircleShape))
        Text("Resting for a moment", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (showChoices) {
            BigButton("One more", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, onOneMore)
            BigButton("All done", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onAllDone)
        }
    }
}

@Composable
private fun BigButton(label: String, background: Color, content: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(36.dp)).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = content, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}

/** The same closing routine every time, then rest. */
@Composable
fun ClosingScreen(onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        delay(8_000)
        done()
    }
    LanternMessage(title = "Time to play", body = "The lantern is going to sleep.\nGo find your grown-up.")
}

/** Child mode is closed until the rest ends or a parent wakes it. */
@Composable
fun RestingScreen(canWake: () -> Boolean, onWake: () -> Unit) {
    val wake by rememberUpdatedState(onWake)
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (canWake()) {
                wake()
                break
            }
        }
    }
    LanternMessage(title = "The lantern is sleeping", body = "Time for something else.")
}

@Composable
private fun LanternMessage(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF2F3431)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(160.dp).clip(CircleShape).background(Color(0xFF3C443F)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE8C98A)))
        }
        Text(title, color = Color(0xFFF4F1EA), fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(body, color = Color(0xFFD6D0C3), fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

/** Calm still screen while the video's stream is found. */
@Composable
fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFECE8DF)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFDDE3DA)))
        Text("Getting ready", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
}
