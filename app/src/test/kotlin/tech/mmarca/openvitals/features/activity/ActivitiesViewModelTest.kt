package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.query.ActivitiesPeriodData
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun emptyRepo() = mockk<ActivityRepository>().also { repo ->
        coEvery { repo.loadWorkouts(any(), any()) } returns emptyList()
        coEvery { repo.loadWorkoutsWithMetrics(any(), any()) } coAnswers {
            repo.loadWorkouts(firstArg(), secondArg())
        }
        coEvery { repo.loadPlannedWorkouts(any(), any()) } returns emptyList()
        coEvery { repo.loadDailySteps(any(), any()) } returns emptyList()
        coEvery { repo.loadDailyNutrition(any(), any()) } returns emptyList()
        coEvery { repo.deleteActivityEntry(any()) } returns Unit
        coEvery { repo.loadActivitiesPeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            ActivitiesPeriodData(
                workouts = repo.loadWorkouts(windows.current.start, windows.current.end),
                previousWorkouts = repo.loadWorkouts(windows.previous.start, windows.previous.end),
                baselineWorkouts = repo.loadWorkouts(windows.baseline.start, windows.baseline.end),
            )
        }
    }

    @Test fun `deleteActivityEntry removes OpenVitals workout and reloads`() = runTest {
        val workout = workout(
            id = "activity-id",
            source = "tech.mmarca.openvitals.debug",
            isOpenVitalsEntry = true,
        )
        var workouts = listOf(workout)
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } answers { workouts }
        coEvery { repo.deleteActivityEntry("activity-id") } coAnswers {
            workouts = emptyList()
        }
        val vm = ActivitiesViewModel(repo)

        vm.deleteActivityEntry("activity-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.workouts.isEmpty())
        coVerify { repo.deleteActivityEntry("activity-id") }
        coVerify(atLeast = 2) { repo.loadWorkouts(any(), any()) }
    }

    @Test fun `deleteActivityEntry ignores workout not created by OpenVitals`() = runTest {
        val workouts = listOf(
            workout(
                id = "external-activity-id",
                source = "com.example",
                isOpenVitalsEntry = false,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns workouts
        val vm = ActivitiesViewModel(repo)

        vm.deleteActivityEntry("external-activity-id")
        advanceUntilIdle()

        assertEquals(workouts, vm.uiState.value.workouts)
        coVerify(exactly = 0) { repo.deleteActivityEntry("external-activity-id") }
    }

    @Test fun `last seven days week mode loads and displays rolling seven day window`() = runTest {
        val repo = emptyRepo()
        val today = LocalDate.now()
        val vm = ActivitiesViewModel(
            repository = repo,
            initialActivityWeekMode = ActivityWeekMode.LAST_7_DAYS,
        )

        advanceUntilIdle()

        val expectedDates = (0..6).map { today.minusDays(6).plusDays(it.toLong()) }
        assertEquals(expectedDates, vm.uiState.value.overviewDays.map { it.date })
        coVerify { repo.loadWorkoutsWithMetrics(today.minusDays(6), today) }
        coVerify { repo.loadDailySteps(today.minusDays(6), today) }
        coVerify { repo.loadDailyNutrition(today.minusDays(6), today) }
    }

    @Test fun `monday to sunday week mode displays all seven days including empty future days`() = runTest {
        val repo = emptyRepo()
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val vm = ActivitiesViewModel(
            repository = repo,
            initialActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
        )

        advanceUntilIdle()

        val expectedDates = (0..6).map { weekStart.plusDays(it.toLong()) }
        assertEquals(expectedDates, vm.uiState.value.overviewDays.map { it.date })
        coVerify { repo.loadWorkoutsWithMetrics(weekStart, weekEnd.coerceAtMost(today)) }
        coVerify { repo.loadDailySteps(weekStart, weekEnd.coerceAtMost(today)) }
        coVerify { repo.loadDailyNutrition(weekStart, weekEnd.coerceAtMost(today)) }
    }

    @Test fun `selectActivityType filters loaded activities without changing selected period`() = runTest {
        val walk = workout(
            id = "walk",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        )
        val bike = workout(
            id = "bike",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        )
        val bikePlan = plannedWorkout(
            id = "bike-plan",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns listOf(walk, bike)
        coEvery { repo.loadPlannedWorkouts(any(), any()) } returns listOf(bikePlan)
        val vm = ActivitiesViewModel(repo)
        val initialRange = vm.uiState.value.selectedRange
        val initialDate = vm.uiState.value.selectedDate

        vm.selectActivityType(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(initialRange, state.selectedRange)
        assertEquals(initialDate, state.selectedDate)
        assertEquals(listOf("bike"), state.workouts.map { it.id })
        assertEquals(listOf("bike-plan"), state.plannedWorkouts.map { it.id })
        assertEquals(listOf("bike"), state.previousWorkouts.map { it.id })
        assertEquals(listOf("bike"), state.baselineWorkouts.map { it.id })
        assertTrue(state.overviewDays.flatMap { it.workouts }.all { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING })
        assertEquals(
            listOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            ),
            state.availableActivityTypes,
        )
    }

    @Test fun `selectActivityType all restores loaded activities`() = runTest {
        val walk = workout(
            id = "walk",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        )
        val bike = workout(
            id = "bike",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns listOf(walk, bike)
        val vm = ActivitiesViewModel(repo)

        vm.selectActivityType(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)
        vm.selectActivityType(null)
        advanceUntilIdle()

        assertEquals(listOf("walk", "bike"), vm.uiState.value.workouts.map { it.id })
        assertEquals(null, vm.uiState.value.selectedActivityType)
    }

    @Test fun `loaded activities expose aggregate stats by activity type`() = runTest {
        val walk = workout(
            id = "walk",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            totalDistanceMeters = 1_000.0,
            durationMs = 600_000,
        )
        val bike = workout(
            id = "bike",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            totalDistanceMeters = 2_000.0,
            durationMs = 300_000,
            averageSpeedMetersPerSecond = 8.0,
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns listOf(walk, bike)
        val vm = ActivitiesViewModel(repo)

        advanceUntilIdle()

        val aggregates = vm.uiState.value.activityTypeAggregates.associateBy { it.exerciseType }
        assertEquals(2, aggregates.size)
        assertEquals(1, aggregates.getValue(ExerciseSessionRecord.EXERCISE_TYPE_WALKING).count)
        assertEquals(1_000.0, aggregates.getValue(ExerciseSessionRecord.EXERCISE_TYPE_WALKING).totalDistanceMeters, 0.01)
        assertEquals(600_000, aggregates.getValue(ExerciseSessionRecord.EXERCISE_TYPE_WALKING).totalMovingDurationMs)
        assertEquals(1, aggregates.getValue(ExerciseSessionRecord.EXERCISE_TYPE_BIKING).count)
        assertEquals(8.0, aggregates.getValue(ExerciseSessionRecord.EXERCISE_TYPE_BIKING).bestSpeedMetersPerSecond ?: 0.0, 0.01)
    }

    @Test fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadWorkoutsWithMetrics(any(), any()) } throws
            SecurityException("exercise read")
        val vm = ActivitiesViewModel(repo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(ScreenError.PermissionDenied, state.error)
        assertTrue(state.workouts.isEmpty())
    }

    @Test fun `an unexpected failure carries its message to the screen`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadWorkoutsWithMetrics(any(), any()) } throws
            IllegalStateException("the provider hung up")
        val vm = ActivitiesViewModel(repo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(ScreenError.Message("the provider hung up"), state.error)
    }

    @Test fun `the type filter re-slices the loaded result without reloading`() = runTest {
        val run = workout(
            id = "run",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            durationMs = 30 * 60_000L,
        )
        val ride = workout(
            id = "ride",
            source = "com.example",
            isOpenVitalsEntry = false,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            durationMs = 60 * 60_000L,
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns listOf(run, ride)
        val vm = ActivitiesViewModel(repo)
        advanceUntilIdle()
        assertEquals(2, workoutStatisticsValues(vm.uiState.value.workouts, emptyList()).workoutCount)

        vm.selectActivityType(ExerciseSessionRecord.EXERCISE_TYPE_BIKING)
        advanceUntilIdle()

        val state = vm.uiState.value
        // The cached result is re-sliced, not refetched.
        coVerify(exactly = 1) { repo.loadWorkoutsWithMetrics(any(), any()) }
        assertEquals(listOf("ride"), state.workouts.map { it.id })
        val statistics = workoutStatisticsValues(state.workouts, state.previousWorkouts)
        assertEquals(1, statistics.workoutCount)
        assertEquals(60 * 60_000L, statistics.totalDurationMs)
        // The dropdown still offers both types, whatever the slice.
        assertEquals(
            listOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ),
            activityTypeFilterOptions(state.availableActivityTypes, state.selectedActivityType),
        )
    }

    @Test fun `moving the daily goal re-derives the goal progress`() = runTest {
        // 45 minutes today: it clears a 30-minute goal and misses a 60-minute one.
        val today = LocalDate.now()
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns listOf(
            workout(
                id = "run",
                source = "com.example",
                isOpenVitalsEntry = false,
                durationMs = 45 * 60_000L,
            )
        )
        val persisted = mutableListOf<Double>()
        val vm = ActivitiesViewModel(repo, onDailyGoalChanged = { persisted += it })
        advanceUntilIdle()

        val period = DatePeriod(today, today)
        val goalKey = MetricDailyGoalKey.WORKOUT_MINUTES
        fun metDays(target: Double) = dailyGoalProgress(
            values = workoutDailyGoalValues(vm.uiState.value.workouts),
            period = period,
            target = target,
            direction = goalKey.direction,
        ).goalMetDays

        assertEquals(30.0, vm.uiState.value.dailyGoalMinutes, 0.0)
        assertEquals(1, metDays(vm.uiState.value.dailyGoalMinutes))

        // Six 5-minute steps: 30 → 60.
        repeat(6) { vm.increaseDailyGoal() }
        advanceUntilIdle()

        assertEquals(60.0, vm.uiState.value.dailyGoalMinutes, 0.0)
        assertEquals(listOf(35.0, 40.0, 45.0, 50.0, 55.0, 60.0), persisted)
        // Moving the goal never refetches: it is a derivation, not a load.
        coVerify(exactly = 1) { repo.loadWorkoutsWithMetrics(any(), any()) }
        assertEquals(0, metDays(vm.uiState.value.dailyGoalMinutes))
    }

    @Test fun `decreasing the daily goal stops at the floor`() = runTest {
        val repo = emptyRepo()
        val persisted = mutableListOf<Double>()
        val vm = ActivitiesViewModel(
            repo,
            initialDailyGoalMinutes = 10.0,
            onDailyGoalChanged = { persisted += it },
        )
        advanceUntilIdle()

        repeat(3) { vm.decreaseDailyGoal() }

        assertEquals(MetricDailyGoalKey.WORKOUT_MINUTES.minValue, vm.uiState.value.dailyGoalMinutes, 0.0)
        assertEquals(listOf(5.0, 5.0, 5.0), persisted)
    }

    @Test fun `a stale load cannot overwrite the newer one it lost to`() = runTest {
        val repo = emptyRepo()
        val gates = mutableListOf<CompletableDeferred<List<ExerciseData>>>()
        coEvery { repo.loadWorkoutsWithMetrics(any(), any()) } coAnswers {
            CompletableDeferred<List<ExerciseData>>().also { gates += it }.await()
        }
        val vm = ActivitiesViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, gates.size)

        // The month selection supersedes the week load still on the wire.
        vm.selectRange(TimeRange.MONTH)
        advanceUntilIdle()
        assertEquals(2, gates.size)

        // The week's answer lands after it was superseded: dropped, not painted.
        gates[0].complete(
            listOf(
                workout(
                    id = "week",
                    source = "com.example",
                    isOpenVitalsEntry = false,
                    durationMs = 99 * 60_000L,
                )
            )
        )
        advanceUntilIdle()
        gates[1].complete(
            listOf(
                workout(
                    id = "month",
                    source = "com.example",
                    isOpenVitalsEntry = false,
                    durationMs = 10 * 60_000L,
                )
            )
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TimeRange.MONTH, state.selectedRange)
        assertEquals(listOf("month"), state.workouts.map { it.id })
        assertEquals(
            10 * 60_000L,
            workoutStatisticsValues(state.workouts, emptyList()).totalDurationMs,
        )
    }

    @Test fun `only the current window pays for the per-session route metrics`() = runTest {
        val repo = mockk<ActivityRepository>().also { mock ->
            coEvery { mock.loadWorkouts(any(), any()) } returns emptyList()
            coEvery { mock.loadWorkoutsWithMetrics(any(), any()) } returns emptyList()
            coEvery { mock.loadPlannedWorkouts(any(), any()) } returns emptyList()
            coEvery { mock.loadDailySteps(any(), any()) } returns emptyList()
            coEvery { mock.loadDailyNutrition(any(), any()) } returns emptyList()
        }
        val today = LocalDate.now()
        ActivitiesViewModel(repo, initialActivityWeekMode = ActivityWeekMode.LAST_7_DAYS)

        advanceUntilIdle()

        // The expensive read is issued once, for the displayed window only.
        coVerify(exactly = 1) { repo.loadWorkoutsWithMetrics(today.minusDays(6), today) }
        coVerify(exactly = 1) { repo.loadWorkoutsWithMetrics(any(), any()) }
        // ...while the previous and baseline windows take the cheap read.
        coVerify(exactly = 2) { repo.loadWorkouts(any(), any()) }
    }

    private fun workout(
        id: String,
        source: String,
        isOpenVitalsEntry: Boolean,
        exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        totalDistanceMeters: Double? = null,
        durationMs: Long = 1_800_000,
        averageSpeedMetersPerSecond: Double? = null,
    ): ExerciseData {
        val start = Instant.parse("${LocalDate.now()}T08:00:00Z")
        val end = start.plusMillis(durationMs)
        return ExerciseData(
            id = id,
            title = "Walk",
            exerciseType = exerciseType,
            startTime = start,
            endTime = end,
            durationMs = durationMs,
            source = source,
            totalDistanceMeters = totalDistanceMeters,
            averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
            isOpenVitalsEntry = isOpenVitalsEntry,
        )
    }

    private fun plannedWorkout(id: String, exerciseType: Int): PlannedExerciseData {
        val start = Instant.parse("${LocalDate.now()}T08:00:00Z")
        val end = start.plusSeconds(1_800)
        return PlannedExerciseData(
            id = id,
            title = null,
            exerciseType = exerciseType,
            startTime = start,
            endTime = end,
            hasExplicitTime = true,
            completedExerciseSessionId = null,
            notes = null,
            blockCount = 0,
            source = "com.example",
        )
    }

    @Test fun `an initial date pins the screen to that day's period`() = runTest {
        val yesterday = java.time.LocalDate.now().minusDays(1)
        val vm = ActivitiesViewModel(emptyRepo(), initialDate = yesterday)
        advanceUntilIdle()

        assertEquals(yesterday, vm.uiState.value.selectedDate)

        vm.resumeCurrentPeriod()
        advanceUntilIdle()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }
}
