package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

val ChartYAxisWidth = 56.dp
val ChartAxisGap = 8.dp

/**
 * The y bounds of a plot.
 *
 * Not just a pair: the *rule* for choosing bounds kept being written per chart — pad by
 * some percent of the span — and every copy is this idea. A flat series has no span to
 * take a percentage of, so it falls back to the magnitude of the value itself, and a
 * series that never goes negative keeps its floor at zero rather than dipping below it.
 */
data class ChartRange(val min: Double, val max: Double) {
    companion object {
        /** Bounds that clear the data by [fraction] of its span, top and bottom. */
        fun padded(
            values: Iterable<Double>,
            fraction: Double = 0.08,
            floor: Double? = null,
        ): ChartRange {
            var min = Double.POSITIVE_INFINITY
            var max = Double.NEGATIVE_INFINITY
            var isEmpty = true
            for (value in values) {
                isEmpty = false
                if (value < min) min = value
                if (value > max) max = value
            }
            if (isEmpty) return ChartRange(0.0, 1.0)

            val span = max - min
            // A flat line has no span; pad against how big the value is instead, so a
            // steady 70 kg does not get a hairline axis around it.
            val basis = if (span > 0) span else if (abs(max) < 1) 1.0 else abs(max)
            val padding = basis * fraction
            val low = min - padding
            return ChartRange(
                min = if (floor != null && low < floor) floor else low,
                max = max + padding,
            )
        }
    }
}

@Composable
fun YAxisChart(
    labels: List<String>,
    chartHeight: Dp,
    modifier: Modifier = Modifier,
    canvasModifier: Modifier = Modifier,
    axisWidth: Dp = ChartYAxisWidth,
    axisGap: Dp = ChartAxisGap,
    content: DrawScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(axisWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            labels.forEach { label ->
                AutoResizeText(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
        Spacer(Modifier.width(axisGap))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight)
                .then(canvasModifier),
            onDraw = content,
        )
    }
}

/**
 * [YAxisChart]'s sibling for plots that are more than one draw call: the same
 * label column and gutter, but the plot area is a composable slot, so a chart can
 * layer gesture surfaces ([ChartScrubber], [ChartReveal]) over its canvas while
 * staying aligned with every other chart's axis.
 */
@Composable
fun YAxisChartSlot(
    labels: List<String>,
    chartHeight: Dp,
    modifier: Modifier = Modifier,
    axisWidth: Dp = ChartYAxisWidth,
    axisGap: Dp = ChartAxisGap,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(axisWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            labels.forEach { label ->
                AutoResizeText(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
        Spacer(Modifier.width(axisGap))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight),
        ) {
            content()
        }
    }
}

@Composable
fun ChartXAxisWithYAxis(
    modifier: Modifier = Modifier,
    axisWidth: Dp = ChartYAxisWidth,
    axisGap: Dp = ChartAxisGap,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.width(axisWidth + axisGap))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

fun DrawScope.drawYAxisGuides(
    gridColor: Color,
    axisColor: Color = gridColor,
    lineCount: Int = 3,
    strokeWidth: Float = 1f,
) {
    if (lineCount < 2) return

    repeat(lineCount) { index ->
        val y = size.height * index / (lineCount - 1).toFloat()
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
    drawLine(
        color = axisColor,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidth,
    )
}

fun chartYAxisLabels(
    minValue: Double,
    maxValue: Double,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
): List<String> {
    val min = minValue.takeIf { it.isFinite() } ?: 0.0
    val max = maxValue.takeIf { it.isFinite() }?.takeIf { it > min } ?: (min + 1.0)
    val mid = min + (max - min) / 2.0
    val values = listOf(max, mid, min)
    val labels = values.map(valueFormatter)
    return if (labels.distinct().size == labels.size) {
        labels
    } else {
        values.map { formatPreciseAxisValue(value = it, range = max - min) }
    }
}

fun formatCompactAxisValue(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000.0 -> "${trimAxisDecimal(value / 1_000_000.0)}M"
        absValue >= 1_000.0 -> "${trimAxisDecimal(value / 1_000.0)}k"
        absValue >= 10.0 -> value.roundToLong().toString()
        absValue == 0.0 -> "0"
        else -> trimAxisDecimal(value)
    }
}

private fun trimAxisDecimal(value: Double): String =
    trimAxisDecimal(value = value, decimals = 1)

private fun formatPreciseAxisValue(value: Double, range: Double): String {
    val decimals = when {
        range < 1.0 -> 2
        range < 10.0 -> 1
        else -> 0
    }
    return when {
        abs(value) >= 1_000_000.0 -> "${trimAxisDecimal(value / 1_000_000.0, decimals.coerceAtLeast(1))}M"
        abs(value) >= 1_000.0 -> "${trimAxisDecimal(value / 1_000.0, decimals.coerceAtLeast(1))}k"
        else -> trimAxisDecimal(value, decimals)
    }
}

private fun trimAxisDecimal(value: Double, decimals: Int): String {
    val scale = 10.0.pow(decimals.toDouble())
    val rounded = (value * scale).roundToLong() / scale
    return if (rounded % 1.0 == 0.0) {
        rounded.roundToLong().toString()
    } else {
        rounded.toString()
    }
}
