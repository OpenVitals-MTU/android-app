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

    private val pill = tech.mmarca.openvitals.domain.model.PillPlan(
        enabled = true,
        activeDays = 21,
        pauseDays = 7,
        packStart = LocalDate.of(2026, 7, 1),
        reminderTime = LocalTime.of(20, 0),
    )

    @Test
    fun `the pill reminder is today at the set time on a taking day, else the next taking day`() {
        assertEquals(at(10, 20, 0), CycleReminderSchedule.nextPill(at(10, 8, 30), pill, takenToday = false))
        assertEquals(at(11, 20, 0), CycleReminderSchedule.nextPill(at(10, 21, 0), pill, takenToday = false))
        assertEquals(at(11, 20, 0), CycleReminderSchedule.nextPill(at(10, 8, 30), pill, takenToday = true))
        // The last taking day has passed: the pause is skipped to the next pack.
        assertEquals(at(29, 20, 0), CycleReminderSchedule.nextPill(at(21, 21, 0), pill, takenToday = false))
        assertEquals(at(29, 20, 0), CycleReminderSchedule.nextPill(at(24, 8, 30), pill, takenToday = false))
        org.junit.Assert.assertNull(CycleReminderSchedule.nextPill(at(10, 8, 30), pill.copy(packStart = null), takenToday = false))
    }

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
