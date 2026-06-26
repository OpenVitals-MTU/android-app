package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.features.manualentry.activity.recording.formatRecordingElapsed
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.HeartColor

@Composable
internal fun ActivityHeartRateChartCard(
    samples: List<HeartRateSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val sorted = samples.sortedBy { it.time }
    val minBpm = sorted.minOf { it.beatsPerMinute }
    val maxBpm = sorted.maxOf { it.beatsPerMinute }
    val avgBpm = sorted.map { it.beatsPerMinute }.average().roundToInt()
    val paddedMin = (minBpm - 5L).coerceAtLeast(30L)
    val paddedMax = maxBpm + 5L
    val sessionDurationMillis = Duration.between(sessionStart, sessionEnd)
        .toMillis()
        .coerceAtLeast(1L)
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_recording_live_heart_rate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActivityHeartRateStat(
                    label = stringResource(R.string.summary_average),
                    value = unitFormatter.heartRate(avgBpm.toLong()).text,
                    modifier = Modifier.weight(1f),
                )
                ActivityHeartRateStat(
                    label = stringResource(R.string.summary_range),
                    value = "${unitFormatter.heartRate(minBpm).text}-${unitFormatter.heartRate(maxBpm).text}",
                    modifier = Modifier.weight(1f),
                )
                ActivityHeartRateStat(
                    label = stringResource(R.string.summary_samples),
                    value = unitFormatter.count(sorted.size),
                    modifier = Modifier.weight(1f),
                )
            }
            MetricLinePlot(
                points = sorted.map { sample ->
                    val elapsed = Duration.between(sessionStart, sample.time)
                        .toMillis()
                        .coerceIn(0L, sessionDurationMillis)
                    MetricLinePlotPoint(
                        xFraction = elapsed.toFloat() / sessionDurationMillis.toFloat(),
                        value = sample.beatsPerMinute.toDouble(),
                    )
                },
                minValue = paddedMin.toDouble(),
                maxValue = paddedMax.toDouble(),
                accentColor = HeartColor,
                chartHeight = chartHeight,
                valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
                pointRadius = if (sorted.size <= 120) 2.dp else 0.dp,
                lineStrokeWidth = 2.dp,
                drawPoints = sorted.size <= 120,
            )
            Spacer(Modifier.height(4.dp))
            ChartXAxisWithYAxis {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    sessionElapsedLabels(sessionDurationMillis).forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.summary_recorded,
                    timeFormatter.format(sorted.first().time.atZone(zone)),
                    timeFormatter.format(sorted.last().time.atZone(zone)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityHeartRateStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = HeartColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun BleRecordingSampleBuffer.toHeartRateSamples(): List<HeartRateSample> =
    heartRateSamples.map { sample ->
        HeartRateSample(
            time = sample.time,
            beatsPerMinute = sample.beatsPerMinute,
            source = "sensor",
        )
    }

internal fun List<BleHeartRateSample>.toHeartRateSamples(): List<HeartRateSample> =
    map { sample ->
        HeartRateSample(
            time = sample.time,
            beatsPerMinute = sample.beatsPerMinute,
            source = "sensor",
        )
    }

private fun sessionElapsedLabels(durationMillis: Long): List<String> {
    val duration = Duration.ofMillis(durationMillis)
    return listOf(
        formatRecordingElapsed(Duration.ZERO),
        formatRecordingElapsed(duration.dividedBy(4)),
        formatRecordingElapsed(duration.dividedBy(2)),
        formatRecordingElapsed(duration.multipliedBy(3).dividedBy(4)),
        formatRecordingElapsed(duration),
    )
}
