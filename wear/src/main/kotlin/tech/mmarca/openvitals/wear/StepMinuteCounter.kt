package tech.mmarca.openvitals.wear

import java.util.TreeMap
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Turns the step counter's total since boot into steps per clock minute.
 * The difference between two readings is spread over the minutes between
 * them, in proportion to time, but at most over the [MAX_SPREAD_MILLIS]
 * before the newer one: a gap of days (the recorder stopped while another
 * app kept the counter running) would otherwise put steps into every night.
 * Of a longer gap only that window's share is kept, at the gap's average
 * pace; the rest has no known minute and is dropped.
 *
 * Pure, so it is testable off the watch. [StepRecorder] is the Android side.
 */
class StepMinuteCounter(
    /** The last reading on this boot, or null when there is none to diff against. */
    last: Reading?,
    /** The newest stored minute, so a restart inside a minute adds to it instead of replacing it. */
    latest: WearLinkProtocol.StepMinute?,
) {
    /** A total of the step counter and when it was read. */
    data class Reading(val epochMillis: Long, val total: Long)

    var last: Reading? = last
        private set

    private var newest: WearLinkProtocol.StepMinute? = latest

    /** Minute start to steps, for the rows changed since the last [drain]. */
    private val changed = TreeMap<Long, Int>()

    /**
     * Takes a reading. The first one, and one below the last, only set the
     * baseline. A time before the last reading or the newest minute counts
     * as that time: a clock set back must not overwrite a stored minute.
     */
    fun onTotal(epochMillis: Long, total: Long) {
        val previous = last
        val floor = maxOf(previous?.epochMillis ?: Long.MIN_VALUE, newest?.epochMillis ?: Long.MIN_VALUE)
        val at = maxOf(epochMillis, floor)
        last = Reading(at, total)
        if (previous == null || total <= previous.total) return
        spread(start = maxOf(previous.epochMillis, newest?.epochMillis ?: Long.MIN_VALUE), to = at, total = total - previous.total)
    }

    /** The rows changed since the last call, oldest first. */
    fun drain(): List<WearLinkProtocol.StepMinute> =
        changed.map { (start, steps) -> WearLinkProtocol.StepMinute(start, steps) }.also { changed.clear() }

    private fun spread(start: Long, to: Long, total: Long) {
        if (to - start <= 0L) {
            add(minuteOf(to), total)
            return
        }
        val from = maxOf(start, to - MAX_SPREAD_MILLIS)
        val span = to - from
        val steps = total * span / (to - start)
        // Rounded on the running sum, so the parts add up to [steps].
        var given = 0L
        var minute = minuteOf(from)
        while (minute <= to) {
            val covered = minOf(minute + MINUTE_MILLIS, to) - from
            val upTo = steps * covered / span
            if (upTo > given) add(minute, upTo - given)
            given = upTo
            minute += MINUTE_MILLIS
        }
    }

    /** More than a sprint in one minute is not possible; the excess has no known minute and is dropped. */
    private fun add(minute: Long, steps: Long) {
        val before = newest?.takeIf { it.epochMillis == minute }?.steps ?: 0
        val total = (before + steps).coerceAtMost(WearLinkProtocol.MAX_STEPS_PER_MINUTE.toLong()).toInt()
        newest = WearLinkProtocol.StepMinute(minute, total)
        changed[minute] = total
    }

    private fun minuteOf(epochMillis: Long): Long = epochMillis - Math.floorMod(epochMillis, MINUTE_MILLIS)

    companion object {
        private const val MINUTE_MILLIS = 60_000L

        /** The widest window a difference is spread over. App heuristic, no published source. */
        const val MAX_SPREAD_MILLIS = 60 * MINUTE_MILLIS
    }
}
