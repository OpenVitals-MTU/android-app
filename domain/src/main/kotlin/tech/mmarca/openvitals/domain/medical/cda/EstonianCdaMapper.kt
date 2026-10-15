package tech.mmarca.openvitals.domain.medical.cda

import org.w3c.dom.Element

/**
 * Maps an Estonian CDA document to FHIR R4 resources, grouped under the clinic that holds it.
 *
 * - The patient becomes `Patient/self` with name, sex and birth date. The national ID code,
 *   the address and the phone are left out.
 * - The author and the clinic become a Practitioner and an Organization, keyed by their public
 *   registry codes, so every document names the same ones.
 * - Visits, diagnoses, prescriptions, lab results, immunisations, dental work, radiology and
 *   pathology become Encounter, Condition, MedicationRequest, Observation, Immunization and
 *   Procedure records. Narrative sections stay in the document.
 * - A referral gives only its diagnoses, as provisional Conditions: the referring doctor had not
 *   confirmed them, and Health Connect has no record type for the request itself.
 * - Ids come from the document's series id and the entry's place, so a re-import updates.
 */
internal object EstonianCdaMapper {
    fun isEstonian(root: Element): Boolean =
        isClinicalDocument(root) && root.children("templateId").any { it.attr("root") == EstonianCdaCodes.TemplateRoot }

    /** [contentHash] stands in for the series id of a document that states none. */
    fun map(root: Element, contentHash: String = ""): CdaMapResult {
        val code = root.child("code")
        EstonianCdaCodes.skipReason(code?.attr("code"))?.let { reason ->
            val clinic = (root.at("custodian", "assignedCustodian", "representedCustodianOrganization") ?: root.at("author", "assignedAuthor", "representedOrganization"))
                ?.child("name")?.text()
            return CdaMapResult.Skipped(reason, code?.attr("displayName") ?: root.child("title")?.text().orEmpty(), clinic)
        }
        val document = CdaDocumentContext(root, CdaProfile.Estonian, contentHash, isReferral = EstonianCdaCodes.isReferral(code?.attr("code")))
        val entries = document.entries {
            // Visits first, so the records of the same document can point at them.
            val (visits, others) = sections.withIndex().partition { (_, section) -> section.sectionCode() == EstonianCdaCodes.Visit }
            (visits + others).forEach { (index, section) -> mapSection(section, "$index") }
        }
        return CdaMapResult.Mapped(entries, document.series, document.version)
    }

    private fun CdaDocumentContext.mapSection(section: Element, place: String) {
        val code = section.sectionCode()
        // A referral repeats earlier findings; only its own diagnoses are new.
        if (isReferral && code != EstonianCdaCodes.Diagnoses) return
        if (code == EstonianCdaCodes.Studies) {
            studies(section.children("entry"), "$code.$place")
        } else {
            section.children("entry").forEachIndexed { index, entry -> mapEntry(code, entry, "$code.$place.$index") }
        }
        section.children("component").mapNotNull { it.child("section") }
            .forEachIndexed { index, subsection -> mapSection(subsection, "$place.$index") }
    }

    private fun CdaDocumentContext.mapEntry(section: String?, entry: Element, place: String) {
        entry.child("encounter")?.let { encounter ->
            when (section) {
                EstonianCdaCodes.Visit -> visitRef = reference("Encounter", visit(encounter, place).id)
                EstonianCdaCodes.Dental -> dentalVisit(encounter, place)
            }
        }
        entry.child("substanceAdministration")?.let { if (section == EstonianCdaCodes.Drugs) prescription(it, place) }
        entry.child("procedure")?.let { procedure ->
            when (section) {
                EstonianCdaCodes.Immunisation -> immunization(procedure, place)
                EstonianCdaCodes.Labs, EstonianCdaCodes.Pathology -> labPanel(procedure, place, inheritedTime = null)
            }
        }
        entry.child("observation")?.let { observation ->
            when (section) {
                EstonianCdaCodes.Radiology -> imaging(observation, place)
                EstonianCdaCodes.Labs -> observationRecord(observation.child("code"), observation, observation, place, time)
                else -> diagnoses(observation, place)
            }
        }
    }
}

/** A CDA document of any profile: a `ClinicalDocument` in HL7's v3 namespace. */
internal fun isClinicalDocument(root: Element): Boolean = root.localName == "ClinicalDocument" && root.namespaceURI == Hl7Namespace

/** Resource types every document of one clinic repeats. They are not what a document is about. */
internal val CdaSharedTypes = setOf("Patient", "Practitioner", "Organization")

internal fun Element.sectionCode(): String? = child("code")?.attr("code")
