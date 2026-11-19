package org.littlelantern.session

/** Parent-set limits. Defaults follow the spec: 60 min/day for ages 2 to 5. */
data class SessionSettings(
    val dailyLimitMs: Long = 60 * MINUTE,
    val sessionLengthMs: Long = 15 * MINUTE,
    val restAfterSessionMs: Long = 60 * MINUTE,
    /** Still pause screen between videos before the choices appear. */
    val pauseMs: Long = 30_000,
    /** How many videos the child can choose from. */
    val choices: Int = 3,
    /** Playback colour saturation, 1 = original. */
    val saturation: Float = 0.6f,
    /** Playback volume, 1 = full. */
    val volume: Float = 0.6f,
) {
    companion object {
        const val MINUTE = 60_000L
    }
}

enum class AfterVideo { PAUSE, END_SESSION, END_DAY }

/**
 * Pure session logic, unit tested.
 * Sessions only ever end when a video finishes (natural stopping point), never mid-video.
 */
object SessionRules {

    fun canStartSession(todayWatchedMs: Long, nowMs: Long, restUntilMs: Long, s: SessionSettings): Boolean =
        todayWatchedMs < s.dailyLimitMs && nowMs >= restUntilMs

    fun afterVideo(sessionWatchedMs: Long, todayWatchedMs: Long, s: SessionSettings): AfterVideo = when {
        todayWatchedMs >= s.dailyLimitMs -> AfterVideo.END_DAY
        sessionWatchedMs >= s.sessionLengthMs -> AfterVideo.END_SESSION
        else -> AfterVideo.PAUSE
    }

    /** When Child mode may play again after a session ends. */
    fun restUntil(end: AfterVideo, nowMs: Long, startOfNextDayMs: Long, s: SessionSettings): Long =
        if (end == AfterVideo.END_DAY) startOfNextDayMs else nowMs + s.restAfterSessionMs

    /**
     * The same small set of videos all week, rotating weekly through the library.
     * [weekIndex] is e.g. epochDay / 7.
     */
    fun <T> choicesForWeek(library: List<T>, count: Int, weekIndex: Long): List<T> {
        if (library.size <= count) return library
        val start = ((weekIndex * count) % library.size).toInt()
        return List(count) { library[(start + it) % library.size] }
    }
}
