package tech.mmarca.openvitals.ui.components

import tech.mmarca.openvitals.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.res.stringResource
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

/**
 * A point a scrub can land on, in the plot's own fraction space. Sorted by [xFraction].
 *
 * The labels are made when they are first read. A plot builds a target for every sample
 * on show, up to 2,500 of them, on every pinch frame, and the tooltip reads the labels of
 * one. Formatting them all cost two number formats per sample per frame.
 */
@Immutable
class ScrubTarget(
    val xFraction: Float,
    val yFraction: Float,
    label: () -> Pair<String, String?>,
) {
    constructor(xFraction: Float, yFraction: Float, primary: String, secondary: String? = null) :
        this(xFraction, yFraction, { primary to secondary })

    private val labels by lazy(LazyThreadSafetyMode.NONE, label)
    val primary: String get() = labels.first
    val secondary: String? get() = labels.second
}

/**
 * The target nearest [fraction] by x. Snaps to a sample, never the
 * interpolated curve: a tooltip only reports measured numbers.
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
 * Drag across a chart to read it: a crosshair on the landed sample and a
 * tooltip. Horizontal drags only, so the page still scrolls. [multiTouch]
 * is the zoom's pinch flag: a second finger clears the crosshair.
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
        // A pinch is in progress.
        if (currentMultiTouch) return
        val targetList = currentTargets
        if (targetList.isEmpty() || width <= 0) return
        val fraction = (x / width).coerceIn(0f, 1f)
        val best = nearestScrubTargetIndex(targetList, fraction) ?: return
        if (best == index) return
        index = best
        // Haptic only when the landed sample changes.
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentOnScrub?.invoke(best)
    }

    fun lift() {
        if (index == null) return
        index = null
        currentOnScrub?.invoke(null)
    }

    // A drag is touch-only. A screen reader steps through the samples instead.
    fun step(delta: Int): Boolean {
        val targetList = currentTargets
        if (targetList.isEmpty()) return false
        val next = ((index ?: -1) + delta).coerceIn(0, targetList.lastIndex)
        if (next == index) return false
        index = next
        currentOnScrub?.invoke(next)
        return true
    }
    val nextLabel = stringResource(R.string.chart_next_point)
    val previousLabel = stringResource(R.string.chart_previous_point)

    // The list is the caller's and can shrink under a zoom, so a stale index
    // or a pinch-interrupted scrub is dropped.
    LaunchedEffect(targets.size, multiTouch) {
        val held = index
        if (held != null && (multiTouch || held >= targets.size)) {
            index = null
            currentOnScrub?.invoke(null)
        }
    }
    val active = index?.takeIf { !multiTouch && it < targets.size }

    Box(
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(nextLabel) { step(1) },
                    CustomAccessibilityAction(previousLabel) { step(-1) },
                )
            }
            .pointerInput(Unit) {
            // Horizontal only, or the page would freeze.
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
        // A ring, not a dot, so it differs from the sample dots.
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
        // Clamped to the plot: a tooltip off the card cannot be read.
        val left = (x - tooltipWidthPx / 2f)
            .coerceIn(0f, (plotWidthPx - tooltipWidthPx).coerceAtLeast(0f))

        Column(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), 0) }
                .width(TooltipWidth)
                .background(tooltipSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                // Stepped to by a screen reader, the new value is read out.
                .semantics { liveRegion = LiveRegionMode.Polite },
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
