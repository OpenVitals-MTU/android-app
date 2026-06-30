package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitySessionDeduplicationTest {

    @Test fun `deduplicateExerciseSessions keeps richer overlapping same type session`() {
        val phone = exercise(
            id = "phone",
            source = "google-fit",
            start = "2026-05-06T06:00:00Z",
            end = "2026-05-06T07:00:00Z",
        )
        val watch = exercise(
            id = "watch",
            source = "garmin",
            start = "2026-05-06T06:01:00Z",
            end = "2026-05-06T07:01:00Z",
            distanceMeters = 10_000.0,
            heartRateBpm = 154L,
            routePoints = 3,
        )

        val result = deduplicateExerciseSessions(listOf(phone, watch))

        assertEquals(listOf("watch"), result.map { it.id })
    }

    @Test fun `deduplicateExerciseSessions keeps separate non overlapping sessions`() {
        val morning = exercise(
            id = "morning",
            source = "google-fit",
            start = "2026-05-06T06:00:00Z",
            end = "2026-05-06T07:00:00Z",
        )
        val evening = exercise(
            id = "evening",
            source = "garmin",
            start = "2026-05-06T18:00:00Z",
            end = "2026-05-06T19:00:00Z",
        )

        val result = deduplicateExerciseSessions(listOf(morning, evening))

        assertEquals(listOf("evening", "morning"), result.map { it.id })
    }

    private fun exercise(
        id: String,
        source: String,
        start: String,
        end: String,
        distanceMeters: Double? = null,
        heartRateBpm: Long? = null,
        routePoints: Int = 0,
    ): ExerciseData {
        val startTime = Instant.parse(start)
        val endTime = Instant.parse(end)
        return ExerciseData(
            id = id,
            title = null,
            exerciseType = 1,
            startTime = startTime,
            endTime = endTime,
            durationMs = Duration.between(startTime, endTime).toMillis(),
            source = source,
            totalDistanceMeters = distanceMeters,
            averageHeartRateBpm = heartRateBpm,
            route = ExerciseRouteData(
                status = if (routePoints > 0) ExerciseRouteStatus.DATA else ExerciseRouteStatus.NO_DATA,
                points = List(routePoints) { index ->
                    ExerciseRoutePoint(
                        time = startTime.plusSeconds(index.toLong()),
                        latitude = 59.0,
                        longitude = 24.0,
                        altitudeMeters = null,
                        horizontalAccuracyMeters = null,
                        verticalAccuracyMeters = null,
                    )
                },
            ),
        )
    }
}
