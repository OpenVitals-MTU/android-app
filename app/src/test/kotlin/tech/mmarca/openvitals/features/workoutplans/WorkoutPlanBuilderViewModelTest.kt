package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
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
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.util.MainDispatcherRule

class WorkoutPlanBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-26T16:47:00Z"), zone)
    private val plannedPermissions = setOf("planned-read", "planned-write")

    private fun repo(
        available: Boolean = true,
        plan: PlannedExerciseData? = null,
        loadError: Throwable? = null,
    ): ActivityRepository = mockk {
        every { plannedWorkoutWritePermissions() } returns if (available) plannedPermissions else emptySet()
        if (loadError != null) {
            coEvery { loadPlannedWorkout(any()) } throws loadError
        } else {
            coEvery { loadPlannedWorkout(any()) } returns plan
        }
        coEvery { writePlannedWorkout(any()) } returns "saved-id"
    }

    private fun viewModel(repository: ActivityRepository, planId: String? = null) =
        WorkoutPlanBuilderViewModel(
            repository = repository,
            appPackageName = "tech.mmarca.openvitals",
            clock = clock,
            planId = planId,
        )

    @Test
    fun `new plan starts today at a rounded time with one empty block and is not dirty`() = runTest {
        val vm = viewModel(repo())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAvailable)
        assertEquals("2026-08-26", state.form.startDateText)
        assertEquals("18:45", state.form.startTimeText)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS, state.form.sessionExerciseType)
        assertEquals(1, state.form.blocks.size)
        assertFalse(state.isDirty)
        assertEquals(plannedPermissions, state.writePermissions)
    }

    @Test
    fun `editing an existing plan loads its blocks and ownership`() = runTest {
        val vm = viewModel(repo(plan = plan(source = "com.other.app")), planId = "planned-id")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Pull-up ladder", state.form.titleText)
        assertEquals(1, state.form.blocks.size)
        assertEquals(WorkoutPlanStepKind.ACTIVE, state.form.blocks.single().steps.single().kind)
        assertFalse(state.isOwnedByApp)
        assertFalse(state.canEdit)
    }

    @Test
    fun `a missing plan shows not found`() = runTest {
        val vm = viewModel(repo(plan = null), planId = "gone")
        advanceUntilIdle()

        assertEquals(ScreenError.NotFound, vm.uiState.value.error)
    }

    @Test
    fun `a permission failure while loading maps to PermissionDenied`() = runTest {
        val vm = viewModel(repo(loadError = SecurityException("no")), planId = "planned-id")
        advanceUntilIdle()

        assertEquals(ScreenError.PermissionDenied, vm.uiState.value.error)
    }

    @Test
    fun `save validates first and writes the plan when the form is complete`() = runTest {
        val repository = repo()
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()
        val errors = vm.uiState.value.validationErrors.map { it.kind }.toSet()
        assertTrue(WorkoutPlanValidationErrorKind.TITLE_REQUIRED in errors)
        assertTrue(WorkoutPlanValidationErrorKind.BLOCK_EMPTY in errors)
        coVerify(exactly = 0) { repository.writePlannedWorkout(any()) }

        val blockId = vm.uiState.value.form.blocks.single().id
        vm.updateTitle("Strength")
        vm.addStep(blockId, WorkoutPlanStepCatalog.first())
        vm.addRestStep(blockId)
        vm.updateBlockRounds(blockId, "3")
        vm.addBlock()
        val plankBlockId = vm.uiState.value.form.blocks.last().id
        vm.addStep(plankBlockId, WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK))
        vm.updateStepGoalValue(plankBlockId, vm.uiState.value.form.blocks.last().steps.single().id, "45")
        vm.updateBlockRounds(plankBlockId, "2")
        assertTrue(vm.uiState.value.validationErrors.isEmpty())
        assertTrue(vm.uiState.value.isDirty)

        val request = slot<PlannedExerciseWriteRequest>()
        coEvery { repository.writePlannedWorkout(capture(request)) } returns "saved-id"
        vm.save()
        advanceUntilIdle()

        assertEquals("saved-id", vm.uiState.value.savedPlanId)
        assertTrue(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.isDirty)
        assertNull(request.captured.id)
        assertEquals(2, request.captured.blocks.size)
        assertEquals(3, request.captured.blocks[0].repetitions)
        assertEquals(PlannedExerciseCompletion.DurationSeconds(45), request.captured.blocks[1].steps.single().completion)
    }

    @Test
    fun `save failure with a permission error is surfaced and keeps the form`() = runTest {
        val repository = repo(plan = plan())
        coEvery { repository.writePlannedWorkout(any()) } throws SecurityException("no write")
        val vm = viewModel(repository, planId = "planned-id")
        advanceUntilIdle()

        vm.updateTitle("Renamed")
        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ScreenError.PermissionDenied, state.error)
        assertFalse(state.isSaving)
        assertTrue(state.isDirty)
        assertNull(state.savedPlanId)
    }

    @Test
    fun `steps and blocks can be reordered and removed`() = runTest {
        val vm = viewModel(repo())
        advanceUntilIdle()
        val blockId = vm.uiState.value.form.blocks.single().id
        vm.addStep(blockId, WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT))
        vm.addRestStep(blockId)

        vm.moveStep(blockId, 1, 0)
        assertEquals(WorkoutPlanStepKind.REST, vm.uiState.value.form.blocks.single().steps.first().kind)

        vm.addBlock()
        val secondId = vm.uiState.value.form.blocks[1].id
        vm.moveBlock(secondId, -1)
        assertEquals(secondId, vm.uiState.value.form.blocks.first().id)

        vm.removeBlock(secondId)
        assertEquals(listOf(blockId), vm.uiState.value.form.blocks.map { it.id })
    }

    private fun plan(source: String = "tech.mmarca.openvitals"): PlannedExerciseData = PlannedExerciseData(
        id = "planned-id",
        title = "Pull-up ladder",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-05-26T08:30:00Z"),
        endTime = Instant.parse("2026-05-26T08:35:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = null,
        blockCount = 1,
        source = source,
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = null,
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = null,
                        completion = PlannedExerciseCompletion.Repetitions(8),
                    ),
                ),
            ),
        ),
    )
}
