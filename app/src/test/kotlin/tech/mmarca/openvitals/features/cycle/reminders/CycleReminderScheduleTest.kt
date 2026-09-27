package tech.mmarca.openvitals.features.cycle.reminders

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.model.CycleReminderConfig

/** The three alarms' timing rules, on a fixed clock. */
class CycleReminderScheduleTest {
    private val zone = ZoneId.of("UTC")
    private val config = CycleReminderConfig(
        enabled = true,
        dailyCheckInEnabled = true,
        dailyCheckInTime = LocalTime.of(21, 0),
        periodWindowEnabled = true,
        periodWindowLeadDays = 2,
        lateCycleEnabled = true,
        lateCycleGraceDays = 1,
    )
    private val estimate = CycleEstimateResult.Available(
        CycleEstimate(
            earliestDate = LocalDate.of(2026, 7, 20),
            centralDate = LocalDate.of(2026, 7, 22),
            latestDate = LocalDate.of(2026, 7, 24),
            cycleCount = 3,
            variabilityDays = 2,
        ),
    )

    @Test
    fun `the daily reminder is today at the set time until it passes or the day is logged`() {
        assertEquals(at(10, 21, 0), CycleReminderSchedule.nextDailyCheckIn(at(10, 8, 30), config, loggedToday = false))
        assertEquals(at(11, 21, 0), CycleReminderSchedule.nextDailyCheckIn(at(10, 21, 30), config, loggedToday = false))
        assertEquals(at(11, 21, 0), CycleReminderSchedule.nextDailyCheckIn(at(10, 8, 30), config, loggedToday = true))
    }

    @Test
    fun `the window reminder is the lead days before the earliest date, at nine`() {
        assertEquals(at(18, 9, 0), CycleReminderSchedule.periodWindow(at(10, 12, 0), config, estimate))
        // Already passed: nothing to arm.
        assertNull(CycleReminderSchedule.periodWindow(at(19, 12, 0), config, estimate))
        assertNull(CycleReminderSchedule.periodWindow(at(10, 12, 0), config, CycleEstimateResult.NeedsMoreHistory))
        assertNull(CycleReminderSchedule.periodWindow(at(10, 12, 0), config, CycleEstimateResult.IntervalsOutOfRange))
    }

    @Test
    fun `the late reminder is the grace days after the latest date, at ten`() {
        assertEquals(at(25, 10, 0), CycleReminderSchedule.lateCycle(at(10, 12, 0), config, estimate))
        assertNull(CycleReminderSchedule.lateCycle(at(26, 12, 0), config, estimate))
        assertNull(CycleReminderSchedule.lateCycle(at(10, 12, 0), config, CycleEstimateResult.NeedsMoreHistory))
    }

    private fun at(day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 7, day, hour, minute, 0, 0, zone)
}
