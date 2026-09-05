package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.WorkoutGuidelineStatus
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.workoutGuidelineProgress
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.ExerciseData

/** The activities screen's folds: key-metric totals, sparkline buckets, strip markers and statistics. */
class ActivitiesOverviewSectionsTest {

    private val monday = LocalDate.of(2026, 3, 2)
    private val tuesday = LocalDate.of(2026, 3, 3)
    private val wednesday = LocalDate.of(2026, 3, 4)
    private val week = DatePeriod(monday, LocalDate.of(2026, 3, 8))

    // region key-metric totals

    @Test fun `key metric totals fold steps, distance, energy, cardio load and average HRV`() {
        val totals = activityOverviewTotals(
            listOf(
                day(
                    monday,
                    steps = 9_000,
                    distanceMeters = 6_000.0,
                    energyBurnedKcal = 2_200.0,
                    energySource = CaloriesBurnedSource.RECORDED_TOTAL,
                    hrvRmssdMs = 40.0,
                    cardioLoad = 30,
                    confidence = CardioLoadConfidence.HIGH,
                ),
                day(
                    tuesday,
                    steps = 7_000,
                    distanceMeters = 4_000.0,
                    energyBurnedKcal = 1_800.0,
                    energySource = CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR,
                    hrvRmssdMs = 60.0,
                    cardioLoad = 20,
                    confidence = CardioLoadConfidence.MEDIUM,
                ),
            )
        )

        assertEquals(16_000L, totals.steps)
        assertEquals(10_000.0, totals.distanceMeters, 0.001)
        assertEquals(4_000.0, totals.energyBurnedKcal, 0.001)
        assertTrue(totals.hasEnergyBurnedData)
        assertEquals(50, totals.cardioLoad)
        assertTrue(totals.hasCardioLoadData)
        // HRV is the only one that AVERAGES; the rest sum.
        assertEquals(50.0, totals.hrvRmssdMs!!, 0.001)
        // The weakest day's confidence carries the whole period.
        assertEquals(CardioLoadConfidence.MEDIUM, totals.cardioLoadConfidence)
    }

    @Test fun `a day with no cardio-load reading is left out of the sum`() {
        val days = listOf(
            day(monday, cardioLoad = 30, confidence = CardioLoadConfidence.HIGH),
            // Never scored: it must not drag the total or the confidence down.
            day(tuesday, cardioLoad = 99, confidence = CardioLoadConfidence.NO_DATA),
        )

        val totals = activityOverviewTotals(days)
        assertEquals(30, totals.cardioLoad)
        assertEquals(CardioLoadConfidence.HIGH, totals.cardioLoadConfidence)

        // ...but its bucket still charts as a zero, not as a hole.
        val series = activityOverviewMetricSeries(
            days = days,
            selectedRange = TimeRange.WEEK,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.cardioLoad.takeIf { _ -> it.cardioLoadConfidence != CardioLoadConfidence.NO_DATA }?.toDouble() }
        assertEquals(listOf(30.0, 0.0), series.values)
    }

    // endregion

    // region buckets

    @Test fun `a week is one bucket per day, in date order`() {
        val series = activityOverviewMetricSeries(
            days = listOf(
                day(wednesday, steps = 3_000),
                day(monday, steps = 1_000),
                day(tuesday, steps = 2_000),
            ),
            selectedRange = TimeRange.WEEK,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.steps.toDouble() }

        assertEquals(listOf(monday, tuesday, wednesday), series.dates)
        assertEquals(listOf(1_000.0, 2_000.0, 3_000.0), series.values)
    }

