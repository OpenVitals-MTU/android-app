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
) {
    val selectedDate: LocalDate = anchorDate.coerceAtMost(today)
    val windows: PeriodWindows = periodWindowsFor(
        range = range,
        anchorDate = selectedDate,
        today = today,
        baselineDays = baselineDays,
    )
}

class PeriodSelectionDriver(
    initialRange: TimeRange,
    initialDate: LocalDate = LocalDate.now(),
    private val onRangeSelected: (TimeRange) -> Unit = {},
) {
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
        val next = selection.nextPeriod()
        return if (next == selection) null else update(next)
    }

    fun selectDate(date: LocalDate): PeriodSelection =
        update(selection.selectDate(date))

    private fun update(next: PeriodSelection): PeriodSelection {
        selection = next
        return next
    }
}

fun periodWindowsFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    baselineDays: Long = DefaultBaselineDays,
): PeriodWindows {
    val current = periodFor(range, anchorDate.coerceAtMost(today), today)
    val previous = previousPeriodFor(range, anchorDate, today)
    val baseline = baselinePeriodBefore(current, baselineDays)
    return PeriodWindows(
        current = current,
        previous = previous,
        baseline = baseline,
    )
}

private const val DefaultBaselineDays = 90L
