package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Turns the step counter's total since boot into steps per clock minute.
 * The difference between two readings goes to the minute of the later one.
 *
 * Pure, so it is testable off the watch. [StepRecorder] is the Android side.
 */
class StepMinuteCounter(
    /** The total last seen on this boot, or null when there is none to diff against. */
    lastTotal: Long?,
    /** The newest stored minute, so a restart inside a minute adds to it instead of replacing it. */
    latest: WearLinkProtocol.StepMinute?,
) {
    var lastTotal: Long? = lastTotal
        private set

    private var minute: WearLinkProtocol.StepMinute? = latest

    /**
     * The minute's row to store after the counter read [total] at
     * [epochMillis]. Null when nothing was walked. The first reading, and one
     * below the last, only set the baseline.
     */
    fun onTotal(epochMillis: Long, total: Long): WearLinkProtocol.StepMinute? {
        val last = lastTotal
        lastTotal = total
        if (last == null || total <= last) return null
        val start = epochMillis - Math.floorMod(epochMillis, MINUTE_MILLIS)
        val before = minute?.takeIf { it.epochMillis == start }?.steps ?: 0
        // More than a sprint: steps counted while nobody listened. The excess has no known minute.
        val steps = (before + (total - last)).coerceAtMost(WearLinkProtocol.MAX_STEPS_PER_MINUTE.toLong()).toInt()
        return WearLinkProtocol.StepMinute(start, steps).also { minute = it }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
