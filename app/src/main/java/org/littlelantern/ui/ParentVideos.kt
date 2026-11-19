package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.littlelantern.check.KidCheck
import org.littlelantern.check.Level
import org.littlelantern.check.Verdict
import org.littlelantern.video.VideoEntry
import org.littlelantern.video.VideoStatus

private val GoodColor = Color(0xFF3F6B4A)
private val CautionColor = Color(0xFF8A6418)
private val HighColor = Color(0xFF9A4A3F)

private fun levelColor(level: Level) = when (level) {
    Level.GOOD -> GoodColor
    Level.CAUTION -> CautionColor
    Level.HIGH -> HighColor
}

private fun verdictColor(v: Verdict) = when (v) {
    Verdict.CALM -> GoodColor
    Verdict.CHECK -> CautionColor
    Verdict.NOT_RECOMMENDED -> HighColor
}

/**
 * Parent adds YouTube video or playlist links, sees each video's kid check, and approves or rejects it.
 * Only approved videos ever reach Child mode.
 */
@Composable
fun ParentVideosScreen(
    videos: List<VideoEntry>,
    progress: Map<String, String>,
    message: String?,
    onAddLink: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRetry: (String) -> Unit,
    onMarkFantastical: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var link by remember { mutableStateOf("") }
    val ordered = videos.sortedBy {
        when (it.status) {
            VideoStatus.REVIEW -> 0
            VideoStatus.CHECKING, VideoStatus.QUEUED -> 1
            VideoStatus.FAILED -> 2
            VideoStatus.APPROVED -> 3
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Videos", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("YouTube video or playlist link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onAddLink(link); link = "" },
                    enabled = link.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add and check") }
                Text(
                    "Nothing is downloaded: the check streams short samples and videos stream when played. Tip: YouTube app, Share, Little Lantern (pause the lock first).",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(ordered, key = { it.id }) { video ->
            VideoCard(video, progress[video.id], onApprove, onReject, onRetry, onMarkFantastical)
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoEntry,
    progressText: String?,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRetry: (String) -> Unit,
    onMarkFantastical: (String, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(video.status == VideoStatus.REVIEW) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(video.title.ifEmpty { video.url }, fontWeight = FontWeight.Bold, maxLines = 2)
        if (video.channel.isNotEmpty()) Text(video.channel, style = MaterialTheme.typography.bodySmall)

        val report = video.metrics?.let { KidCheck.evaluate(it) }
        when (video.status) {
            VideoStatus.QUEUED, VideoStatus.CHECKING ->
                Text(progressText ?: "Waiting", style = MaterialTheme.typography.bodyMedium)
            VideoStatus.FAILED -> {
                Text("Could not add: ${video.error ?: "unknown error"}", color = HighColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onRetry(video.id) }) { Text("Try again") }
                    TextButton(onClick = { onReject(video.id) }) { Text("Remove") }
                }
            }
            VideoStatus.REVIEW, VideoStatus.APPROVED -> if (report != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(12.dp).clip(CircleShape).background(verdictColor(report.verdict)))
                    Spacer(Modifier.size(8.dp))
                    Text(report.verdict.label, color = verdictColor(report.verdict), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide details" else "Details") }
                }
                if (expanded) {
                    report.items.forEach { item ->
                        Column(Modifier.fillMaxWidth()) {
                            Row {
                                Text(item.name, modifier = Modifier.weight(1f))
                                Text(item.value, color = levelColor(item.level), fontWeight = FontWeight.Bold)
                            }
                            Text(item.why, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = video.metrics?.parentMarkedFantastical == true,
                            onCheckedChange = { onMarkFantastical(video.id, it) },
                        )
                        Text("I watched it: it has fantastical or impossible events")
                    }
                    Text(
                        "Scores are measured on this phone. Thresholds are this app's defaults, not research numbers.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (video.status == VideoStatus.REVIEW) {
                        Button(onClick = { onApprove(video.id) }) { Text("Approve") }
                        OutlinedButton(onClick = { onReject(video.id) }) { Text("Reject") }
                    } else {
                        Text("Approved", color = GoodColor, modifier = Modifier.padding(top = 12.dp))
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onReject(video.id) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
