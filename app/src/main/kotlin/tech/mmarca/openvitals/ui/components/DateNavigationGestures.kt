package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val DateNavigationSwipeThresholdFraction = 0.25f
private val MinDateNavigationSwipeThreshold = 40.dp
private val MaxDateNavigationSwipeThreshold = 96.dp

internal fun Modifier.dateNavigationSwipe(
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier = pointerInput(canGoForward, onPrevious, onNext) {
    var horizontalDrag = 0f

    detectHorizontalDragGestures(
        onDragStart = { horizontalDrag = 0f },
        onDragCancel = { horizontalDrag = 0f },
        onDragEnd = {
            val threshold = (size.width * DateNavigationSwipeThresholdFraction)
                .coerceIn(
                    MinDateNavigationSwipeThreshold.toPx(),
                    MaxDateNavigationSwipeThreshold.toPx(),
                )

            when {
                horizontalDrag >= threshold -> onPrevious()
                horizontalDrag <= -threshold && canGoForward -> onNext()
            }

            horizontalDrag = 0f
        },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            horizontalDrag += dragAmount
        },
    )
}
