package org.littlelantern.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.littlelantern.ui.Palette

/**
 * The 30 in-app games, 10 per age. Basis: "Study" = backed by a published study, "CDC" = matches a CDC
 * milestone for that age, "Design" = our judgment. No game has scores, stars, levels or timers.
 */
class GameInfo(
    val id: String,
    val title: String,
    val age: Int,
    val area: String,
    val basis: String,
    val tileShape: Shape,
    val tileColor: Color,
    /** Colour is the only cue: hidden in colour-blind mode. */
    val colourOnly: Boolean = false,
    val play: @Composable (onExit: () -> Unit) -> Unit,
)

object Catalog {

    val all: List<GameInfo> = listOf(
        // ----- Age 3: one step, 2 to 4 choices, big targets -----
        GameInfo("shape_fit", "Shapes", 3, "Spatial thinking", "Study", Shape.TRIANGLE, Palette.Pink) { ShapeFit(it) },
        GameInfo("puzzle_small", "Puzzle", 3, "Spatial thinking", "Study", Shape.SQUARE, Palette.Sky) { PuzzleGame(it, listOf(2 to 1, 2 to 4)) },
        GameInfo("colour_sort", "Sort", 3, "Maths: sorting", "Study", Shape.CIRCLE, Palette.Orange) { ColourSort(it) },
        GameInfo("how_many", "How many?", 3, "Maths: small numbers", "Study", Shape.CIRCLE, Palette.Blue) { HowMany(it) },
        GameInfo("big_small", "Big or small", 3, "Maths: size", "Design", Shape.SQUARE, Palette.Orange) { BigSmall(it) },
        GameInfo("find_pairs", "Find pairs", 3, "Memory", "Design", Shape.STAR, Palette.Pink) { MemoryGame(it, pairs = 2) },
        GameInfo("feelings_basic", "Feelings", 3, "Social and emotional", "Study", Shape.CIRCLE, Palette.Sky) { FeelingsBasic(it) },
        GameInfo("finger_paint", "Paint", 3, "Creativity, fine motor", "Study", Shape.STAR, Palette.Blue) { FingerPaintGame(it) },
        GameInfo("tap_what_i_say", "Listen and tap", 3, "Language: listening", "Design", Shape.TRIANGLE, Palette.Orange) { TapWhatISay(it) },
        GameInfo("move_animals", "Move like animals", 3, "Physical (off screen)", "Design", Shape.CIRCLE, Palette.Pink) {
            MoveGame(it, listOf("Stomp like an elephant!" to "🐘", "Hop like a bunny!" to "🐰", "Waddle like a penguin!" to "🐧", "Stretch tall like a giraffe!" to "🦒"))
        },

        // ----- Age 4: two steps, rules, numbers to 10 -----
        GameInfo("number_path_10", "Number path", 4, "Maths: number sense", "Study", Shape.SQUARE, Palette.Blue) { NumberPathGame(it, length = 10, spinMax = 2, vertical = false) },
        GameInfo("pattern_ab", "What next?", 4, "Maths: patterns", "Study", Shape.CIRCLE, Palette.Orange) { Patterns(it, abc = false) },
        GameInfo("count_5", "Count", 4, "Maths: counting", "CDC", Shape.SQUARE, Palette.Sky) { CountTapGame(it, rounds = 4, range = 3..5) },
        GameInfo("sort_shape_size", "Sort shapes", 4, "Thinking: categories", "Design", Shape.TRIANGLE, Palette.Pink) { SortShapeSize(it) },
        GameInfo("story_order", "Story order", 4, "Language: stories", "CDC", Shape.SQUARE, Palette.Blue) { StoryOrder(it) },
        GameInfo("rhyme_match", "Rhymes", 4, "Early reading", "Study", Shape.STAR, Palette.Orange) { RhymeMatch(it) },
        GameInfo("find_colour", "Find the colour", 4, "Language: colour words", "CDC", Shape.CIRCLE, Palette.Sky, colourOnly = true) { FindColour(it) },
        GameInfo("build_face", "Build a face", 4, "Body parts, fine motor", "CDC", Shape.CIRCLE, Palette.Pink) { BuildFace(it) },
        GameInfo("feelings_help", "What helps?", 4, "Social and emotional", "Study", Shape.CIRCLE, Palette.Blue) { FeelingsHelp(it) },
        GameInfo("hop_balance", "Hop and balance", 4, "Physical (off screen)", "CDC", Shape.TRIANGLE, Palette.Orange) {
            MoveGame(it, listOf("Stand on one foot!" to "🦩", "Now the other foot!" to "🦩", "Hop on one foot!" to "🐸", "Jump like a frog!" to "🐸"))
        },

        // ----- Age 5: switching rules, numbers to 20, sounds and letters -----
        GameInfo("number_path_20", "Number path 20", 5, "Maths: number sense", "Study", Shape.SQUARE, Palette.Blue) { NumberPathGame(it, length = 20, spinMax = 3, vertical = true) },
        GameInfo("pattern_abc", "Patterns", 5, "Maths: patterns", "Study", Shape.TRIANGLE, Palette.Orange) { Patterns(it, abc = true) },
        GameInfo("switch_sort", "Switch sort", 5, "Self-control, flexible thinking", "Study", Shape.STAR, Palette.Blue) { SwitchSort(it) },
        GameInfo("count_ten", "Count to 10", 5, "Maths: counting", "CDC", Shape.SQUARE, Palette.Sky) { CountTen(it) },
        GameInfo("clap_syllables", "Drum the beats", 5, "Early reading", "Study", Shape.CIRCLE, Palette.Orange) { SyllableGame(it) },
        GameInfo("first_sound", "First sound", 5, "Early reading", "Design", Shape.STAR, Palette.Pink) { FirstSound(it) },
        GameInfo("trace_letters", "Trace letters", 5, "Early writing, fine motor", "Design", Shape.TRIANGLE, Palette.Blue) { TraceLettersGame(it) },
        GameInfo("puzzle_9", "Big puzzle", 5, "Spatial thinking", "Design", Shape.SQUARE, Palette.Pink) { PuzzleGame(it, listOf(3 to 2, 3 to 4, 3 to 9)) },
        GameInfo("sink_float", "Sink or float", 5, "Science: predict and check", "Design", Shape.CIRCLE, Palette.Sky) { SinkFloatGame(it) },
        GameInfo("move_freeze", "Move and freeze", 5, "Physical, self-control", "Design", Shape.STAR, Palette.Orange) { MoveFreezeGame(it) },
    )

