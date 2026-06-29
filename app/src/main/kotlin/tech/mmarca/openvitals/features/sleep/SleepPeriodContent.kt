package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.theme.SleepColor

internal fun LazyListScope.sleepPeriodContent(
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    chartDaySelection: ChartDaySelection,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    item {
        val nightsWithSleep = display.durationPoints.filter { it.hours > 0.0 }
        val averageHours = nightsWithSleep.map { it.hours }.average().takeIf { !it.isNaN() } ?: 0.0
        MetricBarChart(
            title = stringResource(R.string.metric_sleep),
            values = display.durationPoints.map { PeriodChartValue(date = it.date, value = it.hours) },
            selectedRange = state.selectedRange,
            period = period,
            accentColor = SleepColor,
            accentAlpha = 0.75f,
            summaryValue = "${
                stringResource(R.string.summary_avg_value, "${unitFormatter.decimal(averageHours, 1)}h")
            } · ${stringResource(R.string.summary_nights, unitFormatter.count(nightsWithSleep.size))}",
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sleep_week_period_content")
                .padding(horizontal = 16.dp, vertical = 8.dp),
            yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
            selectedDate = chartDaySelection.selectedDate,
            onDateSelected = chartDaySelection.onDateSelected,
            valueFormatter = { "${unitFormatter.decimal(it, 1)}h" },
        )
    }
    chartDaySelection.selectedDate?.let { selectedDate ->
        val daySessions = display.overviewDays
            .firstOrNull { it.date == selectedDate }
            ?.sessions
            .orEmpty()
        item {
            PaginatedEntryList(
                title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                entries = daySessions.sortedByDescending { it.endTime },
            ) { session, rowModifier ->
                SleepSessionItem(
                    session = session,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onClick = { onOpenSleepSession(session.id) },
                    modifier = rowModifier,
                )
            }
        }
    }
    sleepInsightSections(
        state = state,
        period = period,
        confidenceSessions = state.sessions,
        display = display,
        unitFormatter = unitFormatter,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
    )

    item {
        PaginatedEntryList(
            title = stringResource(R.string.section_sleep_sessions),
            entries = state.sessions.sortedByDescending { it.endTime },
        ) { session, rowModifier ->
            SleepSessionItem(
                session = session,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onClick = { onOpenSleepSession(session.id) },
                modifier = rowModifier,
            )
        }
    }
}
