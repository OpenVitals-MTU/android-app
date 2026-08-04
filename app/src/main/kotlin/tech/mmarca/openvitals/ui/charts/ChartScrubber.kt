package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas as DrawCanvas

/** A point a scrub can land on, in the plot's own fraction space. Sorted by [xFraction]. */
@Immutable
data class ScrubTarget(
    val xFraction: Float,
    val yFraction: Float,
    val primary: String,
    val secondary: String? = null,
)

/**
 * The target nearest [fraction] by x, which is what a finger means.
 *
 * Snapping to the nearest SAMPLE rather than reading the curve at the finger's exact x
 * is deliberate: the curve between two samples is an interpolation the app invented, and
 * a tooltip must only ever report a number that was actually measured. Strict `<` keeps
 * the first of two equally near targets.
 */
fun nearestScrubTargetIndex(targets: List<ScrubTarget>, fraction: Float): Int? {
    if (targets.isEmpty()) return null
    var best = 0
    var bestDistance = Float.POSITIVE_INFINITY
    for (i in targets.indices) {
        val distance = abs(targets[i].xFraction - fraction)
        if (distance < bestDistance) {
            bestDistance = distance
            best = i
        }
    }
    return best
}

/**
 * Drag across a chart to read it: a crosshair on the landed sample and a tooltip with
 * the value, drawn over the untouched [content].
 *
 * ## The gesture, which is the whole difficulty
 *
 * Every one of these charts lives inside a vertically scrolling screen. A detector that
 * claimed both axes the moment a drag starts inside it would freeze the page under the
 * thumb of a user trying to scroll — not a subtle regression, the app not working. So
 * this uses [detectHorizontalDragGestures] only (the same choice as the sleep
 * hypnogram): a vertical drag is left to the scrolling parent, a horizontal one comes
 * here.
 *
 * [multiTouch] is [ChartZoom]'s pinch flag. A pinch is a zoom, not a read: the finger
 * that started this scrub is already routed here and cannot be handed back, so when a
 * second finger lands the scrubber clears its crosshair and ignores further drags,
 * leaving the two-finger gesture to the zoom.
 */
@Composable
fun ChartScrubber(
    targets: List<ScrubTarget>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiTouch: Boolean = false,
    onScrub: ((Int?) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!enabled || targets.isEmpty()) {
        Box(modifier) { content() }
        return
    }

    var index by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val currentTargets by rememberUpdatedState(targets)
    val currentMultiTouch by rememberUpdatedState(multiTouch)
    val currentOnScrub by rememberUpdatedState(onScrub)

    fun land(x: Float, width: Int) {
        // A pinch is in progress: the drag is the zoom's, not ours.
        if (currentMultiTouch) return
        val targetList = currentTargets
        if (targetList.isEmpty() || width <= 0) return
        val fraction = (x / width).coerceIn(0f, 1f)
        val best = nearestScrubTargetIndex(targetList, fraction) ?: return
        if (best == index) return
        index = best
        // Haptic ONLY when the landed sample changes, so a slow drag ticks per sample
        // rather than buzzing continuously.
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentOnScrub?.invoke(best)
    }

    fun lift() {
        if (index == null) return
        index = null
        currentOnScrub?.invoke(null)
    }

    // The index is ours, but the list it points into is the CALLER'S, and it can change
    // underneath us: zooming the chart shortens it to the points still on screen. An
    // index held over from the longer list reads off the end of the shorter one. So a
    // scrub that no longer refers to anything is dropped — and so is one interrupted by
    // a pinch. Everything that ever indexes another composable's list has this bug
    // waiting in it.
    LaunchedEffect(targets.size, multiTouch) {
        val held = index
        if (held != null && (multiTouch || held >= targets.size)) {
            index = null
            currentOnScrub?.invoke(null)
        }
    }
    val active = index?.takeIf { !multiTouch && it < targets.size }

    Box(
        modifier = modifier.pointerInput(Unit) {
            // HORIZONTAL only. See the doc above: a pan detector would claim the
            // vertical axis too and freeze the page this chart is sitting on.
            detectHorizontalDragGestures(
                onDragStart = { offset -> land(offset.x, size.width) },
                onDragEnd = { lift() },
                onDragCancel = { lift() },
            ) { change, _ ->
                change.consume()
                land(change.position.x, size.width)
            }
        },
    ) {
        content()
        if (active != null) {
            val target = targets[active]
            ScrubCrosshair(target = target, accentColor = accentColor)
            ScrubTooltip(target = target)
        }
    }
}

@Composable
private fun BoxScope.ScrubCrosshair(
    target: ScrubTarget,
    accentColor: Color,
) {
    val crosshairColor = ChartTokens.crosshair
    DrawCanvas(modifier = Modifier.matchParentSize()) {
        val x = target.xFraction.coerceIn(0f, 1f) * size.width
        val y = (1f - target.yFraction.coerceIn(0f, 1f)) * size.height

        drawLine(
            color = crosshairColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        // A ring, not a dot: a filled dot in the accent colour is indistinguishable
        // from the sample dots the plot already draws.
        drawCircle(color = accentColor, radius = 5.dp.toPx(), center = Offset(x, y))
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** The value, floated above the landed sample and kept inside the plot. */
@Composable
private fun BoxScope.ScrubTooltip(target: ScrubTarget) {
    val tooltipSurface = ChartTokens.tooltipSurface
    val onTooltipSurface = ChartTokens.onTooltipSurface

    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
        val plotWidthPx = constraints.maxWidth.toFloat()
        val tooltipWidthPx = with(LocalDensity.current) { TooltipWidth.toPx() }
        val x = target.xFraction.coerceIn(0f, 1f) * plotWidthPx
        // Clamped to the plot: a tooltip that hangs off the card is a tooltip you
        // cannot read, and the samples at the very start and end of a day are exactly
        // the ones a user scrubs to first.
        val left = (x - tooltipWidthPx / 2f)
            .coerceIn(0f, (plotWidthPx - tooltipWidthPx).coerceAtLeast(0f))

        Column(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), 0) }
                .width(TooltipWidth)
                .background(tooltipSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = target.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = onTooltipSurface,
            )
            target.secondary?.let { secondary ->
                Text(
                    text = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = onTooltipSurface.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private val TooltipWidth = 132.dp
