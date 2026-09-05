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
 * The line, the influence strip and the hour row ride one viewport. A strip that kept the
 * day's full scale would explain the curve with the wrong causes.
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

        // After: only a slice remains under the plot, so neither end of the day is on the row.
        composeRule.onNodeWithText("24:00").assertDoesNotExist()
        composeRule.onNodeWithText("00:00").assertDoesNotExist()
    }

    /** Two fingers starting near the centre and moving apart. */
    private fun TouchInjectionScope.pinchApart() {
        val left = center - Offset(40f, 0f)
        val right = center + Offset(40f, 0f)
        down(0, left)
        down(1, right)
        // Several steps rather than one jump: the detector accumulates its scale across moves.
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
