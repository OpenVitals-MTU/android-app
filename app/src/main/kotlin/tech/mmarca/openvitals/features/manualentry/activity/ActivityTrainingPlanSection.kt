package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityTrainingPlanSection(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onCreateNewPlannedWorkout: () -> Unit,
    onApplyPlannedWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.selectedActivityType.supportsSetRepetitions) return

    val plans = state.plannedWorkouts
        .filter { plan -> plan.toActivityEntryType()?.id == state.selectedActivityType.id }
        .sortedBy { it.startTime }
    val selectedPlan = plans.firstOrNull { it.id == state.selectedPlannedWorkoutId }
    var expanded by remember { mutableStateOf(false) }
    val dropdownEnabled = enabled && !state.isLoadingPlannedWorkouts

    ExposedDropdownMenuBox(
        expanded = expanded && dropdownEnabled,
        onExpandedChange = { expanded = dropdownEnabled && it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedPlan?.displayName()
                ?: when {
                    state.isLoadingPlannedWorkouts -> stringResource(R.string.activity_entry_training_plans_loading)
                    else -> stringResource(R.string.activity_entry_training_plan_new)
                },
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.activity_entry_training_plan_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && dropdownEnabled) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded && dropdownEnabled,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.activity_entry_training_plan_new),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    onCreateNewPlannedWorkout()
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = plan.displayName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = plan.summaryText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = {
                        onApplyPlannedWorkout(plan.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
internal fun ActivityTrainingPlanActions(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onSavePlannedWorkout: () -> Unit,
    onUpdatePlannedWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.selectedActivityType.supportsSetRepetitions) return

    if (state.selectedPlannedWorkoutId == null) {
        OpenVitalsOutlinedButton(
            onClick = onSavePlannedWorkout,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
            Text(
                text = stringResource(R.string.activity_entry_save_training_plan),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        } else if (state.hasSelectedPlannedWorkoutChanges) {
        OpenVitalsButton(
            onClick = onUpdatePlannedWorkout,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.activity_entry_update_training_plan))
        }
    } else {
        OpenVitalsOutlinedButton(
            onClick = onUpdatePlannedWorkout,
            enabled = false,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.activity_entry_update_training_plan))
        }
    }
}

@Composable
private fun PlannedExerciseData.displayName(): String =
    title ?: stringResource(R.string.activity_entry_training_plan_unnamed)

@Composable
private fun PlannedExerciseData.summaryText(): String {
    val sets = toRepetitionSetInputs()
    val totalReps = sets.sumOf { set -> set.repetitionsText.toIntOrNull() ?: 0 }
    return when {
        sets.isEmpty() -> stringResource(R.string.planned_workout_blocks, blockCount)
        sets.size == 1 -> stringResource(R.string.activity_entry_plan_one_set_summary, totalReps)
        else -> stringResource(R.string.activity_entry_plan_summary, sets.size, totalReps)
    }
}
