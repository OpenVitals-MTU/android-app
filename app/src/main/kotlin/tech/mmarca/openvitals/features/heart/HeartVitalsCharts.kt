package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.Canvas
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
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate

@Composable
internal fun RespiratoryRateChart(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val buckets = respiratoryRateBuckets(entries, selectedRange, period)
    val plotted = buckets.mapIndexedNotNull { index, bucket ->
        bucket.value.takeIf { it > 0.0 }?.let { RespiratoryRatePlotPoint(index, it) }
    }
    val average = respiratoryRateAverage(buckets)
    val max = plotted.maxOfOrNull { it.value }?.plus(1.0) ?: 1.0
    val min = plotted.minOfOrNull { it.value }?.minus(1.0)?.coerceAtLeast(0.0) ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.metric_respiratory_rate), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (plotted.isEmpty()) return@Canvas

                val lastIndex = buckets.lastIndex.coerceAtLeast(1)
                val points = plotted.map { point ->
                    Offset(
                        x = point.index * size.width / lastIndex,
                        y = size.height * (1f - ((point.value - min) / range).toFloat()),
                    )
                }

                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = respiratoryColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point ->
                    drawCircle(color = respiratoryColor, radius = 4.dp.toPx(), center = point)
                }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = buckets.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${localizedPeriodTitle(selectedRange, period)} · ${
                    stringResource(R.string.summary_value_avg, unitFormatter.respiratoryRate(average).text)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.metric_blood_pressure), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                if (sorted.isEmpty()) return@Canvas
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
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val min = values.minOrNull()?.coerceAtMost(max - 1.0) ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (values.size < 2) return@Canvas
                val points = values.mapIndexed { index, value ->
                    Offset(
                        x = index * size.width / (values.size - 1),
                        y = size.height * (1f - ((value - min) / range).toFloat()),
                    )
                }
                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = accentColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point ->
                    drawCircle(color = accentColor, radius = 4.dp.toPx(), center = point)
                }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = dates,
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class RespiratoryRatePlotPoint(
    val index: Int,
    val value: Double,
)
