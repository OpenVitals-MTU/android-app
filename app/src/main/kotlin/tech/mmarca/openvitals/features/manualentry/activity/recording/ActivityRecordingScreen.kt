package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.content.ClipData
import android.content.ClipDescription
import android.location.Location
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface

@Composable
internal fun ActivityRecordingScreen(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onStartRecording: (Location?) -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onAddLap: () -> Unit,
    onAddMarker: () -> Unit,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
    onChooseSource: () -> Unit,
    onAdjustRepetitionCount: (Long) -> Unit,
    onEndRepetitionSet: () -> Unit,
    onStartNextRepetitionSet: () -> Unit,
    onFinishRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    var isEditingDashboard by rememberSaveable(state.activityTypeId) { mutableStateOf(false) }
    val canEditDashboard = state.recordingKind == ActivityRecordingKind.GPS_ROUTE &&
        (state.status == ActivityRecordingStatus.IDLE || state.status == ActivityRecordingStatus.PAUSED)
    val idleGpsFixState = rememberPreRecordingGpsFixState(
        enabled = state.recordingKind == ActivityRecordingKind.GPS_ROUTE &&
            state.status == ActivityRecordingStatus.IDLE,
    )
    LaunchedEffect(state.status) {
        if (state.status == ActivityRecordingStatus.RECORDING) {
            isEditingDashboard = false
        }
    }
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
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            if (
                state.recordingKind == ActivityRecordingKind.GPS_ROUTE &&
                state.status == ActivityRecordingStatus.IDLE
            ) {
                PreRecordingGpsFixStatus(state = idleGpsFixState)
            }
            if (canEditDashboard) {
                OpenVitalsIconButton(
                    onClick = { isEditingDashboard = !isEditingDashboard },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (isEditingDashboard) Icons.Outlined.Check else Icons.Outlined.Edit,
                        contentDescription = stringResource(
                            if (isEditingDashboard) {
                                R.string.cd_finish_recording_dashboard_editing
                            } else {
                                R.string.cd_edit_recording_dashboard
                            },
                        ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.recordingKind == ActivityRecordingKind.REPETITION) {
                RepetitionRecordingStats(
                    state = state,
                    totalTime = totalTime,
                    movingTime = movingTime,
                    unitFormatter = unitFormatter,
                    onAdjustRepetitionCount = onAdjustRepetitionCount,
                )
            } else {
                GpsRecordingTabs(
                    state = state,
                    totalTime = totalTime,
                    movingTime = movingTime,
                    now = now,
                    unitFormatter = unitFormatter,
                    isEditingDashboard = isEditingDashboard,
                    onUpdateDashboardLayout = onUpdateDashboardLayout,
                )
                RecordingMarkersList(
                    markers = state.markers,
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
        }

        if (state.recordingKind == ActivityRecordingKind.GPS_ROUTE) {
            GpsRecordingControls(
                state = state,
                canStartRecording = idleGpsFixState.latestPreciseFix != null,
                onStartRecording = { onStartRecording(idleGpsFixState.latestPreciseFix) },
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
                onFinishRecording = onFinishRecording,
                onAddLap = onAddLap,
                onAddMarker = onAddMarker,
                onChooseSource = onChooseSource,
            )
        } else {
            RepetitionRecordingControls(
                state = state,
                onEndRepetitionSet = onEndRepetitionSet,
                onStartNextRepetitionSet = onStartNextRepetitionSet,
                onFinishRecording = onFinishRecording,
            )
        }
    }
}

@Composable
private fun GpsRecordingControls(
    state: ActivityRecordingState,
    canStartRecording: Boolean,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onFinishRecording: () -> Unit,
    onAddLap: () -> Unit,
    onAddMarker: () -> Unit,
    onChooseSource: () -> Unit,
) {
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.status == ActivityRecordingStatus.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpenVitalsButton(
                        onClick = onStartRecording,
                        enabled = canStartRecording,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_start),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    OpenVitalsOutlinedButton(
                        onClick = onChooseSource,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                return@Column
            }

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
                    OpenVitalsOutlinedButton(
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
                    OpenVitalsOutlinedButton(
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

                OpenVitalsOutlinedButton(
                    onClick = onFinishRecording,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
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
                OpenVitalsOutlinedButton(
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
                OpenVitalsOutlinedButton(
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
    isEditingDashboard: Boolean,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
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
                isEditingDashboard = isEditingDashboard,
                onUpdateDashboardLayout = onUpdateDashboardLayout,
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
    isEditingDashboard: Boolean,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
) {
    val availableFields = availableRecordingDashboardFields(state)
    val layout = state.dashboardLayout.withAvailableFields(availableFields)
    val stats = recordingDashboardStats(
        state = state,
        totalTime = totalTime,
        movingTime = movingTime,
        now = now,
        unitFormatter = unitFormatter,
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isEditingDashboard) {
            RecordingDashboardEditor(
                layout = layout,
                availableFields = availableFields,
                onUpdateLayout = onUpdateDashboardLayout,
            )
        }
        RecordingDashboardGrid(
            layout = layout,
            stats = stats,
            isEditingDashboard = isEditingDashboard,
            onUpdateLayout = onUpdateDashboardLayout,
        )

        if (state.bleDeviceStatuses.isNotEmpty()) {
            ActivityRecordingSensorStatusCard(deviceStatuses = state.bleDeviceStatuses)
            if (state.bleHeartRateNoSignal && state.currentHeartRateBpm == null) {
                Text(
                    text = stringResource(R.string.activity_recording_sensors_garmin_broadcast_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    }
}

private data class RecordingDashboardStat(
    val value: DisplayValue,
    val label: String,
)

@Composable
private fun recordingDashboardStats(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
): Map<ActivityRecordingDashboardField, RecordingDashboardStat> {
    val waiting = stringResource(R.string.activity_recording_sensors_waiting_short)
    val hasHeartRateSensor = state.bleDeviceStatuses.any {
        BleSensorCapability.HEART_RATE in it.capabilities
    }
    val heartRate = state.currentHeartRateBpm?.let { unitFormatter.heartRate(it) }
        ?: DisplayValue(
            value = if (hasHeartRateSensor && state.bleHeartRateNoSignal) {
                stringResource(R.string.activity_recording_sensors_no_signal_short)
            } else {
                waiting
            },
            unit = "bpm",
        )
    val cadence = state.currentCyclingCadenceRpm ?: state.currentRunningCadenceRpm
    val speed = state.currentSensorSpeedMetersPerSecond
        ?: state.effectiveCurrentSpeedMetersPerSecond(now)

    return mapOf(
        ActivityRecordingDashboardField.HEART_RATE to RecordingDashboardStat(
            value = heartRate,
            label = stringResource(R.string.activity_recording_live_heart_rate),
        ),
        ActivityRecordingDashboardField.CADENCE to RecordingDashboardStat(
            value = cadence?.let { unitFormatter.cadence(it.toDouble()) } ?: DisplayValue(waiting, "rpm"),
            label = stringResource(R.string.activity_recording_live_cadence),
        ),
        ActivityRecordingDashboardField.SPEED to RecordingDashboardStat(
            value = unitFormatter.speed(speed),
            label = stringResource(R.string.activity_entry_recording_speed),
        ),
        ActivityRecordingDashboardField.DISTANCE to RecordingDashboardStat(
            value = unitFormatter.distance(state.distanceMeters),
            label = stringResource(R.string.activity_entry_recording_distance),
        ),
        ActivityRecordingDashboardField.DURATION to RecordingDashboardStat(
            value = DisplayValue(formatRecordingElapsed(totalTime), ""),
            label = stringResource(R.string.activity_entry_recording_total_time),
        ),
        ActivityRecordingDashboardField.MOVING_TIME to RecordingDashboardStat(
            value = DisplayValue(formatRecordingElapsed(movingTime), ""),
            label = stringResource(R.string.activity_entry_recording_moving_time),
        ),
        ActivityRecordingDashboardField.AVERAGE_SPEED to RecordingDashboardStat(
            value = unitFormatter.averageSpeed(state.distanceMeters, totalTime.toMillis()),
            label = stringResource(R.string.activity_entry_recording_average_speed),
        ),
        ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED to RecordingDashboardStat(
            value = unitFormatter.averageSpeed(state.distanceMeters, movingTime.toMillis()),
            label = stringResource(R.string.activity_entry_recording_average_moving_speed),
        ),
        ActivityRecordingDashboardField.MAX_SPEED to RecordingDashboardStat(
            value = unitFormatter.speed(state.maxSpeedMetersPerSecond),
            label = stringResource(R.string.activity_entry_recording_max_speed),
        ),
        ActivityRecordingDashboardField.ELEVATION_GAIN to RecordingDashboardStat(
            value = unitFormatter.elevation(state.displayElevationGainedMeters()),
            label = stringResource(R.string.activity_entry_recording_elevation_gain),
        ),
        ActivityRecordingDashboardField.POWER to RecordingDashboardStat(
            value = state.currentPowerWatts?.let(unitFormatter::power) ?: DisplayValue(waiting, "W"),
            label = stringResource(R.string.activity_recording_live_power),
        ),
        ActivityRecordingDashboardField.STEPS to RecordingDashboardStat(
            value = DisplayValue(unitFormatter.count(state.repetitionCount), ""),
            label = stringResource(R.string.activity_entry_steps_title),
        ),
    )
}

private fun availableRecordingDashboardFields(
    state: ActivityRecordingState,
): List<ActivityRecordingDashboardField> = buildList {
    add(ActivityRecordingDashboardField.HEART_RATE)
    add(ActivityRecordingDashboardField.CADENCE)
    add(ActivityRecordingDashboardField.SPEED)
    add(ActivityRecordingDashboardField.DISTANCE)
    add(ActivityRecordingDashboardField.DURATION)
    add(ActivityRecordingDashboardField.MOVING_TIME)
    add(ActivityRecordingDashboardField.AVERAGE_SPEED)
    add(ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED)
    add(ActivityRecordingDashboardField.MAX_SPEED)
    add(ActivityRecordingDashboardField.ELEVATION_GAIN)
    add(ActivityRecordingDashboardField.POWER)
    if (activityEntryTypeById(state.activityTypeId)?.supportsStepCounting == true) {
        add(ActivityRecordingDashboardField.STEPS)
    }
}

private fun ActivityRecordingDashboardLayout.withAvailableFields(
    availableFields: List<ActivityRecordingDashboardField>,
): ActivityRecordingDashboardLayout {
    val available = availableFields.toSet()
    val normalized = normalized()
    val items = normalized.items.filter { it.field in available }
    if (items.isNotEmpty()) {
        return normalized.copy(
            fields = items.map { it.field },
            sizes = items.associate { it.field to it.size },
        ).normalized()
    }
    val fields = ActivityRecordingDashboardLayout.DefaultFields.filter { it in available }
        .ifEmpty { availableFields }
    return normalized.copy(
        fields = fields,
        sizes = normalized.sizes.filterKeys { it in fields.toSet() },
    ).normalized()
}

@Composable
private fun RecordingDashboardGrid(
    layout: ActivityRecordingDashboardLayout,
    stats: Map<ActivityRecordingDashboardField, RecordingDashboardStat>,
    isEditingDashboard: Boolean,
    onUpdateLayout: (ActivityRecordingDashboardLayout) -> Unit,
) {
    val normalizedLayout = layout.normalized()
    val placements = normalizedLayout.placements()
    if (placements.isEmpty()) return

    val spacing = if (normalizedLayout.template == ActivityRecordingDashboardTemplate.THREE_BY_FOUR) {
        8.dp
    } else {
        10.dp
    }
    val cellHeight = when (normalizedLayout.template) {
        ActivityRecordingDashboardTemplate.LARGE_TOP -> 78.dp
        ActivityRecordingDashboardTemplate.TWO_BY_FOUR -> 96.dp
        ActivityRecordingDashboardTemplate.THREE_BY_FOUR -> 86.dp
    }

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        content = {
            placements.forEach { placement ->
                val field = placement.item.field
                val stat = stats[field] ?: return@forEach
                val index = normalizedLayout.fields.indexOf(field)
                key(field) {
                    RecordingDashboardTile(
                        field = field,
                        stat = stat,
                        size = placement.item.size,
                        emphasized = placement.item.size.hasRoomyMetricText(),
                        compact = placement.item.size.hasCompactMetricText(),
                        isEditingDashboard = isEditingDashboard,
                        onDropField = { draggedField ->
                            if (draggedField != field) {
                                onUpdateLayout(normalizedLayout.withMovedFieldToTarget(draggedField, field))
                            }
                        },
                        onRemove = {
                            onUpdateLayout(normalizedLayout.withRemovedField(field))
                        },
                        onResize = { resizedSize ->
                            onUpdateLayout(
                                normalizedLayout.withFieldSize(
                                    field = field,
                                    size = resizedSize,
                                )
                            )
                        },
                        onMovePrevious = normalizedLayout.fields.getOrNull(index - 1)?.let { targetField ->
                            { onUpdateLayout(normalizedLayout.withMovedFieldToTarget(field, targetField)) }
                        },
                        onMoveNext = normalizedLayout.fields.getOrNull(index + 1)?.let { targetField ->
                            { onUpdateLayout(normalizedLayout.withMovedFieldToTarget(field, targetField)) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val cellHeightPx = cellHeight.roundToPx()
        val layoutWidth = constraints.maxWidth
        val columnWidth = (
            (layoutWidth - spacingPx * (normalizedLayout.template.columns - 1)) /
                normalizedLayout.template.columns
            ).coerceAtLeast(0)
        val layoutHeight = cellHeightPx * normalizedLayout.template.rows +
            spacingPx * (normalizedLayout.template.rows - 1)
        val placeables = measurables.mapIndexed { index, measurable ->
            val placement = placements[index]
            val width = columnWidth * placement.columnSpan + spacingPx * (placement.columnSpan - 1)
            val height = cellHeightPx * placement.rowSpan + spacingPx * (placement.rowSpan - 1)
            measurable.measure(Constraints.fixed(width, height))
        }

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val placement = placements[index]
                val x = placement.column * (columnWidth + spacingPx)
                val y = placement.row * (cellHeightPx + spacingPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
private fun RecordingDashboardTile(
    field: ActivityRecordingDashboardField,
    stat: RecordingDashboardStat,
    size: ActivityRecordingDashboardItemSize,
    emphasized: Boolean,
    compact: Boolean,
    isEditingDashboard: Boolean,
    onDropField: (ActivityRecordingDashboardField) -> Unit,
    onRemove: () -> Unit,
    onResize: (ActivityRecordingDashboardItemSize) -> Unit,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var isDropTargetActive by remember(field, isEditingDashboard) { mutableStateOf(false) }
    val wiggleRotation = if (isEditingDashboard) {
        val wiggleTransition = rememberInfiniteTransition(label = "RecordingDashboardWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -RecordingDashboardEditWiggleDegrees,
            targetValue = RecordingDashboardEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (field.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "RecordingDashboardWiggleRotation",
        )
        rotation
    } else {
        0f
    }
    val removeWidgetLabel = stringResource(R.string.cd_remove_widget)
    val movePreviousLabel = stringResource(R.string.cd_move_widget_up)
    val moveNextLabel = stringResource(R.string.cd_move_widget_down)
    val shrinkLabel = stringResource(R.string.cd_decrease_recording_dashboard_widget_size)
    val growLabel = stringResource(R.string.cd_increase_recording_dashboard_widget_size)
    val latestSize = rememberUpdatedState(size)
    val dragAndDropTarget = remember(field, isEditingDashboard) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDropTargetActive = event.toRecordingDashboardField() != field
            }

            override fun onExited(event: DragAndDropEvent) {
                isDropTargetActive = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDropTargetActive = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val draggedField = event.toRecordingDashboardField() ?: return false
                isDropTargetActive = false
                onDropField(draggedField)
                return true
            }
        }
    }
    val dragSourceModifier = if (isEditingDashboard) {
        Modifier.dragAndDropSource { _ ->
            DragAndDropTransferData(
                clipData = ClipData(
                    ClipDescription(
                        RecordingDashboardDragLabel,
                        arrayOf(RecordingDashboardDragMimeType),
                    ),
                    ClipData.Item(field.name),
                ),
                localState = field,
            )
        }
    } else {
        Modifier
    }
    val dropTargetModifier = if (isEditingDashboard) {
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { startEvent ->
                RecordingDashboardDragMimeType in startEvent.mimeTypes()
            },
            target = dragAndDropTarget,
        )
    } else {
        Modifier
    }
    val editSemanticsModifier = if (isEditingDashboard) {
        Modifier.semantics {
            contentDescription = stat.label
            customActions = buildList {
                onMovePrevious?.let { action ->
                    add(
                        CustomAccessibilityAction(movePreviousLabel) {
                            action()
                            true
                        }
                    )
                }
                onMoveNext?.let { action ->
                    add(
                        CustomAccessibilityAction(moveNextLabel) {
                            action()
                            true
                        }
                    )
                }
                if (size.canShrink()) {
                    add(
                        CustomAccessibilityAction(shrinkLabel) {
                            onResize(size.previousSize())
                            true
                        }
                    )
                }
                if (size.canGrow()) {
                    add(
                        CustomAccessibilityAction(growLabel) {
                            onResize(size.nextSize())
                            true
                        }
                    )
                }
                add(
                    CustomAccessibilityAction(removeWidgetLabel) {
                        onRemove()
                        true
                    }
                )
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .zIndex(if (isDropTargetActive) 1f else 0f)
            .graphicsLayer {
                scaleX = if (isDropTargetActive) 1.02f else 1f
                scaleY = if (isDropTargetActive) 1.02f else 1f
                rotationZ = if (isEditingDashboard && !isDropTargetActive) wiggleRotation else 0f
                shadowElevation = if (isDropTargetActive) 12.dp.toPx() else 0f
            }
            .then(editSemanticsModifier)
            .then(dropTargetModifier),
    ) {
        RecordingDashboardTileContent(
            stat = stat,
            size = size,
            emphasized = emphasized,
            compact = compact,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEditingDashboard) {
                        Modifier.border(
                            width = if (isDropTargetActive) 2.dp else 1.dp,
                            color = if (isDropTargetActive) {
                                WorkoutColor
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    } else {
                        Modifier
                    }
                )
                .then(dragSourceModifier),
        )
        if (isEditingDashboard) {
            RecordingDashboardEditButton(
                onClick = onRemove,
                contentDescription = removeWidgetLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            RecordingDashboardResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                onResize = onResize,
                latestSize = { latestSize.value },
            )
        }
    }
}

@Composable
private fun RecordingDashboardResizeHandle(
    onResize: (ActivityRecordingDashboardItemSize) -> Unit,
    latestSize: () -> ActivityRecordingDashboardItemSize,
    modifier: Modifier = Modifier,
) {
    val handleColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(38.dp)
            .pointerInput(Unit) {
                var dragStartSize = ActivityRecordingDashboardItemSize.SMALL
                var appliedSize = ActivityRecordingDashboardItemSize.SMALL
                var dragOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        dragStartSize = latestSize()
                        appliedSize = dragStartSize
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        val targetSize = dragStartSize.sizeForResizeDrag(
                            dragOffset = dragOffset,
                            stepPx = RecordingDashboardResizeStep.toPx(),
                        )
                        if (targetSize != appliedSize) {
                            appliedSize = targetSize
                            onResize(targetSize)
                        }
                    },
                )
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val insets = listOf(10.dp.toPx(), 17.dp.toPx(), 24.dp.toPx())
            insets.forEach { inset ->
                drawLine(
                    color = handleColor,
                    start = Offset(size.width - inset, size.height),
                    end = Offset(size.width, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun RecordingDashboardEditButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.9f else 0.54f),
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

@Composable
private fun RecordingDashboardTileContent(
    stat: RecordingDashboardStat,
    size: ActivityRecordingDashboardItemSize,
    emphasized: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val valueStyle = when {
        emphasized -> MaterialTheme.typography.displayMedium
        compact -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.headlineMedium
    }
    val unitStyle = when {
        compact -> MaterialTheme.typography.labelMedium
        size.rowSpan == 1 -> MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.titleSmall
    }
    val labelStyle = if (compact || size.rowSpan == 1) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        contentPadding = PaddingValues(if (compact || size.rowSpan == 1) 8.dp else 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                AutoResizeText(
                    text = stat.value.value,
                    style = valueStyle,
                    color = if (emphasized) WorkoutColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (stat.value.unit.isNotBlank()) {
                    Text(
                        text = stat.value.unit,
                        style = unitStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 3.dp, bottom = if (compact) 2.dp else 4.dp),
                        maxLines = 1,
                    )
                }
            }
            AutoResizeText(
                text = stat.label.uppercase(),
                style = labelStyle,
                color = WorkoutColor,
                maxLines = 1,
            )
        }
    }
}

private fun DragAndDropEvent.toRecordingDashboardField(): ActivityRecordingDashboardField? {
    val androidDragEvent = toAndroidDragEvent()
    (androidDragEvent.localState as? ActivityRecordingDashboardField)?.let { return it }
    return androidDragEvent.clipData
        ?.getItemAt(0)
        ?.text
        ?.toString()
        ?.let { fieldName ->
            runCatching { ActivityRecordingDashboardField.valueOf(fieldName) }.getOrNull()
        }
}

private fun ActivityRecordingDashboardLayout.withMovedFieldToTarget(
    field: ActivityRecordingDashboardField,
    targetField: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    val fromIndex = fields.indexOf(field)
    val targetIndex = fields.indexOf(targetField)
    if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return this
    return copy(fields = fields.move(fromIndex, targetIndex)).normalized()
}

private fun ActivityRecordingDashboardLayout.withRemovedField(
    field: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    if (fields.size <= 1) return this
    return copy(
        fields = fields.filterNot { it == field },
        sizes = sizes - field,
    ).normalized()
}

private fun ActivityRecordingDashboardLayout.withAddedField(
    field: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    if (field in fields) return this
    val updated = copy(
        fields = fields + field,
        sizes = sizes - field,
    ).normalized()
    return if (field in updated.fields) updated else this
}

private fun ActivityRecordingDashboardItemSize.nextSize(): ActivityRecordingDashboardItemSize =
    if (columnSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.columns) {
        copy(columnSpan = columnSpan + 1)
    } else {
        copy(rowSpan = (rowSpan + 1).coerceAtMost(ActivityRecordingDashboardTemplate.LARGE_TOP.rows))
    }

private fun ActivityRecordingDashboardItemSize.previousSize(): ActivityRecordingDashboardItemSize =
    if (rowSpan > 1) {
        copy(rowSpan = rowSpan - 1)
    } else {
        copy(columnSpan = (columnSpan - 1).coerceAtLeast(1))
    }

private fun ActivityRecordingDashboardItemSize.canGrow(): Boolean =
    columnSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.columns ||
        rowSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.rows

private fun ActivityRecordingDashboardItemSize.canShrink(): Boolean =
    columnSpan > 1 || rowSpan > 1

private fun ActivityRecordingDashboardItemSize.hasCompactMetricText(): Boolean =
    columnSpan == 1 || rowSpan == 1

private fun ActivityRecordingDashboardItemSize.hasRoomyMetricText(): Boolean =
    columnSpan >= 3 ||
        rowSpan >= 3 ||
        (columnSpan >= 2 && rowSpan >= 2)

private fun ActivityRecordingDashboardItemSize.sizeForResizeDrag(
    dragOffset: Offset,
    stepPx: Float,
): ActivityRecordingDashboardItemSize {
    val targetColumnSpan = (columnSpan + dragOffset.x.dragSteps(stepPx))
        .coerceIn(1, ActivityRecordingDashboardTemplate.LARGE_TOP.columns)
    val targetRowSpan = (rowSpan + dragOffset.y.dragSteps(stepPx))
        .coerceIn(1, ActivityRecordingDashboardTemplate.LARGE_TOP.rows)
    return ActivityRecordingDashboardItemSize(
        columnSpan = targetColumnSpan,
        rowSpan = targetRowSpan,
    )
}

private fun Float.dragSteps(stepPx: Float): Int =
    when {
        this >= stepPx -> (this / stepPx).toInt()
        this <= -stepPx -> (this / stepPx).toInt()
        else -> 0
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordingDashboardEditor(
    layout: ActivityRecordingDashboardLayout,
    availableFields: List<ActivityRecordingDashboardField>,
    onUpdateLayout: (ActivityRecordingDashboardLayout) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val addableFields = availableFields
            .filterNot { it in layout.fields }
            .filter { field -> field in layout.withAddedField(field).fields }
        if (addableFields.isNotEmpty()) {
            Text(
                text = stringResource(R.string.activity_entry_recording_dashboard_add_field),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                addableFields.forEach { field ->
                    AssistChip(
                        onClick = {
                            onUpdateLayout(
                                layout.withAddedField(field),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text(stringResource(field.labelRes)) },
                    )
                }
            }
        }
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
                    OpenVitalsIconButton(onClick = { onDeleteMarker(marker.id) }) {
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
    emphasized: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (emphasized) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.value,
                style = if (emphasized) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = if (emphasized) WorkoutColor else MaterialTheme.colorScheme.onSurface,
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

private val ActivityRecordingDashboardTemplate.labelRes: Int
    get() = when (this) {
        ActivityRecordingDashboardTemplate.TWO_BY_FOUR -> R.string.activity_entry_recording_dashboard_layout_two_by_four
        ActivityRecordingDashboardTemplate.THREE_BY_FOUR -> R.string.activity_entry_recording_dashboard_layout_three_by_four
        ActivityRecordingDashboardTemplate.LARGE_TOP -> R.string.activity_entry_recording_dashboard_layout_large_top
    }

private val ActivityRecordingDashboardField.labelRes: Int
    get() = when (this) {
        ActivityRecordingDashboardField.HEART_RATE -> R.string.activity_recording_live_heart_rate
        ActivityRecordingDashboardField.CADENCE -> R.string.activity_recording_live_cadence
        ActivityRecordingDashboardField.SPEED -> R.string.activity_entry_recording_speed
        ActivityRecordingDashboardField.DISTANCE -> R.string.activity_entry_recording_distance
        ActivityRecordingDashboardField.DURATION -> R.string.activity_entry_recording_total_time
        ActivityRecordingDashboardField.MOVING_TIME -> R.string.activity_entry_recording_moving_time
        ActivityRecordingDashboardField.AVERAGE_SPEED -> R.string.activity_entry_recording_average_speed
        ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED -> R.string.activity_entry_recording_average_moving_speed
        ActivityRecordingDashboardField.MAX_SPEED -> R.string.activity_entry_recording_max_speed
        ActivityRecordingDashboardField.ELEVATION_GAIN -> R.string.activity_entry_recording_elevation_gain
        ActivityRecordingDashboardField.POWER -> R.string.activity_recording_live_power
        ActivityRecordingDashboardField.STEPS -> R.string.activity_entry_steps_title
    }

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
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
        status == ActivityRecordingStatus.IDLE -> R.string.activity_entry_recording_idle
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

private const val RecordingDashboardDragMimeType = "application/vnd.openvitals.recording-dashboard-field"
private const val RecordingDashboardDragLabel = "OpenVitals recording dashboard widget"
private const val RecordingDashboardEditWiggleDegrees = 0.45f
private val RecordingDashboardResizeStep = 44.dp
private const val DefaultTimeSplitMinutes = 5
private val TimeSplitMinuteOptions = listOf(1, 5, 10)
private const val MetersPerMile = 1_609.344
