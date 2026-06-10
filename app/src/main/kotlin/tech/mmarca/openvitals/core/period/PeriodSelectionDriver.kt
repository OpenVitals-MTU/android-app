package tech.mmarca.openvitals.core.period

import java.time.LocalDate

class PeriodSelectionDriver(
    initialRange: TimeRange,
    initialDate: LocalDate = LocalDate.now(),
    initialWeekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    private val onRangeSelected: (TimeRange) -> Unit = {},
) {
    var weekPeriodMode: WeekPeriodMode = initialWeekPeriodMode
    var selection: PeriodSelection = PeriodSelection(
        selectedRange = initialRange,
        selectedDate = initialDate.coerceAtMost(LocalDate.now()),
    )
        private set

    fun selectRange(range: TimeRange): PeriodSelection {
        onRangeSelected(range)
        return update(selection.selectRange(range))
    }

    fun previousPeriod(): PeriodSelection =
        update(selection.previousPeriod())

    fun nextPeriod(): PeriodSelection? {
        val next = selection.nextPeriod(weekPeriodMode = weekPeriodMode)
        return if (next == selection) null else update(next)
    }

    fun selectDate(date: LocalDate): PeriodSelection =
        update(selection.selectDate(date))

    private fun update(next: PeriodSelection): PeriodSelection {
        selection = next
        return next
    }
}
