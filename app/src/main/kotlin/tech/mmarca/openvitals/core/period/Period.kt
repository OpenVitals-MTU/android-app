package tech.mmarca.openvitals.core.period

import java.time.LocalDate

enum class TimeRange(val label: String, val days: Int) {
    DAY("Day", 1),
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
}

enum class WeekPeriodMode {
    MONDAY_TO_SUNDAY,
    LAST_7_DAYS,
}

data class DatePeriod(
    val start: LocalDate,
    val end: LocalDate,
)
