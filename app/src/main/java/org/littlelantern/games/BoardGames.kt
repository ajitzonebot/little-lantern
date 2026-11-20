package org.littlelantern.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.littlelantern.ui.Palette
import kotlin.random.Random

// ---------- Number path (Siegler and Ramani's linear board game) ----------

/**
 * One straight line of numbers. Spin shows 1..[spinMax] dots; the child taps that many squares,
 * hearing each number. Reaching the end finishes quietly; nobody wins.
 */
@Composable
fun NumberPathGame(onExit: () -> Unit, length: Int, spinMax: Int, vertical: Boolean) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    var position by remember(session) { mutableIntStateOf(0) }
    var stepsLeft by remember(session) { mutableIntStateOf(0) }
    var spun by remember(session) { mutableIntStateOf(0) }
    if (position >= length && stepsLeft == 0) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val prompt = if (stepsLeft > 0) "Tap $stepsLeft more" else "Spin!"
    LaunchedEffect(session) { speaker.say("Spin, then move along the numbers.") }

    GameScaffold(prompt = prompt, onBack = onExit, onReplay = { speaker.say(prompt) }) {
        PlayArea {
            val cell: @Composable (Int) -> Unit = { n ->
                val next = stepsLeft > 0 && n == position + 1
                Box(
                    Modifier
                        .size(if (vertical) 50.dp else 30.dp, if (vertical) 22.dp else 52.dp)
                        .background(if (n == position) Palette.Orange else if (n % 2 == 0) Color(0xFFD6CFBF) else Color(0xFFE3DDD0))
                        .border(if (next) 2.dp else 0.dp, if (next) Palette.Sage else Color.Transparent)
                        .clickable(enabled = next, role = Role.Button) {
                            position = n
                            stepsLeft--
                            speaker.say(numberWords[n])
                        }
                        .semantics { contentDescription = "Square $n" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", fontSize = if (vertical) 12.sp else 13.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                }
            }
            if (vertical) {
                Column(Modifier.border(2.dp, Palette.Sand)) { (length downTo 1).forEach { cell(it) } }
            } else {
                Row(Modifier.border(2.dp, Palette.Sand)) { (1..length).forEach { cell(it) } }
            }
        }
        Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(if (stepsLeft == 0) Palette.Sage else Palette.Line)
                    .clickable(enabled = stepsLeft == 0 && position < length, role = Role.Button) {
                        val k = Random.nextInt(1, spinMax + 1).coerceAtMost(length - position)
                        spun = k
                        stepsLeft = k
                        speaker.say(numberWords[k])
                    }
                    .semantics { contentDescription = "Spin" },
                contentAlignment = Alignment.Center,
            ) {
                Dots(if (spun == 0) spinMax else spun, 16.dp, Palette.Surface)
            }
        }
    }
}

// ---------- Find pairs ----------

