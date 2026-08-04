package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's
 * `test/features/activity/activity_route_export_buttons_test.dart`.
 *
 * Three actions with three different intents, none a substitute for another:
 * open-in-map hands the route to a map app, save raises the system file picker,
 * share offers messengers and mail. Losing one of them is the only copy of that
 * route the user can never get out of the app — Health Connect has no export.
 *
 * Where Flutter drops the whole card for a routeless workout, Kotlin keeps the
 * card and says there is no route in it; what both guarantee is that no export
 * action is offered for a route that does not exist.
 */
class ActivityRouteExportButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aRouteOffersOpenInMapAndBothSaveAndShareFormats() {
        val pressed = mutableListOf<String>()
        setCard(route = routeWithPoints(), pressed = pressed)

        listOf(
            R.string.activity_route_open_in_map,
            R.string.activity_route_export_gpx,
            R.string.activity_route_export_kmz,
            R.string.activity_route_share_gpx,
            R.string.activity_route_share_kmz,
        ).forEach { actionRes ->
            composeRule.onNodeWithText(string(actionRes)).performScrollTo().assertIsDisplayed()
        }

        composeRule.onNodeWithText(string(R.string.activity_route_share_kmz)).performScrollTo().performClick()

        assertEquals(listOf("share-kmz"), pressed)
    }

    @Test
    fun aWorkoutWithoutARouteOffersNoExportAtAll() {
        setCard(route = ExerciseRouteData(status = ExerciseRouteStatus.NO_DATA))

        listOf(
            R.string.activity_route_open_in_map,
            R.string.activity_route_export_gpx,
            R.string.activity_route_export_kmz,
            R.string.activity_route_share_gpx,
            R.string.activity_route_share_kmz,
        ).forEach { actionRes ->
            composeRule.onNodeWithText(string(actionRes)).assertDoesNotExist()
        }
        composeRule.onNodeWithText(string(R.string.message_no_route_data)).performScrollTo().assertIsDisplayed()
    }

    private fun routeWithPoints() = ExerciseRouteData(
        status = ExerciseRouteStatus.DATA,
        points = List(3) { index ->
            ExerciseRoutePoint(
                time = START.plus(Duration.ofMinutes(index.toLong())),
                latitude = 59.43 + index * 0.001,
                longitude = 24.75 + index * 0.001,
                altitudeMeters = null,
                horizontalAccuracyMeters = null,
                verticalAccuracyMeters = null,
            )
        },
    )

    private fun setCard(route: ExerciseRouteData, pressed: MutableList<String> = mutableListOf()) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    RouteCard(
                        route = route,
                        unitFormatter = testUnitFormatter(),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onOpenRouteInMap = { pressed += "open" },
                        onSaveRouteAsGpx = { pressed += "save-gpx" },
                        onSaveRouteAsKmz = { pressed += "save-kmz" },
                        onShareRouteAsGpx = { pressed += "share-gpx" },
                        onShareRouteAsKmz = { pressed += "share-kmz" },
                    )
                }
            }
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-07-10T08:00:00Z")
    }
}
