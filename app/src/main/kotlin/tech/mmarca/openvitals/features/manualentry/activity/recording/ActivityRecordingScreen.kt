package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityRecordingScreen(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onAddLap: () -> Unit,
    onAddMarker: () -> Unit,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
    onAdjustRepetitionCount: (Long) -> Unit,
    onEndRepetitionSet: () -> Unit,
    onStartNextRepetitionSet: () -> Unit,
    onFinishRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(state.status) {
        while (state.isActive) {
            now = Instant.now()
            delay(1_000L)
        }
    }

    val movingTime = state.movingDuration(now)
    val totalTime = if (state.recordingKind == ActivityRecordingKind.REPETITION) {
        movingTime.plus(state.restDuration(now))
    } else {
        state.elapsedDuration(now)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.activity_entry_recording_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(state.recordingStatusLabelRes(now)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.recordingKind == ActivityRecordingKind.REPETITION) {
            RepetitionRecordingStats(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                unitFormatter = unitFormatter,
                onAdjustRepetitionCount = onAdjustRepetitionCount,
                onEndRepetitionSet = onEndRepetitionSet,
                onStartNextRepetitionSet = onStartNextRepetitionSet,
                onFinishRecording = onFinishRecording,
                onDiscardRecording = onDiscardRecording,
            )
        } else {
            GpsRecordingTabs(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                now = now,
                unitFormatter = unitFormatter,
                onUpdateMarker = onUpdateMarker,
                onDeleteMarker = onDeleteMarker,
            )
        }

        state.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.recordingKind == ActivityRecordingKind.GPS_ROUTE) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Text(
                    text = stringResource(R.string.activity_entry_recording_finish_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.status == ActivityRecordingStatus.PAUSED) {
                        OutlinedButton(
                            onClick = onResumeRecording,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.action_resume),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPauseRecording,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.action_pause),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }

                    Button(
                        onClick = onFinishRecording,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_finish),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onAddLap,
                            enabled = state.points.size >= 2,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.activity_entry_recording_lap),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        OutlinedButton(
                            onClick = onAddMarker,
                            enabled = state.latestUiPoint != null || state.points.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.activity_entry_recording_marker),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }

                OutlinedButton(
                    onClick = onDiscardRecording,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.action_discard),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun GpsRecordingTabs(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ActivityRecordingTab.STATS) }
    var timeSplitMinutes by rememberSaveable { mutableIntStateOf(DefaultTimeSplitMinutes) }
    val unitSystem = unitFormatter.unitSystem()
    var distanceSplitMeters by rememberSaveable(unitSystem) {
        mutableDoubleStateOf(defaultDistanceSplitMeters(unitSystem))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActivityRecordingTabRow(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
        )

        when (selectedTab) {
            ActivityRecordingTab.STATS -> RecordingStatsTab(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                now = now,
                unitFormatter = unitFormatter,
                onUpdateMarker = onUpdateMarker,
                onDeleteMarker = onDeleteMarker,
            )
            ActivityRecordingTab.INTERVALS -> RecordingSplitsTab(
                splits = if (state.manualLaps.isNotEmpty()) {
                    activityRecordingLapSplits(
                        laps = state.manualLaps,
                        points = state.points,
                        routeBreakIndexes = state.routeBreakIndexes,
                        recordingStartTime = state.startTime,
                        activeEndTime = now,
                    )
                } else {
                    activityRecordingIntervalSplits(state.points, state.routeBreakIndexes)
                },
                emptyMessage = stringResource(R.string.activity_entry_recording_no_intervals),
                unitFormatter = unitFormatter,
                label = { split -> stringResource(R.string.activity_entry_recording_split_interval, split.index) },
            )
            ActivityRecordingTab.BY_TIME -> RecordingSplitsTab(
                splits = activityRecordingTimeSplits(
                    points = state.points,
                    routeBreakIndexes = state.routeBreakIndexes,
                    splitMillis = timeSplitMinutes * 60_000L,
                ),
                emptyMessage = stringResource(R.string.activity_entry_recording_no_time_splits),
                unitFormatter = unitFormatter,
                label = { split ->
                    val startMinute = (split.index - 1) * timeSplitMinutes
                    val endMinute = split.index * timeSplitMinutes
                    stringResource(R.string.activity_entry_recording_split_time_range, startMinute, endMinute)
                },
                controls = {
                    TimeSplitSelector(
                        selectedMinutes = timeSplitMinutes,
                        onSelect = { timeSplitMinutes = it },
                    )
                },
            )
            ActivityRecordingTab.BY_DISTANCE -> RecordingSplitsTab(
                splits = activityRecordingDistanceSplits(
                    points = state.points,
                    routeBreakIndexes = state.routeBreakIndexes,
                    splitMeters = distanceSplitMeters,
                ),
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
                controls = {
                    DistanceSplitSelector(
                        selectedMeters = distanceSplitMeters,
                        unitSystem = unitSystem,
                        unitFormatter = unitFormatter,
                        onSelect = { distanceSplitMeters = it },
                    )
                },
            )
        }
    }
}

