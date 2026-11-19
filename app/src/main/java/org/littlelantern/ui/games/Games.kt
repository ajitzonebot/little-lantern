package org.littlelantern.ui.games

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Calm in-app games. Rules from the spec: no timers, scores, stars, rewards or wrong-answer sounds.
 * Each game has a fixed number of rounds and then ends on its own.
 */
enum class GameKind(val title: String, val grownUpPrompt: String) {
    COUNT("Count", "Ask: \"Can you count the spoons on the table?\""),
    SHAPES("Shapes", "Ask: \"Can you find something round in this room?\""),
    PAIRS("Pairs", "Ask: \"Which picture did you like best?\""),
    PAINT("Paint", "Ask: \"Tell me about your painting.\""),
}

private val Ink = Color(0xFF2F2F2B)
private val Quiet = Color(0xFF5E574B)
private val Card = Color(0xFFFBF9F4)
private val Ground = Color(0xFFF4F1EA)
private val Sage = Color(0xFFDDE3DA)

@Composable
fun GameScreen(kind: GameKind, onFinished: () -> Unit, onBack: () -> Unit) {
    when (kind) {
        GameKind.COUNT -> CountGame(onFinished, onBack)
        GameKind.SHAPES -> ShapesGame(onFinished, onBack)
        GameKind.PAIRS -> PairsGame(onFinished, onBack)
        GameKind.PAINT -> PaintGame(onFinished, onBack)
    }
}

@Composable
private fun GameFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Ground).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Card).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("<", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        content()
    }
}

