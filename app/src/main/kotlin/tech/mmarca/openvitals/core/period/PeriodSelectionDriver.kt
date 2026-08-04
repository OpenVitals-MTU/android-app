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
        updateUserSelection(selection.previousPeriod(weekPeriodMode))

    fun nextPeriod(): PeriodSelection? {
        val next = selection.nextPeriod(weekPeriodMode = weekPeriodMode)
        return if (next == selection) null else updateUserSelection(next)
    }

    fun selectDate(date: LocalDate): PeriodSelection =
        updateUserSelection(selection.selectDate(date))

    /**
     * Drill into a single day: switch to the Day range anchored on [date] — the
     * month heatmap's tap-to-open-day. Persists the range like [selectRange] so
     * the screen reopens on Day, and pins the past period like [selectDate] so a
     * resume does not bounce the user back to today.
     */
    fun selectDay(date: LocalDate): PeriodSelection {
        onRangeSelected(TimeRange.DAY)
        return updateUserSelection(selection.selectRange(TimeRange.DAY).selectDate(date))
    }

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
