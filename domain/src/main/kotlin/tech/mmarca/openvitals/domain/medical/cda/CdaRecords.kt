package tech.mmarca.openvitals.domain.medical.cda

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.w3c.dom.Element

// Record builders every CDA mapper shares. Each leaves out a value it does not have: Health Connect refuses an empty one.

internal const val ActCodes = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
internal const val NullFlavors = "http://terminology.hl7.org/CodeSystem/v3-NullFlavor"
internal const val ConditionCategories = "http://terminology.hl7.org/CodeSystem/condition-category"
internal const val ConditionClinical = "http://terminology.hl7.org/CodeSystem/condition-clinical"
internal const val ConditionVerification = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
internal const val AllergyClinical = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
internal const val AllergyVerification = "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification"
private const val ObservationCategories = "http://terminology.hl7.org/CodeSystem/observation-category"
private const val Interpretations = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation"

/** The Observation categories Health Connect sorts records by. */
internal enum class ObservationCategory(val code: String, val display: String) {
    LABORATORY("laboratory", "Laboratory"),
    VITAL_SIGNS("vital-signs", "Vital Signs"),
    SOCIAL_HISTORY("social-history", "Social History"),
}

/**
 * One observation: a lab result, a vital sign, or a social history item. [test] carries the
 * code's context and the reference ranges; [holder] carries the value, and is [test] itself
 * unless the value sits on a nested observation. Nothing is made without a value or a text.
 */
internal fun CdaDocumentContext.observationRecord(
    codeElement: Element?,
    test: Element,
    holder: Element,
    place: String,
    time: String?,
    category: ObservationCategory = ObservationCategory.LABORATORY,
) {
    val code = codeElement?.codeableConcept(::narrative) ?: return
    val holderText = holder.child("text")?.text()
    val interpretationElement = holder.child("interpretationCode") ?: test.child("interpretationCode")
    val interpretation = interpretationElement?.interpretation()
    val interpretationText = interpretationElement?.child("originalText")?.text()?.takeIf { interpretation == null }
    val ranges = (test.children("referenceRange") + if (holder !== test) holder.children("referenceRange") else emptyList())
        .mapNotNull(::referenceRange)
    var hasResult = false
    val result = buildJsonObject {
        put("resourceType", "Observation")
        put("id", id(place, "Observation"))
        put("status", "final")
        put("category", JsonArray(listOf(codeable(ObservationCategories, category.code, category.display))))
        put("code", code)
        put("subject", patientRef)
        visitRef?.let { put("encounter", it) }
        time?.let { put("effectiveDateTime", it) }
        val valued = holder.child("value")?.let { putValue(it) } ?: false
        if (!valued) holderText?.let { put("valueString", it) }
        hasResult = valued || holderText != null
        interpretation?.let { put("interpretation", JsonArray(listOf(it))) }
        if (ranges.isNotEmpty()) put("referenceRange", JsonArray(ranges))
        putNotes(listOfNotNull(holderText?.takeIf { valued }, interpretationText))
    }
    if (hasResult) add(result)
}

internal fun CdaDocumentContext.procedureRecord(
    place: String,
    code: JsonObject,
    performed: String?,
    bodySite: JsonObject? = null,
    category: JsonObject? = null,
    notes: List<String> = emptyList(),
    encounterRef: JsonObject? = null,
    status: String = "completed",
): JsonObject = buildJsonObject {
    put("resourceType", "Procedure")
    put("id", id(place, "Procedure"))
    put("status", status)
    category?.let { put("category", it) }
    put("code", code)
    put("subject", patientRef)
    encounterRef?.let { put("encounter", it) }
    performed?.let { put("performedDateTime", it) }
    bodySite?.let { put("bodySite", JsonArray(listOf(it))) }
    putNotes(notes)
}

/** The start and end an IVL_TS or a TS states, as a FHIR Period. */
internal fun CdaDocumentContext.period(effectiveTime: Element): JsonObject? {
    val start = dateTime(effectiveTime.timeValue())
    val end = dateTime(effectiveTime.child("high")?.attr("value"))
    if (start == null && end == null) return null
    return buildJsonObject {
        start?.let { put("start", it) }
        end?.let { put("end", it) }
    }
}

/** The observations nested in this one. */
internal fun Element.nested(): List<Element> = children("entryRelationship").mapNotNull { it.child("observation") }

internal fun Element.hasResult(): Boolean = child("value") != null || child("text")?.text() != null

internal fun JsonObjectBuilder.putNotes(notes: List<String>) {
    if (notes.isNotEmpty()) put("note", JsonArray(notes.map { text -> buildJsonObject { put("text", text) } }))
}

internal fun textOnly(text: String): JsonObject = buildJsonObject { put("text", text) }

/** A coded interpretation such as H or L. Free text alone is not one: it becomes a note. */
private fun Element.interpretation(): JsonObject? {
    val code = attr("code") ?: return null
    val system = attr("codeSystem")?.let(CdaCodeSystems::systemUri) ?: Interpretations
    val display = attr("displayName") ?: InterpretationNames[code].takeIf { system == Interpretations }
    return codeable(system, code, display).withText(child("originalText")?.text())
}

/** The standard names of the interpretation codes the documents use, as HL7 gives them. */
private val InterpretationNames = mapOf(
    "H" to "High", "HH" to "Critical high", "L" to "Low", "LL" to "Critical low",
    "N" to "Normal", "A" to "Abnormal", "POS" to "Positive", "NEG" to "Negative",
)

private fun referenceRange(range: Element): JsonObject? {
    val observationRange = range.child("observationRange") ?: return null
    val value = observationRange.child("value")
    val low = value?.child("low")?.quantity(value.attr("unit"))
    val high = value?.child("high")?.quantity(value.attr("unit"))
    val text = observationRange.child("text")?.text() ?: value?.takeIf { it.xsiType() in TextTypes }?.text()
    if (low == null && high == null && text == null) return null
    return buildJsonObject {
        low?.let { put("low", it) }
        high?.let { put("high", it) }
        text?.let { put("text", it) }
    }
}

private val TextTypes = setOf("ED", "ST")

/** Puts a result as the matching FHIR value type. False when there is nothing to put. */
private fun JsonObjectBuilder.putValue(value: Element): Boolean {
    val (key, element) = when (value.xsiType()) {
        "PQ", "REAL" -> "valueQuantity" to value.quantity()
        "IVL_PQ" -> "valueRange" to value.range()
        "CD", "CE", "CV", "CO" -> "valueCodeableConcept" to value.codeableConcept()
        "INT" -> "valueInteger" to value.attr("value")?.toIntOrNull()?.let(::JsonPrimitive)
        "BL" -> "valueBoolean" to value.attr("value")?.toBooleanStrictOrNull()?.let(::JsonPrimitive)
        else -> "valueString" to value.text()?.let(::JsonPrimitive)
    }
    element ?: return false
    put(key, element)
    return true
}
