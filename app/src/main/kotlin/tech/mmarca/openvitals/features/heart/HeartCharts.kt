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
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import kotlin.math.roundToInt

@Composable
internal fun HeartRateChart(
    summaries: List<HeartRateSummary>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = summaries.sortedBy { it.date }
    val maxBpm = sorted.maxOfOrNull { it.maxBpm } ?: 200L
    val minBpm = sorted.minOfOrNull { it.minBpm } ?: 40L
    val range = (maxBpm - minBpm).coerceAtLeast(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metric_average_heart_rate),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)

                sorted.forEachIndexed { index, summary ->
                    val x = index * stepX
                    val yMin = size.height * (1f - (summary.minBpm - minBpm).toFloat() / range)
                    val yMax = size.height * (1f - (summary.maxBpm - minBpm).toFloat() / range)
                    drawLine(
                        color = HeartColor.copy(alpha = 0.25f),
                        start = Offset(x, yMax),
                        end = Offset(x, yMin),
                        strokeWidth = if (sorted.size <= 7) 12.dp.toPx() else 4.dp.toPx(),
                    )
                }

                val avgPoints = sorted.mapIndexed { index, summary ->
                    val x = index * stepX
                    val y = size.height * (1f - (summary.avgBpm - minBpm).toFloat() / range)
                    Offset(x, y)
                }
                for (index in 0 until avgPoints.size - 1) {
                    drawLine(
                        color = HeartColor,
                        start = avgPoints[index],
                        end = avgPoints[index + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                avgPoints.forEach { point ->
                    drawCircle(color = HeartColor, radius = 4.dp.toPx(), center = point)
                }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = sorted.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            if (sorted.isNotEmpty()) {
                val avgAll = sorted.map { it.avgBpm }.average().roundToInt()
                val overallMin = sorted.minOf { it.minBpm }
                val overallMax = sorted.maxOf { it.maxBpm }
                Text(
                    text = "${localizedPeriodTitle(selectedRange, period)} · ${
                        stringResource(
                            R.string.summary_avg_value_range,
                            unitFormatter.heartRate(avgAll.toLong()).text,
                            unitFormatter.heartRate(overallMin).text,
                            unitFormatter.heartRate(overallMax).text,
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun RestingHRChart(
    entries: List<DailyRestingHR>,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.date }
    val maxBpm = sorted.maxOfOrNull { it.bpm } ?: 80L
    val minBpm = sorted.minOfOrNull { it.bpm } ?: 40L
    val range = (maxBpm - minBpm).coerceAtLeast(1L)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - (entry.bpm - minBpm).toFloat() / range.toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = HeartColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt -> drawCircle(color = HeartColor, radius = 4.dp.toPx(), center = pt) }
            }
            Spacer(Modifier.height(4.dp))
            PeriodChartXAxis(
                dates = sorted.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(4.dp))
            val avg = sorted.map { it.bpm }.average().roundToInt()
            Text(
                text = stringResource(
                    R.string.summary_avg_value_range,
                    unitFormatter.heartRate(avg.toLong()).text,
                    unitFormatter.heartRate(minBpm).text,
                    unitFormatter.heartRate(maxBpm).text,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HRVChart(
    entries: List<DailyHrv>,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.date }
    val maxMs = sorted.maxOfOrNull { it.rmssdMs } ?: 100.0
    val minMs = sorted.minOfOrNull { it.rmssdMs } ?: 0.0
    val range = (maxMs - minMs).coerceAtLeast(0.5)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - ((entry.rmssdMs - minMs) / range).toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = HeartColor.copy(alpha = 0.7f),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt ->
                    drawCircle(color = HeartColor.copy(alpha = 0.7f), radius = 4.dp.toPx(), center = pt)
                }
            }
            Spacer(Modifier.height(4.dp))
            PeriodChartXAxis(
                dates = sorted.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(4.dp))
            val avg = sorted.map { it.rmssdMs }.average()
            Text(
                text = stringResource(
                    R.string.summary_avg_value_range,
                    unitFormatter.hrv(avg).text,
                    unitFormatter.hrv(minMs).text,
                    unitFormatter.hrv(maxMs).text,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
