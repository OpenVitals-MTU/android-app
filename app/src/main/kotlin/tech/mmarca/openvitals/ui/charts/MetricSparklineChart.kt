package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MetricSparklineChart(
    values: List<Double>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    minValue: Double? = null,
    baselineFraction: Float = 0.75f,
    baselineAlpha: Float = 0.22f,
    verticalScaleFraction: Float = 0.72f,
    topPaddingFraction: Float = 0.14f,
    lineStrokeWidth: Dp = 4.dp,
    pointRadius: Dp = 3.dp,
    pointStrokeWidth: Dp? = null,
    pointFillRadius: Dp? = null,
    singlePointLine: Boolean = false,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val resolvedMin = minValue ?: 0.0
        val range = (maxValue - resolvedMin).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width / 2f
        val points = values.mapIndexed { index, value ->
            val yFraction = ((value - resolvedMin) / range).toFloat().coerceIn(0f, 1f)
            Offset(
                x = if (values.size > 1) index * stepX else stepX,
                y = size.height - (yFraction * (size.height * verticalScaleFraction)) -
                    (size.height * topPaddingFraction),
            )
        }

        drawLine(
            color = accentColor.copy(alpha = baselineAlpha),
            start = Offset(0f, size.height * baselineFraction),
            end = Offset(size.width, size.height * baselineFraction),
            strokeWidth = 2.dp.toPx(),
        )

        val strokeWidthPx = lineStrokeWidth.toPx()
        if (singlePointLine && points.size == 1) {
            drawLine(
                color = accentColor,
                start = Offset(strokeWidthPx / 2f, points.first().y),
                end = Offset(size.width - strokeWidthPx / 2f, points.first().y),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        } else {
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = accentColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
            }
        }

        points.forEach { point ->
            pointStrokeWidth?.let { stroke ->
                drawCircle(
                    color = accentColor,
                    radius = pointRadius.toPx(),
                    center = point,
                    style = Stroke(width = stroke.toPx()),
                )
            }
            drawCircle(
                color = accentColor,
                radius = (pointFillRadius ?: pointRadius).toPx(),
                center = point,
            )
        }
    }
}
