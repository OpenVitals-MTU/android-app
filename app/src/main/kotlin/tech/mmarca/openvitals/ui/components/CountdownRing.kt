package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.isActive
import tech.mmarca.openvitals.ui.theme.LocalReducedMotion

/**
 * A ring that empties clockwise as a countdown runs out, with the digits in
 * the middle. Decorative for accessibility: the digits carry the value.
 * Driven by wall-clock time against [endsAt], so it sweeps smoothly; under
 * reduced motion it steps with [now]. A null [endsAt] holds the ring full.
 */
@Composable
fun CountdownRing(
    endsAt: Instant?,
    totalMillis: Long,
    now: Instant,
    modifier: Modifier = Modifier,
    size: Dp = CountdownRingDefaults.Size,
    strokeWidth: Dp = CountdownRingDefaults.StrokeWidth,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val reducedMotion = LocalReducedMotion.current
    val endMillis = endsAt?.toEpochMilli()
    val smooth = endMillis != null && !reducedMotion
    var frameClockMillis by remember { mutableLongStateOf(now.toEpochMilli()) }

    if (smooth) {
        LaunchedEffect(endMillis) {
            while (isActive) {
                withFrameMillis { frameClockMillis = System.currentTimeMillis() }
                if (frameClockMillis >= endMillis!!) break
            }
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset(
                x = (this.size.width - diameter) / 2f,
                y = (this.size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            // Read in the draw phase, so each frame redraws without recomposing the content.
            val clock = if (smooth) frameClockMillis else now.toEpochMilli()
            val fraction = countdownRemainingFraction(endMillis, totalMillis, clock)
            if (fraction > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** 1 while not running, 0 once [endMillis] has passed, the remaining share between. */
internal fun countdownRemainingFraction(endMillis: Long?, totalMillis: Long, clockMillis: Long): Float {
    if (endMillis == null) return 1f
    if (totalMillis <= 0L) return 0f
    return ((endMillis - clockMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
}

object CountdownRingDefaults {
    /** The summary ring's diameter: big enough to read across a room. */
    val Size: Dp = 168.dp
    val StrokeWidth: Dp = 12.dp

    /** The hero of a screen read from a metre away: the plan runner. */
    val LargeSize: Dp = 224.dp
    val LargeStrokeWidth: Dp = 14.dp

    /** Inline beside a line of text. */
    val CompactSize: Dp = 40.dp
    val CompactStrokeWidth: Dp = 5.dp
}
