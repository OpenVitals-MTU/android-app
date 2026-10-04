package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.RouteElevation
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

class ActivityRecordingSplitsTest {

    @Test fun `interval splits are empty for empty and single point routes`() {
        assertTrue(activityRecordingIntervalSplits(emptyList(), emptyList()).isEmpty())
        assertTrue(activityRecordingIntervalSplits(listOf(point(0, 0.0)), emptyList()).isEmpty())
    }

    @Test fun `interval splits use route breaks and do not count gap distance`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 60, latitude = 0.001),
            point(seconds = 120, latitude = 0.100),
            point(seconds = 180, latitude = 0.101),
        )

        val splits = activityRecordingIntervalSplits(points, routeBreakIndexes = listOf(2))

        assertEquals(2, splits.size)
        assertEquals(111.2, splits[0].distanceMeters, 1.0)
        assertEquals(111.2, splits[1].distanceMeters, 1.0)
        assertTrue(splits.sumOf { it.distanceMeters } < 250.0)
    }

    @Test fun `time splits include active incomplete split`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0, altitude = 0.0),
            point(seconds = 60, latitude = 0.001, altitude = 10.0),
            point(seconds = 120, latitude = 0.002, altitude = 20.0),
            point(seconds = 180, latitude = 0.003, altitude = 20.0),
        )

        val splits = activityRecordingTimeSplits(
            points = points,
            routeBreakIndexes = emptyList(),
            splitMillis = 120_000L,
        )

        assertEquals(2, splits.size)
        assertEquals(120_000L, splits[0].elapsedMillis)
        assertEquals(60_000L, splits[1].elapsedMillis)
        // Four fixes are too few for the filter to settle by the boundary; the whole still adds up.
        assertEquals(20.0, splits.sumOf { it.climbMeters }, 0.1)
    }

    @Test fun `GPS altitude noise is not banked as climb in every split`() {
        // One fix a second, plus-minus 4 m: summing raw rises gave each minute ~240 m of climb.
        val points = (0L until 600L).map { second ->
            point(seconds = second, latitude = second * 0.00003, altitude = if (second % 2 == 0L) 4.0 else -4.0)
        }

        val timeSplits = activityRecordingTimeSplits(points, emptyList(), splitMillis = 60_000L)
        val distanceSplits = activityRecordingDistanceSplits(points, emptyList(), splitMeters = 500.0)
        val intervalSplits = activityRecordingIntervalSplits(points, emptyList())

        assertEquals(10, timeSplits.size)
        (timeSplits + distanceSplits + intervalSplits).forEach { split ->
            assertEquals(0.0, split.climbMeters, 0.01)
        }
    }

    @Test fun `split climbs add up to the recording's elevation gain`() {
        // A 60 m climb over ten minutes with 3 m of jitter, cut by the minute.
        val points = (0L until 600L).map { second ->
            val jitter = if (second % 3 == 0L) 3.0 else -1.5
            point(seconds = second, latitude = second * 0.00003, altitude = 100.0 + second / 10.0 + jitter)
        }

        val splits = activityRecordingTimeSplits(points, emptyList(), splitMillis = 60_000L)

        assertEquals(RouteElevation.routeElevationGain(points), splits.sumOf { it.climbMeters }, 0.01)
        assertEquals(60.0, splits.sumOf { it.climbMeters }, 6.0)
    }

    @Test fun `time splits do not calculate across route breaks`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 60, latitude = 0.001),
            point(seconds = 120, latitude = 0.100),
            point(seconds = 180, latitude = 0.101),
        )

        val splits = activityRecordingTimeSplits(
            points = points,
            routeBreakIndexes = listOf(2),
            splitMillis = 3_600_000L,
        )

        assertEquals(1, splits.size)
        assertTrue(splits.single().distanceMeters < 250.0)
    }

    @Test fun `distance splits create fixed distance buckets with active remainder`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 60, latitude = 0.001),
            point(seconds = 120, latitude = 0.002),
        )

        val splits = activityRecordingDistanceSplits(
            points = points,
            routeBreakIndexes = emptyList(),
            splitMeters = 100.0,
        )

        assertEquals(3, splits.size)
        assertEquals(100.0, splits[0].distanceMeters, 0.1)
        assertEquals(100.0, splits[1].distanceMeters, 0.1)
        assertTrue(splits[2].distanceMeters in 20.0..25.0)
    }

    @Test fun `split max speed is calculated per bucket`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 100, latitude = 0.001),
            point(seconds = 110, latitude = 0.002),
        )

        val splits = activityRecordingTimeSplits(
            points = points,
            routeBreakIndexes = emptyList(),
            splitMillis = 120_000L,
        )

        assertEquals(1, splits.size)
        assertTrue(splits.single().maxSpeedMetersPerSecond > splits.single().averageSpeedMetersPerSecond)
    }

    @Test fun `manual lap splits do not count route break gaps`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 60, latitude = 0.001),
            point(seconds = 120, latitude = 0.100),
            point(seconds = 180, latitude = 0.101),
        )

        val splits = activityRecordingLapSplits(
            laps = listOf(
                ActivityRecordingLap(
                    startTime = Start,
                    endTime = Start.plusSeconds(180),
                    distanceMeters = null,
                )
            ),
            points = points,
            routeBreakIndexes = listOf(2),
            recordingStartTime = Start,
        )

        assertEquals(1, splits.size)
        assertTrue(splits.single().distanceMeters < 250.0)
    }

    @Test fun `route distance helper avoids route break gaps`() {
        val points = listOf(
            point(seconds = 0, latitude = 0.0),
            point(seconds = 60, latitude = 0.001),
            point(seconds = 120, latitude = 0.100),
            point(seconds = 180, latitude = 0.101),
        )

        val distance = activityRecordingRouteDistanceMeters(
            points = points,
            routeBreakIndexes = listOf(2),
            startTime = Start,
            endTime = Start.plusSeconds(180),
        )

        assertTrue(distance < 250.0)
    }

    private fun point(
        seconds: Long,
        latitude: Double,
        altitude: Double? = null,
    ): ExerciseRoutePoint =
        ExerciseRoutePoint(
            time = Start.plusSeconds(seconds),
            latitude = latitude,
            longitude = 0.0,
            altitudeMeters = altitude,
            horizontalAccuracyMeters = 5.0,
            verticalAccuracyMeters = null,
        )

    private companion object {
        val Start: Instant = Instant.parse("2026-01-01T10:00:00Z")
    }
}
