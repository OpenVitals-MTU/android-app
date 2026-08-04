package tech.mmarca.openvitals.features.mindfulness.reminders

import java.time.ZonedDateTime
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig

internal fun calculateNextMindfulnessReminderTime(
    now: ZonedDateTime,
    config: MindfulnessReminderConfig,
    dailyGoalMet: Boolean = false,
): ZonedDateTime {
    val todayReminder = now.toLocalDate()
        .atTime(config.normalized().reminderTime)
        .atZone(now.zone)
    return if (!dailyGoalMet && todayReminder.isAfter(now)) {
        todayReminder
    } else {
        todayReminder.plusDays(1)
    }
}
