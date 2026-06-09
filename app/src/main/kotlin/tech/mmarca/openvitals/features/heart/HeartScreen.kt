package tech.mmarca.openvitals.features.heart

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.BloodPressureCategory
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.insights.PeriodComparison
import tech.mmarca.openvitals.core.insights.VitalContextInterpretation
import tech.mmarca.openvitals.core.insights.VitalContextStatus
import tech.mmarca.openvitals.core.insights.bloodPressureInterpretation
import tech.mmarca.openvitals.core.insights.bodyTemperatureContext
import tech.mmarca.openvitals.core.insights.dataConfidence
import tech.mmarca.openvitals.core.insights.oxygenSaturationContext
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.insights.respiratoryRateContext
import tech.mmarca.openvitals.core.insights.restingHeartRateContext
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BloodGlucoseEntry
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SkinTemperatureEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

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
    val requestVitalsPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onVitalsPermissionsResult(granted)
    }

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
            HeartMetric.BLOOD_PRESSURE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                bloodPressureContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
            }
            HeartMetric.SPO2 -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                spO2Content(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
            }
            HeartMetric.VO2_MAX -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                vo2MaxContent(state, period, unitFormatter, dateTimeFormatterProvider)
            }
            HeartMetric.RESPIRATORY_RATE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                respiratoryRateContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    chartDaySelection,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
            }
            HeartMetric.BODY_TEMPERATURE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                bodyTemperatureContent(
                    state,
                    period,
                    unitFormatter,
                    dateTimeFormatterProvider,
                    onEditVitalsMeasurement,
                    viewModel::deleteVitalsMeasurementEntry,
                )
            }
            HeartMetric.BLOOD_GLUCOSE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                bloodGlucoseContent(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    chartDaySelection = chartDaySelection,
                )
            }
            HeartMetric.SKIN_TEMPERATURE -> vitalsMetricContent(
                state = state,
                phase3Permissions = viewModel.vitalsPermissions,
                onGrantPermissions = requestVitalsPermissions::launch,
            ) {
                skinTemperatureContent(
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
            item {
                HeartRateChart(
                    summaries = state.dailySummaries,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                IconButton(
                    onClick = onDecreaseThreshold,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_hr_threshold),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
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
            item {
                RestingHRChart(
                    entries = state.dailyRestingHR,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
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
            item {
                HRVChart(
                    entries = state.dailyHrv,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
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

private fun LazyListScope.vitalsMetricContent(
    state: HeartUiState,
    phase3Permissions: Set<String>,
    onGrantPermissions: (Set<String>) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    if (state.missingVitalsPermissions.isNotEmpty()) {
        item {
            PermissionCallout(
                title = stringResource(R.string.vitals_permissions_needed_title),
                body = stringResource(R.string.vitals_permissions_needed_body),
                onGrant = { onGrantPermissions(phase3Permissions) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    content()
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
        item {
            BloodPressureChart(
                entries = state.bloodPressure,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
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

private fun LazyListScope.spO2Content(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.spO2.isNotEmpty()) {
        val sorted = state.spO2.sortedBy { it.time }
        item {
            VitalsLineChart(
                title = stringResource(R.string.metric_oxygen_saturation),
                points = rawVitalsPoints(
                    entries = sorted,
                    time = { it.time },
                    value = { it.percent },
                ),
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = oxygenColor,
                summary = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_value_avg, unitFormatter.percent(state.spO2.map { it.percent }.average()).text)
                }",
                valueFormatter = { unitFormatter.percent(it).text },
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
                entries = state.spO2.filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate },
                value = { unitFormatter.percent(it.percent).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                titleDate = selectedDate,
                editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
                onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.spO2,
            source = { it.source },
            time = { it.time },
            accentColor = oxygenColor,
        )
        oxygenSaturationContextCard(state.spO2.maxByOrNull { it.time })
        spO2Statistics(
            entries = state.spO2,
            previousEntries = state.previousSpO2,
            baselineEntries = state.baselineSpO2,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.spO2,
            value = { unitFormatter.percent(it.percent).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.SPO2, it.id) },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_spo2,
            messageRes = R.string.message_no_oxygen,
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = oxygenColor,
        )
    }
}

private fun LazyListScope.vo2MaxContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val latest = state.latestVo2Max
    if (latest != null) {
        if (state.vo2Max.size > 1) {
            item {
                Vo2MaxChart(
                    entries = state.vo2Max,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                )
            }
        }
        item {
            val value = unitFormatter.vo2Max(latest.vo2MaxMlPerKgPerMin)
            MetricCard(
                title = stringResource(R.string.metric_vo2_max),
                value = value.value,
                unit = value.unit,
                icon = Icons.Outlined.Speed,
                accentColor = vo2Color,
                source = latest.source,
                modifier = metricModifier(),
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.vo2Max,
            source = { it.source },
            time = { it.time },
            accentColor = vo2Color,
        )
        vo2MaxStatistics(
            entries = state.vo2Max,
            previousEntries = state.previousVo2Max,
            baselineEntries = state.baselineVo2Max,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.vo2Max,
            value = { unitFormatter.vo2Max(it.vo2MaxMlPerKgPerMin).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_vo2_max,
            messageRes = R.string.message_no_vo2_max,
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = vo2Color,
        )
    }
}

private fun LazyListScope.respiratoryRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.respiratoryRate.isNotEmpty()) {
        item {
            RespiratoryRateChart(
                entries = state.respiratoryRate,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        if (state.selectedRange == TimeRange.DAY) {
            item {
                SimpleVitalsList(
                    title = stringResource(R.string.vitals_respiratory_rate_readings),
                    entries = state.respiratoryRate,
                    value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                    onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                )
            }
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
                entries = state.respiratoryRate.filter {
                    it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                },
                value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                titleDate = selectedDate,
                editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
                onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.respiratoryRate,
            source = { it.source },
            time = { it.time },
            accentColor = respiratoryColor,
        )
        respiratoryRateContextCard(state.respiratoryRate.map { it.breathsPerMinute }.average())
        respiratoryRateStatistics(
            entries = state.respiratoryRate,
            previousEntries = state.previousRespiratoryRate,
            baselineEntries = state.baselineRespiratoryRate,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        if (state.selectedRange != TimeRange.DAY) {
            item {
                PaginatedEntryList(
                    title = stringResource(R.string.section_respiratory_rate_daily_breakdown),
                    entries = respiratoryRateDaySummaries(state.respiratoryRate).sortedByDescending { it.date },
                ) { summary, rowModifier ->
                    RespiratoryRateDayRow(
                        summary = summary,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = rowModifier,
                    )
                }
            }
        }
        heartEntryRows(
            entries = state.respiratoryRate,
            value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.RESPIRATORY_RATE, it.id) },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_respiratory_rate,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.Favorite,
            accentColor = respiratoryColor,
        )
    }
}

private fun LazyListScope.bodyTemperatureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    if (state.bodyTemperature.isNotEmpty()) {
        item {
            BodyTemperatureChart(
                entries = state.bodyTemperature,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
            )
        }
        item {
            SimpleVitalsList(
                title = stringResource(R.string.vitals_body_temperature_readings),
                entries = state.bodyTemperature,
                value = { unitFormatter.temperature(it.temperatureCelsius).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
                onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.bodyTemperature,
            source = { it.source },
            time = { it.time },
            accentColor = temperatureColor,
        )
        bodyTemperatureContextCard(state.bodyTemperature.maxByOrNull { it.time })
        bodyTemperatureStatistics(
            entries = state.bodyTemperature,
            previousEntries = state.previousBodyTemperature,
            baselineEntries = state.baselineBodyTemperature,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.bodyTemperature,
            value = { unitFormatter.temperature(it.temperatureCelsius).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
            onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BODY_TEMPERATURE, it.id) },
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_body_temp,
            messageRes = R.string.message_no_readings_period,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        )
    }
}

private fun LazyListScope.bloodGlucoseContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    if (state.bloodGlucose.isNotEmpty()) {
        item {
            BloodGlucoseChart(
                entries = state.bloodGlucose,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
                entries = state.bloodGlucose.filter {
                    it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                },
                value = { unitFormatter.bloodGlucose(it.millimolesPerLiter).text },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                titleDate = selectedDate,
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.bloodGlucose,
            source = { it.source },
            time = { it.time },
            accentColor = glucoseColor,
        )
        bloodGlucoseStatistics(
            entries = state.bloodGlucose,
            previousEntries = state.previousBloodGlucose,
            baselineEntries = state.baselineBloodGlucose,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.bloodGlucose,
            value = { unitFormatter.bloodGlucose(it.millimolesPerLiter).text },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_blood_glucose,
            messageRes = R.string.message_no_blood_glucose,
            icon = Icons.Outlined.Favorite,
            accentColor = glucoseColor,
        )
    }
}

private fun LazyListScope.skinTemperatureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    if (state.skinTemperature.isNotEmpty()) {
        item {
            SkinTemperatureChart(
                entries = state.skinTemperature,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            heartEntryRows(
                entries = state.skinTemperature.filter {
                    it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                },
                value = { it.skinTemperatureValue(unitFormatter) },
                source = { it.source },
                time = { it.time },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                titleDate = selectedDate,
            )
        }
        heartRawDataConfidence(
            period = period,
            entries = state.skinTemperature,
            source = { it.source },
            time = { it.time },
            accentColor = temperatureColor,
        )
        skinTemperatureStatistics(
            entries = state.skinTemperature,
            previousEntries = state.previousSkinTemperature,
            baselineEntries = state.baselineSkinTemperature,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        heartEntryRows(
            entries = state.skinTemperature,
            value = { it.skinTemperatureValue(unitFormatter) },
            source = { it.source },
            time = { it.time },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    } else if (!state.isLoading) {
        noHeartMetricData(
            titleRes = R.string.metric_skin_temperature,
            messageRes = R.string.message_no_skin_temperature,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        )
    }
}

private fun LazyListScope.heartAggregateDataConfidence(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = trackedDates,
                sampleCount = sampleCount,
                valueKind = DataValueKind.AGGREGATED,
            ),
            accentColor = accentColor,
            modifier = metricModifier(),
        )
    }
}

private fun <T> LazyListScope.heartRawDataConfidence(
    period: DatePeriod,
    entries: List<T>,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
        val zone = ZoneId.systemDefault()
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = entries.map { time(it).atZone(zone).toLocalDate() },
                sampleCount = entries.size,
                sources = entries.map(source),
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = accentColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.bloodPressureContextCard(entry: BloodPressureEntry?) {
    val interpretation = entry
        ?.let { bloodPressureInterpretation(it.systolicMmHg, it.diastolicMmHg) }
        ?: return
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        val status = bloodPressureCategoryText(interpretation.category)
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_bp_title),
            status = status,
            body = if (interpretation.category == BloodPressureCategory.SEVERE_REFERENCE) {
                stringResource(R.string.interpretation_bp_severe_body)
            } else {
                stringResource(R.string.interpretation_bp_body, status)
            },
            source = stringResource(R.string.interpretation_bp_source),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.restingHeartRateContextCard(bpm: Long) {
    val interpretation = restingHeartRateContext(bpm) ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_resting_hr_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
    )
}

private fun LazyListScope.oxygenSaturationContextCard(entry: SpO2Entry?) {
    val interpretation = entry?.let { oxygenSaturationContext(it.percent) } ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_oxygen_body,
        sourceRes = R.string.interpretation_oxygen_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
    )
}

private fun LazyListScope.respiratoryRateContextCard(breathsPerMinute: Double) {
    val interpretation = respiratoryRateContext(breathsPerMinute) ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_respiratory_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
    )
}

private fun LazyListScope.bodyTemperatureContextCard(entry: BodyTempEntry?) {
    val interpretation = entry?.let { bodyTemperatureContext(it.temperatureCelsius) } ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_temperature_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
    )
}

private fun LazyListScope.vitalContextCard(
    interpretation: VitalContextInterpretation,
    bodyRes: Int,
    sourceRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_vital_title),
            status = vitalContextStatusText(interpretation.status),
            body = stringResource(bodyRes),
            source = stringResource(sourceRes),
            icon = icon,
            accentColor = accentColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
    }
}

@Composable
private fun bloodPressureCategoryText(category: BloodPressureCategory): String =
    when (category) {
        BloodPressureCategory.NORMAL -> stringResource(R.string.interpretation_bp_normal)
        BloodPressureCategory.ELEVATED -> stringResource(R.string.interpretation_bp_elevated)
        BloodPressureCategory.STAGE_1 -> stringResource(R.string.interpretation_bp_stage_1)
        BloodPressureCategory.STAGE_2 -> stringResource(R.string.interpretation_bp_stage_2)
        BloodPressureCategory.SEVERE_REFERENCE -> stringResource(R.string.interpretation_bp_severe)
    }

@Composable
private fun vitalContextStatusText(status: VitalContextStatus): String =
    when (status) {
        VitalContextStatus.WITHIN_REFERENCE -> stringResource(R.string.interpretation_vital_within)
        VitalContextStatus.BELOW_REFERENCE -> stringResource(R.string.interpretation_vital_below)
        VitalContextStatus.ABOVE_REFERENCE -> stringResource(R.string.interpretation_vital_above)
        VitalContextStatus.BELOW_TYPICAL_OXYGEN ->
            stringResource(R.string.interpretation_vital_oxygen_below_typical)
        VitalContextStatus.LOW_OXYGEN_REFERENCE -> stringResource(R.string.interpretation_vital_oxygen_low)
        VitalContextStatus.VERY_LOW_OXYGEN_REFERENCE ->
            stringResource(R.string.interpretation_vital_oxygen_very_low)
    }

private fun LazyListScope.heartRateSampleStatistics(
    samples: List<HeartRateSample>,
    previousSamples: List<HeartRateSample>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = samples.map { it.beatsPerMinute }
    val previousValues = previousSamples.map { it.beatsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(values.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(values.minOrNull() ?: 0L),
        high = unitFormatter.heartRate(values.maxOrNull() ?: 0L),
        readings = samples.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

private fun LazyListScope.heartRateSummaryStatistics(
    summaries: List<HeartRateSummary>,
    previousSummaries: List<HeartRateSummary>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(summaries.map { it.avgBpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(summaries.minOfOrNull { it.minBpm } ?: 0L),
        high = unitFormatter.heartRate(summaries.maxOfOrNull { it.maxBpm } ?: 0L),
        readings = summaries.size,
        comparison = previousSummaries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = summaries.map { summary -> summary.avgBpm }.average(),
                previousValue = it.map { summary -> summary.avgBpm }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = summaries.map { it.avgBpm }.average(),
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

private fun LazyListScope.restingHeartRateStatistics(
    entries: List<DailyRestingHR>,
    previousEntries: List<DailyRestingHR>,
    baselineEntries: List<DailyRestingHR>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(entries.map { it.bpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(entries.minOfOrNull { it.bpm } ?: 0L),
        high = unitFormatter.heartRate(entries.maxOfOrNull { it.bpm } ?: 0L),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.bpm }.average(),
                previousValue = it.map { entry -> entry.bpm }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = entries.map { it.bpm }.average(),
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.bpm.toDouble()) },
    )
}

private fun LazyListScope.hrvStatistics(
    entries: List<DailyHrv>,
    previousEntries: List<DailyHrv>,
    baselineEntries: List<DailyHrv>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.hrv(entries.map { it.rmssdMs }.average()),
        low = unitFormatter.hrv(entries.minOfOrNull { it.rmssdMs } ?: 0.0),
        high = unitFormatter.hrv(entries.maxOfOrNull { it.rmssdMs } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.rmssdMs }.average(),
                previousValue = it.map { entry -> entry.rmssdMs }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.hrv(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = entries.map { it.rmssdMs }.average(),
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.rmssdMs) },
    )
}

private fun LazyListScope.bloodPressureStatistics(
    entries: List<BloodPressureEntry>,
    previousEntries: List<BloodPressureEntry>,
    baselineEntries: List<BloodPressureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val latest = entries.maxByOrNull { it.time }
        val average = unitFormatter.bloodPressure(
            entries.map { it.systolicMmHg }.average().roundToInt(),
            entries.map { it.diastolicMmHg }.average().roundToInt(),
        )
        val highest = entries
            .maxWithOrNull(compareBy<BloodPressureEntry> { it.systolicMmHg }.thenBy { it.diastolicMmHg })
            ?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg) }
            ?: unitFormatter.bloodPressure(0, 0)
        val previousAverageSystolic = previousEntries.takeIf { it.isNotEmpty() }
            ?.map { it.systolicMmHg }
            ?.average()

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_latest),
                    value = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).value }.orEmpty(),
                    unit = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).unit }.orEmpty(),
                    icon = Icons.Outlined.Favorite,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = highest.value,
                    unit = highest.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_readings),
                    value = unitFormatter.count(entries.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = VitalsColor,
                ),
            ) + previousAverageSystolic?.let {
                listOf(
                    previousPeriodInsightStat(
                        comparison = periodComparison(
                            currentValue = entries.map { entry -> entry.systolicMmHg }.average(),
                            previousValue = it,
                        ),
                        selectedRange = selectedRange,
                        unitFormatter = unitFormatter,
                        valueFormatter = { value -> DisplayValue(unitFormatter.count(value.roundToInt()), "mmHg") },
                        accentColor = VitalsColor,
                    )
                )
            }.orEmpty() + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = entries.map { entry -> entry.systolicMmHg }.average(),
                    values = baselineEntries.map { it.systolicBaselineValue() },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { value -> DisplayValue(unitFormatter.count(value.roundToInt()), "mmHg") },
                accentColor = VitalsColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.spO2Statistics(
    entries: List<SpO2Entry>,
    previousEntries: List<SpO2Entry>,
    baselineEntries: List<SpO2Entry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.percent(entries.map { it.percent }.average()),
        low = unitFormatter.percent(entries.minOfOrNull { it.percent } ?: 0.0),
        high = unitFormatter.percent(entries.maxOfOrNull { it.percent } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.percent }.average(),
                previousValue = it.map { entry -> entry.percent }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.percent(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
        period = period,
        baselineCurrentValue = entries.map { it.percent }.average(),
        baselineValues = baselineEntries.map { it.spO2BaselineValue() },
    )
}

