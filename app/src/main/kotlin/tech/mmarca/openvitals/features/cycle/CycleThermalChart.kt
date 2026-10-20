package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureShiftResult
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
private val SkinAxisGutter: Dp = 50.dp
private val TopGutter: Dp = 20.dp
private val BottomGutter: Dp = 26.dp
private val LineStroke: Dp = 3.dp
private val SkinLineStroke: Dp = 2.dp
private val GridStroke: Dp = 1.dp
private val CoverlineStroke: Dp = 2.dp
private val PointRadius: Dp = 4.dp
private val SkinPointRadius: Dp = 2.5.dp
private val DisturbedRadius: Dp = 5.dp
private val DisturbedStroke: Dp = 2.dp
private val LabelGap: Dp = 6.dp
private val AxisLabelGap: Dp = 4.dp
private val ChipDot: Dp = 10.dp
private const val GridSteps = 4
private const val AxisPaddingCelsius = 0.15
private const val SkinAxisPaddingCelsius = 0.1
private const val MinRangeCelsius = 0.1f
/** Basal temperatures are entered to the hundredth; the axis must tell them apart. */
private const val TemperatureDecimals = 2
private const val SkinDayLabelEvery = 7

/**
 * The current cycle's morning temperatures by cycle day, disturbed readings hollow, the coverline
 * dashed; beside them, on their own axis, the night skin temperature as a change from the device's
 * baseline. Each series is switched on and off by its own chip and is never mixed with the other.
 */
