package org.littlelantern.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.littlelantern.ui.Glyph
import org.littlelantern.ui.GlyphIcon
import org.littlelantern.ui.Palette
import kotlin.math.hypot
import kotlin.random.Random

// ---------- Finger paint ----------

private class Stroke2(val color: Color, val points: MutableList<Offset>)

/** Blank page, four colours, finger only. Nothing is scored or kept. */
@Composable
fun FingerPaintGame(onExit: () -> Unit) {
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var color by remember { mutableStateOf(Palette.Blue) }
    var tick by remember { mutableIntStateOf(0) }
    GameScaffold(prompt = null, onBack = onExit) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { strokes.add(Stroke2(color, mutableListOf(it))); tick++ },
                    ) { change, _ ->
                        change.consume()
                        strokes.lastOrNull()?.points?.add(change.position)
                        tick++
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (tick < 0) return@Canvas
                strokes.forEach { s ->
                    val path = Path()
                    s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                    drawPath(path, s.color, style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            Palette.pieces.forEach { c ->
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (c == color) 4.dp else 2.dp, if (c == color) Palette.Ink else Palette.Outline, CircleShape)
                        .clickable(role = Role.Button) { color = c }
                        .semantics { contentDescription = colourName(c) },
                )
            }
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Border, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { strokes.clear(); tick++ }
                    .semantics { contentDescription = "New page" },
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(Glyph.PLUS, Palette.Sage, 24.dp) }
        }
    }
}

// ---------- Trace letters ----------

/** Capital letters made of straight strokes, in a 0..1 box. Each stroke is (x1, y1, x2, y2). */
private val letters: Map<Char, List<FloatArray>> = mapOf(
    'A' to listOf(floatArrayOf(.15f, .95f, .5f, .05f), floatArrayOf(.5f, .05f, .85f, .95f), floatArrayOf(.3f, .6f, .7f, .6f)),
    'E' to listOf(floatArrayOf(.25f, .05f, .25f, .95f), floatArrayOf(.25f, .05f, .8f, .05f), floatArrayOf(.25f, .5f, .7f, .5f), floatArrayOf(.25f, .95f, .8f, .95f)),
    'F' to listOf(floatArrayOf(.25f, .05f, .25f, .95f), floatArrayOf(.25f, .05f, .8f, .05f), floatArrayOf(.25f, .5f, .7f, .5f)),
    'H' to listOf(floatArrayOf(.2f, .05f, .2f, .95f), floatArrayOf(.8f, .05f, .8f, .95f), floatArrayOf(.2f, .5f, .8f, .5f)),
    'I' to listOf(floatArrayOf(.5f, .05f, .5f, .95f), floatArrayOf(.3f, .05f, .7f, .05f), floatArrayOf(.3f, .95f, .7f, .95f)),
    'L' to listOf(floatArrayOf(.25f, .05f, .25f, .95f), floatArrayOf(.25f, .95f, .8f, .95f)),
    'T' to listOf(floatArrayOf(.15f, .05f, .85f, .05f), floatArrayOf(.5f, .05f, .5f, .95f)),
)

private val letterSounds = mapOf('A' to "ah", 'E' to "eh", 'F' to "fff", 'H' to "hh", 'I' to "ih", 'L' to "lll", 'T' to "t")

/** Trace with a finger from the green dot. Done when most of the letter has been covered. */
@Composable
fun TraceLettersGame(onExit: () -> Unit) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val order = remember(session) { letters.keys.toList().pick(4) }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= order.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val letter = order[index]
    val strokes = letters.getValue(letter)
    // Sample points along each stroke; the child covers them.
    val samples = remember(letter) {
        strokes.flatMap { s ->
            val n = 12
            (0..n).map { i -> Offset(s[0] + (s[2] - s[0]) * i / n, s[1] + (s[3] - s[1]) * i / n) }
        }
    }
    var covered by remember(session, index) { mutableStateOf(setOf<Int>()) }
    var ink by remember(session, index) { mutableStateOf(listOf<Offset>()) }
    val done = covered.size >= samples.size * 0.9
    LaunchedEffect(session, index) { speaker.say("Trace the letter $letter. Start at the green dot.") }
    LaunchedEffect(done) {
        if (done) {
            speaker.say("That's $letter. $letter says ${letterSounds[letter]}.")
            delay(2600)
            index++
        }
    }
    GameScaffold(prompt = "Trace $letter", onBack = onExit, onReplay = { speaker.say("Trace the letter $letter.") }) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
                .pointerInput(letter, session) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val p = change.position
                        ink = ink + p
                        val side = minOf(size.width, size.height) * 0.8f
                        val left = (size.width - side) / 2f
                        val top = (size.height - side) / 2f
                        val tol = side * 0.08f
                        val hits = samples.indices.filter { i ->
                            val s = samples[i]
                            hypot(left + s.x * side - p.x, top + s.y * side - p.y) < tol
                        }
                        if (hits.isNotEmpty()) covered = covered + hits
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val side = minOf(size.width, size.height) * 0.8f
                val left = (size.width - side) / 2f
                val top = (size.height - side) / 2f
                fun at(x: Float, y: Float) = Offset(left + x * side, top + y * side)
                strokes.forEach { s ->
                    drawLine(Palette.Line, at(s[0], s[1]), at(s[2], s[3]), strokeWidth = side * 0.12f, cap = StrokeCap.Round)
                    drawLine(
                        Palette.Sand, at(s[0], s[1]), at(s[2], s[3]), strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
                    )
                }
                covered.forEach { i -> drawCircle(Palette.Blue, side * 0.045f, at(samples[i].x, samples[i].y)) }
                val first = strokes.first()
                drawCircle(Palette.StartDot, side * 0.05f, at(first[0], first[1]))
            }
        }
    }
}

