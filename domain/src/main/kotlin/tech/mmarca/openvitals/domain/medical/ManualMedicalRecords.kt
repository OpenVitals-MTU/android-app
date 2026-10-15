package tech.mmarca.openvitals.domain.medical

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** The four kinds of record a person can reasonably type. */
enum class ManualRecordKind(
    val resourceType: String,
    val category: MedicalCategory,
    /** The FHIR status codes offered, the default first. */
    val statuses: List<String>,
    /** Code systems a user may have a code from. Nothing is bundled: the user types the code. */
    val codeSystems: List<String>,
) {
    VACCINE("Immunization", MedicalCategory.VACCINES, listOf("completed", "not-done"), listOf(Cvx, Snomed, Atc)),
    ALLERGY("AllergyIntolerance", MedicalCategory.ALLERGIES, listOf("active", "inactive", "resolved"), listOf(Snomed)),
    MEDICATION("MedicationStatement", MedicalCategory.MEDICATIONS, listOf("active", "completed", "stopped", "on-hold"), listOf(Atc, RxNorm, Snomed)),
    CONDITION("Condition", MedicalCategory.CONDITIONS, listOf("active", "remission", "resolved", "inactive"), listOf(Snomed, Icd10)),
    ;

    companion object {
        fun ofResourceType(type: String): ManualRecordKind? = entries.firstOrNull { it.resourceType == type }

        fun ofCategory(category: MedicalCategory): ManualRecordKind? = entries.firstOrNull { it.category == category }
    }
}

/** A code the user has, such as one printed on a vaccine card. [system] is a code system URI, or null when unknown. */
data class ManualCode(val system: String?, val code: String)

/**
 * What a manual entry form holds. [date] is when a vaccine was given (required), when a
 * medication was started, or when an allergy or condition began. [detail] is a vaccine's lot
 * number, an allergy's reaction, or a medication's dosage. [criticality] is an allergy's only.
 */
data class ManualRecordDraft(
    val kind: ManualRecordKind,
    val name: String = "",
    val status: String = kind.statuses.first(),
    val date: LocalDate? = null,
    val detail: String = "",
    val criticality: String? = null,
    val code: ManualCode? = null,
    val note: String = "",
) {
    /** A name is always needed, and a vaccine needs its date. A date in the future is refused. */
    fun isComplete(today: LocalDate): Boolean =
        name.isNotBlank() && (kind != ManualRecordKind.VACCINE || date != null) && (date == null || !date.isAfter(today))
}

/**
 * Writes manual entries as FHIR. Every record references the owner's Patient, [PatientId],
 * in the manual source, and fills what FHIR requires: a vaccine's status and date, an allergy's
 * and a condition's clinical status, a medication's status. Empty fields are left out, since
 * Health Connect refuses an empty value.
 */
object ManualFhirWriter {
    const val SourceBaseUri = "openvitals://manual"
    const val PatientId = "self"

    /**
     * The record for [draft] with [id]. When [base] is the stored record, the fields a form
     * controls are replaced and every other field is kept, so an edit loses nothing.
     * [savedAt] becomes `meta.lastUpdated`, so phone-to-phone sync can tell the newer edit.
     */
    fun resource(
        draft: ManualRecordDraft,
        id: String,
        today: LocalDate,
        base: JsonObject? = null,
        savedAt: Instant? = null,
    ): JsonObject {
        val start = (base ?: JsonObject(emptyMap())).without(*managedFields(draft.kind))
        val fields = LinkedHashMap<String, JsonElement>()
        fields["resourceType"] = JsonPrimitive(draft.kind.resourceType)
        fields["id"] = JsonPrimitive(id)
        val meta = (start.obj("meta") ?: JsonObject(emptyMap())).with("source", JsonPrimitive(SourceBaseUri))
        fields["meta"] = savedAt?.let { meta.with("lastUpdated", JsonPrimitive(it.truncatedTo(ChronoUnit.SECONDS).toString())) } ?: meta
        start.forEach { (key, value) -> if (key !in fields) fields[key] = value }
        kindFields(draft, today, start).forEach { (key, value) -> fields[key] = value }
        draft.note.trim().takeIf { it.isNotEmpty() }?.let { fields["note"] = buildJsonArray { add(buildJsonObject { put("text", it) }) } }
        return JsonObject(fields)
    }

    /** The owner's Patient record. Parts that are unknown are left out. */
    fun patient(identity: PatientIdentity): JsonObject = buildJsonObject {
        put("resourceType", "Patient")
        put("id", PatientId)
        put("meta", buildJsonObject { put("source", SourceBaseUri) })
        val name = buildJsonObject {
            put("use", "usual")
            identity.givenNames.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { given ->
                put("given", JsonArray(given.map { JsonPrimitive(it.trim()) }))
            }
            identity.familyNames.firstOrNull { it.isNotBlank() }?.let { put("family", it.trim()) }
        }
        if (name.size > 1) put("name", buildJsonArray { add(name) })
        identity.birthDate?.takeIf { it.isNotBlank() }?.let { put("birthDate", it) }
    }

