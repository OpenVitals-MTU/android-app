package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.data.repository.ActivityRepository
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

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(workout, vm.uiState.value.workout)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.loadWorkout("activity-1") }
    }

    @Test fun `missing activity sets not found error`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("missing") } returns null

        val vm = ActivityDetailViewModel(repo, "missing")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals("Activity not found.", vm.uiState.value.error)
    }

    @Test fun `blank activity id fails without calling repository`() = runTest {
        val repo = mockk<ActivityRepository>(relaxed = true)

        val vm = ActivityDetailViewModel(repo, "")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Missing activity id.", vm.uiState.value.error)
        coVerify(exactly = 0) { repo.loadWorkout(any()) }
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } throws RuntimeException("timeout")

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `deleteActivity deletes OpenVitals activity and reports completion`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = true)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.deleteActivityEntry("activity-1") } returns Unit
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
        val vm = ActivityDetailViewModel(repo, "activity-1")

        vm.deleteActivity()

        assertEquals(workout, vm.uiState.value.workout)
        coVerify(exactly = 0) { repo.deleteActivityEntry(any()) }
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
