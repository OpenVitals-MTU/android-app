package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `sets, set rest and weight are checked on active rows and point at the step`() {
        val block = validForm().blocks.single()
        val step = block.steps.first()
        fun errorsFor(edited: WorkoutPlanStepInput) =
            validateWorkoutPlan(validForm().copy(blocks = listOf(block.copy(steps = listOf(edited, WorkoutPlanStepInput.rest())))))
        fun only(kind: WorkoutPlanValidationErrorKind) =
            setOf(WorkoutPlanValidationError(kind, blockId = block.id, stepId = step.id))

        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_SETS_INVALID), errorsFor(step.copy(setsText = "0")))
        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_SETS_INVALID), errorsFor(step.copy(setsText = "100")))
        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_SET_REST_INVALID), errorsFor(step.copy(setRestText = "-1")))
        assertEquals(
            only(WorkoutPlanValidationErrorKind.STEP_SET_REST_INVALID),
            errorsFor(step.copy(setRestText = "1441", setRestUnit = WorkoutPlanDurationUnit.MINUTES)),
        )
        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_WEIGHT_INVALID), errorsFor(step.copy(weightKgText = "abc")))
        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_WEIGHT_INVALID), errorsFor(step.copy(weightKgText = "0")))
        assertEquals(only(WorkoutPlanValidationErrorKind.STEP_WEIGHT_INVALID), errorsFor(step.copy(weightKgText = "1001")))
        val clean = step.copy(setsText = "3", setRestText = "2", setRestUnit = WorkoutPlanDurationUnit.MINUTES, weightKgText = "12,5")
        assertTrue(errorsFor(clean).isEmpty())
    }

    @Test
    fun `minute goals count sixty times and a reps goal ignores the unit`() {
        val step = validForm().blocks.single().steps.first()

        assertEquals(12L, step.copy(goalValueText = "12", durationUnit = WorkoutPlanDurationUnit.MINUTES).goalValueOrNull())
        val timed = step.copy(goalType = WorkoutPlanGoalType.DURATION, goalValueText = "2", durationUnit = WorkoutPlanDurationUnit.MINUTES)
        assertEquals(120L, timed.goalValueOrNull())
        assertNull(timed.copy(goalValueText = "1441").goalValueOrNull())
        assertEquals(60L, WorkoutPlanStepInput.rest().goalValueOrNull())
    }
}
