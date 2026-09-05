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

/** The largest font scale Android offers. It is one slider away for every user. */
const val LargestSystemFontScale = 2f

/**
 * A phone-sized surface with density pinned at 1 (a 393x852 box on every device)
 * and the font scale turned up, so everything that moves, moved because of the text.
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
 * Asserts the scaled screen drew something and none of it ran off the side.
 * Width has no way out: a taller screen scrolls, a wider label is unreadable.
 * The minimum-content half stops a screen that composed nothing from passing.
 * Inside a horizontal scroller the window narrows to that container.
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
