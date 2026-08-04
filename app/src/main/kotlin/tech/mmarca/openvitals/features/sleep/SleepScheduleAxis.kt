package tech.mmarca.openvitals.features.sleep

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Pure clock-axis math for the week/month sleep schedule chart. Times are expressed in "anchored
 * minutes": minutes elapsed since the sleep-window start hour, so a normal night stays contiguous
 * on the axis even though it crosses midnight.
 */
internal object SleepScheduleAxis {

    const val MINUTES_PER_DAY = 24 * 60

    /** Nights at or beyond this in-bed span are implausible and excluded from the axis range. */
    private const val MAX_PLAUSIBLE_NIGHT_MINUTES = 16 * 60

    /** Spans above this use 2-hour ticks instead of hourly ones. */
    private const val WIDE_SPAN_MINUTES = 8 * 60

    /** Minutes since [anchorMinute] for [instant]'s local wall-clock time, in [0, 1440). */
    fun anchoredMinutes(instant: Instant, zone: ZoneId, anchorMinute: Int): Double {
        val time = instant.atZone(zone).toLocalTime()
        val minuteOfDay = time.hour * 60 + time.minute + time.second / 60.0
        return ((minuteOfDay - anchorMinute) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    /**
     * Anchored minutes for [value] measured from this night's [start], so end/stage times that
     * wrap past the anchor stay monotonically after bedtime.
     */
    fun normalizedEndMinutes(start: Instant, value: Instant, zone: ZoneId, anchorMinute: Int): Double {
        val startMinute = anchoredMinutes(start, zone, anchorMinute)
        val valueMinute = anchoredMinutes(value, zone, anchorMinute)
        return if (valueMinute < startMinute) valueMinute + MINUTES_PER_DAY else valueMinute
    }

    /** Minutes since [anchorMinute] for a clock minute-of-day, in [0, 1440). */
    fun anchoredClockMinute(minuteOfDay: Int, anchorMinute: Int): Double =
        (((minuteOfDay - anchorMinute) + MINUTES_PER_DAY) % MINUTES_PER_DAY).toDouble()

    /** Wall-clock time for an anchored minute, for tick and marker labels. */
    fun clockTime(anchoredMinute: Int, anchorMinute: Int): LocalTime {
        val minuteOfDay =
            ((anchorMinute + anchoredMinute) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    }

    /**
     * Axis range over the plausible nights, padded out to whole hours. Nights spanning 16 h or
     * more are skipped from the range computation only (they are still drawn, clipped). Null when
     * no plausible night remains, in which case callers fall back to the plain bar chart.
     */
    fun range(days: List<SleepScheduleDay>, zone: ZoneId, anchorMinute: Int): Range? {
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        days.forEach { day ->
            val start = day.inBedStart ?: return@forEach
            val end = day.inBedEnd ?: return@forEach
            val startMinute = anchoredMinutes(start, zone, anchorMinute)
            val endMinute = normalizedEndMinutes(start, end, zone, anchorMinute)
            if (endMinute <= startMinute) return@forEach
            if (endMinute - startMinute >= MAX_PLAUSIBLE_NIGHT_MINUTES) return@forEach
            if (startMinute < min) min = startMinute
            if (endMinute > max) max = endMinute
        }
        if (min == Double.MAX_VALUE || max <= min) return null
        return Range(min = floor(min / 60.0) * 60.0, max = ceil(max / 60.0) * 60.0)
    }

    /** Vertical axis range in anchored minutes, already padded to whole-hour bounds. */
    data class Range(val min: Double, val max: Double) {
        val span: Double get() = (max - min).coerceAtLeast(1.0)

        /** Tick positions in anchored minutes: hourly, or every 2 h once the span exceeds 8 h. */
        fun tickMinutes(): List<Int> {
            val step = if (span > WIDE_SPAN_MINUTES) 120 else 60
            val first = (ceil(min / step) * step).toInt()
            val last = (floor(max / step) * step).toInt()
            if (last < first) return listOf(min.toInt())
            return (first..last step step).toList()
        }
    }
}