    fun byId(id: String): GameInfo? = all.firstOrNull { it.id == id }

    /** Games for the child's age first, then younger ones still switched on. */
    fun visible(age: Int, disabled: Set<String>, colourBlind: Boolean): List<GameInfo> =
        all.filter { it.age <= age && it.id !in disabled && !(colourBlind && it.colourOnly) }
            .sortedByDescending { it.age }

    /** Games the parent can switch: those up to the child's age. */
    fun forParent(age: Int): List<GameInfo> = all.filter { it.age <= age }.sortedByDescending { it.age }
}

// =================== Game definitions ===================

private val shapeColor = mapOf(Shape.CIRCLE to Palette.Blue, Shape.SQUARE to Palette.Orange, Shape.TRIANGLE to Palette.Pink, Shape.STAR to Palette.Sky)

@Composable
private fun ShapeFit(onExit: () -> Unit) = DragGame(onExit) {
    listOf(3, 4).map { n ->
        val shapes = Shape.entries.take(n)
        DragRound(
            prompt = "Put each shape in its place",
            slots = shapes.map { s ->
                DragSlot(
                    id = s.name,
                    accepts = { it.id == s.name },
                    praise = { shapeName(s) },
                    empty = { ShapeView(s, Color.Transparent, 96.dp, dashed = true) },
                    filled = { ShapeView(s, shapeColor.getValue(s), 96.dp) },
                )
            },
            pieces = shapes.shuffled().map { s -> DragPiece(s.name) { ShapeView(s, shapeColor.getValue(s), 72.dp) } },
            layout = { slot ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    shapes.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { row.forEach { s -> slot(s.name, Modifier.size(104.dp)) } }
                    }
                }
            },
        )
    }
}

