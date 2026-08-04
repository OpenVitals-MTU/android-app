package tech.mmarca.openvitals.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The tagged surface every scaled screen is rendered into. */
const val TextScaleRootTag = "openvitals_text_scale_root"

/**
 * The largest font scale Android's accessibility settings offer.
 *
 * Not a stress value: it is one slider away for every user, and a health app
 * skews towards people who run it. A screen that only reads at 1.0 is a screen
 * those users cannot use.
 */
const val LargestSystemFontScale = 2f

/**
 * A phone-sized surface with the density pinned and the font scale turned up.
 *
 * Density is fixed at 1 so the surface is the same 393x852 box on every device —
 * a layout that only holds on a large phone is not a layout that holds. The font
 * scale is the only thing that varies, which is the point: everything that moves
 * between 1.0 and 2.0 moved because of the text.
 */
@Composable
fun TextScaleSurface(
    fontScale: Float = LargestSystemFontScale,
    width: Dp = 393.dp,
    height: Dp = 852.dp,
    content: @Composable () -> Unit,
) {
    OpenVitalsTheme(dynamicColor = false) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 1f, fontScale = fontScale),
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(width, height)
                    .testTag(TextScaleRootTag),
            ) {
                content()
            }
        }
    }
}

/**
 * Asserts the scaled screen drew something, and that none of it ran off the side.
 *
 * Width is the axis with no way out. A screen that grows taller at 2.0 scrolls;
 * a label that grows wider than the phone is simply unreadable, and it is
 * invisible at 1.0 where the same label fits with room to spare.
 *
 * The minimum-content half is there so the width half cannot pass for the wrong
 * reason: a screen that threw its content away under the larger metrics — or
 * never composed at all — overflows nothing, and would otherwise read as a pass.
 *
 * Inside a horizontally scrollable container the window narrows to that
 * container, and anything parked entirely outside it is left alone — the
 * carousel page waiting off to the right is not an overflow, it is the carousel.
 */
fun ComposeTestRule.assertScaledScreenFitsItsWidth(
    minTextNodes: Int = 3,
    toleranceDp: Float = 2f,
) {
    waitForIdle()
    val surface = onNodeWithTag(TextScaleRootTag, useUnmergedTree = true).fetchSemanticsNode()

    val offenders = mutableListOf<String>()
    var textNodes = 0
    // Density is pinned at 1 in TextScaleSurface, so a dp is a pixel here.
    fun visit(node: SemanticsNode, windowLeft: Float, windowRight: Float, scrolled: Boolean) {
        val left = node.positionInRoot.x
        val right = left + node.size.width
        // Scrolled out of its container entirely: off screen on purpose.
        if (scrolled && (right <= windowLeft || left >= windowRight)) return

        val text = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            ?.takeIf { it.isNotBlank() }
        if (text != null) {
            textNodes += 1
            if (right > windowRight + toleranceDp) {
                offenders += "\"$text\" runs ${(right - windowRight).roundToInt()}dp past the right edge"
            }
            if (left < windowLeft - toleranceDp) {
                offenders += "\"$text\" starts ${(windowLeft - left).roundToInt()}dp left of the surface"
            }
        }

        val scroller = node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
        val childLeft = if (scroller) maxOf(windowLeft, left) else windowLeft
        val childRight = if (scroller) minOf(windowRight, right) else windowRight
        node.children.forEach { visit(it, childLeft, childRight, scrolled || scroller) }
    }
    visit(
        node = surface,
        windowLeft = surface.positionInRoot.x,
        windowRight = surface.positionInRoot.x + surface.size.width,
        scrolled = false,
    )

    assertTrue(
        "The screen rendered $textNodes pieces of text at ${LargestSystemFontScale}x " +
            "font scale; expected at least $minTextNodes",
        textNodes >= minTextNodes,
    )
    assertTrue(
        "Text does not fit the screen at ${LargestSystemFontScale}x font scale:\n" +
            offenders.joinToString("\n"),
        offenders.isEmpty(),
    )
}
