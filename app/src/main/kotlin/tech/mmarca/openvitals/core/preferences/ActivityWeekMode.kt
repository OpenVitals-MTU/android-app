package tech.mmarca.openvitals.core.preferences

import tech.mmarca.openvitals.core.period.WeekPeriodMode

enum class ActivityWeekMode {
    MONDAY_TO_SUNDAY,
    LAST_7_DAYS,
}

fun ActivityWeekMode.toWeekPeriodMode(): WeekPeriodMode =
    when (this) {
        ActivityWeekMode.MONDAY_TO_SUNDAY -> WeekPeriodMode.MONDAY_TO_SUNDAY
        ActivityWeekMode.LAST_7_DAYS -> WeekPeriodMode.LAST_7_DAYS
    }
