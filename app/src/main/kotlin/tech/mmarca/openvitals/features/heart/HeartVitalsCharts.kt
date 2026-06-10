package tech.mmarca.openvitals.features.heart

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.components.YAxisChart
import tech.mmarca.openvitals.ui.components.chartYAxisLabels
import tech.mmarca.openvitals.ui.components.drawYAxisGuides
import tech.mmarca.openvitals.ui.components.formatCompactAxisValue
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.supportsChartDaySelection
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal data class VitalsLinePoint(
    val date: LocalDate,
    val value: Double,
    val time: Instant? = null,
)

internal data class VitalsDailyRange(
    val average: List<VitalsLinePoint>,
    val min: List<VitalsLinePoint>,
    val max: List<VitalsLinePoint>,
) {
    val hasRange: Boolean =
        min.zip(max).any { (low, high) -> low.value != high.value }
}

internal data class VitalsLineSeries(
    val points: List<VitalsLinePoint>,
    val color: Color,
    val label: String? = null,
)

@Composable
internal fun RespiratoryRateChart(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val dailyValues = remember(entries) {
        dailyRangeVitalsPoints(
            entries = entries,
            time = { it.time },
            value = { it.breathsPerMinute },
        )
    }
    val average = respiratoryRateAverage(respiratoryRateBuckets(entries, selectedRange, period))
    val rawPoints = remember(entries) {
        rawVitalsPoints(
            entries = entries,
            time = { it.time },
            value = { it.breathsPerMinute },
        )
    }
    val series = if (selectedRange == TimeRange.DAY) {
        listOf(VitalsLineSeries(rawPoints, respiratoryColor, stringResource(R.string.metric_respiratory_rate)))
    } else {
        buildList {
            add(VitalsLineSeries(dailyValues.average, respiratoryColor, stringResource(R.string.summary_average)))
            if (dailyValues.hasRange) {
                add(VitalsLineSeries(dailyValues.min, respiratoryColor.copy(alpha = 0.55f), stringResource(R.string.stat_lowest)))
                add(VitalsLineSeries(dailyValues.max, VitalsColor.copy(alpha = 0.75f), stringResource(R.string.stat_highest)))
            }
        }
    }

    VitalsTrendLineChart(
        title = stringResource(R.string.metric_respiratory_rate),
        series = series,
        selectedRange = selectedRange,
        period = period,
        accentColor = respiratoryColor,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_value_avg, unitFormatter.respiratoryRate(average).text)
        }",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier.fillMaxWidth(),
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.respiratoryRate(it).text },
    )
}

@Composable
internal fun BloodPressureChart(
    entries: List<BloodPressureEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val systolic = rawVitalsPoints(
        entries = sorted,
        time = { it.time },
        value = { it.systolicMmHg.toDouble() },
    )
    val diastolic = rawVitalsPoints(
        entries = sorted,
        time = { it.time },
        value = { it.diastolicMmHg.toDouble() },
    )
    val series = if (selectedRange == TimeRange.DAY) {
        listOf(
            VitalsLineSeries(systolic, VitalsColor, stringResource(R.string.vitals_entry_systolic_label)),
            VitalsLineSeries(diastolic, HeartColor, stringResource(R.string.vitals_entry_diastolic_label)),
        )
    } else {
        listOf(
            VitalsLineSeries(dailyAverageVitalsPoints(systolic), VitalsColor, stringResource(R.string.vitals_entry_systolic_label)),
            VitalsLineSeries(dailyAverageVitalsPoints(diastolic), HeartColor, stringResource(R.string.vitals_entry_diastolic_label)),
        )
    }

    VitalsTrendLineChart(
        title = stringResource(R.string.metric_blood_pressure),
        series = series,
        selectedRange = selectedRange,
        period = period,
        accentColor = VitalsColor,
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
        }",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        valueFormatter = { "${it.roundToInt()} mmHg" },
    )
}

@Composable
internal fun VitalsLineChart(
    title: String,
    points: List<VitalsLinePoint>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val chartPoints = if (selectedRange == TimeRange.DAY) points else dailyAverageVitalsPoints(points)

    VitalsTrendLineChart(
        title = title,
        series = listOf(VitalsLineSeries(chartPoints, accentColor, title)),
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor,
        summaryText = summary,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}

@Composable
internal fun BodyTemperatureChart(
    entries: List<BodyTempEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.time }
    VitalsLineChart(
        title = stringResource(R.string.metric_body_temp),
        points = rawVitalsPoints(
            entries = sorted,
            time = { it.time },
            value = { it.temperatureCelsius },
        ),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = temperatureColor,
        summary = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
        }",
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.temperature(it).text },
    )
}

