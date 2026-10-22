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

/**
 * Writes manual entries as FHIR R4. Every record about the owner references the owner's
 * Patient, [PatientId], in the manual source. Each fills what FHIR requires of its type and only
 * the fields the type has: Health Connect refuses a missing required field, an unknown top-level
 * field and an empty value.
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
        if (draft.kind.hasNote) {
            draft.note.trim().takeIf { it.isNotEmpty() }?.let { fields["note"] = buildJsonArray { add(text(it)) } }
        }
        return JsonObject(fields)
    }

    /**
     * [draft] with an id for each doctor or clinic typed as a new name, and the records those ids
     * name, to write in the same transaction. A name picked from the user's records keeps its id.
     */
    fun linkNewReferences(
        draft: ManualRecordDraft,
        today: LocalDate,
        newId: () -> String,
        savedAt: Instant? = null,
    ): Pair<ManualRecordDraft, List<JsonObject>> {
        val created = mutableListOf<JsonObject>()
        fun link(reference: ManualReference?): ManualReference? {
            val name = reference?.display?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (reference.id != null) return reference
            val kind = when (reference.type) {
                ManualReference.Practitioner -> ManualRecordKind.PRACTITIONER
                ManualReference.Location -> ManualRecordKind.LOCATION
                else -> ManualRecordKind.ORGANIZATION
            }
            val id = newId()
            created += resource(ManualRecordDraft(kind, name = name), id, today, savedAt = savedAt)
            return reference.copy(display = name, id = id)
        }
        return draft.copy(doctor = link(draft.doctor), organization = link(draft.organization), place = link(draft.place)) to created
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
    fun draftOf(kind: ManualRecordKind, json: JsonObject): ManualRecordDraft = ManualDraftReader.draftOf(kind, json)

    private fun managedFields(kind: ManualRecordKind): Array<String> = when (kind) {
        ManualRecordKind.VACCINE -> arrayOf("status", "vaccineCode", "patient", "occurrenceDateTime", "occurrenceString", "lotNumber", "note")
        ManualRecordKind.ALLERGY -> arrayOf("clinicalStatus", "code", "patient", "criticality", "onsetDateTime", "reaction", "note")
        ManualRecordKind.MEDICATION -> arrayOf("status", "medicationCodeableConcept", "subject", "effectivePeriod", "effectiveDateTime", "dosage", "note")
        ManualRecordKind.PRESCRIPTION ->
            arrayOf("status", "intent", "medicationCodeableConcept", "subject", "authoredOn", "requester", "dosageInstruction", "note")
        ManualRecordKind.CONDITION -> arrayOf("clinicalStatus", "code", "subject", "onsetDateTime", "note")
        ManualRecordKind.VISIT ->
            arrayOf("status", "class", "type", "subject", "period", "reasonCode", "participant", "serviceProvider", "location")
        ManualRecordKind.PROCEDURE ->
            arrayOf("status", "code", "subject", "performedDateTime", "performedPeriod", "reasonCode", "performer", "location", "note")
        ManualRecordKind.LAB_RESULT -> arrayOf(
            "status", "category", "code", "subject", "effectiveDateTime", "effectivePeriod", "valueQuantity", "valueString",
            "valueCodeableConcept", "interpretation", "referenceRange", "performer", "note",
        )
        ManualRecordKind.VITAL_SIGN, ManualRecordKind.SOCIAL_HISTORY, ManualRecordKind.PREGNANCY -> arrayOf(
            "status", "category", "code", "subject", "effectiveDateTime", "effectivePeriod", "valueQuantity", "valueString",
            "valueInteger", "valueDateTime", "valueCodeableConcept", "component", "note",
        )
        ManualRecordKind.PRACTITIONER -> arrayOf("name", "qualification", "telecom")
        ManualRecordKind.PRACTITIONER_ROLE -> arrayOf("practitioner", "organization", "code", "specialty", "location", "telecom")
        ManualRecordKind.ORGANIZATION -> arrayOf("name", "telecom", "address")
        ManualRecordKind.LOCATION -> arrayOf("name", "telecom", "address")
        ManualRecordKind.PATIENT -> arrayOf("name", "birthDate", "gender", "telecom", "address", "generalPractitioner")
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
                    put("clinicalStatus", coding(AllergyClinicalSystem, draft.status))
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
                    detail?.let { put("dosage", buildJsonArray { add(text(it)) }) }
                }
                // A prescription the person was given: always an order, as FHIR requires an intent.
                ManualRecordKind.PRESCRIPTION -> {
                    put("status", JsonPrimitive(draft.status))
                    put("intent", JsonPrimitive("order"))
                    put("medicationCodeableConcept", concept(draft))
                    put("subject", PatientReference)
                    date?.let { put("authoredOn", JsonPrimitive(it)) }
                    draft.doctor?.let { put("requester", reference(it)) }
                    detail?.let { put("dosageInstruction", buildJsonArray { add(text(it)) }) }
                }
                ManualRecordKind.CONDITION -> {
                    put("clinicalStatus", coding(ConditionClinicalSystem, draft.status))
                    put("code", concept(draft))
                    put("subject", PatientReference)
                    date?.let { put("onsetDateTime", JsonPrimitive(it)) }
                    if (start.string("recordedDate") == null) put("recordedDate", JsonPrimitive(today.toString()))
                }
                ManualRecordKind.VISIT, ManualRecordKind.PROCEDURE, ManualRecordKind.LAB_RESULT ->
                    putAll(eventFields(draft, date ?: today.toString()))
                ManualRecordKind.VITAL_SIGN, ManualRecordKind.SOCIAL_HISTORY, ManualRecordKind.PREGNANCY ->
                    putAll(measuredFields(draft, date ?: today.toString()))
                ManualRecordKind.PRACTITIONER,
                ManualRecordKind.PRACTITIONER_ROLE,
                ManualRecordKind.ORGANIZATION,
                ManualRecordKind.LOCATION,
                ManualRecordKind.PATIENT,
                -> putAll(directoryFields(draft))
            }
        }
    }

    /**
     * A visit, a procedure or a lab result. Each carries what FHIR requires of it: a status, a
     * visit's class, and a lab result's laboratory category, which is also how Health Connect
     * files it. A doctor or clinic is a reference to its own record, with its name beside it.
     */
    private fun eventFields(draft: ManualRecordDraft, date: String): Map<String, JsonElement> = buildMap {
        val reason = draft.detail.trim().takeIf { it.isNotEmpty() }?.let { buildJsonArray { add(text(it)) } }
        val doctor = draft.doctor?.let(::reference)
        val organization = draft.organization?.let(::reference)
        val place = draft.place?.let(::reference)
        put("status", JsonPrimitive(draft.status))
        when (draft.kind) {
            ManualRecordKind.VISIT -> {
                put("class", buildJsonObject { put("system", ActCodeSystem); put("code", draft.setting) })
                put("type", buildJsonArray { add(concept(draft)) })
                put("subject", PatientReference)
                put("period", buildJsonObject { put("start", date) })
                reason?.let { put("reasonCode", it) }
                doctor?.let { put("participant", buildJsonArray { add(buildJsonObject { put("individual", it) }) }) }
                organization?.let { put("serviceProvider", it) }
                place?.let { put("location", buildJsonArray { add(buildJsonObject { put("location", it) }) }) }
            }
            ManualRecordKind.PROCEDURE -> {
                put("code", concept(draft))
                put("subject", PatientReference)
                put("performedDateTime", JsonPrimitive(date))
                reason?.let { put("reasonCode", it) }
                // The doctor acts for the clinic; a clinic alone is the one that acted.
                val performer = when {
                    doctor != null -> buildJsonObject {
                        put("actor", doctor)
                        organization?.let { put("onBehalfOf", it) }
                    }
                    organization != null -> buildJsonObject { put("actor", organization) }
                    else -> null
                }
                performer?.let { put("performer", buildJsonArray { add(it) }) }
                place?.let { put("location", it) }
            }
            else -> {
                put("category", buildJsonArray { add(coding(FhirResourceTypes.ObservationCategorySystem, "laboratory")) })
                put("code", concept(draft))
                put("subject", PatientReference)
                put("effectiveDateTime", JsonPrimitive(date))
                labValue(draft)?.let { (key, value) -> put(key, value) }
                draft.interpretation?.let { put("interpretation", buildJsonArray { add(coding(InterpretationSystem, it)) }) }
                draft.referenceRange.trim().takeIf { it.isNotEmpty() }?.let { put("referenceRange", buildJsonArray { add(text(it)) }) }
                organization?.let { put("performer", buildJsonArray { add(it) }) }
            }
        }
    }

    /**
     * A vital sign, social history or pregnancy record: the measure's LOINC code, which files it,
     * the category beside it, and the value as its measure takes it. A pregnancy record carries no
     * category: the platform files it by its code alone.
     */
    private fun measuredFields(draft: ManualRecordDraft, date: String): Map<String, JsonElement> = buildMap {
        val measure = requireNotNull(draft.measure) { "A ${draft.kind} record needs a measure" }
        put("status", JsonPrimitive(draft.status))
        when (draft.kind) {
            ManualRecordKind.VITAL_SIGN -> "vital-signs"
            ManualRecordKind.SOCIAL_HISTORY -> "social-history"
            else -> null
        }?.let { category -> put("category", buildJsonArray { add(coding(FhirResourceTypes.ObservationCategorySystem, category)) }) }
        put(
            "code",
            buildJsonObject {
                measure.loinc?.let { loinc -> put("coding", buildJsonArray { add(loincCoding(loinc, measure.display)) }) }
                put("text", draft.name.trim().ifEmpty { measure.display })
            },
        )
        put("subject", PatientReference)
        put("effectiveDateTime", JsonPrimitive(date))
        val result = draft.result.trim()
        when (measure.value) {
            MeasureValue.QUANTITY -> result.toDecimalOrNull()?.let { put("valueQuantity", quantity(it, measure.unitFor(draft.unit))) }
            MeasureValue.BLOOD_PRESSURE -> {
                val unit = measure.units.first()
                val parts = listOf(SystolicLoinc to result, DiastolicLoinc to draft.secondResult.trim()).mapNotNull { (loinc, text) ->
                    text.toDecimalOrNull()?.let { value ->
                        buildJsonObject {
                            put("code", buildJsonObject { put("coding", buildJsonArray { add(loincCoding(loinc.first, loinc.second)) }) })
                            put("valueQuantity", quantity(value, unit))
                        }
                    }
                }
                if (parts.isNotEmpty()) put("component", JsonArray(parts))
            }
            MeasureValue.INTEGER -> result.toIntOrNull()?.let { put("valueInteger", JsonPrimitive(it)) }
            MeasureValue.DATE -> draft.valueDate?.let { put("valueDateTime", JsonPrimitive(it.toString())) }
            MeasureValue.CODED -> measure.answers.firstOrNull { it.snomed == draft.answer }?.let { answer ->
                put(
                    "valueCodeableConcept",
                    buildJsonObject {
                        put(
                            "coding",
                            buildJsonArray {
                                add(buildJsonObject { put("system", Snomed); put("code", answer.snomed); put("display", answer.display) })
                            },
                        )
                        put("text", result.ifEmpty { answer.display })
                    },
                )
            }
            MeasureValue.TEXT -> result.takeIf { it.isNotEmpty() }?.let { put("valueString", JsonPrimitive(it)) }
        }
    }

    private fun quantity(value: java.math.BigDecimal, unit: MeasureUnit) = buildJsonObject {
        put("value", JsonPrimitive(value))
        put("unit", unit.symbol)
        put("system", UcumSystem)
        put("code", unit.ucum)
    }

    private fun loincCoding(code: String, display: String) = buildJsonObject {
        put("system", Loinc)
        put("code", code)
        put("display", display)
    }

    /**
     * A doctor, a doctor's role, a clinic, a place, or the owner. Each takes its name, what the
     * form adds to it, and a phone number or email address. FHIR gives a place one address and
     * the others a list.
     */
    private fun directoryFields(draft: ManualRecordDraft): Map<String, JsonElement> = buildMap {
        val name = draft.name.trim()
        val detail = draft.detail.trim().takeIf { it.isNotEmpty() }
        when (draft.kind) {
            ManualRecordKind.PRACTITIONER -> {
                put("name", buildJsonArray { add(text(name)) })
                detail?.let { put("qualification", buildJsonArray { add(buildJsonObject { put("code", text(it)) }) }) }
            }
            ManualRecordKind.PRACTITIONER_ROLE -> {
                draft.doctor?.let { put("practitioner", reference(it)) }
                draft.organization?.let { put("organization", reference(it)) }
                put("code", buildJsonArray { add(text(name)) })
                detail?.let { put("specialty", buildJsonArray { add(text(it)) }) }
                draft.place?.let { put("location", buildJsonArray { add(reference(it)) }) }
            }
            ManualRecordKind.LOCATION -> {
                put("name", JsonPrimitive(name))
                detail?.let { put("address", text(it)) }
            }
            ManualRecordKind.PATIENT -> {
                val humanName = buildJsonObject {
                    put("use", "usual")
                    name.split(Regex("\\s+")).filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let { given ->
                        put("given", JsonArray(given.map(::JsonPrimitive)))
                    }
                    draft.familyName.trim().takeIf { it.isNotEmpty() }?.let { put("family", it) }
                }
                put("name", buildJsonArray { add(humanName) })
                draft.date?.let { put("birthDate", JsonPrimitive(it.toString())) }
                draft.gender?.let { put("gender", JsonPrimitive(it)) }
                detail?.let { put("address", buildJsonArray { add(text(it)) }) }
                draft.doctor?.let { put("generalPractitioner", buildJsonArray { add(reference(it)) }) }
            }
            else -> {
                put("name", JsonPrimitive(name))
                detail?.let { put("address", buildJsonArray { add(text(it)) }) }
            }
        }
        draft.contact.trim().takeIf { it.isNotEmpty() }?.let { contact ->
            put(
                "telecom",
                buildJsonArray {
                    add(buildJsonObject { put("system", if ('@' in contact) "email" else "phone"); put("value", contact) })
                },
            )
        }
    }

    /**
     * A number, with a comparator such as `<` when the report gives a bound, is a quantity in
     * the typed unit. Anything else, such as "Negative", is kept as text.
     */
    private fun labValue(draft: ManualRecordDraft): Pair<String, JsonElement>? {
        val result = draft.result.trim().takeIf { it.isNotEmpty() } ?: return null
        val match = LabQuantity.matchEntire(result) ?: return "valueString" to JsonPrimitive(result)
        val (comparator, number) = match.destructured
        return "valueQuantity" to buildJsonObject {
            put("value", JsonPrimitive(number.replace(',', '.').toBigDecimal()))
            comparator.takeIf { it.isNotEmpty() }?.let { put("comparator", it) }
            draft.unit.trim().takeIf { it.isNotEmpty() }?.let { put("unit", it) }
        }
    }

    /** A reference to the record, with its name, or the name alone while it has no record. */
    private fun reference(reference: ManualReference): JsonObject = buildJsonObject {
        reference.id?.let { put("reference", "${reference.type}/$it") }
        put("display", reference.display.trim())
    }

    private fun coding(system: String, code: String) = buildJsonObject {
        put("coding", buildJsonArray { add(buildJsonObject { put("system", system); put("code", code) }) })
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

    private val PatientReference = buildJsonObject { put("reference", "Patient/$PatientId") }
    private val LabQuantity = Regex("""(<=|>=|<|>)?\s*(-?\d+(?:[.,]\d+)?)""")
    private const val ActCodeSystem = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
    private const val UcumSystem = "http://unitsofmeasure.org"
    private val SystolicLoinc = "8480-6" to "Systolic blood pressure"
    private val DiastolicLoinc = "8462-4" to "Diastolic blood pressure"
    private const val InterpretationSystem = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation"
    private const val AllergyClinicalSystem = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
    private const val ConditionClinicalSystem = "http://terminology.hl7.org/CodeSystem/condition-clinical"
}
