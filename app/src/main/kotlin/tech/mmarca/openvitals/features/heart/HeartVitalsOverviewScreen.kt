package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.dataSourceEducationItem
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartVitalsOverviewScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenMetric: (HeartMetric) -> Unit,
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "heart_vitals_overview",
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.HEART_VITALS,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
        ) { period ->
            VitalsOverviewContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                onOpenMetric = onOpenMetric,
            )
        }
    }
}

fun LazyListScope.VitalsOverviewContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onOpenMetric: (HeartMetric) -> Unit,
) {
    if (state.isLoading && !state.hasOverviewData) return

    renderHeartMetricSections(sectionContext) {
        section(MetricDetailSectionId.VITALS_HEART_SECTION) {
            VitalsHeartOverviewSectionContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onOpenMetric = onOpenMetric,
            )
        }
        section(MetricDetailSectionId.VITALS_CARDIOVASCULAR_SECTION) {
            VitalsCardiovascularOverviewSectionContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onOpenMetric = onOpenMetric,
            )
        }
        section(MetricDetailSectionId.VITALS_RESPIRATORY_SECTION) {
            VitalsRespiratoryOverviewSectionContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                onOpenMetric = onOpenMetric,
            )
        }
    }
    dataSourceEducationItem()
}

@Composable
private fun VitalsHeartOverviewSectionContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onOpenMetric: (HeartMetric) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_heart))
    OverviewMetricRowsContent(
        metrics = heartOverviewMetrics(state, unitFormatter),
        onOpenMetric = onOpenMetric,
    )
    HeartOverviewChartsContent(
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        chartDaySelection = chartDaySelection,
    )
}

@Composable
private fun HeartOverviewChartsContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.selectedRange == TimeRange.DAY && state.daySamples.size > 1) {
            HeartRateTimelineCard(
                date = state.selectedDate,
                samples = state.daySamples,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = overviewMetricModifier(),
            )
        }
        if (state.selectedRange != TimeRange.DAY && state.dailySummaries.isNotEmpty()) {
            val sorted = state.dailySummaries.sortedBy { it.date }
            val rangeSummary = heartRateRangeSummary(sorted)
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
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
            )
        }
        if (state.selectedRange != TimeRange.DAY && state.dailyRestingHR.isNotEmpty()) {
            val sorted = state.dailyRestingHR.sortedBy { it.date }
            val rangeSummary = restingHeartRateRangeSummary(sorted)
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
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
            )
        }
        if (state.selectedRange != TimeRange.DAY && state.dailyHrv.isNotEmpty()) {
            val sorted = state.dailyHrv.sortedBy { it.date }
            val rangeSummary = hrvRangeSummary(sorted)
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
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.hrv(it).text },
            )
        }
    }
}

@Composable
private fun VitalsCardiovascularOverviewSectionContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onOpenMetric: (HeartMetric) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_cardiovascular))
    OverviewMetricRowsContent(
        metrics = cardiovascularOverviewMetrics(state, unitFormatter),
        onOpenMetric = onOpenMetric,
    )
    CardiovascularOverviewChartsContent(
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        chartDaySelection = chartDaySelection,
    )
}

@Composable
private fun VitalsRespiratoryOverviewSectionContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onOpenMetric: (HeartMetric) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_respiratory))
    OverviewMetricRowsContent(
        metrics = respiratoryOverviewMetrics(state, period, unitFormatter),
        onOpenMetric = onOpenMetric,
    )
    RespiratoryOverviewChartsContent(
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        chartDaySelection = chartDaySelection,
    )
}