@Composable
private fun ColourSort(onExit: () -> Unit) {
    val colourBlind = LocalColourBlind.current
    DragGame(onExit) {
        listOf(4, 6).map { n ->
            if (!colourBlind) {
                val bins = listOf(Palette.Blue, Palette.Orange)
                DragRound(
                    prompt = "Put each ball in the same colour basket",
                    slots = bins.map { c ->
                        DragSlot("bin${colourName(c)}", accepts = { it.id.startsWith(colourName(c)) }, capacity = n,
                            praise = { colourName(c) }, empty = { SortBin(0) { ShapeView(Shape.SQUARE, c, 44.dp) } },
                            filled = { SortBin(it.size) { ShapeView(Shape.SQUARE, c, 44.dp) } })
                    },
                    pieces = (0 until n).map { i -> val c = bins[i % 2]; DragPiece("${colourName(c)}$i") { ShapeView(Shape.CIRCLE, c, 64.dp) } }.shuffled(),
                    layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { bins.forEach { slot("bin${colourName(it)}", Modifier) } } },
                )
            } else {
                val bins = listOf(Shape.CIRCLE, Shape.SQUARE)
                DragRound(
                    prompt = "Put each shape in the same shape basket",
                    slots = bins.map { s ->
                        DragSlot("bin${s.name}", accepts = { it.id.startsWith(s.name) }, capacity = n, praise = { shapeName(s) },
                            empty = { SortBin(0) { ShapeView(s, Color.Transparent, 44.dp, dashed = true) } },
                            filled = { SortBin(it.size) { ShapeView(s, Color.Transparent, 44.dp, dashed = true) } })
                    },
                    pieces = (0 until n).map { i -> val s = bins[i % 2]; DragPiece("${s.name}$i") { ShapeView(s, Palette.Blue, 64.dp) } }.shuffled(),
                    layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { bins.forEach { slot("bin${it.name}", Modifier) } } },
                )
            }
        }
    }
}

@Composable
private fun HowMany(onExit: () -> Unit) = ChoiceGame(onExit) {
    List(5) { (1..3).random() }.map { n ->
        ChoiceRound(
            prompt = "How many?",
            reply = numberWords[n],
            center = { Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { repeat(n) { ShapeView(Shape.CIRCLE, Palette.Blue, 70.dp) } } },
            options = (1..3).map { k -> ChoiceOption(numberWords[k], correct = k == n) { Dots(k) } },
        )
    }
}

@Composable
private fun BigSmall(onExit: () -> Unit) = ChoiceGame(onExit, optionSize = 120.dp) {
    listOf("🐟", "🐻", "🚗", "🍎", "🐶", "⭐").pick(5).map { e ->
        val big = listOf(true, false).random()
        val prompt = if (big) "Tap the big one" else "Tap the small one"
        ChoiceRound(
            prompt = prompt,
            reply = if (big) "Yes, the big one!" else "Yes, the small one!",
            center = { Emoji(e, 60.dp) },
            options = listOf(true, false).shuffled().map { isBig ->
                ChoiceOption(if (isBig) "Big" else "Small", correct = isBig == big) { Emoji(e, if (isBig) 92.dp else 38.dp) }
            },
        )
    }
}

private class Scene(val text: String, val emoji: String, val mood: Mood, val who: String)

@Composable
private fun FeelingsBasic(onExit: () -> Unit) = ChoiceGame(onExit, optionSize = 110.dp) {
    listOf(
        Scene("Her ice cream fell.", "🍦", Mood.SAD, "She feels sad."),
        Scene("It's his birthday party!", "🎂", Mood.HAPPY, "He feels happy."),
        Scene("Grandma came to play.", "👵", Mood.HAPPY, "She feels happy."),
        Scene("His tower fell down.", "🧱", Mood.SAD, "He feels sad."),
        Scene("She found her lost teddy!", "🧸", Mood.HAPPY, "She feels happy."),
    ).pick(4).map { s ->
        ChoiceRound(
            prompt = s.text,
            reply = s.who,
            anyAccepted = true,
            center = { Emoji(s.emoji, 130.dp) },
            options = listOf(Mood.HAPPY, Mood.SAD).map { m -> ChoiceOption(m.name.lowercase(), correct = m == s.mood) { FaceView(m, 80.dp) } },
        )
    }
}

