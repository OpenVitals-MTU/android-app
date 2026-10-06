package tech.mmarca.openvitals.wear.features.metric

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wear.MetricKind
import tech.mmarca.openvitals.wear.MetricUiState
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.SleepStage
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearPreferences
import tech.mmarca.openvitals.wear.ui.components.BarChart
import tech.mmarca.openvitals.wear.ui.components.BarSegment
import tech.mmarca.openvitals.wear.ui.components.LegendRow
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.Sparkline
import tech.mmarca.openvitals.wear.ui.components.StackedBar
import tech.mmarca.openvitals.wear.ui.components.formatHoursMinutes
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.HeartZoneColors
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

private val ChartHeight = 44.dp
private val SectionPadding = 12.dp

/**
 * One screen for every metric. The first screenful is the overview: the
 * value, and a goal ring where the metric has a goal. Scrolling on brings
 * today's chart, the stats and the last days. A section without data is
 * left out rather than drawn empty.
 */
@Composable
fun MetricDetailScreen(
    metric: WearMetric,
    state: MetricUiState,
    unitSystem: UnitSystem,
    maxHeartRate: Int = WearPreferences().maxHeartRate,
    restingHeartRate: Double? = null,
    sleepStages: Map<SleepStage, Double> = emptyMap(),
    /** Starts a spot measurement of this metric; null when the watch cannot measure it. */
    onMeasure: (() -> Unit)? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    val unit = stringResource(metricUnitLabel(metric, unitSystem))
    val stats = remember(metric, state, restingHeartRate) { metricStats(metric, state, restingHeartRate) }
    val zoneShares = remember(metric, state.today, maxHeartRate) {
        if (metric == WearMetric.HEART_RATE) heartRateZoneShares(state.today, maxHeartRate) else emptyList()
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                MetricHero(metric = metric, state = state, unit = unit, unitSystem = unitSystem)
            }
            if (onMeasure != null) {
                item {
                    Button(
                        onClick = onMeasure,
                        label = { Text(stringResource(R.string.measure_now)) },
                        icon = { Icon(metric.icon, contentDescription = null, tint = metric.accentColor) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (metric == WearMetric.SLEEP && sleepStages.isNotEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.detail_sleep_stages)) } }
                item {
                    BreakdownSection(
                        rows = SleepStage.entries.map { stage ->
                            BreakdownRow(
                                color = stage.color,
                                label = stringResource(stage.label),
                                value = formatHoursMinutes(sleepStages[stage] ?: 0.0),
                                share = (sleepStages[stage] ?: 0.0).toFloat(),
                            )
                        },
                    )
                }
            }
            if (state.today.size >= 2) {
                item { ListSubHeader { Text(stringResource(R.string.detail_today)) } }
                item {
                    val chartModifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SectionPadding)
                        .height(ChartHeight)
                    when (metric.kind) {
                        MetricKind.CUMULATIVE -> BarChart(state.today, metric.accentColor, chartModifier)
                        MetricKind.SAMPLED -> Sparkline(state.today, metric.accentColor, chartModifier)
                    }
                }
            }
            if (zoneShares.any { it > 0f }) {
                item { ListSubHeader { Text(stringResource(R.string.detail_heart_zones)) } }
                item {
                    BreakdownSection(
                        rows = zoneShares.mapIndexed { zone, share ->
                            BreakdownRow(
                                color = HeartZoneColors[zone],
                                label = stringResource(HeartZoneLabels[zone]),
                                value = stringResource(R.string.percent_value, (share * 100).roundToInt()),
                                share = share,
                            )
                        },
                    )
                }
            }
            items(stats) { stat ->
                StatRow(
                    label = stringResource(stat.label),
                    value = formatMetricValue(metric, stat.value, unitSystem),
                    unit = unit,
                )
            }
            if (state.week.isNotEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.detail_week)) } }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SectionPadding),
                    ) {
                        BarChart(
                            values = state.week,
                            color = metric.accentColor,
                            highlightLast = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ChartHeight),
                        )
                        DayLabels(count = state.week.size)
                    }
                }
            }
            if (state.current == null && state.today.isEmpty() && state.week.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.detail_no_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricHero(
    metric: WearMetric,
    state: MetricUiState,
    unit: String,
    unitSystem: UnitSystem,
) {
    val goal = state.goal?.takeIf { it > 0 }
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = metric.icon,
                contentDescription = stringResource(metric.label),
                tint = metric.accentColor,
                modifier = Modifier.size(20.dp),
            )
            MetricValueRow(
                value = state.current?.let { formatMetricValue(metric, it, unitSystem) },
                unit = unit,
                valueStyle = MaterialTheme.typography.displaySmall,
            )
            if (goal != null) {
                Text(
                    text = stringResource(R.string.metric_goal, formatMetricValue(metric, goal, unitSystem)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (goal == null) {
        Box(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    } else {
        Box(modifier = Modifier.size(136.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { ((state.current ?: 0.0) / goal).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = metric.accentColor,
                    trackColor = metric.accentColor.copy(alpha = Emphasis.subtle),
                ),
            )
            content()
        }
    }
}

private data class BreakdownRow(val color: Color, val label: String, val value: String, val share: Float)

/** A stacked bar with one legend line per part, for heart rate zones and sleep stages. */
@Composable
private fun BreakdownSection(rows: List<BreakdownRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SectionPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StackedBar(
            segments = rows.map { BarSegment(it.share, it.color) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )
        rows.forEach { LegendRow(color = it.color, label = it.label, value = it.value) }
    }
}

@Composable
private fun StatRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SectionPadding, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetricValueRow(value = value, unit = unit, valueStyle = MaterialTheme.typography.titleMedium)
    }
}

/** Narrow weekday names for the last [count] days, today last. */
@Composable
private fun DayLabels(count: Int) {
    val labels = remember(count) {
        val today = LocalDate.now()
        List(count) { index ->
            today.minusDays((count - 1 - index).toLong())
                .dayOfWeek
                .getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private data class MetricStat(@param:StringRes val label: Int, val value: Double)

/** The stats that make sense for the metric's kind, from the data that is there. */
private fun metricStats(metric: WearMetric, state: MetricUiState, restingHeartRate: Double?): List<MetricStat> =
    buildList {
        if (metric == WearMetric.HEART_RATE && restingHeartRate != null) {
            add(MetricStat(R.string.stat_resting, restingHeartRate))
        }
        when (metric.kind) {
            MetricKind.SAMPLED -> if (state.today.isNotEmpty()) {
                add(MetricStat(R.string.stat_low, state.today.min()))
                add(MetricStat(R.string.stat_average, state.today.average()))
                add(MetricStat(R.string.stat_high, state.today.max()))
            }
            MetricKind.CUMULATIVE -> {
                val goal = state.goal
                if (goal != null) {
                    add(MetricStat(R.string.stat_remaining, (goal - (state.current ?: 0.0)).coerceAtLeast(0.0)))
                }
                if (state.week.isNotEmpty()) {
                    add(MetricStat(R.string.stat_daily_average, state.week.average()))
                    add(MetricStat(R.string.stat_best_day, state.week.max()))
                }
            }
        }
    }

@WearPreviews
@Composable
private fun MetricDetailScreenGoalPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MetricDetailScreen(WearMetric.STEPS, SampleData.metrics.getValue(WearMetric.STEPS), UnitSystem.METRIC)
        }
    }
}

@WearPreviews
@Composable
private fun MetricDetailScreenSampledPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MetricDetailScreen(
                WearMetric.HEART_RATE,
                SampleData.metrics.getValue(WearMetric.HEART_RATE),
                UnitSystem.METRIC,
                restingHeartRate = 58.0,
                onMeasure = {},
            )
        }
    }
}

@WearPreviews
@Composable
private fun MetricDetailScreenEmptyPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MetricDetailScreen(WearMetric.ELEVATION, MetricUiState(), UnitSystem.METRIC)
        }
    }
}

@WearPreviews
@Composable
private fun MetricDetailScreenSleepPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            MetricDetailScreen(
                WearMetric.SLEEP,
                SampleData.metrics.getValue(WearMetric.SLEEP),
                UnitSystem.METRIC,
                sleepStages = SampleData.sleepStages,
            )
        }
    }
}
