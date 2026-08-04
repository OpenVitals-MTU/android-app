package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's `test/features/bodyenergy/body_energy_chart_zoom_test.dart`.
 *
 * The Body Energy timeline is a line PLUS an influence strip PLUS an hour row,
 * and all three ride one viewport. That sharing is the whole point: a strip that
 * kept the day's full scale while the line stretched would sit the bars under
 * hours they did not happen in, so the card would explain the curve with the
 * wrong causes — worse than not zooming at all.
 *
 * The viewport arithmetic is covered on the JVM by `ChartViewportTest`; what only
 * a device answers is whether a pinch on this particular chart reaches it, and
 * whether the hour row follows.
 */
class BodyEnergyChartZoomTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinchingTheChartZoomsTheLineStripAndHoursTogether() {
        composeRule.setContent {
            OpenVitalsTheme {
                BodyEnergyTimelineChart(
                    points = (0..24).map {
                        BodyEnergyChartPoint(xFraction = it / 24f, score = 50.0 + (it % 5) * 5.0)
                    },
                    influenceBars = (0 until 24).map {
                        BodyEnergyInfluenceBar(
                            xFraction = it / 24f,
                            widthFraction = 1f / 24f,
                            charge = 0.0,
                            drain = (it % 3).toDouble(),
                            influence = BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
                        )
                    },
                    maxMagnitude = 3.0,
                    modifier = Modifier.testTag(CHART).fillMaxWidth(),
                )
            }
        }

        // Before: the whole day is on show, so the row still ends at midnight.
        composeRule.onNodeWithText("24:00").assertIsDisplayed()

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        // After: only a slice of the day remains under the plot, so neither end
        // of the day is on the row any more. An hour row that still read
        // 00:00 … 24:00 would be describing a plot that no longer shows it.
        composeRule.onNodeWithText("24:00").assertDoesNotExist()
        composeRule.onNodeWithText("00:00").assertDoesNotExist()
    }

    /** Two fingers starting near the centre and moving apart. */
    private fun TouchInjectionScope.pinchApart() {
        val left = center - Offset(40f, 0f)
        val right = center + Offset(40f, 0f)
        down(0, left)
        down(1, right)
        // Several steps rather than one jump: the detector accumulates its scale
        // across moves, and a single teleport is not what a real pinch looks like.
        repeat(6) { step ->
            val spread = 40f * (step + 1)
            moveTo(0, left - Offset(spread, 0f))
            moveTo(1, right + Offset(spread, 0f))
        }
        up(0)
        up(1)
    }

    private companion object {
        const val CHART = "body-energy-timeline-chart"
    }
}
