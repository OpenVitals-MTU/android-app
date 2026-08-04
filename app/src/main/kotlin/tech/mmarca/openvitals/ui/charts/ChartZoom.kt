package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** What [ChartZoom] hands its content: the visible slice, and whether a pinch is live. */
@Immutable
data class ChartZoomState(
    val viewport: ChartViewport,
    /**
     * True while a second finger is down — a pinch, not a scrub. The [ChartScrubber]
     * stands down off this flag: the pointer that started a one-finger scrub is already
     * routed to it and cannot be taken back, so the scrubber has to hide itself rather
     * than be hit-tested away.
     */
    val multiTouch: Boolean,
)

/**
 * Pinch a chart to look closer at part of it.
 *
 * **Two fingers, and only two.** This is not a style choice, it is what keeps the rest
 * of the chart working. A chart sits inside a scrolling page and already claims the
 * single-finger horizontal drag for the scrubber ([ChartScrubber]) while leaving the
 * vertical one to the page. A zoom gesture that accepted one finger would have to fight
 * both: it would either eat the scrub, or freeze the page under the user's thumb.
 *
 * So this composable watches the raw pointer stream and consumes position changes ONLY
 * while two or more pointers are down. Consuming in the Main pass — where this node, as
 * a descendant, sees the event before the scrolling parent — is what lets a two-finger
 * gesture win against the parent list, while a single finger is never consumed and
 * behaves exactly as it always has: scrub horizontally, scroll the page vertically, tap
 * a bar to select its day. Nothing that worked before this composable existed behaves
 * any differently.
 *
 * Double tap resets — spotted from the raw pointers for the same reason: a tap detector
 * of its own would hold the tap for its double-tap window and swallow the bar chart's
 * day-selecting tap. A chart you have zoomed into and cannot get out of is worse than
 * one that never zoomed.
 *
 * [keys] name what the chart is ABOUT (metric, period): when they change the viewport
 * resets to the whole chart, because a zoom into last Tuesday means nothing on a
 * different week.
 */
@Composable
fun ChartZoom(
    vararg keys: Any?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (ChartZoomState) -> Unit,
) {
    if (!enabled) {
        // Nothing to zoom into — an empty state, a skeleton, or a heatmap, whose cells
        // are a grid rather than an axis.
        Box(modifier) { content(ChartZoomState(ChartViewport.Full, multiTouch = false)) }
        return
    }

    var viewport by remember(*keys) { mutableStateOf(ChartViewport.Full) }
    var multiTouch by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(*keys) {
            val tapSlopPx = TapSlop.toPx()
            // When and where the last single finger lifted, for spotting a double tap
            // ourselves. Survives across gestures, which is what a double tap is.
            var lastTapAtMillis = 0L
            var lastTapPosition: Offset? = null

            awaitEachGesture {
                // Where each finger currently is, by pointer id.
                val pointers = LinkedHashMap<PointerId, Offset>()

                // The state the pinch started from. Every move is applied to THIS rather
                // than to the last frame's result: compounding frame by frame accumulates
                // the rounding, and a slow pinch would visibly drift.
                var pinchStartViewport: ChartViewport? = null
                var pinchStartSeparation = 0f
                var pinchStartFocus = 0f

                // Whether a pinch has actually happened during this touch, as opposed to
                // two fingers merely resting.
                var isPinching = false
                var downPosition: Offset? = null

                // The fingers' HORIZONTAL separation is the zoom. A pinch that is mostly
                // vertical does almost nothing, which is right — the y axis of these
                // charts is a fixed scale of the thing being measured, and stretching it
                // would only misrepresent it.
                fun separation(): Float {
                    var minX = Float.POSITIVE_INFINITY
                    var maxX = Float.NEGATIVE_INFINITY
                    for (position in pointers.values) {
                        minX = min(minX, position.x)
                        maxX = max(maxX, position.x)
                    }
                    return maxX - minX
                }

                fun focusX(): Float {
                    var sum = 0f
                    for (position in pointers.values) sum += position.x
                    return sum / pointers.size
                }

                // Rebaselines whenever the number of fingers changes, so lifting one of
                // three, or adding a second, does not make the chart leap.
                fun restartPinch() {
                    if (pointers.isEmpty()) isPinching = false
                    if (pointers.size < 2) {
                        pinchStartViewport = null
                        return
                    }
                    val width = size.width.toFloat()
                    pinchStartViewport = viewport
                    pinchStartSeparation = separation()
                    pinchStartFocus =
                        if (width <= 0f) 0.5f else (focusX() / width).coerceIn(0f, 1f)
                }

                // A double tap, worked out from the raw pointers rather than asked of a
                // detector, so it can take nothing away from the taps underneath.
                fun maybeDoubleTap(position: Offset, uptimeMillis: Long) {
                    val down = downPosition ?: return
                    if ((position - down).getDistance() > tapSlopPx) return

                    val lastPosition = lastTapPosition
                    if (lastPosition != null &&
                        uptimeMillis - lastTapAtMillis < DoubleTapWindowMillis &&
                        (position - lastPosition).getDistance() <= tapSlopPx
                    ) {
                        lastTapPosition = null
                        if (viewport.isZoomed) viewport = ChartViewport.Full
                        return
                    }
                    lastTapAtMillis = uptimeMillis
                    lastTapPosition = position
                }

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            when {
                                change.changedToDownIgnoreConsumed() -> {
                                    pointers[change.id] = change.position
                                    downPosition = change.position
                                    restartPinch()
                                }

                                change.changedToUpIgnoreConsumed() -> {
                                    // Only a lift that ended a single-finger, still
                                    // gesture counts as a tap: a finger coming off a
                                    // pinch is not a tap, and must not reset the zoom
                                    // the user just set.
                                    if (pointers.size == 1 && !isPinching) {
                                        maybeDoubleTap(change.position, change.uptimeMillis)
                                    }
                                    pointers.remove(change.id)
                                    restartPinch()
                                }

                                change.pressed -> {
                                    if (pointers.containsKey(change.id)) {
                                        pointers[change.id] = change.position
                                    }
                                }
                            }
                        }

                        val startViewport = pinchStartViewport
                        if (startViewport != null && pointers.size >= 2 && size.width > 0) {
                            val separationNow = separation()
                            if (separationNow > 0f && pinchStartSeparation > 0f) {
                                val scale = separationNow / pinchStartSeparation
                                val focus = focusX() / size.width.toFloat()

                                // Zoom about the point BETWEEN the fingers so the chart
                                // stretches under them rather than jumping, then slide by
                                // however far that point has travelled — which is what
                                // turns a two-finger drag into a pan.
                                val next = startViewport
                                    .zoomed(scale, pinchStartFocus)
                                    .panned(focus - pinchStartFocus)
                                if (next != viewport) {
                                    isPinching = true
                                    viewport = next
                                }
                            }
                        }

                        // Consume ONLY while two or more fingers are down: that beats the
                        // scrolling parent for the pinch without ever claiming the single
                        // finger the scrubber, the page and the taps depend on.
                        if (pointers.size >= 2) {
                            for (change in event.changes) {
                                if (change.positionChanged()) change.consume()
                            }
                        }

                        multiTouch = pointers.size >= 2
                        if (event.changes.none { it.pressed }) break
                    }
                } finally {
                    // The gesture can be torn down mid-pinch (cancellation, key change);
                    // the flag must not stay latched or the scrubber never comes back.
                    multiTouch = false
                }
            }
        },
    ) {
        content(ChartZoomState(viewport, multiTouch))
    }
}

private const val DoubleTapWindowMillis = 300L
private val TapSlop = 18.dp
