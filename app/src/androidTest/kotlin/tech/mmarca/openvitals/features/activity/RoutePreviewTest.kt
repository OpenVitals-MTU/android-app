package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The route-drawing half of Flutter's
 * `test/features/activity/maps/route_map_view_test.dart`.
 *
 * With no offline map pack imported — the state of any phone that has not gone
 * looking for one — this preview is what the app actually draws for a recorded
 * route. It projects latitudes onto a canvas, so the degenerate routes are the
 * dangerous ones: a route with no span divides by zero, and a session whose
 * route failed to import has none at all. Either one crashes the activity
 * detail screen, not just the map.
 */
class RoutePreviewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aRecordedRouteDraws() {
        setPreview(
            listOf(
                point(52.5200, 13.4050, 0),
                point(52.5205, 13.4062, 10),
                point(52.5210, 13.4075, 20),
            ),
        )

        composeRule.onNodeWithTag(TAG).assertExists()
    }

    @Test
    fun anEmptyRouteIsHandledGracefully() {
        setPreview(emptyList())

        composeRule.onNodeWithTag(TAG).assertExists()
    }

    @Test
    fun aSinglePointRouteIsHandledGracefully() {
        // One fix means a zero-wide bounding box; projecting into it is a
        // division by zero unless the span is guarded.
        setPreview(listOf(point(52.5200, 13.4050, 0)))

        composeRule.onNodeWithTag(TAG).assertExists()
    }

    private fun setPreview(points: List<ExerciseRoutePoint>) {
        composeRule.setContent {
            OpenVitalsTheme {
                RoutePreview(
                    points = points,
                    modifier = Modifier
                        .size(240.dp)
                        .testTag(TAG),
                )
            }
        }
    }

    private fun point(latitude: Double, longitude: Double, secondsIn: Long) = ExerciseRoutePoint(
        time = START.plusSeconds(secondsIn),
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = null,
        horizontalAccuracyMeters = null,
        verticalAccuracyMeters = null,
    )

    private companion object {
        const val TAG = "route-preview"
        val START: Instant = Instant.parse("2026-06-23T08:00:00Z")
    }
}
