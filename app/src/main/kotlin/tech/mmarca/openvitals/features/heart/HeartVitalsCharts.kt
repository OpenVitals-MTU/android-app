package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.YAxisChart
import tech.mmarca.openvitals.ui.components.chartYAxisLabels
import tech.mmarca.openvitals.ui.components.drawYAxisGuides
import tech.mmarca.openvitals.ui.components.formatCompactAxisValue
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate
import kotlin.math.roundToInt

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
    val dailyValues = entries
        .groupBy { it.time.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(date = date, value = dayEntries.map { it.breathsPerMinute }.average())
        }
    val average = respiratoryRateAverage(respiratoryRateBuckets(entries, selectedRange, period))

    PeriodHistoryChart(
        title = stringResource(R.string.metric_respiratory_rate),
        values = dailyValues,
        selectedRange = selectedRange,
        period = period,
        accentColor = respiratoryColor.copy(alpha = 0.85f),
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
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val max = sorted.maxOfOrNull { it.systolicMmHg }?.coerceAtLeast(140) ?: 140
    val min = sorted.minOfOrNull { it.diastolicMmHg }?.coerceAtMost(60) ?: 60
    val range = (max - min).coerceAtLeast(1)
    val chartHeight = 150.dp
    val gridColor = VitalsColor.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.metric_blood_pressure), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            YAxisChart(
                labels = chartYAxisLabels(
                    minValue = min.toDouble(),
                    maxValue = max.toDouble(),
                    valueFormatter = { "${it.roundToInt()} mmHg" },
                ),
                chartHeight = chartHeight,
            ) {
                drawYAxisGuides(
                    gridColor = gridColor,
                    axisColor = axisColor,
                    strokeWidth = 1.dp.toPx(),
                )
                if (sorted.isEmpty()) return@YAxisChart
                val stepX = if (sorted.size > 1) size.width / (sorted.size - 1) else size.width
                sorted.forEachIndexed { index, entry ->
                    val x = if (sorted.size > 1) index * stepX else size.width / 2f
                    val ySystolic = size.height * (1f - (entry.systolicMmHg - min).toFloat() / range)
                    val yDiastolic = size.height * (1f - (entry.diastolicMmHg - min).toFloat() / range)
                    drawLine(
                        color = VitalsColor.copy(alpha = 0.35f),
                        start = Offset(x, ySystolic),
                        end = Offset(x, yDiastolic),
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = VitalsColor, radius = 4.dp.toPx(), center = Offset(x, ySystolic))
                    drawCircle(color = HeartColor, radius = 4.dp.toPx(), center = Offset(x, yDiastolic))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${localizedPeriodTitle(selectedRange, period)} · ${
                    stringResource(R.string.summary_readings, unitFormatter.count(sorted.size))
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun VitalsLineChart(
    title: String,
    values: List<Double>,
    dates: List<LocalDate>,
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
    PeriodHistoryChart(
        title = title,
        values = dailyAverageChartValues(values, dates),
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor.copy(alpha = 0.85f),
        summaryText = summary,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}

private fun dailyAverageChartValues(
    values: List<Double>,
    dates: List<LocalDate>,
): List<PeriodChartValue> =
    dates
        .zip(values)
        .groupBy({ it.first }, { it.second })
        .map { (date, dayValues) ->
            PeriodChartValue(date = date, value = dayValues.average())
        }
