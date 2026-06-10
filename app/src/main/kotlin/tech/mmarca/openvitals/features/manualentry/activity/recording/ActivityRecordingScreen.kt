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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.activity.RoutePreview
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityRecordingScreen(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onAdjustRepetitionCount: (Long) -> Unit,
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

    val totalTime = state.elapsedDuration(now)
    val movingTime = state.movingDuration(now)
    val distance = unitFormatter.distance(state.distanceMeters)
    val elevation = unitFormatter.elevation(state.elevationGainedMeters)
    val speed = unitFormatter.averageSpeed(state.distanceMeters, movingTime.toMillis())

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
                text = stringResource(
                    if (state.status == ActivityRecordingStatus.PAUSED) {
                        R.string.activity_entry_recording_paused
                    } else {
                        R.string.activity_entry_recording_active
                    }
                ),
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
            )
        } else {
            if (state.points.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RoutePreview(
                        points = state.points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.activity_entry_recording_waiting_for_gps),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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
                        value = elevation,
                        label = stringResource(R.string.activity_entry_recording_elevation_gain),
                        modifier = Modifier.weight(1f),
                    )
                    RecordingStat(
                        value = DisplayValue(state.points.size.toString(), ""),
                        label = stringResource(R.string.activity_entry_recording_points),
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
        }

        state.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

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

@Composable
internal fun RepetitionRecordingStats(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    unitFormatter: UnitFormatter,
    onAdjustRepetitionCount: (Long) -> Unit,
) {
    val activityType = activityEntryTypeById(state.activityTypeId)
    val countLabel = stringResource(
        if (activityType?.repetitionUnit == ActivityRepetitionUnit.STEPS) {
            R.string.activity_entry_steps_title
        } else {
            R.string.activity_entry_repetitions_title
        }
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = unitFormatter.count(state.repetitionCount),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onAdjustRepetitionCount(-1) },
                    enabled = state.repetitionCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                OutlinedButton(
                    onClick = { onAdjustRepetitionCount(1) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.activity_entry_recording_repetition_correction_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RecordingStat(
            value = DisplayValue(formatRecordingElapsed(totalTime), ""),
            label = stringResource(R.string.activity_entry_recording_total_time),
            modifier = Modifier.weight(1f),
        )
        RecordingStat(
            value = DisplayValue(formatRecordingElapsed(movingTime), ""),
            label = stringResource(R.string.activity_entry_recording_moving_time),
            modifier = Modifier.weight(1f),
        )
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
