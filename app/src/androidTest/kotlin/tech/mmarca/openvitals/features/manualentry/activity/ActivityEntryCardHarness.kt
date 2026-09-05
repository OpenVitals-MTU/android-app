package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.runtime.Composable
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.testing.testUnitFormatter

/** `ActivityEntryCard` with every callback defaulted away. */
@Composable
internal fun TestActivityEntryCard(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter = testUnitFormatter(),
    onSelectActivityType: (ActivityEntryType) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    onFeelingChanged: (ActivityEntryFeeling?) -> Unit = {},
    onNotesChanged: (String) -> Unit = {},
    onDurationChanged: (String) -> Unit = {},
    onClearRoute: () -> Unit = {},
    onChooseSource: () -> Unit = {},
    onAddEntry: () -> Unit = {},
    onEditPlan: (String) -> Unit = {},
    isEditMode: Boolean = false,
) {
    ActivityEntryCard(
        state = state,
        unitFormatter = unitFormatter,
        onSelectActivityType = onSelectActivityType,
        onTitleChanged = onTitleChanged,
        onFeelingChanged = onFeelingChanged,
        onNotesChanged = onNotesChanged,
        onStartDateChanged = {},
        onStartTimeChanged = {},
        onDurationChanged = onDurationChanged,
        onRepetitionModeChanged = {},
        onRepetitionTotalChanged = {},
        onRepetitionSetRepetitionsChanged = { _, _ -> },
        onRepetitionSetRestChanged = { _, _ -> },
        onRepetitionSetGoalTypeChanged = { _, _ -> },
        onRepetitionSetExerciseChanged = { _, _ -> },
        onAddExerciseStep = {},
        onAddRepetitionSet = {},
        onRemoveRepetitionSet = {},
        onChangePlan = {},
        onEditPlan = onEditPlan,
        onClearLinkedPlan = {},
        onSaveAsPlan = {},
        onDistanceChanged = {},
        onElevationChanged = {},
        onActiveCaloriesChanged = {},
        onTotalCaloriesChanged = {},
        onClearRoute = onClearRoute,
        onChooseSource = onChooseSource,
        onRequestWritePermission = {},
        onAddEntry = onAddEntry,
        onDiscardRecordingDraft = {},
        isEditMode = isEditMode,
    )
}

/** Running: a GPS type with no repetitions and no steps. */
internal val runningEntryType: ActivityEntryType =
    DefaultActivityEntryTypes.first { it.labelRes == R.string.exercise_type_running }

/** Walking: step-counted, so it gets a single total and no Total/Sets switch. */
internal val walkingEntryType: ActivityEntryType =
    DefaultActivityEntryTypes.first { it.labelRes == R.string.exercise_type_walking }

/** Push-ups: repetition-counted, and deliberately not GPS-capable. */
internal val pushUpsEntryType: ActivityEntryType =
    checkNotNull(activityEntryTypeById("push_ups"))
