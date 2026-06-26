package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.ui.components.OpenVitalsCard


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.BloodPressureCategory
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.VitalContextInterpretation
import tech.mmarca.openvitals.domain.insights.VitalContextStatus
import tech.mmarca.openvitals.domain.insights.bloodPressureInterpretation
import tech.mmarca.openvitals.domain.insights.bodyTemperatureContext
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.oxygenSaturationContext
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.respiratoryRateContext
import tech.mmarca.openvitals.domain.insights.restingHeartRateContext
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.dataSourceEducationItem
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class HeartMetric {
    AVERAGE_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AverageHeartRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.AVERAGE_HEART_RATE,
    )
}

@Composable
fun RestingHeartRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESTING_HEART_RATE,
    )
}

@Composable
fun HrvScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.HRV,
    )
}

@Composable
fun BloodPressureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BLOOD_PRESSURE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun SpO2Screen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.SPO2,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun Vo2MaxScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.VO2_MAX,
    )
}

@Composable
fun RespiratoryRateScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.RESPIRATORY_RATE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun BodyTemperatureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BODY_TEMPERATURE,
        onEditVitalsMeasurement = onEditVitalsMeasurement,
    )
}

@Composable
fun BloodGlucoseScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.BLOOD_GLUCOSE,
    )
}

@Composable
fun SkinTemperatureScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    HeartMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = HeartMetric.SKIN_TEMPERATURE,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HeartMetricScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: HeartMetric,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEART,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            error = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
        ) { period ->
        when (metric) {
            HeartMetric.AVERAGE_HEART_RATE -> averageHeartRateContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onDecreaseHighHeartRateThreshold = viewModel::decreaseHighHeartRateThreshold,
                onIncreaseHighHeartRateThreshold = viewModel::increaseHighHeartRateThreshold,
                onDecreaseLowHeartRateThreshold = viewModel::decreaseLowHeartRateThreshold,
                onIncreaseLowHeartRateThreshold = viewModel::increaseLowHeartRateThreshold,
            )
            HeartMetric.RESTING_HEART_RATE -> restingHeartRateContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
            )
            HeartMetric.HRV -> hrvContent(state, period, unitFormatter, dateTimeFormatterProvider, chartDaySelection)
            HeartMetric.BLOOD_PRESSURE -> bloodPressureContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.SPO2 -> spO2Content(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.VO2_MAX -> vo2MaxContent(state, period, unitFormatter, dateTimeFormatterProvider)
            HeartMetric.RESPIRATORY_RATE -> respiratoryRateContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.BODY_TEMPERATURE -> bodyTemperatureContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                onEditVitalsMeasurement,
                viewModel::deleteVitalsMeasurementEntry,
            )
            HeartMetric.BLOOD_GLUCOSE -> bloodGlucoseContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
            )
            HeartMetric.SKIN_TEMPERATURE -> skinTemperatureContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
            )
        }
    }
    }
}

