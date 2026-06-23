package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingSplit
import tech.mmarca.openvitals.features.manualentry.activity.recording.activityRecordingDistanceSplits
import tech.mmarca.openvitals.features.manualentry.activity.recording.activityRecordingIntervalSplits
import tech.mmarca.openvitals.features.manualentry.activity.recording.activityRecordingTimeSplits
import tech.mmarca.openvitals.features.manualentry.activity.recording.exerciseLapSplits
import tech.mmarca.openvitals.features.manualentry.activity.recording.formatRecordingElapsed
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityRouteAnalysisCard(
    workout: ExerciseData,
    markers: List<ActivityRecordingMarker>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val routePoints = workout.route.takeIf { it.status == ExerciseRouteStatus.DATA }?.points.orEmpty()
    if (routePoints.size < 2 && markers.isEmpty()) return

    var selectedTab by rememberSaveable { mutableStateOf(SavedActivityAnalysisTab.STATS) }
    val unitSystem = unitFormatter.unitSystem()
    val timeSplits = activityRecordingTimeSplits(
        points = routePoints,
        routeBreakIndexes = emptyList(),
        splitMillis = SavedTimeSplitMinutes * 60_000L,
    )
    val distanceSplitMeters = when (unitSystem) {
        UnitSystem.METRIC -> 1_000.0
        UnitSystem.IMPERIAL -> MetersPerMile
    }
    val distanceSplits = activityRecordingDistanceSplits(
        points = routePoints,
        routeBreakIndexes = emptyList(),
        splitMeters = distanceSplitMeters,
    )
    val intervalSplits = if (workout.laps.isNotEmpty()) {
        exerciseLapSplits(workout.laps, routePoints)
    } else {
        activityRecordingIntervalSplits(routePoints, emptyList())
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_detail_analysis_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            PrimaryScrollableTabRow(
                selectedTabIndex = SavedActivityAnalysisTab.entries.indexOf(selectedTab),
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = WorkoutColor,
            ) {
                SavedActivityAnalysisTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }

            when (selectedTab) {
                SavedActivityAnalysisTab.STATS -> SavedStatsTab(
                    workout = workout,
                    intervalSplits = intervalSplits,
                    unitFormatter = unitFormatter,
                )
                SavedActivityAnalysisTab.INTERVALS -> SavedSplitsTab(
                    splits = intervalSplits,
                    emptyMessage = stringResource(R.string.activity_entry_recording_no_intervals),
                    unitFormatter = unitFormatter,
                    label = { split -> stringResource(R.string.activity_entry_recording_split_interval, split.index) },
                )
                SavedActivityAnalysisTab.BY_TIME -> SavedSplitsTab(
                    splits = timeSplits,
                    emptyMessage = stringResource(R.string.activity_entry_recording_no_time_splits),
                    unitFormatter = unitFormatter,
                    label = { split ->
                        val startMinute = (split.index - 1) * SavedTimeSplitMinutes
                        val endMinute = split.index * SavedTimeSplitMinutes
                        stringResource(R.string.activity_entry_recording_split_time_range, startMinute, endMinute)
                    },
                )
                SavedActivityAnalysisTab.BY_DISTANCE -> SavedSplitsTab(
                    splits = distanceSplits,
                    emptyMessage = stringResource(R.string.activity_entry_recording_no_distance_splits),
                    unitFormatter = unitFormatter,
                    label = { split ->
                        split.endDistanceMeters?.let { endDistance ->
                            distanceRangeLabel(
                                startMeters = split.startDistanceMeters,
                                endMeters = endDistance,
                                unitSystem = unitSystem,
                                unitFormatter = unitFormatter,
                            )
                        } ?: stringResource(R.string.activity_entry_recording_split_interval, split.index)
                    },
                )
                SavedActivityAnalysisTab.MARKERS -> SavedMarkersTab(
                    markers = markers,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }
        }
    }
}

@Composable
private fun SavedStatsTab(
    workout: ExerciseData,
    intervalSplits: List<ActivityRecordingSplit>,
    unitFormatter: UnitFormatter,
) {
    val totalDistance = workout.totalDistanceMeters ?: intervalSplits.sumOf { it.distanceMeters }
    val maxSpeed = intervalSplits.maxOfOrNull { it.maxSpeedMetersPerSecond } ?: 0.0
    val climb = workout.elevationGainedMeters ?: intervalSplits.sumOf { it.climbMeters }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SavedDetailRow(stringResource(R.string.metric_distance), unitFormatter.distance(totalDistance).text)
        SavedDetailRow(stringResource(R.string.detail_duration), unitFormatter.duration(workout.durationMs))
        SavedDetailRow(stringResource(R.string.metric_average_speed), unitFormatter.averageSpeed(totalDistance, workout.durationMs).text)
        SavedDetailRow(stringResource(R.string.activity_entry_recording_max_speed), unitFormatter.speed(maxSpeed).text)
        SavedDetailRow(stringResource(R.string.activity_entry_recording_elevation_gain), unitFormatter.elevation(climb).text)
    }
}

