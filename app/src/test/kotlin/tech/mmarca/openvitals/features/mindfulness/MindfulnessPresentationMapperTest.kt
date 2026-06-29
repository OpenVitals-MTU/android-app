package tech.mmarca.openvitals.features.mindfulness

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfulnessPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `display has data when sessions exist`() {
        val now = Instant.parse("2026-05-10T10:00:00Z")
        val sessions = listOf(
            MindfulnessSession("1", "Breathing", now.minusSeconds(1_800), now, 1_800_000, "test"),
            MindfulnessSession("2", "Meditation", now.minusSeconds(3_600), now, 3_600_000, "test"),
        )

        val display = MindfulnessPresentationMapper.build(
            query = weekQuery,
            dailyGoalMinutes = 15.0,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossSleepSessions = emptyList(),
        )

        assertTrue(display.hasData)
        assertEquals(90L, display.summary.totalMinutes)
        assertEquals(2, display.summary.sessionCount)
        assertEquals(2, display.sampleCount)
        assertNotNull(display.goalProgress)
    }

    @Test fun `display has no data for empty sessions`() {
        val display = MindfulnessPresentationMapper.build(
            query = weekQuery,
            dailyGoalMinutes = 15.0,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = emptyList(),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossSleepSessions = emptyList(),
        )

        assertFalse(display.hasData)
        assertEquals(0L, display.summary.totalMinutes)
        assertTrue(display.dailyMinutes.isEmpty())
    }

    @Test fun `period comparison uses previous session totals`() {
        val now = Instant.parse("2026-05-10T10:00:00Z")
        val sessions = listOf(
            MindfulnessSession("1", "Breathing", now.minusSeconds(1_800), now, 1_800_000, "test"),
        )
        val previous = listOf(
            MindfulnessSession("2", "Meditation", now.minusSeconds(900), now, 900_000, "test"),
        )

        val display = MindfulnessPresentationMapper.build(
            query = weekQuery,
            dailyGoalMinutes = 15.0,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = previous,
            baselineSessions = emptyList(),
            crossSleepSessions = emptyList(),
        )

        assertEquals(1_800_000.0, display.periodComparison.currentValue, 0.01)
        assertEquals(900_000.0, display.periodComparison.previousValue, 0.01)
        assertEquals(900_000L, display.previousTotalMs)
    }

    @Test fun `daily minutes aggregate sessions by date`() {
        val zone = ZoneId.systemDefault()
        val dayOne = anchorDate.minusDays(1).atStartOfDay(zone).plusHours(12).toInstant()
        val dayTwo = anchorDate.atStartOfDay(zone).plusHours(12).toInstant()
        val sessions = listOf(
            MindfulnessSession("1", "A", dayOne.minusSeconds(600), dayOne, 600_000, "test"),
            MindfulnessSession("2", "B", dayOne.minusSeconds(1_200), dayOne, 600_000, "test"),
            MindfulnessSession("3", "C", dayTwo.minusSeconds(1_800), dayTwo, 1_800_000, "test"),
        )

        val display = MindfulnessPresentationMapper.build(
            query = weekQuery,
            dailyGoalMinutes = 15.0,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossSleepSessions = emptyList(),
        )

        assertEquals(2, display.dailyMinutes.size)
        assertEquals(50.0, display.dailyMinutes.sumOf { it.minutes }, 0.01)
    }
}
