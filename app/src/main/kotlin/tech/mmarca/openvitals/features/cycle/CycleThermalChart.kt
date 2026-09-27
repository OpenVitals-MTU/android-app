package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.ThermalShiftResult
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.chartSemantics
import tech.mmarca.openvitals.ui.components.plotSemanticSummary
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val ChartHeight: Dp = 230.dp
private val LeftGutter: Dp = 46.dp
private val RightGutter: Dp = 16.dp
private val TopGutter: Dp = 16.dp
private val BottomGutter: Dp = 26.dp
private val LineStroke: Dp = 3.dp
private val GridStroke: Dp = 1.dp
private val CoverlineStroke: Dp = 2.dp
private val PointRadius: Dp = 4.dp
private val DisturbedRadius: Dp = 5.dp
private val DisturbedStroke: Dp = 2.dp
private val LabelGap: Dp = 6.dp
private val AxisLabelGap: Dp = 4.dp
private const val GridSteps = 4
private const val AxisPaddingCelsius = 0.15
private const val MinRangeCelsius = 0.1f
/** Basal temperatures are entered to the hundredth; the axis must tell them apart. */
private const val TemperatureDecimals = 2

/** The current cycle's temperatures by cycle day, disturbed readings hollow, the coverline dashed. */
@Composable
internal fun CycleThermalCard(
    thermal: CycleThermalDisplay?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.cycle_thermal_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (thermal == null) {
                Text(
                    text = stringResource(R.string.cycle_thermal_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val confirmed = thermal.shift as? ThermalShiftResult.Confirmed
            val verdict = if (confirmed != null) {
                stringResource(R.string.cycle_thermal_shift_confirmed)
            } else {
                stringResource(R.string.cycle_thermal_no_shift)
            }
            val counts = remember(thermal) { thermal.readings.size to thermal.readings.count { it.disturbed } }
            val summary = plotSemanticSummary(
                title = stringResource(R.string.section_cycle_thermal),
                values = remember(thermal) { thermal.readings.map { it.celsius } },
            ) { unitFormatter.temperature(it, TemperatureDecimals).text }
            CycleThermalChart(
                thermal = thermal,
                unitFormatter = unitFormatter,
                contentDescription = listOfNotNull(
                    summary,
                    pluralStringResource(R.plurals.cycle_thermal_readings, counts.first, counts.first),
                    pluralStringResource(R.plurals.cycle_thermal_disturbed, counts.second, counts.second),
                    verdict,
                ).joinToString(", "),
            )
            if (confirmed != null) {
                Text(text = stringResource(R.string.cycle_thermal_shift_confirmed), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.cycle_thermal_coverline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One reading placed on the chart: the cycle day, the temperature, and whether it was disturbed. */
private data class ThermalPoint(val day: Int, val celsius: Double, val disturbed: Boolean)

@Composable
private fun CycleThermalChart(
    thermal: CycleThermalDisplay,
    unitFormatter: UnitFormatter,
    contentDescription: String,
) {
    // The pass over the readings is the same on every recomposition; keep it.
    val points = remember(thermal) {
        thermal.readings.map { reading ->
            ThermalPoint(ChronoUnit.DAYS.between(thermal.cycleStart, reading.date).toInt() + 1, reading.celsius, reading.disturbed)
        }
    }
    val bounds = remember(points) {
        if (points.isEmpty()) {
            0f to 0f
        } else {
            (points.minOf { it.celsius } - AxisPaddingCelsius).toFloat() to (points.maxOf { it.celsius } + AxisPaddingCelsius).toFloat()
        }
    }
    val coverline = remember(thermal.shift) { (thermal.shift as? ThermalShiftResult.Confirmed)?.coverlineCelsius }
    val lineColor = CycleColor
    val disturbedColor = MaterialTheme.colorScheme.tertiary
    val coverlineColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) }
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val dayLabel = stringResource(R.string.cycle_thermal_day_label)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .chartSemantics(contentDescription),
    ) {
        if (points.isEmpty()) return@Canvas
        val left = LeftGutter.toPx()
        val right = RightGutter.toPx()
        val top = TopGutter.toPx()
        val bottom = BottomGutter.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
        val (minTemp, maxTemp) = bounds
        val maxDay = points.maxOf { it.day }.coerceAtLeast(2).toFloat()
        fun xFor(day: Int): Float = left + plotWidth * ((day - 1) / (maxDay - 1).coerceAtLeast(1f))
        fun yFor(celsius: Double): Float {
            val range = (maxTemp - minTemp).coerceAtLeast(MinRangeCelsius)
            return top + plotHeight * (1f - ((celsius.toFloat() - minTemp) / range))
        }

        for (step in 0..GridSteps) {
            val fraction = step.toFloat() / GridSteps
            val y = top + plotHeight * fraction
            val celsius = maxTemp - (maxTemp - minTemp) * fraction
            drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), GridStroke.toPx())
            val text = unitFormatter.temperature(celsius.toDouble(), TemperatureDecimals).value
            val measured = textMeasurer.measure(text, style = axisStyle)
            drawText(textMeasurer, text, Offset(left - measured.size.width - LabelGap.toPx(), y - measured.size.height / 2f), axisStyle)
        }
        coverline?.let { value ->
            val y = yFor(value)
            drawLine(coverlineColor, Offset(left, y), Offset(size.width - right, y), CoverlineStroke.toPx(), pathEffect = dash)
            val text = unitFormatter.temperature(value, TemperatureDecimals).value
            val style = axisStyle.copy(color = coverlineColor)
            val measured = textMeasurer.measure(text, style = style)
            drawText(textMeasurer, text, Offset(size.width - right - measured.size.width, y - measured.size.height - AxisLabelGap.toPx()), style)
        }
        points.zipWithNext().forEach { (from, to) ->
            drawLine(lineColor, Offset(xFor(from.day), yFor(from.celsius)), Offset(xFor(to.day), yFor(to.celsius)), LineStroke.toPx(), StrokeCap.Round)
        }
        points.forEach { point ->
            val center = Offset(xFor(point.day), yFor(point.celsius))
            if (point.disturbed) {
                drawCircle(disturbedColor, DisturbedRadius.toPx(), center, style = Stroke(DisturbedStroke.toPx()))
            } else {
                drawCircle(lineColor, PointRadius.toPx(), center)
            }
            val text = dayLabel.format(point.day)
            val measured = textMeasurer.measure(text, style = axisStyle)
            drawText(textMeasurer, text, Offset(center.x - measured.size.width / 2f, size.height - bottom + AxisLabelGap.toPx()), axisStyle)
        }
    }
}
