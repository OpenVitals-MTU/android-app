package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

data class MetricLinePoint(
    val date: LocalDate,
    val value: Double,
    val time: Instant? = null,
)

data class MetricLineSeries(
    val points: List<MetricLinePoint>,
    val color: Color,
    val label: String? = null,
)

data class MetricLinePlotPoint(
    val xFraction: Float,
    val value: Double,
)

@Composable
fun MetricLinePlot(
    points: List<MetricLinePlotPoint>,
    minValue: Double,
    maxValue: Double,
    accentColor: Color,
    chartHeight: Dp,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    canvasModifier: Modifier = Modifier,
    lineStrokeWidth: Dp = 2.dp,
    pointRadius: Dp = 3.5.dp,
    drawPoints: Boolean = true,
) {
    val axisMax = maxValue.takeIf { it > minValue } ?: (minValue + 1.0)
    val gridColor = accentColor.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)

    YAxisChart(
        labels = chartYAxisLabels(
            minValue = minValue,
            maxValue = axisMax,
            valueFormatter = valueFormatter,
        ),
        chartHeight = chartHeight,
        modifier = modifier,
        canvasModifier = canvasModifier,
    ) {
        drawYAxisGuides(
            gridColor = gridColor,
            axisColor = axisColor,
            strokeWidth = 1.dp.toPx(),
        )
        drawMetricLinePlot(
            points = points,
            minValue = minValue,
            maxValue = axisMax,
            color = accentColor,
            lineStrokeWidth = lineStrokeWidth.toPx(),
            pointRadius = pointRadius.toPx(),
            drawPoints = drawPoints,
        )
    }
}

