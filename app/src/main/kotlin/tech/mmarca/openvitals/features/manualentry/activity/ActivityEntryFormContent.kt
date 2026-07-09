package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingSetupScreen
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState
import tech.mmarca.openvitals.features.manualentry.activity.recording.activityRecordingLocationPermissions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.UnitFormatter

@Composable
internal fun ActivityEntryFormContent(
    state: ActivityEntryUiState,
    recordingState: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onPerformSourceActionAfterPermission: (ActivityEntrySourceAction) -> Unit,
    onRequestGpsLocationPermissions: () -> Unit,
    onRequestActivityRecognitionPermission: () -> Unit,
    onRequestWritePermissions: () -> Unit,
    viewModel: ActivityEntryViewModel,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            when (state.mode) {
                ActivityEntryMode.RECORDING -> {
                    ActivityRecordingSetupScreen(
                        state = state,
                        recordingState = recordingState,
                        unitFormatter = unitFormatter,
                        onSelectActivityType = viewModel::selectActivityType,
                        onStartRecording = { initialFix, restSeconds ->
                            viewModel.startGpsRecording(initialFix, restSeconds)
                        },
                        onRequestLocationPermission = onRequestGpsLocationPermissions,
                        onRequestActivityRecognitionPermission = onRequestActivityRecognitionPermission,
                        onChooseSource = viewModel::chooseSource,
                        onRequestWritePermission = onRequestWritePermissions,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                ActivityEntryMode.CHOOSE_SOURCE -> {
                    ActivityEntrySourceCard(
                        state = state,
                        onStartManualEntry = {
                            onPerformSourceActionAfterPermission(ActivityEntrySourceAction.MANUAL)
                        },
                        onCreateFromExistingPlan = {
                            onPerformSourceActionAfterPermission(ActivityEntrySourceAction.EXISTING_PLAN)
                        },
                        onRecordGpsActivity = {
                            onPerformSourceActionAfterPermission(ActivityEntrySourceAction.RECORD_GPS)
                        },
                        onRequestWritePermission = onRequestWritePermissions,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                ActivityEntryMode.PLAN_ACTIVITY_PICKER -> {
                    ActivityPlanActivityPickerCard(
                        state = state,
                        onSelectActivity = viewModel::selectPlannedWorkoutActivity,
                        onChooseSource = viewModel::chooseSource,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                ActivityEntryMode.PLAN_PICKER -> {
                    ActivityPlanPickerCard(
                        state = state,
                        onSelectPlan = viewModel::applyPlannedWorkout,
                        onChooseActivity = viewModel::choosePlannedWorkoutActivity,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                else -> {
                    ActivityEntryCard(
                        state = state,
                        unitFormatter = unitFormatter,
                        onSelectActivityType = viewModel::selectActivityType,
                        onTitleChanged = viewModel::updateTitle,
                        onFeelingChanged = viewModel::updateFeeling,
                        onNotesChanged = viewModel::updateNotes,
                        onStartDateChanged = viewModel::updateStartDate,
                        onStartTimeChanged = viewModel::updateStartTime,
                        onDurationChanged = viewModel::updateDurationMinutes,
                        onRepetitionModeChanged = viewModel::updateRepetitionMode,
                        onRepetitionTotalChanged = viewModel::updateRepetitionTotal,
                        onRepetitionSetRepetitionsChanged = viewModel::updateRepetitionSetRepetitions,
                        onRepetitionSetRestChanged = viewModel::updateRepetitionSetRest,
                        onAddRepetitionSet = viewModel::addRepetitionSet,
                        onRemoveRepetitionSet = viewModel::removeRepetitionSet,
                        onCreateNewPlannedWorkout = viewModel::createNewPlannedWorkout,
                        onApplyPlannedWorkout = viewModel::applyPlannedWorkout,
                        onSavePlannedWorkout = {
                            viewModel.saveCurrentAsPlannedWorkout(unitFormatter.unitSystem())
                        },
                        onUpdatePlannedWorkout = {
                            viewModel.saveCurrentAsPlannedWorkout(unitFormatter.unitSystem(), updateSelected = true)
                        },
                        onDistanceChanged = viewModel::updateDistance,
                        onElevationChanged = viewModel::updateElevation,
                        onActiveCaloriesChanged = viewModel::updateActiveCalories,
                        onTotalCaloriesChanged = viewModel::updateTotalCalories,
                        onClearRoute = viewModel::clearImportedRoute,
                        onChooseSource = viewModel::chooseSource,
                        onRequestWritePermission = onRequestWritePermissions,
                        onAddEntry = {
                            viewModel.addEntry(unitFormatter.unitSystem())
                        },
                        onDiscardRecordingDraft = viewModel::discardRecordingDraft,
                        isEditMode = state.isEditMode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
