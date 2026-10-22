package tech.mmarca.openvitals.features.manualentry.vitals

import tech.mmarca.openvitals.core.presentation.TemperatureUnits
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BpMealContext
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.GlucoseContext
import tech.mmarca.openvitals.domain.model.GlucoseMgPerDlPerMmolPerL
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues
import tech.mmarca.openvitals.domain.model.Vo2MaxRecordValues
import tech.mmarca.openvitals.features.vitals.glucoseColor
import tech.mmarca.openvitals.features.vitals.vo2Color
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.VitalsColor

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
    val requestWritePermissions = rememberManualEntryWritePermissionRequester {
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
                onPulseInputChanged = viewModel::updatePulseInput,
                onSelectBpMealContext = viewModel::selectBpMealContext,
                onSelectBpBodyPosition = viewModel::selectBpBodyPosition,
                onSelectBpMeasurementLocation = viewModel::selectBpMeasurementLocation,
                onSelectGlucoseUnit = viewModel::selectGlucoseUnit,
                onGlucoseChanged = viewModel::updateGlucose,
                onSelectVo2MaxMeasurementMethod = viewModel::selectVo2MaxMeasurementMethod,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onAddEntry = {
                    viewModel.addEntry(
                        value = canonicalVitalsValue(
                            input = state.inputText,
                            type = state.type,
                            // Glucose is read in the unit picked on the form, the others in the setting.
                            unitSystem = if (state.type == VitalsMeasurementType.BLOOD_GLUCOSE) {
                                state.glucoseUnit
                            } else {
                                unitFormatter.unitSystemFor(state.type)
                            },
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
                onRequestPulseWritePermission = {
                    requestWritePermissions.launch(state.pulseWritePermissions)
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
    onPulseInputChanged: (String) -> Unit,
    onSelectBpMealContext: (BpMealContext?) -> Unit,
    onSelectBpBodyPosition: (Int?) -> Unit,
    onSelectBpMeasurementLocation: (Int?) -> Unit,
    onSelectGlucoseUnit: (UnitSystem) -> Unit,
    onGlucoseChanged: (GlucoseContext) -> Unit,
    onSelectVo2MaxMeasurementMethod: (Int?) -> Unit,
    onEntryTimeChanged: (java.time.Instant) -> Unit,
    onAddEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    onRequestPulseWritePermission: () -> Unit,
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
                        text = stringResource(R.string.vitals_entry_subtitle, title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!state.canWrite && !state.isCheckingPermission) {
                ManualEntryWritePermissionCallout(
                    body = stringResource(R.string.vitals_entry_permission_needed, title),
                    onGrant = onRequestWritePermission,
                )
            }

            when (state.type) {
                VitalsMeasurementType.BLOOD_PRESSURE -> BloodPressureFields(
                    state = state,
                    onSystolicChanged = onInputChanged,
                    onDiastolicChanged = onSecondaryInputChanged,
                    onPulseChanged = onPulseInputChanged,
                    onSelectMealContext = onSelectBpMealContext,
                    onSelectBodyPosition = onSelectBpBodyPosition,
                    onSelectMeasurementLocation = onSelectBpMeasurementLocation,
                    onRequestPulseWritePermission = onRequestPulseWritePermission,
                )
                VitalsMeasurementType.BLOOD_GLUCOSE -> BloodGlucoseFields(
                    state = state,
                    title = title,
                    onLevelChanged = onInputChanged,
                    onSelectUnit = onSelectGlucoseUnit,
                    onGlucoseChanged = onGlucoseChanged,
                )
                else -> {
                    VitalsValueField(
                        value = state.inputText,
                        onValueChange = onInputChanged,
                        enabled = !state.isSavingEntry,
                        label = stringResource(R.string.vitals_entry_value_label, title, unitLabel),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.type == VitalsMeasurementType.VO2_MAX) {
                        OptionDropdown(
                            label = stringResource(R.string.vitals_entry_vo2_method_label),
                            options = Vo2MaxMeasurementMethods,
                            selected = state.vo2MaxMeasurementMethod,
                            optionText = { stringResource(vo2MaxMeasurementMethodLabelRes(it)) },
                            enabled = !state.isSavingEntry,
                            onSelect = onSelectVo2MaxMeasurementMethod,
                        )
                    }
                }
            }

            // A measurement is not always logged when taken, so a new entry offers the time too.
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

/** Systolic and diastolic, the pulse a home monitor shows, and how the reading was taken. */
@Composable
private fun BloodPressureFields(
    state: VitalsMeasurementEntryUiState,
    onSystolicChanged: (String) -> Unit,
    onDiastolicChanged: (String) -> Unit,
    onPulseChanged: (String) -> Unit,
    onSelectMealContext: (BpMealContext?) -> Unit,
    onSelectBodyPosition: (Int?) -> Unit,
    onSelectMeasurementLocation: (Int?) -> Unit,
    onRequestPulseWritePermission: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VitalsValueField(
            value = state.inputText,
            onValueChange = onSystolicChanged,
            enabled = !state.isSavingEntry,
            label = stringResource(R.string.vitals_entry_systolic_label),
            modifier = Modifier.weight(1f),
        )
        VitalsValueField(
            value = state.secondaryInputText,
            onValueChange = onDiastolicChanged,
            enabled = !state.isSavingEntry,
            label = stringResource(R.string.vitals_entry_diastolic_label),
            modifier = Modifier.weight(1f),
        )
    }
    // A home monitor shows the pulse with every reading, so the form takes it too.
    VitalsValueField(
        value = state.pulseInputText,
        onValueChange = onPulseChanged,
        enabled = !state.isSavingEntry,
        label = stringResource(R.string.vitals_entry_pulse_label),
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth(),
    )
    // Asked for only once a pulse is typed: a reading alone never needs heart rate access.
    if (state.canWrite && !state.canWritePulse && state.pulseInputText.isNotBlank()) {
        ManualEntryWritePermissionCallout(
            body = stringResource(R.string.vitals_entry_pulse_permission_needed),
            onGrant = onRequestPulseWritePermission,
        )
    }
    OptionDropdown(
        label = stringResource(R.string.vitals_entry_bp_context_label),
        options = BpMealContext.entries,
        selected = state.bpMealContext,
        optionText = { stringResource(it.labelRes()) },
        enabled = !state.isSavingEntry,
        onSelect = onSelectMealContext,
    )
    OptionDropdown(
        label = stringResource(R.string.vitals_entry_bp_position_label),
        options = BpBodyPositions,
        selected = state.bpBodyPosition,
        optionText = { stringResource(bpBodyPositionLabelRes(it)) },
        enabled = !state.isSavingEntry,
        onSelect = onSelectBodyPosition,
    )
    OptionDropdown(
        label = stringResource(R.string.vitals_entry_bp_location_label),
        options = BpMeasurementLocations,
        selected = state.bpMeasurementLocation,
        optionText = { stringResource(bpMeasurementLocationLabelRes(it)) },
        enabled = !state.isSavingEntry,
        onSelect = onSelectMeasurementLocation,
    )
    BpMeasurementGuide()
}

/** The level in the unit the meter shows, and when and from what it was taken. */
@Composable
private fun BloodGlucoseFields(
    state: VitalsMeasurementEntryUiState,
    title: String,
    onLevelChanged: (String) -> Unit,
    onSelectUnit: (UnitSystem) -> Unit,
    onGlucoseChanged: (GlucoseContext) -> Unit,
) {
    val enabled = !state.isSavingEntry
    val units = listOf(UnitSystem.METRIC, UnitSystem.IMPERIAL)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VitalsValueField(
            value = state.inputText,
            onValueChange = onLevelChanged,
            enabled = enabled,
            label = stringResource(
                R.string.vitals_entry_value_label,
                title,
                VitalsMeasurementType.BLOOD_GLUCOSE.inputUnitLabel(state.glucoseUnit),
            ),
            modifier = Modifier.weight(1f),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(150.dp)) {
            units.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = state.glucoseUnit == unit,
                    onClick = { onSelectUnit(unit) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = units.size),
                    icon = {},
                ) {
                    Text(VitalsMeasurementType.BLOOD_GLUCOSE.inputUnitLabel(unit), maxLines = 1)
                }
            }
        }
    }
    OptionDropdown(
        label = stringResource(R.string.vitals_entry_bp_context_label),
        options = GlucoseRelationsToMeal,
        selected = state.glucose.relationToMeal,
        optionText = { stringResource(glucoseRelationToMealLabelRes(it)) },
        enabled = enabled,
        onSelect = { onGlucoseChanged(state.glucose.copy(relationToMeal = it)) },
    )
    if (state.glucose.relationToMeal == GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL ||
        state.glucose.relationToMeal == GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL
    ) {
        OptionDropdown(
            label = stringResource(R.string.vitals_entry_glucose_meal_label),
            options = GlucoseMealTypes,
            selected = state.glucose.mealType,
            optionText = { stringResource(glucoseMealTypeLabelRes(it)) },
            enabled = enabled,
            onSelect = { onGlucoseChanged(state.glucose.copy(mealType = it)) },
        )
    }
    OptionDropdown(
        label = stringResource(R.string.vitals_entry_glucose_sample_label),
        options = GlucoseSpecimenSources,
        selected = state.glucose.specimenSource,
        optionText = { stringResource(glucoseSpecimenSourceLabelRes(it)) },
        enabled = enabled,
        onSelect = { onGlucoseChanged(state.glucose.copy(specimenSource = it)) },
    )
}

@Composable
private fun VitalsValueField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
    VitalsMeasurementEntryError.INVALID_PULSE -> stringResource(R.string.vitals_entry_invalid_pulse)
    VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.vitals_entry_permission_needed, title)
    VitalsMeasurementEntryError.MISSING_PULSE_WRITE_PERMISSION ->
        stringResource(R.string.vitals_entry_pulse_permission_needed)
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
            TemperatureUnits.fahrenheitToCelsius(value)
        } else {
            value
        }
        VitalsMeasurementType.BLOOD_GLUCOSE -> if (unitSystem == UnitSystem.IMPERIAL) {
            value / GlucoseMgPerDlPerMmolPerL
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
    VitalsMeasurementType.BLOOD_GLUCOSE -> R.string.metric_blood_glucose
    VitalsMeasurementType.VO2_MAX -> R.string.metric_vo2_max
}

