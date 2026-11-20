package org.littlelantern.session

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * The 60-minute day and the progressive calming curve.
 * Minutes 0 to 15: normal. 15 to 60: colour, brightness, volume and choices ease down. 60: the day ends.
 * These are design choices from the spec, not tested interventions.
 */
object SessionMath {
    const val LIMIT_SECONDS = 60 * 60
    private const val CALM_START_MIN = 15f
    private const val CALM_SPAN_MIN = 45f

    /** 0 = normal, 1 = fully calmed. */
    fun calmLevel(usedSeconds: Int): Float =
        ((usedSeconds / 60f - CALM_START_MIN) / CALM_SPAN_MIN).coerceIn(0f, 1f)

    fun saturation(level: Float): Float = 1f - level

    /** Multiplier on the system brightness: 1.0 down to 0.65. */
    fun brightness(level: Float): Float = 1f - 0.35f * level

    /** Multiplier on the parent's volume cap: 1.0 down to 0.5. */
    fun volume(level: Float): Float = 1f - 0.5f * level

    /** How many video choices the home screen shows; null means all. */
    fun choicesShown(level: Float): Int? = when {
        level < 1f / 3f -> null
        level < 2f / 3f -> 4
        else -> 2
    }

    fun remainingSeconds(usedSeconds: Int): Int = (LIMIT_SECONDS - usedSeconds).coerceAtLeast(0)

    fun isOver(usedSeconds: Int): Boolean = usedSeconds >= LIMIT_SECONDS

    /** A video may start only if it ends before the hour is up. */
    fun fits(videoSeconds: Long, usedSeconds: Int): Boolean = videoSeconds <= remainingSeconds(usedSeconds)

    /** Age band 3, 4 or 5 from a birth month. Under 3 uses the age-3 games; over 5 uses age 5. */
    fun ageBand(birth: YearMonth?, today: LocalDate = LocalDate.now()): Int {
        if (birth == null) return 3
        val years = ChronoUnit.YEARS.between(birth.atDay(1), today).toInt()
        return years.coerceIn(3, 5)
    }
}