private val things = listOf(
    "🎩" to "hat", "⚽" to "ball", "🥤" to "cup", "🧦" to "sock", "🍎" to "apple",
    "🚗" to "car", "🐶" to "dog", "🧸" to "teddy", "🔑" to "key", "👟" to "shoe",
)

@Composable
private fun TapWhatISay(onExit: () -> Unit) = ChoiceGame(onExit, optionSize = 100.dp) {
    List(5) {
        val three = things.pick(3)
        val target = three.random()
        ChoiceRound(
            prompt = "Tap the ${target.second}",
            reply = "Yes, the ${target.second}!",
            options = three.map { (e, name) -> ChoiceOption(name, correct = name == target.second) { Emoji(e, 64.dp) } },
        )
    }
}

private data class Unit2(val shape: Shape, val color: Color)

@Composable
private fun UnitView(u: Unit2, size: androidx.compose.ui.unit.Dp) = ShapeView(u.shape, u.color, size)

@Composable
private fun Patterns(onExit: () -> Unit, abc: Boolean) = ChoiceGame(onExit) {
    List(5) {
        val shapes = Shape.entries.pick(3)
        val colors = Palette.pieces.pick(3)
        val a = Unit2(shapes[0], colors[0])
        val b = Unit2(shapes[1], colors[1])
        val c = Unit2(shapes[2], colors[2])
        val (seq, answer, choices) = if (!abc) {
            Triple(listOf(a, b, a, b, a), b, listOf(a, b))
        } else if (listOf(true, false).random()) {
            Triple(listOf(a, a, b, b, a, a, b), b, listOf(a, b, c))
        } else {
            Triple(listOf(a, b, c, a, b), c, listOf(a, b, c))
        }
        ChoiceRound(
            prompt = "What comes next?",
            center = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    seq.forEach { UnitView(it, if (seq.size > 5) 34.dp else 42.dp) }
                    ShapeView(Shape.SQUARE, Color.Transparent, if (seq.size > 5) 34.dp else 42.dp, dashed = true)
                }
            },
            options = choices.shuffled().map { u ->
                ChoiceOption("${colourName(u.color)} ${shapeName(u.shape)}", correct = u == answer) { UnitView(u, 56.dp) }
            },
        )
    }
}

@Composable
private fun SortShapeSize(onExit: () -> Unit) = DragGame(onExit) {
    val shapeBins = listOf(Shape.CIRCLE, Shape.SQUARE)
    val round1 = DragRound(
        prompt = "Put each shape in its basket",
        slots = shapeBins.map { s ->
            DragSlot("bin${s.name}", accepts = { it.id.startsWith(s.name) }, capacity = 6, praise = { shapeName(s) },
                empty = { SortBin(0) { ShapeView(s, Color.Transparent, 44.dp, dashed = true) } },
                filled = { SortBin(it.size) { ShapeView(s, Color.Transparent, 44.dp, dashed = true) } })
        },
        pieces = (0 until 4).map { i -> val s = shapeBins[i % 2]; DragPiece("${s.name}$i") { ShapeView(s, Palette.pieces[i], 60.dp) } }.shuffled(),
        layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { shapeBins.forEach { slot("bin${it.name}", Modifier) } } },
    )
    val sizes = listOf("big", "small")
    val round2 = DragRound(
        prompt = "Big ones here, small ones there",
        slots = sizes.map { z ->
            DragSlot("bin$z", accepts = { it.id.startsWith(z) }, capacity = 6, praise = { z },
                empty = { SortBin(0) { ShapeView(Shape.CIRCLE, Color.Transparent, if (z == "big") 50.dp else 24.dp, dashed = true) } },
                filled = { SortBin(it.size) { ShapeView(Shape.CIRCLE, Color.Transparent, if (z == "big") 50.dp else 24.dp, dashed = true) } })
        },
        pieces = (0 until 4).map { i -> val z = sizes[i % 2]; DragPiece("$z$i") { ShapeView(Shape.CIRCLE, Palette.pieces[i], if (z == "big") 68.dp else 34.dp) } }.shuffled(),
        layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { sizes.forEach { slot("bin$it", Modifier) } } },
    )
    listOf(round1, round2)
}