private fun LazyListScope.vo2MaxStatistics(
    entries: List<Vo2MaxEntry>,
    previousEntries: List<Vo2MaxEntry>,
    baselineEntries: List<Vo2MaxEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.vo2Max(entries.map { it.vo2MaxMlPerKgPerMin }.average()),
        low = unitFormatter.vo2Max(entries.minOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        high = unitFormatter.vo2Max(entries.maxOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.vo2MaxMlPerKgPerMin }.average(),
                previousValue = it.map { entry -> entry.vo2MaxMlPerKgPerMin }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.vo2Max(it) },
        icon = Icons.Outlined.Speed,
        accentColor = vo2Color,
        period = period,
        baselineCurrentValue = entries.map { it.vo2MaxMlPerKgPerMin }.average(),
        baselineValues = baselineEntries.map { it.vo2BaselineValue() },
    )
}

private fun LazyListScope.respiratoryRateStatistics(
    entries: List<RespiratoryRateEntry>,
    previousEntries: List<RespiratoryRateEntry>,
    baselineEntries: List<RespiratoryRateEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.breathsPerMinute }
    val previousValues = previousEntries.map { it.breathsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.respiratoryRate(values.average()),
        low = unitFormatter.respiratoryRate(values.minOrNull() ?: 0.0),
        high = unitFormatter.respiratoryRate(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.respiratoryRate(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.respiratoryRateBaselineValue() },
    )
}

