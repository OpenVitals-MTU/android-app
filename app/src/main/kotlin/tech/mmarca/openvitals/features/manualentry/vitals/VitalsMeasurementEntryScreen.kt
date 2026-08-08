package tech.mmarca.openvitals.features.manualentry.vitals

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BpMealContext
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
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

    LaunchedEffect(type, unitFormatter.unitSystemFor(type)) {
        viewModel.setType(type, unitFormatter.unitSystemFor(type))
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
                onSelectBpMealContext = viewModel::selectBpMealContext,
                onSelectBpBodyPosition = viewModel::selectBpBodyPosition,
                onSelectBpMeasurementLocation = viewModel::selectBpMeasurementLocation,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onAddEntry = {
                    viewModel.addEntry(
                        value = canonicalVitalsValue(
                            input = state.inputText,
                            type = state.type,
                            unitSystem = unitFormatter.unitSystemFor(state.type),
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
    onSelectBpMealContext: (BpMealContext?) -> Unit,
    onSelectBpBodyPosition: (Int?) -> Unit,
    onSelectBpMeasurementLocation: (Int?) -> Unit,
    onEntryTimeChanged: (java.time.Instant) -> Unit,
    onAddEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(state.type.titleRes())
    val unitLabel = state.type.inputUnitLabel(unitFormatter.unitSystemFor(state.type))
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
                    OpenVitalsOutlinedButton(onClick = onRequestWritePermission) {
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
                OptionDropdown(
                    label = stringResource(R.string.vitals_entry_bp_context_label),
                    options = BpMealContext.entries,
                    selected = state.bpMealContext,
                    optionText = { stringResource(it.labelRes()) },
                    enabled = !state.isSavingEntry,
                    onSelect = onSelectBpMealContext,
                )
                OptionDropdown(
                    label = stringResource(R.string.vitals_entry_bp_position_label),
                    options = BpBodyPositions,
                    selected = state.bpBodyPosition,
                    optionText = { stringResource(bpBodyPositionLabelRes(it)) },
                    enabled = !state.isSavingEntry,
                    onSelect = onSelectBpBodyPosition,
                )
                OptionDropdown(
                    label = stringResource(R.string.vitals_entry_bp_location_label),
                    options = BpMeasurementLocations,
                    selected = state.bpMeasurementLocation,
                    optionText = { stringResource(bpMeasurementLocationLabelRes(it)) },
                    enabled = !state.isSavingEntry,
                    onSelect = onSelectBpMeasurementLocation,
                )
                BpMeasurementGuide()
            } else {
                VitalsValueField(
                    value = state.inputText,
                    onValueChange = onInputChanged,
                    enabled = !state.isSavingEntry,
                    label = stringResource(R.string.vitals_entry_value_label, title, unitLabel),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // A measurement is not always logged the moment it was taken — the
            // date and clock are offered on a NEW entry too, defaulting to now.
            ManualEntryTimestampFields(
                timestamp = state.editTime,
                enabled = !state.isSavingEntry,
                onTimestampChanged = onEntryTimeChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            OpenVitalsButton(
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
                    text = vitalsMeasurementEntryErrorText(entryError, state.writeError, title),
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
    writeError: ScreenError?,
    title: String,
): String = when (error) {
    VitalsMeasurementEntryError.INVALID_VALUE -> stringResource(R.string.vitals_entry_invalid_value)
    VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.vitals_entry_permission_needed, title)
    VitalsMeasurementEntryError.WRITE_FAILED -> stringResource(
        R.string.vitals_entry_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}

internal fun canonicalVitalsValue(
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
    VitalsMeasurementType.HRV -> R.string.metric_hrv
}

private fun VitalsMeasurementType.inputUnitLabel(unitSystem: UnitSystem): String = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> "mmHg"
    VitalsMeasurementType.SPO2 -> "%"
    VitalsMeasurementType.RESPIRATORY_RATE -> "br/min"
    VitalsMeasurementType.BODY_TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) "deg F" else "deg C"
    VitalsMeasurementType.HRV -> "ms"
}

fun VitalsMeasurementType.icon(): ImageVector = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.SPO2 -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.RESPIRATORY_RATE -> Icons.Outlined.Air
    VitalsMeasurementType.BODY_TEMPERATURE -> Icons.Outlined.DeviceThermostat
    VitalsMeasurementType.HRV -> Icons.Outlined.FavoriteBorder
}

fun VitalsMeasurementType.accentColor(): Color = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> VitalsColor
    VitalsMeasurementType.SPO2 -> OxygenColor
    VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryColor
    VitalsMeasurementType.BODY_TEMPERATURE -> TemperatureColor
    VitalsMeasurementType.HRV -> VitalsColor
}

private fun UnitFormatter.unitSystemFor(type: VitalsMeasurementType): UnitSystem = when (type) {
    VitalsMeasurementType.BODY_TEMPERATURE -> unitSystem(UnitQuantity.TEMPERATURE)
    else -> unitSystem()
}

internal fun BpMealContext.labelRes(): Int = when (this) {
    BpMealContext.BEFORE_BREAKFAST -> R.string.bp_context_before_breakfast
    BpMealContext.AFTER_BREAKFAST -> R.string.bp_context_after_breakfast
    BpMealContext.BEFORE_LUNCH -> R.string.bp_context_before_lunch
    BpMealContext.AFTER_LUNCH -> R.string.bp_context_after_lunch
    BpMealContext.BEFORE_DINNER -> R.string.bp_context_before_dinner
    BpMealContext.AFTER_DINNER -> R.string.bp_context_after_dinner
}

/**
 * The standard home-measurement protocol (AHA/ESH), collapsed by default so
 * the form stays a form — expanded it reads as the checklist a doctor hands
 * out with the monitor.
 */
@Composable
private fun BpMeasurementGuide() {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.bp_guide_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                listOf(
                    R.string.bp_guide_posture,
                    R.string.bp_guide_circumstances,
                    R.string.bp_guide_equipment,
                    R.string.bp_guide_technique,
                    R.string.bp_guide_target,
                ).forEach { lineRes ->
                    Row {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(lineRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

private val BpBodyPositions = listOf(
    BpRecordValues.BODY_POSITION_SITTING_DOWN,
    BpRecordValues.BODY_POSITION_STANDING_UP,
    BpRecordValues.BODY_POSITION_LYING_DOWN,
    BpRecordValues.BODY_POSITION_RECLINING,
)

private val BpMeasurementLocations = listOf(
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST,
)

internal fun bpBodyPositionLabelRes(position: Int): Int = when (position) {
    BpRecordValues.BODY_POSITION_STANDING_UP -> R.string.bp_position_standing
    BpRecordValues.BODY_POSITION_SITTING_DOWN -> R.string.bp_position_sitting
    BpRecordValues.BODY_POSITION_LYING_DOWN -> R.string.bp_position_lying
    else -> R.string.bp_position_reclining
}

internal fun bpMeasurementLocationLabelRes(location: Int): Int = when (location) {
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST -> R.string.bp_location_left_wrist
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST -> R.string.bp_location_right_wrist
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM -> R.string.bp_location_left_arm
    else -> R.string.bp_location_right_arm
}
