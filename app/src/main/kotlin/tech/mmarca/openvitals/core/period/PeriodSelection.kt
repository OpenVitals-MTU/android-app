package tech.mmarca.openvitals.core.period

import java.time.LocalDate

data class PeriodSelection(
    val selectedRange: TimeRange,
    val selectedDate: LocalDate,
) {
    fun selectRange(range: TimeRange, today: LocalDate = LocalDate.now()): PeriodSelection =
        copy(selectedRange = range, selectedDate = selectedDate.coerceAtMost(today))

    fun previousPeriod(): PeriodSelection =
        copy(selectedDate = selectedRange.shift(selectedDate, steps = -1))

    fun nextPeriod(
        today: LocalDate = LocalDate.now(),
        weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    ): PeriodSelection {
        val nextDate = selectedRange.shift(selectedDate, steps = 1)
        val nextPeriod = periodFor(selectedRange, nextDate, today, weekPeriodMode)
        return if (nextPeriod.start.isAfter(today) || nextPeriod.end.isAfter(today)) {
            this
        } else {
            copy(selectedDate = nextDate)
        }
    }

    fun selectDate(date: LocalDate, today: LocalDate = LocalDate.now()): PeriodSelection =
        copy(selectedDate = date.coerceAtMost(today))

    fun period(
        today: LocalDate = LocalDate.now(),
        weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    ): DatePeriod =
        periodFor(selectedRange, selectedDate, today, weekPeriodMode)
}
