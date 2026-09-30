package tech.mmarca.openvitals.domain.medical.cda

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.w3c.dom.Element

// The records Estonian documents hold, one builder per kind. The shared builders are in CdaRecords.kt.

/** An outpatient visit or a phone consultation, returned so the document's other records can point at it. */
internal fun CdaDocumentContext.visit(encounter: Element, place: String): JsonObject {
    val code = encounter.child("code")
    val qualifiers = code?.children("qualifier")?.mapNotNull { it.child("value") }.orEmpty()
    val virtual = qualifiers.any { EstonianCdaCodes.isPhoneConsultation(it.attr("code"), it.attr("codeSystem")) }
    val types = (listOfNotNull(code) + qualifiers).mapNotNull { it.codeableConcept() }
    val period = encounter.child("effectiveTime")?.let { period(it) } ?: time?.let { buildJsonObject { put("start", it) } }
    val visit = buildJsonObject {
        put("resourceType", "Encounter")
        put("id", id(place, "Encounter"))
        put("status", "finished")
        put(
            "class",
            buildJsonObject {
                put("system", ActCodes)
                put("code", if (virtual) "VR" else "AMB")
                put("display", if (virtual) "virtual" else "ambulatory")
            },
        )
        if (types.isNotEmpty()) put("type", JsonArray(types))
        put("subject", patientRef)
        period?.let { put("period", it) }
        authorRef?.let { put("participant", JsonArray(listOf(buildJsonObject { put("individual", it) }))) }
        organizationRef?.let { put("serviceProvider", it) }
    }
    add(visit)
    return visit
}

/** A dental visit with the diagnoses and the work on each tooth that belong to it. */
internal fun CdaDocumentContext.dentalVisit(encounter: Element, place: String) {
    val visit = visit(encounter, place)
    val visitRef = reference("Encounter", visit.id)
    val performed = visit["period"]?.jsonObject?.get("start")?.jsonPrimitive?.content
    encounter.children("entryRelationship").forEachIndexed { index, relation ->
        relation.child("observation")?.let { diagnoses(it, "$place.$index", visitRef) }
        relation.child("procedure")?.let { procedure ->
            val code = procedure.child("code")?.codeableConcept() ?: return@let
            add(
                procedureRecord(
                    place = "$place.$index",
                    code = code,
                    performed = dateTime(procedure.child("effectiveTime")?.timeValue()) ?: performed,
                    bodySite = procedure.child("targetSiteCode")?.codeableConcept(),
                    encounterRef = visitRef,
                ),
            )
        }
    }
}

/** A coded diagnosis, or the diagnoses nested in an observation that states none itself, as pathology does. */
internal fun CdaDocumentContext.diagnoses(observation: Element, place: String, encounterRef: JsonObject? = visitRef) {
    val condition = condition(observation, place, encounterRef)
    if (condition != null) {
        add(condition)
        return
    }
    observation.nested().forEachIndexed { index, nested -> diagnoses(nested, "$place.$index", encounterRef) }
}

private fun CdaDocumentContext.condition(observation: Element, place: String, encounterRef: JsonObject?): JsonObject? {
    val value = observation.child("value")?.takeIf { it.attr("code") != null } ?: return null
    val type = observation.child("code")
    val isDiagnosis = (type?.attr("code") == EstonianCdaCodes.DiagnosisType && EstonianCdaCodes.isObservationType(type.attr("codeSystem"))) ||
        CdaCodeSystems.isIcd10List(value.attr("codeSystem"))
    if (!isDiagnosis) return null
    val code = value.codeableConcept() ?: return null
    val kind = value.at("qualifier", "value")
    val provisional = isReferral || EstonianCdaCodes.isProvisional(kind?.attr("code"), kind?.attr("codeSystem"))
    val site = observation.child("targetSiteCode")?.codeableConcept()
    val notes = listOfNotNull(title.takeIf { isReferral }) + observation.nested().mapNotNull { it.child("text")?.text() }
    return buildJsonObject {
        put("resourceType", "Condition")
        put("id", id(place, "Condition"))
        put("verificationStatus", codeable(ConditionVerification, if (provisional) "provisional" else "confirmed"))
        put("category", JsonArray(listOf(codeable(ConditionCategories, "encounter-diagnosis", "Encounter Diagnosis"))))
        put("code", code)
        site?.let { put("bodySite", JsonArray(listOf(it))) }
        put("subject", patientRef)
        encounterRef?.let { put("encounter", it) }
        (dateTime(observation.child("effectiveTime")?.timeValue()) ?: time)?.let { put("recordedDate", it) }
        authorRef?.let { put("recorder", it) }
        putNotes(notes)
    }
}

