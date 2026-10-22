package tech.mmarca.openvitals.features.medical

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.LabInterpretations
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualReference
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.medical.VisitSettings
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

/** Where the visit took place. FHIR needs one, so there is no "Not specified". */
@Composable
internal fun VisitSettingField(selected: String, onSelect: (String) -> Unit) {
    OptionDropdown(
        label = stringResource(R.string.medical_entry_setting),
        options = VisitSettings,
        selected = selected,
        optionText = { stringResource(visitSettingLabelRes(it)) },
        enabled = true,
        onSelect = { setting -> setting?.let(onSelect) },
        allowNone = false,
    )
}

/** The result as the report prints it, with its unit, flag and range. A number can carry a bound, such as `<0.5`. */
@Composable
internal fun LabResultFields(draft: ManualRecordDraft, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = draft.result,
            onValueChange = { result -> onChange { it.copy(result = result) } },
            label = { Text(stringResource(R.string.medical_entry_result)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = draft.unit,
            onValueChange = { unit -> onChange { it.copy(unit = unit) } },
            label = { Text(stringResource(R.string.medical_entry_unit)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.weight(1f),
        )
    }
    OptionDropdown(
        label = stringResource(SummaryField.INTERPRETATION.labelRes),
        options = LabInterpretations,
        selected = draft.interpretation,
        optionText = { code -> fhirCodeLabelRes(SummaryField.INTERPRETATION, code)?.let { stringResource(it) } ?: code },
        enabled = true,
        onSelect = { interpretation -> onChange { it.copy(interpretation = interpretation) } },
    )
    EntryTextField(
        label = stringResource(SummaryField.REFERENCE_RANGE.labelRes),
        value = draft.referenceRange,
        capitalization = KeyboardCapitalization.None,
        onValueChange = { range -> onChange { it.copy(referenceRange = range) } },
    )
}

/**
 * A doctor or a clinic: one of the user's own, picked from the list as they type, or a new name,
 * which the save writes as a record of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferenceField(
    label: String,
    value: ManualReference?,
    options: List<ManualReference>,
    type: String,
    onChange: (ManualReference?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val text = value?.display.orEmpty()
    val matches = remember(options, text) {
        options.filter { text.isBlank() || it.display.contains(text.trim(), ignoreCase = true) }
    }
    val isNew = value != null && value.id == null && text.isNotBlank() &&
        options.none { it.display.equals(text.trim(), ignoreCase = true) }
    val showMenu = expanded && matches.isNotEmpty()
    ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = text,
            onValueChange = { typed ->
                onChange(typed.takeIf { it.isNotEmpty() }?.let { ManualReference(type, it) })
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            supportingText = if (isNew) ({ Text(stringResource(R.string.medical_entry_reference_new)) }) else null,
            trailingIcon = if (options.isNotEmpty()) ({ ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) }) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
            matches.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.display) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@StringRes
private fun visitSettingLabelRes(setting: String): Int = when (setting) {
    "EMER" -> R.string.medical_visit_emergency
    "IMP" -> R.string.medical_visit_inpatient
    "ACUTE" -> R.string.medical_visit_acute
    "NONAC" -> R.string.medical_visit_nonacute
    "OBSENC" -> R.string.medical_visit_observation
    "SS" -> R.string.medical_visit_short_stay
    "PRENC" -> R.string.medical_visit_preadmission
    "HH" -> R.string.medical_visit_home
    "VR" -> R.string.medical_visit_virtual
    "FLD" -> R.string.medical_visit_field
    else -> R.string.medical_visit_outpatient
}
