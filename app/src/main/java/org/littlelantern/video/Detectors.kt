package org.littlelantern.video

import kotlin.math.abs

/**
 * Pure measures used by the stream check. Kept free of Android types so they can be unit tested.
 */

/** Counts hard cuts from per-frame luma histograms (each normalised to sum 1). */
class CutCounter(
    private val threshold: Double = 0.55,
    private val minGapSeconds: Double = 0.3,
) {
    private var previous: DoubleArray? = null
    private var lastCutAt = -10.0
    var cuts = 0
        private set

    fun onFrame(histogram: DoubleArray, timeSeconds: Double) {
        val prev = previous
        previous = histogram
        if (prev == null) return
        var distance = 0.0
        for (i in histogram.indices) distance += abs(histogram[i] - prev[i])
        if (distance > threshold && timeSeconds - lastCutAt >= minGapSeconds) {
            cuts++
            lastCutAt = timeSeconds
        }
    }

    fun perMinute(durationSeconds: Double): Double? =
        if (durationSeconds <= 0) null else cuts / (durationSeconds / 60.0)
}

/**
 * Flash counter after the broadcast / WCAG idea: a flash is a pair of opposing changes in
 * relative luminance of at least [minChange], where the darker state is below [darkBelow].
 * Uses the whole-frame average, which is a simplification of the area-based standard.
 * Reports the most flashes seen in any one-second window.
 */
class FlashCounter(
    private val minChange: Double = 0.10,
    private val darkBelow: Double = 0.80,
) {
    private var reference: Double? = null
    private var lastDirection = 0
    private val transitions = ArrayDeque<Double>()
    var maxFlashesPerSecond = 0
        private set

    fun onFrame(value: Double, timeSeconds: Double) {
        val ref = reference
        if (ref == null) {
            reference = value
            return
        }
        val delta = value - ref
        if (abs(delta) >= minChange && minOf(value, ref) < darkBelow) {
            val direction = if (delta > 0) 1 else -1
            if (direction != lastDirection) {
                transitions.addLast(timeSeconds)
                lastDirection = direction
            }
            reference = value
        } else if (abs(delta) < minChange / 2) {
            // Drift slowly with gradual scene changes so they are not counted.
            reference = ref * 0.9 + value * 0.1
        }
        while (transitions.isNotEmpty() && timeSeconds - transitions.first() > 1.0) transitions.removeFirst()
        // Two opposing transitions make one flash.
        maxFlashesPerSecond = maxOf(maxFlashesPerSecond, transitions.size / 2)
    }
}

/** Sudden loudness jumps: a 100 ms window at least [jumpDb] louder than the median of the previous 3 s. */
class LoudnessJumpCounter(
    private val jumpDb: Double = 15.0,
    private val floorDb: Double = -30.0,
    private val windowsPerHistory: Int = 30,
    private val refractoryWindows: Int = 20,
) {
    private val history = ArrayDeque<Double>()
    private var cooldown = 0
    var jumps = 0
        private set

    fun onWindow(db: Double) {
        if (cooldown > 0) cooldown--
        if (history.size >= windowsPerHistory) {
            val median = history.sorted()[history.size / 2]
            if (cooldown == 0 && db >= floorDb && db - median >= jumpDb) {
                jumps++
                cooldown = refractoryWindows
            }
            history.removeFirst()
        }
        history.addLast(db)
    }
}

/** Words in a title or description that often go with manipulative kids' videos. A heuristic, not validated. */
object KeywordScan {
    private val words = listOf(
        "subscribe", "unboxing", "surprise", "giveaway", "challenge", "#ad", "sponsored",
        "link in", "http://", "https://", "merch", "shop now",
    )

    fun hits(text: String): List<String> {
        val lower = text.lowercase()
        return words.filter { lower.contains(it) }
    }
}
