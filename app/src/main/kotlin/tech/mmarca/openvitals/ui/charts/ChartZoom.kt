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
    /** True while a second finger is down. The scrubber hides itself off this flag. */
    val multiTouch: Boolean,
)

/**
 * Pinch a chart to look closer. Two fingers only: one finger already
 * belongs to the scrubber and the page. Position changes are consumed in
 * the Main pass only while two pointers are down, so a single finger
 * behaves as before. Double tap resets, spotted from raw pointers so the
 * bar chart's tap is not swallowed. [keys] reset the viewport when the
 * chart's subject changes.
 */
@Composable
fun ChartZoom(
    vararg keys: Any?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (ChartZoomState) -> Unit,
) {
    if (!enabled) {
        // Nothing to zoom into.
        Box(modifier) { content(ChartZoomState(ChartViewport.Full, multiTouch = false)) }
        return
    }

    var viewport by remember(*keys) { mutableStateOf(ChartViewport.Full) }
    var multiTouch by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(*keys) {
            val tapSlopPx = TapSlop.toPx()
            // The last single-finger lift, for spotting a double tap.
            var lastTapAtMillis = 0L
            var lastTapPosition: Offset? = null

            awaitEachGesture {
                // Where each finger currently is, by pointer id.
                val pointers = LinkedHashMap<PointerId, Offset>()

                // The state the pinch started from. Applying moves to it avoids drift.
                var pinchStartViewport: ChartViewport? = null
                var pinchStartSeparation = 0f
                var pinchStartFocus = 0f

                // Whether a pinch has happened, as opposed to two fingers resting.
                var isPinching = false
                var downPosition: Offset? = null

                // Horizontal separation is the zoom: the y axis is a fixed scale.
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

                // Rebaseline when the finger count changes, so the chart does not leap.
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

                // A double tap from raw pointers, so the taps underneath keep working.
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
                                    // Only a single-finger, still gesture counts as a tap.
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

                                // Zoom about the point between the fingers, then slide
                                // by how far it travelled: a two-finger drag pans.
                                val next = startViewport
                                    .zoomed(scale, pinchStartFocus)
                                    .panned(focus - pinchStartFocus)
                                if (next != viewport) {
                                    isPinching = true
                                    viewport = next
                                }
                            }
                        }

                        // Consume only with two or more fingers down.
                        if (pointers.size >= 2) {
                            for (change in event.changes) {
                                if (change.positionChanged()) change.consume()
                            }
                        }

                        multiTouch = pointers.size >= 2
                        if (event.changes.none { it.pressed }) break
                    }
                } finally {
                    // The gesture can end mid-pinch; the flag must not stay latched.
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
