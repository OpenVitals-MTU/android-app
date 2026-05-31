package tech.mmarca.openvitals.features.activity

import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseRouteData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.model.ExerciseRouteStatus
import tech.mmarca.openvitals.features.manualentry.RouteFileParser

class ActivityRouteExportTest {

    @Test fun `kmz export writes parseable route with metadata`() {
        val points = listOf(
            routePoint("2026-05-26T08:30:00Z", latitude = 59.0000, longitude = 24.0000, altitude = 10.0),
            routePoint("2026-05-26T08:31:00Z", latitude = 59.0010, longitude = 24.0020, altitude = 18.0),
        )
        val workout = workout(
            title = "Morning run",
            notes = "Easy commute",
            points = points,
        )
        val output = ByteArrayOutputStream()

        writeActivityRouteKmz(workout, points, output)

        val parsed = RouteFileParser.parseFile(output.toByteArray(), fileName = "morning-run.kmz")
        assertEquals("Morning run", parsed.name)
        assertEquals("Easy commute", parsed.description)
        assertEquals(points.size, parsed.points.size)
        assertEquals(points.first().time, parsed.startTime)
        assertEquals(points.last().time, parsed.endTime)
        assertEquals(8.0, parsed.elevationGainedMeters, 0.001)
    }

    @Test fun `route export file names use selected format extension`() {
        val workout = workout(
            title = "Morning Run!",
            notes = null,
            points = emptyList(),
        )

        val gpxName = workout.routeExportFileName(ActivityRouteExportFormat.GPX)
        val kmzName = workout.routeExportFileName(ActivityRouteExportFormat.KMZ)

        assertTrue(gpxName.startsWith("morning-run-"))
        assertTrue(gpxName.endsWith(".gpx"))
        assertTrue(kmzName.startsWith("morning-run-"))
        assertTrue(kmzName.endsWith(".kmz"))
    }

    private fun workout(
        title: String?,
        notes: String?,
        points: List<ExerciseRoutePoint>,
    ) = ExerciseData(
        id = "activity-1",
        title = title,
        exerciseType = 56,
        startTime = Instant.parse("2026-05-26T08:30:00Z"),
        endTime = Instant.parse("2026-05-26T09:30:00Z"),
        durationMs = 3_600_000,
        source = "test",
        notes = notes,
        route = ExerciseRouteData(
            status = ExerciseRouteStatus.DATA,
            points = points,
        ),
    )

    private fun routePoint(
        time: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
    ) = ExerciseRoutePoint(
        time = Instant.parse(time),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitude,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )
}
