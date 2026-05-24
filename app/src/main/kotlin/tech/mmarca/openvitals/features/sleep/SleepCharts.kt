package tech.mmarca.openvitals.features.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.dailySleepSummary
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun SleepDurationChart(
    sessions: List<SleepData>,
    selectedRange: TimeRange,
    period: DatePeriod,
    sleepRangeMode: SleepRangeMode,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    durationPoints: List<SleepDurationPoint> = sleepDurationPoints(sessions, period, sleepRangeMode),
) {
    val points = durationPoints
    val nightsWithSleep = points.filter { it.hours > 0.0 }
    val averageHours = nightsWithSleep.map { it.hours }.average().takeIf { !it.isNaN() } ?: 0.0

    PeriodHistoryChart(
        title = stringResource(R.string.metric_sleep),
        values = points.map { PeriodChartValue(date = it.date, value = it.hours) },
        selectedRange = selectedRange,
        period = period,
        accentColor = SleepColor.copy(alpha = 0.75f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_avg_value, "${unitFormatter.decimal(averageHours, 1)}h")
        } · ${stringResource(R.string.summary_nights, unitFormatter.count(nightsWithSleep.size))}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
        valueFormatter = { "${unitFormatter.decimal(it, 1)}h" },
    )
}

internal fun sleepDurationPoints(
    sessions: List<SleepData>,
    period: DatePeriod,
    sleepRangeMode: SleepRangeMode,
): List<SleepDurationPoint> {
    val zone = ZoneId.systemDefault()

    return generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        SleepDurationPoint(
            date = date,
            hours = dailySleepSummary(
                sessions = sessions,
                selectedDate = date,
                sleepRangeMode = sleepRangeMode,
                zone = zone,
            )?.durationHours ?: 0.0,
        )
    }.toList()
}

internal data class SleepDurationPoint(
    val date: LocalDate,
    val hours: Double,
)
