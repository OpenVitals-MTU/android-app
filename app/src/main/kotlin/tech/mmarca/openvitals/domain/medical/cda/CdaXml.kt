package tech.mmarca.openvitals.domain.medical.cda

import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

// Reads HL7 CDA R2: a safe parse, lookups by local name, and the data types FHIR needs.

internal const val Hl7Namespace = "urn:hl7-org:v3"
private const val XsiNamespace = "http://www.w3.org/2001/XMLSchema-instance"

/** The root element, or null when the bytes are not well-formed XML. DTDs and external entities stay off. */
internal fun parseCda(bytes: ByteArray): Element? = runCatching {
    val builder = cdaDocumentBuilderFactory().newDocumentBuilder()
    builder.setErrorHandler(QuietErrors)
    builder.parse(ByteArrayInputStream(bytes)).documentElement
}.getOrNull()

private fun cdaDocumentBuilderFactory(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature("http://xml.org/sax/features/external-general-entities", false)
        trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
        trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }

private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
    runCatching { setFeature(name, value) }
}

/** A broken document fails the parse without printing to the log. */
private object QuietErrors : ErrorHandler {
    override fun warning(exception: SAXParseException) = Unit
    override fun error(exception: SAXParseException) = Unit
    override fun fatalError(exception: SAXParseException) = throw exception
}

/** Child elements with this local name, in any namespace: the Estonian extension reuses CDA's names. */
internal fun Element.children(name: String): List<Element> = buildList {
    var node = firstChild
    while (node != null) {
        if (node is Element && node.localName == name) add(node)
        node = node.nextSibling
    }
}

internal fun Element.child(name: String): Element? = children(name).firstOrNull()

/** The element down a chain of local names, taking the first match at each step. */
internal fun Element.at(vararg names: String): Element? = names.fold(this as Element?) { element, name -> element?.child(name) }

internal fun Element.attr(name: String): String? = getAttribute(name).plainSpaces()

/** The data type, without the namespace prefix some senders add, as in `ns3:PQ`. */
internal fun Element.xsiType(): String? = getAttributeNS(XsiNamespace, "type").substringAfter(':').trim().takeIf { it.isNotEmpty() }

internal fun Element.text(): String? = textContent?.plainSpaces()

/**
 * Runs of any space as one plain space, trimmed. Estonian price list names use the no-break
 * space, and Health Connect refuses it in a FHIR string such as a code's text.
 */
private fun String.plainSpaces(): String? = replace(AnySpace, " ").trim().takeIf { it.isNotEmpty() }

private val AnySpace = Regex("[\\s\\p{Z}]+")

/**
 * A CD with its translations as a FHIR CodeableConcept. Null when it names nothing.
 * [narrative] finds the text an `originalText` points at with `<reference value="#id"/>`.
 */
internal fun Element.codeableConcept(narrative: (String) -> String? = { null }): JsonObject? {
    val codings = (listOf(this) + children("translation")).mapNotNull { it.coding() }
    val original = child("originalText")
    val text = original?.text() ?: original?.child("reference")?.attr("value")?.let(narrative) ?: attr("displayName")
    if (codings.isEmpty() && text == null) return null
    return buildJsonObject {
        if (codings.isNotEmpty()) put("coding", JsonArray(codings))
        text?.let { put("text", it) }
    }
}

private fun Element.coding(): JsonObject? {
    val code = attr("code") ?: return null
    return buildJsonObject {
        attr("codeSystem")?.let(CdaCodeSystems::systemUri)?.let { put("system", it) }
        put("code", code)
        attr("displayName")?.let { put("display", it) }
    }
}

/** A CodeableConcept from one known coding. */
internal fun codeable(system: String, code: String, display: String? = null): JsonObject = buildJsonObject {
    put(
        "coding",
        JsonArray(
            listOf(
                buildJsonObject {
                    put("system", system)
                    put("code", code)
                    display?.let { put("display", it) }
                },
            ),
        ),
    )
}

