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
 * Speed rebuilt from the splits for a device that recorded none. The trace
 * steps, one flat run per split, and the stat row counts splits. The
 * trace states its own average: the chart's mean would be wrong.
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

    // ESTIMATED never reaches here (flat by construction), nor SPEED_SAMPLES (the recorded card wins).
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
 * The height profile. Health Connect has no elevation series, so this is
 * drawn from the route's altitudes. No route or altitude, no card.
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
    // Speed and cadence cannot be negative. Elevation is read against the
    // ground, not zero, and can be below it.
    floorAtZero: Boolean = true,
    // A stepped trace counts splits, not the corners of the steps.
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
    // The axis counts only moving time: a pause gets none of the chart.
    val axis = remember(sessionStart, sessionEnd, pauses) {
        SessionAxis(start = sessionStart, end = sessionEnd, pauses = pauses)
    }
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val drawPoints = sortedValues.size <= 120
    // Built once, identity-stable, so the geometry cache holds.
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
            // Plot and elapsed row share the one viewport.
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
                        // Moving elapsed, matching the labels under it.
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
