package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `steps display populates values for week period`() {
        val dailySteps = listOf(
            DailySteps(anchorDate.minusDays(1), 6_000L, 4_800.0),
            DailySteps(anchorDate, 8_000L, 6_400.0),
        )

        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.STEPS,
            dailyGoal = MetricDailyGoalKey.STEPS.defaultValue,
            dailySteps = dailySteps,
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertTrue(display.hasData)
        assertEquals(listOf(6_000.0, 8_000.0), display.values)
        assertEquals(2, display.activeDays)
    }

    @Test fun `steps display has no data for empty week period`() {
        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.STEPS,
            dailyGoal = MetricDailyGoalKey.STEPS.defaultValue,
            dailySteps = emptyList(),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertFalse(display.hasData)
        assertTrue(display.values.isEmpty())
    }

    @Test fun `steps display computes goal progress`() {
        val dailySteps = listOf(DailySteps(anchorDate, 12_000L, 9_600.0))

        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.STEPS,
            dailyGoal = 10_000.0,
            dailySteps = dailySteps,
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertNotNull(display.goalProgress)
        assertEquals(1, display.goalProgress!!.goalMetDays)
    }

    @Test fun `calories burned display populates values for week period`() {
        val nutrition = listOf(
            DailyNutrition(anchorDate.minusDays(1), hydrationLiters = 0.0, caloriesBurnedKcal = 500.0),
            DailyNutrition(anchorDate, hydrationLiters = 0.0, caloriesBurnedKcal = 700.0),
        )

        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.CALORIES_BURNED,
            dailyGoal = MetricDailyGoalKey.CALORIES_OUT_KCAL.defaultValue,
            dailySteps = emptyList(),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = nutrition,
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertTrue(display.hasData)
        assertEquals(listOf(500.0, 700.0), display.values)
        assertEquals(2, display.activeDays)
    }

    @Test fun `calories burned display has no data when nutrition has no burned calories`() {
        val nutrition = listOf(DailyNutrition(anchorDate, hydrationLiters = 0.0, caloriesBurnedKcal = 0.0))

        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.CALORIES_BURNED,
            dailyGoal = MetricDailyGoalKey.CALORIES_OUT_KCAL.defaultValue,
            dailySteps = emptyList(),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = nutrition,
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertFalse(display.hasData)
        assertTrue(display.values.all { it == 0.0 })
    }

    @Test fun `calories burned display computes goal progress`() {
        val nutrition = listOf(DailyNutrition(anchorDate, hydrationLiters = 0.0, caloriesBurnedKcal = 2_500.0))

        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.CALORIES_BURNED,
            dailyGoal = 2_000.0,
            dailySteps = emptyList(),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = nutrition,
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertNotNull(display.goalProgress)
        assertEquals(1, display.goalProgress!!.goalMetDays)
    }
}
