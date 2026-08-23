package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // Sun 5 – Sat 11 Jul 2026, goal 11,000: 6 of 7 days met yet the week ends 606 short.
    private val week = DatePeriod(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 11))
    private val weekSteps = listOf(11_528.0, 11_248.0, 11_057.0, 12_706.0, 4_519.0, 13_214.0, 12_122.0)
        .mapIndexed { index, steps -> DailyGoalValue(week.start.plusDays(index.toLong()), steps) }
    private fun weekProgress(values: List<DailyGoalValue> = weekSteps) =
        dailyGoalProgress(values, week, target = 11_000.0, direction = DailyGoalDirection.AT_LEAST)

    @Test fun `a finished week reports the cumulative shortfall with nothing left to catch up`() {
        val balance = weekProgress().goalBalance(today = LocalDate.of(2026, 7, 20))!!

        assertEquals(-606.0, balance.balance, 0.01)
        assertEquals(7, balance.elapsedDays)
        assertEquals(0, balance.remainingDays)
        assertNull(balance.catchUpPerDay)
    }

    @Test fun `a week in progress judges only elapsed days and spreads the rest over the remaining ones`() {
        val tuesday = LocalDate.of(2026, 7, 7)
        val balance = weekProgress(weekSteps.take(3)).goalBalance(today = tuesday)!!

        assertEquals(3, balance.elapsedDays)
        assertEquals(833.0, balance.balance, 0.01)
        // Tuesday is still in progress, so Tue–Sat are five days to act on.
        assertEquals(5, balance.remainingDays)
        assertEquals((77_000.0 - 33_833.0) / 5, balance.catchUpPerDay!!, 0.01)
    }

    @Test fun `an elapsed day with nothing logged counts as zero against the goal`() {
        val balance = weekProgress(weekSteps.take(1)).goalBalance(today = LocalDate.of(2026, 7, 6))!!

        assertEquals(2, balance.elapsedDays)
        assertEquals(11_528.0 - 22_000.0, balance.balance, 0.01)
    }

    @Test fun `no catch-up once the whole period is covered or when only today remains`() {
        val ahead = weekProgress(listOf(DailyGoalValue(week.start, 80_000.0)))
            .goalBalance(today = LocalDate.of(2026, 7, 6))!!
        assertTrue(ahead.balance > 0)
        assertNull(ahead.catchUpPerDay)

        val lastDay = weekProgress(weekSteps.take(6)).goalBalance(today = week.end)!!
        assertEquals(1, lastDay.remainingDays)
        assertNull(lastDay.catchUpPerDay)
    }

    @Test fun `a range entirely in the future has no balance yet`() {
        assertNull(weekProgress(emptyList()).goalBalance(today = LocalDate.of(2026, 7, 1)))
    }

    @Test fun `at most goals flip the sign so positive still means in the user's favour`() {
        val progress = dailyGoalProgress(
            values = listOf(
                DailyGoalValue(LocalDate.of(2026, 1, 1), 1_500.0),
                DailyGoalValue(LocalDate.of(2026, 1, 2), 2_500.0),
            ),
            period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4)),
            target = 2_000.0,
            direction = DailyGoalDirection.AT_MOST,
        )
        val balance = progress.goalBalance(today = LocalDate.of(2026, 1, 2))!!

        assertEquals(0.0, balance.balance, 0.01)
        // 8,000 allowed for the period, 4,000 used, 3 days (today included) to spend the rest.
        assertEquals(4_000.0 / 3, balance.catchUpPerDay!!, 0.01)
    }
}
