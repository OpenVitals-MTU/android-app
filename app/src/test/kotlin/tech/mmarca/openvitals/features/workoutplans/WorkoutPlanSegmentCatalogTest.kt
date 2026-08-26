package tech.mmarca.openvitals.features.workoutplans

import androidx.health.connect.client.records.ExerciseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanSegmentCatalogTest {

    @Test
    fun `catalog excludes rest pause and unknown`() {
        val types = WorkoutPlanStepCatalog.map { it.segmentType }.toSet()
        assertFalse(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST in types)
        assertFalse(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE in types)
        assertFalse(ExerciseSegment.EXERCISE_SEGMENT_TYPE_UNKNOWN in types)
        assertTrue(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK in types)
    }

    @Test
    fun `catalog has no duplicate entries and every entry has a label`() {
        val keys = WorkoutPlanStepCatalog.map { it.segmentType to it.description }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(WorkoutPlanStepCatalog.all { it.labelRes != tech.mmarca.openvitals.R.string.hc_segment_unknown })
    }

    @Test
    fun `push-ups preset comes first and planks default to duration`() {
        assertEquals("Push-ups", WorkoutPlanStepCatalog.first().description)
        assertEquals(WorkoutPlanGoalType.REPETITIONS, WorkoutPlanStepCatalog.first().defaultGoal)
        assertEquals(WorkoutPlanGoalType.DURATION, defaultGoalFor(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK))
        assertEquals(WorkoutPlanGoalType.REPETITIONS, defaultGoalFor(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT))
    }
}