@Composable
private fun SavedSplitsTab(
    splits: List<ActivityRecordingSplit>,
    emptyMessage: String,
    unitFormatter: UnitFormatter,
    label: @Composable (ActivityRecordingSplit) -> String,
) {
    if (splits.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        splits.forEachIndexed { index, split ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SavedSplitRow(
                label = label(split),
                split = split,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SavedSplitRow(
    label: String,
    split: ActivityRecordingSplit,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            SavedCompactMetric(
                label = stringResource(R.string.activity_entry_recording_distance),
                value = unitFormatter.distance(split.distanceMeters),
            )
            SavedCompactMetric(
                label = stringResource(R.string.activity_entry_recording_split_elapsed),
                value = DisplayValue(formatRecordingElapsed(Duration.ofMillis(split.elapsedMillis)), ""),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SavedCompactMetric(
                label = stringResource(R.string.activity_entry_recording_split_avg),
                value = unitFormatter.speed(split.averageSpeedMetersPerSecond),
                modifier = Modifier.weight(1f),
            )
            SavedCompactMetric(
                label = stringResource(R.string.activity_entry_recording_split_max),
                value = unitFormatter.speed(split.maxSpeedMetersPerSecond),
                modifier = Modifier.weight(1f),
            )
            SavedCompactMetric(
                label = stringResource(R.string.activity_entry_recording_elevation_gain),
                value = unitFormatter.elevation(split.climbMeters),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SavedMarkersTab(
    markers: List<ActivityRecordingMarker>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (markers.isEmpty()) {
        Text(
            text = stringResource(R.string.activity_detail_no_markers),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }
    val zone = ZoneId.systemDefault()
    Column(modifier = Modifier.fillMaxWidth()) {
        markers.sortedBy { it.time }.forEachIndexed { index, marker ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(marker.name, style = MaterialTheme.typography.titleSmall)
                if (marker.note.isNotBlank()) {
                    Text(marker.note, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = listOf(
                        dateTimeFormatterProvider.shortTime().format(marker.time.atZone(zone)),
                        marker.locationSummary(unitFormatter),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedCompactMetric(
    label: String,
    value: DisplayValue,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = if (value.unit.isBlank()) value.value else "${value.value} ${value.unit}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SavedDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

private enum class SavedActivityAnalysisTab {
    STATS,
    INTERVALS,
    BY_TIME,
    BY_DISTANCE,
    MARKERS,
}

private val SavedActivityAnalysisTab.labelRes: Int
    get() = when (this) {
        SavedActivityAnalysisTab.STATS -> R.string.activity_entry_recording_tab_stats
        SavedActivityAnalysisTab.INTERVALS -> R.string.activity_entry_recording_tab_intervals
        SavedActivityAnalysisTab.BY_TIME -> R.string.activity_entry_recording_tab_by_time
        SavedActivityAnalysisTab.BY_DISTANCE -> R.string.activity_entry_recording_tab_by_distance
        SavedActivityAnalysisTab.MARKERS -> R.string.activity_detail_tab_markers
    }

private fun ActivityRecordingMarker.locationSummary(unitFormatter: UnitFormatter): String {
    val coordinate = "%.5f, %.5f".format(Locale.US, latitude, longitude)
    val altitude = altitudeMeters?.let { " • ${unitFormatter.elevation(it).text}" }.orEmpty()
    return coordinate + altitude
}

private fun distanceRangeLabel(
    startMeters: Double,
    endMeters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
): String {
    val divisor = when (unitSystem) {
        UnitSystem.METRIC -> 1_000.0
        UnitSystem.IMPERIAL -> MetersPerMile
    }
    val unit = when (unitSystem) {
        UnitSystem.METRIC -> "km"
        UnitSystem.IMPERIAL -> "mi"
    }
    val start = startMeters / divisor
    val end = endMeters / divisor
    return "${unitFormatter.decimal(start, splitDistanceDecimals(start))}-" +
        "${unitFormatter.decimal(end, splitDistanceDecimals(end))} $unit"
}

private fun splitDistanceDecimals(value: Double): Int =
    if (value < 1.0 || value % 1.0 != 0.0) 1 else 0

private const val SavedTimeSplitMinutes = 5
private const val MetersPerMile = 1_609.344
