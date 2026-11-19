package org.littlelantern.check

/** One stretch of the video to measure. */
data class Segment(val startMs: Long, val lengthMs: Long) {
    val endMs: Long get() = startMs + lengthMs
}

object Sampling {
    const val SAMPLES = 6
    const val SAMPLE_MS = 20_000L

    /**
     * [count] stretches of up to [lengthMs], centred on evenly spaced points across the whole video,
     * so a 23 minute video is judged from its start, middle and end, not just the first minutes.
     */
    fun segments(durationMs: Long, count: Int = SAMPLES, lengthMs: Long = SAMPLE_MS): List<Segment> {
        if (durationMs <= 0) return emptyList()
        val len = minOf(lengthMs, durationMs / count).coerceAtLeast(1)
        return List(count) { i ->
            val centre = durationMs * (2 * i + 1) / (2 * count)
            val start = (centre - len / 2).coerceIn(0, durationMs - len)
            Segment(start, len)
        }
    }
}
