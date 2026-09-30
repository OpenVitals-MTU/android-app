package tech.mmarca.openvitals.features.medical

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalCategoryBlock

@get:StringRes
internal val MedicalCategory.titleRes: Int
    get() = when (this) {
        MedicalCategory.VACCINES -> R.string.medical_category_vaccines
        MedicalCategory.ALLERGIES -> R.string.medical_category_allergies
        MedicalCategory.CONDITIONS -> R.string.medical_category_conditions
        MedicalCategory.MEDICATIONS -> R.string.medical_category_medications
        MedicalCategory.LAB_RESULTS -> R.string.medical_category_lab_results
        MedicalCategory.PROCEDURES -> R.string.medical_category_procedures
        MedicalCategory.VISITS -> R.string.medical_category_visits
        MedicalCategory.VITAL_SIGNS -> R.string.medical_category_vital_signs
        MedicalCategory.PREGNANCY -> R.string.medical_category_pregnancy
        MedicalCategory.SOCIAL_HISTORY -> R.string.medical_category_social_history
        MedicalCategory.PERSONAL_DETAILS -> R.string.medical_category_personal_details
        MedicalCategory.PRACTITIONER_DETAILS -> R.string.medical_category_practitioner_details
    }

internal val MedicalCategory.icon: ImageVector
    get() = when (this) {
        MedicalCategory.VACCINES -> Icons.Outlined.Vaccines
        MedicalCategory.ALLERGIES -> Icons.Outlined.Sick
        MedicalCategory.CONDITIONS -> Icons.Outlined.Healing
        MedicalCategory.MEDICATIONS -> Icons.Outlined.Medication
        MedicalCategory.LAB_RESULTS -> Icons.Outlined.Science
        MedicalCategory.PROCEDURES -> Icons.Outlined.MedicalServices
        MedicalCategory.VISITS -> Icons.Outlined.LocalHospital
        MedicalCategory.VITAL_SIGNS -> Icons.Outlined.MonitorHeart
        MedicalCategory.PREGNANCY -> Icons.Outlined.PregnantWoman
        MedicalCategory.SOCIAL_HISTORY -> Icons.Outlined.Groups
        MedicalCategory.PERSONAL_DETAILS -> Icons.Outlined.Badge
        MedicalCategory.PRACTITIONER_DETAILS -> Icons.Outlined.AssignmentInd
    }

@get:StringRes
internal val MedicalCategoryBlock.titleRes: Int
    get() = when (this) {
        MedicalCategoryBlock.CARE -> R.string.medical_records_block_care
        MedicalCategoryBlock.SENSITIVE -> R.string.medical_records_block_sensitive
    }

@get:StringRes
internal val SummaryField.labelRes: Int
    get() = when (this) {
        SummaryField.VERIFICATION -> R.string.medical_field_verification
        SummaryField.CATEGORY -> R.string.medical_field_category
        SummaryField.CRITICALITY -> R.string.medical_field_criticality
        SummaryField.SEVERITY -> R.string.medical_field_severity
        SummaryField.REACTION -> R.string.medical_field_reaction
        SummaryField.ONSET -> R.string.medical_field_onset
        SummaryField.RECORDED -> R.string.medical_field_recorded
        SummaryField.LOT_NUMBER -> R.string.medical_field_lot_number
        SummaryField.SITE -> R.string.medical_field_site
        SummaryField.ROUTE -> R.string.medical_field_route
        SummaryField.DOSE_NUMBER -> R.string.medical_field_dose_number
        SummaryField.PERFORMER -> R.string.medical_field_performer
        SummaryField.DOSAGE -> R.string.medical_field_dosage
        SummaryField.REQUESTER -> R.string.medical_field_requester
        SummaryField.REASON -> R.string.medical_field_reason
        SummaryField.INTERPRETATION -> R.string.medical_field_interpretation
        SummaryField.REFERENCE_RANGE -> R.string.medical_field_reference_range
        SummaryField.COMPONENT -> R.string.medical_field_component
        SummaryField.NOTE -> R.string.medical_field_note
        SummaryField.OUTCOME -> R.string.medical_field_outcome
        SummaryField.BODY_SITE -> R.string.medical_field_body_site
        SummaryField.PARTICIPANT -> R.string.medical_field_participant
        SummaryField.LOCATION -> R.string.medical_field_location
        SummaryField.SERVICE_PROVIDER -> R.string.medical_field_service_provider
        SummaryField.PERIOD -> R.string.medical_field_period
        SummaryField.BIRTH_DATE -> R.string.medical_field_birth_date
        SummaryField.GENDER -> R.string.medical_field_gender
        SummaryField.SPECIALTY -> R.string.medical_field_specialty
        SummaryField.TYPE -> R.string.medical_field_type
        SummaryField.CONTACT -> R.string.medical_field_contact
    }

