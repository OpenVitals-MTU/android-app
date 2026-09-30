package tech.mmarca.openvitals.domain.medical.cda

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.w3c.dom.Element

// The records of the standard C-CDA sections, one builder per kind. The shared builders are in CdaRecords.kt.

/** The problems of one entry: a concern act that wraps problem observations, or a bare observation. */
internal fun CdaDocumentContext.problems(entry: Element, place: String) {
    val concern = entry.child("act")
    val observations = concern?.nested() ?: listOfNotNull(entry.child("observation"))
    observations.forEachIndexed { index, observation -> problem(observation, concern, "$place.$index") }
}

private fun CdaDocumentContext.problem(observation: Element, concern: Element?, place: String) {
    if (observation.isNegated()) return
    val code = observation.child("value")?.codeableConcept(::narrative) ?: return
    val time = observation.child("effectiveTime")
    val abatement = dateTime(time?.child("high")?.attr("value"))
    // A concern that is closed, or a problem with an end, is no longer active.
    val resolved = abatement != null || concern?.child("statusCode")?.attr("code") == "completed"
    add(
        buildJsonObject {
            put("resourceType", "Condition")
            put("id", id(place, "Condition"))
            put("clinicalStatus", codeable(ConditionClinical, if (resolved) "resolved" else "active"))
            put("verificationStatus", codeable(ConditionVerification, "confirmed"))
            put("category", JsonArray(listOf(codeable(ConditionCategories, "problem-list-item", "Problem List Item"))))
            put("code", code)
            put("subject", patientRef)
            dateTime(time?.timeValue())?.let { put("onsetDateTime", it) }
            abatement?.let { put("abatementDateTime", it) }
            authorRef?.let { put("recorder", it) }
        },
    )
}

/** The allergies of one entry: a concern act that wraps allergy observations, or a bare observation. */
internal fun CdaDocumentContext.allergies(entry: Element, place: String) {
    val concern = entry.child("act")
    val observations = concern?.nested() ?: listOfNotNull(entry.child("observation"))
    observations.forEachIndexed { index, observation -> allergy(observation, concern, "$place.$index") }
}

private fun CdaDocumentContext.allergy(observation: Element, concern: Element?, place: String) {
    if (observation.isNegated()) return
    // The substance is the participant that is consumed; the observation's own value only says "allergy to a drug".
    val agent = observation.children("participant").firstOrNull { it.attr("typeCode") == "CSM" }?.at("participantRole", "playingEntity")
    val code = agent?.child("code")?.codeableConcept(::narrative) ?: agent?.child("name")?.text()?.let(::textOnly) ?: return
    val time = observation.child("effectiveTime")
    val resolved = time?.child("high")?.attr("value") != null || concern?.child("statusCode")?.attr("code") == "completed"
    val reactions = observation.children("entryRelationship").filter { it.attr("typeCode") == "MFST" }
        .mapNotNull { it.child("observation")?.child("value")?.codeableConcept(::narrative) }
    add(
        buildJsonObject {
            put("resourceType", "AllergyIntolerance")
            put("id", id(place, "AllergyIntolerance"))
            put("clinicalStatus", codeable(AllergyClinical, if (resolved) "resolved" else "active"))
            put("verificationStatus", codeable(AllergyVerification, "confirmed"))
            put("code", code)
            put("patient", patientRef)
            dateTime(time?.timeValue())?.let { put("onsetDateTime", it) }
            authorRef?.let { put("recorder", it) }
            if (reactions.isNotEmpty()) {
                put("reaction", JsonArray(reactions.map { reaction -> buildJsonObject { put("manifestation", JsonArray(listOf(reaction))) } }))
            }
        },
    )
}

/** A medication the person takes or took, with its period and dose. */
internal fun CdaDocumentContext.medication(administration: Element, place: String) {
    if (administration.isNegated()) return
    val medication = administration.drug(::narrative) ?: return
    val status = when (administration.child("statusCode")?.attr("code")) {
        "active" -> "active"
        "completed" -> "completed"
        "aborted", "cancelled" -> "stopped"
        "suspended", "held" -> "on-hold"
        else -> "unknown"
    }
    // The interval is the period of use; a second effectiveTime is the frequency.
    val period = administration.children("effectiveTime").firstOrNull { it.child("low") != null || it.child("high") != null }?.let { period(it) }
    val taken = administration.children("effectiveTime").firstNotNullOfOrNull { it.attr("value") }?.let(::dateTime)
    val sig = administration.child("text")?.let { it.text() ?: it.child("reference")?.attr("value")?.let(::narrative) }
    val route = administration.child("routeCode")?.codeableConcept(::narrative)
    val dose = administration.child("doseQuantity")?.quantity()
    add(
        buildJsonObject {
            put("resourceType", "MedicationStatement")
            put("id", id(place, "MedicationStatement"))
            put("status", status)
            put("medicationCodeableConcept", medication)
            put("subject", patientRef)
            period?.let { put("effectivePeriod", it) } ?: taken?.let { put("effectiveDateTime", it) }
            if (sig != null || route != null || dose != null) {
                val dosage = buildJsonObject {
                    sig?.let { put("text", it) }
                    route?.let { put("route", it) }
                    dose?.let { put("doseAndRate", JsonArray(listOf(buildJsonObject { put("doseQuantity", it) }))) }
                }
                put("dosage", JsonArray(listOf(dosage)))
            }
        },
    )
}

