package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityMetricRelevanceTest {

    @Test fun `universal metrics are relevant for every exercise type`() {
        val unknownType = 9_999
        listOf(
            ActivityDetailMetric.DURATION,
            ActivityDetailMetric.MOVING_TIME,
            ActivityDetailMetric.AVERAGE_HEART_RATE,
            ActivityDetailMetric.CALORIES_BURNED,
            ActivityDetailMetric.ACTIVE_CALORIES,
        ).forEach { metric ->
            assertTrue("$metric", isMetricRelevant(metric, unknownType))
        }
    }

    @Test fun `an unknown exercise type reports the universal absences and nothing invented`() {
        // A Health Connect constant this table has never seen — a new type, or
        // an activity imported from another app. It needs no special case: it
        // reports what every session has, shows anything it did record, and
        // invents nothing. The card can never be empty, because a session always
        // has a duration.
        val unknown = 9_999
        listOf(
            ActivityDetailMetric.DURATION,
            ActivityDetailMetric.AVERAGE_HEART_RATE,
            ActivityDetailMetric.CALORIES_BURNED,
            ActivityDetailMetric.ACTIVE_CALORIES,
        ).forEach { metric ->
            assertTrue("$metric", isMetricRelevant(metric, unknown))
        }
        listOf(
            ActivityDetailMetric.DISTANCE,
            ActivityDetailMetric.AVERAGE_PACE,
            ActivityDetailMetric.STEPS,
            ActivityDetailMetric.CYCLING_CADENCE,
            ActivityDetailMetric.WHEELCHAIR_PUSHES,
        ).forEach { metric ->
            assertFalse("$metric", isMetricRelevant(metric, unknown))
        }
    }

    @Test fun `bike ride does not advertise steps floors or wheelchair pushes`() {
        val biking = ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        assertFalse(isMetricRelevant(ActivityDetailMetric.STEPS, biking))
        assertFalse(isMetricRelevant(ActivityDetailMetric.FLOORS_CLIMBED, biking))
        assertFalse(isMetricRelevant(ActivityDetailMetric.WHEELCHAIR_PUSHES, biking))
        assertFalse(isMetricRelevant(ActivityDetailMetric.AVERAGE_PACE, biking))
        assertTrue(isMetricRelevant(ActivityDetailMetric.CYCLING_CADENCE, biking))
        assertTrue(isMetricRelevant(ActivityDetailMetric.DISTANCE, biking))
        assertTrue(isMetricRelevant(ActivityDetailMetric.AVERAGE_SPEED, biking))
        assertTrue(isMetricRelevant(ActivityDetailMetric.ELEVATION_GAINED, biking))
    }

    @Test fun `run prefers pace and walks the step based sets`() {
        val running = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        listOf(
            ActivityDetailMetric.STEPS,
            ActivityDetailMetric.AVERAGE_PACE,
            ActivityDetailMetric.DISTANCE,
            ActivityDetailMetric.ELEVATION_GAINED,
        ).forEach { metric ->
            assertTrue("$metric", isMetricRelevant(metric, running))
        }
        // A runner has no crank, no push rim and no staircase.
        assertFalse(isMetricRelevant(ActivityDetailMetric.CYCLING_CADENCE, running))
        assertFalse(isMetricRelevant(ActivityDetailMetric.WHEELCHAIR_PUSHES, running))
        assertFalse(isMetricRelevant(ActivityDetailMetric.FLOORS_CLIMBED, running))
    }

    @Test fun `indoor activities do not report missing elevation`() {
        // The ground never rises on a treadmill, so "Elevation gained: Not
        // available" is noise; outdoors it is a real statement about the GPS.
        val treadmill = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
        assertTrue(isMetricRelevant(ActivityDetailMetric.DISTANCE, treadmill))
        listOf(
            treadmill,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ).forEach { indoor ->
            assertFalse(
                "type $indoor",
                isMetricRelevant(ActivityDetailMetric.ELEVATION_GAINED, indoor),
            )
        }
        assertTrue(
            isMetricRelevant(
                ActivityDetailMetric.ELEVATION_GAINED,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ),
        )
    }

    @Test fun `hardware bound metrics are never relevant when absent`() {
        // A power meter, a footpod, a bike computer's own speed average:
        // hardware most people do not own. "Average power: Not available" on
        // every ride is the same noise the fix exists to remove, so these are
        // value-only for EVERY type — the screen still shows them the moment
        // they carry a figure.
        listOf(
            ActivityDetailMetric.AVERAGE_POWER,
            ActivityDetailMetric.STEPS_CADENCE,
            ActivityDetailMetric.RECORDED_SPEED,
        ).forEach { metric ->
            listOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            ).forEach { type ->
                assertFalse("$metric on type $type", isMetricRelevant(metric, type))
            }
        }
    }

    @Test fun `single purpose metrics belong to their single exercise`() {
        assertTrue(
            isMetricRelevant(
                ActivityDetailMetric.WHEELCHAIR_PUSHES,
                ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
            ),
        )
        assertTrue(
            isMetricRelevant(
                ActivityDetailMetric.FLOORS_CLIMBED,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
            ),
        )
        assertTrue(
            isMetricRelevant(
                ActivityDetailMetric.FLOORS_CLIMBED,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
            ),
        )
        // ...and to nothing else.
        listOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
        ).forEach { other ->
            assertFalse(
                "type $other",
                isMetricRelevant(ActivityDetailMetric.FLOORS_CLIMBED, other),
            )
        }
        assertFalse(
            isMetricRelevant(
                ActivityDetailMetric.STEPS,
                ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
            ),
        )
    }

    @Test fun `a recorded value is shown even when the type says it is irrelevant`() {
        // The relevance table decides which ABSENCES are informative. It never
        // suppresses a figure: a bike that somehow reported steps still shows
        // them, and a strength session that recorded a distance still shows it.
        val biking = ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        assertFalse(isMetricRelevant(ActivityDetailMetric.STEPS, biking))
        assertTrue(showsMetricRow(hasValue = true, metric = ActivityDetailMetric.STEPS, exerciseType = biking))
        assertFalse(showsMetricRow(hasValue = false, metric = ActivityDetailMetric.STEPS, exerciseType = biking))

        val strength = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        assertTrue(
            showsMetricRow(
                hasValue = true,
                metric = ActivityDetailMetric.DISTANCE,
                exerciseType = strength,
            ),
        )
        // ...and an informative absence earns its row with no value at all.
        assertTrue(
            showsMetricRow(
                hasValue = false,
                metric = ActivityDetailMetric.DISTANCE,
                exerciseType = biking,
            ),
        )
    }

    @Test fun `a strength session reports none of the distance metrics`() {
        val type = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        listOf(
            ActivityDetailMetric.DISTANCE,
            ActivityDetailMetric.AVERAGE_PACE,
            ActivityDetailMetric.STEPS,
            ActivityDetailMetric.CYCLING_CADENCE,
            ActivityDetailMetric.ELEVATION_GAINED,
        ).forEach { metric ->
            assertFalse("$metric", isMetricRelevant(metric, type))
        }
        // It still burns energy and still has a heart rate.
        assertTrue(isMetricRelevant(ActivityDetailMetric.AVERAGE_HEART_RATE, type))
        assertTrue(isMetricRelevant(ActivityDetailMetric.CALORIES_BURNED, type))
        assertTrue(isMetricRelevant(ActivityDetailMetric.DURATION, type))
    }
}