/** A word for a FHIR status or verification code. Null for a code this app has no word for: it shows as written. */
@StringRes
internal fun fhirStatusLabelRes(code: String?): Int? = when (code) {
    "active" -> R.string.medical_status_active
    "inactive" -> R.string.medical_status_inactive
    "resolved" -> R.string.medical_status_resolved
    "recurrence" -> R.string.medical_status_recurrence
    "relapse" -> R.string.medical_status_relapse
    "remission" -> R.string.medical_status_remission
    "completed" -> R.string.medical_status_completed
    "not-done" -> R.string.medical_status_not_done
    "entered-in-error" -> R.string.medical_status_entered_in_error
    "on-hold" -> R.string.medical_status_on_hold
    "cancelled" -> R.string.medical_status_cancelled
    "stopped" -> R.string.medical_status_stopped
    "draft" -> R.string.medical_status_draft
    "unknown" -> R.string.medical_status_unknown
    "intended" -> R.string.medical_status_intended
    "not-taken" -> R.string.medical_status_not_taken
    "registered" -> R.string.medical_status_registered
    "preliminary" -> R.string.medical_status_preliminary
    "final" -> R.string.medical_status_final
    "amended" -> R.string.medical_status_amended
    "corrected" -> R.string.medical_status_corrected
    "preparation" -> R.string.medical_status_preparation
    "in-progress" -> R.string.medical_status_in_progress
    "planned" -> R.string.medical_status_planned
    "arrived" -> R.string.medical_status_arrived
    "triaged" -> R.string.medical_status_triaged
    "onleave" -> R.string.medical_status_onleave
    "finished" -> R.string.medical_status_finished
    "unconfirmed" -> R.string.medical_status_unconfirmed
    "confirmed" -> R.string.medical_status_confirmed
    "provisional" -> R.string.medical_status_provisional
    "differential" -> R.string.medical_status_differential
    "refuted" -> R.string.medical_status_refuted
    else -> null
}

/** A word for a coded detail value, where the field holds a code rather than text. */
@StringRes
internal fun fhirCodeLabelRes(field: SummaryField, code: String): Int? = when (field) {
    SummaryField.VERIFICATION -> fhirStatusLabelRes(code)
    SummaryField.CRITICALITY -> when (code) {
        "low" -> R.string.medical_code_low
        "high" -> R.string.medical_code_high
        "unable-to-assess" -> R.string.medical_code_unable_to_assess
        else -> null
    }
    SummaryField.GENDER -> when (code) {
        "male" -> R.string.medical_code_male
        "female" -> R.string.medical_code_female
        "other" -> R.string.medical_code_other
        "unknown" -> R.string.medical_status_unknown
        else -> null
    }
    SummaryField.CATEGORY -> when (code) {
        "food" -> R.string.medical_code_food
        "medication" -> R.string.medical_code_medication
        "environment" -> R.string.medical_code_environment
        "biologic" -> R.string.medical_code_biologic
        else -> null
    }
    else -> null
}

/** A short name for a code system, shown as a caption. Code system names are proper names and stay untranslated. */
internal fun codeSystemLabel(system: String): String = KnownCodeSystems[system.trimEnd('/')]
    ?: system.trimEnd('/').substringAfterLast('/').substringAfterLast(':').ifBlank { system }

private val KnownCodeSystems = mapOf(
    "http://snomed.info/sct" to "SNOMED CT",
    "http://loinc.org" to "LOINC",
    "http://hl7.org/fhir/sid/cvx" to "CVX",
    "http://www.nlm.nih.gov/research/umls/rxnorm" to "RxNorm",
    "http://hl7.org/fhir/sid/icd-10" to "ICD-10",
    "http://hl7.org/fhir/sid/icd-10-cm" to "ICD-10-CM",
    "http://www.whocc.no/atc" to "ATC",
    "http://unitsofmeasure.org" to "UCUM",
)

/**
 * A detail value that holds FHIR dates, in the locale's format: onset, recorded and birth
 * dates, and both ends of a period. Anything that does not parse shows as written.
 */
internal fun dateFieldText(field: SummaryField, text: String, formatters: DateTimeFormatterProvider): String = when (field) {
    SummaryField.ONSET, SummaryField.RECORDED, SummaryField.BIRTH_DATE -> FhirDate(text).displayText(formatters.mediumDate())
    SummaryField.PERIOD -> text.split(PeriodSeparator).joinToString(PeriodSeparator) { dateTimeText(it, formatters) }
    else -> text
}

private fun dateTimeText(text: String, formatters: DateTimeFormatterProvider): String =
    runCatching { formatters.mediumDateTime().format(OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault())) }
        .recoverCatching { formatters.mediumDateTime().format(LocalDateTime.parse(text)) }
        .getOrElse { FhirDate(text).displayText(formatters.mediumDate()) }

private const val PeriodSeparator = " – "

/** A full date in the locale's format. A partial one, such as "2020", shows as written. */
internal fun FhirDate.displayText(formatter: DateTimeFormatter): String =
    if (text.length >= FullDateLength) {
        runCatching { formatter.format(LocalDate.parse(text.take(FullDateLength))) }.getOrDefault(text)
    } else {
        text
    }

private const val FullDateLength = 10