@Composable
private fun OverviewMetricRowsContent(
    metrics: List<OverviewMetricCardData>,
    onOpenMetric: (HeartMetric) -> Unit,
) {
    metrics.chunked(2).forEach { row ->
        Row(
            modifier = overviewMetricModifier(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { metric ->
                OverviewMetricCard(
                    metric = metric,
                    onOpenMetric = onOpenMetric,
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewMetricCard(
    metric: OverviewMetricCardData,
    onOpenMetric: (HeartMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(metric.titleRes)
    val value = metric.value
    if (value == null) {
        MetricCardPlaceholder(
            title = title,
            icon = metric.icon,
            accentColor = metric.color,
            message = stringResource(R.string.message_no_readings_period),
            modifier = modifier,
            onClick = { onOpenMetric(metric.metric) },
        )
    } else {
        MetricCard(
            title = title,
            value = value.value,
            unit = value.unit,
            icon = metric.icon,
            accentColor = metric.color,
            source = metric.source,
            modifier = modifier,
            onClick = { onOpenMetric(metric.metric) },
        )
    }
}

@Composable
private fun CardiovascularOverviewChartsContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.bloodPressure.hasRenderableChartData(state.selectedRange) { it.time }) {
            val sortedBloodPressure = state.bloodPressure.sortedBy { it.time }
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
                modifier = overviewMetricModifier(),
                valueFormatter = { "${it.roundToInt()} mmHg" },
            )
        }
        if (state.spO2.hasRenderableChartData(state.selectedRange) { it.time }) {
            val sortedSpO2 = state.spO2.sortedBy { it.time }
            MetricLineChart(
                title = stringResource(R.string.metric_oxygen_saturation),
                entries = sortedSpO2,
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = oxygenColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(
                        R.string.summary_value_avg,
                        unitFormatter.percent(sortedSpO2.map { it.percent }.average()).text,
                    )
                }",
                time = { it.time },
                value = { it.percent },
                valueFormatter = { unitFormatter.percent(it).text },
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }
        if (state.vo2Max.hasRenderableChartData(state.selectedRange) { it.time }) {
            val sortedVo2Max = state.vo2Max.sortedBy { it.time }
            MetricLineChart(
                title = stringResource(R.string.metric_vo2_max),
                entries = sortedVo2Max,
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = vo2Color,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_readings, unitFormatter.count(sortedVo2Max.size))
                }",
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                time = { it.time },
                value = { it.vo2MaxMlPerKgPerMin },
                valueFormatter = { unitFormatter.vo2Max(it).text },
            )
        }
        if (state.bloodGlucose.hasRenderableChartData(state.selectedRange) { it.time }) {
            val sortedBloodGlucose = state.bloodGlucose.sortedBy { it.time }
            MetricLineChart(
                title = stringResource(R.string.metric_blood_glucose),
                entries = sortedBloodGlucose,
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = glucoseColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(
                        R.string.summary_value_avg,
                        unitFormatter.bloodGlucose(sortedBloodGlucose.map { it.millimolesPerLiter }.average()).text,
                    )
                }",
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                time = { it.time },
                value = { it.millimolesPerLiter },
                valueFormatter = { unitFormatter.bloodGlucose(it).text },
            )
        }
    }
}

@Composable
private fun RespiratoryOverviewChartsContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.respiratoryRate.hasRenderableChartData(state.selectedRange) { it.time }) {
            MetricLineChart(
                title = stringResource(R.string.metric_respiratory_rate),
                series = respiratoryRateSeries(
                    entries = state.respiratoryRate,
                    selectedRange = state.selectedRange,
                    metricLabel = stringResource(R.string.metric_respiratory_rate),
                    averageLabel = stringResource(R.string.summary_average),
                    lowestLabel = stringResource(R.string.stat_lowest),
                    highestLabel = stringResource(R.string.stat_highest),
                ),
                selectedRange = state.selectedRange,
                period = period,
                accentColor = respiratoryColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(
                        R.string.summary_value_avg,
                        unitFormatter.respiratoryRate(
                            respiratoryRateAverage(
                                respiratoryRateBuckets(state.respiratoryRate, state.selectedRange, period),
                            ),
                        ).text,
                    )
                }",
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.respiratoryRate(it).text },
            )
        }
        if (state.bodyTemperature.hasRenderableChartData(state.selectedRange) { it.time }) {
            val sortedBodyTemperature = state.bodyTemperature.sortedBy { it.time }
            MetricLineChart(
                title = stringResource(R.string.metric_body_temp),
                entries = sortedBodyTemperature,
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = temperatureColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(R.string.summary_readings, unitFormatter.count(sortedBodyTemperature.size))
                }",
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                time = { it.time },
                value = { it.temperatureCelsius },
                valueFormatter = { unitFormatter.temperature(it).text },
            )
        }
        if (state.skinTemperature.hasRenderableChartData(state.selectedRange) { it.time }) {
            val chartEntries = state.skinTemperature
                .filter { it.averageDeltaCelsius != null }
                .sortedBy { it.time }
            MetricLineChart(
                title = stringResource(R.string.metric_skin_temperature),
                entries = chartEntries,
                selectedRange = state.selectedRange,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = temperatureColor,
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${
                    stringResource(
                        R.string.summary_value_avg,
                        unitFormatter.temperatureDelta(chartEntries.mapNotNull { it.averageDeltaCelsius }.average()).text,
                    )
                }",
                modifier = overviewMetricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                time = { it.time },
                value = { it.averageDeltaCelsius ?: 0.0 },
                valueFormatter = { unitFormatter.temperatureDelta(it).text },
            )
        }
    }
}

