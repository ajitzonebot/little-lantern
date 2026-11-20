package org.littlelantern.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class DragPiece(val id: String, val content: @Composable () -> Unit)

class DragSlot(
    val id: String,
    val accepts: (DragPiece) -> Boolean,
    val capacity: Int = 1,
    /** Said when a piece is accepted here. */
    val praise: (DragPiece) -> String? = { null },
    val empty: @Composable BoxScope.() -> Unit,
    val filled: @Composable BoxScope.(List<DragPiece>) -> Unit,
)

class DragRound(
    val prompt: String,
    val slots: List<DragSlot>,
    val pieces: List<DragPiece>,
    /** Lays out the slots; call `slot(id, modifier)` for each. */
    val layout: @Composable BoxScope.(slot: @Composable (String, Modifier) -> Unit) -> Unit,
)

/**
 * Drag each piece to where it belongs. A wrong drop slides back. When every piece is placed the next
 * round starts; after the last round, the Done panel.
 */
@Composable
fun DragGame(onExit: () -> Unit, pieceSize: Dp = 76.dp, makeRounds: () -> List<DragRound>) {
    val speaker = LocalSpeaker.current
    var session by remember { mutableIntStateOf(0) }
    val rounds = remember(session) { makeRounds() }
    var index by remember(session) { mutableIntStateOf(0) }
    if (index >= rounds.size) {
        DonePanel(onAgain = { session++ }, onBack = onExit)
        return
    }
    val round = rounds[index]
    val drag = remember(session, index) { DragState() }
    var placed by remember(session, index) { mutableStateOf(mapOf<String, List<DragPiece>>()) }
    val remaining = round.pieces.filter { p -> placed.values.none { list -> list.any { it.id == p.id } } }

    LaunchedEffect(session, index) { speaker.say(round.prompt) }
    LaunchedEffect(remaining.isEmpty(), session, index) {
        if (remaining.isEmpty()) {
            delay(1500)
            index++
        }
    }

    GameScaffold(prompt = round.prompt, onBack = onExit, onReplay = { speaker.say(round.prompt) }) {
        PlayArea {
            round.layout(this) { id, modifier ->
                val slot = round.slots.first { it.id == id }
                val inSlot = placed[id].orEmpty()
                DropTarget(id, drag, modifier) {
                    if (inSlot.isEmpty()) slot.empty(this) else slot.filled(this, inSlot)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(min = pieceSize),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            remaining.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { piece ->
                        Draggable(piece.id, drag, onDrop = { target ->
                            val slot = round.slots.firstOrNull { it.id == target } ?: return@Draggable false
                            val current = placed[slot.id].orEmpty()
                            if (current.size < slot.capacity && slot.accepts(piece)) {
                                placed = placed + (slot.id to current + piece)
                                slot.praise(piece)?.let { speaker.say(it) }
                                true
                            } else {
                                false
                            }
                        }) {
                            Box(contentAlignment = Alignment.Center) { piece.content() }
                        }
                    }
                }
            }
        }
    }
}