private fun LazyListScope.bodyTemperatureStatistics(
    entries: List<BodyTempEntry>,
    previousEntries: List<BodyTempEntry>,
    baselineEntries: List<BodyTempEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.temperatureCelsius }
    val previousValues = previousEntries.map { it.temperatureCelsius }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperature(values.average()),
        low = unitFormatter.temperature(values.minOrNull() ?: 0.0),
        high = unitFormatter.temperature(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperature(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.bodyTemperatureBaselineValue() },
    )
}

private fun LazyListScope.bloodGlucoseStatistics(
    entries: List<BloodGlucoseEntry>,
    previousEntries: List<BloodGlucoseEntry>,
    baselineEntries: List<BloodGlucoseEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.millimolesPerLiter }
    val previousValues = previousEntries.map { it.millimolesPerLiter }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.bloodGlucose(values.average()),
        low = unitFormatter.bloodGlucose(values.minOrNull() ?: 0.0),
        high = unitFormatter.bloodGlucose(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.bloodGlucose(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = glucoseColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.bloodGlucoseBaselineValue() },
    )
}

private fun LazyListScope.skinTemperatureStatistics(
    entries: List<SkinTemperatureEntry>,
    previousEntries: List<SkinTemperatureEntry>,
    baselineEntries: List<SkinTemperatureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.mapNotNull { it.averageDeltaCelsius }
    if (values.isEmpty()) return
    val previousValues = previousEntries.mapNotNull { it.averageDeltaCelsius }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperatureDelta(values.average()),
        low = unitFormatter.temperatureDelta(values.minOrNull() ?: 0.0),
        high = unitFormatter.temperatureDelta(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperatureDelta(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.mapNotNull { it.skinTemperatureBaselineValue() },
    )
}

private fun LazyListScope.heartNumericStatistics(
    unitFormatter: UnitFormatter,
    average: DisplayValue,
    low: DisplayValue,
    high: DisplayValue,
    readings: Int,
    comparison: PeriodComparison? = null,
    selectedRange: TimeRange,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    countTitleRes: Int = R.string.stat_readings,
    countUnitRes: Int? = null,
    period: DatePeriod? = null,
    baselineCurrentValue: Double? = null,
    baselineValues: List<BaselineValue> = emptyList(),
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = icon,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_lowest),
                    value = low.value,
                    unit = low.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = high.value,
                    unit = high.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(countTitleRes),
                    value = unitFormatter.count(readings),
                    unit = countUnitRes?.let { stringResource(it) }.orEmpty(),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = accentColor,
                ),
            ) + comparison?.let {
                listOf(
                    previousPeriodInsightStat(
                        comparison = it,
                        selectedRange = selectedRange,
                        unitFormatter = unitFormatter,
                        valueFormatter = comparisonValueFormatter,
                        accentColor = accentColor,
                    )
                )
            }.orEmpty() + if (period != null && baselineCurrentValue != null) {
                personalBaselineInsightStats(
                    insight = personalBaselineInsight(
                        currentValue = baselineCurrentValue,
                        values = baselineValues,
                        referenceDate = period.start.minusDays(1),
                    ),
                    unitFormatter = unitFormatter,
                    valueFormatter = comparisonValueFormatter,
                    accentColor = accentColor,
                )
            } else {
                emptyList()
            },
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.noHeartMetricData(
    titleRes: Int,
    messageRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = metricModifier(),
        )
    }
}