private fun heartOverviewMetrics(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
): List<OverviewMetricCardData> =
    listOf(
        OverviewMetricCardData(
            metric = HeartMetric.AVERAGE_HEART_RATE,
            titleRes = R.string.metric_average_heart_rate,
            value = state.averageHeartRateValue(unitFormatter),
            icon = Icons.Outlined.Favorite,
            color = HeartColor,
            source = state.daySamples.sourceForDay(state.selectedRange),
        ),
        OverviewMetricCardData(
            metric = HeartMetric.RESTING_HEART_RATE,
            titleRes = R.string.metric_resting_heart_rate,
            value = state.restingHeartRateValue(unitFormatter),
            icon = Icons.Outlined.FavoriteBorder,
            color = HeartColor,
        ),
        OverviewMetricCardData(
            metric = HeartMetric.HRV,
            titleRes = R.string.metric_hrv,
            value = state.hrvValue(unitFormatter),
            icon = Icons.Outlined.Speed,
            color = HeartColor.copy(alpha = 0.85f),
        ),
    )

private fun cardiovascularOverviewMetrics(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
): List<OverviewMetricCardData> =
    listOf(
        OverviewMetricCardData(
            metric = HeartMetric.BLOOD_PRESSURE,
            titleRes = R.string.metric_blood_pressure,
            value = state.latestBloodPressure?.let {
                unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg)
            },
            icon = Icons.Outlined.Favorite,
            color = VitalsColor,
            source = state.latestBloodPressure?.source,
        ),
        OverviewMetricCardData(
            metric = HeartMetric.SPO2,
            titleRes = R.string.metric_spo2,
            value = state.latestSpO2?.let { unitFormatter.percent(it.percent) },
            icon = Icons.Outlined.Favorite,
            color = oxygenColor,
            source = state.latestSpO2?.source,
        ),
        OverviewMetricCardData(
            metric = HeartMetric.VO2_MAX,
            titleRes = R.string.metric_vo2_max,
            value = state.latestVo2Max?.let { unitFormatter.vo2Max(it.vo2MaxMlPerKgPerMin) },
            icon = Icons.Outlined.Speed,
            color = vo2Color,
            source = state.latestVo2Max?.source,
        ),
        OverviewMetricCardData(
            metric = HeartMetric.BLOOD_GLUCOSE,
            titleRes = R.string.metric_blood_glucose,
            value = state.latestBloodGlucose?.let { unitFormatter.bloodGlucose(it.millimolesPerLiter) },
            icon = Icons.Outlined.Favorite,
            color = glucoseColor,
            source = state.latestBloodGlucose?.source,
        ),
    )

private fun respiratoryOverviewMetrics(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
): List<OverviewMetricCardData> =
    listOf(
        OverviewMetricCardData(
            metric = HeartMetric.RESPIRATORY_RATE,
            titleRes = R.string.metric_respiratory_rate,
            value = state.respiratoryRateValue(period, unitFormatter),
            icon = Icons.Outlined.Air,
            color = respiratoryColor,
            source = if (state.selectedRange == TimeRange.DAY) {
                state.latestRespiratoryRate?.source
            } else {
                state.respiratoryRate.singleSource { it.source }
            },
        ),
        OverviewMetricCardData(
            metric = HeartMetric.BODY_TEMPERATURE,
            titleRes = R.string.metric_body_temp,
            value = state.latestBodyTemperature?.let { unitFormatter.temperature(it.temperatureCelsius) },
            icon = Icons.Outlined.DeviceThermostat,
            color = temperatureColor,
            source = state.latestBodyTemperature?.source,
        ),
        OverviewMetricCardData(
            metric = HeartMetric.SKIN_TEMPERATURE,
            titleRes = R.string.metric_skin_temperature,
            value = state.latestSkinTemperature?.averageDeltaCelsius?.let(unitFormatter::temperatureDelta),
            icon = Icons.Outlined.DeviceThermostat,
            color = temperatureColor,
            source = state.latestSkinTemperature?.source,
        ),
    )

