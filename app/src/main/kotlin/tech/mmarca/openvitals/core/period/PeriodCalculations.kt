package tech.mmarca.openvitals.core.period

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun periodFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    clipCurrentWeekToToday: Boolean = true,
): DatePeriod = when (range) {
    TimeRange.DAY -> DatePeriod(
        start = anchorDate,
        end = anchorDate,
    )

    TimeRange.WEEK -> weekPeriodFor(
        anchorDate = anchorDate,
        today = today,
        weekPeriodMode = weekPeriodMode,
        clipCurrentWeekToToday = clipCurrentWeekToToday,
    )

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

fun displayPeriodFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
): DatePeriod =
    periodFor(
        range = range,
        anchorDate = anchorDate,
        today = today,
        weekPeriodMode = weekPeriodMode,
        clipCurrentWeekToToday = false,
    )

fun previousPeriodFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
): DatePeriod =
    PeriodSelection(
        selectedRange = range,
        selectedDate = anchorDate.coerceAtMost(today),
    ).previousPeriod().period(today, weekPeriodMode)

fun baselinePeriodBefore(
    period: DatePeriod,
    days: Long = 90,
): DatePeriod =
    DatePeriod(
        start = period.start.minusDays(days),
        end = period.start.minusDays(1),
    )

fun periodWindowsFor(
    range: TimeRange,
    anchorDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    baselineDays: Long = DefaultBaselineDays,
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
): PeriodWindows {
    val current = periodFor(range, anchorDate.coerceAtMost(today), today, weekPeriodMode)
    val previous = previousPeriodFor(range, anchorDate, today, weekPeriodMode)
    val baseline = baselinePeriodBefore(current, baselineDays)
    return PeriodWindows(
        current = current,
        previous = previous,
        baseline = baseline,
    )
}

internal fun TimeRange.shift(anchorDate: LocalDate, steps: Long): LocalDate = when (this) {
    TimeRange.DAY -> anchorDate.plusDays(steps)
    TimeRange.WEEK -> anchorDate.plusWeeks(steps)
    TimeRange.MONTH -> anchorDate.plusMonths(steps)
    TimeRange.YEAR -> anchorDate.plusYears(steps)
}

private fun weekPeriodFor(
    anchorDate: LocalDate,
    today: LocalDate,
    weekPeriodMode: WeekPeriodMode,
    clipCurrentWeekToToday: Boolean,
): DatePeriod =
    when (weekPeriodMode) {
        WeekPeriodMode.MONDAY_TO_SUNDAY -> {
            val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DatePeriod(
                start = start,
                end = if (clipCurrentWeekToToday) {
                    start.plusDays(6).coerceAtMost(today)
                } else {
                    start.plusDays(6)
                },
            )
        }
        WeekPeriodMode.LAST_7_DAYS -> DatePeriod(
            start = anchorDate.minusDays(6),
            end = anchorDate,
        )
    }

internal const val DefaultBaselineDays = 90L
