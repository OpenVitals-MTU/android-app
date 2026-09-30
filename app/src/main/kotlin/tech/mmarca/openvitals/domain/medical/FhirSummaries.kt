package tech.mmarca.openvitals.domain.medical

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A FHIR date or dateTime, shown as written. A partial date such as "2020" sorts as its first day. */
data class FhirDate(val text: String) {
    /** `YYYY-MM-DD`, then the time when there is one. Compares as a string. The zone is ignored. */
    val sortKey: String = run {
        val date = text.take(10)
        val padded = when (date.length) {
            4 -> "$date-01-01"
            7 -> "$date-01"
            else -> date
        }
        val time = text.drop(10).removePrefix("T").take(8)
        if (time.isEmpty()) padded else "${padded}T$time"
    }
}

/** A value to show. [Text.caption] names the code system when only a bare code was available. */
sealed interface SummaryValue {
    data class Text(val text: String, val caption: String? = null) : SummaryValue

    /** Resolved to the referenced record's name on screen when it is readable. */
    data class Reference(val reference: String?, val display: String?) : SummaryValue
}

/** The detail rows a record can have. The screen gives each a label. */
enum class SummaryField {
    VERIFICATION, CATEGORY, CRITICALITY, SEVERITY, REACTION, ONSET, RECORDED, LOT_NUMBER, SITE, ROUTE,
    DOSE_NUMBER, PERFORMER, DOSAGE, REQUESTER, REASON, INTERPRETATION, REFERENCE_RANGE, COMPONENT, NOTE,
    OUTCOME, BODY_SITE, PARTICIPANT, LOCATION, SERVICE_PROVIDER, PERIOD, BIRTH_DATE, GENDER, SPECIALTY, TYPE,
    CONTACT,
}

/** [label] names a row that repeats, such as each part of a blood pressure reading. */
data class SummaryDetail(val field: SummaryField, val value: SummaryValue, val label: String? = null)

/**
 * What a record shows in a list and on its detail screen. Every part is optional: a record
 * with none still shows as untitled, with its type and its raw JSON. [status] is the FHIR
 * code, such as "entered-in-error". [flag] is the interpretation the source set. Nothing
 * here is computed from the values.
 */
data class MedicalRecordSummary(
    val title: SummaryValue?,
    val date: FhirDate?,
    val status: String?,
    val value: String?,
    val flag: String?,
    val details: List<SummaryDetail>,
) {
    val titleText: String? get() = when (title) {
        is SummaryValue.Text -> title.text
        is SummaryValue.Reference -> title.display
        null -> null
    }
}

/** Newest first, by each type's own date. Records with no date come last, by title. */
val MedicalRecordOrder: Comparator<MedicalRecordSummary> =
    compareBy<MedicalRecordSummary> { it.date == null }
        .thenByDescending { it.date?.sortKey }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.titleText.orEmpty() }

/** Picks the fields each resource type shows. See the proposal's "FHIR handling" table. */
object FhirSummaries {
    private val Empty = MedicalRecordSummary(null, null, null, null, null, emptyList())

    /** A record as Health Connect stores it. JSON that does not parse shows as untitled. */
    fun summarize(json: String): MedicalRecordSummary =
        runCatching { Json.parseToJsonElement(json) as? JsonObject }.getOrNull()?.let(::summarize) ?: Empty

