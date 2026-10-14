package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalTime
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.TemperatureUnits
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CervixFirmness
import tech.mmarca.openvitals.domain.model.CervixOpening
import tech.mmarca.openvitals.domain.model.CervixPosition
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.ManualEntryPickerButton
import tech.mmarca.openvitals.features.manualentry.ManualEntryTimePickerDialog
import tech.mmarca.openvitals.features.manualentry.localizedTimeText
import tech.mmarca.openvitals.ui.theme.Spacing

/** Basal body temperature: the value, where and when it was taken, and why it may not count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleEntryBbtSection(
    form: CycleDayForm,
    unitSystem: UnitSystem,
    enabled: Boolean,
    onInputChanged: (String) -> Unit,
    onLocationSelected: (Int?) -> Unit,
    onTimeSelected: (LocalTime?) -> Unit,
    onToggleDisturbance: (BbtDisturbance) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val unitLabel = TemperatureUnits.label(unitSystem)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = stringResource(R.string.cycle_observation_basal_body_temperature), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = form.bbtInputText,
            onValueChange = onInputChanged,
            enabled = enabled,
            label = { Text(stringResource(R.string.cycle_entry_section_bbt, unitLabel)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CycleChipSection(
            label = stringResource(R.string.cycle_entry_bbt_location),
            options = bbtLocationOptions(),
            selection = form.bbtLocation,
            enabled = enabled && form.bbtInputText.isNotBlank(),
            onSelect = onLocationSelected,
        )
        ManualEntryPickerButton(
            label = stringResource(R.string.cycle_entry_bbt_time),
            value = form.bbtTime?.localizedTimeText() ?: stringResource(R.string.option_not_specified),
            icon = Icons.Outlined.Schedule,
            enabled = enabled && form.bbtInputText.isNotBlank(),
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.cycle_entry_bbt_disturbances),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BbtDisturbance.entries.forEach { disturbance ->
                val selected = disturbance in form.bbtDisturbances
                FilterChip(
                    selected = selected,
                    enabled = enabled && form.bbtInputText.isNotBlank(),
                    onClick = { onToggleDisturbance(disturbance) },
                    label = { Text(stringResource(disturbanceLabelRes(disturbance))) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
    }
    if (showTimePicker) {
        ManualEntryTimePickerDialog(
            selectedTime = form.bbtTime ?: DefaultBbtTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                onTimeSelected(time)
            },
        )
    }
}

private val DefaultBbtTime: LocalTime = LocalTime.of(7, 0)

internal fun disturbanceLabelRes(disturbance: BbtDisturbance): Int = when (disturbance) {
    BbtDisturbance.FEVER -> R.string.cycle_disturbance_fever
    BbtDisturbance.ALCOHOL -> R.string.cycle_disturbance_alcohol
    BbtDisturbance.POOR_SLEEP -> R.string.cycle_disturbance_poor_sleep
    BbtDisturbance.TIME_SHIFT -> R.string.cycle_disturbance_time_shift
    BbtDisturbance.LATE_MEASUREMENT -> R.string.cycle_disturbance_late_measurement
    BbtDisturbance.STRESS -> R.string.cycle_disturbance_stress
    BbtDisturbance.MEDICATION -> R.string.cycle_disturbance_medication
}

/** Cervical fluid: the sensation (journal) and the appearance and amount (Health Connect). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleEntryCervicalFluidSection(
    form: CycleDayForm,
    enabled: Boolean,
    onSensation: (CervicalSensation?) -> Unit,
    onAppearance: (Int?) -> Unit,
    onAmount: (Int?) -> Unit,
    onCervixOpening: (CervixOpening?) -> Unit,
    onCervixFirmness: (CervixFirmness?) -> Unit,
    onCervixPosition: (CervixPosition?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = stringResource(R.string.cycle_observation_cervical_mucus), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.cycle_entry_cervical_sensation),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CervicalSensation.entries.forEach { sensation ->
                val selected = form.cervicalSensation == sensation
                FilterChip(
                    selected = selected,
                    enabled = enabled,
                    onClick = { onSensation(if (selected) null else sensation) },
                    label = { Text(stringResource(sensationLabelRes(sensation))) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
        CycleChipSection(
            label = stringResource(R.string.cycle_entry_section_mucus_appearance),
            options = mucusAppearanceOptions(),
            selection = form.mucusAppearance,
            enabled = enabled,
            onSelect = onAppearance,
        )
        CycleChipSection(
            label = stringResource(R.string.cycle_entry_section_mucus_sensation),
            options = mucusSensationOptions(),
            selection = form.mucusAmount,
            enabled = enabled,
            onSelect = onAmount,
        )
        Text(text = stringResource(R.string.cycle_entry_cervix), style = MaterialTheme.typography.titleSmall)
        EnumChipRow(
            label = stringResource(R.string.cycle_entry_cervix_opening),
            options = CervixOpening.entries,
            selected = form.cervixOpening,
            enabled = enabled,
            labelRes = ::cervixOpeningLabelRes,
            onSelect = onCervixOpening,
        )
        EnumChipRow(
            label = stringResource(R.string.cycle_entry_cervix_firmness),
            options = CervixFirmness.entries,
            selected = form.cervixFirmness,
            enabled = enabled,
            labelRes = ::cervixFirmnessLabelRes,
            onSelect = onCervixFirmness,
        )
        EnumChipRow(
            label = stringResource(R.string.cycle_entry_cervix_position),
            options = CervixPosition.entries,
            selected = form.cervixPosition,
            enabled = enabled,
            labelRes = ::cervixPositionLabelRes,
            onSelect = onCervixPosition,
        )
    }
}

/** One row of single-choice chips over an enum; tapping the chosen one clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> EnumChipRow(
    label: String,
    options: List<T>,
    selected: T?,
    enabled: Boolean,
    labelRes: (T) -> Int,
    onSelect: (T?) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            FilterChip(
                selected = isSelected,
                enabled = enabled,
                onClick = { onSelect(if (isSelected) null else option) },
                label = { Text(stringResource(labelRes(option))) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                } else {
                    null
                },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

internal fun cervixOpeningLabelRes(opening: CervixOpening): Int = when (opening) {
    CervixOpening.CLOSED -> R.string.cycle_cervix_closed
    CervixOpening.MEDIUM -> R.string.cycle_cervix_medium
    CervixOpening.OPEN -> R.string.cycle_cervix_open
}

internal fun cervixFirmnessLabelRes(firmness: CervixFirmness): Int = when (firmness) {
    CervixFirmness.FIRM -> R.string.cycle_cervix_firm
    CervixFirmness.SOFT -> R.string.cycle_cervix_soft
}

internal fun cervixPositionLabelRes(position: CervixPosition): Int = when (position) {
    CervixPosition.LOW -> R.string.cycle_cervix_low
    CervixPosition.MEDIUM -> R.string.cycle_cervix_medium
    CervixPosition.HIGH -> R.string.cycle_cervix_high
}

internal fun sensationLabelRes(sensation: CervicalSensation): Int = when (sensation) {
    CervicalSensation.DRY -> R.string.cycle_sensation_dry
    CervicalSensation.DAMP -> R.string.cycle_sensation_damp
    CervicalSensation.WET -> R.string.cycle_sensation_wet
    CervicalSensation.SLIPPERY -> R.string.cycle_sensation_slippery
}
