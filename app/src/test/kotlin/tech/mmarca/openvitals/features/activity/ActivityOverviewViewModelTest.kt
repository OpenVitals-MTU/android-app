package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadMethod
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityOverviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun activityRepo(
        steps: List<DailySteps> = emptyList(),
        nutrition: List<DailyNutrition> = emptyList(),
        workouts: List<ExerciseData> = emptyList(),
    ) = mockk<ActivityRepository>().also { repo ->
        coEvery { repo.loadDailySteps(any(), any()) } returns steps
        coEvery { repo.loadDailyNutrition(any(), any()) } returns nutrition
        coEvery { repo.loadWorkouts(any(), any()) } returns workouts
    }

    private fun heartRepo(
        hrv: List<DailyHrv> = emptyList(),
        heartRateSamples: List<HeartRateSample> = emptyList(),
        restingHeartRate: List<DailyRestingHR> = emptyList(),
    ) = mockk<HeartRepository>().also { repo ->
        coEvery { repo.loadDailyHRV(any(), any()) } returns hrv
        coEvery { repo.loadHeartRateSamples(any<LocalDate>(), any<LocalDate>()) } returns heartRateSamples
        coEvery { repo.loadDailyRestingHR(any(), any()) } returns restingHeartRate
    }

    @Test
    fun `load merges activity nutrition and hrv into daily overview`() = runTest {
        val activityRepo = activityRepo(
            steps = listOf(
                DailySteps(
                    date = today,
                    steps = 9_000L,
                    distanceMeters = 6_800.0,
                    activeCaloriesKcal = 350.0,
                ),
            ),
            nutrition = listOf(
                DailyNutrition(
                    date = today,
                    hydrationLiters = 0.0,
                    caloriesBurnedKcal = 2_250.0,
                ),
            ),
        )
        val heartRepo = heartRepo(
            hrv = listOf(DailyHrv(today, rmssdMs = 42.5)),
        )

        val vm = ActivityOverviewViewModel(activityRepo, heartRepo, mainDispatcherRule.dispatcherProvider)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(30, state.days.size)
        assertEquals(9_000L, state.today.steps)
        assertEquals(6_800.0, state.today.distanceMeters, 0.001)
        assertEquals(350.0, state.today.activeCaloriesKcal!!, 0.001)
        assertEquals(2_250.0, state.today.energyBurnedKcal, 0.001)
        assertEquals(42.5, state.today.hrvRmssdMs!!, 0.001)
        assertEquals(5, state.today.cardioLoad)
        assertEquals(CardioLoadConfidence.LOW, state.today.cardioLoadConfidence)
        assertEquals(CardioLoadMethod.MOVEMENT_FALLBACK, state.today.cardioLoadScore.method)
        assertEquals(5, state.today.cardioLoadScore.movementFallbackScore)
        assertTrue(state.today.hasActivity)
    }

    @Test
    fun `load maps workouts into overview days`() = runTest {
        val start = Instant.parse("${today}T10:00:00Z")
        val workout = workout(start, start.plusSeconds(45 * 60L))
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo(workouts = listOf(workout)),
            heartRepository = heartRepo(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        assertEquals(listOf(workout.id), vm.uiState.value.today.workouts.map { it.id })
        assertTrue(vm.uiState.value.today.hasActivity)
    }

    @Test
    fun `cardio load uses trimp with high confidence when heart rate coverage and calibration are available`() = runTest {
        val start = Instant.parse("${today}T10:00:00Z")
        val heartRateSamples = (0..30).map { minute ->
            HeartRateSample(
                time = start.plusSeconds(minute * 60L),
                beatsPerMinute = if (minute == 15) 190L else 145L,
                source = "watch",
            )
        }
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo(
                workouts = listOf(workout(start, start.plusSeconds(30 * 60L))),
            ),
            heartRepository = heartRepo(
                heartRateSamples = heartRateSamples,
                restingHeartRate = listOf(DailyRestingHR(today, 60L)),
            ),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        assertTrue(vm.uiState.value.today.cardioLoad > 0)
        assertEquals(CardioLoadConfidence.HIGH, vm.uiState.value.today.cardioLoadConfidence)
        assertEquals(CardioLoadMethod.TRIMP_ACTIVITY_WINDOWS, vm.uiState.value.today.cardioLoadScore.method)
        assertEquals(30.0, vm.uiState.value.today.cardioLoadScore.coveredMinutes, 0.001)
        assertEquals(30.0, vm.uiState.value.today.cardioLoadScore.expectedMinutes, 0.001)
        assertEquals(31, vm.uiState.value.today.cardioLoadScore.heartRateSampleCount)
        assertEquals(1, vm.uiState.value.today.cardioLoadScore.activityWindowCount)
        assertEquals(60L, vm.uiState.value.today.cardioLoadScore.restingHeartRateBpm)
        assertEquals(190L, vm.uiState.value.today.cardioLoadScore.maxHeartRateBpm)
    }

    @Test
    fun `cardio load uses medium confidence when max heart rate is estimated`() = runTest {
        val start = Instant.parse("${today}T10:00:00Z")
        val heartRateSamples = (0..30).map { minute ->
            HeartRateSample(
                time = start.plusSeconds(minute * 60L),
                beatsPerMinute = 120L,
                source = "watch",
            )
        }
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo(
                workouts = listOf(workout(start, start.plusSeconds(30 * 60L))),
            ),
            heartRepository = heartRepo(
                heartRateSamples = heartRateSamples,
                restingHeartRate = listOf(DailyRestingHR(today, 60L)),
            ),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        assertTrue(vm.uiState.value.today.cardioLoad > 0)
        assertEquals(CardioLoadConfidence.MEDIUM, vm.uiState.value.today.cardioLoadConfidence)
    }

    @Test
    fun `cardio load has no data when heart rate and movement are insufficient`() = runTest {
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo(),
            heartRepository = heartRepo(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        assertEquals(0, vm.uiState.value.today.cardioLoad)
        assertEquals(CardioLoadConfidence.NO_DATA, vm.uiState.value.today.cardioLoadConfidence)
    }

    @Test
    fun `cardio load uses low confidence fallback for meaningful movement without heart rate`() = runTest {
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo(
                steps = listOf(
                    DailySteps(
                        date = today,
                        steps = 1_224L,
                        distanceMeters = 482.0,
                    )
                ),
            ),
            heartRepository = heartRepo(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        assertEquals(1, vm.uiState.value.today.cardioLoad)
        assertEquals(CardioLoadConfidence.LOW, vm.uiState.value.today.cardioLoadConfidence)
        assertEquals(CardioLoadMethod.MOVEMENT_FALLBACK, vm.uiState.value.today.cardioLoadScore.method)
    }

    @Test
    fun `metric days use the selected Monday to Sunday week`() {
        val selectedMonday = LocalDate.of(2026, 6, 1)
        val loadedDays = (-6..6).map { offset ->
            ActivityOverviewDay(date = selectedMonday.plusDays(offset.toLong()))
        }
        val state = ActivityOverviewUiState(
            selectedDate = selectedMonday,
            days = loadedDays,
        )

        assertEquals(
            (0..6).map { selectedMonday.plusDays(it.toLong()) },
            state.metricDays.map { it.date },
        )
    }

    @Test
    fun `metric days fill missing days in the selected week`() {
        val selectedMonday = LocalDate.of(2026, 6, 1)
        val state = ActivityOverviewUiState(
            selectedDate = selectedMonday,
            days = listOf(ActivityOverviewDay(date = selectedMonday, steps = 1_000L)),
        )

        assertEquals(7, state.metricDays.size)
        assertEquals((0..6).map { selectedMonday.plusDays(it.toLong()) }, state.metricDays.map { it.date })
        assertEquals(1_000L, state.metricDays.first().steps)
        assertTrue(state.metricDays.drop(1).all { it.steps == 0L })
    }

    @Test
    fun `metric days can use rolling last seven days`() {
        val selectedMonday = LocalDate.of(2026, 6, 1)
        val loadedDays = (-6..6).map { offset ->
            ActivityOverviewDay(date = selectedMonday.plusDays(offset.toLong()))
        }
        val state = ActivityOverviewUiState(
            selectedDate = selectedMonday,
            days = loadedDays,
            activityWeekMode = ActivityWeekMode.LAST_7_DAYS,
        )

        assertEquals(
            (-6..0).map { selectedMonday.plusDays(it.toLong()) },
            state.metricDays.map { it.date },
        )
    }

    @Test
    fun `load requests the last thirty days ending today`() = runTest {
        val activityRepo = activityRepo()
        val heartRepo = heartRepo()

        ActivityOverviewViewModel(activityRepo, heartRepo, mainDispatcherRule.dispatcherProvider)

        coVerify {
            activityRepo.loadDailySteps(today.minusDays(29), today)
            activityRepo.loadDailyNutrition(today.minusDays(29), today)
            activityRepo.loadWorkouts(today.minusDays(29), today)
            heartRepo.loadHeartRateSamples(today.minusDays(29), today)
            heartRepo.loadDailyRestingHR(today.minusDays(29), today)
            heartRepo.loadDailyHRV(today.minusDays(29), today)
        }
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val activityRepo = mockk<ActivityRepository>()
        coEvery { activityRepo.loadDailySteps(any(), any()) } throws RuntimeException("timeout")
        val vm = ActivityOverviewViewModel(
            activityRepository = activityRepo,
            heartRepository = heartRepo(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ScreenError.Message("timeout"), state.error)
    }

    private fun workout(start: Instant, end: Instant): ExerciseData =
        ExerciseData(
            id = "activity-${start.toEpochMilli()}",
            title = null,
            exerciseType = 0,
            startTime = start,
            endTime = end,
            durationMs = end.toEpochMilli() - start.toEpochMilli(),
            source = "OpenVitals",
            startZoneOffset = ZoneOffset.UTC,
            endZoneOffset = ZoneOffset.UTC,
        )
}
