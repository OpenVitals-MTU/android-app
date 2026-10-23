package tech.mmarca.openvitals.domain.medical

import tech.mmarca.openvitals.domain.model.MedicalCategory

data class FhirRejectedResource(val resource: PreparedResource, val problem: FhirPreflightProblem)

/**
 * One source's records after the analyze step, ready for the confirm step.
 *
 * [baseUri] is null when the user names the source. [versionStated] is true when the
 * file's index gives [fhirVersion], as Apple's export does, and false when it is a guess.
 * [likelyCategories] counts the [ready] records by the category Health Connect will most
 * likely assign; the null key counts Observations it classifies by LOINC code.
 */
data class FhirImportGroup(
    val baseUri: String?,
    val suggestedName: String,
    val fhirVersion: String,
    val ready: List<PreparedResource>,
    val rejected: List<FhirRejectedResource>,
    val skippedTypes: Map<String, Int>,
    val notes: List<FhirIdNote>,
    val versionStated: Boolean = false,
) {
    val likelyCategories: Map<MedicalCategory?, Int>
        get() = ready.groupingBy { FhirResourceTypes.likelyCategory(it.json) }.eachCount()
}

/** A file after the analyze step. Nothing is written yet. [patients] feeds the patient check. */
data class FhirImportAnalysis(
    val groups: List<FhirImportGroup>,
    val isOpenVitalsExport: Boolean,
) {
    val patients: List<PreparedResource> get() = groups.flatMap { group -> group.ready.filter { it.type == "Patient" } }
    val readyCount: Int get() = groups.sumOf { it.ready.size }
}

/**
 * The analyze step: group by source, lift the results a DiagnosticReport holds inside itself
 * ([DiagnosticReportResults]), convert DSTU2 records to R4, skip unsupported types, assign ids,
 * then pre-flight. A record is DSTU2 when the Bundle's profile says so, or by its own shape
 * ([Dstu2ToR4.looksDstu2]): a plain file states no version. A result lifted from a DSTU2 report is DSTU2 too.
 */
object FhirImportAnalyzer {
    fun analyze(file: FhirFile, fileName: String): FhirImportAnalysis {
        val dstu2Bundle = file.bundleProfiles.any(Dstu2ToR4::profileNamesDstu2)
        val groups = FhirSourceGrouper.group(file, fileName).map { group ->
            val scope = group.baseUri ?: FhirSourceGrouper.ImportScheme
            val liftNotes = mutableListOf<FhirIdNote>()
            val entries = group.entries.flatMap { entry ->
                val dstu2 = dstu2Bundle || Dstu2ToR4.looksDstu2(entry.resource)
                val results = DiagnosticReportResults.of(entry, scope).map { (result, note) ->
                    liftNotes += note
                    if (dstu2 || Dstu2ToR4.looksDstu2(result.resource)) result.asR4() else result
                }
                listOf(if (dstu2) entry.asR4() else entry) + results
            }
            val (supported, unsupported) = entries.partition { it.type in FhirResourceTypes.supported }
            val ids = FhirIdAssigner.assign(supported, scope = scope)
            val checked = ids.resources.map { it to FhirPreflight.check(it) }
            FhirImportGroup(
                baseUri = group.baseUri,
                suggestedName = group.suggestedName,
                fhirVersion = FhirVersionDetector.detect(group, file.bundleProfiles),
                ready = checked.filter { it.second == null }.map { it.first },
                rejected = checked.mapNotNull { (resource, problem) -> problem?.let { FhirRejectedResource(resource, it) } },
                skippedTypes = unsupported.groupingBy { it.type ?: "" }.eachCount(),
                notes = liftNotes + ids.notes,
                versionStated = group.fhirVersion != null,
            )
        }
        return FhirImportAnalysis(groups, file.isOpenVitalsExport)
    }

    /** The entry with its record in R4. A type with no mapping stays as it is: the platform's reason reaches the report. */
    private fun FhirEntry.asR4(): FhirEntry = Dstu2ToR4.convert(resource)?.let { copy(resource = it) } ?: this
}
