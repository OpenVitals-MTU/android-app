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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest

/**
 * Importing an activity that has NO GPS: an indoor run, a trainer ride, a
 * strength session.
 *
 * Both fixtures are REAL files a user could not import. They parsed fine — the
 * FIT parser has always had a routeless branch — and then died one layer later,
 * twice over:
 *
 *  * CALORIES. A FIT session records `total_calories` and has no active-calorie
 *    field, so active came back null and the form ESTIMATED it from METs and
 *    distance. The estimate landed beside the file's own measured total and
 *    exceeded it (226 estimated active against a measured 208 total), so the
 *    write was refused for "total cannot be lower than active" — an invented
 *    number contradicting a measured one. This hit every routeless FIT file.
 *
 *  * SPORT. Type inference joined the sport, the name and the FILE NAME into
 *    one string and substring-matched it, testing `run` before `cycling`. So
 *    `…Indoor_CyclingiSmoothRun.fit` — a 27 km trainer ride — imported as a
 *    RUN, because the exporter's name is in the file name. The FIT sport said
 *    cycling, and knew, and was outvoted.
 */
class IndoorFitImportTest {

    private fun parse(fixture: String): RouteFileImport {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/fit/$fixture")) {
            "Missing fixture /fit/$fixture"
        }.use { it.readBytes() }
        return RouteFileParser.parseFile(bytes, fileName = fixture)
    }

    private data class ImportOutcome(
        val type: ActivityEntryType,
        val request: ActivityWriteRequest?,
    )

    /** The chain the Settings importer and the entry form both run. */
    private fun import(parsed: RouteFileImport): ImportOutcome {
        val clock = Clock.systemDefaultZone()
        val state = ActivityEntryUiState()
            .withRouteImport(parsed, UnitSystem.METRIC, clock)
        return ImportOutcome(
            type = state.selectedActivityType,
            request = buildWriteRequest(state, UnitSystem.METRIC),
        )
    }

    @Test fun `an indoor run imports keeping the calories it measured`() {
        // RunGap, 2.33 km on a treadmill. 208 kcal in the file.
        val parsed = parse("indoor_running_rungap.fit")
        // No GPS: that is the point.
        assertTrue(parsed.points.isEmpty())
        assertEquals(208.0, parsed.totalCaloriesKcal!!, 0.001)
        assertNull(parsed.activeCaloriesKcal)

        val imported = import(parsed)

        // It used to be null here — refused, because an estimated 226 active
        // sat above the measured 208 total.
        assertNotNull(imported.request)
        requireNotNull(imported.request)
        assertEquals(208.0, imported.request.totalCaloriesKcal!!, 0.001)
        // The file did not measure it, so it is not invented.
        assertNull(imported.request.activeCaloriesKcal)
        assertEquals(2_334.0, imported.request.distanceMeters!!, 0.001)
        assertTrue(imported.request.routePoints.isEmpty())
    }

    @Test fun `an indoor ride imports as a STATIONARY BIKE not as a run`() {
        // iSmoothRun, 27.46 km on a trainer. The file name contains "Run".
        val parsed = parse("indoor_cycling_ismoothrun.fit")
        assertTrue(parsed.points.isEmpty())

        val imported = import(parsed)

        // The FIT sport says cycling; the file NAME says run, and loses.
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            imported.type.exerciseType,
        )
        assertFalse(imported.type.supportsGpsRoute)
        assertNotNull(imported.request)
        requireNotNull(imported.request)
        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            imported.request.exerciseType,
        )
        assertEquals(27_460.0, imported.request.distanceMeters!!, 0.001)
        assertEquals(945.0, imported.request.totalCaloriesKcal!!, 0.001)
        // The heart rate the trainer recorded rides along in the sample buffer
        // — an indoor ride has no route to carry it, but it is not without
        // data.
        assertEquals(8, imported.request.bleSamples.heartRateSamples.size)
    }

    @Test fun `a file that measured NO calories still gets both estimated`() {
        // The mirror of the calorie fix, and the reason it is "estimate both or
        // estimate neither" rather than "never estimate": a GPX carries no
        // calories at all, and its import must still arrive with a usable pair.
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val clock = Clock.systemDefaultZone()
        val state = ActivityEntryUiState()
            .withRouteImport(
                RouteFileImport(
                    fileName = "morning_run.gpx",
                    points = emptyList(),
                    distanceMeters = 5_000.0,
                    elevationGainedMeters = 0.0,
                    startTime = start,
                    endTime = start.plusSeconds(30 * 60),
                    type = "running",
                ),
                UnitSystem.METRIC,
                clock,
            )

        assertTrue(state.activeCaloriesText.isNotEmpty())
        assertTrue(state.totalCaloriesText.isNotEmpty())
    }

    @Test fun `a generic FIT sport still yields to the name`() {
        // FIT's `training` and `fitness equipment` are its "I do not know"
        // answers. The sport wins over the file name only when it NAMES
        // something — otherwise `Functional Strength Training.fit` would import
        // as a generic workout, losing what the only informative word in the
        // file was telling us.
        val strength = inferActivityType(
            RouteFileImport(
                fileName = "Functional Strength Training.fit",
                points = emptyList(),
                distanceMeters = 0.0,
                elevationGainedMeters = 0.0,
                startTime = Instant.parse("2026-05-26T08:00:00Z"),
                endTime = Instant.parse("2026-05-26T09:00:00Z"),
                type = "training",
            ),
            DefaultActivityEntryTypes.first(),
        )

        assertEquals(
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            strength.exerciseType,
        )
    }
}