@Composable
fun MemoryGame(onExit: () -> Unit, pairs: Int) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val faces = remember(session) {
        val kinds = Shape.entries.flatMap { s -> Palette.pieces.map { c -> s to c } }.pick(pairs)
        (kinds + kinds).shuffled()
    }
    var open by remember(session) { mutableStateOf(listOf<Int>()) }
    var matched by remember(session) { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()
    if (matched.size == faces.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    LaunchedEffect(session) { speaker.say("Find two the same.") }
    GameScaffold(prompt = "Find two the same", onBack = onExit, onReplay = { speaker.say("Find two the same.") }) {
        PlayArea {
            val cols = if (faces.size <= 4) 2 else if (faces.size <= 8) 2 else 3
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                faces.indices.chunked(cols).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { i ->
                            val up = i in open || i in matched
                            Box(
                                Modifier
                                    .size(if (faces.size <= 4) 110.dp else 84.dp, if (faces.size <= 4) 130.dp else 96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (up) Color.White else Palette.Sage)
                                    .border(if (up) 2.dp else 0.dp, Palette.Outline, RoundedCornerShape(14.dp))
                                    .clickable(enabled = !up && open.size < 2, role = Role.Button) {
                                        val now = open + i
                                        open = now
                                        if (now.size == 2) {
                                            scope.launch {
                                                if (faces[now[0]] == faces[now[1]]) {
                                                    matched = matched + now
                                                    speaker.say("Same!")
                                                    open = emptyList()
                                                } else {
                                                    delay(900)
                                                    open = emptyList()
                                                }
                                            }
                                        }
                                    }
                                    .semantics { contentDescription = if (up) "${colourName(faces[i].second)} ${shapeName(faces[i].first)}" else "Card" },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (up) ShapeView(faces[i].first, faces[i].second, 56.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Picture puzzles ----------

/** Simple scenes drawn in code. Index picks the scene. */
fun DrawScope.drawScene(index: Int, s: Size) {
    val w = s.width
    val h = s.height
    val outline = Stroke(width = w * 0.012f)
    when (index % 2) {
        0 -> {
            drawRect(Palette.SkyBg, size = s)
            drawCircle(Palette.Orange, radius = w * 0.14f, center = Offset(w * 0.75f, h * 0.25f))
            drawCircle(Palette.Outline, radius = w * 0.14f, center = Offset(w * 0.75f, h * 0.25f), style = outline)
            val hill = Path().apply {
                moveTo(0f, h * 0.65f); quadraticBezierTo(w * 0.5f, h * 0.35f, w, h * 0.65f); lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(hill, Palette.Grass)
            drawPath(hill, Palette.Outline, style = outline)
            val roof = Path().apply { moveTo(w * 0.57f, h * 0.71f); lineTo(w * 0.71f, h * 0.59f); lineTo(w * 0.85f, h * 0.71f); close() }
            drawPath(roof, Palette.Blue)
            drawPath(roof, Palette.Outline, style = outline)
            drawRect(Palette.Pink, topLeft = Offset(w * 0.6f, h * 0.7f), size = Size(w * 0.22f, h * 0.2f))
            drawRect(Palette.Outline, topLeft = Offset(w * 0.6f, h * 0.7f), size = Size(w * 0.22f, h * 0.2f), style = outline)
        }
        else -> {
            drawRect(Palette.SkyBg, size = s)
            drawRect(Palette.Water, topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.4f))
            drawCircle(Palette.Orange, radius = w * 0.12f, center = Offset(w * 0.22f, h * 0.22f))
            val hull = Path().apply { moveTo(w * 0.3f, h * 0.62f); lineTo(w * 0.8f, h * 0.62f); lineTo(w * 0.7f, h * 0.76f); lineTo(w * 0.4f, h * 0.76f); close() }
            drawPath(hull, Palette.Blue)
            drawPath(hull, Palette.Outline, style = outline)
            val sail = Path().apply { moveTo(w * 0.55f, h * 0.2f); lineTo(w * 0.55f, h * 0.58f); lineTo(w * 0.78f, h * 0.58f); close() }
            drawPath(sail, Palette.Pink)
            drawPath(sail, Palette.Outline, style = outline)
        }
    }
}

@Composable
private fun PuzzleTile(scene: Int, grid: Int, cell: Int, size: Dp) {
    Canvas(Modifier.size(size)) {
        val full = Size(this.size.width * grid, this.size.height * grid)
        val col = cell % grid
        val row = cell / grid
        clipRect {
            translate(-col * this.size.width, -row * this.size.height) { drawScene(scene, full) }
        }
        drawRect(Palette.Outline, style = Stroke(width = 2.dp.toPx()))
    }
}

/**
 * Picture puzzle: some tiles are missing; drag each piece to its place.
 * Each round is (grid size, number of missing tiles).
 */
@Composable
fun PuzzleGame(onExit: () -> Unit, rounds: List<Pair<Int, Int>>) {
    DragGame(onExit, pieceSize = 72.dp) {
        rounds.mapIndexed { r, (grid, missingCount) ->
            val scene = r + Random.nextInt(2)
            val cells = grid * grid
            val missing = (0 until cells).shuffled().take(missingCount).toSet()
            val tile: Dp = if (grid == 2) 120.dp else 84.dp
            val pieceTile: Dp = if (grid == 2) 80.dp else 64.dp
            DragRound(
                prompt = "Put the pieces in",
                slots = missing.map { c ->
                    DragSlot(
                        id = "c$c",
                        accepts = { it.id == "p$c" },
                        empty = { EmptySlot(tile) },
                        filled = { PuzzleTile(scene, grid, c, tile) },
                    )
                },
                pieces = missing.shuffled().map { c -> DragPiece("p$c") { PuzzleTile(scene, grid, c, pieceTile) } },
                layout = { slot ->
                    Column {
                        (0 until grid).forEach { row ->
                            Row {
                                (0 until grid).forEach { col ->
                                    val c = row * grid + col
                                    if (c in missing) slot("c$c", Modifier.size(tile)) else PuzzleTile(scene, grid, c, tile)
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

// ---------- Tap to count (apples) ----------

@Composable
fun CountTapGame(onExit: () -> Unit, rounds: Int, range: IntRange) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val counts = remember(session) { List(rounds) { range.random() } }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= counts.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val n = counts[index]
    var tapped by remember(session, index) { mutableStateOf(listOf<Int>()) }
    LaunchedEffect(session, index) { speaker.say("Count the apples. Tap each one.") }
    LaunchedEffect(tapped.size == n, session, index) {
        if (tapped.size == n) {
            delay(900)
            speaker.say("${numberWords[n]} apples!")
            delay(1800)
            index++
        }
    }
    GameScaffold(prompt = "Count the apples", onBack = onExit, onReplay = { speaker.say("Count the apples.") }) {
        PlayArea {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                (0 until n).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { i ->
                            val order = tapped.indexOf(i)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .alpha(if (order >= 0) 0.4f else 1f)
                                        .clip(CircleShape)
                                        .clickable(enabled = order < 0, role = Role.Button) {
                                            tapped = tapped + i
                                            speaker.say(numberWords[tapped.size])
                                        }
                                        .semantics { contentDescription = "Apple" },
                                ) { Emoji("🍎", 64.dp) }
                                Text(if (order >= 0) "${order + 1}" else " ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- Clap the syllables ----------

@Composable
fun SyllableGame(onExit: () -> Unit) {
    val speaker = LocalSpeaker.current
    val words = remember {
        listOf(
            Triple("🐱", "cat", listOf("cat")), Triple("☀️", "sun", listOf("sun")),
            Triple("🍎", "apple", listOf("ap", "ple")), Triple("🐯", "tiger", listOf("ti", "ger")),
            Triple("🐰", "rabbit", listOf("rab", "bit")), Triple("🍌", "banana", listOf("ba", "na", "na")),
            Triple("🦋", "butterfly", listOf("but", "ter", "fly")), Triple("🐘", "elephant", listOf("el", "e", "phant")),
        )
    }
    var session by remember { mutableIntStateOf(0) }
    val list = remember(session) { words.pick(5) }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= list.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val (emoji, word, parts) = list[index]
    var taps by remember(session, index) { mutableIntStateOf(0) }
    var lastTap by remember(session, index) { mutableStateOf(0L) }
    val sayWord = { speaker.say(parts.joinToString(" ... ")) }
    LaunchedEffect(session, index) { speaker.say("Tap the drum for each beat."); delay(1800); sayWord() }
    LaunchedEffect(lastTap) {
        if (taps == 0) return@LaunchedEffect
        delay(1500)
        if (taps == parts.size) {
            speaker.say("Yes! $word has ${numberWords[parts.size]} ${if (parts.size == 1) "beat" else "beats"}.")
            delay(2400)
            index++
        } else {
            speaker.say("Listen again.")
            delay(1000)
            sayWord()
            taps = 0
        }
    }
    GameScaffold(prompt = parts.joinToString(" · "), onBack = onExit, onReplay = sayWord) {
        PlayArea { Emoji(emoji, 140.dp) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Palette.Orange)
                    .border(3.dp, Palette.Outline, CircleShape)
                    .clickable(role = Role.Button) {
                        taps++
                        lastTap = System.currentTimeMillis()
                    }
                    .semantics { contentDescription = "Drum" },
            )
            Spacer(Modifier.width(20.dp))
            Dots(taps, 18.dp, Palette.Sage)
        }
    }
}

/** Animated alpha helper for fading choices. */
@Composable
fun fade(target: Float) = animateFloatAsState(target, tween(400), label = "fade").value

@Composable
fun FillBox(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) { content() }
