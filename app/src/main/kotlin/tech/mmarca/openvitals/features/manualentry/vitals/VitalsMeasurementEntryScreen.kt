package tech.mmarca.openvitals.features.manualentry.vitals

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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.theme.VitalsColor

private const val FahrenheitFreezingPoint = 32.0
private const val FahrenheitPerCelsius = 1.8
private val OxygenColor = Color(0xFF00897B)
private val RespiratoryColor = Color(0xFF5E97F6)
private val TemperatureColor = Color(0xFFFF7043)

@Composable
fun VitalsMeasurementEntryScreen(
    type: VitalsMeasurementType,
    viewModel: VitalsMeasurementEntryViewModel,
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
            VitalsMeasurementEntryCard(
                state = state,
                unitFormatter = unitFormatter,
                onInputChanged = viewModel::updateInput,
                onSecondaryInputChanged = viewModel::updateSecondaryInput,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onAddEntry = {
                    viewModel.addEntry(
                        value = canonicalVitalsValue(
                            input = state.inputText,
                            type = state.type,
                            unitSystem = unitFormatter.unitSystem(),
                        ),
                        secondaryValue = if (state.type == VitalsMeasurementType.BLOOD_PRESSURE) {
                            state.secondaryInputText.toVitalsDoubleOrNull()
                        } else {
                            null
                        },
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
private fun VitalsMeasurementEntryCard(
    state: VitalsMeasurementEntryUiState,
    unitFormatter: UnitFormatter,
    onInputChanged: (String) -> Unit,
    onSecondaryInputChanged: (String) -> Unit,
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
                                R.string.vitals_entry_subtitle
                            } else {
                                R.string.vitals_entry_permission_needed
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

            if (state.type == VitalsMeasurementType.BLOOD_PRESSURE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VitalsValueField(
                        value = state.inputText,
                        onValueChange = onInputChanged,
                        enabled = !state.isSavingEntry,
                        label = stringResource(R.string.vitals_entry_systolic_label),
                        modifier = Modifier.weight(1f),
                    )
                    VitalsValueField(
                        value = state.secondaryInputText,
                        onValueChange = onSecondaryInputChanged,
                        enabled = !state.isSavingEntry,
                        label = stringResource(R.string.vitals_entry_diastolic_label),
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                VitalsValueField(
                    value = state.inputText,
                    onValueChange = onInputChanged,
                    enabled = !state.isSavingEntry,
                    label = stringResource(R.string.vitals_entry_value_label, title, unitLabel),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                        stringResource(R.string.vitals_entry_add_selected, title)
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = vitalsMeasurementEntryErrorText(entryError, state.writeErrorMessage, title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun VitalsValueField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun vitalsMeasurementEntryErrorText(
    error: VitalsMeasurementEntryError,
    message: String?,
    title: String,
): String = when (error) {
    VitalsMeasurementEntryError.INVALID_VALUE -> stringResource(R.string.vitals_entry_invalid_value)
    VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.vitals_entry_permission_needed, title)
    VitalsMeasurementEntryError.WRITE_FAILED -> stringResource(
        R.string.vitals_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private fun canonicalVitalsValue(
    input: String,
    type: VitalsMeasurementType,
    unitSystem: UnitSystem,
): Double? {
    val value = input.toVitalsDoubleOrNull() ?: return null
    return when (type) {
        VitalsMeasurementType.BODY_TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) {
            (value - FahrenheitFreezingPoint) / FahrenheitPerCelsius
        } else {
            value
        }
        else -> value
    }
}

private fun String.toVitalsDoubleOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

@StringRes
fun VitalsMeasurementType.titleRes(): Int = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> R.string.metric_blood_pressure
    VitalsMeasurementType.SPO2 -> R.string.metric_spo2
    VitalsMeasurementType.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
    VitalsMeasurementType.BODY_TEMPERATURE -> R.string.metric_body_temp
}

private fun VitalsMeasurementType.inputUnitLabel(unitSystem: UnitSystem): String = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> "mmHg"
    VitalsMeasurementType.SPO2 -> "%"
    VitalsMeasurementType.RESPIRATORY_RATE -> "br/min"
    VitalsMeasurementType.BODY_TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) "deg F" else "deg C"
}

fun VitalsMeasurementType.icon(): ImageVector = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> Icons.Outlined.Favorite
    VitalsMeasurementType.SPO2 -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.RESPIRATORY_RATE -> Icons.Outlined.Air
    VitalsMeasurementType.BODY_TEMPERATURE -> Icons.Outlined.DeviceThermostat
}

fun VitalsMeasurementType.accentColor(): Color = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> VitalsColor
    VitalsMeasurementType.SPO2 -> OxygenColor
    VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryColor
    VitalsMeasurementType.BODY_TEMPERATURE -> TemperatureColor
}
