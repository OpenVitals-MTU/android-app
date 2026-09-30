package tech.mmarca.openvitals.domain.medical.cda

import java.time.ZoneId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.w3c.dom.Element
import tech.mmarca.openvitals.domain.medical.FhirEntry
import tech.mmarca.openvitals.domain.medical.FhirOrigin
import tech.mmarca.openvitals.domain.medical.FhirVersionDetector
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.fhirId
import tech.mmarca.openvitals.domain.medical.sha256Hex

/** One CDA document: its records as FHIR, or why it is left out. */
internal sealed interface CdaMapResult {
    /** [series] and [version] order the versions of one document, so the newest is read last and wins. */
    data class Mapped(val entries: List<FhirEntry>, val series: String, val version: Int) : CdaMapResult

    /** [clinic] is the name of the organisation that holds the document. */
    data class Skipped(val reason: MedicalSourceSkipReason, val title: String, val clinic: String? = null) : CdaMapResult
}

/**
 * What differs between kinds of CDA document: how ids and sources are named, which registry
 * names a clinic or a clinician, and which zone a time with no zone is in.
 */
internal class CdaProfile(
    val idPrefix: String,
    /** Null when the zone is not known: a time with no zone then keeps only its date. */
    val zone: ZoneId?,
    val sourceBaseUri: String,
    /** The source name for a document that names no organisation. Null lets the user name it. */
    val defaultSourceName: String?,
    /** The id root that names a clinic, when the profile has one. Its extension alone is then the clinic's code. */
    val organizationRoot: String?,
    val practitionerRoot: String?,
    val patientId: String,
) {
    companion object {
        /** Documents of Estonia's health information system: registry codes, and Estonian time. */
        val Estonian = CdaProfile(
            idPrefix = "ee",
            zone = ZoneId.of("Europe/Tallinn"),
            sourceBaseUri = "openvitals://ee-tis",
            defaultSourceName = "Terviseportaal",
            organizationRoot = EstonianCdaCodes.BusinessRegistryRoot,
            practitionerRoot = EstonianCdaCodes.HealthcareProfessionalRoot,
            patientId = "self",
        )

        /** Any other CDA document, such as the C-CDA a patient portal hands out. */
        val Generic = CdaProfile(
            idPrefix = "cda",
            zone = null,
            sourceBaseUri = "openvitals://cda",
            defaultSourceName = null,
            organizationRoot = null,
            practitionerRoot = null,
            patientId = "patient",
        )
    }
}

/**
 * The state of one document while it is mapped: its ids, its references, and the records made
 * so far. The patient keeps name, sex and birth date; identifiers, address and phone are left out.
 *
 * [contentHash] stands in for the series id of a document that states none. [referral] marks an
 * Estonian referral, whose diagnoses are not yet confirmed.
 */
