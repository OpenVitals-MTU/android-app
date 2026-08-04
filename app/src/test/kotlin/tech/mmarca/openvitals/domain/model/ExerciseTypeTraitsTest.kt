package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseTypeTraitsTest {

    @Test
    fun `activities that travel are distance based`() {
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR))
        // Machines that simulate covering ground still have a distance.
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE))
        assertTrue(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL))
    }

    @Test
    fun `a strength session does not travel, whatever GPS drift says`() {
        assertFalse(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING))
        assertFalse(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_YOGA))
        assertFalse(isDistanceBasedExercise(ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING))
    }

    @Test
    fun `step and cycling exercises are subsets of distance based`() {
        assertTrue(distanceBasedExercises.containsAll(stepBasedExercises))
        assertTrue(distanceBasedExercises.containsAll(cyclingExercises))
    }

    @Test
    fun `indoor exercises are the machine-bound distance activities`() {
        assertTrue(indoorExercises.all { it in distanceBasedExercises })
        assertTrue(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL in indoorExercises)
        assertFalse(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING in indoorExercises)
    }

    @Test
    fun `pace-preferring exercises are the on-foot ones`() {
        assertTrue(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING in prefersPaceExercises)
        assertTrue(ExerciseSessionRecord.EXERCISE_TYPE_HIKING in prefersPaceExercises)
        assertFalse(ExerciseSessionRecord.EXERCISE_TYPE_BIKING in prefersPaceExercises)
    }
}
