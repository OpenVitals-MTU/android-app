package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.ui.theme.HeartColor
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
internal fun HeartRateTimelineCard(
    date: LocalDate,
    samples: List<HeartRateSample>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val sorted = samples.sortedBy { it.time }
    val minBpm = sorted.minOfOrNull { it.beatsPerMinute } ?: 40L
    val maxBpm = sorted.maxOfOrNull { it.beatsPerMinute } ?: 160L
    val avgBpm = sorted.map { it.beatsPerMinute }.average().roundToInt()
    val paddedMin = (minBpm - 5L).coerceAtLeast(30L)
    val paddedMax = maxBpm + 5L
    val range = (paddedMax - paddedMin).coerceAtLeast(1L)
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    val dayDurationMillis = Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)
    val firstSample = sorted.first().time.atZone(zone)
    val lastSample = sorted.last().time.atZone(zone)
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeartRateStat(
                    label = stringResource(R.string.summary_average),
                    value = unitFormatter.heartRate(avgBpm.toLong()).text,
                    modifier = Modifier.weight(1f),
                )
                HeartRateStat(
                    label = stringResource(R.string.summary_range),
                    value = "${unitFormatter.heartRate(minBpm).text}-${unitFormatter.heartRate(maxBpm).text}",
                    modifier = Modifier.weight(1f),
                )
                HeartRateStat(
                    label = stringResource(R.string.summary_samples),
                    value = unitFormatter.count(sorted.size),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                repeat(4) { index ->
                    val y = size.height * index / 3f
                    drawLine(
                        color = HeartColor.copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val points = sorted.map { sample ->
                    val elapsed = Duration.between(dayStart, sample.time).toMillis()
                        .coerceIn(0L, dayDurationMillis)
                    val x = size.width * elapsed.toFloat() / dayDurationMillis
                    val y = size.height * (
                        1f - (sample.beatsPerMinute - paddedMin).toFloat() / range.toFloat()
                    )
                    Offset(x, y)
                }

                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = HeartColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point ->
                    drawCircle(
                        color = HeartColor,
                        radius = 3.dp.toPx(),
                        center = point,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.summary_recorded,
                    timeFormatter.format(firstSample),
                    timeFormatter.format(lastSample),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HeartRateEmptyDayCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.message_no_heart_samples_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.message_heart_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HeartRateDayRow(
    summary: HeartRateSummary,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    restingBpm: Long? = null,
    hrvMs: Double? = null,
) {
    val dayFormatter = dateTimeFormatterProvider.chartDay()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayFormatter.format(summary.date),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.summary_value_avg, unitFormatter.heartRate(summary.avgBpm).text),
                    style = MaterialTheme.typography.titleSmall,
                    color = HeartColor,
                )
                Text(
                    text = "${unitFormatter.heartRate(summary.minBpm).text}-${unitFormatter.heartRate(summary.maxBpm).text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (restingBpm != null) {
                    Text(
                        text = stringResource(R.string.summary_resting_value, unitFormatter.heartRate(restingBpm).text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hrvMs != null) {
                    Text(
                        text = stringResource(R.string.summary_hrv_value, unitFormatter.hrv(hrvMs).text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RestingHRDayCard(
    bpm: Long,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.metric_resting_heart_rate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = unitFormatter.heartRate(bpm).text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = HeartColor,
                )
            }
        }
    }
}

@Composable
internal fun HRVDayCard(
    rmssdMs: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.metric_hrv),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${unitFormatter.hrv(rmssdMs).text} RMSSD",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HeartColor,
                )
            }
        }
    }
}

@Composable
private fun HeartRateStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
