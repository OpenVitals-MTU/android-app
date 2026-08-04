package tech.mmarca.openvitals.features.activity.maps

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.maps.MapView as MapLibreMapView
import org.mapsforge.map.android.view.MapView as MapsforgeMapView
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The map half of Flutter's `test/features/activity/maps/route_map_view_test.dart`
 * — the cases that only exist once an offline pack is in play. The route-drawing
 * half lives in [tech.mmarca.openvitals.features.activity.RoutePreviewTest].
 *
 * [OfflineRouteMapOrPreview] chooses between a real tile renderer and the
 * canvas preview purely from [OfflineMapRepository.state], which is read back
 * from `filesDir/offline_maps/metadata.json`. That makes the branch reachable
 * from a test without a real map: this class moves the installed library aside,
 * writes its own metadata plus a stub pack file, and calls
 * [OfflineMapRepository.refresh] — then puts the real library back afterwards.
 *
 * What that buys and what it does not:
 *  - The branch itself, the recenter control and the recenter tap are real, and
 *    the assertions distinguish the two branches: `noPackDrawsNoMapView` asks
 *    for the control and asserts it is absent, while
 *    `anImportedPackRendersAMapView...` proves the very same request produces
 *    one once a pack exists. Neither can pass for the "no map was built at all"
 *    reason.
 *  - The pack file is a stub, so nothing is drawn from it. These tests say
 *    nothing about tile rendering, labels or the camera's resulting position —
 *    only that the code paths run and do not throw.
 *  - "Fetches no network tiles" is not pinned here. It is pinned where it is
 *    decidable: `LocalAppManifestPolicyTest` (the app removes INTERNET) and
 *    `OfflineMapNetworkPolicyTest` (every URL the style can carry is a local
 *    file). See the latter's KDoc.
 */
class OfflineRouteMapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val repository = EntryPointAccessors
        .fromApplication(context.applicationContext, OfflineMapUiEntryPoint::class.java)
        .offlineMapRepository()
    private val mapsDirectory = File(context.filesDir, "offline_maps")
    private val stashDirectory = File(context.filesDir, "offline_maps-instrumentation-stash")

    /**
     * The repository is the app's real singleton over the app's real files, so
     * whatever the device already has imported is moved aside rather than
     * deleted — a developer running this on their own phone keeps their packs.
     */
    @Before
    fun stashInstalledLibrary() {
        stashDirectory.deleteRecursively()
        if (mapsDirectory.exists()) {
            assertTrue(
                "Could not move the installed offline map library aside.",
                mapsDirectory.renameTo(stashDirectory),
            )
        }
        repository.refresh()
    }

    @After
    fun restoreInstalledLibrary() {
        mapsDirectory.deleteRecursively()
        stashDirectory.renameTo(mapsDirectory)
        repository.refresh()
    }

    @Test
    fun withNoImportedPackNoMapViewIsBuiltAndNoRecenterControlIsOffered() {
        assertTrue(
            "The stash left an active pack behind; the fallback would not be under test.",
            repository.state.value.activeMapPacks.isEmpty(),
        )

        // showRecenterControl is deliberately on: the control is a property of
        // the map branch, so asking for it and still not getting one is what
        // proves the preview was drawn instead.
        setRouteMap(points = BERLIN_ROUTE, showRecenterControl = true)

        composeRule.onNodeWithContentDescription(recenterDescription).assertDoesNotExist()
        assertNull("A MapLibre map was built with no pack imported.", findMapLibreMapView())
        assertNull("A Mapsforge map was built with no pack imported.", findMapsforgeMapView())
    }

    @Test
    fun anImportedPackRendersAMapViewAndStillHidesTheRecenterControlByDefault() {
        installStubPmtilesPack()

        // showRecenterControl is left at its default.
        setRouteMap(points = BERLIN_ROUTE)

        assertNotNull(
            "The imported pack did not reach the composable, so nothing here is under test.",
            findMapLibreMapView(),
        )
        composeRule.onNodeWithContentDescription(recenterDescription).assertDoesNotExist()
    }

    @Test
    fun theRecenterControlRefitsTheCameraToTheRouteBounds() {
        installStubPmtilesPack()

        setRouteMap(
            points = BERLIN_ROUTE,
            currentPoint = BERLIN_ROUTE.last(),
            showRecenterControl = true,
        )
        // Without a ready map the tap is a no-op, and "it did not throw" would
        // mean nothing. Waiting for the map makes the tap reach fitCamera.
        awaitMapReady()

        composeRule.onNodeWithContentDescription(recenterDescription)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(recenterDescription).assertIsDisplayed()
    }

    @Test
    fun theRecenterControlHandlesASinglePointRoute() {
        // One fix is a zero-area bounding box, and MapLibre's LatLngBounds
        // refuses to be built from a single point — so recentering has to take
        // the newLatLngZoom branch instead. Getting that wrong throws out of the
        // FAB's onClick, which is a crash on the recording screen.
        installStubPmtilesPack()

        setRouteMap(points = BERLIN_ROUTE.take(1), showRecenterControl = true)
        awaitMapReady()

        composeRule.onNodeWithContentDescription(recenterDescription)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(recenterDescription).assertIsDisplayed()
    }

    private val recenterDescription: String
        get() = string(R.string.cd_recenter_map)

    private fun setRouteMap(
        points: List<ExerciseRoutePoint>,
        currentPoint: ExerciseRoutePoint? = null,
        showRecenterControl: Boolean = false,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                OfflineRouteMapOrPreview(
                    points = points,
                    currentPoint = currentPoint,
                    showRecenterControl = showRecenterControl,
                    modifier = Modifier.requiredSize(320.dp),
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * A pack the repository will accept and the renderer will never draw from.
     *
     * Only the metadata decides the branch — [OfflineMapMetadataStore] reads the
     * pack list back and keeps the entries whose file exists — so the file
     * contents only have to be present, not to be a real archive.
     */
    private fun installStubPmtilesPack() {
        mapsDirectory.mkdirs()
        File(mapsDirectory, "$STUB_PACK_ID.pmtiles").writeBytes(ByteArray(STUB_PACK_BYTES))
        File(mapsDirectory, "metadata.json").writeText(
            """
            {
              "activeFormat": "PMTILES",
              "packs": [
                {
                  "id": "$STUB_PACK_ID",
                  "displayName": "Instrumentation stub",
                  "originalFileName": "$STUB_PACK_ID.pmtiles",
                  "format": "PMTILES",
                  "sizeBytes": $STUB_PACK_BYTES,
                  "importedAtMillis": ${IMPORTED_AT.toEpochMilli()}
                }
              ]
            }
            """.trimIndent(),
        )
        repository.refresh()
        assertTrue(
            "The stub pack was not picked up by the repository.",
            repository.state.value.activeMapPacks.isNotEmpty(),
        )
    }

    /**
     * Blocks until MapLibre has handed the composable its map.
     *
     * `getMapAsync` fires immediately once the map exists, so registering a
     * second callback here is a readiness probe rather than a second map.
     */
    private fun awaitMapReady() {
        val mapView = requireNotNull(findMapLibreMapView()) {
            "No MapLibre map view was built, so there is nothing to recenter."
        }
        val ready = AtomicBoolean(false)
        instrumentation.runOnMainSync { mapView.getMapAsync { ready.set(true) } }
        composeRule.waitUntil(timeoutMillis = MapReadyTimeoutMillis) { ready.get() }
    }

    private fun findMapLibreMapView(): MapLibreMapView? =
        findViewOfType(MapLibreMapView::class.java)

    private fun findMapsforgeMapView(): MapsforgeMapView? =
        findViewOfType(MapsforgeMapView::class.java)

    /**
     * The rendered Android views, which is where a tile renderer would be: an
     * `AndroidView` leaves no trace in the semantics tree, so the composable's
     * two branches are only distinguishable from the real view hierarchy.
     */
    private fun <T : View> findViewOfType(type: Class<T>): T? {
        var found: T? = null
        instrumentation.runOnMainSync {
            found = resumedActivities()
                .asSequence()
                .mapNotNull { activity -> activity.window?.decorView?.firstDescendantOfType(type) }
                .firstOrNull()
        }
        return found
    }

    private fun resumedActivities(): Collection<Activity> =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)

    private fun <T : View> View.firstDescendantOfType(type: Class<T>): T? {
        if (type.isInstance(this)) return type.cast(this)
        val group = this as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            group.getChildAt(index).firstDescendantOfType(type)?.let { return it }
        }
        return null
    }

    private fun point(latitude: Double, longitude: Double, secondsIn: Long) = ExerciseRoutePoint(
        time = START.plusSeconds(secondsIn),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = null,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )

    private val BERLIN_ROUTE: List<ExerciseRoutePoint>
        get() = listOf(
            point(52.5200, 13.4050, 0),
            point(52.5205, 13.4062, 10),
            point(52.5210, 13.4075, 20),
        )

    private companion object {
        const val STUB_PACK_ID = "openvitals-instrumentation-stub"
        const val STUB_PACK_BYTES = 512
        const val MapReadyTimeoutMillis = 15_000L
        val START: Instant = Instant.parse("2026-06-23T08:00:00Z")
        val IMPORTED_AT: Instant = Instant.parse("2026-06-23T07:00:00Z")
    }
}