    fun summarize(json: JsonObject): MedicalRecordSummary = when (json.resourceType) {
        "Immunization" -> MedicalRecordSummary(
            title = json.coded("vaccineCode"),
            date = json.date("occurrenceDateTime"),
            status = json.string("status"),
            value = null,
            flag = null,
            details = listOfNotNull(
                json.string("lotNumber")?.let { detail(SummaryField.LOT_NUMBER, it) },
                json.coded("site")?.let { SummaryDetail(SummaryField.SITE, it) },
                json.coded("route")?.let { SummaryDetail(SummaryField.ROUTE, it) },
                json.objects("protocolApplied").firstNotNullOfOrNull { it.literal("doseNumberPositiveInt") ?: it.string("doseNumberString") }
                    ?.let { detail(SummaryField.DOSE_NUMBER, it) },
            ) + json.objects("performer").mapNotNull { it.reference("actor") }.map { SummaryDetail(SummaryField.PERFORMER, it) },
        )
        "AllergyIntolerance", "Condition" -> clinical(json)
        "MedicationRequest", "MedicationStatement" -> medication(json)
        "Medication" -> MedicalRecordSummary(
            title = json.coded("code"),
            date = null,
            status = json.string("status"),
            value = null,
            flag = null,
            details = listOfNotNull(json.coded("form")?.let { SummaryDetail(SummaryField.TYPE, it) }),
        )
        "Observation" -> observation(json)
        "Procedure" -> MedicalRecordSummary(
            title = json.coded("code"),
            date = json.date("performedDateTime") ?: json.obj("performedPeriod")?.date("start"),
            status = json.string("status"),
            value = null,
            flag = null,
            details = json.codedList("bodySite").map { SummaryDetail(SummaryField.BODY_SITE, it) } +
                json.objects("performer").mapNotNull { it.reference("actor") }.map { SummaryDetail(SummaryField.PERFORMER, it) } +
                json.codedList("reasonCode").map { SummaryDetail(SummaryField.REASON, it) } +
                listOfNotNull(json.coded("outcome")?.let { SummaryDetail(SummaryField.OUTCOME, it) }) +
                json.objects("note").mapNotNull { it.string("text") }.map { detail(SummaryField.NOTE, it) },
        )
        "Encounter" -> MedicalRecordSummary(
            title = json.codedList("type").firstOrNull() ?: json.obj("class")?.coding(),
            date = json.obj("period")?.date("start"),
            status = json.string("status"),
            value = null,
            flag = null,
            details = listOfNotNull(
                json.obj("period")?.period()?.let { detail(SummaryField.PERIOD, it) },
                json.reference("serviceProvider")?.let { SummaryDetail(SummaryField.SERVICE_PROVIDER, it) },
            ) + json.objects("participant").mapNotNull { it.reference("individual") }.map { SummaryDetail(SummaryField.PARTICIPANT, it) } +
                json.codedList("reasonCode").map { SummaryDetail(SummaryField.REASON, it) } +
                json.objects("location").mapNotNull { it.reference("location") }.map { SummaryDetail(SummaryField.LOCATION, it) },
        )
        "Patient" -> MedicalRecordSummary(
            title = json.humanName()?.let { SummaryValue.Text(it) },
            date = null,
            status = null,
            value = null,
            flag = null,
            details = listOfNotNull(
                json.string("birthDate")?.let { detail(SummaryField.BIRTH_DATE, it) },
                json.string("gender")?.let { detail(SummaryField.GENDER, it) },
            ),
        )
        "Practitioner", "PractitionerRole", "Organization", "Location" -> directory(json)
        else -> Empty
    }

    private fun clinical(json: JsonObject): MedicalRecordSummary {
        val clinical = json.obj("clinicalStatus")?.firstCode()
        val verification = json.obj("verificationStatus")?.firstCode()
        val onset = json.date("onsetDateTime") ?: json.obj("onsetPeriod")?.date("start")
        val recorded = json.date("recordedDate")
        return MedicalRecordSummary(
            title = json.coded("code"),
            date = onset ?: recorded,
            // A record marked wrong or refuted says so first.
            status = verification?.takeIf { it in setOf("entered-in-error", "refuted") } ?: clinical,
            value = null,
            flag = null,
            details = listOfNotNull(
                verification?.let { detail(SummaryField.VERIFICATION, it) },
                json.string("criticality")?.let { detail(SummaryField.CRITICALITY, it) },
                json.coded("severity")?.let { SummaryDetail(SummaryField.SEVERITY, it) },
                onset?.let { detail(SummaryField.ONSET, it.text) } ?: json.string("onsetString")?.let { detail(SummaryField.ONSET, it) },
                recorded?.let { detail(SummaryField.RECORDED, it.text) },
            ) + json.strings("category").map { detail(SummaryField.CATEGORY, it) } +
                json.objects("reaction").flatMap { it.codedList("manifestation") }.map { SummaryDetail(SummaryField.REACTION, it) } +
                json.codedList("bodySite").map { SummaryDetail(SummaryField.BODY_SITE, it) } +
                json.objects("note").mapNotNull { it.string("text") }.map { detail(SummaryField.NOTE, it) },
        )
    }

    private fun medication(json: JsonObject): MedicalRecordSummary {
        val dosage = json.objects("dosage") + json.objects("dosageInstruction")
        return MedicalRecordSummary(
            title = json.coded("medicationCodeableConcept") ?: json.reference("medicationReference"),
            date = json.date("authoredOn") ?: json.date("effectiveDateTime") ?: json.obj("effectivePeriod")?.date("start")
                ?: json.date("dateAsserted"),
            status = json.string("status"),
            value = null,
            flag = null,
            details = dosage.mapNotNull { it.string("text") }.map { detail(SummaryField.DOSAGE, it) } +
                listOfNotNull(json.reference("requester")?.let { SummaryDetail(SummaryField.REQUESTER, it) }) +
                json.codedList("reasonCode").map { SummaryDetail(SummaryField.REASON, it) } +
                json.objects("reasonReference").map { SummaryDetail(SummaryField.REASON, it.asReference()) },
        )
    }

    private fun observation(json: JsonObject): MedicalRecordSummary {
        val components = json.objects("component").mapNotNull { component ->
            component.value()?.let { value -> SummaryDetail(SummaryField.COMPONENT, SummaryValue.Text(value), component.obj("code")?.codeableLabel()) }
        }
        val flag = json.codedList("interpretation").firstOrNull()
        return MedicalRecordSummary(
            title = json.coded("code"),
            date = json.date("effectiveDateTime") ?: json.obj("effectivePeriod")?.date("start") ?: json.date("effectiveInstant")
                ?: json.date("issued"),
            status = json.string("status"),
            value = json.value() ?: components.takeIf { it.isNotEmpty() }?.joinToString(" / ") { (it.value as SummaryValue.Text).text },
            flag = (flag as? SummaryValue.Text)?.text,
            details = listOfNotNull(flag?.let { SummaryDetail(SummaryField.INTERPRETATION, it) }) +
                json.objects("referenceRange").mapNotNull { it.range() }.map { detail(SummaryField.REFERENCE_RANGE, it) } +
                components +
                json.objects("performer").map { SummaryDetail(SummaryField.PERFORMER, it.asReference()) } +
                json.objects("note").mapNotNull { it.string("text") }.map { detail(SummaryField.NOTE, it) },
        )
    }