internal class CdaDocumentContext(
    private val root: Element,
    private val profile: CdaProfile,
    contentHash: String,
    val isReferral: Boolean = false,
) {
    /** The document's title, which a referral's diagnoses carry as a note. */
    val title: String? = root.child("code")?.attr("displayName") ?: root.child("title")?.text()

    val series: String = (root.child("setId") ?: root.child("id"))
        ?.let { listOfNotNull(it.attr("root"), it.attr("extension")).joinToString(":") }?.takeIf { it.isNotEmpty() } ?: contentHash
    val version: Int = root.child("versionNumber")?.attr("value")?.toIntOrNull() ?: 0

    /** When the document was written: the fallback for a record that states no time. */
    val time: String? = dateTime(root.child("effectiveTime")?.timeValue())

    private val author: Element? = root.at("author", "assignedAuthor")
    private val clinic: Element? =
        root.at("custodian", "assignedCustodian", "representedCustodianOrganization") ?: author?.child("representedOrganization")

    private val organization: JsonObject? = clinic?.let(::organization)
    private val practitioner: JsonObject? = author?.let(::practitioner)

    val patientRef: JsonObject = reference("Patient", profile.patientId)
    val organizationRef: JsonObject? = organization?.let { reference("Organization", it.id) }
    val authorRef: JsonObject? = practitioner?.let { reference("Practitioner", it.id) }

    /** The document's one visit, once made, for the records that belong to it. */
    var visitRef: JsonObject? = null

    private val origin: FhirOrigin? = run {
        val name = clinic?.child("name")?.text() ?: profile.defaultSourceName
        val key = clinicKey()
        when {
            key != null -> FhirOrigin("${profile.sourceBaseUri}/$key", name ?: key, FhirVersionDetector.R4)
            // With no organisation to name it, the user names the source, unless the profile has a name for it.
            name != null && profile.defaultSourceName != null -> FhirOrigin("${profile.sourceBaseUri}/unknown", name, FhirVersionDetector.R4)
            else -> null
        }
    }

    /** Narrative text by its `ID`, which entries point at instead of repeating it. */
    private val narrativeById: Map<String, String> by lazy {
        buildMap {
            fun walk(element: Element) {
                element.attr("ID")?.let { id -> element.text()?.let { put(id, it) } }
                var node = element.firstChild
                while (node != null) {
                    if (node is Element) walk(node)
                    node = node.nextSibling
                }
            }
            root.at("component", "structuredBody")?.let(::walk)
        }
    }

    private val resources = mutableListOf<JsonObject>()

    fun add(resource: JsonObject) {
        resources += resource
    }

    /** How many records are made so far. */
    val size: Int get() = resources.size

    /** A stable id: the document series, the entry's place, and the resource type. */
    fun id(place: String, type: String): String = "${profile.idPrefix}-" + sha256Hex("$series|$place|$type").take(IdHexLength)

    /** A CDA time as FHIR, in the profile's zone when the time states none. */
    fun dateTime(ts: String?): String? = fhirDateTime(ts, profile.zone)

    /** The narrative text a `#id` reference points at. */
    fun narrative(reference: String): String? = narrativeById[reference.removePrefix("#")]

    /** The top-level sections of the body, in document order. */
    val sections: List<Element> get() = root.at("component", "structuredBody")?.children("component")?.mapNotNull { it.child("section") }.orEmpty()

    /** The patient, the clinic and the author, then whatever [mapSections] adds. */
    fun entries(mapSections: CdaDocumentContext.() -> Unit): List<FhirEntry> {
        patient()?.let(::add)
        organization?.let(::add)
        practitioner?.let(::add)
        mapSections()
        return resources.map { FhirEntry(it, origin = origin) }
    }

    private fun patient(): JsonObject? {
        val person = root.at("recordTarget", "patientRole", "patient") ?: return null
        val name = person.child("name")?.humanName()
        // HL7's M and F, and Estonia's N (naine).
        val gender = when (person.child("administrativeGenderCode")?.attr("code")?.uppercase()) {
            "M" -> "male"
            "N", "F" -> "female"
            else -> null
        }
        val birthDate = fhirDate(person.child("birthTime")?.attr("value"))
        return buildJsonObject {
            put("resourceType", "Patient")
            put("id", profile.patientId)
            name?.let { put("name", JsonArray(listOf(it))) }
            gender?.let { put("gender", it) }
            birthDate?.let { put("birthDate", it) }
        }
    }

    private fun organization(clinic: Element): JsonObject? {
        val id = clinicId()
        val name = clinic.child("name")?.text()
        if (id == null && name == null) return null
        val code = id?.attr("extension") ?: id?.attr("root").takeIf { profile.organizationRoot == null }
        return buildJsonObject {
            put("resourceType", "Organization")
            put("id", "${profile.idPrefix}-org-" + safeId(code ?: sha256Hex(name.orEmpty()).take(IdHexLength)))
            id?.let(::identifier)?.let { put("identifier", JsonArray(listOf(it))) }
            name?.let { put("name", it) }
        }
    }

    private fun practitioner(assigned: Element): JsonObject? {
        val ids = assigned.children("id")
        val id = profile.practitionerRoot?.let { root -> ids.firstOrNull { it.attr("root") == root } }
            ?: ids.firstOrNull { it.attr("extension") != null }.takeIf { profile.practitionerRoot == null }
        val code = id?.attr("extension")
        val name = assigned.at("assignedPerson", "name")?.humanName()
        if (code == null && name == null) return null
        val qualification = assigned.child("code")?.codeableConcept()
        return buildJsonObject {
            put("resourceType", "Practitioner")
            put("id", "${profile.idPrefix}-prac-" + safeId(code ?: sha256Hex(name.toString()).take(IdHexLength)))
            id?.let(::identifier)?.let { put("identifier", JsonArray(listOf(it))) }
            name?.let { put("name", JsonArray(listOf(it))) }
            qualification?.let { put("qualification", JsonArray(listOf(buildJsonObject { put("code", it) }))) }
        }
    }

    /** The id that names the clinic: the profile's registry when it has one, else the first id. */
    private fun clinicId(): Element? {
        val ids = clinic?.children("id").orEmpty()
        return profile.organizationRoot?.let { root -> ids.firstOrNull { it.attr("root") == root } } ?: ids.firstOrNull()
    }

    /** The clinic's place in a source address: its registry code, or its id root and extension. */
    private fun clinicKey(): String? {
        val id = clinicId() ?: return null
        val extension = id.attr("extension")
        return when {
            profile.organizationRoot != null -> extension
            else -> listOfNotNull(id.attr("root"), extension).joinToString("/") { safeId(it) }.takeIf { it.isNotEmpty() }
        }
    }

    /** An id as a FHIR Identifier, when it has an extension to be the value. */
    private fun identifier(id: Element): JsonObject? {
        val value = id.attr("extension") ?: return null
        return buildJsonObject {
            id.attr("root")?.let { put("system", "urn:oid:$it") }
            put("value", value)
        }
    }

    private companion object {
        const val IdHexLength = 40
        const val MaxIdLength = 64
        const val LongestIdPrefix = "cda-prac-"
        val NotIdCharacters = Regex("[^A-Za-z0-9.-]")

        /** A FHIR id: letters, digits, `-` and `.`, at most 64 characters. */
        fun safeId(text: String): String = text.replace(NotIdCharacters, "-").take(MaxIdLength - LongestIdPrefix.length)
    }
}

/** The id a CDA mapper always sets. */
internal val JsonObject.id: String get() = requireNotNull(fhirId)
