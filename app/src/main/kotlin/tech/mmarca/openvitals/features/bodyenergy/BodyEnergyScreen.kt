package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

@Composable
fun BodyEnergyDetailsScreen(
    viewModel: BodyEnergyViewModel,
    selectedDate: LocalDate,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(selectedDate) {
        if (state.selectedDate != selectedDate) {
            viewModel.selectDate(selectedDate)
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.BODY_ENERGY,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { uxState ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::refresh,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            showTimeRangeSelector = false,
            syncPaused = uxState.syncPaused,
        ) {
            val result = state.result
            if (result == null && state.error != null) {
                item {
                    ErrorMessage(
                        message = state.error.resolve() ?: stringResource(R.string.unknown_error),
                    )
                }
                return@MetricDetailScaffold
            }
            if (result == null) return@MetricDetailScaffold
            bodyEnergyContent(
                result = result,
            )
        }
    }
}

private fun LazyListScope.bodyEnergyContent(
    result: BodyEnergyTimelineResult,
) {
    item {
        BodyEnergySummaryCard(
            result = result,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        BodyEnergyDayTimelineCard(
            timeline = result.latestDay,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (result.latestDay?.confidence == BodyEnergyConfidence.LOW) {
        item {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.body_energy_timeline_low_confidence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BodyEnergySummaryCard(
    result: BodyEnergyTimelineResult,
    modifier: Modifier = Modifier,
) {
    val latest = result.latestDay
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = bodyEnergyColor(latest?.currentScore),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.screen_body_energy),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.body_energy_timeline_estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = latest?.currentScore?.toString() ?: "--",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = bodyEnergyColor(latest?.currentScore),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_start),
                    value = latest?.startScore?.toString() ?: "--",
                    modifier = Modifier.weight(1f),
                )
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_charged),
                    value = "+${result.charged}",
                    modifier = Modifier.weight(1f),
                )
                BodyEnergyStat(
                    label = stringResource(R.string.body_energy_timeline_drained),
                    value = "-${result.drained}",
                    modifier = Modifier.weight(1f),
                )
            }
            BodyEnergyStat(
                label = stringResource(R.string.body_energy_timeline_confidence),
                value = confidenceText(latest?.confidence ?: BodyEnergyConfidence.NO_DATA),
                body = latest?.confidenceReason.orEmpty(),
            )
        }
    }
}

@Composable
private fun BodyEnergyDayTimelineCard(
    timeline: BodyEnergyTimeline?,
    modifier: Modifier = Modifier,
) {
    BodyEnergyChartCard(
        title = stringResource(R.string.body_energy_timeline_day_title),
        points = timeline?.dayPlotPoints().orEmpty(),
        empty = timeline == null || timeline.points.isEmpty(),
        modifier = modifier,
    )
}

@Composable
private fun BodyEnergyChartCard(
    title: String,
    points: List<MetricLinePlotPoint>,
    empty: Boolean,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (empty) {
                Text(
                    text = stringResource(R.string.body_energy_timeline_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MetricLinePlot(
                    points = points,
                    minValue = 0.0,
                    maxValue = 100.0,
                    accentColor = MaterialTheme.colorScheme.primary,
                    chartHeight = 180.dp,
                    valueFormatter = { it.roundToInt().toString() },
                    drawPoints = points.size <= 40,
                    modifier = Modifier.fillMaxWidth(),
                    canvasModifier = Modifier.height(180.dp),
                )
            }
        }
    }
}

@Composable
private fun BodyEnergyStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    body: String = "",
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun BodyEnergyTimeline.dayPlotPoints(): List<MetricLinePlotPoint> {
    if (points.isEmpty()) return emptyList()
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val totalSeconds = Duration.ofDays(1).seconds.toFloat()
    return points.map { point ->
        MetricLinePlotPoint(
            xFraction = (Duration.between(start, point.time).seconds / totalSeconds).coerceIn(0f, 1f),
            value = point.score.toDouble(),
        )
    }
}

@Composable
private fun bodyEnergyColor(score: Int?): Color =
    when {
        score == null -> MaterialTheme.colorScheme.onSurfaceVariant
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.tertiary
        score >= 40 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

private fun confidenceText(confidence: BodyEnergyConfidence): String =
    when (confidence) {
        BodyEnergyConfidence.HIGH -> "High"
        BodyEnergyConfidence.MEDIUM -> "Medium"
        BodyEnergyConfidence.LOW -> "Low"
        BodyEnergyConfidence.NO_DATA -> "No data"
    }