@Composable
internal fun CycleThermalCard(
    thermal: CycleThermalDisplay?,
    skin: CycleSkinDisplay?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    var showBbt by rememberSaveable { mutableStateOf(true) }
    var showSkin by rememberSaveable { mutableStateOf(true) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.cycle_thermal_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (thermal == null && skin == null) {
                Text(
                    text = stringResource(R.string.cycle_thermal_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            SeriesChips(
                showBbt = showBbt && thermal != null,
                showSkin = showSkin && skin != null,
                hasBbt = thermal != null,
                hasSkin = skin != null,
                onToggleBbt = { showBbt = !showBbt },
                onToggleSkin = { showSkin = !showSkin },
            )
            val bbtVisible = showBbt && thermal != null
            val skinVisible = showSkin && skin != null
            if (skin == null) {
                Text(
                    text = stringResource(R.string.cycle_thermal_skin_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!bbtVisible && !skinVisible) {
                Text(
                    text = stringResource(R.string.cycle_thermal_none_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val confirmed = thermal?.shift as? ThermalShiftResult.Confirmed
            val skinText = if (skin != null) skinResultText(skin, unitFormatter, dateTimeFormatterProvider) else null
            val verdict = if (confirmed != null) {
                stringResource(R.string.cycle_thermal_shift_confirmed)
            } else {
                stringResource(R.string.cycle_thermal_no_shift)
            }
            val description = remember(thermal) { thermal?.readings.orEmpty().map { it.celsius } }
            val summary = plotSemanticSummary(
                title = stringResource(R.string.section_cycle_thermal),
                values = description,
            ) { unitFormatter.temperature(it, TemperatureDecimals).text }
            val disturbed = remember(thermal) { thermal?.readings.orEmpty().count { it.disturbed } }
            CycleThermalChart(
                thermal = thermal.takeIf { bbtVisible },
                skin = skin.takeIf { skinVisible },
                unitFormatter = unitFormatter,
                contentDescription = listOfNotNull(
                    summary.takeIf { bbtVisible },
                    pluralStringResource(R.plurals.cycle_thermal_readings, description.size, description.size).takeIf { bbtVisible },
                    pluralStringResource(R.plurals.cycle_thermal_disturbed, disturbed, disturbed).takeIf { bbtVisible },
                    verdict.takeIf { bbtVisible },
                    skinText.takeIf { skinVisible },
                ).joinToString(", "),
            )
            if (bbtVisible && confirmed != null) {
                Text(text = stringResource(R.string.cycle_thermal_shift_confirmed), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.cycle_thermal_coverline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (skinVisible && skin != null && skinText != null) {
                CycleSkinResultLine(skin = skin, text = skinText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesChips(
    showBbt: Boolean,
    showSkin: Boolean,
    hasBbt: Boolean,
    hasSkin: Boolean,
    onToggleBbt: () -> Unit,
    onToggleSkin: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SeriesChip(R.string.cycle_thermal_chip_bbt, bbtColor(), showBbt, hasBbt, R.string.cycle_thermal_bbt_unavailable, onToggleBbt)
        SeriesChip(R.string.cycle_thermal_chip_skin, skinColor(), showSkin, hasSkin, R.string.cycle_thermal_skin_unavailable, onToggleSkin)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesChip(labelRes: Int, color: Color, selected: Boolean, enabled: Boolean, unavailableRes: Int, onClick: () -> Unit) {
    val unavailable = stringResource(unavailableRes)
    FilterChip(
        modifier = if (enabled) Modifier else Modifier.semantics { stateDescription = unavailable },
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
        leadingIcon = { Spacer(Modifier.size(ChipDot).background(color, CircleShape)) },
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun bbtColor(): Color = CycleColor

@Composable
private fun skinColor(): Color = MaterialTheme.colorScheme.secondary

/** One reading placed on the chart: the cycle day, the temperature, and whether it was disturbed. */
private data class ThermalPoint(val day: Int, val celsius: Double, val disturbed: Boolean)

/** One night of skin temperature: the cycle day and the change from the device's baseline. */
private data class SkinPoint(val day: Int, val delta: Double)

/** A vertical scale from [min] to [max] over the plot's height. */
private class Axis(val min: Float, val max: Float, val top: Float, val height: Float) {
    fun yFor(value: Double): Float {
        val range = (max - min).coerceAtLeast(MinRangeCelsius)
        return top + height * (1f - ((value.toFloat() - min) / range))
    }

    fun valueAt(fraction: Float): Double = (max - (max - min) * fraction).toDouble()
}

private fun cycleDay(start: LocalDate, date: LocalDate): Int = ChronoUnit.DAYS.between(start, date).toInt() + 1

@Composable
private fun CycleThermalChart(
    thermal: CycleThermalDisplay?,
    skin: CycleSkinDisplay?,
    unitFormatter: UnitFormatter,
    contentDescription: String,
) {
    // The pass over the readings is the same on every recomposition; keep it.
    val points = remember(thermal) {
        thermal?.readings.orEmpty().map { reading ->
            ThermalPoint(cycleDay(thermal!!.cycleStart, reading.date), reading.celsius, reading.disturbed)
        }
    }
    val skinPoints = remember(skin) {
        skin?.nights.orEmpty().map { SkinPoint(cycleDay(skin!!.cycleStart, it.date), it.deltaCelsius) }
    }
    val bounds = remember(points) {
        if (points.isEmpty()) {
            0f to 0f
        } else {
            (points.minOf { it.celsius } - AxisPaddingCelsius).toFloat() to (points.maxOf { it.celsius } + AxisPaddingCelsius).toFloat()
        }
    }
    val skinBounds = remember(skinPoints) {
        if (skinPoints.isEmpty()) {
            0f to 0f
        } else {
            (skinPoints.minOf { it.delta } - SkinAxisPaddingCelsius).toFloat() to (skinPoints.maxOf { it.delta } + SkinAxisPaddingCelsius).toFloat()
        }
    }
    val coverline = remember(thermal?.shift) { (thermal?.shift as? ThermalShiftResult.Confirmed)?.coverlineCelsius }
    val skinShiftDay = remember(skin) {
        (skin?.result as? SkinTemperatureShiftResult.Confirmed)?.let { cycleDay(skin.cycleStart, it.firstHighDay) }
    }
    val style = ChartStyle(
        bbt = bbtColor(),
        skin = skinColor(),
        disturbed = MaterialTheme.colorScheme.tertiary,
        coverline = MaterialTheme.colorScheme.outline,
        grid = MaterialTheme.colorScheme.outlineVariant,
        text = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
    )
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) }
    val textMeasurer = rememberTextMeasurer()
    val dayLabel = stringResource(R.string.cycle_thermal_day_label)
    val bbtTitle = stringResource(R.string.cycle_thermal_axis_bbt) + " " + unitFormatter.temperature(0.0).unit
    val skinTitle = stringResource(R.string.cycle_thermal_axis_skin) + " " + unitFormatter.temperatureDelta(0.0).unit

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .chartSemantics(contentDescription),
    ) {
        if (points.isEmpty() && skinPoints.isEmpty()) return@Canvas
        val left = if (points.isEmpty()) RightGutter.toPx() else LeftGutter.toPx()
        val right = if (skinPoints.isEmpty()) RightGutter.toPx() else SkinAxisGutter.toPx()
        val top = TopGutter.toPx()
        val bottom = BottomGutter.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
        val maxDay = (points.map { it.day } + skinPoints.map { it.day }).max().coerceAtLeast(2).toFloat()
        fun xFor(day: Int): Float = left + plotWidth * ((day - 1) / (maxDay - 1).coerceAtLeast(1f))
        val bbtAxis = Axis(bounds.first, bounds.second, top, plotHeight)
        val skinAxis = Axis(skinBounds.first, skinBounds.second, top, plotHeight)
        val frame = Frame(left, right, top, bottom)

        for (step in 0..GridSteps) {
            val fraction = step.toFloat() / GridSteps
            val y = top + plotHeight * fraction
            drawLine(style.grid, Offset(left, y), Offset(size.width - right, y), GridStroke.toPx())
            if (points.isNotEmpty()) {
                val text = unitFormatter.temperature(bbtAxis.valueAt(fraction), TemperatureDecimals).value
                val measured = textMeasurer.measure(text, style = style.text)
                drawText(textMeasurer, text, Offset(left - measured.size.width - LabelGap.toPx(), y - measured.size.height / 2f), style.text)
            }
            if (skinPoints.isNotEmpty()) {
                val text = unitFormatter.temperatureDelta(skinAxis.valueAt(fraction)).value
                val measured = textMeasurer.measure(text, style = style.text.copy(color = style.skin))
                drawText(textMeasurer, text, Offset(size.width - right + LabelGap.toPx(), y - measured.size.height / 2f), style.text.copy(color = style.skin))
            }
        }
        if (points.isNotEmpty()) drawAxisTitle(textMeasurer, bbtTitle, style.text.copy(color = style.bbt), Offset(left, 0f), alignEnd = false)
        if (skinPoints.isNotEmpty()) {
            drawAxisTitle(textMeasurer, skinTitle, style.text.copy(color = style.skin), Offset(size.width, 0f), alignEnd = true)
        }
        skinShiftDay?.let { day ->
            val x = xFor(day)
            drawLine(style.skin, Offset(x, top), Offset(x, top + plotHeight), CoverlineStroke.toPx(), pathEffect = dash)
        }
        drawSkin(skinPoints, skinAxis, style, textMeasurer, dayLabel, drawDayLabels = points.isEmpty(), frame, ::xFor)
        coverline?.let { value ->
            val y = bbtAxis.yFor(value)
            drawLine(style.coverline, Offset(left, y), Offset(size.width - right, y), CoverlineStroke.toPx(), pathEffect = dash)
            val text = unitFormatter.temperature(value, TemperatureDecimals).value
            val coverStyle = style.text.copy(color = style.coverline)
            val measured = textMeasurer.measure(text, style = coverStyle)
            drawText(textMeasurer, text, Offset(size.width - right - measured.size.width, y - measured.size.height - AxisLabelGap.toPx()), coverStyle)
        }
        drawBbt(points, bbtAxis, style, textMeasurer, dayLabel, frame, ::xFor)
    }
}

private class ChartStyle(
    val bbt: Color,
    val skin: Color,
    val disturbed: Color,
    val coverline: Color,
    val grid: Color,
    val text: TextStyle,
)

private class Frame(val left: Float, val right: Float, val top: Float, val bottom: Float)

private fun DrawScope.drawAxisTitle(measurer: TextMeasurer, text: String, style: TextStyle, origin: Offset, alignEnd: Boolean) {
    val measured = measurer.measure(text, style = style)
    val x = if (alignEnd) origin.x - measured.size.width - AxisLabelGap.toPx() else origin.x - measured.size.width / 2f
    drawText(measurer, text, Offset(x.coerceAtLeast(0f), origin.y), style)
}

private fun DrawScope.drawSkin(
    points: List<SkinPoint>,
    axis: Axis,
    style: ChartStyle,
    measurer: TextMeasurer,
    dayLabel: String,
    drawDayLabels: Boolean,
    frame: Frame,
    xFor: (Int) -> Float,
) {
    points.zipWithNext().forEach { (from, to) ->
        // A gap of days is a break in the line, not a made-up reading.
        if (to.day - from.day == 1) {
            drawLine(style.skin, Offset(xFor(from.day), axis.yFor(from.delta)), Offset(xFor(to.day), axis.yFor(to.delta)), SkinLineStroke.toPx(), StrokeCap.Round)
        }
    }
    points.forEach { point ->
        drawCircle(style.skin, SkinPointRadius.toPx(), Offset(xFor(point.day), axis.yFor(point.delta)))
        if (drawDayLabels && (point.day == 1 || point.day % SkinDayLabelEvery == 0)) {
            val text = dayLabel.format(point.day)
            val measured = measurer.measure(text, style = style.text)
            drawText(measurer, text, Offset(xFor(point.day) - measured.size.width / 2f, size.height - frame.bottom + AxisLabelGap.toPx()), style.text)
        }
    }
}

private fun DrawScope.drawBbt(
    points: List<ThermalPoint>,
    axis: Axis,
    style: ChartStyle,
    measurer: TextMeasurer,
    dayLabel: String,
    frame: Frame,
    xFor: (Int) -> Float,
) {
    points.zipWithNext().forEach { (from, to) ->
        drawLine(style.bbt, Offset(xFor(from.day), axis.yFor(from.celsius)), Offset(xFor(to.day), axis.yFor(to.celsius)), LineStroke.toPx(), StrokeCap.Round)
    }
    points.forEach { point ->
        val center = Offset(xFor(point.day), axis.yFor(point.celsius))
        if (point.disturbed) {
            drawCircle(style.disturbed, DisturbedRadius.toPx(), center, style = Stroke(DisturbedStroke.toPx()))
        } else {
            drawCircle(style.bbt, PointRadius.toPx(), center)
        }
        val text = dayLabel.format(point.day)
        val measured = measurer.measure(text, style = style.text)
        drawText(measurer, text, Offset(center.x - measured.size.width / 2f, size.height - frame.bottom + AxisLabelGap.toPx()), style.text)
    }
}
