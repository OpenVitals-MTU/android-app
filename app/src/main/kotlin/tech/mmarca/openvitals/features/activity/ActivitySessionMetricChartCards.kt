package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SessionAxis
import tech.mmarca.openvitals.ui.components.SessionAxisLabels
import tech.mmarca.openvitals.ui.components.SessionPause
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.StepsColor

@Composable
internal fun ActivitySpeedChartCard(
    samples: List<SpeedSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (samples.isEmpty()) return

    ActivitySessionMetricChartCard(
        title = stringResource(R.string.activity_recording_live_speed),
        sortedValues = samples.sortedBy { it.time }.map { it.time to it.metersPerSecond },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = DistanceColor,
        valueFormatter = { unitFormatter.speed(it).text },
        modifier = modifier,
        pauses = pauses,
    )
}

/**
 * Speed over the session for a device that recorded none — rebuilt from the
 * splits, which know how far each segment went and how long it took.
 *
 * The trace STEPS, one flat run per split, because that is the resolution the
 * numbers have — a split's speed is an average over its window, not a reading
 * at an instant. The title says where it came from ("every 1 km", "per lap")
 * and the stat row counts SPLITS rather than samples, because there are no
 * samples here. The [ActivitySplitSpeedTrace] states its own average: the
 * chart's own mean would weigh a 200 m limp home equally with a 1 km split and
 * quietly report a faster session than happened.
 */
@Composable
internal fun ActivitySplitSpeedChartCard(
    trace: ActivitySplitSpeedTrace,
    source: SplitSource,
    splitDistanceMeters: Double,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (trace.samples.isEmpty()) return

    // ESTIMATED never reaches here (it is flat by construction, so the display
    // builds no trace for it), and neither does SPEED_SAMPLES (that source
    // exists only when speed WAS recorded, and the recorded card wins).
    val title: String
    val countLabel: String
    when (source) {
        SplitSource.DEVICE_LAPS -> {
            title = stringResource(R.string.activity_speed_per_lap_title)
            countLabel = stringResource(R.string.activity_speed_laps_label)
        }
        else -> {
            title = stringResource(
                R.string.activity_speed_per_split_title,
                splitDistanceLabel(unitFormatter, splitDistanceMeters),
            )
            countLabel = stringResource(R.string.activity_speed_splits_label)
        }
    }

    ActivitySessionMetricChartCard(
        title = title,
        sortedValues = trace.samples.map { it.time to it.metersPerSecond },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = DistanceColor,
        valueFormatter = { unitFormatter.speed(it).text },
        countText = unitFormatter.count(trace.splitCount),
        countLabel = countLabel,
        averageOverride = trace.averageMetersPerSecond,
        modifier = modifier,
        pauses = pauses,
    )
}

/**
 * The height profile of the session.
 *
 * Health Connect has no elevation series: `ElevationGainedRecord` is one total
 * for the whole session — it says you climbed 240 m, never where. So this is
 * drawn from the ROUTE's altitudes, the only thing in Health Connect that
 * knows the shape of a climb. An activity with no route, or a route recorded
 * without altitude, has no profile to show and no card.
 */
@Composable
internal fun ActivityElevationChartCard(
    samples: List<ActivityElevationSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (samples.isEmpty()) return

    ActivitySessionMetricChartCard(
        title = stringResource(R.string.metric_elevation),
        sortedValues = samples.map { it.time to it.meters },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = ElevationColor,
        valueFormatter = { unitFormatter.elevation(it).text },
        floorAtZero = false,
        modifier = modifier,
        pauses = pauses,
    )
}

@Composable
internal fun ActivityCadenceChartCard(
    samples: List<ActivityCadenceSample>,
    kind: ActivityCadenceKind,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    val filtered = samples.filter { it.kind == kind }
    if (filtered.isEmpty()) return

    val title = when (kind) {
        ActivityCadenceKind.CYCLING -> stringResource(R.string.metric_cycling_cadence)
        ActivityCadenceKind.STEPS -> stringResource(R.string.metric_steps_cadence)
    }
    val accentColor = when (kind) {
        ActivityCadenceKind.CYCLING -> CycleColor
        ActivityCadenceKind.STEPS -> StepsColor
    }

    ActivitySessionMetricChartCard(
        title = title,
        sortedValues = filtered.sortedBy { it.time }.map { it.time to it.rate },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = accentColor,
        valueFormatter = { unitFormatter.cadence(it).text },
        modifier = modifier,
        pauses = pauses,
    )
}

