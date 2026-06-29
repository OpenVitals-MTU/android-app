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

        assertEquals(3, display.summary.trackedDays)
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
}
