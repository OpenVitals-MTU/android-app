// ChartScrubber lives under ui/charts/ but declares ui.components.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's `test/ui/charts/chart_scrubber_test.dart`.
 *
 * Which target a fraction lands on is pure and covered on the JVM by
 * `ChartScrubberTest`. What needs a device is the arbitration: a chart sits in
 * a scrolling page and has to tell a read from a scroll using nothing but the
 * first few pixels of a drag, and it has to get out of the way when a second
 * finger turns the gesture into a pinch.
 */
class ChartScrubberGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aVerticalDragStartingOnTheChart_stillScrollsThePage() {
        // Claiming every drag that begins on a chart would trap the page: a
        // thumb that happens to land on a chart could not scroll past it.
        val scrollState = ScrollState(0)
        var scrubbed: Int? = null
        setScrubber(scrollState = scrollState) { scrubbed = it }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, center)
            moveTo(0, center - Offset(0f, 400f))
            up(0)
        }
        composeRule.waitForIdle()

        assertTrue("the page must scroll", scrollState.value > 0)
        assertNull("and the chart must not read a value", scrubbed)
    }

    @Test
    fun aHorizontalDragReadsTheChart() {
        var scrubbed: Int? = null
        setScrubber { scrubbed = it }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, centerLeft + Offset(8f, 0f))
            moveTo(0, center)
            up(0)
        }
        composeRule.waitForIdle()

        assertEquals("a drag across the middle lands on the middle target", 1, scrubbed)
    }

    @Test
    fun itSnapsToTheNearestSampleNeverBetweenTwo() {
        var scrubbed: Int? = null
        setScrubber { scrubbed = it }

        // Deliberately between the first and second targets, nearer the first.
        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, centerLeft + Offset(8f, 0f))
            moveTo(0, Offset(width * 0.2f, center.y))
            up(0)
        }
        composeRule.waitForIdle()

        assertEquals(0, scrubbed)
    }

    @Test
    fun itStandsDownWhileAPinchIsInProgress() {
        // The pointer that began a one-finger scrub is already routed here and
        // cannot be taken back, so the scrubber has to withdraw itself once a
        // second finger makes the gesture a pinch.
        var scrubbed: Int? = null
        setScrubber(multiTouch = true) { scrubbed = it }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, centerLeft + Offset(8f, 0f))
            moveTo(0, center)
            up(0)
        }
        composeRule.waitForIdle()

        assertNull(scrubbed)
    }

    @Test
    fun aChartWithNothingToSayStaysInert() {
        var scrubbed: Int? = null
        setScrubber(targets = emptyList()) { scrubbed = it }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, centerLeft + Offset(8f, 0f))
            moveTo(0, center)
            up(0)
        }
        composeRule.waitForIdle()

        assertNull("no targets means no detector at all", scrubbed)
    }

    private fun setScrubber(
        targets: List<ScrubTarget> = THREE_TARGETS,
        multiTouch: Boolean = false,
        scrollState: ScrollState? = null,
        onScrub: (Int?) -> Unit,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                val chart: @Composable () -> Unit = {
                    ChartScrubber(
                        targets = targets,
                        accentColor = Color(0xFF4CAF50),
                        multiTouch = multiTouch,
                        onScrub = { if (it != null) onScrub(it) },
                        modifier = Modifier.testTag(CHART).fillMaxWidth().height(200.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(200.dp))
                    }
                }
                if (scrollState != null) {
                    Column(Modifier.verticalScroll(scrollState)) {
                        chart()
                        Spacer(Modifier.height(2_000.dp))
                    }
                } else {
                    chart()
                }
            }
        }
    }

    private companion object {
        const val CHART = "chart-scrubber-under-test"

        /** Left edge, middle, right edge — so a landing index is unambiguous. */
        val THREE_TARGETS = listOf(
            ScrubTarget(xFraction = 0f, yFraction = 0.5f, primary = "first"),
            ScrubTarget(xFraction = 0.5f, yFraction = 0.5f, primary = "middle"),
            ScrubTarget(xFraction = 1f, yFraction = 0.5f, primary = "last"),
        )
    }
}
