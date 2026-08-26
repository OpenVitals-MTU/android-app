package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.Emphasis

private val StepIconSize = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutPlanStepPickerSheet(
    onPick: (WorkoutPlanStepChoice) -> Unit,
    onDismiss: () -> Unit,
    onPickRest: () -> Unit = {},
    showRest: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val labelled = remember(context) {
        val presets = WorkoutPlanStepCatalog.filter { it.description != null }
        val rest = WorkoutPlanStepCatalog.filter { it.description == null }
        (presets + rest.sortedBy { it.label(context).lowercase() }).map { it to it.label(context) }
    }
    val choices = remember(query, labelled) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) labelled else labelled.filter { (_, label) -> label.lowercase().contains(needle) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.workout_plan_search_exercises)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )
            LazyColumn(modifier = Modifier.padding(top = Spacing.sm)) {
                if (showRest && query.isBlank()) {
                    item(key = "rest") {
                        PickerRow(
                            label = stringResource(R.string.workout_plan_step_rest),
                            leading = { Icon(imageVector = Icons.Outlined.Hotel, contentDescription = null, modifier = Modifier.size(StepIconSize)) },
                            onClick = onPickRest,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill))
                    }
                }
                items(choices, key = { (choice, _) -> "${choice.segmentType}:${choice.description.orEmpty()}" }) { (choice, label) ->
                    PickerRow(
                        label = label,
                        leading = { Icon(imageVector = Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(StepIconSize)) },
                        onClick = { onPick(choice) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.lg),
        )
    }
}
