package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod

class DailyGoalsTest {

    @Test fun `at least goals count tracked days and streaks`() {
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 1), 8.0),
                DailyGoalValue(LocalDate.of(2026, 1, 2), 6.0),
                DailyGoalValue(LocalDate.of(2026, 1, 4), 10.0),
                DailyGoalValue(LocalDate.of(2026, 1, 5), 12.0),
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5)),
            target = 10.0,
            direction = DailyGoalDirection.AT_LEAST,
        )

        assertEquals(4, progress.trackedDays)
        assertEquals(2, progress.goalMetDays)
        assertEquals(50, progress.successRatePercent)
        assertEquals(2, progress.currentStreakDays)
        assertEquals(2, progress.longestStreakDays)
        assertEquals(1.5, progress.averageGapToGoal, 0.01)
    }

    @Test fun `at most goals ignore missing days and count only logged values`() {
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 1), 1_500.0),
                DailyGoalValue(LocalDate.of(2026, 1, 2), 2_500.0),
                DailyGoalValue(LocalDate.of(2026, 1, 4), 1_800.0),
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4)),
            target = 2_000.0,
            direction = DailyGoalDirection.AT_MOST,
        )

        val missingDay = progress.days.single { it.date == LocalDate.of(2026, 1, 3) }
        assertFalse(missingDay.isTracked)
        assertFalse(missingDay.isMet)
        assertEquals(3, progress.trackedDays)
        assertEquals(2, progress.goalMetDays)
        assertEquals(67, progress.successRatePercent)
        assertEquals(1, progress.currentStreakDays)
        assertEquals(1, progress.longestStreakDays)
        assertEquals(166.67, progress.averageGapToGoal, 0.01)
    }

    @Test fun `values on the same day are summed before goal evaluation`() {
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 1), 3.0),
                DailyGoalValue(LocalDate.of(2026, 1, 1), 4.0),
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
            target = 6.0,
            direction = DailyGoalDirection.AT_LEAST,
        )

        assertEquals(7.0, progress.days.single().value, 0.01)
        assertTrue(progress.days.single().isMet)
    }
}
