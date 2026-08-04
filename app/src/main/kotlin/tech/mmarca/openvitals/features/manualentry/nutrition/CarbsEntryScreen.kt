package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.NutritionColor

private const val GramsPerOunce = 28.349523125

@Composable
fun CarbsEntryScreen(
    viewModel: CarbsEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
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
            CarbsEntryCard(
                state = state,
                unitSystem = unitFormatter.unitSystem(),
                onInputChanged = viewModel::updateInput,
                onAddEntry = {
                    viewModel.addEntry(canonicalCarbsGrams(state.inputText, unitFormatter.unitSystem()))
                },
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.writePermissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CarbsEntryCard(
    state: CarbsEntryUiState,
    unitSystem: UnitSystem,
    onInputChanged: (String) -> Unit,
    onAddEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.metric_carbs)
    val unitLabel = carbsInputUnitLabel(unitSystem)
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission
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
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (state.canWrite) {
                                R.string.carbs_entry_subtitle
                            } else {
                                R.string.carbs_entry_permission_needed
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.canWrite && !state.isCheckingPermission) {
                    OpenVitalsOutlinedButton(onClick = onRequestWritePermission) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            }

            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInputChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = { Text(stringResource(R.string.carbs_entry_value_label, unitLabel)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OpenVitalsButton(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.carbs_entry_add),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = carbsEntryErrorText(entryError, state.writeError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun carbsEntryErrorText(
    error: CarbsEntryError,
    writeError: ScreenError?,
): String = when (error) {
    CarbsEntryError.INVALID_VALUE -> stringResource(R.string.carbs_entry_invalid_value)
    CarbsEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.carbs_entry_permission_needed)
    CarbsEntryError.WRITE_FAILED -> stringResource(
        R.string.carbs_entry_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}

internal fun canonicalCarbsGrams(input: String, unitSystem: UnitSystem): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value
        UnitSystem.IMPERIAL -> value * GramsPerOunce
    }
}

private fun carbsInputUnitLabel(unitSystem: UnitSystem): String =
    when (unitSystem) {
        UnitSystem.METRIC -> "g"
        UnitSystem.IMPERIAL -> "oz"
    }
