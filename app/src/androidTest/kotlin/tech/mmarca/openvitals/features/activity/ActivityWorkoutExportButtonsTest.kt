package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The metric formats (TCX, CSV, FIT) are unconditional; the route formats (GPX, KMZ and a FIT
 * with the route) appear exactly when a route does, each group under its own heading.
 */
class ActivityWorkoutExportButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWorkoutWithARouteOffersEveryFormat() {
        val pressed = mutableListOf<String>()
        setCard(withRoute = true, pressed = pressed)

        listOf(
            R.string.activity_workout_export_with_route,
            R.string.activity_workout_export_without_route,
            R.string.activity_route_export_gpx,
            R.string.activity_route_export_kmz,
            R.string.activity_route_share_gpx,
            R.string.activity_route_share_kmz,
            R.string.activity_workout_export_tcx,
            R.string.activity_workout_export_csv,
            R.string.activity_workout_share_tcx,
            R.string.activity_workout_share_csv,
        ).forEach { actionRes ->
            composeRule.onNodeWithText(string(actionRes)).performScrollTo().assertIsDisplayed()
        }
        // FIT sits in both groups: the route's first, then the routeless one.
        composeRule.onAllNodesWithText(string(R.string.activity_workout_export_fit)).assertCountEquals(2)
        val shareFit = composeRule.onAllNodesWithText(string(R.string.activity_workout_share_fit))
        shareFit.assertCountEquals(2)

        composeRule.onNodeWithText(string(R.string.activity_route_share_kmz)).performScrollTo().performClick()
        shareFit[0].performScrollTo().performClick()
        shareFit[1].performScrollTo().performClick()

        assertEquals(listOf("share-kmz", "share-route-fit", "share-fit"), pressed)
    }

    @Test
    fun aRoutelessWorkoutOffersOnlyTheMetricFormats() {
        val pressed = mutableListOf<String>()
        setCard(withRoute = false, pressed = pressed)

        listOf(
            R.string.activity_workout_export_with_route,
            R.string.activity_workout_export_without_route,
            R.string.activity_route_export_gpx,
            R.string.activity_route_export_kmz,
            R.string.activity_route_share_gpx,
            R.string.activity_route_share_kmz,
        ).forEach { actionRes ->
            composeRule.onNodeWithText(string(actionRes)).assertDoesNotExist()
        }
        listOf(
            R.string.activity_workout_export_tcx,
            R.string.activity_workout_export_csv,
            R.string.activity_workout_export_fit,
            R.string.activity_workout_share_tcx,
            R.string.activity_workout_share_csv,
            R.string.activity_workout_share_fit,
        ).forEach { actionRes ->
            composeRule.onNodeWithText(string(actionRes)).performScrollTo().assertIsDisplayed()
        }

        composeRule.onNodeWithText(string(R.string.activity_workout_share_csv)).performScrollTo().performClick()

        assertEquals(listOf("share-csv"), pressed)
    }

    private fun setCard(withRoute: Boolean, pressed: MutableList<String>) {
        fun action(tag: String): () -> Unit = { pressed += tag }
        fun routeAction(tag: String): (() -> Unit)? = if (withRoute) action(tag) else null
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    WorkoutExportCard(
                        onSaveAsTcx = action("save-tcx"),
                        onSaveAsCsv = action("save-csv"),
                        onSaveAsFit = action("save-fit"),
                        onShareAsTcx = action("share-tcx"),
                        onShareAsCsv = action("share-csv"),
                        onShareAsFit = action("share-fit"),
                        onSaveRouteAsGpx = routeAction("save-gpx"),
                        onSaveRouteAsKmz = routeAction("save-kmz"),
                        onShareRouteAsGpx = routeAction("share-gpx"),
                        onShareRouteAsKmz = routeAction("share-kmz"),
                        onSaveRouteAsFit = routeAction("save-route-fit"),
                        onShareRouteAsFit = routeAction("share-route-fit"),
                    )
                }
            }
        }
    }
}
