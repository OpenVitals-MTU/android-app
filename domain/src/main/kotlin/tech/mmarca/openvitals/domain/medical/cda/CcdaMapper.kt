package tech.mmarca.openvitals.domain.medical.cda

import org.w3c.dom.Element

/**
 * Maps a CDA document with the standard LOINC-coded sections to FHIR R4: the C-CDA a patient
 * portal hands out as "download my record", and patient summaries built the same way.
 *
 * | Section | LOINC | FHIR |
 * |---|---|---|
 * | Problems | 11450-4 | Condition |
 * | Allergies | 48765-2 | AllergyIntolerance |
 * | Medications | 10160-0 | MedicationStatement |
 * | Immunizations | 11369-6 | Immunization |
 * | Results | 30954-2 | Observation, laboratory |
 * | Vital signs | 8716-3 | Observation, vital-signs |
 * | Social history | 29762-2 | Observation, social-history |
 * | Procedures | 47519-4 | Procedure |
 * | Encounters | 46240-8 | Encounter |
 *
 * Other sections, and the narrative of every section, stay in the document. An entry that says
 * "none known" (a negated entry, or one with no code and no text) makes no record.
 *
 * A document written by a device or an app, with no person as author, gives no vital signs:
 * those are the app's own measurements, such as Apple Health's `export_cda.xml` holds, and
 * measurements belong with Health Connect's fitness data, not with medical records.
 */
internal object CcdaMapper {
    private const val Problems = "11450-4"
    private const val Allergies = "48765-2"
    private const val Medications = "10160-0"
    private const val Immunizations = "11369-6"
    private const val Results = "30954-2"
    private const val VitalSigns = "8716-3"
    private const val SocialHistory = "29762-2"
    private const val Procedures = "47519-4"
    private const val Encounters = "46240-8"

    /** [contentHash] stands in for the series id of a document that states none. */
    fun map(root: Element, contentHash: String): CdaMapResult {
        val document = CdaDocumentContext(root, CdaProfile.Generic, contentHash)
        val author = root.at("author", "assignedAuthor")
        val byDevice = author?.child("assignedAuthoringDevice") != null && author.child("assignedPerson") == null
        val entries = document.entries { sections.forEachIndexed { index, section -> mapSection(section, "$index", vitals = !byDevice) } }
        return CdaMapResult.Mapped(entries, document.series, document.version)
    }

    private fun CdaDocumentContext.mapSection(section: Element, place: String, vitals: Boolean) {
        val code = section.sectionCode()
        section.children("entry").forEachIndexed { index, entry ->
            val here = "$code.$place.$index"
            when (code) {
                Problems -> problems(entry, here)
                Allergies -> allergies(entry, here)
                Medications -> entry.child("substanceAdministration")?.let { medication(it, here) }
                Immunizations -> entry.child("substanceAdministration")?.let { vaccination(it, here) }
                Results -> results(entry, here, ObservationCategory.LABORATORY)
                VitalSigns -> if (vitals) results(entry, here, ObservationCategory.VITAL_SIGNS)
                SocialHistory -> results(entry, here, ObservationCategory.SOCIAL_HISTORY)
                Procedures -> (entry.child("procedure") ?: entry.child("observation") ?: entry.child("act"))?.let { procedureDone(it, here) }
                Encounters -> entry.child("encounter")?.let { encounter(it, here) }
            }
        }
        section.children("component").mapNotNull { it.child("section") }
            .forEachIndexed { index, subsection -> mapSection(subsection, "$place.$index", vitals) }
    }
}
