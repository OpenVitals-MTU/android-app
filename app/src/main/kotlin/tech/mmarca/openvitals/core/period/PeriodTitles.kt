package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

/**
 * A dated span for a *past* rolling window ("22 May – 20 Jun 2026"), so a
 * rolling last-N-days period that no longer ends today reads as the span it
 * actually is rather than borrowing the single calendar month/year its start
 * falls in (which named a mostly-June window "May 2026"). The year rides on the
 * end date, and on the start too when the window straddles a year boundary.
 */
internal fun rollingSpanTitle(
    period: DatePeriod,
    locale: Locale = Locale.getDefault(),
): String {
    val spanFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val spanFormatterWithYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val crossesYears = period.start.year != period.end.year
    val start = (if (crossesYears) spanFormatterWithYear else spanFormatter).format(period.start)
    return "$start – ${spanFormatterWithYear.format(period.end)}"
}

fun periodTitle(
    range: TimeRange,
    period: DatePeriod,
    today: LocalDate = LocalDate.now(),
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
): String = when (range) {
    TimeRange.DAY -> when (period.start) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> dateFormatter.format(period.start)
    }

    TimeRange.WEEK -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
        "Last 7 days"
    } else if (weekPeriodMode.usesRollingDates()) {
        // A past rolling window is a dated span, not the calendar week of its start.
        rollingSpanTitle(period)
    } else if (today in period.start..period.end) {
        "This week"
    } else {
        "Week of ${dateFormatter.format(period.start)}"
    }
    TimeRange.MONTH -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
        "Last 30 days"
    } else if (weekPeriodMode.usesRollingDates()) {
        rollingSpanTitle(period)
    } else if (period.end == today) {
        "This month"
    } else {
        monthFormatter.format(period.start)
    }
    TimeRange.YEAR -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
        "Last 365 days"
    } else if (weekPeriodMode.usesRollingDates()) {
        rollingSpanTitle(period)
    } else if (period.end == today) {
        "This year"
    } else {
        yearFormatter.format(period.start)
    }
}
