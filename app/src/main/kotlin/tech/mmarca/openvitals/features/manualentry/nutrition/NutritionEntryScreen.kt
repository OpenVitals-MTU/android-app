package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimestampFields
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.NutritionColor

@Composable
fun NutritionEntryScreen(
    viewModel: NutritionEntryViewModel,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberManualEntryWritePermissionRequester {
        viewModel.refreshPermission()
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermission()
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            NutritionEntryCard(
                state = state,
                onAmountChanged = viewModel::updateAmount,
                onAddNutrient = viewModel::addNutrient,
                onRemoveNutrient = viewModel::removeNutrient,
                onTimestampChanged = viewModel::updateTimestamp,
                onAddEntry = viewModel::addEntry,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun NutritionEntryCard(
    state: NutritionEntryUiState,
    onAmountChanged: (NutritionNutrient, String) -> Unit,
    onAddNutrient: (NutritionNutrient) -> Unit,
    onRemoveNutrient: (NutritionNutrient) -> Unit,
    onTimestampChanged: (Instant) -> Unit,
    onAddEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nutrientComparator = nutrientTitleComparator(LocalContext.current.resources)
    var nutrientChooserOpen by rememberSaveable { mutableStateOf(false) }
    val fieldsEnabled = !state.isSavingEntry && (!state.isEditMode || state.isEditEntryLoaded)
    // The main nutrients keep their order; the added ones read alphabetically below them.
    val primaryRows = state.rows.filter { it.nutrient in PrimaryNutritionEntryNutrients }
    val addedRows = remember(state.rows, nutrientComparator) {
        state.rows
            .filterNot { it.nutrient in PrimaryNutritionEntryNutrients }
            .sortedByTitle(nutrientComparator)
    }

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = NutritionColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.screen_nutrition),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.nutrition_entry_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!state.canWrite && !state.isCheckingPermission) {
                ManualEntryWritePermissionCallout(
                    body = stringResource(R.string.nutrition_entry_permission_needed),
                    onGrant = onRequestWritePermission,
                )
            }

            ManualEntryTimestampFields(
                timestamp = state.timestamp,
                enabled = fieldsEnabled,
                onTimestampChanged = onTimestampChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            primaryRows.forEach { row ->
                NutrientAmountRow(
                    row = row,
                    onAmountChanged = { text -> onAmountChanged(row.nutrient, text) },
                    onRemove = null,
                    enabled = fieldsEnabled,
                )
            }
            addedRows.forEach { row ->
                NutrientAmountRow(
                    row = row,
                    onAmountChanged = { text -> onAmountChanged(row.nutrient, text) },
                    onRemove = { onRemoveNutrient(row.nutrient) },
                    enabled = fieldsEnabled,
                )
            }
            AddNutrientButton(
                enabled = fieldsEnabled && state.addableNutrients.isNotEmpty(),
                onClick = { nutrientChooserOpen = true },
                labelRes = R.string.nutrition_entry_add_another_nutrient,
            )

            OpenVitalsButton(
                onClick = onAddEntry,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        if (state.isEditMode) R.string.action_save else R.string.nutrition_entry_add,
                    ),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = nutritionEntryErrorText(entryError, state.writeError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (nutrientChooserOpen) {
        NutrientChooserDialog(
            availableNutrients = state.addableNutrients.sortedWith(nutrientComparator),
            onDismiss = { nutrientChooserOpen = false },
            onSelectNutrient = { nutrient ->
                onAddNutrient(nutrient)
                nutrientChooserOpen = false
            },
        )
    }
}

@Composable
private fun nutritionEntryErrorText(
    error: NutritionEntryError,
    writeError: ScreenError?,
): String = when (error) {
    NutritionEntryError.NO_VALUES -> stringResource(R.string.nutrition_entry_no_values)
    NutritionEntryError.INVALID_VALUE -> stringResource(R.string.nutrition_entry_invalid_value)
    NutritionEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.nutrition_entry_permission_needed)
    NutritionEntryError.WRITE_FAILED -> stringResource(
        R.string.nutrition_entry_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}
