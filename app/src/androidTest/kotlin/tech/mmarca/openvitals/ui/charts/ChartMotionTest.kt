// ChartSkeleton and ChartReveal live under ui/charts/ but declare ui.components.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.testing.AnimatorScaleRule
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Two animations and the switch that governs them. The skeleton repeats, which can hang
 * an idle-wait forever; the reveal runs once. Reduce-motion is the accessibility contract,
 * and Kotlin reads the real system scale, so [AnimatorScaleRule] moves the real setting.
 */
class ChartMotionTest {

    @get:Rule(order = 0)
    val animatorScale = AnimatorScaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun theSkeletonSettlesWhenMotionIsOff() {
        animatorScale.motion(enabled = false)

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(SKELETON)) {
                    ChartSkeleton(shape = ChartSkeletonShape.BARS)
                }
            }
        }

        // If the repeat were still running this would never return.
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SKELETON).assertExists()
    }

    @Test
    fun theSkeletonDoesAnimateWhenMotionIsOn() {
        animatorScale.motion(enabled = true)
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(SKELETON)) { ChartSkeleton() }
            }
        }

        // A repeating animation never goes idle, so two frames far apart in its pulse must differ.
        composeRule.mainClock.advanceTimeByFrame()
        val first = composeRule.onNodeWithTag(SKELETON).captureToImage().asAndroidBitmap()
        composeRule.mainClock.advanceTimeBy(400)
        val later = composeRule.onNodeWithTag(SKELETON).captureToImage().asAndroidBitmap()

        assertTrue("the skeleton must pulse when motion is on", first.differsFrom(later))
    }

    @Test
    fun theRevealIsFullyDrawnOnTheFirstFrameWhenMotionIsOff() {
        animatorScale.motion(enabled = false)
        val seen = mutableListOf<Float>()

        composeRule.setContent {
            OpenVitalsTheme {
                ChartReveal { progress ->
                    seen += progress
                    Box(Modifier.height(10.dp))
                }
            }
        }
        composeRule.waitForIdle()

        // Not zero-then-one: one immediately.
        assertEquals(1f, seen.first(), 0f)
        assertEquals(1f, seen.last(), 0f)
    }

    @Test
    fun theRevealAnimatesFromNothingWhenMotionIsOn() {
        animatorScale.motion(enabled = true)
        composeRule.mainClock.autoAdvance = false
        val seen = mutableListOf<Float>()

        composeRule.setContent {
            OpenVitalsTheme {
                ChartReveal { progress ->
                    seen += progress
                    Box(Modifier.height(10.dp))
                }
            }
        }

        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("it starts from nothing", 0f, seen.first(), 0f)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals("and runs to completion", 1f, seen.last(), 0f)
        assertTrue("through intermediate frames, not a jump", seen.size > 2)
    }

    /** True when any pixel differs — the skeleton pulses alpha, nothing else. */
    private fun android.graphics.Bitmap.differsFrom(other: android.graphics.Bitmap): Boolean {
        if (width != other.width || height != other.height) return true
        val mine = IntArray(width * height)
        val theirs = IntArray(width * height)
        getPixels(mine, 0, width, 0, 0, width, height)
        other.getPixels(theirs, 0, width, 0, 0, width, height)
        return !mine.contentEquals(theirs)
    }

    private companion object {
        const val SKELETON = "chart-skeleton-under-test"
    }
}
