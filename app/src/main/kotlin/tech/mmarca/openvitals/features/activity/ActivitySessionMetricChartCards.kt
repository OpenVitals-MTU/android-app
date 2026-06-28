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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.StepsColor

@Composable
internal fun ActivitySpeedChartCard(
    samples: List<SpeedSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
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
) {
    if (sortedValues.isEmpty()) return

    val minValue = sortedValues.minOf { it.second }
    val maxValue = sortedValues.maxOf { it.second }
    val avgValue = sortedValues.map { it.second }.average()
    val valueRange = (maxValue - minValue).coerceAtLeast(0.001)
    val paddedMin = (minValue - valueRange * 0.1).coerceAtLeast(0.0)
    val paddedMax = maxValue + valueRange * 0.1
    val sessionDurationMillis = Duration.between(sessionStart, sessionEnd)
        .toMillis()
        .coerceAtLeast(1L)
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val drawPoints = sortedValues.size <= 120

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
                    label = stringResource(R.string.summary_samples),
                    value = unitFormatter.count(sortedValues.size),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
            }
            MetricLinePlot(
                points = sortedValues.map { (time, value) ->
                    val elapsed = Duration.between(sessionStart, time)
                        .toMillis()
                        .coerceIn(0L, sessionDurationMillis)
                    MetricLinePlotPoint(
                        xFraction = elapsed.toFloat() / sessionDurationMillis.toFloat(),
                        value = value,
                    )
                },
                minValue = paddedMin,
                maxValue = paddedMax,
                accentColor = accentColor,
                chartHeight = chartHeight,
                valueFormatter = valueFormatter,
                pointRadius = if (drawPoints) 2.dp else 0.dp,
                lineStrokeWidth = 2.dp,
                drawPoints = drawPoints,
            )
            Spacer(Modifier.height(4.dp))
            ChartXAxisWithYAxis {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    sessionElapsedLabels(sessionDurationMillis).forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