    private fun directory(json: JsonObject): MedicalRecordSummary = MedicalRecordSummary(
        title = when (json.resourceType) {
            "Practitioner" -> json.humanName()?.let { SummaryValue.Text(it) }
            "PractitionerRole" -> json.reference("practitioner") ?: json.codedList("code").firstOrNull()
            else -> json.string("name")?.let { SummaryValue.Text(it) }
        },
        date = null,
        status = json.string("status"),
        value = null,
        flag = null,
        details = json.codedList("specialty").map { SummaryDetail(SummaryField.SPECIALTY, it) } +
            json.codedList("type").map { SummaryDetail(SummaryField.TYPE, it) } +
            (if (json.resourceType == "PractitionerRole") json.codedList("code").map { SummaryDetail(SummaryField.TYPE, it) } else emptyList()) +
            listOfNotNull(json.reference("organization")?.let { SummaryDetail(SummaryField.SERVICE_PROVIDER, it) }) +
            json.objects("telecom").mapNotNull { it.string("value") }.map { detail(SummaryField.CONTACT, it) },
    )

    // Readers.

    private fun detail(field: SummaryField, text: String) = SummaryDetail(field, SummaryValue.Text(text))

    private fun JsonObject.date(key: String): FhirDate? = string(key)?.takeIf { it.isNotBlank() }?.let(::FhirDate)

    private fun JsonObject.literal(key: String): String? = primitive(key)?.content

    private fun JsonObject.coded(key: String): SummaryValue? = obj(key)?.codeable()

    private fun JsonObject.codedList(key: String): List<SummaryValue> = objects(key).mapNotNull { it.codeable() }

    private fun JsonObject.firstCode(): String? = objects("coding").firstNotNullOfOrNull { it.string("code") }

    /** Text first, then the first coding's display, then its code with the system as a caption. */
    private fun JsonObject.codeable(): SummaryValue.Text? {
        string("text")?.let { return SummaryValue.Text(it) }
        val codings = objects("coding")
        codings.firstNotNullOfOrNull { it.string("display") }?.let { return SummaryValue.Text(it) }
        val coding = codings.firstOrNull { it.string("code") != null } ?: return null
        return SummaryValue.Text(coding.string("code")!!, caption = coding.string("system"))
    }

    private fun JsonObject.coding(): SummaryValue.Text? =
        string("display")?.let { SummaryValue.Text(it) }
            ?: string("code")?.let { SummaryValue.Text(it, caption = string("system")) }

    private fun JsonObject.reference(key: String): SummaryValue.Reference? = obj(key)?.asReference()

    private fun JsonObject.asReference(): SummaryValue.Reference =
        SummaryValue.Reference(reference = string("reference"), display = string("display"))

    private fun JsonObject.humanName(): String? {
        val name = objects("name").firstOrNull { it.string("use") in setOf("usual", "official") } ?: objects("name").firstOrNull()
        return name?.string("text") ?: name?.let { (it.strings("given") + listOfNotNull(it.string("family"))).joinToString(" ") }
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.period(): String? {
        val start = string("start")
        val end = string("end")
        return when {
            start != null && end != null -> "$start – $end"
            else -> start ?: end
        }
    }

    /** An Observation or component value as written, with its unit. */
    private fun JsonObject.value(): String? =
        obj("valueQuantity")?.quantity()
            ?: obj("valueCodeableConcept")?.codeableLabel()
            ?: string("valueString")
            ?: primitive("valueBoolean")?.content
            ?: primitive("valueInteger")?.content
            ?: obj("valueRange")?.let { range -> rangeText(range.obj("low")?.quantity(), range.obj("high")?.quantity()) }
            ?: obj("valueRatio")?.let { ratio ->
                listOfNotNull(ratio.obj("numerator")?.quantity(), ratio.obj("denominator")?.quantity()).joinToString(" : ").ifEmpty { null }
            }
            ?: string("valueDateTime")
            ?: string("valueTime")

    private fun JsonObject.quantity(): String? {
        val number = (this["value"] as? JsonPrimitive)?.content ?: return null
        val comparator = string("comparator").orEmpty()
        val unit = string("unit") ?: string("code")
        return listOfNotNull("$comparator$number", unit).joinToString(" ")
    }

    private fun JsonObject.range(): String? =
        string("text") ?: rangeText(obj("low")?.quantity(), obj("high")?.quantity())

    private fun rangeText(low: String?, high: String?): String? = when {
        low != null && high != null -> "$low – $high"
        low != null -> "≥ $low"
        high != null -> "≤ $high"
        else -> null
    }
}
