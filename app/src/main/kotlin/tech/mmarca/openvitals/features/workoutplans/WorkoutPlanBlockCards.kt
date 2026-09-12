package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.features.manualentry.activity.FieldErrorText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

private val GoalFieldWidth = 96.dp

@Composable
internal fun WorkoutPlanBlockCard(
    index: Int,
    block: WorkoutPlanBlockInput,
    state: WorkoutPlanBuilderUiState,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onNameChanged: (String) -> Unit,
    onRoundsChanged: (String) -> Unit,
    onMoveBlock: (Int) -> Unit,
    onRemoveBlock: () -> Unit,
    onAddExercise: () -> Unit,
    onAddRest: () -> Unit,
    onStepGoalTypeChanged: (String, WorkoutPlanGoalType) -> Unit,
    onStepGoalValueChanged: (String, String) -> Unit,
    onStepDurationUnitChanged: (String, WorkoutPlanDurationUnit) -> Unit,
    onStepSetsChanged: (String, String) -> Unit,
    onStepSetRestChanged: (String, String) -> Unit,
    onStepSetRestUnitChanged: (String, WorkoutPlanDurationUnit) -> Unit,
    onStepWeightChanged: (String, String) -> Unit,
    onStepDescriptionChanged: (String, String) -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onRemoveStep: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blockError = state.blockError(block.id)
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.workout_plan_block_title, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onMoveBlock(-1) }, enabled = enabled && canMoveUp) {
                    Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.workout_plan_move_up))
                }
                IconButton(onClick = { onMoveBlock(1) }, enabled = enabled && canMoveDown) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.workout_plan_move_down))
                }
                IconButton(onClick = onRemoveBlock, enabled = enabled) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.workout_plan_remove))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = block.nameText,
                    onValueChange = onNameChanged,
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.workout_plan_block_name_label)) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = block.roundsText,
                    onValueChange = onRoundsChanged,
                    enabled = enabled,
                    singleLine = true,
                    isError = blockError?.kind == WorkoutPlanValidationErrorKind.BLOCK_ROUNDS_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.workout_plan_block_rounds_label)) },
                    modifier = Modifier.width(GoalFieldWidth),
                )
            }
            FieldErrorText(blockError?.message())

            block.steps.forEachIndexed { stepIndex, step ->
                if (stepIndex > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill))
                }
                WorkoutPlanStepRow(
                    step = step,
                    errors = state.stepErrors(step.id),
                    enabled = enabled,
                    canMoveUp = stepIndex > 0,
                    canMoveDown = stepIndex < block.steps.lastIndex,
                    onGoalTypeChanged = { onStepGoalTypeChanged(step.id, it) },
                    onGoalValueChanged = { onStepGoalValueChanged(step.id, it) },
                    onDurationUnitChanged = { onStepDurationUnitChanged(step.id, it) },
                    onSetsChanged = { onStepSetsChanged(step.id, it) },
                    onSetRestChanged = { onStepSetRestChanged(step.id, it) },
                    onSetRestUnitChanged = { onStepSetRestUnitChanged(step.id, it) },
                    onWeightChanged = { onStepWeightChanged(step.id, it) },
                    onDescriptionChanged = { onStepDescriptionChanged(step.id, it) },
                    onMoveUp = { onMoveStep(stepIndex, stepIndex - 1) },
                    onMoveDown = { onMoveStep(stepIndex, stepIndex + 1) },
                    onRemove = { onRemoveStep(step.id) },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OpenVitalsOutlinedButton(
                    onClick = onAddExercise,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.workout_plan_add_exercise),
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
                OpenVitalsOutlinedButton(
                    onClick = onAddRest,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Hotel, contentDescription = null)
                    Text(
                        text = stringResource(R.string.workout_plan_add_rest),
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }
        }
    }
}

/** What the goal control offers: reps, or a duration typed in seconds or minutes. */
private enum class GoalChoice(val labelRes: Int) {
    REPS(R.string.workout_plan_goal_reps),
    SECONDS(R.string.workout_plan_goal_seconds_short),
    MINUTES(R.string.workout_plan_goal_minutes_short),
}

private fun WorkoutPlanStepInput.goalChoice(): GoalChoice = when {
    goalType == WorkoutPlanGoalType.REPETITIONS -> GoalChoice.REPS
    durationUnit == WorkoutPlanDurationUnit.MINUTES -> GoalChoice.MINUTES
    else -> GoalChoice.SECONDS
}

