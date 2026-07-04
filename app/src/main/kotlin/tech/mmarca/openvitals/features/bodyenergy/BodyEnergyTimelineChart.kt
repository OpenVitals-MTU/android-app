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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
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
    val positionedPoints = points.map { point ->
        Offset(
            x = size.width * point.xFraction.coerceIn(0f, 1f),
            y = size.height * (1f - (point.score / 100.0).toFloat().coerceIn(0f, 1f)),
        )
    }
    for (index in 0 until positionedPoints.lastIndex) {
        drawLine(
            color = color,
            start = positionedPoints[index],
            end = positionedPoints[index + 1],
            strokeWidth = lineStrokeWidth,
            cap = StrokeCap.Round,
        )
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

@Composable
internal fun bodyEnergyInfluenceColor(influence: BodyEnergyPrimaryInfluence): Color =
    when (influence) {
        BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> MaterialTheme.colorScheme.primary
        BodyEnergyPrimaryInfluence.QUIET_REST -> MaterialTheme.colorScheme.tertiary
        BodyEnergyPrimaryInfluence.EXERTION -> MaterialTheme.colorScheme.error
        BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> MaterialTheme.colorScheme.secondary
        BodyEnergyPrimaryInfluence.RECOVERY_DEBT -> MaterialTheme.colorScheme.errorContainer
        BodyEnergyPrimaryInfluence.NO_DATA -> MaterialTheme.colorScheme.outline
        BodyEnergyPrimaryInfluence.STEADY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
