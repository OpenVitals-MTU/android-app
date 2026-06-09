package tech.mmarca.openvitals.features.manualentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter

@Composable
internal fun ActivityEntryCard(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter,
    onSelectActivityType: (ActivityEntryType) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onRepetitionModeChanged: (ActivityRepetitionEntryMode) -> Unit,
    onRepetitionTotalChanged: (String) -> Unit,
    onRepetitionSetRepetitionsChanged: (Int, String) -> Unit,
    onRepetitionSetRestChanged: (Int, String) -> Unit,
    onAddRepetitionSet: () -> Unit,
    onRemoveRepetitionSet: (Int) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onElevationChanged: (String) -> Unit,
    onActiveCaloriesChanged: (String) -> Unit,
    onTotalCaloriesChanged: (String) -> Unit,
    onClearRoute: () -> Unit,
    onChooseSource: () -> Unit,
    onRequestWritePermission: () -> Unit,
    onAddEntry: () -> Unit,
    onDiscardRecordingDraft: () -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission && !state.isImportingRoute
    val durationError = state.validationErrorText(ActivityEntryField.DURATION)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActivityEntryHeader(state = state, onRequestWritePermission = onRequestWritePermission)

            if (!isEditMode) {
                OutlinedButton(
                    onClick = onChooseSource,
                    enabled = !state.isSavingEntry && !state.isImportingRoute,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.activity_entry_choose_another_source))
                }
            }

            ActivityTypeSelector(
                types = if (state.importedRoute == null) {
                    state.activityTypes
                } else {
                    state.activityTypes.filter { it.supportsGpsRoute }
                },
                selectedType = state.selectedActivityType,
                onSelectActivityType = onSelectActivityType,
                errorText = state.validationErrorText(ActivityEntryField.ACTIVITY_TYPE),
            )

            OutlinedTextField(
                value = state.titleText,
                onValueChange = onTitleChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_title_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            ActivityStartDateTimeFields(
                state = state,
                enabled = !state.isSavingEntry,
                onStartDateChanged = onStartDateChanged,
                onStartTimeChanged = onStartTimeChanged,
            )

            OutlinedTextField(
                value = state.durationMinutesText,
                onValueChange = onDurationChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.activity_entry_duration_label)) },
                isError = durationError != null,
                supportingText = durationError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            ActivityRepetitionInputs(
                state = state,
                enabled = !state.isSavingEntry,
                onModeChanged = onRepetitionModeChanged,
                onTotalChanged = onRepetitionTotalChanged,
                onSetRepetitionsChanged = onRepetitionSetRepetitionsChanged,
                onSetRestChanged = onRepetitionSetRestChanged,
                onAddSet = onAddRepetitionSet,
                onRemoveSet = onRemoveRepetitionSet,
            )

            ActivityMetricInputs(
                state = state,
                unitSystem = unitFormatter.unitSystem(),
                enabled = true,
                onDistanceChanged = onDistanceChanged,
                onElevationChanged = onElevationChanged,
                onActiveCaloriesChanged = onActiveCaloriesChanged,
                onTotalCaloriesChanged = onTotalCaloriesChanged,
            )

            OutlinedTextField(
                value = state.notesText,
                onValueChange = onNotesChanged,
                enabled = !state.isSavingEntry,
                minLines = 2,
                label = { Text(stringResource(R.string.activity_entry_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            ImportedActivityRouteSection(
                state = state,
                unitFormatter = unitFormatter,
                onClearRoute = onClearRoute,
            )

            Button(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(if (isEditMode) R.string.action_save else R.string.activity_entry_add),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            if (state.isRecordingDraft && !isEditMode) {
                OutlinedButton(
                    onClick = onDiscardRecordingDraft,
                    enabled = !state.isSavingEntry && !state.isImportingRoute,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.action_discard),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
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
