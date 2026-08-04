package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The x axes a time chart can sit on, and the label rows that describe them.
 *
 * These exist because the same twenty lines kept being written per card, and most
 * copies were wrong the same way: each intraday card scaled its x positions by the
 * time ELAPSED so far — so on a chart opened at 12:49 a 09:29 reading landed at 74%
 * of the width — and then drew a fixed `00:00 / 06:00 / 12:00 / 18:00` row
 * underneath. The chart's only job is to say WHEN, and it said the wrong hour. So
 * the rule lives here once: **the x axis is the whole day (or the whole session),
 * always.** Today's series simply stops at "now" instead of stretching to the right
 * edge, which is honest — the rest of the day has not happened.
 */

private const val MinutesPerDay = 1440f

/**
 * The five evenly-spaced labels under a day chart, for the slice of the day on show.
 *
 * Evenly spaced across the PLOT, so each one says what time it is at that point —
 * which at full zoom is `00:00 / 06:00 / 12:00 / 18:00 / 24:00`, exactly the row the
 * cards always drew, and zoomed in is the hours actually under the plot. A row that
 * still said `00:00 … 24:00` over a plot showing half past seven to nine would be
 * the elapsed-scaling bug back again.
 */
fun dayAxisLabelsFor(viewport: ChartViewport = ChartViewport.Full): List<String> =
    (0..4).map { tick ->
        hhmm(viewport.dataFraction(tick / 4f) * MinutesPerDay)
    }

private fun hhmm(minutesIntoDay: Float): String {
    val total = minutesIntoDay.roundToLong().coerceIn(0L, MinutesPerDay.toLong())
    val hours = total / 60
    val minutes = total % 60
    return "%02d:%02d".format(hours, minutes)
}

/**
 * The `00:00 … 24:00` label row under an intraday chart. Wrap in
 * [ChartXAxisWithYAxis] when the plot above has a y-axis gutter — a row that starts
 * at the card's edge does not describe a plot that starts 64dp in.
 */
@Composable
fun DayAxisLabels(
    viewport: ChartViewport = ChartViewport.Full,
    modifier: Modifier = Modifier,
) {
    AxisLabelRow(labels = dayAxisLabelsFor(viewport), modifier = modifier)
}

/** Where [time] sits across `[start, end]`, in `0..1`. */
fun axisFractionOf(start: Instant, end: Instant, time: Instant): Float {
    val spanMs = Duration.between(start, end).toMillis().coerceAtLeast(1L)
    val elapsed = Duration.between(start, time).toMillis().coerceIn(0L, spanMs)
    return elapsed.toFloat() / spanMs
}

/**
 * Whether the day `[dayStart, dayEnd)` is the one [now] falls in.
 *
 * Judged against the clock it is handed rather than the wall clock, so a chart
 * (and a test) can say which day it is looking at.
 */
fun isDayToday(dayStart: Instant, dayEnd: Instant, now: Instant): Boolean =
    !now.isBefore(dayStart) && now.isBefore(dayEnd)

/**
 * How much of the day a series may claim: the now-fraction on today, and the whole
 * width on any other day.
 *
 * Today's line stops at "now" — held out to the right edge at two in the afternoon
 * it would draw ten hours that have not happened.
 */
fun dayEndFraction(dayStart: Instant, dayEnd: Instant, now: Instant): Float =
    if (isDayToday(dayStart, dayEnd, now)) axisFractionOf(dayStart, dayEnd, now) else 1f

/**
 * A running total's line: anchored at `(0, 0)` — it climbs from nothing at
 * midnight — and held flat at the last value out to [endFraction], so a day that
 * stopped accumulating reads as a plateau rather than a cliff. [endFraction] is the
 * now-fraction on today and 1 on a past day: a line held out to the right edge at
 * two in the afternoon would draw ten hours that have not happened.
 *
 * [fractions] are `(day fraction, running total)` pairs, ascending by fraction.
 */
fun cumulativeDayPlotPoints(
    fractions: List<Pair<Float, Double>>,
    endFraction: Float,
): List<MetricLinePlotPoint> {
    if (fractions.isEmpty()) return emptyList()
    return buildList(fractions.size + 2) {
        add(MetricLinePlotPoint(xFraction = 0f, value = 0.0))
        fractions.forEach { (fraction, value) ->
            add(MetricLinePlotPoint(xFraction = fraction, value = value))
        }
        add(MetricLinePlotPoint(xFraction = endFraction, value = fractions.last().second))
    }
}

/**
 * A raw (non-cumulative) day series' line: each reading at its real position across
 * the day, and nothing invented — no midnight anchor, no trailing hold. A weight at
 * 06:00 says nothing about midnight, and nothing about tonight.
 */
fun <T> rawDayPlotPoints(
    samples: List<T>,
    dayStart: Instant,
    dayEnd: Instant,
    time: (T) -> Instant,
    value: (T) -> Double,
): List<MetricLinePlotPoint> =
    samples.map { sample ->
        MetricLinePlotPoint(
            xFraction = axisFractionOf(dayStart, dayEnd, time(sample)),
            value = value(sample),
        )
    }

/** One stretch of a session the recording was paused for. */
@Immutable
data class SessionPause(val start: Instant, val end: Instant)

/**
 * Where a moment sits within one recorded session, and the axis that says so.
 *
 * The day axis's counterpart, for a chart whose x axis is a workout rather than a
 * day: a sample is placed against the WHOLE session, not against the samples that
 * happen to exist.
 *
 * The axis counts only MOVING time. A pause is not part of the ride, so it gets
 * none of the chart: a 21-minute bike ride with a 10½-minute pause in it spent more
 * than half the width of every card on a stretch where nothing was recorded, and
 * the elevation trace drew a smooth spline across the hole — a climb that never
 * happened, because a line joins the fixes either side of a gap whatever sits
 * between them. Collapsing the pause puts those two fixes next to each other.
 */
