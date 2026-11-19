package org.littlelantern.gate

/**
 * Detects the parent's secret gesture: [requiredPresses] volume-key presses within [windowMillis].
 * Pure Kotlin so it can be unit tested.
 */
class PressSequenceDetector(
    private val requiredPresses: Int = 5,
    private val windowMillis: Long = 3_000,
) {
    private val presses = ArrayDeque<Long>()

    /** Records a press at [nowMillis]. Returns true when the sequence is complete. */
    fun onPress(nowMillis: Long): Boolean {
        presses.addLast(nowMillis)
        while (presses.isNotEmpty() && nowMillis - presses.first() > windowMillis) {
            presses.removeFirst()
        }
        if (presses.size >= requiredPresses) {
            presses.clear()
            return true
        }
        return false
    }
}
