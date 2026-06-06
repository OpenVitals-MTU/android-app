package tech.mmarca.openvitals.data.model

import java.time.LocalTime

data class MindfulnessReminderConfig(
    val enabled: Boolean = false,
    val reminderTime: LocalTime = DefaultReminderTime,
) {
    fun normalized(): MindfulnessReminderConfig = this

    companion object {
        val DefaultReminderTime: LocalTime = LocalTime.of(18, 0)
    }
}