@Composable
fun MetricLineChart(
    title: String,
    points: List<MetricLinePoint>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summaryText: String,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    seriesLabel: String? = title,
    averagePeriodPoints: Boolean = true,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val chartPoints = if (averagePeriodPoints && selectedRange != TimeRange.DAY) {
        dailyAverageLinePoints(points)
    } else {
        points
    }

    MetricLineChart(
        title = title,
        series = listOf(MetricLineSeries(chartPoints, accentColor, seriesLabel)),
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor,
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}

@Composable
fun <T> MetricLineChart(
    title: String,
    entries: List<T>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summaryText: String,
    time: (T) -> Instant,
    value: (T) -> Double,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    seriesLabel: String? = title,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    MetricLineChart(
        title = title,
        points = entries.mapLinePoints(time = time, value = value),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = accentColor,
        summaryText = summaryText,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        seriesLabel = seriesLabel,
        valueFormatter = valueFormatter,
    )
}

@Composable
fun MetricLineChart(
    title: String,
    series: List<MetricLineSeries>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val visibleSeries = series
        .map { chartSeries ->
            chartSeries.copy(points = chartSeries.points.filter { point ->
                point.value.isFinite() && !point.date.isBefore(period.start) && !point.date.isAfter(period.end)
            })
        }
        .filter { it.points.isNotEmpty() }
    val allPoints = visibleSeries.flatMap { it.points }
    if (allPoints.isEmpty()) return
    if (selectedRange == TimeRange.DAY && allPoints.mapNotNull { it.time }.distinct().size <= 1) return

    val allValues = allPoints.map { it.value }
    val minValue = allValues.minOrNull() ?: return
    val maxValue = allValues.maxOrNull() ?: return
    val (axisMin, axisMax) = paddedLineAxisRange(minValue, maxValue)
    val axisDates = remember(period) { datesInPeriod(period) }
    val chartHeight = 150.dp
    val zone = ZoneId.systemDefault()
    val dayStart = remember(period, zone) { period.start.atStartOfDay(zone).toInstant() }
    val dayEnd = remember(period, zone) { period.start.plusDays(1).atStartOfDay(zone).toInstant() }
    val dayDurationMillis = remember(dayStart, dayEnd) {
        Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)
    }
    val periodDayCount = axisDates.size.coerceAtLeast(1)
    val gridColor = accentColor.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val chartTapModifier = if (
        selectedRange.supportsChartDaySelection() &&
        onDateSelected != null &&
        axisDates.isNotEmpty()
    ) {
        Modifier.pointerInput(axisDates, onDateSelected) {
            detectTapGestures { offset ->
                val slotWidth = size.width.toFloat() / axisDates.size
                val index = (offset.x / slotWidth).toInt().coerceIn(0, axisDates.lastIndex)
                onDateSelected(axisDates[index])
            }
        }
    } else {
        Modifier
    }

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            YAxisChart(
                labels = chartYAxisLabels(
                    minValue = axisMin,
                    maxValue = axisMax,
                    valueFormatter = valueFormatter,
                ),
                chartHeight = chartHeight,
                canvasModifier = chartTapModifier,
            ) {
                drawYAxisGuides(
                    gridColor = gridColor,
                    axisColor = axisColor,
                    strokeWidth = 1.dp.toPx(),
                )
                drawLineSelectedDateHighlight(
                    selectedRange = selectedRange,
                    selectedDate = selectedDate,
                    period = period,
                    axisDates = axisDates,
                    color = accentColor.copy(alpha = 0.16f),
                )
                visibleSeries.forEach { lineSeries ->
                    drawMetricLineSeries(
                        points = lineSeries.points,
                        selectedRange = selectedRange,
                        period = period,
                        dayStart = dayStart,
                        dayDurationMillis = dayDurationMillis,
                        periodDayCount = periodDayCount,
                        minValue = axisMin,
                        maxValue = axisMax,
                        color = lineSeries.color,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (selectedRange == TimeRange.DAY) {
                DayTimeXAxis()
            } else {
                ChartXAxisWithYAxis {
                    PeriodChartXAxis(
                        dates = axisDates,
                        selectedRange = selectedRange,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                }
            }
            if (visibleSeries.size > 1) {
                Spacer(Modifier.height(8.dp))
                MetricLineLegend(visibleSeries)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun <T> List<T>.mapLinePoints(
    time: (T) -> Instant,
    value: (T) -> Double,
): List<MetricLinePoint> =
    map { entry ->
        val entryTime = time(entry)
        MetricLinePoint(
            date = entryTime.atZone(ZoneId.systemDefault()).toLocalDate(),
            time = entryTime,
            value = value(entry),
        )
    }.sortedBy { it.time }

fun dailyAverageLinePoints(points: List<MetricLinePoint>): List<MetricLinePoint> =
    points
        .groupBy { it.date }
        .map { (date, dayPoints) ->
            MetricLinePoint(
                date = date,
                value = dayPoints.map { it.value }.average(),
            )
        }
        .sortedBy { it.date }

private fun DrawScope.drawMetricLinePlot(
    points: List<MetricLinePlotPoint>,
    minValue: Double,
    maxValue: Double,
    color: Color,
    lineStrokeWidth: Float,
    pointRadius: Float,
    drawPoints: Boolean,
) {
    if (points.isEmpty()) return

    val range = (maxValue - minValue).coerceAtLeast(0.000001)
    val positionedPoints = points.map { point ->
        Offset(
            x = size.width * point.xFraction.coerceIn(0f, 1f),
            y = size.height * (1f - ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)),
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
private fun DayTimeXAxis() {
    ChartXAxisWithYAxis {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricLineLegend(series: List<MetricLineSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        series.forEach { item ->
            item.label?.let { label ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(8.dp)
                            .background(item.color, shape = androidx.compose.foundation.shape.CircleShape),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMetricLineSeries(
    points: List<MetricLinePoint>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dayStart: Instant,
    dayDurationMillis: Long,
    periodDayCount: Int,
    minValue: Double,
    maxValue: Double,
    color: Color,
) {
    val range = (maxValue - minValue).coerceAtLeast(1.0)
    val positionedPoints = points.map { point ->
        val x = if (selectedRange == TimeRange.DAY) {
            val pointTime = point.time ?: point.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val elapsed = Duration.between(dayStart, pointTime).toMillis().coerceIn(0L, dayDurationMillis)
            size.width * elapsed.toFloat() / dayDurationMillis
        } else {
            val slotWidth = size.width / periodDayCount
            val daysFromStart = ChronoUnit.DAYS.between(period.start, point.date)
                .coerceIn(0L, (periodDayCount - 1).toLong())
            daysFromStart * slotWidth + slotWidth / 2f
        }
        Offset(
            x = x,
            y = size.height * (1f - ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)),
        )
    }

    for (index in 0 until positionedPoints.lastIndex) {
        drawLine(
            color = color,
            start = positionedPoints[index],
            end = positionedPoints[index + 1],
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    positionedPoints.forEach { point ->
        drawCircle(
            color = color,
            radius = 3.5.dp.toPx(),
            center = point,
        )
    }
}

private fun DrawScope.drawLineSelectedDateHighlight(
    selectedRange: TimeRange,
    selectedDate: LocalDate?,
    period: DatePeriod,
    axisDates: List<LocalDate>,
    color: Color,
) {
    if (!selectedRange.supportsChartDaySelection() || selectedDate == null || selectedDate !in period.start..period.end) {
        return
    }
    val index = axisDates.indexOf(selectedDate)
    if (index < 0 || axisDates.isEmpty()) return

    val slotWidth = size.width / axisDates.size
    drawRect(
        color = color,
        topLeft = Offset(index * slotWidth, 0f),
        size = Size(slotWidth, size.height),
    )
}

private fun paddedLineAxisRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    val range = maxValue - minValue
    val padding = if (range == 0.0) {
        max(abs(maxValue) * 0.05, 1.0)
    } else {
        range * 0.08
    }
    return (minValue - padding) to (maxValue + padding)
}

private fun datesInPeriod(period: DatePeriod): List<LocalDate> =
    generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.toList()
