package tech.mmarca.openvitals.features.manualentry.mindfulness

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MindfulnessEntryScreen(
    viewModel: MindfulnessEntryViewModel,
    onEntrySaved: () -> Unit = {},
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
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }
    MindfulnessBellEffect(state.bellEvent)
    MindfulnessBackgroundPreviewEffect(
        event = state.backgroundEvent,
        isTimerRunning = state.isTimerRunning,
    )
    MindfulnessBackgroundEffect(
        sound = state.backgroundSound,
        isPlaying = state.isTimerRunning,
    )

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        if (!state.isEditMode) {
            item {
                MindfulnessTimerCard(
                    state = state,
                    onDurationChanged = viewModel::updateDurationMinutes,
                    onIntervalEnabledChanged = viewModel::updateIntervalEnabled,
                    onIntervalChanged = viewModel::updateIntervalMinutes,
                    onBellSoundChanged = viewModel::updateBellSound,
                    onBackgroundSoundChanged = viewModel::updateBackgroundSound,
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
        }
        item {
            MindfulnessManualEntryCard(
                state = state,
                onMinutesChanged = viewModel::updateManualMinutes,
                onEntryStartTimeChanged = viewModel::updateEntryStartTime,
                onAddEntry = viewModel::addManualEntry,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
