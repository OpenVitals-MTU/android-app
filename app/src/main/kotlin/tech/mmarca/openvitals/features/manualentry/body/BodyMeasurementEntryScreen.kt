package tech.mmarca.openvitals.features.manualentry.body

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Straighten
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
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimestampFields
import tech.mmarca.openvitals.features.manualentry.ManualEntryWritePermissionCallout
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor

@Composable
fun BodyMeasurementEntryScreen(
    viewModel: BodyMeasurementEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val units = BodyEntryUnits(
        mass = unitFormatter.unitSystem(UnitQuantity.WEIGHT),
        height = unitFormatter.unitSystem(UnitQuantity.HEIGHT),
    )
    val requestWritePermissions = rememberManualEntryWritePermissionRequester {
        viewModel.refreshPermission()
    }

    LaunchedEffect(units) {
        viewModel.start(units)
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
                units = units,
                onInputChanged = viewModel::updateInput,
                onBodyWaterInputChanged = viewModel::setBodyWaterInput,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onSave = { viewModel.save(units) },
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
    units: BodyEntryUnits,
    onInputChanged: (BodyMeasurementType, String) -> Unit,
    onBodyWaterInputChanged: (BodyWaterInput) -> Unit,
    onEntryTimeChanged: (Instant) -> Unit,
    onSave: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A single value keeps its own name in the copy; the weigh-in speaks of all of them.
    val singleType = state.types.singleOrNull()
    val title = stringResource(singleType?.titleRes() ?: R.string.screen_body_composition_entry)
    val permissionText = if (singleType != null) {
        stringResource(R.string.body_entry_permission_needed, title)
    } else {
        stringResource(R.string.body_composition_entry_permission_needed)
    }
    val fieldsEnabled = !state.isSavingEntry
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
                    imageVector = singleType?.icon() ?: Icons.Outlined.MonitorWeight,
                    contentDescription = null,
                    tint = singleType?.accentColor() ?: WeightColor,
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
                        text = if (singleType != null) {
                            stringResource(R.string.body_entry_subtitle, title)
                        } else {
                            stringResource(R.string.body_composition_entry_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.needsWritePermission) {
                ManualEntryWritePermissionCallout(
                    body = permissionText,
                    onGrant = onRequestWritePermission,
                )
            }

            ManualEntryTimestampFields(
                timestamp = state.entryTime,
                enabled = fieldsEnabled,
                onTimestampChanged = onEntryTimeChanged,
                modifier = Modifier.fillMaxWidth(),
            )

            state.types.forEach { type ->
                if (type == BodyMeasurementType.BODY_WATER_MASS && state.offersBodyWaterPercent) {
                    BodyWaterField(
                        state = state,
                        massUnitLabel = type.inputUnitLabel(units.mass),
                        enabled = fieldsEnabled,
                        onInputChanged = { onInputChanged(type, it) },
                        onBodyWaterInputChanged = onBodyWaterInputChanged,
                    )
                } else {
                    BodyMeasurementField(
                        label = stringResource(
                            R.string.body_entry_value_label,
                            stringResource(type.fieldLabelRes()),
                            type.inputUnitLabel(units.systemFor(type)),
                        ),
                        value = state.inputs[type].orEmpty(),
                        isError = type in state.invalidTypes,
                        enabled = fieldsEnabled,
                        onValueChange = { onInputChanged(type, it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OpenVitalsButton(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when {
                        state.isEditMode -> stringResource(R.string.action_save)
                        singleType != null -> stringResource(R.string.body_entry_add_selected, title)
                        else -> stringResource(R.string.body_composition_entry_save)
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            state.entryError?.let { entryError ->
                Text(
                    text = bodyMeasurementEntryErrorText(
                        error = entryError,
                        writeError = state.writeError,
                        isComposition = state.isComposition,
                        permissionText = permissionText,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Body water, typed as the scale shows it: a share of the weight above, or a mass. */
@Composable
private fun BodyWaterField(
    state: BodyMeasurementEntryUiState,
    massUnitLabel: String,
    enabled: Boolean,
    onInputChanged: (String) -> Unit,
    onBodyWaterInputChanged: (BodyWaterInput) -> Unit,
) {
    val type = BodyMeasurementType.BODY_WATER_MASS
    val options = listOf(BodyWaterInput.PERCENT to "%", BodyWaterInput.MASS to massUnitLabel)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyMeasurementField(
            label = stringResource(
                R.string.body_entry_value_label,
                stringResource(type.fieldLabelRes()),
                options.first { it.first == state.bodyWaterUnit }.second,
            ),
            value = state.inputs[type].orEmpty(),
            isError = type in state.invalidTypes,
            enabled = enabled,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(112.dp)) {
            options.forEachIndexed { index, (input, label) ->
                SegmentedButton(
                    selected = state.bodyWaterUnit == input,
                    onClick = { onBodyWaterInputChanged(input) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {},
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun BodyMeasurementField(
    label: String,
    value: String,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun bodyMeasurementEntryErrorText(
    error: BodyMeasurementEntryError,
    writeError: ScreenError?,
    isComposition: Boolean,
    permissionText: String,
): String = when (error) {
    BodyMeasurementEntryError.NOTHING_TO_SAVE -> stringResource(R.string.body_entry_nothing_to_save)
    BodyMeasurementEntryError.INVALID_VALUE -> if (isComposition) {
        stringResource(R.string.body_composition_entry_invalid_values)
    } else {
        stringResource(R.string.body_entry_invalid_value)
    }
    BodyMeasurementEntryError.BODY_WATER_NEEDS_WEIGHT ->
        stringResource(R.string.body_composition_entry_water_needs_weight)
    BodyMeasurementEntryError.MISSING_WRITE_PERMISSION -> permissionText
    BodyMeasurementEntryError.WRITE_FAILED -> stringResource(
        R.string.body_entry_write_failed,
        writeError.resolve() ?: stringResource(R.string.unknown_error),
    )
}

@StringRes
fun BodyMeasurementType.titleRes(): Int = when (this) {
    BodyMeasurementType.WEIGHT -> R.string.metric_weight
    BodyMeasurementType.HEIGHT -> R.string.metric_height
    BodyMeasurementType.BODY_FAT -> R.string.metric_body_fat
    BodyMeasurementType.LEAN_MASS -> R.string.metric_lean_mass
    BodyMeasurementType.BODY_WATER_MASS -> R.string.metric_body_water_mass
    BodyMeasurementType.BONE_MASS -> R.string.metric_bone_mass
    BodyMeasurementType.BMR -> R.string.metric_bmr
}

/** On the form body water may be a share, so its field drops "mass". */
@StringRes
private fun BodyMeasurementType.fieldLabelRes(): Int =
    if (this == BodyMeasurementType.BODY_WATER_MASS) R.string.metric_body_water else titleRes()

fun BodyMeasurementType.icon(): ImageVector = when (this) {
    BodyMeasurementType.HEIGHT -> Icons.Outlined.Straighten
    BodyMeasurementType.BMR -> Icons.Outlined.LocalFireDepartment
    else -> Icons.Outlined.MonitorWeight
}

fun BodyMeasurementType.accentColor(): Color = when (this) {
    BodyMeasurementType.BODY_FAT -> BodyFatColor
    BodyMeasurementType.BMR -> CaloriesColor
    else -> WeightColor
}
