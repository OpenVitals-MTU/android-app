package tech.mmarca.openvitals.features.manualentry.body

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.WeightColor

private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54

@Composable
fun BodyMeasurementEntryScreen(
    type: BodyMeasurementType,
    viewModel: BodyMeasurementEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LaunchedEffect(type, unitFormatter.unitSystem()) {
        viewModel.setType(type, unitFormatter.unitSystem())
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
            BodyMeasurementEntryCard(
                state = state,
                unitFormatter = unitFormatter,
                onInputChanged = viewModel::updateInput,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onAddEntry = {
                    viewModel.addEntry(
                        canonicalBodyMeasurementValue(
                            input = state.inputText,
                            type = state.type,
                            unitSystem = unitFormatter.unitSystem(),
                        )
                    )
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
private fun BodyMeasurementEntryCard(
    state: BodyMeasurementEntryUiState,
    unitFormatter: UnitFormatter,
    onInputChanged: (String) -> Unit,
    onEntryTimeChanged: (java.time.Instant) -> Unit,
    onAddEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(state.type.titleRes())
    val unitLabel = state.type.inputUnitLabel(unitFormatter.unitSystem())
    val enabled = state.canWrite && !state.isSavingEntry && !state.isCheckingPermission
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                    imageVector = state.type.icon(),
                    contentDescription = null,
                    tint = state.type.accentColor(),
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
                                R.string.body_entry_subtitle
                            } else {
                                R.string.body_entry_permission_needed
                            },
                            title,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.canWrite && !state.isCheckingPermission) {
                    OutlinedButton(onClick = onRequestWritePermission) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            }

            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInputChanged,
                enabled = !state.isSavingEntry,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.body_entry_value_label,
                            title,
                            unitLabel,
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.isEditMode) {
                ManualEntryTimestampFields(
                    timestamp = state.editTime,
                    enabled = !state.isSavingEntry,
                    onTimestampChanged = onEntryTimeChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = onAddEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (state.isEditMode) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(R.string.body_entry_add_selected, title)
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = bodyMeasurementEntryErrorText(entryError, state.writeErrorMessage, title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun bodyMeasurementEntryErrorText(
    error: BodyMeasurementEntryError,
    message: String?,
    title: String,
): String = when (error) {
    BodyMeasurementEntryError.INVALID_VALUE -> stringResource(R.string.body_entry_invalid_value)
    BodyMeasurementEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.body_entry_permission_needed, title)
    BodyMeasurementEntryError.WRITE_FAILED -> stringResource(
        R.string.body_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private fun canonicalBodyMeasurementValue(
    input: String,
    type: BodyMeasurementType,
    unitSystem: UnitSystem,
): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return when (type) {
        BodyMeasurementType.WEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) {
            value / PoundsPerKilogram
        } else {
            value
        }
        BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) {
            value * CentimetersPerInch
        } else {
            value
        }
        BodyMeasurementType.BODY_FAT -> value
    }
}

@StringRes
fun BodyMeasurementType.titleRes(): Int = when (this) {
    BodyMeasurementType.WEIGHT -> R.string.metric_weight
    BodyMeasurementType.HEIGHT -> R.string.metric_height
    BodyMeasurementType.BODY_FAT -> R.string.metric_body_fat
}

private fun BodyMeasurementType.inputUnitLabel(unitSystem: UnitSystem): String = when (this) {
    BodyMeasurementType.WEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
    BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) "in" else "cm"
    BodyMeasurementType.BODY_FAT -> "%"
}

fun BodyMeasurementType.icon(): ImageVector = when (this) {
    BodyMeasurementType.HEIGHT -> Icons.Outlined.Straighten
    else -> Icons.Outlined.MonitorWeight
}

fun BodyMeasurementType.accentColor(): Color = when (this) {
    BodyMeasurementType.BODY_FAT -> BodyFatColor
    else -> WeightColor
}