@Composable
private fun ActivityRecordingTabRow(
    selectedTab: ActivityRecordingTab,
    onSelect: (ActivityRecordingTab) -> Unit,
) {
    val tabs = ActivityRecordingTab.entries
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = WorkoutColor,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

@Composable
private fun RecordingStatsTab(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
) {
    val distance = unitFormatter.distance(state.distanceMeters)
    val elevation = unitFormatter.elevation(state.displayElevationGainedMeters())
    val speed = unitFormatter.speed(state.effectiveCurrentSpeedMetersPerSecond(now))
    val maxSpeed = unitFormatter.speed(state.maxSpeedMetersPerSecond)
    val averageSpeed = unitFormatter.averageSpeed(state.distanceMeters, totalTime.toMillis())
    val averageMovingSpeed = unitFormatter.averageSpeed(state.distanceMeters, movingTime.toMillis())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecordingStat(
                    value = distance,
                    label = stringResource(R.string.activity_entry_recording_distance),
                    modifier = Modifier.weight(1f),
                )
                RecordingStat(
                    value = DisplayValue(formatRecordingElapsed(totalTime), ""),
                    label = stringResource(R.string.activity_entry_recording_total_time),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecordingStat(
                    value = speed,
                    label = stringResource(R.string.activity_entry_recording_speed),
                    modifier = Modifier.weight(1f),
                )
                RecordingStat(
                    value = DisplayValue(formatRecordingElapsed(movingTime), ""),
                    label = stringResource(R.string.activity_entry_recording_moving_time),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecordingStat(
                    value = averageSpeed,
                    label = stringResource(R.string.activity_entry_recording_average_speed),
                    modifier = Modifier.weight(1f),
                )
                RecordingStat(
                    value = averageMovingSpeed,
                    label = stringResource(R.string.activity_entry_recording_average_moving_speed),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecordingStat(
                    value = elevation,
                    label = stringResource(R.string.activity_entry_recording_elevation_gain),
                    modifier = Modifier.weight(1f),
                )
                RecordingStat(
                    value = maxSpeed,
                    label = stringResource(R.string.activity_entry_recording_max_speed),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.lastAccuracyMeters?.let { accuracyMeters ->
            Text(
                text = stringResource(
                    R.string.activity_entry_recording_accuracy,
                    unitFormatter.elevation(accuracyMeters).text,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RecordingMarkersList(
            markers = state.markers,
            unitFormatter = unitFormatter,
            onUpdateMarker = onUpdateMarker,
            onDeleteMarker = onDeleteMarker,
        )
    }
}

@Composable
private fun RecordingSplitsTab(
    splits: List<ActivityRecordingSplit>,
    emptyMessage: String,
    unitFormatter: UnitFormatter,
    label: @Composable (ActivityRecordingSplit) -> String,
    controls: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        controls?.invoke()

        if (splits.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            splits.forEachIndexed { index, split ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                RecordingSplitRow(
                    split = split,
                    label = label(split),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordingMarkersList(
    markers: List<ActivityRecordingMarker>,
    unitFormatter: UnitFormatter,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
) {
    if (markers.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.activity_entry_recording_markers_title),
            style = MaterialTheme.typography.titleSmall,
        )
        markers.sortedBy { it.time }.forEachIndexed { index, marker ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = marker.name,
                        onValueChange = { name -> onUpdateMarker(marker.copy(name = name)) },
                        label = { Text(stringResource(R.string.activity_entry_recording_marker_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDeleteMarker(marker.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                }
                OutlinedTextField(
                    value = marker.note,
                    onValueChange = { note -> onUpdateMarker(marker.copy(note = note)) },
                    label = { Text(stringResource(R.string.activity_entry_recording_marker_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = marker.locationSummary(unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordingSplitRow(
    split: ActivityRecordingSplit,
    label: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val distance = unitFormatter.distance(split.distanceMeters)
    val elapsed = DisplayValue(formatRecordingElapsed(Duration.ofMillis(split.elapsedMillis)), "")
    val averageSpeed = unitFormatter.speed(split.averageSpeedMetersPerSecond)
    val maxSpeed = unitFormatter.speed(split.maxSpeedMetersPerSecond)
    val climb = unitFormatter.elevation(split.climbMeters)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_distance),
                value = distance,
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_elapsed),
                value = elapsed,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_avg),
                value = averageSpeed,
                modifier = Modifier.weight(1f),
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_max),
                value = maxSpeed,
                modifier = Modifier.weight(1f),
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_elevation_gain),
                value = climb,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun ActivityRecordingMarker.locationSummary(unitFormatter: UnitFormatter): String {
    val coordinate = "%.5f, %.5f".format(Locale.US, latitude, longitude)
    val altitude = altitudeMeters?.let { " • ${unitFormatter.elevation(it).text}" }.orEmpty()
    return coordinate + altitude
}

@Composable
private fun CompactSplitMetric(
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
private fun TimeSplitSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
) {
    SplitSelector(
        title = stringResource(R.string.activity_entry_recording_time_split),
        options = TimeSplitMinuteOptions,
        selected = selectedMinutes,
        label = { minutes -> stringResource(R.string.activity_entry_recording_split_minutes, minutes) },
        onSelect = onSelect,
    )
}

@Composable
private fun DistanceSplitSelector(
    selectedMeters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
    onSelect: (Double) -> Unit,
) {
    SplitSelector(
        title = stringResource(R.string.activity_entry_recording_distance_split),
        options = distanceSplitOptions(unitSystem),
        selected = selectedMeters,
        label = { meters -> distanceSplitOptionLabel(meters, unitSystem, unitFormatter) },
        onSelect = onSelect,
    )
}

@Composable
private fun <T> SplitSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = { Text(label(option)) },
                )
            }
        }
    }
}


@Composable
internal fun RecordingStat(
    value: DisplayValue,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.value,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
            )
            if (value.unit.isNotBlank()) {
                Text(
                    text = value.unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
                )
            }
        }
        AutoResizeText(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = WorkoutColor,
            maxLines = 1,
        )
    }
}

internal fun formatRecordingElapsed(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private val ActivityRecordingTab.labelRes: Int
    get() = when (this) {
        ActivityRecordingTab.STATS -> R.string.activity_entry_recording_tab_stats
        ActivityRecordingTab.INTERVALS -> R.string.activity_entry_recording_tab_intervals
        ActivityRecordingTab.BY_TIME -> R.string.activity_entry_recording_tab_by_time
        ActivityRecordingTab.BY_DISTANCE -> R.string.activity_entry_recording_tab_by_distance
    }

private fun defaultDistanceSplitMeters(unitSystem: UnitSystem): Double =
    when (unitSystem) {
        UnitSystem.METRIC -> 1_000.0
        UnitSystem.IMPERIAL -> MetersPerMile
    }

private fun distanceSplitOptions(unitSystem: UnitSystem): List<Double> =
    when (unitSystem) {
        UnitSystem.METRIC -> listOf(500.0, 1_000.0, 5_000.0)
        UnitSystem.IMPERIAL -> listOf(0.5 * MetersPerMile, MetersPerMile, 5.0 * MetersPerMile)
    }

private fun distanceSplitOptionLabel(
    meters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> meters / 1_000.0
        UnitSystem.IMPERIAL -> meters / MetersPerMile
    }
    val unit = when (unitSystem) {
        UnitSystem.METRIC -> "km"
        UnitSystem.IMPERIAL -> "mi"
    }
    return "${unitFormatter.decimal(value, splitDistanceDecimals(value))} $unit"
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

private fun ActivityRecordingState.recordingStatusLabelRes(now: Instant): Int =
    when {
        status == ActivityRecordingStatus.PAUSED -> R.string.activity_entry_recording_paused
        status == ActivityRecordingStatus.RESTING -> R.string.activity_entry_recording_resting
        recordingKind != ActivityRecordingKind.GPS_ROUTE -> R.string.activity_entry_recording_active
        isAutoIdle(now) -> R.string.activity_entry_recording_idle
        gpsStatus == ActivityGpsStatus.FIX -> R.string.activity_entry_recording_gps_fix
        gpsStatus == ActivityGpsStatus.POOR_ACCURACY -> R.string.activity_entry_recording_gps_poor
        gpsStatus == ActivityGpsStatus.LOST -> R.string.activity_entry_recording_gps_lost
        gpsStatus == ActivityGpsStatus.DISABLED -> R.string.activity_entry_recording_gps_off
        gpsStatus == ActivityGpsStatus.WAITING_FOR_FIX -> R.string.activity_entry_recording_waiting_for_gps
        else -> R.string.activity_entry_recording_active
    }

private const val DefaultTimeSplitMinutes = 5
private val TimeSplitMinuteOptions = listOf(1, 5, 10)
private const val MetersPerMile = 1_609.344
