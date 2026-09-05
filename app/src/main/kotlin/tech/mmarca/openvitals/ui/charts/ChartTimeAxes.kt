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
 * The x axes a time chart sits on, and their label rows. One rule: the x
 * axis is the whole day or the whole session, always. Today's series stops
 * at now rather than stretching to the edge.
 */

private const val MinutesPerDay = 1440f

/**
 * Five evenly spaced labels under a day chart, for the slice on show. At
 * full zoom `00:00 / 06:00 / 12:00 / 18:00 / 24:00`.
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

/** The `00:00 … 24:00` row under an intraday chart. Wrap in [ChartXAxisWithYAxis] when the plot has a y gutter. */
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

/** Whether `[dayStart, dayEnd)` is the day [now] falls in. */
fun isDayToday(dayStart: Instant, dayEnd: Instant, now: Instant): Boolean =
    !now.isBefore(dayStart) && now.isBefore(dayEnd)

/** How much of the day a series may claim: the now-fraction today, the whole width otherwise. */
fun dayEndFraction(dayStart: Instant, dayEnd: Instant, now: Instant): Float =
    if (isDayToday(dayStart, dayEnd, now)) axisFractionOf(dayStart, dayEnd, now) else 1f

/**
 * A running total's line: anchored at `(0, 0)` and held flat at the last
 * value out to [endFraction]. [fractions] are `(day fraction, total)` pairs.
 */
fun cumulativeDayPlotPoints(
    fractions: List<Pair<Float, Double>>,
    endFraction: Float,
): List<MetricLinePlotPoint> {
    if (fractions.isEmpty()) return emptyList()
    // The anchor and the hold are scaffolding, marked synthetic so they get no dot.
    return buildList(fractions.size + 2) {
        add(MetricLinePlotPoint(xFraction = 0f, value = 0.0, synthetic = true))
        fractions.forEach { (fraction, value) ->
            add(MetricLinePlotPoint(xFraction = fraction, value = value))
        }
        add(
            MetricLinePlotPoint(
                xFraction = endFraction,
                value = fractions.last().second,
                synthetic = true,
            )
        )
    }
}

/** A raw day series' line: each reading at its real position, nothing invented. */
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
 * Where a moment sits within one session, and the axis that says so. The
 * axis counts only moving time: a pause gets none of the chart, so the two
 * fixes either side of it sit next to each other.
 */
@Immutable
class SessionAxis(
    val start: Instant,
    val end: Instant,
    pauses: List<SessionPause> = emptyList(),
) {
    /** The pauses clipped to the session, ordered and merged where they overlap. */
    val pauses: List<SessionPause> = normalizePauses(pauses, start, end)

    /** Moving milliseconds, the full width of the axis. At least 1. */
    val durationMs: Long = max(
        Duration.between(start, end).toMillis() - this.pauses.sumOf { pause ->
            Duration.between(pause.start, pause.end).toMillis()
        },
        1L,
    )

    /** Where [time] sits across the session, in `0..1`, counting only moving time. */
    fun fractionOf(time: Instant): Float = movingMillisAt(time).toFloat() / durationMs

    /**
     * Moving milliseconds up to [time]. An instant inside a pause resolves
     * to the moment the pause began.
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

    /** The inverse of [fractionOf], for the scrubber. */
    fun elapsedAt(fraction: Float): Duration =
        Duration.ofMillis((fraction.coerceIn(0f, 1f) * durationMs).roundToLong())

    /** Elapsed labels at the quarters of the slice on show. */
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

/** `m:ss` under an hour, `h:mm:ss` above, like the recording stopwatch. */
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

/** The instants a row of clock times names: start, middle and end of the visible slice. */
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