/** This CodeableConcept with [text] as its text, when there is one. */
internal fun JsonObject.withText(text: String?): JsonObject =
    if (text == null) this else JsonObject(LinkedHashMap(this).apply { put("text", JsonPrimitive(text)) })

internal fun reference(type: String, id: String): JsonObject = buildJsonObject { put("reference", "$type/$id") }

private val TimeShape = Regex("""(\d{4})(\d{2})?(\d{2})?(\d{2})?(\d{2})?(\d{2})?(?:\.\d+)?([+-]\d{4})?""")
/**
 * A CDA TS as a FHIR date or dateTime. FHIR needs a zone on a time. A time without one takes
 * [localZone]'s offset for that moment, or keeps only its date when the zone is not known.
 * A date alone stays a date.
 */
internal fun fhirDateTime(ts: String?, localZone: ZoneId?): String? {
    val match = ts?.trim()?.let(TimeShape::matchEntire) ?: return null
    val (year, month, day, hour, minute, second, zone) = match.destructured
    return runCatching {
        when {
            month.isEmpty() -> year
            day.isEmpty() -> "$year-$month".also { LocalDate.of(year.toInt(), month.toInt(), 1) }
            hour.isEmpty() -> LocalDate.of(year.toInt(), month.toInt(), day.toInt()).toString()
            else -> {
                val local = LocalDateTime.of(
                    year.toInt(), month.toInt(), day.toInt(), hour.toInt(),
                    minute.ifEmpty { "0" }.toInt(), second.ifEmpty { "0" }.toInt(),
                )
                val offset = if (zone.isNotEmpty()) ZoneOffset.of(zone) else localZone?.rules?.getOffset(local)
                offset?.let { local.atOffset(it).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) } ?: local.toLocalDate().toString()
            }
        }
    }.getOrNull()
}

/** The date part alone, for a FHIR `date` such as a birth date. */
internal fun fhirDate(ts: String?): String? = fhirDateTime(ts, localZone = null)?.take(DateLength)

private const val DateLength = 10

/** The time an element states: its own value, else the start of its interval. */
internal fun Element.timeValue(): String? = attr("value") ?: child("low")?.attr("value") ?: child("center")?.attr("value")

/** A PQ as a FHIR Quantity, or null without a number. The unit stays as written: it is rarely UCUM. */
@OptIn(ExperimentalSerializationApi::class)
internal fun Element.quantity(fallbackUnit: String? = null): JsonObject? {
    val value = decimal(attr("value")) ?: return null
    return buildJsonObject {
        put("value", JsonUnquotedLiteral(value.toPlainString()))
        (attr("unit") ?: fallbackUnit)?.takeUnless { it == UnityUnit }?.let { put("unit", it) }
    }
}

/** An IVL_PQ as a FHIR Range: `< 5` has a high and no low. */
internal fun Element.range(): JsonObject? {
    val low = child("low")?.quantity(attr("unit"))
    val high = child("high")?.quantity(attr("unit"))
    if (low == null && high == null) return null
    return buildJsonObject {
        low?.let { put("low", it) }
        high?.let { put("high", it) }
    }
}

/** A quantity as a person reads it, such as "1 TA". */
internal fun Element.quantityText(): String? {
    val value = decimal(attr("value")) ?: return null
    return listOfNotNull(value.toPlainString(), attr("unit")?.takeUnless { it == UnityUnit }).joinToString(" ")
}

private fun decimal(text: String?): BigDecimal? = text?.replace(',', '.')?.toBigDecimalOrNull()

/** UCUM's "1": a number with no unit. */
private const val UnityUnit = "1"

/** A PN as a FHIR HumanName. A name given only as text keeps that text. */
internal fun Element.humanName(): JsonObject? {
    val given = children("given").mapNotNull { it.text() }
    val family = children("family").mapNotNull { it.text() }.joinToString(" ").ifBlank { null }
    val text = if (given.isEmpty() && family == null) text() else null
    if (given.isEmpty() && family == null && text == null) return null
    return buildJsonObject {
        family?.let { put("family", it) }
        if (given.isNotEmpty()) put("given", JsonArray(given.map(::JsonPrimitive)))
        text?.let { put("text", it) }
    }
}
