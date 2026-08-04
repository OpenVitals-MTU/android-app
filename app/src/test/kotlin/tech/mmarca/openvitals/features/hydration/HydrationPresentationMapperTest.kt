package tech.mmarca.openvitals.features.hydration

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.DailyHydration
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `display has data when hydration is tracked`() {
        val dailyHydration = listOf(
            DailyHydration(anchorDate.minusDays(1), 1.5),
            DailyHydration(anchorDate, 2.0),
        )

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
        )

        assertTrue(display.hasData)
        assertEquals(3.5, display.summary.totalLiters, 0.01)
        assertEquals(2, display.summary.trackedDays)
        assertEquals(1.75, display.summary.averageLiters, 0.01)
    }

    @Test fun `display has no data for empty hydration`() {
        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = emptyList(),
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
        )

        assertFalse(display.hasData)
        assertEquals(0.0, display.summary.totalLiters, 0.01)
        assertEquals(0.0, display.summary.averageLiters, 0.01)
        assertEquals(0, display.summary.trackedDays)
        assertEquals(0, display.summary.loggedDays)
        assertEquals(0, display.summary.goalMetDays)
        assertEquals(0, display.summary.goalSuccessRatePercent)
        assertEquals(0.0, display.goalProgress, 0.01)
        assertEquals(0, display.sampleCount)
        assertTrue(display.trackedDates.isEmpty())
    }

    @Test fun `summary ignores zero intake days for averages`() {
        val dailyHydration = listOf(
            DailyHydration(anchorDate.minusDays(4), 0.0),
            DailyHydration(anchorDate.minusDays(3), 1.0),
            DailyHydration(anchorDate.minusDays(2), 2.0),
            DailyHydration(anchorDate.minusDays(1), 0.0),
            DailyHydration(anchorDate, 1.5),
        )

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
        )

        // A day with nothing logged is not a tracked day, and the average is
        // over the tracked ones.
        assertEquals(5, display.summary.loggedDays)
        assertEquals(3, display.summary.trackedDays)
        assertEquals(4.5, display.summary.totalLiters, 0.01)
        assertEquals(1.5, display.summary.averageLiters, 0.01)
        assertEquals(2.0, display.summary.bestDayLiters, 0.01)
        assertEquals(1, display.summary.currentTrackedStreakDays)
    }

    @Test fun `goal statistics use configured daily goal`() {
        val dailyHydration = listOf(
            DailyHydration(anchorDate.minusDays(3), 2.0),
            DailyHydration(anchorDate.minusDays(2), 2.5),
            DailyHydration(anchorDate.minusDays(1), 1.0),
            DailyHydration(anchorDate, 2.0),
        )

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
        )

        assertEquals(3, display.summary.goalMetDays)
        assertEquals(75, display.summary.goalSuccessRatePercent)
        assertEquals(1, display.summary.currentGoalStreakDays)
        assertEquals(2, display.summary.longestGoalStreakDays)
    }

    @Test fun `period comparison uses previous total liters`() {
        val dailyHydration = listOf(DailyHydration(anchorDate, 2.0))
        val previous = listOf(DailyHydration(anchorDate.minusDays(7), 1.0))

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = previous,
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
        )

        assertEquals(1.0, display.previousTotalLiters, 0.01)
        assertEquals(2.0, display.periodComparison.currentValue, 0.01)
        assertEquals(1.0, display.periodComparison.previousValue, 0.01)
    }

    // The selected week is 2026-05-04..2026-05-10 (Monday to Sunday).

    @Test fun `elapsed days count the whole period once it is over`() {
        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = listOf(DailyHydration(anchorDate, 2.0)),
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate.plusDays(30),
        )

        assertEquals(7, display.summary.elapsedDays)
    }

    @Test fun `a period running past today is cut at today`() {
        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = listOf(DailyHydration(anchorDate.minusDays(6), 2.0)),
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            // Wednesday: Monday, Tuesday and Wednesday have happened.
            today = anchorDate.minusDays(4),
        )

        assertEquals(3, display.summary.elapsedDays)
        // A goal you have not had the chance to miss yet must not count against you.
        assertEquals(1.0 / 3.0, display.goalProgress, 0.001)
    }

    @Test fun `a period entirely in the future has no elapsed days and no progress`() {
        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = emptyList(),
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate.minusDays(30),
        )

        assertEquals(0, display.summary.elapsedDays)
        assertEquals(0.0, display.goalProgress, 0.001)
    }

    @Test fun `goal progress divides by elapsed days not by tracked days`() {
        val dailyHydration = listOf(DailyHydration(anchorDate.minusDays(6), 2.5))

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate,
        )

        // One tracked day, hit. Dividing by tracked days filled the bar completely, which
        // rewarded you for logging less; dividing by the seven days of the week does not.
        assertEquals(1, display.summary.trackedDays)
        assertEquals(1.0 / 7.0, display.goalProgress, 0.001)
        // The tracked-day figures are deliberately unchanged.
        assertEquals(100, display.summary.goalSuccessRatePercent)
        assertEquals(2.5, display.summary.averageLiters, 0.001)
    }

    @Test fun `a goal of zero never fills the bar and never divides by zero`() {
        val dailyHydration = listOf(DailyHydration(anchorDate.minusDays(6), 2.5))

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 0.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate,
        )

        assertEquals(0, display.summary.goalMetDays)
        assertEquals(0, display.summary.goalSuccessRatePercent)
        assertEquals(0.0, display.goalProgress, 0.001)
    }

    @Test fun `goal progress is clamped to one`() {
        val dailyHydration = (0L..6L).map { offset ->
            DailyHydration(anchorDate.minusDays(6 - offset), 3.0)
        }

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate,
        )

        assertEquals(1.0, display.goalProgress, 0.001)
    }

    @Test fun `an unfinished today does not break the current goal streak`() {
        val dailyHydration = listOf(
            DailyHydration(anchorDate.minusDays(2), 2.0),
            DailyHydration(anchorDate.minusDays(1), 2.0),
            DailyHydration(anchorDate, 0.5),
        )

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate,
        )

        // Without the guard the streak collapsed to 0 at midnight and stayed there until
        // today's goal was met.
        assertEquals(2, display.summary.currentGoalStreakDays)
    }

    @Test fun `a finished day short of the goal does break the streak`() {
        val dailyHydration = listOf(
            DailyHydration(anchorDate.minusDays(2), 2.0),
            DailyHydration(anchorDate.minusDays(1), 0.5),
            DailyHydration(anchorDate, 2.0),
        )

        val display = HydrationPresentationMapper.build(
            query = weekQuery,
            dailyGoalLiters = 2.0,
            dailyHydration = dailyHydration,
            previousDailyHydration = emptyList(),
            baselineDailyHydration = emptyList(),
            crossWeightEntries = emptyList(),
            today = anchorDate,
        )

        assertEquals(1, display.summary.currentGoalStreakDays)
    }
}
