package tech.mmarca.openvitals.features.manualentry

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.MindfulnessBellSound
import tech.mmarca.openvitals.ui.theme.MindfulnessColor

@Composable
fun MindfulnessEntryScreen(
    viewModel: MindfulnessEntryViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }
    MindfulnessBellEffect(state.bellEvent)

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            MindfulnessTimerCard(
                state = state,
                onDurationChanged = viewModel::updateDurationMinutes,
                onIntervalEnabledChanged = viewModel::updateIntervalEnabled,
                onIntervalChanged = viewModel::updateIntervalMinutes,
                onBellSoundChanged = viewModel::updateBellSound,
                onStartTimer = viewModel::startTimer,
                onStopTimer = viewModel::stopTimer,
                onResumeTimer = viewModel::resumeTimer,
                onSaveTimerSession = viewModel::saveTimerSession,
                onDiscardTimer = viewModel::discardTimer,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            MindfulnessManualEntryCard(
                state = state,
                onMinutesChanged = viewModel::updateManualMinutes,
                onAddEntry = viewModel::addManualEntry,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MindfulnessTimerCard(
    state: MindfulnessEntryUiState,
    onDurationChanged: (String) -> Unit,
    onIntervalEnabledChanged: (Boolean) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onBellSoundChanged: (MindfulnessBellSound) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSaveTimerSession: () -> Unit,
    onDiscardTimer: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canEditTimer = !state.isTimerRunning && !state.isTimerPaused && !state.timerCompleted && !state.isSavingEntry
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MindfulnessEntryHeader(
                state = state,
                onRequestWritePermission = onRequestWritePermission,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.durationMinutesText,
                    onValueChange = onDurationChanged,
                    enabled = canEditTimer,
                    singleLine = true,
                    label = { Text(stringResource(R.string.mindfulness_entry_duration_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.intervalMinutesText,
                    onValueChange = onIntervalChanged,
                    enabled = canEditTimer && state.intervalEnabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.mindfulness_entry_interval_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.intervalEnabled,
                    onCheckedChange = onIntervalEnabledChanged,
                    enabled = canEditTimer,
                )
                Text(
                    text = stringResource(R.string.mindfulness_entry_interval_bell),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            BellSoundSelector(
                selectedSound = state.bellSound,
                enabled = canEditTimer,
                onBellSoundChanged = onBellSoundChanged,
            )

            Text(
                text = formattedTimer(state.remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            TimerActions(
                state = state,
                onStartTimer = onStartTimer,
                onStopTimer = onStopTimer,
                onResumeTimer = onResumeTimer,
                onSaveTimerSession = onSaveTimerSession,
                onDiscardTimer = onDiscardTimer,
            )

            state.entryError?.let { entryError ->
                Text(
                    text = mindfulnessEntryErrorText(entryError, state.writeErrorMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MindfulnessEntryHeader(
    state: MindfulnessEntryUiState,
    onRequestWritePermission: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.SelfImprovement,
            contentDescription = null,
            tint = MindfulnessColor,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.mindfulness_entry_timer_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when {
                        !state.mindfulnessAvailable -> R.string.mindfulness_entry_unavailable
                        state.canWrite -> R.string.mindfulness_entry_subtitle
                        else -> R.string.mindfulness_entry_permission_needed
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.canWrite && state.mindfulnessAvailable && !state.isCheckingPermission) {
            OutlinedButton(onClick = onRequestWritePermission) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

@Composable
private fun BellSoundSelector(
    selectedSound: MindfulnessBellSound,
    enabled: Boolean,
    onBellSoundChanged: (MindfulnessBellSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mindfulness_entry_bell_sound),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(MindfulnessBellSound.entries, key = { it.name }) { sound ->
                FilterChip(
                    selected = sound == selectedSound,
                    onClick = { onBellSoundChanged(sound) },
                    enabled = enabled,
                    label = { Text(stringResource(sound.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun TimerActions(
    state: MindfulnessEntryUiState,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSaveTimerSession: () -> Unit,
    onDiscardTimer: () -> Unit,
) {
    when {
        state.isTimerRunning -> {
            OutlinedButton(
                onClick = onStopTimer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_stop_timer))
            }
        }
        state.isTimerPaused -> {
            Button(
                onClick = onResumeTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_resume_timer))
            }
            Button(
                onClick = onSaveTimerSession,
                enabled = state.canWrite && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_save_session))
            }
            OutlinedButton(
                onClick = onDiscardTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_discard_timer))
            }
        }
        state.timerCompleted -> {
            Text(
                text = stringResource(R.string.mindfulness_entry_completed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onSaveTimerSession,
                enabled = state.canWrite && !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_save_session))
            }
            OutlinedButton(
                onClick = onDiscardTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_discard_timer))
            }
        }
        else -> {
            Button(
                onClick = onStartTimer,
                enabled = !state.isSavingEntry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.mindfulness_entry_start_timer))
            }
        }
    }
}

@Composable
private fun MindfulnessManualEntryCard(
    state: MindfulnessEntryUiState,
    onMinutesChanged: (String) -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.mindfulness_entry_manual_title),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = state.manualMinutesText,
                onValueChange = onMinutesChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.mindfulness_entry_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.mindfulness_entry_add_minutes),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MindfulnessBellEffect(event: MindfulnessBellEvent?) {
    val context = LocalContext.current
    LaunchedEffect(event?.id) {
        val sound = event?.sound ?: return@LaunchedEffect
        val player = runCatching { MediaPlayer.create(context, sound.rawRes()) }.getOrNull()
            ?: return@LaunchedEffect
        var released = false
        fun releasePlayer() {
            if (!released) {
                player.release()
                released = true
            }
        }
        try {
            suspendCancellableCoroutine { continuation ->
                player.setOnCompletionListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                player.setOnErrorListener { _, _, _ ->
                    if (continuation.isActive) continuation.resume(Unit)
                    true
                }
                continuation.invokeOnCancellation { releasePlayer() }
                runCatching { player.start() }.onFailure {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        } finally {
            releasePlayer()
        }
    }
}

@Composable
private fun mindfulnessEntryErrorText(
    error: MindfulnessEntryError,
    message: String?,
): String = when (error) {
    MindfulnessEntryError.INVALID_TIMER -> stringResource(R.string.mindfulness_entry_invalid_timer)
    MindfulnessEntryError.INVALID_MANUAL_ENTRY -> stringResource(R.string.mindfulness_entry_invalid_manual)
    MindfulnessEntryError.TIMER_TOO_SHORT -> stringResource(R.string.mindfulness_entry_timer_too_short)
    MindfulnessEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.mindfulness_entry_permission_needed)
    MindfulnessEntryError.UNAVAILABLE -> stringResource(R.string.mindfulness_entry_unavailable)
    MindfulnessEntryError.WRITE_FAILED -> stringResource(
        R.string.mindfulness_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private fun MindfulnessBellSound.labelRes(): Int = when (this) {
    MindfulnessBellSound.STRUCK -> R.string.mindfulness_bell_struck
    MindfulnessBellSound.RUBBED -> R.string.mindfulness_bell_rubbed
    MindfulnessBellSound.BRIGHT -> R.string.mindfulness_bell_bright
    MindfulnessBellSound.TEMPLE -> R.string.mindfulness_bell_temple
    MindfulnessBellSound.HARMONY -> R.string.mindfulness_bell_harmony
}

private fun MindfulnessBellSound.rawRes(): Int = when (this) {
    MindfulnessBellSound.STRUCK -> R.raw.bowl_struck
    MindfulnessBellSound.RUBBED -> R.raw.bowl_rubbed
    MindfulnessBellSound.BRIGHT -> R.raw.bowl_bright
    MindfulnessBellSound.TEMPLE -> R.raw.bowl_temple
    MindfulnessBellSound.HARMONY -> R.raw.bowl_harmony
}

private fun formattedTimer(seconds: Int): String {
    val clampedSeconds = seconds.coerceAtLeast(0)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