/** A vaccination given, or one recorded as not given. One with no date is left out: FHIR requires it. */
internal fun CdaDocumentContext.vaccination(administration: Element, place: String) {
    val vaccine = administration.drug(::narrative) ?: return
    val occurrence = dateTime(administration.child("effectiveTime")?.timeValue()) ?: return
    val material = administration.at("consumable", "manufacturedProduct", "manufacturedMaterial")
    add(
        buildJsonObject {
            put("resourceType", "Immunization")
            put("id", id(place, "Immunization"))
            put("status", if (administration.isNegated()) "not-done" else "completed")
            put("vaccineCode", vaccine)
            put("patient", patientRef)
            put("occurrenceDateTime", occurrence)
            material?.child("lotNumberText")?.text()?.let { put("lotNumber", it) }
            administration.child("approachSiteCode")?.codeableConcept(::narrative)?.let { put("site", it) }
            administration.child("routeCode")?.codeableConcept(::narrative)?.let { put("route", it) }
            administration.child("doseQuantity")?.quantity()?.let { put("doseQuantity", it) }
        },
    )
}

/** The observations of one entry: an organizer's components, as lab panels and vital signs come, or a bare observation. */
internal fun CdaDocumentContext.results(entry: Element, place: String, category: ObservationCategory) {
    val organizer = entry.child("organizer")
    val groupTime = dateTime(organizer?.child("effectiveTime")?.timeValue())
    val observations = organizer?.children("component")?.mapNotNull { it.child("observation") } ?: listOfNotNull(entry.child("observation"))
    observations.forEachIndexed { index, observation ->
        if (observation.isNegated()) return@forEachIndexed
        val observed = dateTime(observation.child("effectiveTime")?.timeValue()) ?: groupTime ?: time
        observationRecord(observation.child("code"), observation, observation, "$place.$index", observed, category)
    }
}

/** A procedure done, in whichever of the three shapes C-CDA allows: procedure, observation or act. */
internal fun CdaDocumentContext.procedureDone(element: Element, place: String) {
    if (element.isNegated()) return
    val code = element.child("code")?.codeableConcept(::narrative) ?: return
    val status = when (element.child("statusCode")?.attr("code")) {
        "completed" -> "completed"
        "active" -> "in-progress"
        "aborted" -> "stopped"
        "cancelled" -> "not-done"
        else -> "unknown"
    }
    add(
        procedureRecord(
            place = place,
            code = code,
            performed = dateTime(element.child("effectiveTime")?.timeValue()),
            bodySite = element.child("targetSiteCode")?.codeableConcept(::narrative),
            status = status,
        ),
    )
}

/** A visit. Its class is stated only when the document codes it as one; otherwise it is unknown. */
internal fun CdaDocumentContext.encounter(encounter: Element, place: String) {
    val code = encounter.child("code")
    val type = code?.codeableConcept(::narrative)
    val classCode = code?.takeIf { it.attr("codeSystem") == ActCodeOid }?.attr("code")
    add(
        buildJsonObject {
            put("resourceType", "Encounter")
            put("id", id(place, "Encounter"))
            put("status", "finished")
            put(
                "class",
                buildJsonObject {
                    put("system", if (classCode != null) ActCodes else NullFlavors)
                    put("code", classCode ?: "UNK")
                },
            )
            type?.let { put("type", JsonArray(listOf(it))) }
            put("subject", patientRef)
            encounter.child("effectiveTime")?.let { period(it) }?.let { put("period", it) }
        },
    )
}

/** The drug or vaccine of a substanceAdministration, named by its product name when the code has no text. */
private fun Element.drug(narrative: (String) -> String?): JsonObject? {
    val material = at("consumable", "manufacturedProduct", "manufacturedMaterial") ?: return null
    val name = material.child("name")?.text()
    val coded = material.child("code")?.codeableConcept(narrative)
    return when {
        coded != null && "text" !in coded -> coded.withText(name)
        coded != null -> coded
        else -> name?.let(::textOnly)
    }
}

/** True for an entry that records an absence, such as "no known allergies". */
private fun Element.isNegated(): Boolean = attr("negationInd") == "true"

private const val ActCodeOid = "2.16.840.1.113883.5.4"