    @Test fun `a year rolls its days up into one bucket per month`() {
        val days = listOf(
            day(LocalDate.of(2026, 1, 5), steps = 1_000, hrvRmssdMs = 30.0),
            day(LocalDate.of(2026, 1, 20), steps = 2_000, hrvRmssdMs = 50.0),
            day(LocalDate.of(2026, 2, 3), steps = 4_000),
        )

        val steps = activityOverviewMetricSeries(
            days = days,
            selectedRange = TimeRange.YEAR,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.steps.toDouble() }
        val hrv = activityOverviewMetricSeries(
            days = days,
            selectedRange = TimeRange.YEAR,
            aggregation = ActivityOverviewMetricAggregation.AVERAGE,
        ) { it.hrvRmssdMs }

        assertEquals(listOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 3)), steps.dates)
        // Steps sum across the month; HRV averages across the days that have it.
        assertEquals(listOf(3_000.0, 4_000.0), steps.values)
        assertEquals(listOf(40.0, 0.0), hrv.values)
    }

    @Test fun `more days than buckets chunk down to the cap`() {
        val series = activityOverviewMetricSeries(
            days = (1..28).map { day(LocalDate.of(2026, 3, it), steps = 100) },
            selectedRange = TimeRange.MONTH,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.steps.toDouble() }

        assertEquals(7, series.dates.size)
        // 28 days over 7 buckets: four days each, 400 steps a bucket.
        assertEquals(List(7) { 400.0 }, series.values)
    }

    @Test fun `the week strip marks the days that carry a workout - week only`() {
        val session = workout(monday.atTime(7, 0), id = "w")
        val days = listOf(day(monday, workouts = listOf(session)), day(tuesday))

        val strip = activityOverviewStripBuckets(days, TimeRange.WEEK)
        assertEquals(2, strip.size)
        assertEquals(session, activityOverviewMarkerWorkout(strip.first()))
        assertNull(activityOverviewMarkerWorkout(strip.last()))

        // A month draws no strip: 28 cells, or one cell standing for four days.
        assertTrue(activityOverviewStripBuckets(days, TimeRange.MONTH).isEmpty())
    }

    @Test fun `marker is empty when day has movement metrics but no workout`() {
        val date = LocalDate.of(2026, 6, 24)
        val bucket = ActivityOverviewBucket(
            date = date,
            days = listOf(
                ActivityOverviewDay(
                    date = date,
                    steps = 5_000L,
                    distanceMeters = 4_000.0,
                    energyBurnedKcal = 300.0,
                )
            ),
        )

        assertNull(activityOverviewMarkerWorkout(bucket))
    }

    // endregion

    // region statistics

    @Test fun `statistics fold the period total, average, longest and previous total`() {
        val statistics = workoutStatisticsValues(
            workouts = listOf(
                workout(monday.atTime(7, 0), id = "a"),
                workout(tuesday.atTime(7, 0), id = "b", duration = Duration.ofMinutes(50)),
            ),
            previousWorkouts = listOf(
                workout(
                    LocalDate.of(2026, 2, 24).atTime(7, 0),
                    id = "c",
                    duration = Duration.ofMinutes(20),
                ),
            ),
        )

        assertEquals(2, statistics.workoutCount)
        assertEquals(Duration.ofMinutes(80).toMillis(), statistics.totalDurationMs)
        assertEquals(Duration.ofMinutes(40).toMillis(), statistics.averageDurationMs)
        assertEquals(Duration.ofMinutes(50).toMillis(), statistics.longestDurationMs)
        assertEquals(Duration.ofMinutes(20).toMillis(), statistics.previousTotalDurationMs)

        // The bar series is minutes per day, one entry per trained day.
        val daily = workoutDailyGoalValues(
            listOf(
                workout(monday.atTime(7, 0), id = "a"),
                workout(tuesday.atTime(7, 0), id = "b", duration = Duration.ofMinutes(50)),
            )
        ).sortedBy { it.date }
        assertEquals(listOf(30.0, 50.0), daily.map { it.value })
        assertEquals(listOf(monday, tuesday), daily.map { it.date })
    }

    @Test fun `the HHS guideline averages by week on a month or a year`() {
        val loggedMinutes = 140.0

        // A week of 140 logged minutes is 140 against the 150-minute reference.
        assertEquals(1.0, week.weekCount(), 0.0001)
        assertEquals(140.0, workoutGuidelineProgress(loggedMinutes)!!.loggedMinutes, 0.0001)

        // The same 140 minutes over a 28-day month is 35 minutes a week.
        val month = DatePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 28))
        assertEquals(4.0, month.weekCount(), 0.0001)
        assertEquals(
            35.0,
            workoutGuidelineProgress(loggedMinutes / month.weekCount())!!.loggedMinutes,
            0.0001,
        )
    }

    @Test fun `the filter options are the union with the selection, by label`() {
        // Cycling (8) and Running (56); the selection is Yoga (83).
        val options = activityTypeFilterOptions(
            availableActivityTypes = listOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ),
            selectedActivityType = ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        )

        // The absent selection is kept, and the list has a stable order.
        assertEquals(3, options.size)
        assertTrue(options.contains(ExerciseSessionRecord.EXERCISE_TYPE_YOGA))
        assertEquals(options.sorted(), options)
    }

    // endregion

    @Test fun `an empty period derives zeroes and nulls, not a crash`() {
        val totals = activityOverviewTotals(emptyList())
        assertEquals(0L, totals.steps)
        assertEquals(0.0, totals.distanceMeters, 0.0)
        assertEquals(0.0, totals.energyBurnedKcal, 0.0)
        assertEquals(false, totals.hasEnergyBurnedData)
        assertEquals(0, totals.cardioLoad)
        assertEquals(false, totals.hasCardioLoadData)
        assertEquals(CardioLoadConfidence.NO_DATA, totals.cardioLoadConfidence)
        assertNull(totals.hrvRmssdMs)

        val series = activityOverviewMetricSeries(
            days = emptyList(),
            selectedRange = TimeRange.WEEK,
            aggregation = ActivityOverviewMetricAggregation.SUM,
        ) { it.steps.toDouble() }
        assertTrue(series.dates.isEmpty())
        assertTrue(series.values.isEmpty())
        assertTrue(activityOverviewStripBuckets(emptyList(), TimeRange.WEEK).isEmpty())

        val statistics = workoutStatisticsValues(emptyList(), emptyList())
        assertEquals(0, statistics.workoutCount)
        assertEquals(0L, statistics.totalDurationMs)
        assertEquals(0L, statistics.averageDurationMs)
        assertEquals(0L, statistics.longestDurationMs)
        assertTrue(workoutDailyGoalValues(emptyList()).isEmpty())

        // A period with no workouts still has a goal to have missed.
        val goal = dailyGoalProgress(
            values = workoutDailyGoalValues(emptyList()),
            period = week,
            target = MetricDailyGoalKey.WORKOUT_MINUTES.defaultValue,
            direction = MetricDailyGoalKey.WORKOUT_MINUTES.direction,
        )
        assertEquals(0, goal.goalMetDays)
        val guideline = workoutGuidelineProgress(0.0)
        assertNotNull(guideline)
        assertEquals(WorkoutGuidelineStatus.NO_LOGGED_MINUTES, guideline!!.status)
    }

    private fun day(
        date: LocalDate,
        steps: Long = 0L,
        distanceMeters: Double = 0.0,
        energyBurnedKcal: Double = 0.0,
        energySource: CaloriesBurnedSource = CaloriesBurnedSource.NO_DATA,
        hrvRmssdMs: Double? = null,
        cardioLoad: Int = 0,
        confidence: CardioLoadConfidence = CardioLoadConfidence.NO_DATA,
        workouts: List<ExerciseData> = emptyList(),
    ) = ActivityOverviewDay(
        date = date,
        steps = steps,
        distanceMeters = distanceMeters,
        energyBurnedKcal = energyBurnedKcal,
        energyBurnedSource = energySource,
        workouts = workouts,
        hrvRmssdMs = hrvRmssdMs,
        cardioLoadScore = CardioLoadEstimate(score = cardioLoad, confidence = confidence),
    )

    private fun workout(
        start: java.time.LocalDateTime,
        id: String = "w",
        type: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        duration: Duration = Duration.ofMinutes(30),
    ): ExerciseData {
        val startInstant: Instant = start.atZone(ZoneId.systemDefault()).toInstant()
        return ExerciseData(
            id = id,
            title = null,
            exerciseType = type,
            startTime = startInstant,
            endTime = startInstant.plus(duration),
            durationMs = duration.toMillis(),
            source = "test",
        )
    }
}
