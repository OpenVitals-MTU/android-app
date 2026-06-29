package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingScreen
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.AppThemeMode

@Composable
internal fun ActivityEntryRecordingContent(
    recordingState: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    viewModel: ActivityEntryViewModel,
    isFocusMode: Boolean,
    isOutdoorMode: Boolean,
    onFocusModeChanged: (Boolean) -> Unit,
    onOutdoorModeChanged: (Boolean) -> Unit,
    onActivityRecordingTitleChanged: (Int?) -> Unit,
    onActivityRecordingEditStateChanged: (Boolean, Boolean, () -> Unit) -> Unit,
    appThemeMode: AppThemeMode,
) {
    ActivityRecordingScreen(
        state = recordingState,
        unitFormatter = unitFormatter,
        onStartRecording = viewModel::startGpsRecording,
        onPauseRecording = viewModel::pauseGpsRecording,
        onResumeRecording = viewModel::resumeGpsRecording,
        onAddLap = viewModel::addRecordingLap,
        onAddMarker = viewModel::addRecordingMarker,
        onUpdateMarker = viewModel::updateRecordingMarker,
        onDeleteMarker = viewModel::deleteRecordingMarker,
        onUpdateDashboardLayout = viewModel::updateRecordingDashboardLayout,
        onChooseSource = viewModel::chooseSource,
        onAdjustRepetitionCount = viewModel::adjustRepetitionRecording,
        onEndRepetitionSet = viewModel::endRepetitionSet,
        onStartNextRepetitionSet = viewModel::startNextRepetitionSet,
        onFinishRecording = {
            viewModel.finishGpsRecording(unitFormatter.unitSystem())
        },
        onActivityRecordingTitleChanged = onActivityRecordingTitleChanged,
        onDashboardEditStateChanged = onActivityRecordingEditStateChanged,
        isFocusMode = isFocusMode,
        onFocusModeChanged = onFocusModeChanged,
        isOutdoorMode = isOutdoorMode,
        onOutdoorModeChanged = onOutdoorModeChanged,
        appThemeMode = appThemeMode,
        modifier = if (isFocusMode || isOutdoorMode) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        },
    )
}
