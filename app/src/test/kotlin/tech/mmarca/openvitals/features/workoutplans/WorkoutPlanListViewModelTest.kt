package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSessionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.util.MainDispatcherRule

class WorkoutPlanListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-26T10:00:00Z"), zone)

    private val today = plan("today", Instant.parse("2026-08-26T17:00:00Z"))
    private val tomorrow = plan("tomorrow", Instant.parse("2026-08-27T07:00:00Z"))
    private val lastWeek = plan("last-week", Instant.parse("2026-08-19T07:00:00Z"))
    private val foreignDone = plan(
        "foreign",
        Instant.parse("2026-08-26T06:00:00Z"),
        source = "com.other.app",
        completedId = "session-1",
    )

    private fun repo(
        available: Boolean = true,
        plans: List<PlannedExerciseData> = listOf(today, tomorrow, lastWeek, foreignDone),
        loadError: Throwable? = null,
    ): ActivityRepository = mockk {
        every { plannedWorkoutWritePermissions() } returns if (available) setOf("r", "w") else emptySet()
        if (loadError != null) {
            coEvery { loadPlannedWorkouts(any(), any()) } throws loadError
        } else {
            coEvery { loadPlannedWorkouts(any(), any()) } returns plans
        }
        coEvery { deletePlannedWorkout(any()) } returns Unit
        coEvery { writePlannedWorkout(any()) } returns "copy-id"
    }

    private fun viewModel(repository: ActivityRepository) =
        WorkoutPlanListViewModel(repository = repository, appPackageName = "tech.mmarca.openvitals", clock = clock)

    @Test
    fun `plans are grouped by day and completed ones land in past`() = runTest {
        val vm = viewModel(repo())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("today"), state.items(WorkoutPlanGroup.TODAY).map { it.plan.id })
        assertEquals(listOf("tomorrow"), state.items(WorkoutPlanGroup.UPCOMING).map { it.plan.id })
        assertEquals(setOf("last-week", "foreign"), state.items(WorkoutPlanGroup.PAST).map { it.plan.id }.toSet())
        val foreign = state.items.first { it.plan.id == "foreign" }
        assertFalse(foreign.isOwnedByApp)
        assertFalse(foreign.canEdit)
        assertFalse(foreign.canStart)
        assertTrue(state.items.first { it.plan.id == "today" }.canEdit)
    }

    @Test
    fun `unavailable planned exercise skips loading`() = runTest {
        val repository = repo(available = false)
        val vm = viewModel(repository)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isAvailable)
        coVerify(exactly = 0) { repository.loadPlannedWorkouts(any(), any()) }
    }

    @Test
    fun `permission failures surface as PermissionDenied`() = runTest {
        val vm = viewModel(repo(loadError = SecurityException("no")))
        advanceUntilIdle()

        assertEquals(ScreenError.PermissionDenied, vm.uiState.value.error)
    }

    @Test
    fun `delete asks for confirmation then removes the plan`() = runTest {
        val repository = repo()
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.requestDelete("today")
        assertEquals("today", vm.uiState.value.pendingDeleteId)
        vm.cancelDelete()
        assertNull(vm.uiState.value.pendingDeleteId)
        coVerify(exactly = 0) { repository.deletePlannedWorkout(any()) }

        vm.requestDelete("today")
        vm.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deletePlannedWorkout("today") }
        assertTrue(vm.uiState.value.items.none { it.plan.id == "today" })
        assertEquals(WorkoutPlanListMessage.DELETED, vm.uiState.value.message)
    }

    @Test
    fun `copy to today writes a fresh plan for today and reloads`() = runTest {
        val repository = repo()
        val request = slot<PlannedExerciseWriteRequest>()
        coEvery { repository.writePlannedWorkout(capture(request)) } returns "copy-id"
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.copyToToday("last-week")
        advanceUntilIdle()

        assertNull(request.captured.id)
        assertEquals("2026-08-26", request.captured.startTime.atZone(zone).toLocalDate().toString())
        // 09:00 local has already gone by at the 12:00 clock, so the copy is scheduled for now.
        assertEquals(java.time.LocalTime.now(clock), request.captured.startTime.atZone(zone).toLocalTime())
        assertEquals(lastWeek.blocks, request.captured.blocks)
        assertEquals(WorkoutPlanListMessage.COPIED_TO_TODAY, vm.uiState.value.message)
        coVerify(exactly = 2) { repository.loadPlannedWorkouts(any(), any()) }
    }

    private fun plan(
        id: String,
        start: Instant,
        source: String = "tech.mmarca.openvitals",
        completedId: String? = null,
    ): PlannedExerciseData = PlannedExerciseData(
        id = id,
        title = id,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = start,
        endTime = start.plusSeconds(30 * 60),
        hasExplicitTime = true,
        completedExerciseSessionId = completedId,
        notes = null,
        blockCount = 0,
        source = source,
    )

    @Test fun `copy to today keeps a clock time that is still ahead`() = runTest {
        val repository = repo()
        val request = slot<PlannedExerciseWriteRequest>()
        coEvery { repository.writePlannedWorkout(capture(request)) } returns "copy-id"
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.copyToToday("today")
        advanceUntilIdle()

        assertEquals(today.startTime.atZone(zone).toLocalTime(), request.captured.startTime.atZone(zone).toLocalTime())
    }

    @Test fun `repeat copies the plan for today and asks the screen to start it`() = runTest {
        val repository = repo()
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.repeatPlan("foreign")
        advanceUntilIdle()

        assertEquals("copy-id", vm.uiState.value.pendingStartPlanId)
        coVerify(exactly = 1) { repository.writePlannedWorkout(match { it.id == null && it.blocks == foreignDone.blocks }) }
        vm.onStartPlanHandled()
        assertNull(vm.uiState.value.pendingStartPlanId)
    }

    @Test fun `a plan is grouped by the day it was written in, not the phone's current zone`() = runTest {
        // 23:30 in Los Angeles on the 26th is 08:30 in Madrid on the 27th.
        val abroad = plan("abroad", Instant.parse("2026-08-27T06:30:00Z")).copy(
            startZoneOffset = java.time.ZoneOffset.ofHours(-7),
        )
        val vm = viewModel(repo(plans = listOf(abroad)))
        advanceUntilIdle()

        assertEquals(listOf("abroad"), vm.uiState.value.items(WorkoutPlanGroup.TODAY).map { it.plan.id })
    }
}
