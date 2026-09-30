package tech.mmarca.openvitals.features.medical

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.ManualCode
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

private val Criticalities = listOf("low", "high", "unable-to-assess")

/** One kind's fields. Names are free text; a code is optional and nothing is looked up. */
@Composable
internal fun MedicalRecordEntryFields(draft: ManualRecordDraft, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    val kind = draft.kind
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            EntryTextField(
                label = stringResource(nameLabelRes(kind)),
                value = draft.name,
                onValueChange = { name -> onChange { it.copy(name = name) } },
            )
            Text(text = stringResource(R.string.medical_entry_status), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                kind.statuses.forEach { status ->
                    FilterChip(
                        selected = draft.status == status,
                        onClick = { onChange { it.copy(status = status) } },
                        label = { Text(statusText(status)) },
                    )
                }
            }
            EntryDateField(
                label = stringResource(dateLabelRes(kind)),
                date = draft.date,
                // A vaccine always has a date; the others may not know one.
                optional = kind != ManualRecordKind.VACCINE,
                onChange = { date -> onChange { it.copy(date = date) } },
            )
            detailLabelRes(kind)?.let { labelRes ->
                EntryTextField(label = stringResource(labelRes), value = draft.detail, onValueChange = { detail -> onChange { it.copy(detail = detail) } })
            }
            if (kind == ManualRecordKind.ALLERGY) {
                OptionDropdown(
                    label = stringResource(SummaryField.CRITICALITY.labelRes),
                    options = Criticalities,
                    selected = draft.criticality,
                    optionText = { code -> fhirCodeLabelRes(SummaryField.CRITICALITY, code)?.let { stringResource(it) } ?: code },
                    enabled = true,
                    onSelect = { criticality -> onChange { it.copy(criticality = criticality) } },
                )
            }
            CodeFields(draft, onChange)
            EntryTextField(
                label = stringResource(SummaryField.NOTE.labelRes),
                value = draft.note,
                singleLine = false,
                onValueChange = { note -> onChange { it.copy(note = note) } },
            )
        }
    }
}

/** Asked once, on the first manual entry, when no Patient record can be read. */
@Composable
internal fun MedicalOwnerCard(owner: MedicalOwnerFields, onChange: ((MedicalOwnerFields) -> MedicalOwnerFields) -> Unit) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = stringResource(R.string.medical_entry_owner_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.medical_entry_owner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EntryTextField(stringResource(R.string.medical_entry_given_name), owner.givenName, capitalization = KeyboardCapitalization.Words) { name ->
                onChange { it.copy(givenName = name) }
            }
            EntryTextField(stringResource(R.string.medical_entry_family_name), owner.familyName, capitalization = KeyboardCapitalization.Words) { name ->
                onChange { it.copy(familyName = name) }
            }
            EntryDateField(
                label = stringResource(SummaryField.BIRTH_DATE.labelRes),
                date = owner.birthDate,
                optional = true,
                onChange = { date -> onChange { it.copy(birthDate = date) } },
            )
        }
    }
}

@Composable
private fun CodeFields(draft: ManualRecordDraft, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    val code = draft.code ?: ManualCode(system = null, code = "")
    OptionDropdown(
        label = stringResource(R.string.medical_entry_code_system),
        options = draft.kind.codeSystems,
        selected = code.system,
        optionText = { codeSystemLabel(it) },
        enabled = true,
        onSelect = { system -> onChange { it.copy(code = code.copy(system = system)) } },
    )
    EntryTextField(label = stringResource(R.string.medical_entry_code), value = code.code, capitalization = KeyboardCapitalization.None) { value ->
        onChange { it.copy(code = code.copy(code = value)) }
    }
}

@Composable
private fun EntryTextField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A date button. The picker stops at today. An optional date can be cleared. */
@Composable
private fun EntryDateField(label: String, date: LocalDate?, optional: Boolean, onChange: (LocalDate?) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Text(text = label, style = MaterialTheme.typography.labelLarge)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OpenVitalsOutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
            Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: stringResource(R.string.medical_entry_date_not_set))
        }
        if (optional && date != null) {
            IconButton(onClick = { onChange(null) }) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.medical_entry_clear_date))
            }
        }
    }
    if (picking) {
        HealthDatePickerDialog(
            selectedDate = date ?: LocalDate.now(),
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                onChange(it)
            },
        )
    }
}

@StringRes
internal fun entryTitleRes(kind: ManualRecordKind, edit: Boolean): Int = when (kind) {
    ManualRecordKind.VACCINE -> if (edit) R.string.medical_entry_edit_vaccine else R.string.medical_entry_add_vaccine
    ManualRecordKind.ALLERGY -> if (edit) R.string.medical_entry_edit_allergy else R.string.medical_entry_add_allergy
    ManualRecordKind.MEDICATION -> if (edit) R.string.medical_entry_edit_medication else R.string.medical_entry_add_medication
    ManualRecordKind.CONDITION -> if (edit) R.string.medical_entry_edit_condition else R.string.medical_entry_add_condition
}

@StringRes
private fun nameLabelRes(kind: ManualRecordKind): Int = when (kind) {
    ManualRecordKind.VACCINE -> R.string.medical_entry_name_vaccine
    ManualRecordKind.ALLERGY -> R.string.medical_entry_name_allergy
    ManualRecordKind.MEDICATION -> R.string.medical_entry_name_medication
    ManualRecordKind.CONDITION -> R.string.medical_entry_name_condition
}

@StringRes
private fun dateLabelRes(kind: ManualRecordKind): Int = when (kind) {
    ManualRecordKind.VACCINE -> R.string.medical_entry_date_given
    ManualRecordKind.MEDICATION -> R.string.medical_entry_date_started
    ManualRecordKind.ALLERGY, ManualRecordKind.CONDITION -> SummaryField.ONSET.labelRes
}

@StringRes
private fun detailLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VACCINE -> SummaryField.LOT_NUMBER.labelRes
    ManualRecordKind.ALLERGY -> SummaryField.REACTION.labelRes
    ManualRecordKind.MEDICATION -> SummaryField.DOSAGE.labelRes
    ManualRecordKind.CONDITION -> null
}
