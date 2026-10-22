package tech.mmarca.openvitals.domain.medical

import java.time.LocalDate
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** The kinds of record a person can reasonably type. */
enum class ManualRecordKind(
    val resourceType: String,
    val category: MedicalCategory,
    /** The FHIR status codes offered, the default first. Entered-in-error and unknown are left out. */
    val statuses: List<String>,
    /** Code systems a user may have a code from. Nothing is bundled: the user types the code. */
    val codeSystems: List<String>,
    /** The statuses of something still ahead. Only these may carry a date in the future. */
    val futureStatuses: Set<String> = emptySet(),
) {
    VACCINE("Immunization", MedicalCategory.VACCINES, listOf("completed", "not-done"), listOf(Cvx, Snomed, Atc)),
    ALLERGY("AllergyIntolerance", MedicalCategory.ALLERGIES, listOf("active", "inactive", "resolved"), listOf(Snomed)),
    MEDICATION(
        "MedicationStatement",
        MedicalCategory.MEDICATIONS,
        listOf("active", "completed", "intended", "stopped", "on-hold", "not-taken"),
        listOf(Atc, RxNorm, Snomed),
        futureStatuses = setOf("intended"),
    ),
    PRESCRIPTION(
        "MedicationRequest",
        MedicalCategory.MEDICATIONS,
        listOf("active", "on-hold", "completed", "stopped", "cancelled", "draft"),
        listOf(Atc, RxNorm, Snomed),
    ),
    CONDITION(
        "Condition",
        MedicalCategory.CONDITIONS,
        listOf("active", "recurrence", "relapse", "remission", "resolved", "inactive"),
        listOf(Snomed, Icd10),
    ),
    VISIT(
        "Encounter",
        MedicalCategory.VISITS,
        listOf("finished", "planned", "arrived", "triaged", "in-progress", "onleave", "cancelled"),
        listOf(Snomed),
        futureStatuses = setOf("planned"),
    ),
    PROCEDURE(
        "Procedure",
        MedicalCategory.PROCEDURES,
        listOf("completed", "preparation", "in-progress", "on-hold", "stopped", "not-done"),
        listOf(Snomed, Cpt),
        futureStatuses = setOf("preparation"),
    ),
    LAB_RESULT("Observation", MedicalCategory.LAB_RESULTS, ObservationStatuses, listOf(Loinc, Snomed)),
    VITAL_SIGN("Observation", MedicalCategory.VITAL_SIGNS, ObservationStatuses, emptyList()),
    SOCIAL_HISTORY("Observation", MedicalCategory.SOCIAL_HISTORY, ObservationStatuses, emptyList()),
    PREGNANCY("Observation", MedicalCategory.PREGNANCY, ObservationStatuses, emptyList()),
    PRACTITIONER("Practitioner", MedicalCategory.PRACTITIONER_DETAILS, emptyList(), emptyList()),
    PRACTITIONER_ROLE("PractitionerRole", MedicalCategory.PRACTITIONER_DETAILS, emptyList(), emptyList()),
    ORGANIZATION("Organization", MedicalCategory.VISITS, emptyList(), emptyList()),
    LOCATION("Location", MedicalCategory.VISITS, emptyList(), emptyList()),
    PATIENT("Patient", MedicalCategory.PERSONAL_DETAILS, emptyList(), emptyList()),
    ;

    /** A record of something that happened on a day: without the day it says little. */
    val needsDate: Boolean
        get() = this == VACCINE || this == VISIT || this == PROCEDURE || this == LAB_RESULT || isMeasured

    /** An Observation that says one [ObservationMeasure]: a vital sign, social history, or pregnancy. */
    val isMeasured: Boolean
        get() = this == VITAL_SIGN || this == SOCIAL_HISTORY || this == PREGNANCY

    /** A doctor, a role, a clinic, a place, or the owner: a record the others point at, not one about the owner's care. */
    val isDirectory: Boolean
        get() = this == PRACTITIONER || this == PRACTITIONER_ROLE || this == ORGANIZATION || this == LOCATION || this == PATIENT

    /** FHIR R4 gives these no `note`, and Health Connect refuses a field the type does not have. */
    val hasNote: Boolean
        get() = this != VISIT && !isDirectory

    /** The owner's Patient record is written with the first entry and edited, never added. */
    val canAdd: Boolean
        get() = this != PATIENT

    companion object {
        /** The kind that writes [type] records into [category]: an Observation's kind depends on where it is filed. */
        fun of(type: String, category: MedicalCategory?): ManualRecordKind? =
            entries.firstOrNull { it.resourceType == type && it.category == category } ?: entries.firstOrNull { it.resourceType == type }

        fun ofCategory(category: MedicalCategory): ManualRecordKind? = entries.firstOrNull { it.category == category }
    }
}

/** A code the user has, such as one printed on a vaccine card. [system] is a code system URI, or null when unknown. */
data class ManualCode(val system: String?, val code: String)

/**
 * A doctor or a clinic a record points at. [type] is `Practitioner` or `Organization`. [id] is
 * that record's id in the manual source, or null for a name typed as new: the save writes the
 * record first. [display] travels in the reference too, so the name shows even where the record
 * cannot be read.
 */
data class ManualReference(val type: String, val display: String, val id: String? = null) {
    companion object {
        const val Practitioner = "Practitioner"
        const val Organization = "Organization"
        const val Location = "Location"
    }
}

