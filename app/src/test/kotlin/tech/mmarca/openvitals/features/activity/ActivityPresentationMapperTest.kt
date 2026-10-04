package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.PeriodComparisonDirection
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    // Three days inside `weekQuery`'s Monday-to-Sunday window, so the goal fold sees them.
    private val day3 = LocalDate.of(2026, 5, 6)
    private val day4 = LocalDate.of(2026, 5, 7)
    private val day5 = LocalDate.of(2026, 5, 8)

    private val dayQuery = PeriodLoadQuery(
        range = TimeRange.DAY,
        anchorDate = day5,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    private fun displayFor(
        metric: ActivityMetric = ActivityMetric.STEPS,
        dailySteps: List<DailySteps> = emptyList(),
        dailyGoal: Double = metric.dailyGoalKey.defaultValue,
        query: PeriodLoadQuery = weekQuery,
        previousDailySteps: List<DailySteps> = emptyList(),
        nutrition: List<DailyNutrition> = emptyList(),
        previousNutrition: List<DailyNutrition> = emptyList(),
        activityProgress: List<ActivityProgressPoint> = emptyList(),
    ) = ActivityPresentationMapper.build(
        query = query,
        metric = metric,
        dailyGoal = dailyGoal,
        dailySteps = dailySteps,
        previousDailySteps = previousDailySteps,
        baselineDailySteps = emptyList(),
        nutrition = nutrition,
        previousNutrition = previousNutrition,
        baselineNutrition = emptyList(),
        activityProgress = activityProgress,
    ).metric

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

    @Test fun `steps display sums values and counts only the days with movement`() {
        val display = displayFor(
            dailySteps = listOf(
                dailySteps(day3, steps = 9_000L),
                dailySteps(day4, steps = 0L),
                dailySteps(day5, steps = 7_000L),
            ),
        )

        assertTrue(display.hasData)
        assertEquals(listOf(9_000.0, 0.0, 7_000.0), display.values)
        assertEquals(16_000.0, display.values.sum(), 0.0)
        assertEquals(9_000.0, display.values.maxOrNull()!!, 0.0)
        assertEquals(2, display.activeDays)
        // The zero day is not "tracked", and it is not a sample either.
        assertEquals(listOf(day3, day5), display.trackedDates)
        assertEquals(2, display.sampleCount)
    }

    @Test fun `the daily average divides by active days, not calendar days`() {
        val display = displayFor(
            dailySteps = listOf(
                dailySteps(day3, steps = 9_000L),
                dailySteps(day4, steps = 0L),
                dailySteps(day5, steps = 7_000L),
            ),
        )

        // 16 000 over the two days that moved, not over the three in the window.
        assertEquals(8_000.0, averageOrZero(display.values.sum(), display.activeDays), 0.0)
        assertEquals(8_000.0, display.baselineCurrentValue, 0.0)
    }

    @Test fun `steps display compares against the previous period total`() {
        val display = displayFor(
            dailySteps = listOf(dailySteps(day5, steps = 10_000L)),
            previousDailySteps = listOf(dailySteps(day3, steps = 8_000L)),
        )

        assertEquals(8_000.0, display.previousTotal, 0.0)
        assertEquals(10_000.0, display.periodComparison!!.currentValue, 0.0)
        assertEquals(PeriodComparisonDirection.UP, display.periodComparison!!.direction)
    }

    @Test fun `steps display computes goal progress`() {
        val display = displayFor(
            dailySteps = listOf(
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
        val week = displayFor(dailySteps = emptyList())
        assertFalse(week.hasData)
        assertTrue(week.values.isEmpty())

        assertTrue(displayFor(query = dayQuery).hasData)
    }

    @Test fun `a day is described by its intraday samples`() {
        val display = displayFor(
            query = dayQuery,
            dailySteps = listOf(dailySteps(day5, steps = 5_000L)),
            activityProgress = listOf(
                progressPoint(8, totalSteps = 0L),
                progressPoint(9, totalSteps = 1_200L),
                progressPoint(10, totalSteps = 5_000L),
            ),
        )

        // The zero-valued sample does not count.
        assertEquals(2, display.sampleCount)
        assertEquals(3, display.intradayPoints.size)
        assertEquals(5_000.0, display.dayTotal, 0.0)
    }

    @Test fun `intraday points are dropped for a metric the device never sampled`() {
        val display = displayFor(
            metric = ActivityMetric.FLOORS,
            query = dayQuery,
            dailySteps = listOf(dailySteps(day5, floorsClimbed = 4)),
            activityProgress = listOf(
                progressPoint(9, totalSteps = 100L),
                progressPoint(10, totalFloorsClimbed = 4),
            ),
        )

        // Only the point that carries a floors reading survives.
        assertEquals(1, display.intradayPoints.size)
        assertEquals(4.0, display.intradayPoints.single().value, 0.0)
    }

    @Test fun `calories burned reads the nutrition slice, not daily steps`() {
        val display = displayFor(
            metric = ActivityMetric.CALORIES_BURNED,
            dailyGoal = 2_000.0,
            dailySteps = listOf(dailySteps(day5, steps = 9_999L)),
            nutrition = listOf(nutrition(day3, 2_100.0), nutrition(day5, 2_300.0)),
            previousNutrition = listOf(nutrition(day3, 2_000.0)),
        )

        assertEquals(listOf(2_100.0, 2_300.0), display.values)
        assertEquals(2, display.activeDays)
        assertEquals(2_000.0, display.previousTotal, 0.0)
        assertTrue(display.hasData)
        // The goal fold reads the same slice: both days clear the 2000 kcal target.
        assertEquals(2, display.goalProgress!!.goalMetDays)
    }

    @Test fun `calories burned display has no data when nutrition has no burned calories`() {
        val display = displayFor(
            metric = ActivityMetric.CALORIES_BURNED,
            nutrition = listOf(nutrition(anchorDate, 0.0)),
        )

        assertFalse(display.hasData)
        assertEquals(listOf(0.0), display.values)
    }

    @Test fun `a nullable metric has no data until a row actually carries it`() {
        val never = displayFor(ActivityMetric.FLOORS, listOf(dailySteps(day3), dailySteps(day4)))
        assertFalse(never.hasData)

        // A recorded zero is data; an absent column is not.
        val zero = displayFor(ActivityMetric.FLOORS, listOf(dailySteps(day3, floorsClimbed = 0)))
        assertTrue(zero.hasData)
        assertEquals(0, zero.activeDays)
    }

    @Test fun `steps has data whenever rows exist, distance needs a positive one`() {
        // Steps: the column is never null, so a zero row is a real, chartable zero.
        assertTrue(displayFor(ActivityMetric.STEPS, listOf(dailySteps(day3))).hasData)

        // Distance diverges from Flutter on purpose: a zero-distance row is no reading.
        assertFalse(displayFor(ActivityMetric.DISTANCE, listOf(dailySteps(day3))).hasData)
        assertTrue(
            displayFor(ActivityMetric.DISTANCE, listOf(dailySteps(day3, distanceMeters = 1.0))).hasData,
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
        val metrics = listOf(
            ActivityMetric.STEPS,
            ActivityMetric.DISTANCE,
            ActivityMetric.FLOORS,
            ActivityMetric.ACTIVE_CALORIES,
            ActivityMetric.ELEVATION,
            ActivityMetric.WHEELCHAIR_PUSHES,
        )

        assertEquals(
            mapOf(
                ActivityMetric.STEPS to listOf(9_000.0),
                ActivityMetric.DISTANCE to listOf(6_500.0),
                ActivityMetric.FLOORS to listOf(12.0),
                ActivityMetric.ACTIVE_CALORIES to listOf(480.0),
                ActivityMetric.ELEVATION to listOf(95.0),
                ActivityMetric.WHEELCHAIR_PUSHES to listOf(1_500.0),
            ),
            metrics.associateWith { displayFor(it, rows).values },
        )
    }

    @Test fun `every metric maps to its own goal key`() {
        val keys = ActivityMetric.entries.map { it.dailyGoalKey }.toSet()

        assertEquals(ActivityMetric.entries.size, keys.size)
    }
}