private fun LazyListScope.averageHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onDecreaseHighHeartRateThreshold: () -> Unit,
    onIncreaseHighHeartRateThreshold: () -> Unit,
    onDecreaseLowHeartRateThreshold: () -> Unit,
    onIncreaseLowHeartRateThreshold: () -> Unit,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.daySamples.isNotEmpty() -> {
            if (state.daySamples.size > 1) {
                item {
                    HeartRateTimelineCard(
                        date = state.selectedDate,
                        samples = state.daySamples,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = metricModifier(),
                    )
                }
            }
            heartRateThresholdChecks(
                state = state,
                unitFormatter = unitFormatter,
                onDecreaseHighHeartRateThreshold = onDecreaseHighHeartRateThreshold,
                onIncreaseHighHeartRateThreshold = onIncreaseHighHeartRateThreshold,
                onDecreaseLowHeartRateThreshold = onDecreaseLowHeartRateThreshold,
                onIncreaseLowHeartRateThreshold = onIncreaseLowHeartRateThreshold,
            )
            heartRawDataConfidence(
                period = period,
                entries = state.daySamples,
                source = { it.source },
                time = { it.time },
                accentColor = HeartColor,
            )
            heartRateSampleStatistics(
                samples = state.daySamples,
                previousSamples = state.previousDaySamples,
                baselineSummaries = state.baselineDailySummaries,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
            heartEntryRows(
                entries = state.daySamples,
                value = { unitFormatter.heartRate(it.beatsPerMinute).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        state.selectedRange == TimeRange.DAY && !state.isLoading -> {
            item { HeartRateEmptyDayCard(modifier = metricModifier()) }
        }
        state.dailySummaries.isNotEmpty() -> {
            val sorted = state.dailySummaries.sortedBy { it.date }
            val rangeSummary = heartRateRangeSummary(sorted)
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_average_heart_rate),
                    series = heartRateSeries(
                        summaries = sorted,
                        averageLabel = stringResource(R.string.summary_average),
                        lowestLabel = stringResource(R.string.stat_lowest),
                        highestLabel = stringResource(R.string.stat_highest),
                    ),
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = HeartColor,
                    summaryText = rangeSummary?.let {
                        "${localizedPeriodTitle(state.selectedRange, period)} · ${
                            stringResource(
                                R.string.summary_avg_value_range,
                                unitFormatter.heartRate(it.average).text,
                                unitFormatter.heartRate(it.min).text,
                                unitFormatter.heartRate(it.max).text,
                            )
                        }"
                    } ?: localizedPeriodTitle(state.selectedRange, period),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
                )
            }
            heartRateThresholdChecks(
                state = state,
                unitFormatter = unitFormatter,
                onDecreaseHighHeartRateThreshold = onDecreaseHighHeartRateThreshold,
                onIncreaseHighHeartRateThreshold = onIncreaseHighHeartRateThreshold,
                onDecreaseLowHeartRateThreshold = onDecreaseLowHeartRateThreshold,
                onIncreaseLowHeartRateThreshold = onIncreaseLowHeartRateThreshold,
            )
            chartDaySelection.selectedDate?.let { selectedDate ->
                item {
                    PaginatedEntryList(
                        title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                        entries = state.dailySummaries.filter { it.date == selectedDate },
                    ) { summary, rowModifier ->
                        HeartRateDayRow(
                            summary = summary,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = rowModifier,
                        )
                    }
                }
            }
            heartAggregateDataConfidence(
                period = period,
                trackedDates = state.dailySummaries.map { it.date },
                sampleCount = state.dailySummaries.size,
                accentColor = HeartColor,
            )
            heartRateSummaryStatistics(
                summaries = state.dailySummaries,
                previousSummaries = state.previousDailySummaries,
                baselineSummaries = state.baselineDailySummaries,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
            item {
                PaginatedEntryList(
                    title = stringResource(R.string.section_daily_breakdown),
                    entries = state.dailySummaries.sortedByDescending { it.date },
                ) { summary, rowModifier ->
                    HeartRateDayRow(
                        summary = summary,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = rowModifier,
                    )
                }
            }
            dataSourceEducationItem()
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_average_heart_rate,
            messageRes = R.string.message_no_heart_period,
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.heartRateThresholdChecks(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
    onDecreaseHighHeartRateThreshold: () -> Unit,
    onIncreaseHighHeartRateThreshold: () -> Unit,
    onDecreaseLowHeartRateThreshold: () -> Unit,
    onIncreaseLowHeartRateThreshold: () -> Unit,
) {
    item {
        SectionHeader(
            text = stringResource(R.string.heart_rate_health_checks_title),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    item {
        Row(
            modifier = metricModifier(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeartRateThresholdCheckCard(
                check = state.highHeartRateCheck,
                title = stringResource(R.string.heart_rate_high_title),
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
                onDecreaseThreshold = onDecreaseHighHeartRateThreshold,
                onIncreaseThreshold = onIncreaseHighHeartRateThreshold,
                modifier = Modifier.weight(1f),
            )
            HeartRateThresholdCheckCard(
                check = state.lowHeartRateCheck,
                title = stringResource(R.string.heart_rate_low_title),
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
                onDecreaseThreshold = onDecreaseLowHeartRateThreshold,
                onIncreaseThreshold = onIncreaseLowHeartRateThreshold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeartRateThresholdCheckCard(
    check: HeartRateThresholdCheck,
    title: String,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    onDecreaseThreshold: () -> Unit,
    onIncreaseThreshold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (check.type == HeartRateThresholdCheckType.HIGH) {
                        Icons.Outlined.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = HeartColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (check.hasData) unitFormatter.count(check.count) else stringResource(R.string.no_data),
                style = MaterialTheme.typography.headlineSmall,
                color = HeartColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = heartRateThresholdSubtitle(check, selectedRange),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OpenVitalsIconButton(
                    onClick = onDecreaseThreshold,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_hr_threshold),
                        modifier = Modifier.size(18.dp),
                    )
                }
                OpenVitalsIconButton(
                    onClick = onIncreaseThreshold,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_increase_hr_threshold),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun heartRateThresholdSubtitle(
    check: HeartRateThresholdCheck,
    selectedRange: TimeRange,
): String {
    val stringRes = when (check.type) {
        HeartRateThresholdCheckType.HIGH -> if (selectedRange == TimeRange.DAY) {
            R.string.heart_rate_samples_at_or_above
        } else {
            R.string.heart_rate_days_at_or_above
        }
        HeartRateThresholdCheckType.LOW -> if (selectedRange == TimeRange.DAY) {
            R.string.heart_rate_samples_at_or_below
        } else {
            R.string.heart_rate_days_at_or_below
        }
    }
    return stringResource(stringRes, check.thresholdBpm)
}

private fun LazyListScope.restingHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.dayRestingBpm != null -> {
            item {
                RestingHRDayCard(
                    bpm = state.dayRestingBpm,
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
            heartAggregateDataConfidence(
                period = period,
                trackedDates = listOf(state.selectedDate),
                sampleCount = 1,
                accentColor = HeartColor,
            )
            restingHeartRateContextCard(state.dayRestingBpm)
            heartNumericStatistics(
                unitFormatter = unitFormatter,
                average = unitFormatter.heartRate(state.dayRestingBpm),
                low = unitFormatter.heartRate(state.dayRestingBpm),
                high = unitFormatter.heartRate(state.dayRestingBpm),
                readings = 1,
                comparison = state.previousDayRestingBpm?.let {
                    periodComparison(state.dayRestingBpm.toDouble(), it.toDouble())
                },
                selectedRange = state.selectedRange,
                comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
                icon = Icons.Outlined.FavoriteBorder,
                accentColor = HeartColor,
                period = period,
                baselineCurrentValue = state.dayRestingBpm.toDouble(),
                baselineValues = state.baselineDailyRestingHR.map { BaselineValue(it.date, it.bpm.toDouble()) },
            )
            heartDailyEntries(
                entries = listOf(DailyRestingHR(state.selectedDate, state.dayRestingBpm)),
                date = { it.date },
                value = { unitFormatter.heartRate(it.bpm).text },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = HeartColor,
            )
        }
        state.selectedRange != TimeRange.DAY && state.dailyRestingHR.isNotEmpty() -> {
            val sorted = state.dailyRestingHR.sortedBy { it.date }
            val rangeSummary = restingHeartRateRangeSummary(sorted)
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_resting_heart_rate),
                    points = sorted.map { MetricLinePoint(date = it.date, value = it.bpm.toDouble()) },
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = HeartColor,
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_avg_value_range,
                            unitFormatter.heartRate(rangeSummary.average).text,
                            unitFormatter.heartRate(rangeSummary.min).text,
                            unitFormatter.heartRate(rangeSummary.max).text,
                        )
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                heartDailyEntries(
                    entries = state.dailyRestingHR.filter { it.date == selectedDate },
                    date = { it.date },
                    value = { unitFormatter.heartRate(it.bpm).text },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = HeartColor,
                    titleDate = selectedDate,
                )
            }
            heartAggregateDataConfidence(
                period = period,
                trackedDates = state.dailyRestingHR.map { it.date },
                sampleCount = state.dailyRestingHR.size,
                accentColor = HeartColor,
            )
            restingHeartRateContextCard(
                state.dailyRestingHR.map { it.bpm }.average().roundToInt().toLong(),
            )
            restingHeartRateStatistics(
                entries = state.dailyRestingHR,
                previousEntries = state.previousDailyRestingHR,
                baselineEntries = state.baselineDailyRestingHR,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
            heartDailyEntries(
                entries = state.dailyRestingHR,
                date = { it.date },
                value = { unitFormatter.heartRate(it.bpm).text },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = HeartColor,
            )
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_resting_heart_rate,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.hrvContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    when {
        state.selectedRange == TimeRange.DAY && state.dayHrvMs != null -> {
            item {
                HRVDayCard(
                    rmssdMs = state.dayHrvMs,
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
            heartAggregateDataConfidence(
                period = period,
                trackedDates = listOf(state.selectedDate),
                sampleCount = 1,
                accentColor = HeartColor,
            )
            heartNumericStatistics(
                unitFormatter = unitFormatter,
                average = unitFormatter.hrv(state.dayHrvMs),
                low = unitFormatter.hrv(state.dayHrvMs),
                high = unitFormatter.hrv(state.dayHrvMs),
                readings = 1,
                comparison = state.previousDayHrvMs?.let {
                    periodComparison(state.dayHrvMs, it)
                },
                selectedRange = state.selectedRange,
                comparisonValueFormatter = { unitFormatter.hrv(it) },
                icon = Icons.Outlined.FavoriteBorder,
                accentColor = HeartColor,
                period = period,
                baselineCurrentValue = state.dayHrvMs,
                baselineValues = state.baselineDailyHrv.map { BaselineValue(it.date, it.rmssdMs) },
            )
            heartDailyEntries(
                entries = listOf(DailyHrv(state.selectedDate, state.dayHrvMs)),
                date = { it.date },
                value = { unitFormatter.hrv(it.rmssdMs).text },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = HeartColor,
            )
        }
        state.selectedRange != TimeRange.DAY && state.dailyHrv.isNotEmpty() -> {
            val sorted = state.dailyHrv.sortedBy { it.date }
            val rangeSummary = hrvRangeSummary(sorted)
            item {
                MetricLineChart(
                    title = stringResource(R.string.metric_hrv),
                    points = sorted.map { MetricLinePoint(date = it.date, value = it.rmssdMs) },
                    selectedRange = state.selectedRange,
                    period = period,
                    accentColor = HeartColor.copy(alpha = 0.85f),
                    summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_avg_value_range,
                            unitFormatter.hrv(rangeSummary.average).text,
                            unitFormatter.hrv(rangeSummary.min).text,
                            unitFormatter.hrv(rangeSummary.max).text,
                        )
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    valueFormatter = { unitFormatter.hrv(it).text },
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                heartDailyEntries(
                    entries = state.dailyHrv.filter { it.date == selectedDate },
                    date = { it.date },
                    value = { unitFormatter.hrv(it.rmssdMs).text },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    accentColor = HeartColor,
                    titleDate = selectedDate,
                )
            }
            heartAggregateDataConfidence(
                period = period,
                trackedDates = state.dailyHrv.map { it.date },
                sampleCount = state.dailyHrv.size,
                accentColor = HeartColor,
            )
            hrvStatistics(
                entries = state.dailyHrv,
                previousEntries = state.previousDailyHrv,
                baselineEntries = state.baselineDailyHrv,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
            )
            heartDailyEntries(
                entries = state.dailyHrv,
                date = { it.date },
                value = { unitFormatter.hrv(it.rmssdMs).text },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = HeartColor,
            )
        }
        !state.isLoading -> noHeartMetricData(
            titleRes = R.string.metric_hrv,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
        )
    }
}

private fun LazyListScope.bloodPressureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.bloodPressure.isNotEmpty()) {
        val sortedBloodPressure = state.bloodPressure.sortedBy { it.time }
        item {
            MetricLineChart(
                title = stringResource(R.string.metric_blood_pressure),
                series = bloodPressureSeries(
                    entries = sortedBloodPressure,
                    selectedRange = state.selectedRange,
                    systolicLabel = stringResource(R.string.vitals_entry_systolic_label),
                    diastolicLabel = stringResource(R.string.vitals_entry_diastolic_label),
                ),
                selectedRange = state.selectedRange,
                period = period,
                accentColor = VitalsColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_readings, unitFormatter.count(sortedBloodPressure.size))
                }",
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                valueFormatter = { "${it.roundToInt()} mmHg" },
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.bloodPressure,
            source = { it.source },
            time = { it.time },
            accentColor = VitalsColor,
        )
        bloodPressureContextCard(state.bloodPressure.maxByOrNull { it.time })
        bloodPressureStatistics(
            entries = state.bloodPressure,
            previousEntries = state.previousBloodPressure,
            baselineEntries = state.baselineBloodPressure,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.bloodPressure,
            value = { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_blood_pressure,
            messageRes = R.string.message_no_blood_pressure,
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
        )
    }
}