/** The doctors and clinics already in the manual source, offered by the form before a new name is typed. */
data class ManualDirectory(
    val practitioners: List<ManualReference> = emptyList(),
    val organizations: List<ManualReference> = emptyList(),
    val locations: List<ManualReference> = emptyList(),
) {
    /** [reference] pointed at the record of the same name when one exists, so typing a known name makes no twin. */
    fun resolve(reference: ManualReference?): ManualReference? {
        if (reference == null || reference.id != null) return reference
        val name = reference.display.trim().takeIf { it.isNotEmpty() } ?: return null
        val known = when (reference.type) {
            ManualReference.Practitioner -> practitioners
            ManualReference.Location -> locations
            else -> organizations
        }
        return known.firstOrNull { it.display.equals(name, ignoreCase = true) } ?: reference.copy(display = name)
    }
}

/**
 * What a manual entry form holds. [date] is when a vaccine was given, a visit or procedure took
 * place or a lab sample was taken (all required), when a medication was started, or when an
 * allergy or condition began. [detail] is a vaccine's lot number, an allergy's reaction, a
 * medication's dosage, the reason for a visit or procedure, a doctor's qualification, or a
 * clinic's address. [criticality] is an allergy's only. [doctor] and [organization] are who saw
 * the person, did the procedure or ran the test. [setting] is a visit's FHIR encounter class
 * code. [result], [unit], [interpretation] and [referenceRange] are a lab result's. [contact] is a
 * doctor's, clinic's, place's or the owner's phone number or email address. [place] is where a
 * visit or procedure took place, or where a doctor works. For the owner's own record, [name] is
 * the given name, [familyName] the family name, [date] the birth date, [gender] FHIR's code, and
 * [doctor] the family doctor. A vital sign, social history or pregnancy
 * record says one [measure]: its value is [result] (with [secondResult], the diastolic, for a blood
 * pressure, and [unit] the UCUM code typed in), [answer] for a coded one, whose words are in
 * [result], or [valueDate].
 */
data class ManualRecordDraft(
    val kind: ManualRecordKind,
    val name: String = "",
    val status: String = kind.statuses.firstOrNull().orEmpty(),
    val date: LocalDate? = null,
    val detail: String = "",
    val criticality: String? = null,
    val code: ManualCode? = null,
    val note: String = "",
    val doctor: ManualReference? = null,
    val organization: ManualReference? = null,
    val setting: String = VisitSettings.first(),
    val result: String = "",
    val unit: String = "",
    val interpretation: String? = null,
    val referenceRange: String = "",
    val contact: String = "",
    val measure: ObservationMeasure? = ObservationMeasure.of(kind).firstOrNull(),
    val secondResult: String = "",
    val answer: String? = null,
    val valueDate: LocalDate? = null,
    val place: ManualReference? = null,
    val familyName: String = "",
    val gender: String? = null,
) {
    /** True when the date lies after [today] for a status that is not still ahead. */
    fun hasFutureDate(today: LocalDate): Boolean = date != null && date.isAfter(today) && status !in kind.futureStatuses

    /**
     * A name is needed, except where a measure names the record; a dated kind needs its date, a lab
     * result its result, and a measured kind a value its measure can take. Only what is still
     * ahead may be dated ahead.
     */
    fun isComplete(today: LocalDate): Boolean =
        (name.isNotBlank() || (kind.isMeasured && measure != null && measure.value != MeasureValue.TEXT) ||
            (kind == ManualRecordKind.PATIENT && familyName.isNotBlank())) &&
            (kind != ManualRecordKind.PRACTITIONER_ROLE || doctor != null) &&
            (!kind.needsDate || date != null) &&
            (kind != ManualRecordKind.LAB_RESULT || result.isNotBlank()) &&
            (!kind.isMeasured || hasMeasuredValue()) &&
            !hasFutureDate(today)

    private fun hasMeasuredValue(): Boolean = when (measure?.value) {
        null -> false
        MeasureValue.QUANTITY -> result.toDecimalOrNull() != null
        MeasureValue.BLOOD_PRESSURE -> result.toDecimalOrNull() != null && secondResult.toDecimalOrNull() != null
        MeasureValue.INTEGER -> result.trim().toIntOrNull()?.let { it >= 0 } == true
        MeasureValue.DATE -> valueDate != null
        MeasureValue.CODED -> answer != null
        MeasureValue.TEXT -> result.isNotBlank()
    }
}

/** A decimal as typed, with a comma or a point. */
internal fun String.toDecimalOrNull(): java.math.BigDecimal? = trim().replace(',', '.').toBigDecimalOrNull()

private val ObservationStatuses = listOf("final", "registered", "preliminary", "amended", "corrected", "cancelled")

/**
 * FHIR's encounter classes (HL7 v3 ActEncounterCode), the default first: outpatient,
 * emergency, inpatient, acute and non-acute inpatient, observation, short stay, pre-admission,
 * home, virtual and field.
 */
val VisitSettings: List<String> = listOf("AMB", "EMER", "IMP", "ACUTE", "NONAC", "OBSENC", "SS", "PRENC", "HH", "VR", "FLD")

/** FHIR's administrative genders. */
val Genders: List<String> = listOf("female", "male", "other", "unknown")

/** FHIR's interpretation codes a lab report prints beside a result. */
val LabInterpretations: List<String> = listOf("H", "L", "N", "A")

internal const val Snomed = "http://snomed.info/sct"
internal const val Cvx = "http://hl7.org/fhir/sid/cvx"
internal const val Atc = "http://www.whocc.no/atc"
internal const val RxNorm = "http://www.nlm.nih.gov/research/umls/rxnorm"
internal const val Icd10 = "http://hl7.org/fhir/sid/icd-10"
internal const val Cpt = "http://www.ama-assn.org/go/cpt"
internal const val Loinc = "http://loinc.org"
