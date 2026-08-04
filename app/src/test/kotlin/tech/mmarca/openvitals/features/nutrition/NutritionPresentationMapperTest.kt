package tech.mmarca.openvitals.features.nutrition

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.DailyMacros
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `totals sum daily macros`() {
        val dailyMacros = listOf(
            DailyMacros(
                date = anchorDate.minusDays(1),
                energyKcal = 1_900.0,
                proteinGrams = 90.0,
                carbsGrams = 220.0,
                fatGrams = 60.0,
            ),
            DailyMacros(
                date = anchorDate,
                energyKcal = 2_100.0,
                proteinGrams = 100.0,
                carbsGrams = 250.0,
                fatGrams = 70.0,
            ),
        )

        val display = NutritionPresentationMapper.build(
            query = weekQuery,
            metric = NutritionMetric.CALORIES_IN,
            dailyGoal = 2_000.0,
            dailyMacros = dailyMacros,
            previousDailyMacros = emptyList(),
            baselineDailyMacros = emptyList(),
            entries = emptyList(),
        )

        assertTrue(display.hasData)
        assertEquals(4_000.0, display.totals.energyKcal, 0.01)
        assertEquals(190.0, display.totals.proteinGrams, 0.01)
        assertEquals(470.0, display.totals.carbsGrams, 0.01)
        assertEquals(130.0, display.totals.fatGrams, 0.01)
    }

    @Test fun `display has no data for empty macros and entries`() {
        val display = NutritionPresentationMapper.build(
            query = weekQuery,
            metric = NutritionMetric.PROTEIN,
            dailyGoal = 100.0,
            dailyMacros = emptyList(),
            previousDailyMacros = emptyList(),
            baselineDailyMacros = emptyList(),
            entries = emptyList(),
        )

        assertFalse(display.hasData)
        assertEquals(0.0, display.totals.energyKcal, 0.01)
        assertFalse(display.metric.hasData)
    }

    @Test fun `metric display tracks goal progress and period comparison`() {
        val dailyMacros = listOf(
            DailyMacros(date = anchorDate.minusDays(1), proteinGrams = 80.0),
            DailyMacros(date = anchorDate, proteinGrams = 120.0),
        )
        val previous = listOf(
            DailyMacros(date = anchorDate.minusDays(8), proteinGrams = 50.0),
        )

        val display = NutritionPresentationMapper.build(
            query = weekQuery,
            metric = NutritionMetric.PROTEIN,
            dailyGoal = 100.0,
            dailyMacros = dailyMacros,
            previousDailyMacros = previous,
            baselineDailyMacros = emptyList(),
            entries = emptyList(),
        )

        assertTrue(display.metric.hasData)
        assertEquals(200.0, display.metric.totalValue, 0.01)
        assertEquals(50.0, display.metric.previousTotal, 0.01)
        assertEquals(200.0, display.metric.periodComparison.currentValue, 0.01)
        assertEquals(50.0, display.metric.periodComparison.previousValue, 0.01)
        assertEquals(2, display.metric.loggedDays)
        assertEquals(100.0, display.metric.averageValue, 0.01)
        assertNotNull(display.metric.goalProgress)
    }

    @Test fun `macro split is computed when macros are present`() {
        val dailyMacros = listOf(
            DailyMacros(
                date = anchorDate,
                proteinGrams = 100.0,
                carbsGrams = 200.0,
                fatGrams = 50.0,
            ),
        )

        val display = NutritionPresentationMapper.build(
            query = weekQuery,
            metric = NutritionMetric.CALORIES_IN,
            dailyGoal = 2_000.0,
            dailyMacros = dailyMacros,
            previousDailyMacros = emptyList(),
            baselineDailyMacros = emptyList(),
            entries = emptyList(),
        )

        assertNotNull(display.macroSplit)
    }
}
