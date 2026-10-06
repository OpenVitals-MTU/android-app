package tech.mmarca.openvitals.wear.features.recording

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.HeartZoneColors

private const val StartAngle = 135f
private const val SweepAngle = 270f
private const val GapAngle = 3f

/**
 * The five heart rate zones as an arc along the screen edge, the current
 * zone lit and a dot at [progress] (0 at the start of zone 1, 1 at the
 * maximum). Null [zone] leaves every segment dimmed.
 */
@Composable
fun HeartZoneArc(zone: Int?, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val segment = SweepAngle / HeartZoneColors.size
        HeartZoneColors.forEachIndexed { index, color ->
            drawArc(
                color = if (index == zone) color else color.copy(alpha = Emphasis.subtle),
                startAngle = StartAngle + index * segment + GapAngle / 2,
                sweepAngle = segment - GapAngle,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (zone != null) {
            val angle = Math.toRadians((StartAngle + SweepAngle * progress).toDouble())
            val radius = arcSize.width / 2
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                color = HeartZoneColors[zone],
                radius = stroke * 0.9f,
                center = Offset(
                    center.x + (radius * cos(angle)).toFloat(),
                    center.y + (radius * sin(angle)).toFloat(),
                ),
            )
        }
    }
}