internal fun VitalsMeasurementType.inputUnitLabel(unitSystem: UnitSystem): String = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> "mmHg"
    VitalsMeasurementType.SPO2 -> "%"
    VitalsMeasurementType.RESPIRATORY_RATE -> "br/min"
    VitalsMeasurementType.BODY_TEMPERATURE -> TemperatureUnits.label(unitSystem)
    VitalsMeasurementType.HRV -> "ms"
    VitalsMeasurementType.BLOOD_GLUCOSE -> if (unitSystem == UnitSystem.IMPERIAL) "mg/dL" else "mmol/L"
    VitalsMeasurementType.VO2_MAX -> "mL/kg/min"
}

fun VitalsMeasurementType.icon(): ImageVector = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.SPO2 -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.RESPIRATORY_RATE -> Icons.Outlined.Air
    VitalsMeasurementType.BODY_TEMPERATURE -> Icons.Outlined.DeviceThermostat
    VitalsMeasurementType.HRV -> Icons.Outlined.FavoriteBorder
    VitalsMeasurementType.BLOOD_GLUCOSE -> Icons.Outlined.Bloodtype
    VitalsMeasurementType.VO2_MAX -> Icons.Outlined.Speed
}

fun VitalsMeasurementType.accentColor(): Color = when (this) {
    VitalsMeasurementType.BLOOD_PRESSURE -> VitalsColor
    VitalsMeasurementType.SPO2 -> OxygenColor
    VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryColor
    VitalsMeasurementType.BODY_TEMPERATURE -> TemperatureColor
    VitalsMeasurementType.HRV -> VitalsColor
    VitalsMeasurementType.BLOOD_GLUCOSE -> glucoseColor
    VitalsMeasurementType.VO2_MAX -> vo2Color
}

