package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ActivityDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial load fetches selected activity`() = runTest {
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(workout, vm.uiState.value.workout)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.loadWorkout("activity-1") }
    }

    @Test fun `initial load backfills missing averages from samples`() = runTest {
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        val heartRepo = mockk<HeartRepository>()
        val heartSamples = listOf(
            HeartRateSample(workout.startTime, 100L, "test"),
            HeartRateSample(workout.startTime.plusSeconds(60), 110L, "test"),
        )
        val speedSamples = listOf(
            SpeedSample(workout.startTime, 2.0, "test"),
            SpeedSample(workout.startTime.plusSeconds(60), 4.0, "test"),
        )
        val cadenceSamples = listOf(
            ActivityCadenceSample(workout.startTime, 160.0, ActivityCadenceKind.STEPS, "test"),
            ActivityCadenceSample(workout.startTime.plusSeconds(60), 180.0, ActivityCadenceKind.STEPS, "test"),
            ActivityCadenceSample(workout.startTime, 80.0, ActivityCadenceKind.CYCLING, "test"),
            ActivityCadenceSample(workout.startTime.plusSeconds(60), 100.0, ActivityCadenceKind.CYCLING, "test"),
        )
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { heartRepo.loadHeartRateSamples(workout.startTime, workout.endTime) } returns heartSamples
        coEvery { repo.loadSpeedSamples(workout.startTime, workout.endTime) } returns speedSamples
        coEvery { repo.loadActivityCadenceSamples(workout.startTime, workout.endTime) } returns cadenceSamples

        val vm = ActivityDetailViewModel(repo, "activity-1", heartRepository = heartRepo)
        val backfilled = requireNotNull(vm.uiState.value.workout)

        assertEquals(105L, backfilled.averageHeartRateBpm)
        assertEquals(3.0, backfilled.averageSpeedMetersPerSecond ?: 0.0, 0.001)
        assertEquals(170.0, backfilled.averageStepsCadenceRate ?: 0.0, 0.001)
        assertEquals(90.0, backfilled.averageCyclingCadenceRpm ?: 0.0, 0.001)
        assertEquals(heartSamples, vm.uiState.value.heartRateSamples)
        assertEquals(speedSamples, vm.uiState.value.speedSamples)
        assertEquals(cadenceSamples, vm.uiState.value.cadenceSamples)
    }

    @Test fun `missing activity sets not found error`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("missing") } returns null

        val vm = ActivityDetailViewModel(repo, "missing")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals(ScreenError.NotFound, vm.uiState.value.error)
    }

    @Test fun `blank activity id fails without calling repository`() = runTest {
        val repo = mockk<ActivityRepository>(relaxed = true)

        val vm = ActivityDetailViewModel(repo, "")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.MissingArgument, vm.uiState.value.error)
        coVerify(exactly = 0) { repo.loadWorkout(any()) }
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } throws RuntimeException("timeout")
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    @Test fun `deleteActivity deletes OpenVitals activity and reports completion`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = true)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.deleteActivityEntry("activity-1") } returns Unit
        stubMetricSamples(repo)
        val vm = ActivityDetailViewModel(repo, "activity-1")
        var deleted = false

        vm.deleteActivity { deleted = true }

        assertFalse(vm.uiState.value.isDeleting)
        assertNull(vm.uiState.value.workout)
        assertEquals(true, deleted)
        coVerify(exactly = 1) { repo.deleteActivityEntry("activity-1") }
    }

    @Test fun `deleteActivity ignores workout not created by OpenVitals`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = false)
        val repo = mockk<ActivityRepository>(relaxed = true)
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        val vm = ActivityDetailViewModel(repo, "activity-1")

        vm.deleteActivity()

        assertEquals(workout, vm.uiState.value.workout)
        coVerify(exactly = 0) { repo.deleteActivityEntry(any()) }
    }

    private fun stubMetricSamples(repo: ActivityRepository) {
        coEvery { repo.loadSpeedSamples(any(), any()) } returns emptyList()
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } returns emptyList()
    }

    private fun workout(id: String, isOpenVitalsEntry: Boolean = false) = ExerciseData(
        id = id,
        title = "Morning run",
        exerciseType = 56,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusSeconds(3_600),
        durationMs = 3_600_000,
        source = "test",
        isOpenVitalsEntry = isOpenVitalsEntry,
    )
}