@Immutable
class SessionAxis(
    val start: Instant,
    val end: Instant,
    pauses: List<SessionPause> = emptyList(),
) {
    /**
     * The pauses clipped to the session, ordered and merged where they overlap —
     * merged because the arithmetic below subtracts each in turn, and two
     * overlapping segments (which a source app is free to write) would otherwise
     * have their shared stretch taken out twice.
     */
    val pauses: List<SessionPause> = normalizePauses(pauses, start, end)

    /**
     * MOVING milliseconds — the session's span less what it was paused for, and the
     * full width of the axis. At least 1: sessions of zero duration exist (a
     * recording stopped the instant it started) and would divide by zero.
     */
    val durationMs: Long = max(
        Duration.between(start, end).toMillis() - this.pauses.sumOf { pause ->
            Duration.between(pause.start, pause.end).toMillis()
        },
        1L,
    )

    /** Where [time] sits across the session, in `0..1`, counting only moving time. */
    fun fractionOf(time: Instant): Float = movingMillisAt(time).toFloat() / durationMs

    /**
     * Moving milliseconds from the start of the session up to [time].
     *
     * An instant INSIDE a pause resolves to the moment the pause began: nothing
     * moved while it ran, so everything recorded during it belongs at that one point
     * on the axis. (Heart rate keeps sampling through a pause — a strap does not
     * stop because the ride did — and those samples stack there rather than
     * stretching the pause back open.)
     */
    private fun movingMillisAt(time: Instant): Long {
        val at = time.toEpochMilli()
        val from = start.toEpochMilli()
        if (at <= from) return 0L
        var moving = at - from
        for (pause in pauses) {
            val pauseStart = pause.start.toEpochMilli()
            if (at <= pauseStart) break
            moving -= min(at, pause.end.toEpochMilli()) - pauseStart
        }
        return moving.coerceIn(0L, durationMs)
    }

    /**
     * The inverse of [fractionOf]: how far into the session that x was. The
     * scrubber needs it — a finger lands on an x, and the chart has to say when
     * that was. Moving elapsed, matching the labels directly under it.
     */
    fun elapsedAt(fraction: Float): Duration =
        Duration.ofMillis((fraction.coerceIn(0f, 1f) * durationMs).roundToLong())

    /**
     * Elapsed labels at the quarters — `0:00 … 15:00 … 30:00 … 45:00 … 1:00:00` —
     * computed from the slice of the session ON SHOW, which at full zoom is the
     * whole of it and gives back exactly the five it always did.
     */
    fun elapsedLabelsFor(viewport: ChartViewport = ChartViewport.Full): List<String> =
        (0..4).map { tick ->
            formatElapsedChartLabel(
                Duration.ofMillis(
                    (viewport.dataFraction(tick / 4f) * durationMs).roundToLong(),
                ),
            )
        }

    override fun equals(other: Any?): Boolean =
        other is SessionAxis && other.start == start && other.end == end && other.pauses == pauses

    override fun hashCode(): Int = 31 * (31 * start.hashCode() + end.hashCode()) + pauses.hashCode()
}

private fun normalizePauses(
    pauses: List<SessionPause>,
    start: Instant,
    end: Instant,
): List<SessionPause> {
    val from = start.toEpochMilli()
    val to = end.toEpochMilli()
    val clipped = pauses.mapNotNull { pause ->
        val pauseStart = max(from, pause.start.toEpochMilli())
        val pauseEnd = min(to, pause.end.toEpochMilli())
        if (pauseEnd > pauseStart) {
            SessionPause(Instant.ofEpochMilli(pauseStart), Instant.ofEpochMilli(pauseEnd))
        } else {
            null
        }
    }
    if (clipped.size < 2) return clipped
    val sorted = clipped.sortedBy { it.start }
    val merged = mutableListOf(sorted.first())
    for (pause in sorted.drop(1)) {
        val last = merged.last()
        if (!pause.start.isAfter(last.end)) {
            if (pause.end.isAfter(last.end)) {
                merged[merged.lastIndex] = SessionPause(last.start, pause.end)
            }
        } else {
            merged.add(pause)
        }
    }
    return merged
}

/**
 * The recording-elapsed format the session axis labels use: `m:ss` under an hour,
 * `h:mm:ss` above — the same shape as the recording screen's stopwatch, so the axis
 * under a workout chart reads like the timer that produced it.
 */
fun formatElapsedChartLabel(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** The elapsed-time label row under a session chart. Wrap in [ChartXAxisWithYAxis]. */
@Composable
fun SessionAxisLabels(
    axis: SessionAxis,
    viewport: ChartViewport = ChartViewport.Full,
    modifier: Modifier = Modifier,
) {
    AxisLabelRow(labels = axis.elapsedLabelsFor(viewport), modifier = modifier)
}

/**
 * The instants a row of CLOCK times under a chart should name: when the visible
 * slice starts, when it ends, and the moment halfway between. At full zoom that is
 * exactly the start/middle/end the caller always drew.
 */
fun timeAxisInstantsFor(
    start: Instant,
    end: Instant,
    viewport: ChartViewport = ChartViewport.Full,
): List<Instant> {
    val spanMs = Duration.between(start, end).toMillis()
    return listOf(0f, 0.5f, 1f).map { fraction ->
        start.plusMillis((viewport.dataFraction(fraction) * spanMs).roundToLong())
    }
}

@Composable
private fun AxisLabelRow(labels: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
