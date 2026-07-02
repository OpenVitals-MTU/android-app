package tech.mmarca.openvitals.features.heart

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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricLineChart
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.dataSourceEducationItem
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun LazyListScope.averageHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onDecreaseHighHeartRateThreshold: () -> Unit,
    onIncreaseHighHeartRateThreshold: () -> Unit,
    onDecreaseLowHeartRateThreshold: () -> Unit,
    onIncreaseLowHeartRateThreshold: () -> Unit,
) {
    val display = state.display.metric
    when {
        display.hasDayHeartRateSamples -> {
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = null,
                intradayChart = if (display.showDayHeartRateTimeline) {
                    {
                        HeartRateTimelineCard(
                            date = state.selectedDate,
                            samples = state.daySamples,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = metricModifier(),
                        )
                    }
                } else {
                    null
                },
                highlightCard = {
                    HeartRateThresholdChecksContent(
                        state = state,
                        unitFormatter = unitFormatter,
                        onDecreaseHighHeartRateThreshold = onDecreaseHighHeartRateThreshold,
                        onIncreaseHighHeartRateThreshold = onIncreaseHighHeartRateThreshold,
                        onDecreaseLowHeartRateThreshold = onDecreaseLowHeartRateThreshold,
                        onIncreaseLowHeartRateThreshold = onIncreaseLowHeartRateThreshold,
                    )
                },
                dataConfidence = {
                    HeartRawDataConfidenceContent(
                        period = period,
                        entries = state.daySamples,
                        source = { it.source },
                        time = { it.time },
                        accentColor = HeartColor,
                    )
                },
                statistics = {
                    HeartRateSampleStatisticsContent(
                        samples = state.daySamples,
                        previousSamples = state.previousDaySamples,
                        baselineSummaries = state.baselineDailySummaries,
                        period = period,
                        selectedRange = state.selectedRange,
                        unitFormatter = unitFormatter,
                    )
                },
                entries = {
                    HeartEntryListContent(
                        entries = state.daySamples,
                        value = { unitFormatter.heartRate(it.beatsPerMinute).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                },
            )
        }
        state.selectedRange == TimeRange.DAY && !state.isLoading -> {
            item { HeartRateEmptyDayCard(modifier = metricModifier()) }
        }
        display.hasPeriodHeartRateSummaries -> {
            val sorted = display.sortedDailySummaries
            val rangeSummary = display.heartRateRangeSummary
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = chartDaySelection.selectedDate,
                periodChart = {
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
                },
                highlightCard = {
                    HeartRateThresholdChecksContent(
                        state = state,
                        unitFormatter = unitFormatter,
                        onDecreaseHighHeartRateThreshold = onDecreaseHighHeartRateThreshold,
                        onIncreaseHighHeartRateThreshold = onIncreaseHighHeartRateThreshold,
                        onDecreaseLowHeartRateThreshold = onDecreaseLowHeartRateThreshold,
                        onIncreaseLowHeartRateThreshold = onIncreaseLowHeartRateThreshold,
                    )
                },
                selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                    {
                        PaginatedEntryList(
                            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                            entries = sorted.filter { it.date == selectedDate },
                        ) { summary, rowModifier ->
                            HeartRateDayRow(
                                summary = summary,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                modifier = rowModifier,
                            )
                        }
                    }
                },
                dataConfidence = {
                    HeartAggregateDataConfidenceContent(
                        period = period,
                        trackedDates = display.heartRateTrackedDates,
                        sampleCount = display.heartRateSampleCount,
                        accentColor = HeartColor,
                    )
                },
                statistics = {
                    HeartRateSummaryStatisticsContent(
                        summaries = sorted,
                        previousSummaries = state.previousDailySummaries,
                        baselineSummaries = state.baselineDailySummaries,
                        period = period,
                        selectedRange = state.selectedRange,
                        unitFormatter = unitFormatter,
                    )
                },
                entries = {
                    PaginatedEntryList(
                        title = stringResource(R.string.section_daily_breakdown),
                        entries = sorted.sortedByDescending { it.date },
                    ) { summary, rowModifier ->
                        HeartRateDayRow(
                            summary = summary,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = rowModifier,
                        )
                    }
                },
            )
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

@Composable
internal fun HeartRateThresholdChecksContent(
    state: HeartUiState,
    unitFormatter: UnitFormatter,
    onDecreaseHighHeartRateThreshold: () -> Unit,
    onIncreaseHighHeartRateThreshold: () -> Unit,
    onDecreaseLowHeartRateThreshold: () -> Unit,
    onIncreaseLowHeartRateThreshold: () -> Unit,
) {
    Column(modifier = metricModifier()) {
        SectionHeader(
            text = stringResource(R.string.heart_rate_health_checks_title),
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
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
internal fun HeartRateThresholdCheckCard(
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

internal fun LazyListScope.restingHeartRateContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val display = state.display.metric
    when {
        display.hasDayRestingRate -> {
            val dayRestingSamples = state.dayRestingSamples.sortedBy { it.time }
            val restingBpm = state.dayRestingBpm
                ?: dayRestingSamples.map { it.beatsPerMinute }.average().roundToLong()
            val lowRestingBpm = dayRestingSamples.minOfOrNull { it.beatsPerMinute } ?: restingBpm
            val highRestingBpm = dayRestingSamples.maxOfOrNull { it.beatsPerMinute } ?: restingBpm
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = null,
                intradayChart = if (dayRestingSamples.size > 1) {
                    {
                        RestingHeartRateTimelineCard(
                            date = state.selectedDate,
                            samples = dayRestingSamples,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = metricModifier(),
                        )
                    }
                } else {
                    null
                },
                highlightCard = {
                    Column {
                        RestingHRDayCard(
                            bpm = restingBpm,
                            unitFormatter = unitFormatter,
                            modifier = metricModifier(),
                        )
                        RestingHeartRateContextCardContent(restingBpm)
                    }
                },
                dataConfidence = {
                    if (dayRestingSamples.isNotEmpty()) {
                        HeartRawDataConfidenceContent(
                            period = period,
                            entries = dayRestingSamples,
                            source = { it.source },
                            time = { it.time },
                            accentColor = HeartColor,
                        )
                    } else {
                        HeartAggregateDataConfidenceContent(
                            period = period,
                            trackedDates = display.vitalsTrackedDates,
                            sampleCount = display.vitalsSampleCount,
                            accentColor = HeartColor,
                        )
                    }
                },
                statistics = {
                    HeartNumericStatisticsContent(
                        unitFormatter = unitFormatter,
                        average = unitFormatter.heartRate(restingBpm),
                        low = unitFormatter.heartRate(lowRestingBpm),
                        high = unitFormatter.heartRate(highRestingBpm),
                        readings = dayRestingSamples.size.coerceAtLeast(1),
                        comparison = display.restingDayComparison,
                        selectedRange = state.selectedRange,
                        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
                        icon = Icons.Outlined.FavoriteBorder,
                        accentColor = HeartColor,
                        period = period,
                        baselineCurrentValue = restingBpm.toDouble(),
                        baselineValues = display.restingBaselineValues,
                    )
                },
                entries = {
                    if (dayRestingSamples.isNotEmpty()) {
                        HeartEntryListContent(
                            entries = dayRestingSamples,
                            value = { unitFormatter.heartRate(it.beatsPerMinute).text },
                            source = { it.source },
                            time = { it.time },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                        )
                    } else {
                        HeartDailyEntryListContent(
                            entries = listOf(DailyRestingHR(state.selectedDate, restingBpm)),
                            date = { it.date },
                            value = { unitFormatter.heartRate(it.bpm).text },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            accentColor = HeartColor,
                        )
                    }
                },
            )
        }
        display.hasPeriodRestingRate -> {
            val sorted = state.dailyRestingHR.sortedBy { it.date }
            val rangeSummary = display.restingRangeSummary!!
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = chartDaySelection.selectedDate,
                periodChart = {
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
                },
                selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                    {
                        HeartDailyEntryListContent(
                            entries = sorted.filter { it.date == selectedDate },
                            date = { it.date },
                            value = { unitFormatter.heartRate(it.bpm).text },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            accentColor = HeartColor,
                            titleDate = selectedDate,
                        )
                    }
                },
                dataConfidence = {
                    HeartAggregateDataConfidenceContent(
                        period = period,
                        trackedDates = display.vitalsTrackedDates,
                        sampleCount = display.vitalsSampleCount,
                        accentColor = HeartColor,
                    )
                },
                contextInsight = display.restingPeriodAverageBpm?.let { averageBpm ->
                    { RestingHeartRateContextCardContent(averageBpm) }
                },
                statistics = {
                    RestingHeartRateStatisticsContent(
                        entries = sorted,
                        previousEntries = state.previousDailyRestingHR,
                        baselineEntries = state.baselineDailyRestingHR,
                        period = period,
                        selectedRange = state.selectedRange,
                        unitFormatter = unitFormatter,
                    )
                },
                entries = {
                    HeartDailyEntryListContent(
                        entries = sorted,
                        date = { it.date },
                        value = { unitFormatter.heartRate(it.bpm).text },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = HeartColor,
                    )
                },
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

internal fun LazyListScope.hrvContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val display = state.display.metric
    when {
        display.hasDayHrv -> {
            val dayHrvSamples = state.dayHrvSamples.sortedBy { it.time }
            val hrvMs = state.dayHrvMs ?: dayHrvSamples.map { it.rmssdMs }.average()
            val lowHrvMs = dayHrvSamples.minOfOrNull { it.rmssdMs } ?: hrvMs
            val highHrvMs = dayHrvSamples.maxOfOrNull { it.rmssdMs } ?: hrvMs
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = null,
                intradayChart = if (dayHrvSamples.size > 1) {
                    {
                        HrvTimelineCard(
                            date = state.selectedDate,
                            samples = dayHrvSamples,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = metricModifier(),
                        )
                    }
                } else {
                    null
                },
                highlightCard = {
                    HRVDayCard(
                        rmssdMs = hrvMs,
                        unitFormatter = unitFormatter,
                        modifier = metricModifier(),
                    )
                },
                dataConfidence = {
                    if (dayHrvSamples.isNotEmpty()) {
                        HeartRawDataConfidenceContent(
                            period = period,
                            entries = dayHrvSamples,
                            source = { it.source },
                            time = { it.time },
                            accentColor = HeartColor,
                        )
                    } else {
                        HeartAggregateDataConfidenceContent(
                            period = period,
                            trackedDates = display.vitalsTrackedDates,
                            sampleCount = display.vitalsSampleCount,
                            accentColor = HeartColor,
                        )
                    }
                },
                statistics = {
                    HeartNumericStatisticsContent(
                        unitFormatter = unitFormatter,
                        average = unitFormatter.hrv(hrvMs),
                        low = unitFormatter.hrv(lowHrvMs),
                        high = unitFormatter.hrv(highHrvMs),
                        readings = dayHrvSamples.size.coerceAtLeast(1),
                        comparison = display.hrvDayComparison,
                        selectedRange = state.selectedRange,
                        comparisonValueFormatter = { unitFormatter.hrv(it) },
                        icon = Icons.Outlined.FavoriteBorder,
                        accentColor = HeartColor,
                        period = period,
                        baselineCurrentValue = hrvMs,
                        baselineValues = display.hrvBaselineValues,
                    )
                },
                entries = {
                    if (dayHrvSamples.isNotEmpty()) {
                        HeartEntryListContent(
                            entries = dayHrvSamples,
                            value = { unitFormatter.hrv(it.rmssdMs).text },
                            source = { it.source },
                            time = { it.time },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                        )
                    } else {
                        HeartDailyEntryListContent(
                            entries = listOf(DailyHrv(state.selectedDate, hrvMs)),
                            date = { it.date },
                            value = { unitFormatter.hrv(it.rmssdMs).text },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            accentColor = HeartColor,
                        )
                    }
                },
            )
        }
        display.hasPeriodHrv -> {
            val sorted = state.dailyHrv.sortedBy { it.date }
            val rangeSummary = display.hrvRangeSummary!!
            renderChartMetricSections(
                sectionContext = sectionContext,
                selectedRange = state.selectedRange,
                period = period,
                selectedDate = chartDaySelection.selectedDate,
                periodChart = {
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
                },
                selectedDayEntries = chartDaySelection.selectedDate?.let { selectedDate ->
                    {
                        HeartDailyEntryListContent(
                            entries = sorted.filter { it.date == selectedDate },
                            date = { it.date },
                            value = { unitFormatter.hrv(it.rmssdMs).text },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            accentColor = HeartColor,
                            titleDate = selectedDate,
                        )
                    }
                },
                dataConfidence = {
                    HeartAggregateDataConfidenceContent(
                        period = period,
                        trackedDates = display.vitalsTrackedDates,
                        sampleCount = display.vitalsSampleCount,
                        accentColor = HeartColor,
                    )
                },
                statistics = {
                    HrvStatisticsContent(
                        entries = sorted,
                        previousEntries = state.previousDailyHrv,
                        baselineEntries = state.baselineDailyHrv,
                        period = period,
                        selectedRange = state.selectedRange,
                        unitFormatter = unitFormatter,
                    )
                },
                entries = {
                    HeartDailyEntryListContent(
                        entries = sorted,
                        date = { it.date },
                        value = { unitFormatter.hrv(it.rmssdMs).text },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = HeartColor,
                    )
                },
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

internal fun LazyListScope.bloodPressureContent(
    state: HeartUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    sectionContext: MetricDetailSectionContext,
    onEditVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
    onDeleteVitalsMeasurement: (VitalsMeasurementType, String) -> Unit,
) {
    val display = state.display.metric
    if (display.hasVitalsEntries) {
        val sortedBloodPressure = state.bloodPressure.sortedBy { it.time }
        renderChartMetricSections(
            sectionContext = sectionContext,
            selectedRange = state.selectedRange,
            period = period,
            selectedDate = null,
            periodChart = {
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
                        stringResource(R.string.summary_readings, unitFormatter.count(display.vitalsSampleCount))
                    }",
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    valueFormatter = { "${it.roundToInt()} mmHg" },
                )
            },
            dataConfidence = {
                HeartRawDataConfidenceContent(
                    period = period,
                    entries = state.bloodPressure,
                    source = { it.source },
                    time = { it.time },
                    accentColor = VitalsColor,
                )
            },
            contextInsight = {
                BloodPressureContextCardContent(state.bloodPressure.maxByOrNull { it.time })
            },
            statistics = {
                BloodPressureStatisticsContent(
                    entries = state.bloodPressure,
                    previousEntries = state.previousBloodPressure,
                    baselineEntries = state.baselineBloodPressure,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                )
            },
            entries = {
                HeartEntryListContent(
                    entries = state.bloodPressure,
                    value = { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).text },
                    source = { it.source },
                    time = { it.time },
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
                    onDelete = { onDeleteVitalsMeasurement(VitalsMeasurementType.BLOOD_PRESSURE, it.id) },
                )
            },
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