/** A prescription. Whether the drug is still taken is not in the document, so the status is unknown. */
internal fun CdaDocumentContext.prescription(administration: Element, place: String) {
    val material = administration.at("consumable", "manufacturedProduct", "manufacturedMaterial") ?: return
    val name = material.child("name")?.text()
    val medication = material.child("code")?.codeableConcept()?.withText(name) ?: name?.let(::textOnly) ?: return
    val dose = administration.child("doseQuantity")
    val form = administration.child("administrationUnitCode")?.let { it.attr("displayName") ?: it.child("originalText")?.text() }
    val dosageText = listOfNotNull(dose?.quantityText(), administration.child("rateQuantity")?.quantityText(), form)
        .joinToString(" · ").ifEmpty { null }
    val doseQuantity = dose?.quantity()
    add(
        buildJsonObject {
            put("resourceType", "MedicationRequest")
            put("id", id(place, "MedicationRequest"))
            put("status", "unknown")
            put("intent", "order")
            put("medicationCodeableConcept", medication)
            put("subject", patientRef)
            visitRef?.let { put("encounter", it) }
            (dateTime(administration.child("effectiveTime")?.timeValue()) ?: time)?.let { put("authoredOn", it) }
            authorRef?.let { put("requester", it) }
            if (dosageText != null || doseQuantity != null) {
                val dosage = buildJsonObject {
                    dosageText?.let { put("text", it) }
                    doseQuantity?.let { put("doseAndRate", JsonArray(listOf(buildJsonObject { put("doseQuantity", it) }))) }
                }
                put("dosageInstruction", JsonArray(listOf(dosage)))
            }
        },
    )
}

/** A vaccination: the vaccine, its lot, the dose, and the disease it protects against. */
internal fun CdaDocumentContext.immunization(procedure: Element, place: String) {
    val relation = procedure.children("entryRelationship").firstOrNull { it.child("substanceAdministration") != null } ?: return
    val administration = relation.child("substanceAdministration") ?: return
    val material = administration.at("consumable", "manufacturedProduct", "manufacturedMaterial") ?: return
    val name = material.child("name")?.text()
    val vaccine = material.child("code")?.codeableConcept()?.withText(name) ?: name?.let(::textOnly) ?: return
    val occurrence = dateTime(procedure.child("effectiveTime")?.timeValue() ?: administration.child("effectiveTime")?.timeValue())
        ?: time ?: return
    val doseNumber = relation.child("sequenceNumber")?.attr("value")?.toIntOrNull()?.takeIf { it > 0 }
    val target = procedure.child("methodCode")?.codeableConcept()
    add(
        buildJsonObject {
            put("resourceType", "Immunization")
            put("id", id(place, "Immunization"))
            put("status", "completed")
            put("vaccineCode", vaccine)
            put("patient", patientRef)
            put("occurrenceDateTime", occurrence)
            material.child("lotNumberText")?.text()?.let { put("lotNumber", it) }
            administration.child("doseQuantity")?.quantity()?.let { put("doseQuantity", it) }
            // FHIR requires the dose number wherever the target disease is given.
            doseNumber?.let { number ->
                val applied = buildJsonObject {
                    target?.let { put("targetDisease", JsonArray(listOf(it))) }
                    put("doseNumberPositiveInt", number)
                }
                put("protocolApplied", JsonArray(listOf(applied)))
            }
        },
    )
}

