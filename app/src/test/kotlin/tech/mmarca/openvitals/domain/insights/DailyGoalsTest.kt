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
        assertEquals(2, progress.currentStreakDays())
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
        assertEquals(1, progress.currentStreakDays())
        assertEquals(1, progress.longestStreakDays)
        assertEquals(166.67, progress.averageGapToGoal, 0.01)
    }

    @Test fun `a met today still counts toward the current streak`() {
        val today = LocalDate.of(2026, 1, 3)
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 2), 11.0),
                DailyGoalValue(today, 14.0),
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), today),
            target = 10.0,
            direction = DailyGoalDirection.AT_LEAST,
        )

        assertEquals(2, progress.currentStreakDays(today))
    }

    @Test fun `an unmet today is skipped by the current streak, not a break`() {
        val today = LocalDate.of(2026, 1, 3)
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 1), 12.0),
                DailyGoalValue(LocalDate.of(2026, 1, 2), 11.0),
                // Nothing logged for Jan 3 (today) yet.
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), today),
            target = 10.0,
            direction = DailyGoalDirection.AT_LEAST,
        )

        // The day is still in progress: only a PAST unmet day ends the streak.
        assertEquals(2, progress.currentStreakDays(today))
        // Once the same day lies in the past, it genuinely broke the streak.
        assertEquals(0, progress.currentStreakDays(LocalDate.of(2026, 1, 5)))
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
