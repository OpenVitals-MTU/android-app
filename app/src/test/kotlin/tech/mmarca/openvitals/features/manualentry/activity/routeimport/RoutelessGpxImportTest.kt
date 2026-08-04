package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryUnits
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
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest

/**
 * A GPX with no locations in it — and an activity all the same.
 *
 * Both fixtures are REAL HealthFit exports the app refused with "GPX route must
 * contain at least 2 timestamped location points". The reasoning behind that
 * refusal was that a GPX is a list of PLACES, so an indoor session cannot be
 * written as one. The files say otherwise: every `<trkpt>` carries a `<time>`
 * and NO `lat`/`lon` whatsoever — 1931 of them for a strength session, 1422 for
 * an indoor run — and the strength file hangs a heart rate off each.
 *
 * What a routeless GPX genuinely lacks is DISTANCE and CALORIES. Those are not
 * invented: distance stays zero, and the calories are estimated by the entry
 * form from the duration — which is safe here exactly because the file measured
 * none to contradict.
 */
class RoutelessGpxImportTest {

    private fun parse(fixture: String): RouteFileImport {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/gpx/$fixture")) {
            "Missing fixture /gpx/$fixture"
        }.use { it.readBytes() }
        return RouteFileParser.parseFile(bytes, fileName = fixture)
    }

    private data class ImportOutcome(
        val type: ActivityEntryType,
        val request: ActivityWriteRequest?,
        val activeCalories: String,
    )

    private fun import(parsed: RouteFileImport): ImportOutcome {
        val clock = Clock.systemDefaultZone()
        val state = ActivityEntryUiState()
            .withRouteImport(parsed, ActivityEntryUnits.uniform(UnitSystem.METRIC), clock)
        return ImportOutcome(
            type = state.selectedActivityType,
            request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC)),
            activeCalories = state.activeCaloriesText,
        )
    }

    @Test fun `a strength session - 1931 heartbeats and not one location`() {
        val parsed = parse("strength_training.gpx")

        assertTrue(parsed.points.isEmpty())
        // The session is in the timestamps: 05:50:28 to 06:23:23.
        assertEquals(Instant.parse("2026-07-08T05:50:28Z"), parsed.startTime)
        assertEquals(Instant.parse("2026-07-08T06:23:23Z"), parsed.endTime)
        assertEquals(1975L, parsed.durationSeconds)
        // And the heart rate was there all along, in the extensions.
        assertEquals(1931, parsed.bleSamples.heartRateSamples.size)
        assertEquals(101L, parsed.bleSamples.heartRateSamples.first().beatsPerMinute)

        val imported = import(parsed)

        // This used to throw.
        assertNotNull(imported.request)
        requireNotNull(imported.request)
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            imported.type.exerciseType,
        )
        assertTrue(imported.request.routePoints.isEmpty())
        assertEquals(1931, imported.request.bleSamples.heartRateSamples.size)
        assertEquals(0.0, imported.request.distanceMeters ?: 0.0, 0.001)
    }

    @Test fun `an indoor run - the times and the sport the file names`() {
        val parsed = parse("indoor_running.gpx")

        assertTrue(parsed.points.isEmpty())
        assertEquals(1421L, parsed.durationSeconds)
        // `<trk><type>running</type>` — the file says what it is, and is
        // believed.
        assertEquals("running", parsed.type)

        val imported = import(parsed)

        assertNotNull(imported.request)
        requireNotNull(imported.request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, imported.request.exerciseType)
        // Nothing was measured, so the estimate is free to fill both fields —
        // the rule that keeps a guess from ever standing beside a measurement.
        assertTrue(imported.activeCalories.isNotEmpty())
    }

    @Test fun `a GPX with neither places nor times is still refused`() {
        // The guard that survives: an empty (or corrupt, or HTML) file must not
        // arrive as a blank activity. What changed is that "no LOCATIONS" no
        // longer means "no activity" — "no timestamps either" does.
        val empty = "<?xml version=\"1.0\"?><gpx version=\"1.1\"><trk><trkseg>" +
            "</trkseg></trk></gpx>"

        val failure = runCatching { RouteFileParser.parse(empty, fileName = "empty.gpx") }

        assertTrue(failure.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun `a routed GPX keeps its route AND gains its heart rate`() {
        // The same collector runs on files that do have a track, so a GPX whose
        // trackpoints carry `gpxtpx:hr` no longer throws that heart rate away at
        // the parser — it used to import as a bare line on a map.
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
              <trk><type>running</type><trkseg>
                <trkpt lat="52.5" lon="13.4"><ele>34</ele><time>2026-07-08T05:50:28Z</time>
                  <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>120</gpxtpx:hr><gpxtpx:cad>84</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
                </trkpt>
                <trkpt lat="52.51" lon="13.41"><ele>40</ele><time>2026-07-08T05:55:28Z</time>
                  <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>148</gpxtpx:hr><gpxtpx:cad>86</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
                </trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()

        val parsed = RouteFileParser.parse(gpx, fileName = "run.gpx")

        assertEquals(2, parsed.points.size)
        assertEquals(
            listOf(120L, 148L),
            parsed.bleSamples.heartRateSamples.map { it.beatsPerMinute },
        )
        // Per foot, as everywhere else: 84 is 168 steps a minute.
        assertEquals(
            listOf(168L, 172L),
            parsed.bleSamples.stepsCadenceSamples.map { it.stepsPerMinute },
        )
    }
}
