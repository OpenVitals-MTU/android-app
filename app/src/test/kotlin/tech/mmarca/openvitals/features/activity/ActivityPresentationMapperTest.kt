package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.PeriodComparisonDirection
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    // The three days the metric-display cases share; all inside `weekQuery`'s
    // Monday-to-Sunday window so the goal fold sees them.
    private val day3 = LocalDate.of(2026, 5, 6)
    private val day4 = LocalDate.of(2026, 5, 7)
    private val day5 = LocalDate.of(2026, 5, 8)

    private fun metricDisplayOf(
        metric: ActivityMetric,
        rows: List<DailySteps>,
        dailyGoal: Double,
    ) = ActivityPresentationMapper.build(
        query = weekQuery,
        metric = metric,
        dailyGoal = dailyGoal,
        dailySteps = rows,
        previousDailySteps = emptyList(),
        baselineDailySteps = emptyList(),
        nutrition = emptyList(),
        previousNutrition = emptyList(),
        baselineNutrition = emptyList(),
        activityProgress = emptyList(),
    ).metric

    private fun stepsDisplay(
        rows: List<DailySteps>,
        dailyGoal: Double = MetricDailyGoalKey.STEPS.defaultValue,
    ) = metricDisplayOf(ActivityMetric.STEPS, rows, dailyGoal)

    private fun floorsDisplay(rows: List<DailySteps>) =
        metricDisplayOf(ActivityMetric.FLOORS, rows, 10.0)

    private fun dailySteps(
        date: LocalDate,
        steps: Long = 0L,
        distanceMeters: Double = 0.0,
        floorsClimbed: Int? = null,
        activeCaloriesKcal: Double? = null,
        elevationGainedMeters: Double? = null,
        wheelchairPushes: Long? = null,
    ) = DailySteps(
        date = date,
        steps = steps,
        distanceMeters = distanceMeters,
        wheelchairPushes = wheelchairPushes,
        floorsClimbed = floorsClimbed,
        activeCaloriesKcal = activeCaloriesKcal,
        elevationGainedMeters = elevationGainedMeters,
    )

    private fun nutrition(date: LocalDate, caloriesBurnedKcal: Double) =
        DailyNutrition(date, hydrationLiters = 0.0, caloriesBurnedKcal = caloriesBurnedKcal)

    private fun progressPoint(
        hour: Int,
        totalSteps: Long = 0L,
        totalFloorsClimbed: Int? = null,
    ): ActivityProgressPoint {
        val time: Instant = day5.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant()
        return ActivityProgressPoint(
            time = time,
            totalSteps = totalSteps,
            totalDistanceMeters = null,
            totalCaloriesBurnedKcal = null,
            totalFloorsClimbed = totalFloorsClimbed,
        )
    }

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

    @Test fun `steps display sums values and counts only the days with movement`() {
        val display = stepsDisplay(
            listOf(
                dailySteps(day3, steps = 9_000L),
                dailySteps(day4, steps = 0L),
                dailySteps(day5, steps = 7_000L),
            )
        )

        assertEquals(listOf(9_000.0, 0.0, 7_000.0), display.values)
        assertEquals(16_000.0, display.values.sum(), 0.0)
        assertEquals(9_000.0, display.values.maxOrNull()!!, 0.0)
        assertEquals(2, display.activeDays)
        // The zero day is not "tracked", and it is not a sample either.
        assertEquals(listOf(day3, day5), display.trackedDates)
        assertEquals(2, display.sampleCount)
    }

    @Test fun `the daily average divides by active days, not calendar days`() {
        val display = stepsDisplay(
            listOf(
                dailySteps(day3, steps = 9_000L),
                dailySteps(day4, steps = 0L),
                dailySteps(day5, steps = 7_000L),
            )
        )

        // 16 000 over the two days that moved, not over the three in the window.
        assertEquals(8_000.0, averageOrZero(display.values.sum(), display.activeDays), 0.0)
        assertEquals(8_000.0, display.baselineCurrentValue, 0.0)
    }

    @Test fun `steps display compares against the previous period total`() {
        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.STEPS,
            dailyGoal = MetricDailyGoalKey.STEPS.defaultValue,
            dailySteps = listOf(dailySteps(day5, steps = 10_000L)),
            previousDailySteps = listOf(dailySteps(day3, steps = 8_000L)),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertEquals(8_000.0, display.previousTotal, 0.0)
        assertEquals(10_000.0, display.periodComparison!!.currentValue, 0.0)
        assertEquals(PeriodComparisonDirection.UP, display.periodComparison!!.direction)
    }

    @Test fun `steps goal progress counts the days that reached the target`() {
        val display = stepsDisplay(
            listOf(
                dailySteps(day3, steps = 9_000L),
                dailySteps(day4, steps = 100L),
                dailySteps(day5, steps = 8_000L),
            ),
            dailyGoal = 8_000.0,
        )

        // 9000 and 8000 meet an at-least goal of 8000; 100 does not.
        assertEquals(2, display.goalProgress!!.goalMetDays)
        assertEquals(3, display.goalProgress!!.trackedDays)
    }

    @Test fun `a week with no rows has no data, a day always does`() {
        assertFalse(stepsDisplay(emptyList()).hasData)

        val day = ActivityPresentationMapper.build(
            query = PeriodLoadQuery(
                range = TimeRange.DAY,
                anchorDate = day5,
                weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
            ),
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
        assertTrue(day.hasData)
    }

    @Test fun `a day is described by its intraday samples`() {
        val display = ActivityPresentationMapper.build(
            query = PeriodLoadQuery(
                range = TimeRange.DAY,
                anchorDate = day5,
                weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
            ),
            metric = ActivityMetric.STEPS,
            dailyGoal = MetricDailyGoalKey.STEPS.defaultValue,
            dailySteps = listOf(dailySteps(day5, steps = 5_000L)),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = listOf(
                progressPoint(8, totalSteps = 0L),
                progressPoint(9, totalSteps = 1_200L),
                progressPoint(10, totalSteps = 5_000L),
            ),
        ).metric

        // The zero-valued sample does not count.
        assertEquals(2, display.sampleCount)
        assertEquals(3, display.intradayPoints.size)
        assertEquals(5_000.0, display.dayTotal, 0.0)
    }

    @Test fun `intraday points are dropped for a metric the device never sampled`() {
        val display = ActivityPresentationMapper.build(
            query = PeriodLoadQuery(
                range = TimeRange.DAY,
                anchorDate = day5,
                weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
            ),
            metric = ActivityMetric.FLOORS,
            dailyGoal = 10.0,
            dailySteps = listOf(dailySteps(day5, floorsClimbed = 4)),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = emptyList(),
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = listOf(
                progressPoint(9, totalSteps = 100L),
                progressPoint(10, totalFloorsClimbed = 4),
            ),
        ).metric

        // Only the point that carries a floors reading survives.
        assertEquals(1, display.intradayPoints.size)
        assertEquals(4.0, display.intradayPoints.single().value, 0.0)
    }

    @Test fun `calories burned reads the nutrition slice, not daily steps`() {
        val display = ActivityPresentationMapper.build(
            query = weekQuery,
            metric = ActivityMetric.CALORIES_BURNED,
            dailyGoal = 2_000.0,
            dailySteps = listOf(dailySteps(day5, steps = 9_999L)),
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = listOf(nutrition(day3, 2_100.0), nutrition(day5, 2_300.0)),
            previousNutrition = listOf(nutrition(day3, 2_000.0)),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        ).metric

        assertEquals(listOf(2_100.0, 2_300.0), display.values)
        assertEquals(2_000.0, display.previousTotal, 0.0)
        assertTrue(display.hasData)
    }

    @Test fun `a nullable metric has no data until a row actually carries it`() {
        val never = floorsDisplay(listOf(dailySteps(day3), dailySteps(day4)))
        assertFalse(never.hasData)

        // A recorded zero is data; an absent column is not.
        val zero = floorsDisplay(listOf(dailySteps(day3, floorsClimbed = 0)))
        assertTrue(zero.hasData)
        assertEquals(0, zero.activeDays)
    }

    @Test fun `steps has data whenever rows exist, distance needs a positive one`() {
        // Steps: the column is never null, so a row is a reading even at zero —
        // a day the user did not move is a real, chartable zero.
        assertTrue(stepsDisplay(listOf(dailySteps(day3))).hasData)

        // Distance parts company with Flutter here on purpose: a zero-distance
        // row is treated as no reading, so the screen shows its placeholder
        // rather than a flat line at the axis.
        assertFalse(metricDisplayOf(ActivityMetric.DISTANCE, listOf(dailySteps(day3)), 5_000.0).hasData)
        assertTrue(
            metricDisplayOf(
                ActivityMetric.DISTANCE,
                listOf(dailySteps(day3, distanceMeters = 1.0)),
                5_000.0,
            ).hasData,
        )
    }

    @Test fun `each metric reads its own column`() {
        val rows = listOf(
            dailySteps(
                day5,
                steps = 9_000L,
                distanceMeters = 6_500.0,
                floorsClimbed = 12,
                activeCaloriesKcal = 480.0,
                elevationGainedMeters = 95.0,
                wheelchairPushes = 1_500L,
            )
        )

        assertEquals(listOf(9_000.0), metricDisplayOf(ActivityMetric.STEPS, rows, 8_000.0).values)
        assertEquals(listOf(6_500.0), metricDisplayOf(ActivityMetric.DISTANCE, rows, 5_000.0).values)
        assertEquals(listOf(12.0), metricDisplayOf(ActivityMetric.FLOORS, rows, 10.0).values)
        assertEquals(listOf(480.0), metricDisplayOf(ActivityMetric.ACTIVE_CALORIES, rows, 400.0).values)
        assertEquals(listOf(95.0), metricDisplayOf(ActivityMetric.ELEVATION, rows, 100.0).values)
        assertEquals(
            listOf(1_500.0),
            metricDisplayOf(ActivityMetric.WHEELCHAIR_PUSHES, rows, 1_000.0).values,
        )
    }

    @Test fun `every metric maps to its own goal key`() {
        val keys = ActivityMetric.entries.map { it.dailyGoalKey }.toSet()

        assertEquals(ActivityMetric.entries.size, keys.size)
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
