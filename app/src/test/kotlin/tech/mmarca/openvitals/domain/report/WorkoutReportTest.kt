package tech.mmarca.openvitals.domain.report

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseData

class WorkoutReportTest {

    private fun workout(
        startOffsetHours: Long,
        type: Int,
        durationMinutes: Long,
        distanceMeters: Double? = null,
        title: String? = null,
    ): ExerciseData {
        val start = Instant.parse("2026-06-01T08:00:00Z").plusSeconds(startOffsetHours * 3_600)
        return ExerciseData(
            id = "w$startOffsetHours",
            title = title,
            exerciseType = type,
            startTime = start,
            endTime = start.plusSeconds(durationMinutes * 60),
            durationMs = durationMinutes * 60_000,
            source = "test",
            totalDistanceMeters = distanceMeters,
        )
    }

    @Test fun `no workouts means no detail`() {
        assertNull(workoutsDetail(emptyList()))
    }

    @Test fun `sessions come back sorted by start time`() {
        val detail = workoutsDetail(
            listOf(
                workout(48, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, 30),
                workout(0, ExerciseSessionRecord.EXERCISE_TYPE_WALKING, 60),
            ),
        )!!

        assertTrue(detail.sessions[0].start < detail.sessions[1].start)
    }

    @Test fun `type totals sum their own sessions, biggest time first`() {
        val detail = workoutsDetail(
            listOf(
                workout(0, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, 30, distanceMeters = 5_000.0),
                workout(24, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, 40, distanceMeters = 7_000.0),
                workout(48, ExerciseSessionRecord.EXERCISE_TYPE_YOGA, 90),
            ),
        )!!

        assertEquals(2, detail.byType.size)
        val yoga = detail.byType[0]
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_YOGA, yoga.exerciseType)
        assertEquals(90 * 60_000L, yoga.totalDurationMs)
        assertNull(yoga.totalDistanceMeters)
        val running = detail.byType[1]
        assertEquals(2, running.sessions)
        assertEquals(12_000.0, running.totalDistanceMeters!!, 1e-9)
    }

    @Test fun `blank titles and zero distances are dropped, not shown`() {
        val detail = workoutsDetail(
            listOf(workout(0, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, 30, distanceMeters = 0.0, title = "  ")),
        )!!

        val session = detail.sessions.single()
        assertNull(session.title)
        assertNull(session.distanceMeters)
    }
}