@Composable
private fun ActivitySessionMetricChartCard(
    title: String,
    sortedValues: List<Pair<Instant, Double>>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    accentColor: Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    // Speed and cadence cannot be negative, and an axis that starts anywhere
    // else would be lying about how fast you were going. ELEVATION is the
    // opposite: a run between 300 m and 350 m has fifty metres of relief in
    // it, and pinning the axis to sea level would draw it as a flat line at
    // the top of the card. Height is read against the ground, not against
    // zero — and can be below it.
    floorAtZero: Boolean = true,
    // A recorded trace counts its samples; a trace stepped per split says so
    // itself, because "12 samples" for six splits would be counting the
    // corners of the steps.
    countText: String? = null,
    countLabel: String? = null,
    averageOverride: Double? = null,
    pauses: List<SessionPause> = emptyList(),
) {
    if (sortedValues.isEmpty()) return

    val minValue = sortedValues.minOf { it.second }
    val maxValue = sortedValues.maxOf { it.second }
    val avgValue = averageOverride ?: sortedValues.map { it.second }.average()
    val valueRange = (maxValue - minValue).coerceAtLeast(0.001)
    val paddedMin = (minValue - valueRange * 0.1).let { if (floorAtZero) it.coerceAtLeast(0.0) else it }
    val paddedMax = maxValue + valueRange * 0.1
    // The axis counts only moving time: a pause is not part of the ride, so it
    // gets none of the chart — collapsing it also stops the spline from drawing a
    // climb across a hole where nothing was recorded.
    val axis = remember(sessionStart, sessionEnd, pauses) {
        SessionAxis(start = sessionStart, end = sessionEnd, pauses = pauses)
    }
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val drawPoints = sortedValues.size <= 120
    // Built once, identity-stable across pinch frames, so the plot's geometry
    // cache holds.
    val chartPoints = remember(sortedValues, axis) {
        sortedValues.map { (time, value) ->
            MetricLinePlotPoint(xFraction = axis.fractionOf(time), value = value)
        }
    }

    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActivitySessionMetricStat(
                    label = stringResource(R.string.summary_average),
                    value = valueFormatter(avgValue),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
                ActivitySessionMetricStat(
                    label = stringResource(R.string.summary_range),
                    value = "${valueFormatter(minValue)}-${valueFormatter(maxValue)}",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
                ActivitySessionMetricStat(
                    label = countLabel ?: stringResource(R.string.summary_samples),
                    value = countText ?: unitFormatter.count(sortedValues.size),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
            }
            // The plot and its elapsed row share the one viewport, so the labels
            // always describe the slice of the session actually on show.
            ChartZoom(sessionStart, sessionEnd, sortedValues) { zoom ->
                Column {
                    MetricLinePlot(
                        points = chartPoints,
                        minValue = paddedMin,
                        maxValue = paddedMax,
                        accentColor = accentColor,
                        chartHeight = chartHeight,
                        valueFormatter = valueFormatter,
                        pointRadius = if (drawPoints) 2.dp else 0.dp,
                        lineStrokeWidth = 2.dp,
                        drawPoints = drawPoints,
                        viewport = zoom.viewport,
                        multiTouch = zoom.multiTouch,
                        // Moving elapsed, matching the labels directly under it.
                        scrubLabel = { point ->
                            valueFormatter(point.value) to
                                formatElapsedChartLabel(axis.elapsedAt(point.xFraction))
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    ChartXAxisWithYAxis {
                        SessionAxisLabels(axis = axis, viewport = zoom.viewport)
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.summary_recorded,
                    timeFormatter.format(sortedValues.first().first.atZone(zone)),
                    timeFormatter.format(sortedValues.last().first.atZone(zone)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivitySessionMetricStat(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
