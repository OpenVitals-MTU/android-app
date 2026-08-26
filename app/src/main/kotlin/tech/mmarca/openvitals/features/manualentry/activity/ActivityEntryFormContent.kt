package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingSetupScreen
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun ActivityEntryFormContent(
    state: ActivityEntryUiState,
    recordingState: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onPerformSourceActionAfterPermission: (ActivityEntrySourceAction) -> Unit,
    onRequestGpsLocationPermissions: () -> Unit,
    onRequestActivityRecognitionPermission: () -> Unit,
    onRequestWritePermissions: () -> Unit,
    onOpenWorkoutPlans: () -> Unit,
    onOpenWorkoutPlanBuilder: (String) -> Unit,
    viewModel: ActivityEntryViewModel,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = Spacing.sm)) {
        item {
            val cardModifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            when (state.mode) {
                ActivityEntryMode.RECORDING -> ActivityRecordingSetupScreen(
                    state = state,
                    recordingState = recordingState,
                    unitFormatter = unitFormatter,
                    onSelectActivityType = viewModel::selectActivityType,
                    onStartRecording = { initialFix, restSeconds, withoutGps ->
                        viewModel.startGpsRecording(initialFix, restSeconds, withoutGps)
                    },
                    onStartHeartRateRecoveryTest = viewModel::startHeartRateRecoveryTest,
                    onStartPlan = viewModel::startPlanRecording,
                    onStartTodayPlan = viewModel::prepareGuidedPlan,
                    onRequestLocationPermission = onRequestGpsLocationPermissions,
                    onRequestActivityRecognitionPermission = onRequestActivityRecognitionPermission,
                    onChooseSource = viewModel::showStartHub,
                    onRequestWritePermission = onRequestWritePermissions,
                    modifier = cardModifier,
                )
                ActivityEntryMode.START_HUB -> ActivityStartHub(
                    state = state,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onLogFromPlan = { planId ->
                        onPerformSourceActionAfterPermission(ActivityEntrySourceAction.LogFromPlan(planId))
                    },
                    onStartPlan = { planId ->
                        onPerformSourceActionAfterPermission(ActivityEntrySourceAction.StartPlan(planId))
                    },
                    onRepeatPlan = { planId ->
                        onPerformSourceActionAfterPermission(ActivityEntrySourceAction.RepeatPlan(planId))
                    },
                    onRecord = { onPerformSourceActionAfterPermission(ActivityEntrySourceAction.Record) },
                    onLogManually = { onPerformSourceActionAfterPermission(ActivityEntrySourceAction.Manual) },
                    onManagePlans = onOpenWorkoutPlans,
                    onRequestWritePermission = onRequestWritePermissions,
                    modifier = cardModifier,
                )
                ActivityEntryMode.MANUAL,
                ActivityEntryMode.ROUTE_IMPORT,
                -> ActivityEntryCard(
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
                    onRepetitionSetGoalTypeChanged = viewModel::updateRepetitionSetGoalType,
                    onRepetitionSetExerciseChanged = viewModel::updateRepetitionSetExercise,
                    onAddExerciseStep = viewModel::addExerciseStep,
                    onAddRepetitionSet = viewModel::addRepetitionSet,
                    onRemoveRepetitionSet = viewModel::removeRepetitionSet,
                    onChangePlan = viewModel::showStartHub,
                    onEditPlan = onOpenWorkoutPlanBuilder,
                    onClearLinkedPlan = viewModel::clearLinkedPlan,
                    onSaveAsPlan = { viewModel.saveAsPlan(ActivityEntryUnits.from(unitFormatter)) },
                    onDistanceChanged = viewModel::updateDistance,
                    onElevationChanged = viewModel::updateElevation,
                    onActiveCaloriesChanged = viewModel::updateActiveCalories,
                    onTotalCaloriesChanged = viewModel::updateTotalCalories,
                    onClearRoute = viewModel::clearImportedRoute,
                    onChooseSource = viewModel::showStartHub,
                    onRequestWritePermission = onRequestWritePermissions,
                    onAddEntry = { viewModel.addEntry(ActivityEntryUnits.from(unitFormatter)) },
                    onDiscardRecordingDraft = viewModel::discardRecordingDraft,
                    isEditMode = state.isEditMode,
                    modifier = cardModifier,
                )
            }
        }
    }
}
