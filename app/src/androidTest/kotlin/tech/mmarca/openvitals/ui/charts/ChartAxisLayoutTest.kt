// The axis composables live under ui/charts/ but declare ui.components.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Where the row is drawn: a chart with a y-axis gutter starts 64dp into its card, and a
 * label row starting at the card's edge names the wrong moment at every tick.
 */
class ChartAxisLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayAxisLabels_startWhereThePlotStarts_notWhereTheCardDoes() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.fillMaxWidth()) {
                    ChartXAxisWithYAxis { DayAxisLabels() }
                }
            }
        }

        val first = composeRule.onNodeWithText(FIRST_TICK).getUnclippedBoundsInRoot()
        assertTrue(
            "the first tick must be inset past the y-axis gutter, was ${first.left}",
            first.left >= ChartYAxisWidth,
        )
    }

    @Test
    fun dayAxisLabels_aPainterWithNoYAxisCanOptOut() {
        // An edge-to-edge chart gets an edge-to-edge row; the inset is the wrapper's, opt-in.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.fillMaxWidth()) { DayAxisLabels() }
            }
        }

        val first = composeRule.onNodeWithText(FIRST_TICK).getUnclippedBoundsInRoot()
        assertTrue(
            "without the wrapper the row starts at the edge, was ${first.left}",
            first.left < ChartYAxisWidth,
        )
    }

    @Test
    fun sessionAxisLabels_startWhereThePlotStarts() {
        val start = Instant.parse("2026-06-23T08:00:00Z")
        val axis = SessionAxis(start = start, end = start.plus(Duration.ofMinutes(45)))

        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.fillMaxWidth()) {
                    ChartXAxisWithYAxis { SessionAxisLabels(axis = axis) }
                }
            }
        }

        val labels = axis.elapsedLabelsFor()
        val first = composeRule.onNodeWithText(labels.first()).getUnclippedBoundsInRoot()
        assertTrue(
            "the elapsed row is inset like the day row, was ${first.left}",
            first.left >= ChartYAxisWidth,
        )
        composeRule.onNodeWithText(labels.last()).assertIsDisplayed()
    }

    private companion object {
        /** `dayAxisLabelsFor` at full zoom starts the day at midnight. */
        const val FIRST_TICK = "00:00"
    }
}
