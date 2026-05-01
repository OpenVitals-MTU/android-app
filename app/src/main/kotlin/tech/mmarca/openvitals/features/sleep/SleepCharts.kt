package tech.mmarca.openvitals.features.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun SleepDurationChart(
    sessions: List<SleepData>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val points = sleepDurationPoints(sessions, period)
    val nightsWithSleep = points.filter { it.hours > 0.0 }
    val averageHours = nightsWithSleep.map { it.hours }.average().takeIf { !it.isNaN() } ?: 0.0

    PeriodBarChart(
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
    )
}

private fun sleepDurationPoints(
    sessions: List<SleepData>,
    period: DatePeriod,
): List<SleepDurationPoint> {
    val zone = ZoneId.systemDefault()
    val sessionByDate = sessions
        .groupBy { it.endTime.atZone(zone).toLocalDate() }
        .mapValues { (_, dailySessions) -> dailySessions.maxByOrNull { it.durationMs } }

    return generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        SleepDurationPoint(
            date = date,
            hours = sessionByDate[date]?.durationHours ?: 0.0,
        )
    }.toList()
}

private data class SleepDurationPoint(
    val date: LocalDate,
    val hours: Double,
)
