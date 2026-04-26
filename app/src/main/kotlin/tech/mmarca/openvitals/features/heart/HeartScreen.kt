package tech.mmarca.openvitals.features.heart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.data.model.DailyHrv
import tech.mmarca.openvitals.data.model.DailyRestingHR
import tech.mmarca.openvitals.data.model.HeartRateSample
import tech.mmarca.openvitals.data.model.HeartRateSummary
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartScreen(viewModel: HeartViewModel) {
    val state by viewModel.uiState.collectAsState()
    val dayRestingBpm = state.dayRestingBpm
    val dayHrvMs = state.dayHrvMs
    val requestVitalsPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onVitalsPermissionsResult(granted)
    }

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
    ) { period ->
        when {
            state.selectedRange == TimeRange.DAY && state.daySamples.isNotEmpty() -> {
                item {
                    HeartRateTimelineCard(
                        date = state.selectedDate,
                        samples = state.daySamples,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            state.selectedRange == TimeRange.DAY && !state.isLoading -> {
                item {
                    HeartRateEmptyDayCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            state.dailySummaries.isNotEmpty() -> {
                item {
                    HeartRateChart(
                        summaries = state.dailySummaries,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item { SectionHeader("Daily breakdown") }
                val restingByDate = state.dailyRestingHR.associateBy { it.date }
                val hrvByDate = state.dailyHrv.associateBy { it.date }
                items(state.dailySummaries.sortedByDescending { it.date }) { summary ->
                    HeartRateDayRow(
                        summary = summary,
                        restingBpm = restingByDate[summary.date]?.bpm,
                        hrvMs = hrvByDate[summary.date]?.rmssdMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            !state.isLoading -> {
                item {
                    Text(
                        text = "No heart rate data in the selected period.\n\nMake sure the heart rate permission is granted and a connected device has synced data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (state.selectedRange == TimeRange.DAY && dayRestingBpm != null) {
            item {
                RestingHRDayCard(
                    bpm = dayRestingBpm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange == TimeRange.DAY && dayHrvMs != null) {
            item {
                HRVDayCard(
                    rmssdMs = dayHrvMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange != TimeRange.DAY && state.dailyRestingHR.isNotEmpty()) {
            item { SectionHeader("Resting heart rate") }
            item {
                RestingHRChart(
                    entries = state.dailyRestingHR,
                    selectedRange = state.selectedRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange != TimeRange.DAY && state.dailyHrv.isNotEmpty()) {
            item { SectionHeader("Heart rate variability (HRV)") }
            item {
                HRVChart(
                    entries = state.dailyHrv,
                    selectedRange = state.selectedRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item { SectionHeader("Vitals") }
        HeartVitalsContent(
            state = state,
            phase3Permissions = viewModel.vitalsPermissions,
            onGrantPermissions = requestVitalsPermissions::launch,
            selectedRange = state.selectedRange,
            period = period,
        )
    }
}

@Composable
private fun HeartRateChart(
    summaries: List<HeartRateSummary>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    val sorted = summaries.sortedBy { it.date }
    val maxBpm = sorted.maxOfOrNull { it.maxBpm } ?: 200L
    val minBpm = sorted.minOfOrNull { it.minBpm } ?: 40L
    val range = (maxBpm - minBpm).coerceAtLeast(1)
    val labelStride = when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> 1
        TimeRange.MONTH -> 5
        TimeRange.YEAR -> 30
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Average heart rate",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            val chartHeight = 120.dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                sorted.forEachIndexed { index, summary ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (index % labelStride == 0 || index == sorted.lastIndex) {
                            Text(
                                text = dayFormatter.format(summary.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (sorted.isNotEmpty()) {
                val avgAll = sorted.map { it.avgBpm }.average().roundToInt()
                val overallMin = sorted.minOf { it.minBpm }
                val overallMax = sorted.maxOf { it.maxBpm }
                Text(
                    text = "${periodTitle(selectedRange, period)} · Avg $avgAll bpm · range $overallMin-$overallMax bpm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeartRateTimelineCard(
    date: LocalDate,
    samples: List<HeartRateSample>,
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
                    label = "Avg",
                    value = "$avgBpm bpm",
                    modifier = Modifier.weight(1f),
                )
                HeartRateStat(
                    label = "Range",
                    value = "$minBpm-$maxBpm bpm",
                    modifier = Modifier.weight(1f),
                )
                HeartRateStat(
                    label = "Samples",
                    value = sorted.size.toString(),
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
                text = "${timeFormatter.format(firstSample)}-${timeFormatter.format(lastSample)} recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun HeartRateEmptyDayCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "No heart rate samples recorded on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Try another date or check that a connected device synced point-in-time heart data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeartRateDayRow(
    summary: HeartRateSummary,
    modifier: Modifier = Modifier,
    restingBpm: Long? = null,
    hrvMs: Double? = null,
) {
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
                    text = "${summary.avgBpm} bpm avg",
                    style = MaterialTheme.typography.titleSmall,
                    color = HeartColor,
                )
                Text(
                    text = "${summary.minBpm}-${summary.maxBpm} bpm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (restingBpm != null) {
                    Text(
                        text = "Resting $restingBpm bpm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hrvMs != null) {
                    Text(
                        text = "HRV ${"%.1f".format(hrvMs)} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestingHRDayCard(bpm: Long, modifier: Modifier = Modifier) {
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
                    text = "Resting heart rate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$bpm bpm",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HeartColor,
                )
            }
        }
    }
}

@Composable
private fun HRVDayCard(rmssdMs: Double, modifier: Modifier = Modifier) {
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
                    text = "Heart rate variability (HRV)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${"%.1f".format(rmssdMs)} ms RMSSD",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HeartColor,
                )
            }
        }
    }
}

@Composable
private fun RestingHRChart(
    entries: List<DailyRestingHR>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.date }
    val maxBpm = sorted.maxOfOrNull { it.bpm } ?: 80L
    val minBpm = sorted.minOfOrNull { it.bpm } ?: 40L
    val range = (maxBpm - minBpm).coerceAtLeast(1L)
    val labelStride = when (selectedRange) {
        TimeRange.WEEK -> 1
        TimeRange.MONTH -> 5
        TimeRange.YEAR -> 30
        else -> 1
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                sorted.forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (index % labelStride == 0 || index == sorted.lastIndex) {
                            Text(
                                text = dayFormatter.format(entry.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val avg = sorted.map { it.bpm }.average().roundToInt()
            Text(
                text = "Avg $avg bpm · range $minBpm-$maxBpm bpm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HRVChart(
    entries: List<DailyHrv>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.date }
    val maxMs = sorted.maxOfOrNull { it.rmssdMs } ?: 100.0
    val minMs = sorted.minOfOrNull { it.rmssdMs } ?: 0.0
    val range = (maxMs - minMs).coerceAtLeast(0.5)
    val labelStride = when (selectedRange) {
        TimeRange.WEEK -> 1
        TimeRange.MONTH -> 5
        TimeRange.YEAR -> 30
        else -> 1
    }

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
                points.forEach { pt -> drawCircle(color = HeartColor.copy(alpha = 0.7f), radius = 4.dp.toPx(), center = pt) }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                sorted.forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (index % labelStride == 0 || index == sorted.lastIndex) {
                            Text(
                                text = dayFormatter.format(entry.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val avg = sorted.map { it.rmssdMs }.average()
            Text(
                text = "Avg ${"%.1f".format(avg)} ms · range ${"%.1f".format(minMs)}-${"%.1f".format(maxMs)} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
