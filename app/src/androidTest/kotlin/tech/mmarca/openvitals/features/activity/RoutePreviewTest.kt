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
 * With no map pack this preview is what the app draws for a route. It projects onto a canvas,
 * so a route with no span divides by zero and one with no points crashes the detail screen.
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
        // One fix is a zero-wide bounding box; projecting into it divides by zero unless guarded.
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
