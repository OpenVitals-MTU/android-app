package tech.mmarca.openvitals.features.mindfulness.reminders

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.model.MindfulnessReminderConfig

class MindfulnessReminderScheduleTest {
    private val zone = ZoneId.of("UTC")

    @Test fun `next reminder before configured time schedules today`() {
        val next = calculateNextMindfulnessReminderTime(
            now = at(hour = 12, minute = 0),
            config = MindfulnessReminderConfig(reminderTime = LocalTime.of(18, 0)),
        )

        assertEquals(at(hour = 18, minute = 0), next)
    }

    @Test fun `next reminder after configured time schedules tomorrow`() {
        val next = calculateNextMindfulnessReminderTime(
            now = at(hour = 19, minute = 0),
            config = MindfulnessReminderConfig(reminderTime = LocalTime.of(18, 0)),
        )

        assertEquals(at(day = 2, hour = 18, minute = 0), next)
    }

    @Test fun `goal met schedules tomorrow`() {
        val next = calculateNextMindfulnessReminderTime(
            now = at(hour = 12, minute = 0),
            config = MindfulnessReminderConfig(reminderTime = LocalTime.of(18, 0)),
            dailyGoalMet = true,
        )

        assertEquals(at(day = 2, hour = 18, minute = 0), next)
    }

    private fun at(day: Int = 1, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 6, day, hour, minute, 0, 0, zone)
}
