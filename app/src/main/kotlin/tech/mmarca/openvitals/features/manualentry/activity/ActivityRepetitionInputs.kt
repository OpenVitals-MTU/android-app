package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.activity.exerciseSegmentLabel
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanStepChoice
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanStepPickerSheet
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

private val GoalFieldWidth = 96.dp

/** Which row a picked exercise lands on. */
private sealed interface StepPickerTarget {
    data object Add : StepPickerTarget
    data class Change(val index: Int) : StepPickerTarget
}

/**
 * The session's repetition structure: one total, a step count, or a list
 * of steps. Generic set types name an exercise per step.
 */
@Composable
internal fun ActivityRepetitionInputs(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onModeChanged: (ActivityRepetitionEntryMode) -> Unit,
    onTotalChanged: (String) -> Unit,
    onSetRepetitionsChanged: (Int, String) -> Unit,
    onSetRestChanged: (Int, String) -> Unit,
    onSetGoalTypeChanged: (Int, Boolean) -> Unit,
    onSetExerciseChanged: (Int, WorkoutPlanStepChoice) -> Unit,
    onAddExercise: (WorkoutPlanStepChoice) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = state.selectedActivityType
    if (!type.isRepetitionLike) return
    val mixed = type.supportsMixedExercises
    var pickerTarget by remember { mutableStateOf<StepPickerTarget?>(null) }

    val errorText = state.validationErrorText(ActivityEntryField.REPETITIONS)
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(
                    if (type.repetitionUnit == ActivityRepetitionUnit.STEPS) {
                        R.string.activity_entry_steps_title
                    } else {
                        R.string.activity_entry_repetitions_title
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
            )

            when {
                type.repetitionUnit == ActivityRepetitionUnit.STEPS -> OutlinedTextField(
                    value = state.repetitionTotalText,
                    onValueChange = onTotalChanged,
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.activity_entry_steps_label)) },
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                !mixed && state.repetitionMode == ActivityRepetitionEntryMode.TOTAL -> {
                    RepetitionModeButtons(state.repetitionMode, enabled, onModeChanged)
                    OutlinedTextField(
                        value = state.repetitionTotalText,
                        onValueChange = onTotalChanged,
                        enabled = enabled,
                        singleLine = true,
                        label = { Text(stringResource(R.string.activity_entry_repetitions_label)) },
                        isError = errorText != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    if (!mixed) RepetitionModeButtons(state.repetitionMode, enabled, onModeChanged)
                    state.repetitionSets.forEachIndexed { index, set ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill))
                        }
                        StepRow(
                            index = index,
                            set = set,
                            ownSegmentType = type.segmentType,
                            showExercisePicker = mixed,
                            enabled = enabled,
                            isError = errorText != null,
                            canRemove = state.repetitionSets.size > 1,
                            onPickExercise = { pickerTarget = StepPickerTarget.Change(index) },
                            onGoalTypeChanged = { onSetGoalTypeChanged(index, it) },
                            onValueChanged = { onSetRepetitionsChanged(index, it) },
                            onRestChanged = { onSetRestChanged(index, it) },
                            onRemove = { onRemoveSet(index) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (mixed) {
                            OpenVitalsOutlinedButton(
                                onClick = { pickerTarget = StepPickerTarget.Add },
                                enabled = enabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(imageVector = Icons.Outlined.FitnessCenter, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.workout_plan_add_exercise),
                                    modifier = Modifier.padding(start = Spacing.sm),
                                )
                            }
                        }
                        OpenVitalsOutlinedButton(
                            onClick = onAddSet,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                            Text(
                                text = stringResource(R.string.activity_entry_add_set),
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                }
            }

            FieldErrorText(errorText)
        }
    }

    pickerTarget?.let { target ->
        WorkoutPlanStepPickerSheet(
            onPick = { choice ->
                when (target) {
                    StepPickerTarget.Add -> onAddExercise(choice)
                    is StepPickerTarget.Change -> onSetExerciseChanged(target.index, choice)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
            showRest = false,
        )
    }
}

@Composable
private fun StepRow(
    index: Int,
    set: ActivityRepetitionSetInput,
    ownSegmentType: Int?,
    showExercisePicker: Boolean,
    enabled: Boolean,
    isError: Boolean,
    canRemove: Boolean,
    onPickExercise: () -> Unit,
    onGoalTypeChanged: (Boolean) -> Unit,
    onValueChanged: (String) -> Unit,
    onRestChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val exerciseLabel = set.label
        ?: set.segmentType?.takeIf { it != ownSegmentType }?.let { exerciseSegmentLabel(it) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (showExercisePicker) {
            ActivityPickerField(
                label = stringResource(R.string.activity_entry_step_exercise_label),
                value = exerciseLabel
                    ?: set.segmentType?.let { exerciseSegmentLabel(it) }
                    ?: stringResource(R.string.activity_entry_step_choose_exercise),
                icon = Icons.Outlined.FitnessCenter,
                enabled = enabled,
                isError = false,
                onClick = onPickExercise,
            )
        } else if (exerciseLabel != null) {
            Text(
                text = exerciseLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                listOf(false to R.string.workout_plan_goal_reps, true to R.string.workout_plan_goal_seconds)
                    .forEachIndexed { buttonIndex, (isDuration, labelRes) ->
                        SegmentedButton(
                            selected = set.isDuration == isDuration,
                            onClick = { onGoalTypeChanged(isDuration) },
                            enabled = enabled,
                            shape = SegmentedButtonDefaults.itemShape(index = buttonIndex, count = 2),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
            }
            OutlinedTextField(
                value = set.repetitionsText,
                onValueChange = onValueChanged,
                enabled = enabled,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            if (set.isDuration) {
                                R.string.activity_entry_set_seconds_label
                            } else {
                                R.string.activity_entry_set_repetitions_label
                            },
                            index + 1,
                        ),
                    )
                },
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(GoalFieldWidth),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = set.restMinutesText,
                onValueChange = onRestChanged,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_set_rest_label)) },
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OpenVitalsIconButton(onClick = onRemove, enabled = enabled && canRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_entry),
                )
            }
        }
    }
}

@Composable
private fun RepetitionModeButtons(
    selectedMode: ActivityRepetitionEntryMode,
    enabled: Boolean,
    onModeChanged: (ActivityRepetitionEntryMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val totalButton: @Composable RowScope.() -> Unit = {
            Text(stringResource(R.string.activity_entry_repetition_mode_total))
        }
        val setsButton: @Composable RowScope.() -> Unit = {
            Text(stringResource(R.string.activity_entry_repetition_mode_sets))
        }
        if (selectedMode == ActivityRepetitionEntryMode.TOTAL) {
            OpenVitalsButton(onClick = { onModeChanged(ActivityRepetitionEntryMode.TOTAL) }, enabled = enabled, modifier = Modifier.weight(1f), content = totalButton)
        } else {
            OpenVitalsOutlinedButton(onClick = { onModeChanged(ActivityRepetitionEntryMode.TOTAL) }, enabled = enabled, modifier = Modifier.weight(1f), content = totalButton)
        }
        if (selectedMode == ActivityRepetitionEntryMode.SETS) {
            OpenVitalsButton(onClick = { onModeChanged(ActivityRepetitionEntryMode.SETS) }, enabled = enabled, modifier = Modifier.weight(1f), content = setsButton)
        } else {
            OpenVitalsOutlinedButton(onClick = { onModeChanged(ActivityRepetitionEntryMode.SETS) }, enabled = enabled, modifier = Modifier.weight(1f), content = setsButton)
        }
    }
}
