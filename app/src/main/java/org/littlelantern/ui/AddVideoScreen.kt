package org.littlelantern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.littlelantern.data.Video
import org.littlelantern.video.CheckReport
import org.littlelantern.video.KeywordScan
import org.littlelantern.video.OnlineChecks
import org.littlelantern.video.ResolvedVideo
import org.littlelantern.video.StreamCheck
import org.littlelantern.video.Streams
import org.littlelantern.video.Verdict

private sealed interface CheckState {
    data object Idle : CheckState
    data object Resolving : CheckState
    data class Checking(val video: ResolvedVideo) : CheckState
    data class Done(val video: ResolvedVideo, val report: CheckReport, val sponsor: OnlineChecks.SponsorResult?) : CheckState
    data class Failed(val message: String) : CheckState
}

/**
 * Parent: paste a YouTube link, run the checks (all online, nothing downloaded), then approve or reject.
 */
@Composable
fun AddVideoScreen(apiKey: String, onApprove: (Video) -> Unit, onClose: () -> Unit) {
    var link by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<CheckState>(CheckState.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }
    var fantasy by remember { mutableStateOf(false) }
    var urging by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun runCheck() {
        job?.cancel()
        progress = 0f
        job = scope.launch {
            state = CheckState.Resolving
            val resolved = runCatching { Streams.resolve(link.trim()) }.getOrElse {
                state = CheckState.Failed("Could not open this link. Check it is a YouTube video link.")
                return@launch
            }
            state = CheckState.Checking(resolved)
            val (report, sponsor) = coroutineScope {
                val sb = async { OnlineChecks.sponsorBlock(resolved.id) }
                val yt = async { OnlineChecks.youtubeFlags(resolved.id, apiKey) }
                val loud = async { resolved.checkAudioUrl?.let { url -> runCatching { StreamCheck.loudnessJumps(url) }.getOrNull() } }
                val visual = resolved.checkVideoUrl?.let { url ->
                    runCatching { StreamCheck.visual(url, resolved.durationSeconds) { progress = it } }
                }
                val sponsorResult = sb.await()
                val flags = yt.await()
                val v = visual?.getOrNull()
                CheckReport(
                    cutsPerMinute = v?.cutsPerMinute,
                    flashesPerSecondMax = v?.flashesMax,
                    redFlashesPerSecondMax = v?.redFlashesMax,
                    saturation = v?.saturation,
                    loudnessJumps = loud.await(),
                    analysisError = when {
                        visual == null -> "No stream found to check"
                        visual.isFailure -> visual.exceptionOrNull()?.message ?: "Stream check failed"
                        else -> null
                    },
                    interactionSegments = sponsorResult?.interaction,
                    sponsorSegments = sponsorResult?.sponsor,
                    paidPlacement = flags.paidPlacement,
                    madeForKids = flags.madeForKids,
                    keywordHits = KeywordScan.hits(resolved.title + "\n" + resolved.description),
                ) to sponsorResult
            }
            state = CheckState.Done(resolved, report, sponsor)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Add a video", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("YouTube link") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::runCheck, enabled = link.isNotBlank() && state !is CheckState.Resolving && state !is CheckState.Checking) {
                Text("Check")
            }
        }

        when (val s = state) {
            CheckState.Idle -> Text(
                "The check streams the lowest-quality version once, measures it in memory and saves nothing.",
                fontSize = 13.sp,
                color = Palette.InkSoft,
            )
            CheckState.Resolving -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is CheckState.Failed -> Text(s.message, color = Color(0xFF9A4A3F))
            is CheckState.Checking -> {
                VideoHeader(s.video)
                Text("Streaming and measuring…", fontSize = 13.sp, color = Palette.InkSoft)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            is CheckState.Done -> {
                VideoHeader(s.video)
                ReportCards(s.report)
                Card("You decide") {
                    CheckRow("Mostly fantasy (talking animals, magic)", fantasy) { fantasy = it }
                    CheckRow("Characters urge the child to keep watching", urging) { urging = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Reject") }
                    Button(
                        onClick = {
                            onApprove(
                                Video(
                                    id = s.video.id,
                                    url = link.trim(),
                                    title = s.video.title,
                                    channel = s.video.channel,
                                    durationSeconds = s.video.durationSeconds,
                                    thumbnailUrl = s.video.thumbnailUrl,
                                    skipSegments = s.sponsor?.segments ?: emptyList(),
                                    report = s.report.copy(parentFantasy = fantasy, parentUrging = urging),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Approve") }
                }
            }
        }
        if (state !is CheckState.Done) {
            OutlinedButton(onClick = { job?.cancel(); onClose() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun VideoHeader(v: ResolvedVideo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = v.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(8.dp)).background(Palette.Line),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(v.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2)
            Text("${v.channel} · ${v.durationSeconds / 60} min ${v.durationSeconds % 60} s", fontSize = 12.sp, color = Palette.InkSoft)
        }
    }
}

@Composable
fun ReportCards(r: CheckReport) {
    Card("Pace and flash (streamed, nothing saved)") {
        Line("Cuts per minute", r.cutsPerMinute?.let { "%.1f · %s".format(it, r.paceLabel) } ?: "Unknown", r.paceVerdict)
        Text("Slow show in Lillard 2011: about 1.8. Fast show: about 5.5.", fontSize = 11.sp, color = Palette.InkSoft)
        Line("Flashes (more than 3 per second)", verdictText(r.flashVerdict, "None", "Found"), r.flashVerdict)
        Line("Saturated red flashes", verdictText(r.redFlashVerdict, "None", "Found"), r.redFlashVerdict)
        Line("Colour saturation (rough)", r.saturationLabel, null)
        Line("Sudden loudness jumps", r.loudnessJumps?.toString() ?: "Unknown", r.loudnessJumps?.let { if (it > 3) Verdict.FLAG else Verdict.PASS } ?: Verdict.UNKNOWN)
        r.analysisError?.let { Text("Stream check: $it", fontSize = 11.sp, color = Color(0xFF9A4A3F)) }
    }
    Card("Dark patterns (online)") {
        Line(
            "\"Like and subscribe\" segments",
            r.interactionSegments?.let { if (it == 0) "None marked" else "$it found, will skip" } ?: "Unknown",
            r.interactionSegments?.let { if (it == 0) Verdict.PASS else Verdict.FLAG } ?: Verdict.UNKNOWN,
        )
        Line(
            "Sponsor / self-promo segments",
            r.sponsorSegments?.let { if (it == 0) "None marked" else "$it found, will skip" } ?: "Unknown",
            r.sponsorSegments?.let { if (it == 0) Verdict.PASS else Verdict.FLAG } ?: Verdict.UNKNOWN,
        )
        Line("Paid product placement", r.paidPlacement?.let { if (it) "Yes" else "No" } ?: "Unknown", r.paidPlacement?.let { if (it) Verdict.FLAG else Verdict.PASS } ?: Verdict.UNKNOWN)
        Line("Marked made for kids", r.madeForKids?.let { if (it) "Yes" else "No" } ?: "Unknown", null)
        Line(
            "Title and description words",
            r.keywordHits?.let { if (it.isEmpty()) "None" else it.joinToString(", ") } ?: "Unknown",
            r.keywordHits?.let { if (it.isEmpty()) Verdict.PASS else Verdict.FLAG } ?: Verdict.UNKNOWN,
        )
        Text(
            "SponsorBlock is crowd-marked and may be empty. Paid placement and made-for-kids need a YouTube API key in Parent mode. Unknown is never shown as a pass.",
            fontSize = 11.sp,
            color = Palette.InkSoft,
        )
    }
}

private fun verdictText(v: Verdict, pass: String, flag: String) = when (v) {
    Verdict.PASS -> pass
    Verdict.FLAG -> flag
    Verdict.UNKNOWN -> "Unknown"
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun Line(label: String, value: String, verdict: Verdict?) {
    val color = when (verdict) {
        Verdict.PASS -> Color(0xFF3F5A42)
        Verdict.FLAG -> Color(0xFF8A5A1F)
        Verdict.UNKNOWN -> Color(0xFF6B6559)
        null -> Palette.Ink
    }
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, fontSize = 13.sp)
    }
}
