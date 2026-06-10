package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

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

    TimeRange.WEEK -> if (today in period.start..period.end) {
        "This week"
    } else {
        "Week of ${dateFormatter.format(period.start)}"
    }
    TimeRange.MONTH -> if (period.end == today) "This month" else monthFormatter.format(period.start)
    TimeRange.YEAR -> if (period.end == today) "This year" else yearFormatter.format(period.start)
}