    /** A stored record read back into a form. Fields the form does not show stay in the record. */
    fun draftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft {
        val concept = when (kind) {
            ManualRecordKind.VACCINE -> json.obj("vaccineCode")
            ManualRecordKind.MEDICATION -> json.obj("medicationCodeableConcept")
            ManualRecordKind.ALLERGY, ManualRecordKind.CONDITION -> json.obj("code")
        }
        val coding = concept?.objects("coding")?.firstOrNull()
        val status = when (kind) {
            ManualRecordKind.VACCINE, ManualRecordKind.MEDICATION -> json.string("status")
            ManualRecordKind.ALLERGY, ManualRecordKind.CONDITION -> json.obj("clinicalStatus")?.objects("coding")?.firstOrNull()?.string("code")
        }
        val dateText = when (kind) {
            ManualRecordKind.VACCINE -> json.string("occurrenceDateTime")
            ManualRecordKind.MEDICATION -> json.obj("effectivePeriod")?.string("start") ?: json.string("effectiveDateTime")
            ManualRecordKind.ALLERGY, ManualRecordKind.CONDITION -> json.string("onsetDateTime")
        }
        val detail = when (kind) {
            ManualRecordKind.VACCINE -> json.string("lotNumber")
            ManualRecordKind.ALLERGY -> json.objects("reaction").firstOrNull()?.objects("manifestation")?.firstOrNull()?.codeableLabel()
            ManualRecordKind.MEDICATION -> json.objects("dosage").firstOrNull()?.string("text")
            ManualRecordKind.CONDITION -> null
        }
        return ManualRecordDraft(
            kind = kind,
            name = concept?.codeableLabel().orEmpty(),
            status = status?.takeIf { it in kind.statuses } ?: kind.statuses.first(),
            date = dateText?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            detail = detail.orEmpty(),
            criticality = json.string("criticality").takeIf { kind == ManualRecordKind.ALLERGY },
            code = coding?.string("code")?.let { ManualCode(coding.string("system"), it) },
            note = json.objects("note").firstOrNull()?.string("text").orEmpty(),
        )
    }

    private fun managedFields(kind: ManualRecordKind): Array<String> = when (kind) {
        ManualRecordKind.VACCINE -> arrayOf("status", "vaccineCode", "patient", "occurrenceDateTime", "occurrenceString", "lotNumber", "note")
        ManualRecordKind.ALLERGY -> arrayOf("clinicalStatus", "code", "patient", "criticality", "onsetDateTime", "reaction", "note")
        ManualRecordKind.MEDICATION -> arrayOf("status", "medicationCodeableConcept", "subject", "effectivePeriod", "effectiveDateTime", "dosage", "note")
        ManualRecordKind.CONDITION -> arrayOf("clinicalStatus", "code", "subject", "onsetDateTime", "note")
    }

    private fun kindFields(draft: ManualRecordDraft, today: LocalDate, start: JsonObject): Map<String, JsonElement> {
        val detail = draft.detail.trim().takeIf { it.isNotEmpty() }
        val date = draft.date?.toString()
        return buildMap {
            when (draft.kind) {
                ManualRecordKind.VACCINE -> {
                    put("status", JsonPrimitive(draft.status))
                    put("vaccineCode", concept(draft))
                    put("patient", PatientReference)
                    put("occurrenceDateTime", JsonPrimitive(date ?: today.toString()))
                    detail?.let { put("lotNumber", JsonPrimitive(it)) }
                }
                ManualRecordKind.ALLERGY -> {
                    put("clinicalStatus", clinicalStatus(AllergyClinicalSystem, draft.status))
                    put("code", concept(draft))
                    put("patient", PatientReference)
                    draft.criticality?.let { put("criticality", JsonPrimitive(it)) }
                    date?.let { put("onsetDateTime", JsonPrimitive(it)) }
                    if (start.string("recordedDate") == null) put("recordedDate", JsonPrimitive(today.toString()))
                    detail?.let { reaction ->
                        put("reaction", buildJsonArray { add(buildJsonObject { put("manifestation", buildJsonArray { add(text(reaction)) }) }) })
                    }
                }
                ManualRecordKind.MEDICATION -> {
                    put("status", JsonPrimitive(draft.status))
                    put("medicationCodeableConcept", concept(draft))
                    put("subject", PatientReference)
                    date?.let { put("effectivePeriod", buildJsonObject { put("start", it) }) }
                    detail?.let { put("dosage", buildJsonArray { add(buildJsonObject { put("text", it) }) }) }
                }
                ManualRecordKind.CONDITION -> {
                    put("clinicalStatus", clinicalStatus(ConditionClinicalSystem, draft.status))
                    put("code", concept(draft))
                    put("subject", PatientReference)
                    date?.let { put("onsetDateTime", JsonPrimitive(it)) }
                    if (start.string("recordedDate") == null) put("recordedDate", JsonPrimitive(today.toString()))
                }
            }
        }
    }

    /** The name as text, and the user's code as a coding when there is one. */
    private fun concept(draft: ManualRecordDraft): JsonObject = buildJsonObject {
        draft.code?.takeIf { it.code.isNotBlank() }?.let { code ->
            put(
                "coding",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            code.system?.let { put("system", it) }
                            put("code", code.code.trim())
                        },
                    )
                },
            )
        }
        put("text", draft.name.trim())
    }

    private fun text(value: String) = buildJsonObject { put("text", value) }

    private fun clinicalStatus(system: String, code: String) = buildJsonObject {
        put("coding", buildJsonArray { add(buildJsonObject { put("system", system); put("code", code) }) })
    }

    private val PatientReference = buildJsonObject { put("reference", "Patient/$PatientId") }
    private const val AllergyClinicalSystem = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
    private const val ConditionClinicalSystem = "http://terminology.hl7.org/CodeSystem/condition-clinical"
}

private const val Snomed = "http://snomed.info/sct"
private const val Cvx = "http://hl7.org/fhir/sid/cvx"
private const val Atc = "http://www.whocc.no/atc"
private const val RxNorm = "http://www.nlm.nih.gov/research/umls/rxnorm"
private const val Icd10 = "http://hl7.org/fhir/sid/icd-10"