private fun HeartUiState.averageHeartRateValue(unitFormatter: UnitFormatter): DisplayValue? =
    if (selectedRange == TimeRange.DAY) {
        daySamples
            .takeIf { it.isNotEmpty() }
            ?.map { it.beatsPerMinute }
            ?.average()
            ?.roundToInt()
            ?.toLong()
            ?.let(unitFormatter::heartRate)
    } else {
        dailySummaries
            .takeIf { it.isNotEmpty() }
            ?.map { it.avgBpm }
            ?.average()
            ?.roundToInt()
            ?.toLong()
            ?.let(unitFormatter::heartRate)
    }

private fun HeartUiState.restingHeartRateValue(unitFormatter: UnitFormatter): DisplayValue? =
    if (selectedRange == TimeRange.DAY) {
        dayRestingBpm?.let(unitFormatter::heartRate)
    } else {
        dailyRestingHR
            .takeIf { it.isNotEmpty() }
            ?.map { it.bpm }
            ?.average()
            ?.roundToInt()
            ?.toLong()
            ?.let(unitFormatter::heartRate)
    }

private fun HeartUiState.hrvValue(unitFormatter: UnitFormatter): DisplayValue? =
    if (selectedRange == TimeRange.DAY) {
        dayHrvMs?.let(unitFormatter::hrv)
    } else {
        dailyHrv
            .takeIf { it.isNotEmpty() }
            ?.map { it.rmssdMs }
            ?.average()
            ?.let(unitFormatter::hrv)
    }

private fun HeartUiState.respiratoryRateValue(
    period: DatePeriod,
    unitFormatter: UnitFormatter,
): DisplayValue? {
    if (respiratoryRate.isEmpty()) return null
    if (selectedRange == TimeRange.DAY) {
        return latestRespiratoryRate?.let { unitFormatter.respiratoryRate(it.breathsPerMinute) }
    }
    return respiratoryRateAverage(
        respiratoryRateBuckets(
            entries = respiratoryRate,
            selectedRange = selectedRange,
            period = period,
        )
    ).let(unitFormatter::respiratoryRate)
}

private fun List<HeartRateSample>.sourceForDay(selectedRange: TimeRange): String? =
    takeIf { selectedRange == TimeRange.DAY }
        ?.map { it.source }
        ?.distinct()
        ?.singleOrNull()

private fun <T> List<T>.singleSource(source: (T) -> String): String? =
    map(source).distinct().singleOrNull()

private fun <T> List<T>.hasRenderableChartData(
    selectedRange: TimeRange,
    time: (T) -> Instant,
): Boolean =
    if (selectedRange == TimeRange.DAY) {
        map(time).distinct().size > 1
    } else {
        isNotEmpty()
    }

private data class OverviewMetricCardData(
    val metric: HeartMetric,
    val titleRes: Int,
    val value: DisplayValue?,
    val icon: ImageVector,
    val color: Color,
    val source: String? = null,
)

private val HeartUiState.hasOverviewData: Boolean
    get() = daySamples.isNotEmpty() ||
        dailySummaries.isNotEmpty() ||
        dayRestingBpm != null ||
        dayHrvMs != null ||
        dailyRestingHR.isNotEmpty() ||
        dailyHrv.isNotEmpty() ||
        bloodPressure.isNotEmpty() ||
        spO2.isNotEmpty() ||
        respiratoryRate.isNotEmpty() ||
        bodyTemperature.isNotEmpty() ||
        vo2Max.isNotEmpty() ||
        bloodGlucose.isNotEmpty() ||
        skinTemperature.isNotEmpty()

private fun overviewMetricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
