package org.littlelantern.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChoiceOption(
    val label: String,
    val correct: Boolean,
    /** Said when tapped, before checking (e.g. the word in a rhyme game). */
    val sayOnTap: String? = null,
    /** Said after an accepted tap; overrides the round's [ChoiceRound.reply]. */
    val reply: String? = null,
    val content: @Composable () -> Unit,
)

class ChoiceRound(
    val prompt: String,
    val options: List<ChoiceOption>,
    val reply: String? = null,
    /** Any choice is accepted (feelings games, predictions). */
    val anyAccepted: Boolean = false,
    val center: (@Composable () -> Unit)? = null,
)

/**
 * Prompt, picture, a few choices. A wrong tap only fades that choice; the right one moves on.
 * No counters, no timers.
 */
@Composable
fun ChoiceGame(
    onExit: () -> Unit,
    optionSize: Dp = 96.dp,
    makeRounds: () -> List<ChoiceRound>,
) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val rounds = remember(session) { makeRounds() }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= rounds.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val round = rounds[index]
    var faded by remember(session, index) { mutableStateOf(setOf<Int>()) }
    var locked by remember(session, index) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(session, index) { speaker.say(round.prompt) }

    GameScaffold(prompt = round.prompt, onBack = onExit, onReplay = { speaker.say(round.prompt) }) {
        PlayArea {
            round.center?.invoke()
        }
        val rows = if (round.options.size > 3) round.options.chunked(2) else listOf(round.options)
        Column(
            Modifier.fillMaxWidth().heightIn(min = optionSize),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var base = 0
            rows.forEach { row ->
                val start = base
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEachIndexed { j, opt ->
                        val i = start + j
                        val isFaded = i in faded
                        Box(Modifier.alpha(if (isFaded) 0.3f else 1f)) {
                            Tile(opt.label, optionSize, enabled = !locked && !isFaded, onClick = {
                                opt.sayOnTap?.let { speaker.say(it) }
                                if (opt.correct || round.anyAccepted) {
                                    locked = true
                                    scope.launch {
                                        val reply = opt.reply ?: round.reply
                                        if (reply != null) {
                                            if (opt.sayOnTap != null) delay(900)
                                            speaker.say(reply)
                                        }
                                        delay(1600)
                                        index++
                                    }
                                } else {
                                    faded = faded + i
                                }
                            }) { opt.content() }
                        }
                    }
                }
                base += row.size
            }
        }
    }
}
