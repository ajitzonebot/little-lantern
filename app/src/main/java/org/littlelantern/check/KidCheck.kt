package org.littlelantern.check

enum class Level { GOOD, CAUTION, HIGH }

enum class Verdict(val label: String) {
    CALM("Calm"),
    CHECK("Check before approving"),
    NOT_RECOMMENDED("Not recommended"),
}

/** Raw measurements of one video. Null means "could not measure". */
data class Metrics(
    val cutsPerMinute: Double?,
    val flashesPerMinute: Double?,
    val meanSaturation: Double?,
    val loudSpikesPerMinute: Double?,
    val durationMs: Long,
    /** Words in the title or description that hint at fantastical or attention-bait content. */
    val fantasyHints: List<String>,
    /** The parent ticked "has fantastical or impossible events". */
    val parentMarkedFantastical: Boolean,
)

data class CheckItem(val name: String, val value: String, val level: Level, val why: String)

data class KidReport(val items: List<CheckItem>, val verdict: Verdict)

/**
 * How suitable a video is for a 2 to 5 year old, from the factors in docs/SPEC.md.
 * Thresholds are this project's defaults, not research-set numbers; the parent makes the final call.
 */
object KidCheck {

    private val HINT_WORDS = listOf(
        "surprise", "egg", "eggs", "compilation", "superhero", "superheroes", "magic", "magical",
        "transform", "transformation", "challenge", "prank", "slime", "unboxing", "monster",
        "crazy", "fail", "fails", "nonstop", "non-stop", "1 hour", "2 hours", "3 hours", "+ more", "+more",
    )

    fun fantasyHints(title: String, description: String): List<String> {
        val text = " " + "$title $description".lowercase().replace(Regex("[^a-z0-9+ -]"), " ") + " "
        return HINT_WORDS.filter { word ->
            if (word.startsWith("+")) text.contains(word) else text.contains(" $word ")
        }.distinct()
    }

    fun evaluate(m: Metrics): KidReport {
        val items = buildList {
            add(
                rate(
                    "Pace (scene cuts per minute)", m.cutsPerMinute, good = 8.0, caution = 15.0, format = "%.0f",
                    why = "Fast-paced shows lowered 4 year olds' self-control right after 9 minutes (Lillard 2011). Evidence on pace alone is mixed.",
                ),
            )
            add(
                rate(
                    "Big brightness jumps per minute", m.flashesPerMinute, good = 1.0, caution = 4.0, format = "%.1f",
                    why = "Sudden flashes are a strong attention grabber; overstimulation harmed young mice (Christakis 2012, animal study).",
                ),
            )
            add(
                rate(
                    "Colour intensity (0 to 1)", m.meanSaturation, good = 0.40, caution = 0.55, format = "%.2f",
                    why = "No study shows bright colour harms; busy visuals distracted children in classrooms (Fisher 2014). Low weight.",
                ),
            )
            add(
                rate(
                    "Sudden loud sounds per minute", m.loudSpikesPerMinute, good = 1.0, caution = 3.0, format = "%.1f",
                    why = "Loud spikes pull attention back to the screen; part of the overstimulation pattern.",
                ),
            )
            val minutes = m.durationMs / 60_000.0
            add(
                CheckItem(
                    "Length", "%.0f min".format(minutes),
                    when {
                        minutes <= 10 -> Level.GOOD
                        minutes <= 20 -> Level.CAUTION
                        else -> Level.HIGH
                    },
                    "Short videos give natural stopping points; long ones fight the 15 minute session.",
                ),
            )
            add(
                CheckItem(
                    "Fantastical content",
                    when {
                        m.parentMarkedFantastical -> "Marked by parent"
                        m.fantasyHints.isNotEmpty() -> "Title hints: " + m.fantasyHints.joinToString()
                        else -> "None found in title"
                    },
                    when {
                        m.parentMarkedFantastical -> Level.HIGH
                        m.fantasyHints.isNotEmpty() -> Level.CAUTION
                        else -> Level.GOOD
                    },
                    "Fantastical events lowered self-control even in an educational show (Lillard 2015). Watch a minute to judge.",
                ),
            )
        }
        val highs = items.count { it.level == Level.HIGH }
        val cautions = items.count { it.level == Level.CAUTION }
        val verdict = when {
            highs >= 2 -> Verdict.NOT_RECOMMENDED
            highs == 1 || cautions >= 2 -> Verdict.CHECK
            else -> Verdict.CALM
        }
        return KidReport(items, verdict)
    }

    private fun rate(
        name: String,
        value: Double?,
        good: Double,
        caution: Double,
        format: String,
        why: String,
    ): CheckItem {
        if (value == null) return CheckItem(name, "Not measured", Level.CAUTION, why)
        val level = when {
            value <= good -> Level.GOOD
            value <= caution -> Level.CAUTION
            else -> Level.HIGH
        }
        return CheckItem(name, format.format(value), level, why)
    }
}
