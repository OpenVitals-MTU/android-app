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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest

/**
 * Importing an activity with no GPS. Both fixtures are real files that used to fail:
 *
 *  * Calories: a FIT session has no active-calorie field, so the form estimated it,
 *    the estimate exceeded the measured total, and the write was refused.
 *  * Sport: type inference substring-matched sport, name and file name together,
 *    so `Indoor_CyclingiSmoothRun.fit` imported as a run.
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
            .withRouteImport(parsed, ActivityEntryUnits.uniform(UnitSystem.METRIC), clock)
        return ImportOutcome(
            type = state.selectedActivityType,
            request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC)),
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

        // Used to be null: an estimated 226 active sat above the measured 208 total.
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
        // The heart rate the trainer recorded rides along in the sample buffer.
        assertEquals(8, imported.request.bleSamples.heartRateSamples.size)
    }

    @Test fun `a file that measured NO calories still gets both estimated`() {
        // A GPX carries no calories, and its import must still arrive with a usable pair.
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
                ActivityEntryUnits.uniform(UnitSystem.METRIC),
                clock,
            )

        assertTrue(state.activeCaloriesText.isNotEmpty())
        assertTrue(state.totalCaloriesText.isNotEmpty())
    }

    @Test fun `a generic FIT sport still yields to the name`() {
        // FIT's `training` and `fitness equipment` mean "I do not know". The sport wins over
        // the file name only when it names something.
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
