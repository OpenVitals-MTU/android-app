package tech.mmarca.openvitals.features.hydration.reminders

import java.time.LocalTime
import java.time.ZonedDateTime
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig

/**
 * The next fire. With a [lastIntake] anchor the countdown is measured from
 * the last drink, snapped into the window and rolled past [now].
 */
internal fun calculateNextHydrationReminderTime(
    now: ZonedDateTime,
    config: HydrationReminderConfig,
    dailyGoalMet: Boolean = false,
    lastIntake: ZonedDateTime? = null,
): ZonedDateTime {
    if (!dailyGoalMet && lastIntake != null) {
        var candidate = nextReminderFrom(lastIntake, config)
        // Defensive bound: never spin on a degenerate config.
        var steps = 0
        while (!candidate.isAfter(now) && steps < MaxAnchorRollForwardSteps) {
            val following = nextReminderFrom(candidate, config)
            if (!following.isAfter(candidate)) break
            candidate = following
            steps++
        }
        if (candidate.isAfter(now)) return candidate
    }
    return nextReminderFrom(now, config, dailyGoalMet)
}

private const val MaxAnchorRollForwardSteps = 512

private fun nextReminderFrom(
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
