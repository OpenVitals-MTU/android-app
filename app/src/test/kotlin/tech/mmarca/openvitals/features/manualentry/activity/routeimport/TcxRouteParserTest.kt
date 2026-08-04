package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Clock
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest

/**
 * TCX — the format Strava and Garmin export an INDOOR activity as, and the one
 * the app could not read.
 *
 * A user reporting "GPX route must contain at least 2 timestamped location
 * points" on an indoor activity was usually holding a TCX. It is XML, so it
 * fell through the dispatcher into the GPX parser, which looked for `trkpt`,
 * found none, and blamed the file — while the file was carrying a complete
 * session: duration, distance, calories, heart rate and cadence, with the
 * `Position` element that a GPX cannot live without simply absent.
 */
class TcxRouteParserTest {

    private fun parse(fixture: String): RouteFileImport {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/tcx/$fixture")) {
            "Missing fixture /tcx/$fixture"
        }.use { it.readBytes() }
        return RouteFileParser.parseFile(bytes, fileName = fixture)
    }

    private fun writeRequest(parsed: RouteFileImport): ActivityWriteRequest? {
        val clock = Clock.systemDefaultZone()
        val state = ActivityEntryUiState()
            .withRouteImport(parsed, UnitSystem.METRIC, clock)
        return buildWriteRequest(state, UnitSystem.METRIC)
    }

    // An indoor ride: no GPS, and a whole activity anyway.

    @Test fun `indoor ride keeps the session the file recorded`() {
        val parsed = parse("indoor_ride.tcx")

        // No Position: that is the point.
        assertTrue(parsed.points.isEmpty())
        assertEquals(15_000.0, parsed.distanceMeters, 0.001)
        assertEquals(1800L, parsed.durationSeconds)
        assertEquals(420.0, parsed.totalCaloriesKcal!!, 0.001)
        // TCX has no active-calorie field, so active stays unknown rather than
        // being invented — an estimate placed beside a measured total is what
        // made every routeless FIT file unsavable.
        assertNull(parsed.activeCaloriesKcal)
        assertEquals(Instant.parse("2026-01-14T18:30:00Z"), parsed.startTime)
        assertEquals(Instant.parse("2026-01-14T19:00:00Z"), parsed.endTime)
    }

    @Test fun `indoor ride carries the heart rate cadence and speed beside it`() {
        val parsed = parse("indoor_ride.tcx")

        assertEquals(
            listOf(110L, 142L, 138L),
            parsed.bleSamples.heartRateSamples.map { it.beatsPerMinute },
        )
        // A bike's cadence is PEDALLING cadence: a different Health Connect
        // record from a runner's steps, and the sport is what decides which.
        assertEquals(
            listOf(85L, 92L, 88L),
            parsed.bleSamples.cyclingCadenceSamples.map { it.rpm },
        )
        assertTrue(parsed.bleSamples.stepsCadenceSamples.isEmpty())
        assertEquals(
            listOf(7.5, 8.9, 8.1),
            parsed.bleSamples.speedSamples.map { it.metersPerSecond },
        )
    }

    @Test fun `indoor ride imports - which is the whole bug`() {
        val request = writeRequest(parse("indoor_ride.tcx"))

        assertNotNull(request)
        requireNotNull(request)
        assertEquals(15_000.0, request.distanceMeters!!, 0.001)
        assertEquals(420.0, request.totalCaloriesKcal!!, 0.001)
        assertNull(request.activeCaloriesKcal)
        assertTrue(request.routePoints.isEmpty())
        assertEquals(3, request.bleSamples.heartRateSamples.size)
    }

    // An outdoor run: the route still works.

    @Test fun `outdoor run reads the track and the samples along it`() {
        val parsed = parse("outdoor_run.tcx")

        assertEquals(3, parsed.points.size)
        assertEquals(52.5, parsed.points.first().latitude, 0.0001)
        assertEquals(34.0, parsed.points.first().altitudeMeters!!, 0.001)
        assertEquals(2_000.0, parsed.distanceMeters, 0.001)
        assertEquals(150.0, parsed.totalCaloriesKcal!!, 0.001)
        assertEquals(3, parsed.bleSamples.heartRateSamples.size)
        // A runner's TCX cadence counts ONE foot: 82 is 164 steps a minute, and
        // every watch that reads it doubles it back.
        assertEquals(
            listOf(164L, 172L, 168L),
            parsed.bleSamples.stepsCadenceSamples.map { it.stepsPerMinute },
        )
        assertTrue(parsed.bleSamples.cyclingCadenceSamples.isEmpty())
    }

    @Test fun `outdoor run is a run and it saves`() {
        val request = writeRequest(parse("outdoor_run.tcx"))

        assertNotNull(request)
        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, request.exerciseType)
        assertEquals(3, request.routePoints.size)
    }

    @Test fun `a TCX is recognised by its CONTENT not its extension`() {
        // The dispatcher sniffs. A .tcx renamed to .gpx used to die in the GPX
        // parser with a message about location points — the very report that
        // started this.
        val bytes = requireNotNull(javaClass.getResourceAsStream("/tcx/indoor_ride.tcx")) {
            "Missing fixture /tcx/indoor_ride.tcx"
        }.use { it.readBytes() }

        val parsed = RouteFileParser.parseFile(bytes, fileName = "mystery.gpx")

        assertEquals(15_000.0, parsed.distanceMeters, 0.001)
    }
}
