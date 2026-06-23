package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.R

@Composable
internal fun ActivityPlanActivityPickerCard(
    state: ActivityEntryUiState,
    onSelectActivity: (String) -> Unit,
    onChooseSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activityTypes = state.plannedWorkouts
        .mapNotNull { it.toActivityEntryType() }
        .distinctBy { it.id }
        .sortedBy { it.id }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_entry_plan_activity_picker_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.isLoadingPlannedWorkouts) {
                Text(
                    text = stringResource(R.string.activity_entry_training_plans_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (activityTypes.isEmpty()) {
                Text(
                    text = stringResource(R.string.activity_entry_plan_activity_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                activityTypes.forEach { type ->
                    OutlinedButton(
                        onClick = { onSelectActivity(type.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(type.labelRes),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onChooseSource,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.activity_entry_choose_another_source),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun ActivityPlanPickerCard(
    state: ActivityEntryUiState,
    onSelectPlan: (String) -> Unit,
    onChooseActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTypeId = state.selectedPlannedWorkoutActivityTypeId
    val plans = state.plannedWorkouts
        .filter { plan -> plan.toActivityEntryType()?.id == selectedTypeId }
        .sortedBy { it.startTime }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_entry_plan_picker_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (plans.isEmpty()) {
                Text(
                    text = stringResource(R.string.activity_entry_plan_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                plans.forEach { plan ->
                    PlannedWorkoutButton(
                        plan = plan,
                        onClick = { onSelectPlan(plan.id) },
                    )
                }
            }
            OutlinedButton(
                onClick = onChooseActivity,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.activity_entry_plan_choose_activity),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PlannedWorkoutButton(
    plan: PlannedExerciseData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sets = plan.toRepetitionSetInputs()
    val totalReps = sets.sumOf { set -> set.repetitionsText.toIntOrNull() ?: 0 }
    val summary = when {
        sets.isEmpty() -> stringResource(R.string.planned_workout_blocks, plan.blockCount)
        sets.size == 1 -> stringResource(R.string.activity_entry_plan_one_set_summary, totalReps)
        else -> stringResource(R.string.activity_entry_plan_summary, sets.size, totalReps)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = plan.title ?: stringResource(R.string.activity_entry_training_plan_unnamed),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PlanSetPreview(sets = sets)
        }
    }
}

@Composable
private fun PlanSetPreview(sets: List<ActivityRepetitionSetInput>) {
    if (sets.isEmpty()) return
    val parts = sets.flatMapIndexed { index, set ->
        buildList {
            set.repetitionsText.toIntOrNull()?.let { reps ->
                add(stringResource(R.string.activity_entry_plan_preview_reps, reps))
            }
            set.restMinutesText.toLongOrNull()?.takeIf { it > 0L && index < sets.lastIndex }?.let { rest ->
                add(stringResource(R.string.activity_entry_plan_preview_rest, rest))
            }
        }
    }
    if (parts.isEmpty()) return

    val shown = parts.take(MaxPlanPreviewParts)
    val remaining = parts.size - shown.size
    val text = buildString {
        append(shown.joinToString(" • "))
        if (remaining > 0) {
            append(" • ")
            append(stringResource(R.string.activity_entry_plan_preview_more, remaining))
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private const val MaxPlanPreviewParts = 5
