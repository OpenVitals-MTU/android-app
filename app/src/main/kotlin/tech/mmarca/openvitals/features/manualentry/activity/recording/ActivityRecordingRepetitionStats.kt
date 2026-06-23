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

@Composable
internal fun RepetitionRecordingStats(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    unitFormatter: UnitFormatter,
    onAdjustRepetitionCount: (Long) -> Unit,
    onEndRepetitionSet: () -> Unit,
    onStartNextRepetitionSet: () -> Unit,
    onFinishRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
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
                text = unitFormatter.count(state.currentSetRepetitionCount),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onAdjustRepetitionCount(-1) },
                    enabled = state.status == ActivityRecordingStatus.RECORDING &&
                        state.currentSetRepetitionCount > 0L,
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
                    enabled = state.status == ActivityRecordingStatus.RECORDING,
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
            if (state.status == ActivityRecordingStatus.RESTING) {
                Text(
                    text = stringResource(
                        R.string.activity_entry_recording_rest_remaining,
                        formatRecordingElapsed(state.restRemainingDuration()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(
                    onClick = onStartNextRepetitionSet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.activity_entry_recording_start_next_set),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            } else {
                Button(
                    onClick = onEndRepetitionSet,
                    enabled = state.currentSetRepetitionCount > 0L,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.activity_entry_recording_end_set),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
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
        RecordingStat(
            value = DisplayValue(formatRecordingElapsed(state.restDuration()), ""),
            label = stringResource(R.string.activity_entry_recording_rest_time),
            modifier = Modifier.weight(1f),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                text = stringResource(R.string.activity_entry_recording_end_session),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        OutlinedButton(
            onClick = onDiscardRecording,
            modifier = Modifier.weight(1f),
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
