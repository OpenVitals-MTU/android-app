package tech.mmarca.openvitals.core.period

import java.time.LocalDate

data class PeriodWindows(
    val current: DatePeriod,
    val previous: DatePeriod,
    val baseline: DatePeriod,
)

data class PeriodLoadQuery(
    val range: TimeRange,
    val anchorDate: LocalDate,
    val today: LocalDate = LocalDate.now(),
    val baselineDays: Long = DefaultBaselineDays,
    val weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
) {
    val selectedDate: LocalDate = anchorDate.coerceAtMost(today)
    val windows: PeriodWindows = periodWindowsFor(
        range = range,
        anchorDate = selectedDate,
        today = today,
        baselineDays = baselineDays,
        weekPeriodMode = weekPeriodMode,
    )
}
