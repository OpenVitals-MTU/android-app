package tech.mmarca.openvitals.features.hydration.reminders

import java.time.LocalTime
import java.time.ZonedDateTime
import tech.mmarca.openvitals.data.model.HydrationReminderConfig

internal fun calculateNextHydrationReminderTime(
    now: ZonedDateTime,
    config: HydrationReminderConfig,
    dailyGoalMet: Boolean = false,
): ZonedDateTime {
    val normalized = config.normalized()
    if (dailyGoalMet) {
        return now.toLocalDate()
            .plusDays(1)
            .atTime(normalized.activeStartTime)
            .atZone(now.zone)
            .plusMinutes(normalized.intervalMinutes.toLong())
    }

    if (!isWithinHydrationReminderActiveHours(now.toLocalTime(), normalized)) {
        return nextActiveStartAfter(now, normalized)
            .plusMinutes(normalized.intervalMinutes.toLong())
    }

    val candidate = now.plusMinutes(normalized.intervalMinutes.toLong())
    return if (isWithinHydrationReminderActiveHours(candidate.toLocalTime(), normalized)) {
        candidate
    } else {
        nextActiveStartAfter(candidate, normalized)
            .plusMinutes(normalized.intervalMinutes.toLong())
    }
}

internal fun isWithinHydrationReminderActiveHours(
    time: LocalTime,
    config: HydrationReminderConfig,
): Boolean {
    val start = config.activeStartTime
    val end = config.activeEndTime
    if (start == end) return true
    return if (end.isAfter(start)) {
        !time.isBefore(start) && time.isBefore(end)
    } else {
        !time.isBefore(start) || time.isBefore(end)
    }
}

private fun nextActiveStartAfter(
    moment: ZonedDateTime,
    config: HydrationReminderConfig,
): ZonedDateTime {
    val todayStart = moment.toLocalDate()
        .atTime(config.activeStartTime)
        .atZone(moment.zone)
    return if (todayStart.isAfter(moment)) todayStart else todayStart.plusDays(1)
}
