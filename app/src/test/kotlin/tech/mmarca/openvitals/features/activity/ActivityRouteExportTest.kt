package tech.mmarca.openvitals.features.activity

import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileParser

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

    @Test fun `gpx export writes parseable route with metadata`() {
        val points = listOf(
            routePoint("2026-05-26T08:30:00Z", latitude = 59.0000, longitude = 24.0000, altitude = 10.0),
            routePoint("2026-05-26T08:31:30Z", latitude = 59.0010, longitude = 24.0020, altitude = 18.0),
        )
        val workout = workout(
            title = "Morning run",
            notes = "Easy commute",
            points = points,
        )
        val output = ByteArrayOutputStream()

        writeActivityRouteGpx(workout, points, output, serializer = recordingXmlSerializer())

        val gpx = output.toString(Charsets.UTF_8.name())
        assertTrue(gpx.contains("""creator="OpenVitals""""))
        // Instant.toString drops the zero fraction — no `.000Z` anywhere.
        assertFalse(gpx.contains(".000Z"))
        assertTrue(gpx.contains("<time>2026-05-26T08:30:00Z</time>"))
        val parsed = RouteFileParser.parse(gpx, fileName = "morning-run.gpx")
        assertEquals("Morning run", parsed.name)
        assertEquals("Easy commute", parsed.description)
        assertEquals(points.size, parsed.points.size)
        assertEquals(points.first().time, parsed.startTime)
        assertEquals(points.last().time, parsed.endTime)
    }

    @Test fun `kmz escapes markup in title and notes`() {
        val points = listOf(
            routePoint("2026-05-26T08:30:00Z", latitude = 59.0, longitude = 24.0, altitude = 1.0),
            routePoint("2026-05-26T08:31:00Z", latitude = 59.001, longitude = 24.002, altitude = 2.0),
        )
        val workout = workout(
            title = """Run <with> "friends" & family""",
            notes = "it's <fine>",
            points = points,
        )
        val output = ByteArrayOutputStream()

        writeActivityRouteKmz(workout, points, output)

        val parsed = RouteFileParser.parseFile(output.toByteArray(), fileName = "run.kmz")
        assertEquals("""Run <with> "friends" & family""", parsed.name)
        assertEquals("it's <fine>", parsed.description)
    }

    @Test fun `blank title falls back to activity-route`() {
        val workout = workout(title = "  !!! ", notes = null, points = emptyList())

        assertTrue(
            workout.routeExportFileName(ActivityRouteExportFormat.GPX)
                .startsWith("activity-route-"),
        )
    }

    @Test fun `sorted points require a non-empty route`() {
        // The share/save path must fail rather than hand another app an empty
        // track it will render as a blank map.
        val workout = workout(title = null, notes = null, points = emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            workout.sortedRoutePointsForExport()
        }
    }

    @Test fun `sorted points order by time`() {
        val late = routePoint("2026-05-26T08:31:00Z", latitude = 1.0, longitude = 1.0, altitude = null)
        val early = routePoint("2026-05-26T08:30:00Z", latitude = 0.0, longitude = 0.0, altitude = null)
        val workout = workout(title = null, notes = null, points = listOf(late, early))

        assertEquals(listOf(early, late), workout.sortedRoutePointsForExport())
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
        altitude: Double?,
    ) = ExerciseRoutePoint(
        time = Instant.parse(time),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitude,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )
}