/** Shown when any game ends: no prize, a question for the grown-up, then home. */
@Composable
fun GameDoneScreen(kind: GameKind, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFECE8DF)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(160.dp).clip(CircleShape).background(Sage), contentAlignment = Alignment.Center) {
            Duck(Modifier.size(96.dp))
        }
        Text("All done", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("FOR YOUR GROWN-UP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Quiet)
            Text(kind.grownUpPrompt, fontSize = 18.sp, color = Ink)
        }
        Box(
            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(36.dp)).background(Color(0xFF5B6B5D)).clickable(onClick = onHome),
            contentAlignment = Alignment.Center,
        ) { Text("Back home", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

// ---------- Count ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountGame(onFinished: () -> Unit, onBack: () -> Unit) {
    val rounds = remember { List(5) { Random.nextInt(1, 6) } }
    var round by remember { mutableIntStateOf(0) }
    val target = rounds[round]
    val choices = remember(round) { ((1..6).filter { it != target }.shuffled().take(2) + target).sorted() }
    val tapped = remember(round) { mutableStateListOf<Int>() }
    val tried = remember(round) { mutableStateListOf<Int>() }
    var solved by remember(round) { mutableStateOf(false) }
    val speak = rememberSpeaker()
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(solved) {
        if (solved) {
            delay(1_500)
            if (round < rounds.lastIndex) round++ else finished()
        }
    }

    GameFrame("How many ducks?", onBack) {
        FlowRow(
            Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(Sage).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            repeat(target) { i ->
                Box(
                    Modifier.size(80.dp).clip(CircleShape)
                        .background(if (i in tapped) Color(0x33FFFFFF) else Color.Transparent)
                        .clickable {
                            if (i !in tapped) {
                                tapped.add(i)
                                speak(tapped.size.toString())
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { Duck(Modifier.size(72.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            choices.forEach { n ->
                val faded = n in tried && n != target
                Box(
                    Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(24.dp))
                        .background(if (solved && n == target) Sage else Card)
                        .clickable(enabled = !solved && !faded) {
                            if (n == target) {
                                solved = true
                                speak("$n ducks")
                            } else {
                                tried.add(n) // no sound, the number just fades
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ink.copy(alpha = if (faded) 0.25f else 1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Round ${round + 1} of ${rounds.size}", fontSize = 14.sp, color = Quiet, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/** Slow text-to-speech for counting aloud. Silent if the phone has no voice installed. */
@Composable
private fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    val holder = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setSpeechRate(0.8f)
                holder.value = engine
            }
        }
        onDispose { engine?.shutdown() }
    }
    return { text -> holder.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "little-lantern") }
}

// ---------- Shapes ----------

private enum class ShapeKind(val color: Color) {
    CIRCLE(Color(0xFF9DAF9E)),
    SQUARE(Color(0xFFAFBBC9)),
    TRIANGLE(Color(0xFFC9B48E)),
    DIAMOND(Color(0xFFC29A93)),
}

@Composable
private fun ShapesGame(onFinished: () -> Unit, onBack: () -> Unit) {
    val sizes = listOf(3, 3, 4)
    var round by remember { mutableIntStateOf(0) }
    val kinds = remember(round) { ShapeKind.entries.take(sizes[round]) }
    val pieceOrder = remember(round) { kinds.shuffled() }
    val placed = remember(round) { mutableStateListOf<ShapeKind>() }
    val offsets = remember(round) { mutableStateMapOf<ShapeKind, Offset>() }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(placed.size, round) {
        if (placed.size == kinds.size) {
            delay(1_000)
            if (round < sizes.lastIndex) round++ else finished()
        }
    }

    GameFrame("Put each shape in its home", onBack) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val density = LocalDensity.current
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val piecePx = with(density) { 88.dp.toPx() }
            val n = kinds.size
            fun holeCenter(k: ShapeKind) = Offset(w * (kinds.indexOf(k) + 1) / (n + 1), h * 0.2f)
            fun homeCenter(k: ShapeKind) = Offset(w * (pieceOrder.indexOf(k) + 1) / (n + 1), h * 0.72f)

            Box(
                Modifier.fillMaxWidth().height(with(density) { (h * 0.4f).toDp() }).clip(RoundedCornerShape(24.dp)).background(Color(0xFFD8CFC0)),
            )
            kinds.forEach { k ->
                val c = holeCenter(k)
                Canvas(
                    Modifier.offset { IntOffset((c.x - piecePx / 2).roundToInt(), (c.y - piecePx / 2).roundToInt()) }
                        .size(88.dp),
                ) { drawShape(k, filled = k in placed, hole = k !in placed) }
            }
            pieceOrder.filter { it !in placed }.forEach { k ->
                val pos = offsets[k] ?: homeCenter(k)
                Canvas(
                    Modifier.offset { IntOffset((pos.x - piecePx / 2).roundToInt(), (pos.y - piecePx / 2).roundToInt()) }
                        .size(88.dp)
                        .pointerInput(k, round) {
                            detectDragGestures(
                                onDragEnd = {
                                    val p = offsets[k] ?: homeCenter(k)
                                    val target = holeCenter(k)
                                    if (hypot(p.x - target.x, p.y - target.y) < piecePx * 0.7f) {
                                        placed.add(k)
                                    } else {
                                        offsets[k] = homeCenter(k) // wrong spot: back home, no sound
                                    }
                                },
                                onDragCancel = { offsets[k] = homeCenter(k) },
                            ) { change, drag ->
                                change.consume()
                                offsets[k] = (offsets[k] ?: homeCenter(k)) + drag
                            }
                        },
                ) { drawShape(k, filled = true, hole = false) }
            }
        }
        Text("Round ${round + 1} of ${sizes.size}", fontSize = 14.sp, color = Quiet, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun DrawScope.drawShape(kind: ShapeKind, filled: Boolean, hole: Boolean) {
    val s = size.minDimension
    val inset = s * 0.08f
    val path = Path().apply {
        when (kind) {
            ShapeKind.CIRCLE -> addOval(androidx.compose.ui.geometry.Rect(inset, inset, s - inset, s - inset))
            ShapeKind.SQUARE -> addRect(androidx.compose.ui.geometry.Rect(inset, inset, s - inset, s - inset))
            ShapeKind.TRIANGLE -> {
                moveTo(s / 2, inset); lineTo(s - inset, s - inset); lineTo(inset, s - inset); close()
            }
            ShapeKind.DIAMOND -> {
                moveTo(s / 2, inset); lineTo(s - inset, s / 2); lineTo(s / 2, s - inset); lineTo(inset, s / 2); close()
            }
        }
    }
    if (filled) drawPath(path, kind.color)
    if (hole) drawPath(path, Color(0xFFB5AA97))
    drawPath(path, Color(0xFF6E5E45), style = Stroke(width = s * 0.03f))
}

// ---------- Pairs ----------

@Composable
private fun PairsGame(onFinished: () -> Unit, onBack: () -> Unit) {
    val rounds = 2
    var round by remember { mutableIntStateOf(0) }
    val cards = remember(round) {
        val kinds = ShapeKind.entries.shuffled().take(3)
        (kinds + kinds).shuffled()
    }
    val faceUp = remember(round) { mutableStateListOf<Int>() }
    val matched = remember(round) { mutableStateListOf<Int>() }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(faceUp.size, round) {
        if (faceUp.size == 2) {
            delay(2_000) // stay face up long enough to remember
            faceUp.clear()
        }
    }
    LaunchedEffect(matched.size, round) {
        if (matched.size == cards.size) {
            delay(1_000)
            if (round < rounds - 1) round++ else finished()
        }
    }

    GameFrame("Find the pairs", onBack) {
        cards.indices.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { i ->
                    val shown = i in faceUp || i in matched
                    Box(
                        Modifier.weight(1f).height(130.dp).clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    i in matched -> Sage
                                    shown -> Card
                                    else -> Color(0xFFC9C1B2)
                                },
                            )
                            .clickable(enabled = !shown && faceUp.size < 2) {
                                faceUp.add(i)
                                if (faceUp.size == 2) {
                                    val (a, b) = faceUp
                                    if (cards[a] == cards[b]) {
                                        matched.addAll(listOf(a, b))
                                        faceUp.clear()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (shown) Canvas(Modifier.size(72.dp)) { drawShape(cards[i], filled = true, hole = false) }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Round ${round + 1} of $rounds", fontSize = 14.sp, color = Quiet, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

// ---------- Paint ----------

private class PaintStroke(val color: Color, val points: SnapshotStateList<Offset>)

private val paintColors = listOf(
    Color(0xFF8FA391), Color(0xFFC9A86A), Color(0xFF8A9DB3), Color(0xFFC29A93), Color(0xFF7A6A55),
)

@Composable
private fun PaintGame(onFinished: () -> Unit, onBack: () -> Unit) {
    val pages = 3
    var page by remember { mutableIntStateOf(1) }
    var color by remember { mutableStateOf(paintColors.first()) }
    val strokes = remember(page) { mutableStateListOf<PaintStroke>() }

    GameFrame("Paint", onBack) {
        Canvas(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White)
                .pointerInput(page) {
                    detectDragGestures(
                        onDragStart = { start -> strokes.add(PaintStroke(color, mutableStateListOf(start))) },
                    ) { change, _ ->
                        change.consume()
                        strokes.lastOrNull()?.points?.add(change.position)
                    }
                },
        ) {
            strokes.forEach { s ->
                if (s.points.size == 1) {
                    drawCircle(s.color, radius = 9.dp.toPx(), center = s.points.first())
                } else {
                    val path = Path().apply {
                        moveTo(s.points.first().x, s.points.first().y)
                        s.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, s.color, style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            paintColors.forEach { c ->
                Box(
                    Modifier.size(if (c == color) 52.dp else 44.dp).clip(CircleShape)
                        .background(if (c == color) Ink else Color.Transparent)
                        .padding(4.dp).clip(CircleShape).background(c)
                        .clickable { color = c },
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Card).clickable {
                    if (page < pages) page++ else onFinished()
                },
                contentAlignment = Alignment.Center,
            ) { Text(if (page < pages) "New" else "Done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink) }
        }
        Text("Page $page of $pages", fontSize = 14.sp, color = Quiet, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

// ---------- Drawing helpers ----------

@Composable
private fun Duck(modifier: Modifier) {
    Canvas(modifier) { drawDuck() }
}

private fun DrawScope.drawDuck() {
    val s = size.minDimension
    val outline = Color(0xFF6E5E45)
    val body = Color(0xFFE8C98A)
    drawOval(body, topLeft = Offset(s * 0.15f, s * 0.5f), size = Size(s * 0.75f, s * 0.38f))
    drawOval(outline, topLeft = Offset(s * 0.15f, s * 0.5f), size = Size(s * 0.75f, s * 0.38f), style = Stroke(s * 0.03f))
    drawCircle(body, radius = s * 0.17f, center = Offset(s * 0.37f, s * 0.35f))
    drawCircle(outline, radius = s * 0.17f, center = Offset(s * 0.37f, s * 0.35f), style = Stroke(s * 0.03f))
    drawCircle(outline, radius = s * 0.025f, center = Offset(s * 0.33f, s * 0.32f))
    val beak = Path().apply {
        moveTo(s * 0.21f, s * 0.34f); lineTo(s * 0.08f, s * 0.38f); lineTo(s * 0.21f, s * 0.41f); close()
    }
    drawPath(beak, Color(0xFFC98A4A))
}