private fun UnitFormatter.unitSystemFor(type: VitalsMeasurementType): UnitSystem = when (type) {
    VitalsMeasurementType.BODY_TEMPERATURE -> unitSystem(UnitQuantity.TEMPERATURE)
    VitalsMeasurementType.BLOOD_GLUCOSE -> unitSystem(UnitQuantity.BLOOD_GLUCOSE)
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

/** The standard home-measurement protocol (AHA/ESH), collapsed by default. */
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

private val GlucoseRelationsToMeal = listOf(
    GlucoseRecordValues.RELATION_TO_MEAL_FASTING,
    GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL,
    GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL,
    GlucoseRecordValues.RELATION_TO_MEAL_GENERAL,
)

private val GlucoseMealTypes = listOf(
    GlucoseRecordValues.MEAL_TYPE_BREAKFAST,
    GlucoseRecordValues.MEAL_TYPE_LUNCH,
    GlucoseRecordValues.MEAL_TYPE_DINNER,
    GlucoseRecordValues.MEAL_TYPE_SNACK,
)

/** Tears are left out: Health Connect has the source, no meter a person types from does. */
private val GlucoseSpecimenSources = listOf(
    GlucoseRecordValues.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
    GlucoseRecordValues.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
    GlucoseRecordValues.SPECIMEN_SOURCE_PLASMA,
    GlucoseRecordValues.SPECIMEN_SOURCE_SERUM,
    GlucoseRecordValues.SPECIMEN_SOURCE_WHOLE_BLOOD,
)

/** "Not specified" is saved as Health Connect's other method, so other has no entry of its own. */
private val Vo2MaxMeasurementMethods = listOf(
    Vo2MaxRecordValues.MEASUREMENT_METHOD_METABOLIC_CART,
    Vo2MaxRecordValues.MEASUREMENT_METHOD_COOPER_TEST,
    Vo2MaxRecordValues.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
    Vo2MaxRecordValues.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
    Vo2MaxRecordValues.MEASUREMENT_METHOD_HEART_RATE_RATIO,
)

private fun glucoseRelationToMealLabelRes(relation: Int): Int = when (relation) {
    GlucoseRecordValues.RELATION_TO_MEAL_FASTING -> R.string.report_pdf_glucose_fasting
    GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL -> R.string.report_pdf_glucose_before_meal
    GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL -> R.string.report_pdf_glucose_after_meal
    else -> R.string.report_pdf_glucose_general
}

private fun glucoseMealTypeLabelRes(mealType: Int): Int = when (mealType) {
    GlucoseRecordValues.MEAL_TYPE_BREAKFAST -> R.string.meal_breakfast
    GlucoseRecordValues.MEAL_TYPE_LUNCH -> R.string.meal_lunch
    GlucoseRecordValues.MEAL_TYPE_DINNER -> R.string.meal_dinner
    else -> R.string.meal_snack
}

private fun glucoseSpecimenSourceLabelRes(source: Int): Int = when (source) {
    GlucoseRecordValues.SPECIMEN_SOURCE_CAPILLARY_BLOOD -> R.string.glucose_sample_capillary
    GlucoseRecordValues.SPECIMEN_SOURCE_INTERSTITIAL_FLUID -> R.string.glucose_sample_interstitial
    GlucoseRecordValues.SPECIMEN_SOURCE_PLASMA -> R.string.glucose_sample_plasma
    GlucoseRecordValues.SPECIMEN_SOURCE_SERUM -> R.string.glucose_sample_serum
    else -> R.string.glucose_sample_whole_blood
}

private fun vo2MaxMeasurementMethodLabelRes(method: Int): Int = when (method) {
    Vo2MaxRecordValues.MEASUREMENT_METHOD_METABOLIC_CART -> R.string.vo2_method_metabolic_cart
    Vo2MaxRecordValues.MEASUREMENT_METHOD_COOPER_TEST -> R.string.vo2_method_cooper
    Vo2MaxRecordValues.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST -> R.string.vo2_method_multistage
    Vo2MaxRecordValues.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST -> R.string.vo2_method_rockport
    else -> R.string.vo2_method_heart_rate_ratio
}
