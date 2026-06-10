package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R

@Composable
internal fun ActivityRepetitionInputs(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onModeChanged: (ActivityRepetitionEntryMode) -> Unit,
    onTotalChanged: (String) -> Unit,
    onSetRepetitionsChanged: (Int, String) -> Unit,
    onSetRestChanged: (Int, String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = state.selectedActivityType
    if (!type.isRepetitionLike) return

    val errorText = state.validationErrorText(ActivityEntryField.REPETITIONS)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(
                    if (type.repetitionUnit == ActivityRepetitionUnit.STEPS) {
                        R.string.activity_entry_steps_title
                    } else {
                        R.string.activity_entry_repetitions_title
                    }
                ),
                style = MaterialTheme.typography.titleSmall,
            )

            if (type.repetitionUnit == ActivityRepetitionUnit.STEPS) {
                OutlinedTextField(
                    value = state.repetitionTotalText,
                    onValueChange = onTotalChanged,
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.activity_entry_steps_label)) },
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                RepetitionModeButtons(
                    selectedMode = state.repetitionMode,
                    enabled = enabled,
                    onModeChanged = onModeChanged,
                )

                if (state.repetitionMode == ActivityRepetitionEntryMode.TOTAL) {
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
                } else {
                    state.repetitionSets.forEachIndexed { index, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = set.repetitionsText,
                                onValueChange = { onSetRepetitionsChanged(index, it) },
                                enabled = enabled,
                                singleLine = true,
                                label = { Text(stringResource(R.string.activity_entry_set_repetitions_label, index + 1)) },
                                isError = errorText != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = set.restMinutesText,
                                onValueChange = { onSetRestChanged(index, it) },
                                enabled = enabled && index < state.repetitionSets.lastIndex,
                                singleLine = true,
                                label = { Text(stringResource(R.string.activity_entry_set_rest_label)) },
                                isError = errorText != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onRemoveSet(index) },
                                enabled = enabled && state.repetitionSets.size > 1,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_entry),
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onAddSet,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.activity_entry_add_set),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }

            FieldErrorText(errorText)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val totalButton: @Composable RowScope.() -> Unit = {
            Text(stringResource(R.string.activity_entry_repetition_mode_total))
        }
        val setsButton: @Composable RowScope.() -> Unit = {
            Text(stringResource(R.string.activity_entry_repetition_mode_sets))
        }
        if (selectedMode == ActivityRepetitionEntryMode.TOTAL) {
            Button(
                onClick = { onModeChanged(ActivityRepetitionEntryMode.TOTAL) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                content = totalButton,
            )
        } else {
            OutlinedButton(
                onClick = { onModeChanged(ActivityRepetitionEntryMode.TOTAL) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                content = totalButton,
            )
        }
        if (selectedMode == ActivityRepetitionEntryMode.SETS) {
            Button(
                onClick = { onModeChanged(ActivityRepetitionEntryMode.SETS) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                content = setsButton,
            )
        } else {
            OutlinedButton(
                onClick = { onModeChanged(ActivityRepetitionEntryMode.SETS) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                content = setsButton,
            )
        }
    }
}
