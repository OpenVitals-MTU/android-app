package tech.mmarca.openvitals.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Rect
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
 * Asserts the scaled screen drew something and lost none of its text. Three ways
 * text is lost at a large font scale: it runs off the side (a taller screen scrolls,
 * a wider label is unreadable), it runs past the bottom of a box with a set height,
 * or the text itself is cut or, on a single line, shortened with an ellipsis.
 * The minimum-content half stops a screen that composed nothing from passing.
 * Inside a scroller the window narrows to that container.
 */
fun ComposeTestRule.assertScaledScreenFits(
    minTextNodes: Int = 3,
    toleranceDp: Float = 2f,
) {
    waitForIdle()
    val surface = onNodeWithTag(TextScaleRootTag, useUnmergedTree = true).fetchSemanticsNode()

    val offenders = mutableListOf<String>()
    val textNodes = mutableListOf<Pair<String, SemanticsNode>>()
    // Density is pinned at 1 in TextScaleSurface, so a dp is a pixel here.
    fun visit(node: SemanticsNode, window: Rect, scrolled: Boolean, container: Rect?) {
        val bounds = Rect(node.positionInRoot, node.size.toSize())
        // Scrolled out of its container entirely: off screen on purpose.
        if (scrolled && (bounds.right <= window.left || bounds.left >= window.right ||
                bounds.bottom <= window.top || bounds.top >= window.bottom)
        ) {
            return
        }

        val text = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            ?.takeIf { it.isNotBlank() }
        if (text != null) {
            textNodes += text to node
            if (bounds.right > window.right + toleranceDp) {
                offenders += "\"$text\" runs ${(bounds.right - window.right).roundToInt()}dp past the right edge"
            }
            if (bounds.left < window.left - toleranceDp) {
                offenders += "\"$text\" starts ${(window.left - bounds.left).roundToInt()}dp left of the surface"
            }
            if (container != null && bounds.bottom > container.bottom + toleranceDp) {
                offenders += "\"$text\" runs ${(bounds.bottom - container.bottom).roundToInt()}dp past the bottom of its box"
            }
        }

        val horizontal = node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
        val vertical = node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null
        val scroller = horizontal || vertical
        val childWindow = Rect(
            left = if (horizontal) maxOf(window.left, bounds.left) else window.left,
            top = if (vertical) maxOf(window.top, bounds.top) else window.top,
            right = if (horizontal) minOf(window.right, bounds.right) else window.right,
            bottom = if (vertical) minOf(window.bottom, bounds.bottom) else window.bottom,
        )
        // A scroller's children may sit partly outside it; a plain box's may not.
        val childContainer = if (scroller) null else bounds
        node.children.forEach { visit(it, childWindow, scrolled || scroller, childContainer) }
    }
    visit(
        node = surface,
        window = Rect(surface.positionInRoot, surface.size.toSize()),
        scrolled = false,
        container = null,
    )

    // Text that did not fit its own bounds, from the layout the screen drew.
    runOnUiThread {
        textNodes.forEach { (text, node) ->
            val layout = node.textLayoutResult() ?: return@forEach
            val ellipsized = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
            when {
                // A one-line value or label lost its end. A trimmed description keeps its start.
                ellipsized -> if (layout.lineCount == 1) offenders += "\"$text\" is shortened with an ellipsis"
                layout.hasVisualOverflow -> offenders += "\"$text\" is cut off"
            }
        }
    }

    assertTrue(
        "The screen rendered ${textNodes.size} pieces of text at ${LargestSystemFontScale}x " +
            "font scale; expected at least $minTextNodes",
        textNodes.size >= minTextNodes,
    )
    assertTrue(
        "Text does not fit the screen at ${LargestSystemFontScale}x font scale:\n" +
            offenders.joinToString("\n"),
        offenders.isEmpty(),
    )
}

private fun SemanticsNode.textLayoutResult(): TextLayoutResult? {
    val results = mutableListOf<TextLayoutResult>()
    val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
    return if (action(results)) results.firstOrNull() else null
}