@Composable
private fun WorkoutPlanStepRow(
    step: WorkoutPlanStepInput,
    errors: List<WorkoutPlanValidationError>,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onGoalTypeChanged: (WorkoutPlanGoalType) -> Unit,
    onGoalValueChanged: (String) -> Unit,
    onDurationUnitChanged: (WorkoutPlanDurationUnit) -> Unit,
    onSetsChanged: (String) -> Unit,
    onSetRestChanged: (String) -> Unit,
    onSetRestUnitChanged: (WorkoutPlanDurationUnit) -> Unit,
    onWeightChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    fun error(kind: WorkoutPlanValidationErrorKind): WorkoutPlanValidationError? = errors.firstOrNull { it.kind == kind }
    val goalError = error(WorkoutPlanValidationErrorKind.STEP_GOAL_INVALID)
    val setsError = error(WorkoutPlanValidationErrorKind.STEP_SETS_INVALID)
    val setRestError = error(WorkoutPlanValidationErrorKind.STEP_SET_REST_INVALID)
    val weightError = error(WorkoutPlanValidationErrorKind.STEP_WEIGHT_INVALID)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (step.kind) {
                        WorkoutPlanStepKind.REST -> stringResource(R.string.workout_plan_step_rest)
                        WorkoutPlanStepKind.ACTIVE,
                        WorkoutPlanStepKind.UNSUPPORTED,
                        -> stepLabel(step)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (step.kind == WorkoutPlanStepKind.UNSUPPORTED) {
                    val summary = step.raw?.let { completionText(it.completion) }
                    Text(
                        text = listOfNotNull(summary, stringResource(R.string.workout_plan_step_unsupported)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onMoveUp, enabled = enabled && canMoveUp) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.workout_plan_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = enabled && canMoveDown) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.workout_plan_move_down))
            }
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.workout_plan_remove))
            }
        }

        val targets = step.performanceTargets.mapNotNull { performanceTargetText(it) }
        if (targets.isNotEmpty()) {
            Text(
                text = stringResource(R.string.workout_plan_targets_kept, targets.joinToString(" · ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (step.kind == WorkoutPlanStepKind.UNSUPPORTED) return@Column

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step.kind == WorkoutPlanStepKind.ACTIVE) {
                val selected = step.goalChoice()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    GoalChoice.entries.forEachIndexed { index, choice ->
                        SegmentedButton(
                            selected = selected == choice,
                            onClick = {
                                when (choice) {
                                    GoalChoice.REPS -> onGoalTypeChanged(WorkoutPlanGoalType.REPETITIONS)
                                    GoalChoice.SECONDS -> {
                                        onGoalTypeChanged(WorkoutPlanGoalType.DURATION)
                                        onDurationUnitChanged(WorkoutPlanDurationUnit.SECONDS)
                                    }
                                    GoalChoice.MINUTES -> {
                                        onGoalTypeChanged(WorkoutPlanGoalType.DURATION)
                                        onDurationUnitChanged(WorkoutPlanDurationUnit.MINUTES)
                                    }
                                }
                            },
                            enabled = enabled,
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GoalChoice.entries.size),
                        ) {
                            Text(stringResource(choice.labelRes))
                        }
                    }
                }
            } else {
                DurationUnitButtons(
                    unit = step.durationUnit,
                    enabled = enabled,
                    onUnitChanged = onDurationUnitChanged,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = step.goalValueText,
                onValueChange = onGoalValueChanged,
                enabled = enabled,
                singleLine = true,
                isError = goalError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(GoalFieldWidth),
            )
        }
        FieldErrorText(goalError?.message())

        if (step.kind == WorkoutPlanStepKind.ACTIVE) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = step.setsText,
                    onValueChange = onSetsChanged,
                    enabled = enabled,
                    singleLine = true,
                    isError = setsError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.workout_plan_step_sets_label)) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = step.weightKgText,
                    onValueChange = onWeightChanged,
                    enabled = enabled,
                    singleLine = true,
                    isError = weightError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.workout_plan_step_weight_label)) },
                    modifier = Modifier.weight(1f),
                )
            }
            FieldErrorText(setsError?.message())
            FieldErrorText(weightError?.message())
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = step.setRestText,
                    onValueChange = onSetRestChanged,
                    enabled = enabled,
                    singleLine = true,
                    isError = setRestError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.workout_plan_step_set_rest_label)) },
                    modifier = Modifier.weight(1f),
                )
                DurationUnitButtons(
                    unit = step.setRestUnit,
                    enabled = enabled,
                    onUnitChanged = onSetRestUnitChanged,
                )
            }
            FieldErrorText(setRestError?.message())
            OutlinedTextField(
                value = step.descriptionText,
                onValueChange = onDescriptionChanged,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.workout_plan_step_description_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "sec | min" for a duration field. */
@Composable
private fun DurationUnitButtons(
    unit: WorkoutPlanDurationUnit,
    enabled: Boolean,
    onUnitChanged: (WorkoutPlanDurationUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        WorkoutPlanDurationUnit.entries.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = unit == choice,
                onClick = { onUnitChanged(choice) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = WorkoutPlanDurationUnit.entries.size),
            ) {
                Text(
                    stringResource(
                        when (choice) {
                            WorkoutPlanDurationUnit.SECONDS -> R.string.workout_plan_goal_seconds_short
                            WorkoutPlanDurationUnit.MINUTES -> R.string.workout_plan_goal_minutes_short
                        },
                    ),
                )
            }
        }
    }
}