private val stories = listOf(
    listOf("🥚", "🐣", "🐥"),
    listOf("🌰", "🌱", "🌳"),
    listOf("☁️", "🌧️", "🌈"),
)

@Composable
private fun StoryOrder(onExit: () -> Unit) = DragGame(onExit) {
    stories.shuffled().take(2).map { story ->
        DragRound(
            prompt = "Put the pictures in order",
            slots = (0..2).map { i ->
                DragSlot("s$i", accepts = { it.id == "c$i" },
                    empty = { Column(horizontalAlignment = Alignment.CenterHorizontally) { EmptySlot(84.dp); Dots(i + 1, 10.dp) } },
                    filled = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { Emoji(story[i], 64.dp) }; Dots(i + 1, 10.dp) } })
            },
            pieces = (0..2).shuffled().map { i -> DragPiece("c$i") { Emoji(story[i], 60.dp) } },
            layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { (0..2).forEach { slot("s$it", Modifier) } } },
        )
    }
}

private class Rhyme(val word: Pair<String, String>, val rhyme: Pair<String, String>, val others: List<Pair<String, String>>)

private val rhymes = listOf(
    Rhyme("🐱" to "cat", "🎩" to "hat", listOf("🐶" to "dog", "☀️" to "sun")),
    Rhyme("🐝" to "bee", "🌳" to "tree", listOf("🚗" to "car", "🐟" to "fish")),
    Rhyme("🌙" to "moon", "🥄" to "spoon", listOf("⚽" to "ball", "👟" to "shoe")),
    Rhyme("🐍" to "snake", "🎂" to "cake", listOf("🎩" to "hat", "🚌" to "bus")),
    Rhyme("🦊" to "fox", "📦" to "box", listOf("🥤" to "cup", "🛏️" to "bed")),
    Rhyme("🐐" to "goat", "⛵" to "boat", listOf("🔑" to "key", "🧦" to "sock")),
)

@Composable
private fun RhymeMatch(onExit: () -> Unit) = ChoiceGame(onExit) {
    rhymes.pick(4).map { r ->
        ChoiceRound(
            prompt = "What rhymes with ${r.word.second}?",
            reply = "Yes! ${r.word.second}, ${r.rhyme.second}.",
            center = { Emoji(r.word.first, 120.dp) },
            options = (r.others + r.rhyme).shuffled().map { (e, w) ->
                ChoiceOption(w, correct = w == r.rhyme.second, sayOnTap = w) { Emoji(e, 60.dp) }
            },
        )
    }
}

@Composable
private fun FindColour(onExit: () -> Unit) = ChoiceGame(onExit, optionSize = 90.dp) {
    List(4) {
        val target = Palette.pieces.random()
        ChoiceRound(
            prompt = "Find the ${colourName(target)} balloon",
            reply = "Yes, ${colourName(target)}!",
            options = Palette.pieces.shuffled().map { c -> ChoiceOption(colourName(c), correct = c == target) { Balloon(c, 76.dp) } },
        )
    }
}

@Composable
private fun BuildFace(onExit: () -> Unit) = DragGame(onExit) {
    listOf(listOf(FacePart.NOSE, FacePart.MOUTH), listOf(FacePart.EYES, FacePart.NOSE, FacePart.MOUTH)).map { parts ->
        DragRound(
            prompt = "Build the face",
            slots = parts.map { p ->
                DragSlot(p.name, accepts = { it.id == p.name }, praise = { p.name.lowercase() },
                    empty = { EmptySlot(if (p == FacePart.MOUTH) 90.dp else 64.dp) },
                    filled = { FacePartView(p, if (p == FacePart.MOUTH) 90.dp else 64.dp) })
            },
            pieces = parts.shuffled().map { p -> DragPiece(p.name) { FacePartView(p, 64.dp) } },
            layout = { slot ->
                Box(contentAlignment = Alignment.TopCenter) {
                    FaceBase(280.dp, showEyes = FacePart.EYES !in parts)
                    if (FacePart.EYES in parts) slot(FacePart.EYES.name, Modifier.offset(y = 70.dp))
                    slot(FacePart.NOSE.name, Modifier.offset(y = 135.dp))
                    slot(FacePart.MOUTH.name, Modifier.offset(y = 195.dp))
                }
            },
        )
    }
}

