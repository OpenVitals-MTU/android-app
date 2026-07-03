package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityBackfillTest {

    @Test
    fun `route backfill fills missing distance and elevation`() {
        val workout = workout(
            totalDistanceMeters = null,
            elevationGainedMeters = null,
            route = ExerciseRouteData(
                status = ExerciseRouteStatus.DATA,
                points = listOf(
                    routePoint(seconds = 0, latitude = 0.0, longitude = 0.0, altitudeMeters = 10.0),
                    routePoint(seconds = 60, latitude = 0.0, longitude = 0.01, altitudeMeters = 12.0),
                    routePoint(seconds = 120, latitude = 0.01, longitude = 0.01, altitudeMeters = 11.0),
                    routePoint(seconds = 180, latitude = 0.01, longitude = 0.02, altitudeMeters = 14.5),
                ),
            ),
        )

        val result = workout.withRouteBackfilledMetrics()

        assertEquals(3_335.85, result.totalDistanceMeters ?: 0.0, 0.1)
        assertEquals(5.5, result.elevationGainedMeters ?: 0.0, 0.001)
    }

    @Test
    fun `route backfill replaces empty zero summaries with route values`() {
        val workout = workout(
            totalDistanceMeters = 0.0,
            elevationGainedMeters = 0.0,
            route = ExerciseRouteData(
                status = ExerciseRouteStatus.DATA,
                points = listOf(
                    routePoint(seconds = 0, latitude = 0.0, longitude = 0.0, altitudeMeters = 10.0),
                    routePoint(seconds = 60, latitude = 0.0, longitude = 0.01, altitudeMeters = 13.0),
                ),
            ),
        )

        val result = workout.withRouteBackfilledMetrics()

        assertEquals(1_111.95, result.totalDistanceMeters ?: 0.0, 0.1)
        assertEquals(3.0, result.elevationGainedMeters ?: 0.0, 0.001)
    }

    @Test
    fun `route backfill preserves recorded summaries`() {
        val workout = workout(
            totalDistanceMeters = 500.0,
            elevationGainedMeters = 20.0,
            route = ExerciseRouteData(
                status = ExerciseRouteStatus.DATA,
                points = listOf(
                    routePoint(seconds = 0, latitude = 0.0, longitude = 0.0, altitudeMeters = 10.0),
                    routePoint(seconds = 60, latitude = 0.0, longitude = 0.01, altitudeMeters = 13.0),
                ),
            ),
        )

        val result = workout.withRouteBackfilledMetrics()

        assertEquals(500.0, result.totalDistanceMeters ?: 0.0, 0.001)
        assertEquals(20.0, result.elevationGainedMeters ?: 0.0, 0.001)
    }

    @Test
    fun `route backfill leaves elevation missing without altitude data`() {
        val workout = workout(
            totalDistanceMeters = null,
            elevationGainedMeters = null,
            route = ExerciseRouteData(
                status = ExerciseRouteStatus.DATA,
                points = listOf(
                    routePoint(seconds = 0, latitude = 0.0, longitude = 0.0),
                    routePoint(seconds = 60, latitude = 0.0, longitude = 0.01),
                ),
            ),
        )

        val result = workout.withRouteBackfilledMetrics()

        assertEquals(1_111.95, result.totalDistanceMeters ?: 0.0, 0.1)
        assertNull(result.elevationGainedMeters)
    }

    @Test
    fun `sample backfill fills missing averages`() {
        val workout = workout()

        val result = workout.withSampleBackfilledMetrics(
            heartRateSamples = listOf(
                HeartRateSample(time = Instant.EPOCH, beatsPerMinute = 100L, source = "test"),
                HeartRateSample(time = Instant.EPOCH.plusSeconds(1), beatsPerMinute = 110L, source = "test"),
            ),
            speedSamples = listOf(
                SpeedSample(time = Instant.EPOCH, metersPerSecond = 2.0, source = "test"),
                SpeedSample(time = Instant.EPOCH.plusSeconds(1), metersPerSecond = 4.0, source = "test"),
            ),
            cadenceSamples = listOf(
                ActivityCadenceSample(
                    time = Instant.EPOCH,
                    rate = 160.0,
                    kind = ActivityCadenceKind.STEPS,
                    source = "test",
                ),
                ActivityCadenceSample(
                    time = Instant.EPOCH.plusSeconds(1),
                    rate = 180.0,
                    kind = ActivityCadenceKind.STEPS,
                    source = "test",
                ),
                ActivityCadenceSample(
                    time = Instant.EPOCH,
                    rate = 80.0,
                    kind = ActivityCadenceKind.CYCLING,
                    source = "test",
                ),
                ActivityCadenceSample(
                    time = Instant.EPOCH.plusSeconds(1),
                    rate = 100.0,
                    kind = ActivityCadenceKind.CYCLING,
                    source = "test",
                ),
            ),
        )

        assertEquals(105L, result.averageHeartRateBpm)
        assertEquals(3.0, result.averageSpeedMetersPerSecond ?: 0.0, 0.001)
        assertEquals(170.0, result.averageStepsCadenceRate ?: 0.0, 0.001)
        assertEquals(90.0, result.averageCyclingCadenceRpm ?: 0.0, 0.001)
    }

    @Test
    fun `sample backfill preserves recorded averages`() {
        val workout = workout(
            averageHeartRateBpm = 130L,
            averageSpeedMetersPerSecond = 5.0,
            averageStepsCadenceRate = 190.0,
            averageCyclingCadenceRpm = 95.0,
        )

        val result = workout.withSampleBackfilledMetrics(
            heartRateSamples = listOf(HeartRateSample(Instant.EPOCH, 100L, "test")),
            speedSamples = listOf(SpeedSample(Instant.EPOCH, 2.0, "test")),
            cadenceSamples = listOf(
                ActivityCadenceSample(Instant.EPOCH, 160.0, ActivityCadenceKind.STEPS, "test"),
                ActivityCadenceSample(Instant.EPOCH, 80.0, ActivityCadenceKind.CYCLING, "test"),
            ),
        )

        assertEquals(130L, result.averageHeartRateBpm)
        assertEquals(5.0, result.averageSpeedMetersPerSecond ?: 0.0, 0.001)
        assertEquals(190.0, result.averageStepsCadenceRate ?: 0.0, 0.001)
        assertEquals(95.0, result.averageCyclingCadenceRpm ?: 0.0, 0.001)
    }

    private fun workout(
        totalDistanceMeters: Double? = null,
        elevationGainedMeters: Double? = null,
        averageHeartRateBpm: Long? = null,
        averageSpeedMetersPerSecond: Double? = null,
        averageStepsCadenceRate: Double? = null,
        averageCyclingCadenceRpm: Double? = null,
        route: ExerciseRouteData = ExerciseRouteData(),
    ) = ExerciseData(
        id = "activity-1",
        title = "Morning run",
        exerciseType = 56,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusSeconds(3_600),
        durationMs = 3_600_000L,
        source = "test",
        totalDistanceMeters = totalDistanceMeters,
        elevationGainedMeters = elevationGainedMeters,
        averageHeartRateBpm = averageHeartRateBpm,
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
        averageStepsCadenceRate = averageStepsCadenceRate,
        averageCyclingCadenceRpm = averageCyclingCadenceRpm,
        route = route,
    )

    private fun routePoint(
        seconds: Long,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double? = null,
    ) = ExerciseRoutePoint(
        time = Instant.EPOCH.plusSeconds(seconds),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )
}
