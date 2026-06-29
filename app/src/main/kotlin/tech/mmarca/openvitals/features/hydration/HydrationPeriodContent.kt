package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.LocalTime
import java.time.ZoneId

internal fun LazyListScope.hydrationPeriodContent(
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
    onEditHydrationEntry: (String) -> Unit,
    onDeleteHydrationEntry: (String) -> Unit,
) {
    val display = state.display
    if (!display.hasData) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.metric_hydration),
                icon = Icons.Outlined.LocalDrink,
                accentColor = HydrationColor,
                message = stringResource(R.string.message_no_hydration_period),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        hydrationGoalAndReminderItems(
            state = state,
            display = display,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            hasNotificationPermission = hasNotificationPermission,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDecreaseInterval = onDecreaseInterval,
            onIncreaseInterval = onIncreaseInterval,
            onSelectActiveStartTime = onSelectActiveStartTime,
            onSelectActiveEndTime = onSelectActiveEndTime,
        )
        return
    }

    item {
        HydrationSummary(
            state = state,
            display = display,
            period = period,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    item {
        val modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        if (state.selectedRange == TimeRange.DAY) {
            HydrationDayGoalProgress(
                liters = display.dayLiters,
                dailyGoalLiters = state.dailyGoalLiters,
                period = period,
                unitFormatter = unitFormatter,
                modifier = modifier,
            )
        } else {
            val useWeekAccent = state.selectedRange == TimeRange.WEEK
            MetricBarChart(
                title = stringResource(R.string.metric_hydration_trend),
                data = state.dailyHydration,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = if (useWeekAccent) HydrationWeekChartColor else HydrationColor,
                accentAlpha = if (useWeekAccent) 1f else 0.85f,
                summaryValue = unitFormatter.hydration(display.summary.totalLiters).text,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                date = { it.date },
                value = { it.liters },
                modifier = modifier.then(
                    if (state.selectedRange == TimeRange.WEEK) {
                        Modifier.testTag("hydration_week_period_content")
                    } else {
                        Modifier
                    },
                ),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { unitFormatter.hydration(it).text },
            )
        }
    }
    chartDaySelection.selectedDate?.let { selectedDate ->
        hydrationEntries(
            entries = state.hydrationEntries.filter {
                it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
            },
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            titleDate = selectedDate,
            onEditHydrationEntry = onEditHydrationEntry,
            onDeleteHydrationEntry = onDeleteHydrationEntry,
        )
    }
    item {
        HydrationDataConfidence(
            display = display,
            period = period,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    hydrationGoalAndReminderItems(
        state = state,
        display = display,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        hasNotificationPermission = hasNotificationPermission,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
        onToggleReminders = onToggleReminders,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onDecreaseInterval = onDecreaseInterval,
        onIncreaseInterval = onIncreaseInterval,
        onSelectActiveStartTime = onSelectActiveStartTime,
        onSelectActiveEndTime = onSelectActiveEndTime,
    )
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        HydrationStatistics(
            display = display,
            period = period,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    hydrationWeightInsight(display = display)
    hydrationEntries(
        entries = state.hydrationEntries,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditHydrationEntry = onEditHydrationEntry,
        onDeleteHydrationEntry = onDeleteHydrationEntry,
    )
}

private fun LazyListScope.hydrationGoalAndReminderItems(
    state: HydrationUiState,
    display: HydrationDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
) {
    item {
        HydrationGoalCard(
            display = display,
            dailyGoalLiters = state.dailyGoalLiters,
            unitFormatter = unitFormatter,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    item {
        HydrationReminderCard(
            config = state.reminderConfig,
            hasNotificationPermission = hasNotificationPermission,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDecreaseInterval = onDecreaseInterval,
            onIncreaseInterval = onIncreaseInterval,
            onSelectActiveStartTime = onSelectActiveStartTime,
            onSelectActiveEndTime = onSelectActiveEndTime,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun LazyListScope.hydrationWeightInsight(display: HydrationDisplayState) {
    val insight = display.crossMetricInsight ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_hydration_weight_title),
            positiveMessage = stringResource(R.string.cross_hydration_weight_positive),
            negativeMessage = stringResource(R.string.cross_hydration_weight_negative),
            neutralMessage = stringResource(R.string.cross_hydration_weight_neutral),
            accentColor = HydrationColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
