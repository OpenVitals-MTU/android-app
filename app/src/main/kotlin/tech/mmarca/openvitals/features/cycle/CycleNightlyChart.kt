package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.NightlySeries
import tech.mmarca.openvitals.domain.cycle.NightlySignal
import tech.mmarca.openvitals.ui.components.chartSemantics
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.SleepColor

private val ChartHeight: Dp = 220.dp
private val Gutter: Dp = 44.dp
private val TopGutter: Dp = 12.dp
private val BottomGutter: Dp = 24.dp
private val LineStroke: Dp = 2.5.dp
private val GridStroke: Dp = 1.dp
private val PointRadius: Dp = 3.5.dp
private val LabelGap: Dp = 6.dp
private const val GridSteps = 4
private const val DayLabelEvery = 5

/** One colour per signal, so a line, its chip and its legend row read as one thing. */
internal fun nightlySignalColor(signal: NightlySignal): Color = when (signal) {
    NightlySignal.RESTING_HEART_RATE -> HeartColor
    NightlySignal.HRV_RMSSD -> SleepColor
    NightlySignal.RESPIRATORY_RATE -> HydrationColor
}

/** Breathing is read to a tenth, pulse and HRV to the unit. */
internal fun formatNightlyValue(signal: NightlySignal, value: Double): String =
    if (signal == NightlySignal.RESPIRATORY_RATE) {
        String.format(Locale.getDefault(), "%.1f", value)
    } else {
        String.format(Locale.getDefault(), "%.0f", value)
    }

/** The span one signal's own axis covers. A flat line still gets a visible span. */
internal data class NightlyAxis(val min: Double, val max: Double) {
    val span: Double get() = (max - min).coerceAtLeast(MIN_SPAN)

    companion object {
        private const val MIN_SPAN = 1.0

        fun of(series: NightlySeries): NightlyAxis {
            val values = series.points.map { it.value }
            val low = values.min()
            val high = values.max()
            // A little air so the line does not sit on the frame.
            val pad = ((high - low).coerceAtLeast(MIN_SPAN)) * 0.1
            return NightlyAxis(low - pad, high + pad)
        }
    }
}

/**
 * The visible signals by cycle day, each on its own axis (the span of its own values),
 * so three different units share one plot without one flattening the others. The first
 * visible signal labels the left axis, the second the right; every visible signal's
 * range is also spelled out in the legend under the chart.
 */
@Composable
internal fun CycleNightlyChart(
    visible: List<NightlySeries>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val axes = remember(visible) { visible.associate { it.signal to NightlyAxis.of(it) } }
    val maxDay = remember(visible) { visible.maxOf { s -> s.points.maxOf { it.cycleDay } }.coerceAtLeast(2) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val dayLabel = stringResource(R.string.cycle_thermal_day_label)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .chartSemantics(contentDescription),
    ) {
        val left = Gutter.toPx()
        val right = Gutter.toPx()
        val top = TopGutter.toPx()
        val bottom = BottomGutter.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
        fun xFor(day: Int): Float = left + plotWidth * ((day - 1).toFloat() / (maxDay - 1).coerceAtLeast(1))
        fun yFor(axis: NightlyAxis, value: Double): Float =
            top + plotHeight * (1f - ((value - axis.min) / axis.span).toFloat())

        for (step in 0..GridSteps) {
            val y = top + plotHeight * step / GridSteps
            drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), GridStroke.toPx())
        }
        // The axis labels: the top, middle and bottom of the first two visible signals.
        visible.take(2).forEachIndexed { index, series ->
            val axis = axes.getValue(series.signal)
            // Not the signal colour: the small text must clear contrast in both themes. The legend carries the colour.
            val style = axisStyle
            for (step in 0..2) {
                val fraction = step / 2f
                val text = formatNightlyValue(series.signal, axis.max - axis.span * fraction)
                val measured = textMeasurer.measure(text, style = style)
                val x = if (index == 0) left - measured.size.width - LabelGap.toPx() else size.width - right + LabelGap.toPx()
                drawText(textMeasurer, text, Offset(x, top + plotHeight * fraction - measured.size.height / 2f), style)
            }
        }
        for (day in 1..maxDay step DayLabelEvery) {
            val text = dayLabel.format(day)
            val measured = textMeasurer.measure(text, style = axisStyle)
            drawText(textMeasurer, text, Offset(xFor(day) - measured.size.width / 2f, size.height - bottom + LabelGap.toPx()), axisStyle)
        }
        visible.forEach { series ->
            val axis = axes.getValue(series.signal)
            val color = nightlySignalColor(series.signal)
            // Nights are not joined across a missing night: a gap stays a gap.
            series.points.zipWithNext().forEach { (from, to) ->
                if (to.cycleDay - from.cycleDay == 1) {
                    drawLine(
                        color,
                        Offset(xFor(from.cycleDay), yFor(axis, from.value)),
                        Offset(xFor(to.cycleDay), yFor(axis, to.value)),
                        LineStroke.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
            series.points.forEach { point ->
                val center = Offset(xFor(point.cycleDay), yFor(axis, point.value))
                if (point.isDailyValue) {
                    // Hollow: the device's daily value, not a reading from the night.
                    drawCircle(color, PointRadius.toPx(), center, style = Stroke(LineStroke.toPx() * 0.7f))
                } else {
                    drawCircle(color, PointRadius.toPx(), center)
                }
            }
        }
    }
}
