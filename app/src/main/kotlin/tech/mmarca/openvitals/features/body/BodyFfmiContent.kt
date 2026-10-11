package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.theme.BodyFatColor

/**
 * The FFMI screen. The headline is the dashboard tile's value, from the latest weight, body fat
 * and height on record. The chart, statistics and entries are the period's [FfmiReading]s.
 */
internal fun LazyListScope.ffmiContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val latest = state.display.ffmiReadings.lastOrNull()
    if (latest == null) {
        // No body fat in the period: the latest on record, as the other body screens fall back.
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_ffmi,
            value = state.summary.adjustedFfmi?.let { ffmiDisplay(it, unitFormatter) },
            icon = Icons.Outlined.FitnessCenter,
            accentColor = BodyFatColor,
            unitFormatter = unitFormatter,
            extraStatistics = { FfmiContextCardContent(state, unitFormatter) },
            entryCount = 0,
        )
        return
    }
    ffmiReadingsContent(
        state = state,
        latest = latest,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        chartDaySelection = chartDaySelection,
        sectionContext = sectionContext,
    )
}

private fun LazyListScope.ffmiReadingsContent(
    state: BodyUiState,
    latest: FfmiReading,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
) {
    val readings = state.display.ffmiReadings
    val metricData = state.metricDataFor(BodyMetric.FFMI, unitFormatter)
    renderBodyMetricOrderedContent(
        BodyMetricOrderedContentSpec(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            summaryContent = {
                val value = ffmiDisplay(state.summary.adjustedFfmi ?: latest.adjustedFfmi, unitFormatter)
                MetricCard(
                    title = stringResource(R.string.metric_ffmi),
                    value = value.value,
                    unit = value.unit,
                    icon = Icons.Outlined.FitnessCenter,
                    accentColor = BodyFatColor,
                    modifier = metricModifier(),
                )
            },
            periodChart = {
                BodyPeriodMetricChart(
                    metricData = metricData,
                    selectedRange = state.selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    modifier = metricModifier(),
                )
            },
            selectedDayEntries = {
                FfmiReadingEntriesContent(
                    readings = readings,
                    chartDaySelection = chartDaySelection,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = true,
                )
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = readings,
                    source = { it.source },
                    time = { it.time },
                    accentColor = BodyFatColor,
                    valueKind = DataValueKind.CALCULATED,
                )
            },
            statistics = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FfmiStatisticsContent(state, latest, period, unitFormatter)
                    FfmiContextCardContent(state, unitFormatter)
                }
            },
            entries = {
                FfmiReadingEntriesContent(
                    readings = readings,
                    chartDaySelection = chartDaySelection,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = false,
                )
            },
        ),
    )
}

@Composable
private fun FfmiStatisticsContent(
    state: BodyUiState,
    latest: FfmiReading,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
) {
    val values = state.display.ffmiReadings.map { it.adjustedFfmi }
    BodyNumericStatisticsContent(
        latest = ffmiDisplay(latest.adjustedFfmi, unitFormatter),
        average = ffmiDisplay(values.average(), unitFormatter),
        low = ffmiDisplay(values.min(), unitFormatter),
        high = ffmiDisplay(values.max(), unitFormatter),
        readings = values.size,
        comparison = state.display.previousFfmiReadings.lastOrNull()?.let { previous ->
            periodComparison(currentValue = latest.adjustedFfmi, previousValue = previous.adjustedFfmi)
        },
        selectedRange = state.selectedRange,
        comparisonValueFormatter = { ffmiDisplay(it, unitFormatter) },
        icon = Icons.Outlined.FitnessCenter,
        accentColor = BodyFatColor,
        unitFormatter = unitFormatter,
        period = period,
        baselineCurrentValue = latest.adjustedFfmi,
        baselineValues = state.display.baselineFfmiReadings.map { it.ffmiBaselineValue() },
    )
}

/** The category card, for the same latest values as the headline. */
@Composable
private fun FfmiContextCardContent(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
) {
    BmiContextCardsContent(
        bmi = null,
        ffmi = state.summary.ffmi,
        adjustedFfmi = state.summary.adjustedFfmi,
        unitFormatter = unitFormatter,
    )
}

/** A derived value: the rows edit nothing, since each comes from two readings. */
@Composable
private fun FfmiReadingEntriesContent(
    readings: List<FfmiReading>,
    chartDaySelection: ChartDaySelection,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedOnly: Boolean,
) {
    bodyMetricReadingEntriesContent(
        entries = readings,
        selectedDate = chartDaySelection.selectedDate,
        value = { ffmiDisplay(it.adjustedFfmi, unitFormatter).text },
        source = { it.source },
        time = { it.time },
        accentColor = BodyFatColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        selectedOnly = selectedOnly,
    )
}

private fun ffmiDisplay(value: Double, unitFormatter: UnitFormatter): DisplayValue =
    DisplayValue(unitFormatter.decimal(value, 1), "")
