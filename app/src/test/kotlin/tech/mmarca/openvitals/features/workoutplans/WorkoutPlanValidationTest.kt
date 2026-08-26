package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanValidationTest {

    private val squat = WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT)

    private fun validForm(): WorkoutPlanFormInput = WorkoutPlanFormInput(
        titleText = "Legs",
        startDateText = "2026-08-26",
        startTimeText = "18:00",
        durationMinutesText = "30",
        blocks = listOf(
            WorkoutPlanBlockInput(
                roundsText = "3",
                steps = listOf(WorkoutPlanStepInput.active(squat), WorkoutPlanStepInput.rest()),
            ),
        ),
    )

    private fun kinds(form: WorkoutPlanFormInput): Set<WorkoutPlanValidationErrorKind> =
        validateWorkoutPlan(form).map { it.kind }.toSet()

    @Test
    fun `a complete form validates cleanly`() {
        assertTrue(validateWorkoutPlan(validForm()).isEmpty())
    }

    @Test
    fun `title date time and duration are each checked`() {
        assertEquals(setOf(WorkoutPlanValidationErrorKind.TITLE_REQUIRED), kinds(validForm().copy(titleText = "  ")))
        assertEquals(setOf(WorkoutPlanValidationErrorKind.START_DATE_INVALID), kinds(validForm().copy(startDateText = "26/08")))
        assertEquals(setOf(WorkoutPlanValidationErrorKind.START_TIME_INVALID), kinds(validForm().copy(startTimeText = "6pm")))
        assertEquals(setOf(WorkoutPlanValidationErrorKind.DURATION_INVALID), kinds(validForm().copy(durationMinutesText = "0")))
        assertEquals(
            setOf(WorkoutPlanValidationErrorKind.DURATION_INVALID),
            kinds(validForm().copy(durationMinutesText = (MaxWorkoutPlanDurationMinutes + 1).toString())),
        )
    }

    @Test
    fun `blocks need rounds and steps, and the plan needs an exercise`() {
        assertEquals(setOf(WorkoutPlanValidationErrorKind.NO_BLOCKS), kinds(validForm().copy(blocks = emptyList())))

        val block = validForm().blocks.single()
        val badRounds = validateWorkoutPlan(validForm().copy(blocks = listOf(block.copy(roundsText = "0"))))
        assertEquals(
            setOf(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.BLOCK_ROUNDS_INVALID, blockId = block.id)),
            badRounds,
        )

        val empty = validateWorkoutPlan(validForm().copy(blocks = listOf(block.copy(steps = emptyList()))))
        assertEquals(
            setOf(WorkoutPlanValidationError(WorkoutPlanValidationErrorKind.BLOCK_EMPTY, blockId = block.id)),
            empty,
        )

        val restOnly = validForm().copy(blocks = listOf(block.copy(steps = listOf(WorkoutPlanStepInput.rest()))))
        assertEquals(setOf(WorkoutPlanValidationErrorKind.NO_ACTIVE_STEP), kinds(restOnly))
    }

    @Test
    fun `step goals must be at least one and point at the step`() {
        val block = validForm().blocks.single()
        val step = block.steps.first().copy(goalValueText = "0")
        val errors = validateWorkoutPlan(validForm().copy(blocks = listOf(block.copy(steps = listOf(step)))))

        assertEquals(
            setOf(
                WorkoutPlanValidationError(
                    WorkoutPlanValidationErrorKind.STEP_GOAL_INVALID,
                    blockId = block.id,
                    stepId = step.id,
                ),
            ),
            errors,
        )
    }
}
