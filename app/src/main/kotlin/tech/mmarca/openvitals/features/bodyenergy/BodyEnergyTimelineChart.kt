package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.YAxisChart
import tech.mmarca.openvitals.ui.components.chartYAxisLabels
import tech.mmarca.openvitals.ui.components.drawYAxisGuides

@Composable
internal fun BodyEnergyTimelineChart(
    points: List<BodyEnergyChartPoint>,
    influenceBars: List<BodyEnergyInfluenceBar>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = lineColor.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val noDataColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
    val lineHeight = 172.dp
    val barHeight = 44.dp
    Column(modifier = modifier.fillMaxWidth()) {
        YAxisChart(
            labels = chartYAxisLabels(
                minValue = 0.0,
                maxValue = 100.0,
                valueFormatter = { it.roundToInt().toString() },
            ),
            chartHeight = lineHeight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            drawYAxisGuides(
                gridColor = gridColor,
                axisColor = axisColor,
                strokeWidth = 1.dp.toPx(),
            )
            drawBodyEnergyLine(
                points = points,
                color = lineColor,
                lineStrokeWidth = 2.5.dp.toPx(),
                pointRadius = 3.dp.toPx(),
                drawPoints = points.size <= 40,
            )
        }
        ChartXAxisWithYAxis(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        ) {
            BodyEnergyInfluenceBars(
                bars = influenceBars,
                axisColor = axisColor,
                noDataColor = noDataColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
            )
        }
    }
}

