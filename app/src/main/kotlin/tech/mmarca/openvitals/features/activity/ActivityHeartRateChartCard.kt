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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import androidx.health.connect.client.records.ExerciseSegment
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SessionAxis
import tech.mmarca.openvitals.ui.components.SessionAxisLabels
import tech.mmarca.openvitals.ui.components.SessionPause
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.HeartColor

@Composable
internal fun ActivityHeartRateChartCard(
    samples: List<HeartRateSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (samples.isEmpty()) return

    val sorted = samples.sortedBy { it.time }
    val minBpm = sorted.minOf { it.beatsPerMinute }
    val maxBpm = sorted.maxOf { it.beatsPerMinute }
    val avgBpm = sorted.map { it.beatsPerMinute }.average().roundToInt()
    val paddedMin = (minBpm - 5L).coerceAtLeast(30L)
    val paddedMax = maxBpm + 5L
    // The axis counts only moving time: a pause is not part of the ride, so it
    // gets none of the chart. Heart rate keeps sampling through a pause (a strap
    // does not stop because the ride did); those samples stack at the moment the
    // pause began rather than stretching it back open.
    val axis = remember(sessionStart, sessionEnd, pauses) {
        SessionAxis(start = sessionStart, end = sessionEnd, pauses = pauses)
    }
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    // Built once, identity-stable across pinch frames, so the plot's geometry
    // cache holds.
    val chartPoints = remember(sorted, axis) {
        sorted.map { sample ->
            MetricLinePlotPoint(
                xFraction = axis.fractionOf(sample.time),
                value = sample.beatsPerMinute.toDouble(),
            )
        }
    }

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
            // The plot and its elapsed row share the one viewport, so the labels
            // always describe the slice of the session actually on show.
            ChartZoom(sessionStart, sessionEnd, samples) { zoom ->
                Column {
                    MetricLinePlot(
                        points = chartPoints,
                        minValue = paddedMin.toDouble(),
                        maxValue = paddedMax.toDouble(),
                        accentColor = HeartColor,
                        chartHeight = chartHeight,
                        valueFormatter = { unitFormatter.heartRate(it.roundToLong()).text },
                        pointRadius = if (sorted.size <= 120) 2.dp else 0.dp,
                        lineStrokeWidth = 2.dp,
                        drawPoints = sorted.size <= 120,
                        viewport = zoom.viewport,
                        multiTouch = zoom.multiTouch,
                        // Moving elapsed, matching the labels directly under it:
                        // reporting wall-clock here would have the scrubber
                        // disagree with its own axis.
                        scrubLabel = { point ->
                            unitFormatter.heartRate(point.value.roundToLong()).text to
                                formatElapsedChartLabel(axis.elapsedAt(point.xFraction))
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    ChartXAxisWithYAxis {
                        SessionAxisLabels(axis = axis, viewport = zoom.viewport)
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

/** The pause segments of a workout, as the pauses the session axis skips. */
internal fun List<ExerciseSegmentData>.toSessionPauses(): List<SessionPause> =
    filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
        .map { SessionPause(start = it.startTime, end = it.endTime) }