private fun <T> LazyListScope.heartEntryRows(
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    if (entries.isEmpty()) return

    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = entries.sortedByDescending(time),
        ) { entry, rowModifier ->
            VitalsReadingRow(
                label = value(entry),
                source = source(entry),
                time = time(entry).atZone(ZoneId.systemDefault()),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit
                    ?.takeIf { editable(entry) }
                    ?.let { edit -> { edit(entry) } },
                onDelete = onDelete
                    ?.takeIf { editable(entry) }
                    ?.let { delete -> { delete(entry) } },
                modifier = rowModifier,
            )
        }
    }
}

private fun <T> LazyListScope.heartDailyEntries(
    entries: List<T>,
    date: (T) -> LocalDate,
    value: (T) -> String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    titleDate: LocalDate? = null,
) {
    if (entries.isEmpty()) return

    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = entries.sortedByDescending(date),
        ) { entry, rowModifier ->
            HeartDailyEntryRow(
                date = date(entry),
                value = value(entry),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = accentColor,
                modifier = rowModifier,
            )
        }
    }
}

@Composable
private fun HeartDailyEntryRow(
    date: LocalDate,
    value: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(date),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
            )
        }
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

private fun BloodPressureEntry.systolicBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = systolicMmHg.toDouble(),
    )

private fun SpO2Entry.spO2BaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = percent,
    )

private fun RespiratoryRateEntry.respiratoryRateBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = breathsPerMinute,
    )

private fun BodyTempEntry.bodyTemperatureBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = temperatureCelsius,
    )

private fun Vo2MaxEntry.vo2BaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = vo2MaxMlPerKgPerMin,
    )

private fun BloodGlucoseEntry.bloodGlucoseBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = millimolesPerLiter,
    )

private fun SkinTemperatureEntry.skinTemperatureBaselineValue(): BaselineValue? =
    averageDeltaCelsius?.let { delta ->
        BaselineValue(
            date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
            value = delta,
        )
    }

private fun SkinTemperatureEntry.skinTemperatureValue(unitFormatter: UnitFormatter): String =
    averageDeltaCelsius
        ?.let { unitFormatter.temperatureDelta(it).text }
        ?: baselineCelsius?.let { unitFormatter.temperature(it).text }
        ?: ""