@Composable
internal fun BloodGlucoseChart(
    entries: List<BloodGlucoseEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.time }
    VitalsLineChart(
        title = stringResource(R.string.metric_blood_glucose),
        points = rawVitalsPoints(
            entries = sorted,
            time = { it.time },
            value = { it.millimolesPerLiter },
        ),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = glucoseColor,
        summary = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_value_avg, unitFormatter.bloodGlucose(sorted.map { it.millimolesPerLiter }.average()).text)
        }",
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.bloodGlucose(it).text },
    )
}

@Composable
internal fun SkinTemperatureChart(
    entries: List<SkinTemperatureEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val chartEntries = entries
        .filter { it.averageDeltaCelsius != null }
        .sortedBy { it.time }
    if (chartEntries.isEmpty()) return

    VitalsLineChart(
        title = stringResource(R.string.metric_skin_temperature),
        points = rawVitalsPoints(
            entries = chartEntries,
            time = { it.time },
            value = { it.averageDeltaCelsius ?: 0.0 },
        ),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = temperatureColor,
        summary = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(
                R.string.summary_value_avg,
                unitFormatter.temperatureDelta(chartEntries.mapNotNull { it.averageDeltaCelsius }.average()).text,
            )
        }",
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.temperatureDelta(it).text },
    )
}

@Composable
internal fun Vo2MaxChart(
    entries: List<Vo2MaxEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.time }
    VitalsLineChart(
        title = stringResource(R.string.metric_vo2_max),
        points = rawVitalsPoints(
            entries = sorted,
            time = { it.time },
            value = { it.vo2MaxMlPerKgPerMin },
        ),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = vo2Color,
        summary = "${localizedPeriodTitle(selectedRange, period)} · ${
            stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
        }",
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.vo2Max(it).text },
    )
}

@Composable
internal fun VitalsTrendLineChart(
    title: String,
    series: List<VitalsLineSeries>,
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
    val (axisMin, axisMax) = paddedAxisRange(minValue, maxValue)
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

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                drawSelectedDateHighlight(
                    selectedRange = selectedRange,
                    selectedDate = selectedDate,
                    period = period,
                    axisDates = axisDates,
                    color = accentColor.copy(alpha = 0.16f),
                )
                visibleSeries.forEach { lineSeries ->
                    drawVitalsLineSeries(
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
                VitalsLineLegend(visibleSeries)
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

@Composable
private fun DayTimeXAxis() {
    ChartXAxisWithYAxis {
        val labels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
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
private fun VitalsLineLegend(series: List<VitalsLineSeries>) {
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

internal fun <T> rawVitalsPoints(
    entries: List<T>,
    time: (T) -> Instant,
    value: (T) -> Double,
): List<VitalsLinePoint> =
    entries
        .map { entry ->
            val entryTime = time(entry)
            VitalsLinePoint(
                date = entryTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                time = entryTime,
                value = value(entry),
            )
        }
        .sortedBy { it.time }

internal fun dailyAverageVitalsPoints(points: List<VitalsLinePoint>): List<VitalsLinePoint> =
    points
        .groupBy { it.date }
        .map { (date, dayPoints) ->
            VitalsLinePoint(
                date = date,
                value = dayPoints.map { it.value }.average(),
            )
        }
        .sortedBy { it.date }

internal fun <T> dailyRangeVitalsPoints(
    entries: List<T>,
    time: (T) -> Instant,
    value: (T) -> Double,
): VitalsDailyRange {
    val dayRanges = entries
        .groupBy { time(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            val values = dayEntries.map(value)
            VitalsLinePoint(date = date, value = values.average()) to
                (VitalsLinePoint(date = date, value = values.minOrNull() ?: 0.0) to
                    VitalsLinePoint(date = date, value = values.maxOrNull() ?: 0.0))
        }
        .sortedBy { it.first.date }

    return VitalsDailyRange(
        average = dayRanges.map { it.first },
        min = dayRanges.map { it.second.first },
        max = dayRanges.map { it.second.second },
    )
}

private fun DrawScope.drawVitalsLineSeries(
    points: List<VitalsLinePoint>,
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

private fun DrawScope.drawSelectedDateHighlight(
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

private fun paddedAxisRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
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
