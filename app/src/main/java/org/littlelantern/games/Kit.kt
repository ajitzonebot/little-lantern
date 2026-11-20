package org.littlelantern.games

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.littlelantern.ui.Glyph
import org.littlelantern.ui.GlyphIcon
import org.littlelantern.ui.Palette
import org.littlelantern.ui.RoundButton
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Speech ----------

/** Spoken prompts: children of 3 to 5 cannot read, so every instruction is said aloud. */
interface Speaker {
    fun say(text: String)
}

object SilentSpeaker : Speaker {
    override fun say(text: String) = Unit
}

class TtsSpeaker(context: Context) : Speaker, TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.85f)
            ready = true
        }
    }

    override fun say(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ll")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

val LocalSpeaker = staticCompositionLocalOf<Speaker> { SilentSpeaker }
val LocalColourBlind = staticCompositionLocalOf { false }

// ---------- Shapes ----------

enum class Shape { CIRCLE, SQUARE, TRIANGLE, STAR }

fun shapePath(shape: Shape, s: Size): Path = Path().apply {
    val w = s.width
    val h = s.height
    when (shape) {
        Shape.CIRCLE -> addOval(Rect(w * 0.1f, h * 0.1f, w * 0.9f, h * 0.9f))
        Shape.SQUARE -> addRoundRect(RoundRect(w * 0.12f, h * 0.12f, w * 0.88f, h * 0.88f, CornerRadius(w * 0.06f)))
        Shape.TRIANGLE -> {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.92f, h * 0.88f)
            lineTo(w * 0.08f, h * 0.88f)
            close()
        }
        Shape.STAR -> {
            val pts = listOf(50 to 8, 61 to 38, 93 to 38, 67 to 57, 77 to 89, 50 to 70, 23 to 89, 33 to 57, 7 to 38, 39 to 38)
            pts.forEachIndexed { i, (x, y) ->
                if (i == 0) moveTo(w * x / 100f, h * y / 100f) else lineTo(w * x / 100f, h * y / 100f)
            }
            close()
        }
    }
}

fun DrawScope.drawGameShape(shape: Shape, color: Color, dashed: Boolean = false) {
    val path = shapePath(shape, size)
    if (dashed) {
        drawPath(
            path,
            Palette.Sand,
            style = Stroke(width = size.minDimension * 0.05f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
        )
    } else {
        drawPath(path, color)
        drawPath(path, Palette.Outline, style = Stroke(width = size.minDimension * 0.045f, join = StrokeJoin.Round))
    }
}

@Composable
fun ShapeView(shape: Shape, color: Color, size: Dp, modifier: Modifier = Modifier, dashed: Boolean = false) {
    Canvas(modifier.size(size)) { drawGameShape(shape, color, dashed) }
}

fun colourName(c: Color): String = when (c) {
    Palette.Blue -> "blue"
    Palette.Orange -> "orange"
    Palette.Sky -> "light blue"
    Palette.Pink -> "pink"
    else -> "this colour"
}

fun shapeName(s: Shape): String = s.name.lowercase()

/** Everyday objects drawn by the system emoji font: recognisable pictures with no image files. */
@Composable
fun Emoji(char: String, size: Dp, modifier: Modifier = Modifier) {
    Text(char, fontSize = (size.value * 0.8f).sp, modifier = modifier)
}

// ---------- Screen frame ----------

@Composable
fun GameScaffold(
    prompt: String?,
    onBack: () -> Unit,
    onReplay: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Glyph.BACK, "Back", onBack)
            if (prompt != null) {
                Spacer(Modifier.width(12.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = onReplay != null, role = Role.Button) { onReplay?.invoke() }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(Glyph.SPEAKER, Palette.Sage, 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(prompt, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Sage)
                }
            }
        }
        content()
    }
}

/** Play area card. */
@Composable
fun ColumnScope.PlayArea(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** End of a game: no score, no stars. Again or back. */
@Composable
fun DonePanel(onAgain: () -> Unit, onBack: () -> Unit) {
    val speaker = LocalSpeaker.current
    LaunchedEffect(Unit) { speaker.say("All done.") }
    GameScaffold(prompt = null, onBack = onBack) {
        PlayArea {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                GlyphIcon(Glyph.LANTERN, Palette.Sand, 64.dp)
                Text("All done", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BigButton("Again", onAgain)
                    BigButton("Back", onBack)
                }
            }
        }
    }
}

@Composable
fun BigButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Sage)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** A tappable choice tile. */
@Composable
fun Tile(label: String, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ---------- Drag and drop ----------

/** Drop targets register their on-screen bounds; a dragged piece is dropped on the target under its centre. */
class DragState {
    val targets = mutableStateMapOf<String, Rect>()
}

@Composable
fun DropTarget(id: String, state: DragState, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.onGloballyPositioned { state.targets[id] = it.boundsInRoot() },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * A piece the child drags. [onDrop] gets the target id under the piece's centre (or null) and returns
 * whether it was accepted. A rejected piece slides back; nothing buzzes.
 */
@Composable
fun Draggable(
    key: Any,
    state: DragState,
    onDrop: (String?) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var offset by remember(key) { mutableStateOf(Offset.Zero) }
    var origin by remember(key) { mutableStateOf(Rect.Zero) }
    var dragging by remember(key) { mutableStateOf(false) }
    val drop by rememberUpdatedState(onDrop)
    Box(
        modifier
            .onGloballyPositioned { origin = it.boundsInRoot() }
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .zIndex(if (dragging) 1f else 0f)
            .pointerInput(key) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        val centre = origin.center + offset
                        val target = state.targets.entries.firstOrNull { it.value.contains(centre) }?.key
                        drop(target)
                        offset = Offset.Zero
                        dragging = false
                    },
                    onDragCancel = {
                        offset = Offset.Zero
                        dragging = false
                    },
                ) { change, amount ->
                    change.consume()
                    offset += amount
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Dashed empty slot. */
@Composable
fun EmptySlot(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawRoundRect(
            Palette.Sand,
            cornerRadius = CornerRadius(this.size.minDimension * 0.12f),
            style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
        )
    }
}

/** A row of dots, used instead of numerals for the youngest children. */
@Composable
fun Dots(count: Int, dot: Dp = 16.dp, color: Color = Palette.InkSoft) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) {
            Box(Modifier.size(dot).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

/** Screen-resting overlay for movement games: the screen dims while the child moves. */
@Composable
fun RestOverlay(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(text, color = Palette.Bg, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(32.dp))
    }
}

/** Shuffles helpers. */
fun <T> List<T>.pick(n: Int): List<T> = shuffled().take(n)

/** Cardinal words for spoken numbers. */
val numberWords = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
)
