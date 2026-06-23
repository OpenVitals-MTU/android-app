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
    private var userPinnedPastPeriod = isPastPeriod(selection)

    fun selectRange(range: TimeRange): PeriodSelection {
        onRangeSelected(range)
        return update(selection.selectRange(range), userPinnedPastPeriod)
    }

    fun previousPeriod(): PeriodSelection =
        updateUserSelection(selection.previousPeriod())

    fun nextPeriod(): PeriodSelection? {
        val next = selection.nextPeriod(weekPeriodMode = weekPeriodMode)
        return if (next == selection) null else updateUserSelection(next)
    }

    fun selectDate(date: LocalDate): PeriodSelection =
        updateUserSelection(selection.selectDate(date))

    fun resumeCurrentPeriod(today: LocalDate = LocalDate.now()): PeriodSelection? {
        if (userPinnedPastPeriod || !isPastPeriod(selection, today)) return null
        return update(selection.selectDate(today, today), userPinnedPastPeriod = false)
    }

    private fun updateUserSelection(next: PeriodSelection): PeriodSelection =
        update(next, userPinnedPastPeriod = isPastPeriod(next))

    private fun update(next: PeriodSelection, userPinnedPastPeriod: Boolean): PeriodSelection {
        selection = next
        this.userPinnedPastPeriod = userPinnedPastPeriod
        return next
    }

    private fun isPastPeriod(
        selection: PeriodSelection,
        today: LocalDate = LocalDate.now(),
    ): Boolean =
        selection.period(today, weekPeriodMode).end.isBefore(today)
}
