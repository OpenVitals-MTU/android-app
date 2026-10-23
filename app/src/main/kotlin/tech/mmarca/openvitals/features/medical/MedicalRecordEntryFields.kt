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
import androidx.compose.runtime.LaunchedEffect
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
import tech.mmarca.openvitals.domain.medical.Genders
import tech.mmarca.openvitals.domain.medical.ManualCode
import tech.mmarca.openvitals.domain.medical.ManualDirectory
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.ManualReference
import tech.mmarca.openvitals.domain.medical.MeasureValue
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

private val Criticalities = listOf("low", "high", "unable-to-assess")

/**
 * One kind's fields. Names are free text; a code is optional and nothing is looked up. A doctor
 * or clinic is picked from [directory] or typed as a new name.
 */
@Composable
internal fun MedicalRecordEntryFields(
    draft: ManualRecordDraft,
    directory: ManualDirectory,
    today: LocalDate,
    onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit,
) {
    val kind = draft.kind
    val measureLabel = draft.measure?.let { stringResource(measureLabelRes(it)) }
    // A measure names its record in the user's language, the first one included.
    LaunchedEffect(draft.measure) {
        if (measureLabel != null && draft.measure?.value != MeasureValue.TEXT && draft.name.isBlank()) {
            onChange { it.copy(name = measureLabel) }
        }
    }
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (kind.isMeasured) {
                MeasureField(kind, draft.measure, onChange)
            }
            if (!kind.isMeasured || draft.measure?.value == MeasureValue.TEXT) {
                EntryTextField(
                    label = stringResource(nameLabelRes(kind)),
                    value = draft.name,
                    capitalization = if (kind.isDirectory) KeyboardCapitalization.Words else KeyboardCapitalization.Sentences,
                    onValueChange = { name -> onChange { it.copy(name = name) } },
                )
            }
            if (kind == ManualRecordKind.PATIENT) {
                PatientFields(draft, onChange)
            }
            if (kind.statuses.isNotEmpty()) {
                StatusChips(kind, draft.status) { status -> onChange { it.copy(status = status) } }
            }
            if (kind == ManualRecordKind.VISIT) {
                VisitSettingField(draft.setting) { setting -> onChange { it.copy(setting = setting) } }
            }
            dateLabelRes(kind)?.let { labelRes ->
                EntryDateField(
                    label = stringResource(labelRes),
                    date = draft.date,
                    // A vaccine, a visit, a procedure or a test happened on a day; the others may not know one.
                    optional = !kind.needsDate,
                    allowFuture = draft.status in kind.futureStatuses,
                    onChange = { date -> onChange { it.copy(date = date) } },
                )
                if (draft.hasFutureDate(today)) {
                    Text(
                        text = stringResource(R.string.medical_entry_future_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (kind == ManualRecordKind.LAB_RESULT) {
                LabResultFields(draft, onChange)
            }
            if (kind.isMeasured) {
                MeasuredValueFields(
                    draft = draft,
                    onPickDate = {
                        EntryDateField(
                            label = measureLabel.orEmpty(),
                            date = draft.valueDate,
                            optional = false,
                            allowFuture = true,
                            onChange = { date -> onChange { it.copy(valueDate = date) } },
                        )
                    },
                    onChange = onChange,
                )
            }
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
            ReferenceFields(draft, directory, onChange)
            if (kind.isDirectory) {
                EntryTextField(
                    label = stringResource(R.string.medical_entry_contact),
                    value = draft.contact,
                    capitalization = KeyboardCapitalization.None,
                    onValueChange = { contact -> onChange { it.copy(contact = contact) } },
                )
            }
            if (kind.codeSystems.isNotEmpty()) {
                CodeFields(draft, onChange)
            }
            if (kind.hasNote) {
                EntryTextField(
                    label = stringResource(SummaryField.NOTE.labelRes),
                    value = draft.note,
                    singleLine = false,
                    onValueChange = { note -> onChange { it.copy(note = note) } },
                )
            }
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
private fun StatusChips(kind: ManualRecordKind, selected: String, onSelect: (String) -> Unit) {
    Text(text = stringResource(R.string.medical_entry_status), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        kind.statuses.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(statusText(status)) },
            )
        }
    }
}

/** The doctor and the clinic, hospital or lab a kind names. */
@Composable
private fun ReferenceFields(
    draft: ManualRecordDraft,
    directory: ManualDirectory,
    onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit,
) {
    doctorLabelRes(draft.kind)?.let { labelRes ->
        ReferenceField(
            label = stringResource(labelRes),
            value = draft.doctor,
            options = directory.practitioners,
            type = ManualReference.Practitioner,
            onChange = { doctor -> onChange { it.copy(doctor = doctor) } },
        )
    }
    organizationLabelRes(draft.kind)?.let { labelRes ->
        ReferenceField(
            label = stringResource(labelRes),
            value = draft.organization,
            options = directory.organizations,
            type = ManualReference.Organization,
            onChange = { organization -> onChange { it.copy(organization = organization) } },
        )
    }
    placeLabelRes(draft.kind)?.let { labelRes ->
        ReferenceField(
            label = stringResource(labelRes),
            value = draft.place,
            options = directory.locations,
            type = ManualReference.Location,
            onChange = { place -> onChange { it.copy(place = place) } },
        )
    }
}

/** The owner's family name and gender, beside the given name above. */
@Composable
private fun PatientFields(draft: ManualRecordDraft, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    EntryTextField(
        label = stringResource(R.string.medical_entry_family_name),
        value = draft.familyName,
        capitalization = KeyboardCapitalization.Words,
        onValueChange = { name -> onChange { it.copy(familyName = name) } },
    )
    OptionDropdown(
        label = stringResource(SummaryField.GENDER.labelRes),
        options = Genders,
        selected = draft.gender,
        optionText = { code -> fhirCodeLabelRes(SummaryField.GENDER, code)?.let { stringResource(it) } ?: code },
        enabled = true,
        onSelect = { gender -> onChange { it.copy(gender = gender) } },
    )
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
internal fun EntryTextField(
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

/** A date button. The picker stops at today unless [allowFuture]. An optional date can be cleared. */
@Composable
private fun EntryDateField(label: String, date: LocalDate?, optional: Boolean, allowFuture: Boolean = false, onChange: (LocalDate?) -> Unit) {
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
            latestDate = if (allowFuture) null else LocalDate.now(),
        )
    }
}

@StringRes
internal fun entryTitleRes(kind: ManualRecordKind, edit: Boolean): Int = when (kind) {
    ManualRecordKind.VACCINE -> if (edit) R.string.medical_entry_edit_vaccine else R.string.medical_entry_add_vaccine
    ManualRecordKind.ALLERGY -> if (edit) R.string.medical_entry_edit_allergy else R.string.medical_entry_add_allergy
    ManualRecordKind.MEDICATION -> if (edit) R.string.medical_entry_edit_medication else R.string.medical_entry_add_medication
    ManualRecordKind.PRESCRIPTION -> if (edit) R.string.medical_entry_edit_prescription else R.string.medical_entry_add_prescription
    ManualRecordKind.CONDITION -> if (edit) R.string.medical_entry_edit_condition else R.string.medical_entry_add_condition
    ManualRecordKind.VISIT -> if (edit) R.string.medical_entry_edit_visit else R.string.medical_entry_add_visit
    ManualRecordKind.PROCEDURE -> if (edit) R.string.medical_entry_edit_procedure else R.string.medical_entry_add_procedure
    ManualRecordKind.LAB_RESULT -> if (edit) R.string.medical_entry_edit_lab_result else R.string.medical_entry_add_lab_result
    ManualRecordKind.VITAL_SIGN -> if (edit) R.string.medical_entry_edit_vital_sign else R.string.medical_entry_add_vital_sign
    ManualRecordKind.SOCIAL_HISTORY -> if (edit) R.string.medical_entry_edit_social_history else R.string.medical_entry_add_social_history
    ManualRecordKind.PREGNANCY -> if (edit) R.string.medical_entry_edit_pregnancy else R.string.medical_entry_add_pregnancy
    ManualRecordKind.PRACTITIONER -> if (edit) R.string.medical_entry_edit_practitioner else R.string.medical_entry_add_practitioner
    ManualRecordKind.PRACTITIONER_ROLE ->
        if (edit) R.string.medical_entry_edit_practitioner_role else R.string.medical_entry_add_practitioner_role
    ManualRecordKind.ORGANIZATION -> if (edit) R.string.medical_entry_edit_organization else R.string.medical_entry_add_organization
    ManualRecordKind.LOCATION -> if (edit) R.string.medical_entry_edit_location else R.string.medical_entry_add_location
    ManualRecordKind.PATIENT -> R.string.medical_entry_edit_patient
}

@StringRes
private fun nameLabelRes(kind: ManualRecordKind): Int = when (kind) {
    ManualRecordKind.VACCINE -> R.string.medical_entry_name_vaccine
    ManualRecordKind.ALLERGY -> R.string.medical_entry_name_allergy
    ManualRecordKind.MEDICATION, ManualRecordKind.PRESCRIPTION -> R.string.medical_entry_name_medication
    ManualRecordKind.CONDITION -> R.string.medical_entry_name_condition
    ManualRecordKind.VISIT -> R.string.medical_entry_name_visit
    ManualRecordKind.PROCEDURE -> R.string.medical_entry_name_procedure
    ManualRecordKind.LAB_RESULT -> R.string.medical_entry_name_lab_result
    ManualRecordKind.VITAL_SIGN, ManualRecordKind.SOCIAL_HISTORY, ManualRecordKind.PREGNANCY -> R.string.medical_entry_name_topic
    ManualRecordKind.PRACTITIONER -> R.string.medical_entry_name_practitioner
    ManualRecordKind.PRACTITIONER_ROLE -> R.string.medical_entry_name_role
    ManualRecordKind.ORGANIZATION, ManualRecordKind.LOCATION -> R.string.medical_entry_name_organization
    ManualRecordKind.PATIENT -> R.string.medical_entry_given_name
}

@StringRes
private fun dateLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VACCINE -> R.string.medical_entry_date_given
    ManualRecordKind.MEDICATION -> R.string.medical_entry_date_started
    ManualRecordKind.PRESCRIPTION -> R.string.medical_entry_date_prescribed
    ManualRecordKind.PATIENT -> SummaryField.BIRTH_DATE.labelRes
    ManualRecordKind.ALLERGY, ManualRecordKind.CONDITION -> SummaryField.ONSET.labelRes
    ManualRecordKind.VISIT -> R.string.medical_entry_date_visit
    ManualRecordKind.PROCEDURE -> R.string.medical_entry_date_performed
    ManualRecordKind.LAB_RESULT -> R.string.medical_entry_date_collected
    ManualRecordKind.VITAL_SIGN -> R.string.medical_entry_date_measured
    ManualRecordKind.SOCIAL_HISTORY, ManualRecordKind.PREGNANCY -> R.string.medical_entry_date_recorded
    ManualRecordKind.PRACTITIONER, ManualRecordKind.PRACTITIONER_ROLE, ManualRecordKind.ORGANIZATION, ManualRecordKind.LOCATION -> null
}

@StringRes
private fun detailLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VACCINE -> SummaryField.LOT_NUMBER.labelRes
    ManualRecordKind.ALLERGY -> SummaryField.REACTION.labelRes
    ManualRecordKind.MEDICATION, ManualRecordKind.PRESCRIPTION -> SummaryField.DOSAGE.labelRes
    ManualRecordKind.VISIT, ManualRecordKind.PROCEDURE -> SummaryField.REASON.labelRes
    ManualRecordKind.PRACTITIONER -> R.string.medical_entry_qualification
    ManualRecordKind.PRACTITIONER_ROLE -> SummaryField.SPECIALTY.labelRes
    ManualRecordKind.ORGANIZATION, ManualRecordKind.LOCATION, ManualRecordKind.PATIENT -> R.string.medical_entry_address
    else -> null
}

@StringRes
private fun doctorLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VISIT, ManualRecordKind.PRACTITIONER_ROLE -> R.string.medical_entry_doctor
    ManualRecordKind.PROCEDURE -> SummaryField.PERFORMER.labelRes
    ManualRecordKind.PRESCRIPTION -> R.string.medical_entry_prescribed_by
    ManualRecordKind.PATIENT -> R.string.medical_entry_family_doctor
    else -> null
}

@StringRes
private fun organizationLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VISIT, ManualRecordKind.PROCEDURE, ManualRecordKind.PRACTITIONER_ROLE -> R.string.medical_entry_clinic
    ManualRecordKind.LAB_RESULT -> R.string.medical_entry_laboratory
    else -> null
}

@StringRes
private fun placeLabelRes(kind: ManualRecordKind): Int? = when (kind) {
    ManualRecordKind.VISIT, ManualRecordKind.PROCEDURE, ManualRecordKind.PRACTITIONER_ROLE -> R.string.medical_entry_place
    else -> null
}
