package tech.mmarca.openvitals.core.period

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

enum class TimeRange(val label: String, val days: Int) {
    DAY("Day", 1),
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
}

data class DatePeriod(
    val start: LocalDate,
    val end: LocalDate,
)

data class PeriodSelection(
    val selectedRange: TimeRange,
    val selectedDate: LocalDate,
) {
    fun selectRange(range: TimeRange, today: LocalDate = LocalDate.now()): PeriodSelection =
        copy(selectedRange = range, selectedDate = selectedDate.coerceAtMost(today))

    fun previousPeriod(): PeriodSelection =
        copy(selectedDate = selectedRange.shift(selectedDate, steps = -1))

    fun nextPeriod(today: LocalDate = LocalDate.now()): PeriodSelection {
        val nextDate = selectedRange.shift(selectedDate, steps = 1)
        val nextPeriod = periodFor(selectedRange, nextDate, today)
        return if (nextPeriod.start.isAfter(today) || nextPeriod.end.isAfter(today)) {
            this
        } else {
            copy(selectedDate = nextDate)
        }
    }

    fun selectDate(date: LocalDate, today: LocalDate = LocalDate.now()): PeriodSelection =
        copy(selectedDate = date.coerceAtMost(today))

    fun period(today: LocalDate = LocalDate.now()): DatePeriod =
        periodFor(selectedRange, selectedDate, today)
}

fun periodFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): DatePeriod = when (range) {
    TimeRange.DAY -> DatePeriod(
        start = anchorDate,
        end = anchorDate,
    )

    TimeRange.WEEK -> {
        val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6).coerceAtMost(today)
        DatePeriod(start = start, end = end)
    }

    TimeRange.MONTH -> {
        val start = anchorDate.withDayOfMonth(1)
        val end = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth()).coerceAtMost(today)
        DatePeriod(start = start, end = end)
    }

    TimeRange.YEAR -> {
        val start = anchorDate.withDayOfYear(1)
        val end = anchorDate.withDayOfYear(anchorDate.lengthOfYear()).coerceAtMost(today)
        DatePeriod(start = start, end = end)
    }
}

fun previousPeriodFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): DatePeriod =
    PeriodSelection(
        selectedRange = range,
        selectedDate = anchorDate.coerceAtMost(today),
    ).previousPeriod().period(today)

fun baselinePeriodBefore(
    period: DatePeriod,
    days: Long = 90,
): DatePeriod =
    DatePeriod(
        start = period.start.minusDays(days),
        end = period.start.minusDays(1),
    )

fun periodTitle(
    range: TimeRange,
    period: DatePeriod,
    today: LocalDate = LocalDate.now(),
): String = when (range) {
    TimeRange.DAY -> when (period.start) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> dateFormatter.format(period.start)
    }

    TimeRange.WEEK -> if (period.end == today) "This week" else "Week of ${dateFormatter.format(period.start)}"
    TimeRange.MONTH -> if (period.end == today) "This month" else monthFormatter.format(period.start)
    TimeRange.YEAR -> if (period.end == today) "This year" else yearFormatter.format(period.start)
}

fun periodSubtitle(range: TimeRange, period: DatePeriod): String = when (range) {
    TimeRange.DAY -> dateFormatter.format(period.start)
    TimeRange.WEEK -> "${dateFormatter.format(period.start)} - ${dateFormatter.format(period.end)}"
    TimeRange.MONTH,
    TimeRange.YEAR -> "${dateFormatter.format(period.start)} - ${dateFormatter.format(period.end)}"
}

private fun TimeRange.shift(anchorDate: LocalDate, steps: Long): LocalDate = when (this) {
    TimeRange.DAY -> anchorDate.plusDays(steps)
    TimeRange.WEEK -> anchorDate.plusWeeks(steps)
    TimeRange.MONTH -> anchorDate.plusMonths(steps)
    TimeRange.YEAR -> anchorDate.plusYears(steps)
}
