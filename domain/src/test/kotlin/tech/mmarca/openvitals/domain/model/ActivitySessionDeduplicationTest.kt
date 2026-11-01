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

    @Test fun `two sources filing one walk under different types are one session`() {
        // One walk, two wrists: the Garmin calls it a walk, the band a hike.
        val garmin = exercise(
            id = "garmin",
            source = "tech.mmarca.openvitals",
            exerciseType = 79,
            start = "2026-10-25T09:00:00Z",
            end = "2026-10-25T12:05:00Z",
            distanceMeters = 15_000.0,
            heartRateBpm = 110L,
        )
        val band = exercise(
            id = "band",
            source = "nodomain.freeyourgadget.gadgetbridge",
            exerciseType = 37,
            start = "2026-10-25T09:02:00Z",
            end = "2026-10-25T12:03:00Z",
            distanceMeters = 14_800.0,
        )

        assertEquals(listOf("garmin"), deduplicateExerciseSessions(listOf(band, garmin)).map { it.id })
    }

    @Test fun `one source keeps two overlapping sessions of different types`() {
        // Yoga and stretching logged over the same hour by hand are two records on purpose.
        val yoga = exercise(
            id = "yoga",
            source = "tech.mmarca.openvitals",
            exerciseType = 83,
            start = "2026-10-25T18:00:00Z",
            end = "2026-10-25T19:00:00Z",
        )
        val stretching = exercise(
            id = "stretching",
            source = "tech.mmarca.openvitals",
            exerciseType = 68,
            start = "2026-10-25T18:00:00Z",
            end = "2026-10-25T19:00:00Z",
        )

        assertEquals(2, deduplicateExerciseSessions(listOf(yoga, stretching)).size)
    }

    @Test fun `the most recently edited of two identical duplicates wins`() {
        val older = exercise(
            id = "older",
            source = "com.watch",
            start = "2026-07-12T09:00:00Z",
            end = "2026-07-12T09:30:00Z",
            lastModifiedTime = Instant.parse("2026-07-12T10:00:00Z"),
        )
        val newer = exercise(
            id = "newer",
            source = "com.watch",
            start = "2026-07-12T09:00:00Z",
            end = "2026-07-12T09:30:00Z",
            lastModifiedTime = Instant.parse("2026-07-12T11:00:00Z"),
        )

        // Same richness, same duration: only lastModifiedTime can separate them, in either arrival order.
        assertEquals("newer", deduplicateExerciseSessions(listOf(older, newer)).single().id)
        assertEquals("newer", deduplicateExerciseSessions(listOf(newer, older)).single().id)
    }

    private fun exercise(
        id: String,
        source: String,
        start: String,
        end: String,
        exerciseType: Int = 1,
        distanceMeters: Double? = null,
        heartRateBpm: Long? = null,
        routePoints: Int = 0,
        lastModifiedTime: Instant? = null,
    ): ExerciseData {
        val startTime = Instant.parse(start)
        val endTime = Instant.parse(end)
        return ExerciseData(
            id = id,
            title = null,
            exerciseType = exerciseType,
            startTime = startTime,
            endTime = endTime,
            durationMs = Duration.between(startTime, endTime).toMillis(),
            source = source,
            totalDistanceMeters = distanceMeters,
            averageHeartRateBpm = heartRateBpm,
            lastModifiedTime = lastModifiedTime,
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
