package tech.mmarca.openvitals.features.manualentry.cycle

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.features.cycle.measurementLocationLabelRes
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

/** One single-select section as a dropdown with "Not specified" first. */
@Composable
internal fun CycleChipSection(
    label: String,
    options: List<Pair<Int, String>>,
    selection: Int?,
    enabled: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionDropdown(
        label = label,
        options = options,
        selected = options.firstOrNull { it.first == selection },
        optionText = { it.second },
        enabled = enabled,
        onSelect = { onSelect(it?.first) },
        modifier = modifier,
    )
}

@Composable
internal fun CycleToggleSection(
    label: String,
    chipLabel: String,
    logged: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilterChip(
            selected = logged,
            enabled = enabled,
            onClick = onToggle,
            label = { Text(chipLabel) },
        )
    }
}

@Composable
internal fun CycleBbtSection(
    label: String,
    inputText: String,
    location: Int?,
    enabled: Boolean,
    onInputChanged: (String) -> Unit,
    onLocationSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            enabled = enabled,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CycleChipSection(
            label = stringResource(R.string.cycle_entry_bbt_location),
            options = bbtLocationOptions(),
            selection = location,
            enabled = enabled,
            onSelect = onLocationSelected,
        )
    }
}

@Composable
internal fun flowOptions(): List<Pair<Int, String>> = listOf(
    CycleRecordValues.FLOW_LIGHT to stringResource(R.string.cycle_flow_light),
    CycleRecordValues.FLOW_MEDIUM to stringResource(R.string.cycle_flow_medium),
    CycleRecordValues.FLOW_HEAVY to stringResource(R.string.cycle_flow_heavy),
)

@Composable
internal fun protectionOptions(): List<Pair<Int, String>> = listOf(
    CycleRecordValues.PROTECTION_PROTECTED to stringResource(R.string.cycle_protection_protected),
    CycleRecordValues.PROTECTION_UNPROTECTED to stringResource(R.string.cycle_protection_unprotected),
    CycleRecordValues.PROTECTION_UNKNOWN to stringResource(R.string.cycle_protection_unknown),
)

@Composable
internal fun ovulationOptions(): List<Pair<Int, String>> = listOf(
    CycleRecordValues.OVULATION_POSITIVE to stringResource(R.string.cycle_ovulation_positive),
    CycleRecordValues.OVULATION_HIGH to stringResource(R.string.cycle_ovulation_high),
    CycleRecordValues.OVULATION_NEGATIVE to stringResource(R.string.cycle_ovulation_negative),
    CycleRecordValues.OVULATION_INCONCLUSIVE to stringResource(R.string.cycle_ovulation_inconclusive),
)

@Composable
internal fun mucusAppearanceOptions(): List<Pair<Int, String>> = listOf(
    CycleRecordValues.MUCUS_APPEARANCE_DRY to stringResource(R.string.cycle_mucus_dry),
    CycleRecordValues.MUCUS_APPEARANCE_STICKY to stringResource(R.string.cycle_mucus_sticky),
    CycleRecordValues.MUCUS_APPEARANCE_CREAMY to stringResource(R.string.cycle_mucus_creamy),
    CycleRecordValues.MUCUS_APPEARANCE_WATERY to stringResource(R.string.cycle_mucus_watery),
    CycleRecordValues.MUCUS_APPEARANCE_EGG_WHITE to stringResource(R.string.cycle_mucus_egg_white),
    CycleRecordValues.MUCUS_APPEARANCE_UNUSUAL to stringResource(R.string.cycle_mucus_unusual),
)

@Composable
internal fun mucusSensationOptions(): List<Pair<Int, String>> = listOf(
    CycleRecordValues.MUCUS_SENSATION_LIGHT to stringResource(R.string.cycle_mucus_light),
    CycleRecordValues.MUCUS_SENSATION_MEDIUM to stringResource(R.string.cycle_mucus_medium),
    CycleRecordValues.MUCUS_SENSATION_HEAVY to stringResource(R.string.cycle_mucus_heavy),
)

@Composable
internal fun bbtLocationOptions(): List<Pair<Int, String>> = listOf(4, 5, 10, 1, 8).map { location ->
    location to stringResource(measurementLocationLabelRes(location))
}
