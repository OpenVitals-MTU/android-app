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
 * The map branch of the route view. [OfflineRouteMapOrPreview] chooses the renderer from
 * [OfflineMapRepository.state], so this class moves the installed library aside, writes
 * its own metadata plus a stub pack, and calls [OfflineMapRepository.refresh].
 * The pack is a stub: these tests cover the branch and the recenter control, not rendering.
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

    /** The repository is the app's real singleton, so installed packs are moved aside, not deleted. */
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

        // showRecenterControl is on: asking for the control and not getting one proves the preview was drawn.
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
        // Without a ready map the tap is a no-op. Waiting makes it reach fitCamera.
        awaitMapReady()

        composeRule.onNodeWithContentDescription(recenterDescription)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(recenterDescription).assertIsDisplayed()
    }

    @Test
    fun theRecenterControlHandlesASinglePointRoute() {
        // One fix is a zero-area box, and LatLngBounds refuses a single point, so recentering
        // must take the newLatLngZoom branch. Getting it wrong throws out of the FAB's onClick.
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

    /** A pack the repository accepts and the renderer never draws from. Only the metadata decides the branch. */
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

    /** Blocks until MapLibre has handed the composable its map. `getMapAsync` fires immediately once it exists. */
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

    /** The rendered Android views. An `AndroidView` leaves no trace in the semantics tree. */
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
