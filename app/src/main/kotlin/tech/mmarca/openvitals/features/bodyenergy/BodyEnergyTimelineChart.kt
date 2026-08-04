package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.ChartViewport
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.DayAxisLabels
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.movingAverageY
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

private const val MinutesPerDay = 24 * 60

/**
 * The Body Energy day timeline: the shared [MetricLinePlot] over a 0-100 score,
 * a charge/drain influence strip beneath it, and the hour row under both.
 *
 * The line used to be drawn here by hand — its own spline, its own grid, and no
 * hour axis at all, so a card that could tell you the score could not tell you
 * WHEN. All three now sit inside one [ChartZoom] and share the one viewport: a
 * strip whose bars sat under the wrong stretch of the curve they explain, or an
 * hour row that disagreed with either, would be worse than not zooming at all.
 *
 * Everything drawn arrives precomputed on [BodyEnergyDisplayState] — the bucket
 * fractions, the bar magnitudes and the strip's scale.
 */
@Composable
internal fun BodyEnergyTimelineChart(
    points: List<BodyEnergyChartPoint>,
    influenceBars: List<BodyEnergyInfluenceBar>,
    maxMagnitude: Double,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val noDataColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
    val influenceColors = bodyEnergyInfluenceColors()
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    // Scores are integers (0..100) sampled per bucket, so the raw series is a
    // staircase. Damp that quantization with the shared moving average before it
    // reaches the plot, otherwise the curve traces the steps and reads as
    // jagged. A DATA decision, which is why it happens here and not in the
    // painter.
    val plotPoints = remember(points) {
        movingAverageY(points.map { Offset(it.xFraction, it.score.toFloat()) })
            .map { MetricLinePlotPoint(xFraction = it.x, value = it.y.toDouble()) }
    }

    ChartZoom(points, enabled = points.isNotEmpty(), modifier = modifier.fillMaxWidth()) { zoom ->
        Column(modifier = Modifier.fillMaxWidth()) {
            MetricLinePlot(
                points = plotPoints,
                // A score DEFINED as 0 to 100. Deliberately unpadded: padding
                // would invent headroom above a ceiling and depth below a floor,
                // and a 0-100 score that draws itself off its own scale lies.
                minValue = 0.0,
                maxValue = 100.0,
                accentColor = accentColor,
                chartHeight = ChartTokens.heightBodyEnergy,
                valueFormatter = { it.roundToInt().toString() },
                lineStrokeWidth = 2.5.dp,
                drawPoints = points.size <= 40,
                viewport = zoom.viewport,
                multiTouch = zoom.multiTouch,
                // The score points carry no timestamp of their own — only where
                // they sit across the day — which is all the tooltip needs.
                scrubLabel = { point ->
                    point.value.roundToInt().toString() to clockAt(point.xFraction, timeFormatter)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ChartXAxisWithYAxis(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                BodyEnergyInfluenceBars(
                    bars = influenceBars,
                    maxMagnitude = maxMagnitude,
                    axisColor = axisColor,
                    noDataColor = noDataColor,
                    colors = influenceColors,
                    viewport = zoom.viewport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ChartTokens.heightInfluenceStrip),
                )
            }
            // The plot has a y-axis gutter, so the hour row insets to match it —
            // the line's x, the bar's x and the row's 12:00 all come from the
            // same fraction of the same day.
            ChartXAxisWithYAxis(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                DayAxisLabels(viewport = zoom.viewport, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BodyEnergyInfluenceBars(
    bars: List<BodyEnergyInfluenceBar>,
    maxMagnitude: Double,
    axisColor: Color,
    noDataColor: Color,
    colors: Map<BodyEnergyPrimaryInfluence, Color>,
    modifier: Modifier = Modifier,
    viewport: ChartViewport = ChartViewport.Full,
) {
    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        drawLine(
            color = axisColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
        )
        if (bars.isEmpty()) return@Canvas
        val scale = maxMagnitude.takeIf { it > 0.0 } ?: 1.0
        val minBarWidth = 2.dp.toPx()
        // Bars ride the SAME viewport as the score line above: position through
        // it, widen by the zoom factor, and clip rather than clamp so a bar
        // half-off the edge stays honestly half-off.
        val body: DrawScope.() -> Unit = {
            bars.forEach { bar ->
                val visibleFraction = viewport.visibleFraction(bar.xFraction.coerceIn(0f, 1f))
                // Cheap cull: a bar well outside the window contributes nothing.
                if (visibleFraction < -0.05f || visibleFraction > 1.05f) return@forEach
                val x = size.width * visibleFraction
                // The bar keeps its share of the DAY, so it grows as the day is
                // stretched: its plot width is that share divided by the visible
                // span.
                val width = (size.width * (bar.widthFraction / viewport.span) * 0.82f)
                    .coerceIn(minBarWidth, size.width)
                val left = x - width / 2f
                // The one bar-corner rule, rather than this strip's own flat 2px
                // — which made it the only bar in the app with square shoulders.
                val radiusPx = ChartTokens.barRadius(width.toDp()).toPx()
                val cornerRadius = CornerRadius(radiusPx, radiusPx)
                val color = colors.getValue(bar.influence)
                if (bar.charge > 0.0) {
                    val height = ((bar.charge / scale).toFloat() * centerY).coerceIn(1f, centerY)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, centerY - height),
                        size = Size(width, height),
                        cornerRadius = cornerRadius,
                    )
                }
                if (bar.drain > 0.0) {
                    val height = ((bar.drain / scale).toFloat() * centerY).coerceIn(1f, centerY)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, centerY),
                        size = Size(width, height),
                        cornerRadius = cornerRadius,
                    )
                }
                // A NO_DATA bucket with neither charge nor drain reads as a
                // low-emphasis tick spanning the strip: nothing moved, and we do
                // not know whether anything should have.
                if (bar.charge <= 0.0 &&
                    bar.drain <= 0.0 &&
                    bar.influence == BodyEnergyPrimaryInfluence.NO_DATA
                ) {
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
        if (viewport.isZoomed) clipRect { body() } else body()
    }
}

/**
 * The clock time a fraction of the way through the day, in the device's own
 * 12/24-hour convention.
 */
private fun clockAt(fraction: Float, formatter: DateTimeFormatter): String {
    val minutes = (fraction.coerceIn(0f, 1f) * MinutesPerDay).roundToInt()
    return LocalTime.of((minutes / 60).coerceIn(0, 23), minutes % 60).format(formatter)
}

/**
 * The accent colour for every Body Energy influence, resolved once so the strip
 * painter can look one up without a composable call per bar.
 *
 * Charge is cool, drain is warm, and everyday activity gets its own hue rather
 * than borrowing exertion's: "a 20k-step day" and "a hard session" are the two
 * readings this split exists to tell apart, and one colour for both would put
 * them back together in the only place the user can see.
 */
@Composable
internal fun bodyEnergyInfluenceColors(): Map<BodyEnergyPrimaryInfluence, Color> = mapOf(
    // Recovery / charge — cool hues, clearly separated from the warm drain set.
    BodyEnergyPrimaryInfluence.SLEEP_RECOVERY to StepsColor, // green
    BodyEnergyPrimaryInfluence.QUIET_REST to WorkoutColor, // cyan
    // Drain — warm hues plus everyday activity's blue, spread so each is distinct.
    BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY to DistanceColor, // blue
    BodyEnergyPrimaryInfluence.EXERTION to CaloriesColor, // red
    BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE to FloorsColor, // amber
    BodyEnergyPrimaryInfluence.RECOVERY_DEBT to HeartColor, // magenta
    // Neutral / absent — low-emphasis greys.
    BodyEnergyPrimaryInfluence.NO_DATA to MaterialTheme.colorScheme.outline,
    BodyEnergyPrimaryInfluence.STEADY to MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
internal fun bodyEnergyInfluenceColor(influence: BodyEnergyPrimaryInfluence): Color =
    bodyEnergyInfluenceColors().getValue(influence)
