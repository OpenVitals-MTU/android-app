// ChartZoom lives under ui/charts/ but declares ui.components; mirrored here.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the `ChartZoom` cases of Flutter's `test/ui/charts/chart_zoom_test.dart`.
 *
 * The viewport arithmetic itself is covered on the JVM by `ChartViewportTest`.
 * What only a device can answer is whether the gestures reach it: whether two
 * fingers are recognised as a pinch, whether one finger is refused as one, and
 * whether either of those survives being inside a scrolling page — which is
 * where a chart actually lives, and where the gesture arena gets to have an
 * opinion.
 */
class ChartZoomGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoFingersPinchingApart_zoomsIn() {
        var viewport = ChartViewport.Full
        setChart { viewport = it.viewport }

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        assertTrue("a pinch outwards must narrow the viewport", viewport.isZoomed)
    }

    @Test
    fun twoFingersZoom_evenInsideAScrollingPage() {
        // A chart is always inside a scrollable. If the scroll container wins the
        // arena on the first finger, the pinch never arrives and zoom is dead in
        // the only place it is ever used.
        var viewport = ChartViewport.Full
        setChart(inScrollingPage = true) { viewport = it.viewport }

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        assertTrue(viewport.isZoomed)
    }

    @Test
    fun oneFingerDraggingHorizontally_doesNotZoom() {
        // One finger across a chart is a scrub, not a zoom. Treating it as a pan
        // would move the data out from under a reading the user is taking.
        var viewport = ChartViewport.Full
        var sawMultiTouch = false
        setChart {
            viewport = it.viewport
            if (it.multiTouch) sawMultiTouch = true
        }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, centerLeft + Offset(20f, 0f))
            moveTo(0, centerRight - Offset(20f, 0f))
            up(0)
        }
        composeRule.waitForIdle()

        assertFalse("one finger is not a pinch", viewport.isZoomed)
        assertFalse("one finger must not raise multiTouch", sawMultiTouch)
    }

    @Test
    fun thePageStillScrolls_whenDraggedFromInsideAChart() {
        // The chart claiming every vertical drag would trap the page: a user who
        // starts a scroll with their thumb over a chart gets a stuck screen.
        val scrollState = androidx.compose.foundation.ScrollState(0)
        setChart(inScrollingPage = true, scrollState = scrollState) {}

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, center)
            moveTo(0, center - Offset(0f, 400f))
            up(0)
        }
        composeRule.waitForIdle()

        assertTrue("the page must still scroll under a vertical drag", scrollState.value > 0)
    }

    @Test
    fun doubleTap_returnsTheWholeChart() {
        var viewport = ChartViewport.Full
        setChart { viewport = it.viewport }

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()
        assertTrue("precondition: the chart is zoomed in", viewport.isZoomed)

        composeRule.onNodeWithTag(CHART).performTouchInput {
            down(0, center); up(0)
            down(0, center); up(0)
        }
        composeRule.waitForIdle()

        assertEquals(ChartViewport.Full, viewport)
    }

    @Test
    fun aPinchStillLands_whenOneFingerMovesBeforeTheOther() {
        // Real fingers do not land together. If the detector treats the first
        // finger's movement as a committed one-finger gesture before the second
        // arrives, pinching works in a test harness and fails on a phone.
        var viewport = ChartViewport.Full
        setChart { viewport = it.viewport }

        composeRule.onNodeWithTag(CHART).performTouchInput {
            val left = center - Offset(40f, 0f)
            val right = center + Offset(40f, 0f)
            down(0, left)
            moveTo(0, left - Offset(15f, 0f))
            down(1, right)
            repeat(6) { step ->
                val spread = 40f * (step + 1)
                moveTo(0, left - Offset(spread, 0f))
                moveTo(1, right + Offset(spread, 0f))
            }
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        assertTrue(viewport.isZoomed)
    }

    @Test
    fun changingTheChartsKey_resetsTheZoomRatherThanCarryingItOver() {
        // Paging to another year hands the same chart a different set of data.
        // Keeping the old viewport would show a window onto a range that no
        // longer exists, framing an arbitrary slice of the new one.
        var viewport = ChartViewport.Full
        var key by androidx.compose.runtime.mutableStateOf(2025)

        composeRule.setContent {
            OpenVitalsTheme {
                ChartZoom(key, modifier = Modifier.testTag(CHART).fillMaxWidth().height(200.dp)) { zoom ->
                    viewport = zoom.viewport
                    Box(Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()
        assertTrue("precondition: zoomed", viewport.isZoomed)

        composeRule.runOnIdle { key = 2026 }
        composeRule.waitForIdle()

        assertEquals(ChartViewport.Full, viewport)
    }

    /** Two fingers starting near the centre and moving apart. */
    private fun androidx.compose.ui.test.TouchInjectionScope.pinchApart() {
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

    private fun setChart(
        inScrollingPage: Boolean = false,
        scrollState: androidx.compose.foundation.ScrollState? = null,
        onZoom: (ChartZoomState) -> Unit,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                val chart: @Composable () -> Unit = {
                    ChartZoom(Unit, modifier = Modifier.testTag(CHART).fillMaxWidth().height(200.dp)) { zoom ->
                        onZoom(zoom)
                        Box(Modifier.fillMaxWidth().height(200.dp))
                    }
                }
                if (inScrollingPage) {
                    val state = scrollState ?: rememberScrollState()
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(state)) {
                        chart()
                        // Enough below the chart that the page has somewhere to go.
                        Spacer(Modifier.height(2_000.dp))
                    }
                } else {
                    chart()
                }
            }
        }
    }

    private companion object {
        const val CHART = "chart-zoom-under-test"
    }
}