@Composable
private fun FeelingsHelp(onExit: () -> Unit) = ChoiceGame(onExit, optionSize = 96.dp) {
    val helps = listOf(
        Triple("🌬️", "a big breath", "Taking a big breath can help."),
        Triple("🤗", "a hug", "A hug can help."),
        Triple("🛋️", "a quiet corner", "A quiet corner can help."),
    )
    listOf(
        Triple("He feels cross. What could help?", Mood.CROSS, "cross"),
        Triple("She feels scared. What could help?", Mood.SCARED, "scared"),
        Triple("He feels sad. What could help?", Mood.SAD, "sad"),
    ).shuffled().map { (prompt, mood, _) ->
        ChoiceRound(
            prompt = prompt,
            anyAccepted = true,
            center = { FaceView(mood, 150.dp) },
            options = helps.map { (e, name, reply) -> ChoiceOption(name, correct = true, sayOnTap = null, reply = reply) { Emoji(e, 56.dp) } },
        )
    }
}

@Composable
private fun SwitchSort(onExit: () -> Unit) = DragGame(onExit) {
    // Card-sort task: bins show an orange star and a blue circle; cards are blue stars and orange circles.
    val bins = listOf(Unit2(Shape.STAR, Palette.Orange), Unit2(Shape.CIRCLE, Palette.Blue))
    val cards = listOf(Unit2(Shape.STAR, Palette.Blue), Unit2(Shape.CIRCLE, Palette.Orange))
    listOf(true, false).map { byColour ->
        DragRound(
            prompt = if (byColour) "Sort by colour" else "Now sort by shape",
            slots = bins.mapIndexed { i, b ->
                DragSlot("bin$i", capacity = 4, accepts = { piece ->
                    val card = cards[piece.id.substringAfter("card").substringBefore("_").toInt()]
                    if (byColour) card.color == b.color else card.shape == b.shape
                }, empty = { SortBin(0) { UnitView(b, 48.dp) } }, filled = { SortBin(it.size) { UnitView(b, 48.dp) } })
            },
            pieces = (0 until 4).map { i -> val k = i % 2; DragPiece("card${k}_$i") { UnitView(cards[k], 60.dp) } }.shuffled(),
            layout = { slot -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { slot("bin0", Modifier); slot("bin1", Modifier) } },
        )
    }
}

@Composable
private fun CountTen(onExit: () -> Unit) = ChoiceGame(onExit) {
    List(5) {
        val n = (1..10).random()
        val low = (n - 1).coerceIn(1, 8)
        ChoiceRound(
            prompt = "How many dots?",
            reply = "Yes, ${numberWords[n]}!",
            center = { TenFrame(n) },
            options = (low..low + 2).shuffled().map { k ->
                ChoiceOption(numberWords[k], correct = k == n, sayOnTap = numberWords[k]) {
                    Text("$k", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                }
            },
        )
    }
}

private class Sound(val sound: String, val target: Pair<String, String>, val match: Pair<String, String>, val others: List<Pair<String, String>>)

private val sounds = listOf(
    Sound("mmm", "🌙" to "moon", "🐭" to "mouse", listOf("☀️" to "sun", "🚗" to "car")),
    Sound("sss", "☀️" to "sun", "🧦" to "sock", listOf("🌙" to "moon", "🐟" to "fish")),
    Sound("fff", "🐟" to "fish", "🦊" to "fox", listOf("🐱" to "cat", "🚌" to "bus")),
    Sound("lll", "🦁" to "lion", "🍃" to "leaf", listOf("🐶" to "dog", "🎩" to "hat")),
)

@Composable
private fun FirstSound(onExit: () -> Unit) = ChoiceGame(onExit) {
    sounds.shuffled().map { s ->
        ChoiceRound(
            prompt = "Which starts like ${s.sound}, ${s.target.second}?",
            reply = "Yes! ${s.target.second}, ${s.match.second}.",
            center = { Emoji(s.target.first, 120.dp) },
            options = (s.others + s.match).shuffled().map { (e, w) ->
                ChoiceOption(w, correct = w == s.match.second, sayOnTap = w) { Emoji(e, 60.dp) }
            },
        )
    }
}
