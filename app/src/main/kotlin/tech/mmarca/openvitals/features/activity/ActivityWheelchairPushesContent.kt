package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.theme.WheelchairPushesColor
import kotlin.math.roundToLong

internal fun LazyListScope.wheelchairPushesContent(
    state: ActivityUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.wheelchairPushes != null }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
                val pushesTotal = state.dailySteps.firstOrNull()?.wheelchairPushes ?: 0L
                IntradayActivityChartCard(
                    selectedDate = state.selectedDate,
                    title = stringResource(R.string.metric_wheelchair_pushes),
                    valueText = "${unitFormatter.count(pushesTotal)} ${stringResource(R.string.unit_pushes)}",
                    emptyText = stringResource(R.string.message_no_wheelchair_pushes),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    points = state.activityProgress.mapNotNull { point ->
                        point.totalWheelchairPushes?.let { point.time to it.toDouble() }
                    },
                    accentColor = WheelchairPushesColor,
                    yAxisValueFormatter = { unitFormatter.count(it.roundToLong()) },
                    modifier = activityMetricModifier(),
                )
            } else {
                WheelchairPushesBarChart(
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = activityMetricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            activityDailyEntries(
                entries = state.dailySteps.filter { (it.wheelchairPushes ?: 0L) > 0L && it.date == selectedDate },
                date = { it.date },
                value = {
                    DisplayValue(
                        unitFormatter.count(it.wheelchairPushes ?: 0L),
                        stringResource(R.string.unit_pushes),
                    )
                },
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = WheelchairPushesColor,
                titleDate = selectedDate,
            )
        }
        val values = state.dailySteps.map { (it.wheelchairPushes ?: 0L).toDouble() }
        activityGoal(
            state = state,
            period = period,
            values = state.dailySteps.map { DailyGoalValue(it.date, (it.wheelchairPushes ?: 0L).toDouble()) },
            unitFormatter = unitFormatter,
            icon = Icons.AutoMirrored.Outlined.Accessible,
            accentColor = WheelchairPushesColor,
            direction = ActivityMetric.WHEELCHAIR_PUSHES.dailyGoalKey.direction,
            goalFormatter = {
                DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_pushes))
            },
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
        )
        activityDataConfidence(
            period = period,
            trackedDates = state.dailySteps.filter { (it.wheelchairPushes ?: 0L) > 0L }.map { it.date },
            sampleCount = if (state.selectedRange == TimeRange.DAY) {
                state.activityProgress.count { (it.totalWheelchairPushes ?: 0L) > 0L }
            } else {
                values.count { it > 0.0 }
            },
            accentColor = WheelchairPushesColor,
        )
        activityStatistics(
            unitFormatter = unitFormatter,
            period = period,
            total = { DisplayValue(unitFormatter.count(values.sum().roundToLong()), stringResource(R.string.unit_pushes)) },
            average = {
                DisplayValue(
                    unitFormatter.count(averageOrZero(values.sum(), values.count { it > 0.0 }).roundToLong()),
                    stringResource(R.string.unit_pushes),
                )
            },
            best = { DisplayValue(unitFormatter.count((values.maxOrNull() ?: 0.0).roundToLong()), stringResource(R.string.unit_pushes)) },
            activeDays = values.count { it > 0.0 },
            comparison = periodComparison(
                currentValue = values.sum(),
                previousValue = state.previousDailySteps.sumOf { (it.wheelchairPushes ?: 0L).toDouble() },
            ),
            selectedRange = state.selectedRange,
            comparisonValueFormatter = {
                DisplayValue(unitFormatter.count(it.roundToLong()), stringResource(R.string.unit_pushes))
            },
            baselineCurrentValue = averageOrZero(values.sum(), values.count { it > 0.0 }),
            baselineValues = state.baselineDailySteps.map { BaselineValue(it.date, (it.wheelchairPushes ?: 0L).toDouble()) },
            icon = Icons.AutoMirrored.Outlined.Accessible,
            accentColor = WheelchairPushesColor,
            includeHeader = false,
        )
        activityDailyEntries(
            entries = state.dailySteps.filter { it.wheelchairPushes != null },
            date = { it.date },
            value = {
                DisplayValue(unitFormatter.count(it.wheelchairPushes ?: 0L), stringResource(R.string.unit_pushes))
            },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            accentColor = WheelchairPushesColor,
        )
    } else if (!state.isLoading) {
        noMetricData(
            R.string.metric_wheelchair_pushes,
            R.string.message_no_wheelchair_pushes,
            Icons.AutoMirrored.Outlined.Accessible,
            WheelchairPushesColor,
        )
    }
}
