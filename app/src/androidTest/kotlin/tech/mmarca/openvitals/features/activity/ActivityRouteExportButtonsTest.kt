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

/** Open-in-map is offered exactly when a route exists; an empty track renders as a blank map. */
class ActivityRouteExportButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aRouteOffersOpenInMap() {
        val pressed = mutableListOf<String>()
        setCard(route = routeWithPoints(), pressed = pressed)

        composeRule.onNodeWithText(string(R.string.activity_route_open_in_map))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf("open"), pressed)
    }

    @Test
    fun aWorkoutWithoutARouteOffersNoMapOpening() {
        setCard(route = ExerciseRouteData(status = ExerciseRouteStatus.NO_DATA))

        composeRule.onNodeWithText(string(R.string.activity_route_open_in_map)).assertDoesNotExist()
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
                    )
                }
            }
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-07-10T08:00:00Z")
    }
}