/**
 * The results of a lab panel, and of the panels inside it. A test's result is on the test
 * itself or on an observation nested in it. A procedure with no results but a written
 * conclusion, as in pathology, becomes a procedure with that text.
 */
internal fun CdaDocumentContext.labPanel(procedure: Element, place: String, inheritedTime: String?) {
    val specimenTime = procedure.at("specimen", "productOf", "process", "effectiveTime")?.timeValue()
    val panelTime = dateTime(specimenTime ?: procedure.child("effectiveTime")?.timeValue()) ?: inheritedTime ?: time
    val before = size
    procedure.children("entryRelationship").forEachIndexed { index, relation ->
        val here = "$place.$index"
        relation.child("observation")?.let { test ->
            val holder = if (test.hasResult()) test else test.nested().firstOrNull { it.hasResult() } ?: test
            // An observation coded only by its role, such as ANA, holds the result of the procedure itself.
            val code = if (EstonianCdaCodes.isObservationType(test.child("code")?.attr("codeSystem"))) procedure.child("code") else test.child("code")
            observationRecord(code, test, holder, here, panelTime)
        }
        relation.child("procedure")?.let { labPanel(it, here, panelTime) }
    }
    if (size == before) {
        val code = procedure.child("code")?.codeableConcept() ?: return
        val notes = listOfNotNull(procedure.child("text")?.text())
        if (notes.isNotEmpty()) add(procedureRecord(place, code, panelTime, notes = notes, encounterRef = visitRef))
    }
}

/**
 * The studies in a "procedures done" section. A study is a procedure with its description,
 * and an observation with its finding under the same price list code: they become one record.
 * A finding with no procedure is a study of its own.
 */
internal fun CdaDocumentContext.studies(entries: List<Element>, place: String) {
    val findings = entries.mapNotNull { it.child("observation") }
    val matched = mutableSetOf<Element>()
    entries.forEachIndexed { index, entry ->
        val procedure = entry.child("procedure") ?: return@forEachIndexed
        if (procedure.children("entryRelationship").any { it.child("observation") != null || it.child("procedure") != null }) {
            // A study with measured results reads like a lab panel.
            labPanel(procedure, "$place.$index", inheritedTime = null)
            return@forEachIndexed
        }
        val code = procedure.child("code")?.codeableConcept() ?: return@forEachIndexed
        val codes = procedure.child("code")?.codes().orEmpty()
        val own = findings.filter { finding -> finding.child("code")?.codes().orEmpty().any { it in codes } }
        matched += own
        val notes = listOfNotNull(procedure.child("text")?.text()) + own.mapNotNull { it.child("text")?.text() }
        val performed = dateTime(procedure.child("effectiveTime")?.timeValue()) ?: time
        add(procedureRecord("$place.$index", code, performed, notes = notes, encounterRef = visitRef))
    }
    entries.forEachIndexed { index, entry ->
        entry.child("observation")?.takeIf { it !in matched }?.let { imaging(it, "$place.$index") }
    }
}

/** The codes of a CD and its translations, which is how a study's procedure and finding name each other. */
private fun Element.codes(): Set<String> = (listOf(this) + children("translation")).mapNotNull { it.attr("code") }.toSet()

/** A radiology study, with its findings and conclusion as notes. */
internal fun CdaDocumentContext.imaging(observation: Element, place: String) {
    val code = observation.child("code")?.codeableConcept() ?: observation.child("methodCode")?.codeableConcept() ?: return
    val notes = listOfNotNull(observation.child("text")?.text()) + observation.nested().mapNotNull { it.child("text")?.text() ?: it.child("value")?.text() }
    add(
        procedureRecord(
            place = place,
            code = code,
            performed = dateTime(observation.child("effectiveTime")?.timeValue()) ?: time,
            bodySite = observation.child("targetSiteCode")?.codeableConcept(),
            category = observation.child("methodCode")?.codeableConcept(),
            notes = notes,
            encounterRef = visitRef,
        ),
    )
}