@Composable
private fun BodyEnergyInfluenceBars(
    bars: List<BodyEnergyInfluenceBar>,
    axisColor: Color,
    noDataColor: Color,
    modifier: Modifier = Modifier,
) {
    val sleepColor = bodyEnergyInfluenceColor(BodyEnergyPrimaryInfluence.SLEEP_RECOVERY)
    val restColor = bodyEnergyInfluenceColor(BodyEnergyPrimaryInfluence.QUIET_REST)
    val exertionColor = bodyEnergyInfluenceColor(BodyEnergyPrimaryInfluence.EXERTION)
    val elevatedHeartRateColor = bodyEnergyInfluenceColor(BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE)
    val recoveryDebtColor = bodyEnergyInfluenceColor(BodyEnergyPrimaryInfluence.RECOVERY_DEBT)
    Canvas(modifier = modifier) {
        if (bars.isEmpty()) return@Canvas
        val centerY = size.height / 2f
        drawLine(
            color = axisColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
        )
        val maxMagnitude = bars
            .maxOfOrNull { max(it.charge, it.drain) }
            ?.takeIf { it > 0.0 }
            ?: 1.0
        val minBarWidth = 2.dp.toPx()
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        bars.forEach { bar ->
            val x = size.width * bar.xFraction.coerceIn(0f, 1f)
            val width = max(minBarWidth, size.width * bar.widthFraction * 0.82f)
            val left = (x - width / 2f).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
            val color = when (bar.influence) {
                BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> sleepColor
                BodyEnergyPrimaryInfluence.QUIET_REST -> restColor
                BodyEnergyPrimaryInfluence.EXERTION -> exertionColor
                BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> elevatedHeartRateColor
                BodyEnergyPrimaryInfluence.RECOVERY_DEBT -> recoveryDebtColor
                BodyEnergyPrimaryInfluence.NO_DATA,
                BodyEnergyPrimaryInfluence.STEADY -> noDataColor
            }
            if (bar.charge > 0.0) {
                val height = ((bar.charge / maxMagnitude).toFloat() * centerY).coerceAtLeast(1.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, centerY - height),
                    size = Size(width, height),
                    cornerRadius = cornerRadius,
                )
            }
            if (bar.drain > 0.0) {
                val height = ((bar.drain / maxMagnitude).toFloat() * centerY).coerceAtLeast(1.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, centerY),
                    size = Size(width, height),
                    cornerRadius = cornerRadius,
                )
            }
            if (bar.charge <= 0.0 && bar.drain <= 0.0 && bar.influence == BodyEnergyPrimaryInfluence.NO_DATA) {
                drawLine(
                    color = noDataColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = minBarWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawBodyEnergyLine(
    points: List<BodyEnergyChartPoint>,
    color: Color,
    lineStrokeWidth: Float,
    pointRadius: Float,
    drawPoints: Boolean,
) {
    if (points.isEmpty()) return
    val rawPoints = points.map { point ->
        Offset(
            x = size.width * point.xFraction.coerceIn(0f, 1f),
            y = size.height * (1f - (point.score / 100.0).toFloat().coerceIn(0f, 1f)),
        )
    }
    // Scores are integers (0..100) sampled per bucket, so the raw series is a
    // staircase. Damp that quantization with a small moving average before
    // splining, otherwise the curve just traces the steps and reads as jagged.
    val positionedPoints = movingAverageY(rawPoints)
    if (positionedPoints.size >= 3) {
        drawPath(
            path = smoothLinePath(positionedPoints, maxY = size.height),
            color = color,
            style = Stroke(
                width = lineStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    } else {
        for (index in 0 until positionedPoints.lastIndex) {
            drawLine(
                color = color,
                start = positionedPoints[index],
                end = positionedPoints[index + 1],
                strokeWidth = lineStrokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
    if (drawPoints) {
        positionedPoints.forEach { point ->
            drawCircle(
                color = color,
                radius = pointRadius,
                center = point,
            )
        }
    }
}

/**
 * Smooths the Y series with a centered moving average, keeping each point's X.
 * The window radius scales with point count (wider for denser series) so the
 * integer-quantized score staircase becomes a flowing line before splining.
 */
private fun movingAverageY(pts: List<Offset>): List<Offset> {
    if (pts.size < 3) return pts
    val radius = (pts.size / 16).coerceIn(1, 4)
    return pts.mapIndexed { index, point ->
        val from = (index - radius).coerceAtLeast(0)
        val to = (index + radius).coerceAtMost(pts.lastIndex)
        var sum = 0f
        for (i in from..to) sum += pts[i].y
        point.copy(y = sum / (to - from + 1))
    }
}

/**
 * Builds a smooth curve through [pts] using a Catmull-Rom spline converted to
 * cubic Bézier segments. Control-point Y is clamped to [0, maxY] so the eased
 * curve can never overshoot past the chart's 0/100 bounds.
 */
private fun smoothLinePath(pts: List<Offset>, maxY: Float): Path {
    val path = Path()
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.lastIndex) {
        val p0 = pts[if (i == 0) 0 else i - 1]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[if (i + 2 <= pts.lastIndex) i + 2 else pts.lastIndex]
        val control1X = p1.x + (p2.x - p0.x) / 6f
        val control1Y = (p1.y + (p2.y - p0.y) / 6f).coerceIn(0f, maxY)
        val control2X = p2.x - (p3.x - p1.x) / 6f
        val control2Y = (p2.y - (p3.y - p1.y) / 6f).coerceIn(0f, maxY)
        path.cubicTo(control1X, control1Y, control2X, control2Y, p2.x, p2.y)
    }
    return path
}

@Composable
internal fun bodyEnergyInfluenceColor(influence: BodyEnergyPrimaryInfluence): Color =
    when (influence) {
        // Recovery / charge — cool hues, clearly separated from the warm drain set.
        BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> StepsColor // green
        BodyEnergyPrimaryInfluence.QUIET_REST -> WorkoutColor // cyan
        // Drain / decrease — warm hues, spread across the warm arc so each is distinct.
        BodyEnergyPrimaryInfluence.EXERTION -> CaloriesColor // red
        BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> FloorsColor // amber
        BodyEnergyPrimaryInfluence.RECOVERY_DEBT -> HeartColor // magenta
        // Neutral / absent — low-emphasis greys.
        BodyEnergyPrimaryInfluence.NO_DATA -> MaterialTheme.colorScheme.outline
        BodyEnergyPrimaryInfluence.STEADY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
