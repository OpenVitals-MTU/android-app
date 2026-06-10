package tech.mmarca.openvitals.features.hydration.reminders

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig

class HydrationReminderScheduleTest {
    private val zone = ZoneId.of("UTC")

    @Test fun `next reminder inside active hours adds interval`() {
        val now = at(hour = 10, minute = 0)

        val next = calculateNextHydrationReminderTime(
            now = now,
            config = HydrationReminderConfig(intervalMinutes = 90),
        )

        assertEquals(at(hour = 11, minute = 30), next)
    }

    @Test fun `next reminder before active hours waits until active start plus interval`() {
        val now = at(hour = 5, minute = 0)

        val next = calculateNextHydrationReminderTime(
            now = now,
            config = HydrationReminderConfig(intervalMinutes = 120),
        )

        assertEquals(at(hour = 9, minute = 0), next)
    }

    @Test fun `next reminder crossing active end moves to next active start plus interval`() {
        val now = at(hour = 22, minute = 30)

        val next = calculateNextHydrationReminderTime(
            now = now,
            config = HydrationReminderConfig(intervalMinutes = 60),
        )

        assertEquals(at(day = 2, hour = 8, minute = 0), next)
    }

    @Test fun `overnight active hours include times after midnight before end`() {
        val config = HydrationReminderConfig(
            activeStartTime = LocalTime.of(7, 0),
            activeEndTime = LocalTime.of(1, 0),
        )

        assertTrue(isWithinHydrationReminderActiveHours(LocalTime.of(23, 0), config))
        assertTrue(isWithinHydrationReminderActiveHours(LocalTime.of(0, 30), config))
        assertFalse(isWithinHydrationReminderActiveHours(LocalTime.of(2, 0), config))
    }

    @Test fun `goal met schedules tomorrow after active start interval`() {
        val now = at(hour = 12, minute = 0)

        val next = calculateNextHydrationReminderTime(
            now = now,
            config = HydrationReminderConfig(intervalMinutes = 120),
            dailyGoalMet = true,
        )

        assertEquals(at(day = 2, hour = 9, minute = 0), next)
    }

    private fun at(day: Int = 1, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 6, day, hour, minute, 0, 0, zone)
}