// ---------- Movement (screen rests) ----------

/** Each prompt is spoken, then the screen dims for [restSeconds] while the child moves. */
@Composable
fun MoveGame(onExit: () -> Unit, prompts: List<Pair<String, String>>, restSeconds: Int = 30) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= prompts.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val (text, emoji) = prompts[index]
    LaunchedEffect(session, index) {
        speaker.say(text)
        delay(restSeconds * 1000L)
        index++
    }
    Box(Modifier.fillMaxSize()) {
        GameScaffold(prompt = text, onBack = onExit, onReplay = { speaker.say(text) }) {
            PlayArea(Modifier.background(Palette.Rest)) { Emoji(emoji, 150.dp) }
        }
        RestOverlay("Screen rests while you move")
    }
}

/** Dance while the voice says dance; freeze when it says freeze. Practises stopping on cue. */
@Composable
fun MoveFreezeGame(onExit: () -> Unit) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    var round by remember(session) { mutableIntStateOf(0) }
    var dancing by remember(session) { mutableStateOf(true) }
    if (round >= 4) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    LaunchedEffect(session, round) {
        dancing = true
        speaker.say("Dance!")
        delay(Random.nextLong(6_000, 12_000))
        dancing = false
        speaker.say("Freeze!")
        delay(Random.nextLong(3_000, 5_000))
        round++
    }
    Box(Modifier.fillMaxSize()) {
        GameScaffold(prompt = if (dancing) "Dance!" else "Freeze!", onBack = onExit) {
            PlayArea(Modifier.background(Palette.Rest)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Emoji(if (dancing) "💃" else "🧊", 140.dp)
                    Text(if (dancing) "Dance" else "Freeze", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                }
            }
        }
        RestOverlay("Screen rests while you dance")
    }
}

// ---------- Sink or float ----------

private data class Floater(val emoji: String, val name: String, val floats: Boolean)

private val floaters = listOf(
    Floater("🪨", "stone", false), Floater("🍃", "leaf", true), Floater("🥄", "spoon", false),
    Floater("🍎", "apple", true), Floater("🔑", "key", false), Floater("🦆", "rubber duck", true),
    Floater("🪙", "coin", false), Floater("⛵", "toy boat", true),
)

/** Guess first, then watch. Any guess is fine; the point is predict-then-check. */
@Composable
fun SinkFloatGame(onExit: () -> Unit) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val items = remember(session) { floaters.pick(5) }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= items.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val item = items[index]
    var dropped by remember(session, index) { mutableStateOf(false) }
    val prompt = "Will the ${item.name} sink or float?"
    LaunchedEffect(session, index) { speaker.say(prompt) }
    LaunchedEffect(dropped) {
        if (dropped) {
            delay(1200)
            speaker.say(if (item.floats) "The ${item.name} floats!" else "The ${item.name} sinks!")
            delay(2400)
            index++
        }
    }
    GameScaffold(prompt = "Sink or float?", onBack = onExit, onReplay = { speaker.say(prompt) }) {
        PlayArea {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Water)
                        .border(3.dp, Palette.Outline, RoundedCornerShape(16.dp)),
                )
                val align = when {
                    !dropped -> Alignment.TopCenter
                    item.floats -> Alignment.Center
                    else -> Alignment.BottomCenter
                }
                Box(Modifier.align(align)) { Emoji(item.emoji, 90.dp) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
            Tile("Sink", 96.dp, enabled = !dropped, onClick = { dropped = true }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⬇", fontSize = 34.sp, color = Palette.Blue)
                    Text("Sink", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tile("Float", 96.dp, enabled = !dropped, onClick = { dropped = true }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("〰", fontSize = 34.sp, color = Palette.Orange)
                    Text("Float", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
