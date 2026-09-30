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

/** The analyze step: group by source, skip unsupported types, assign ids, then pre-flight. */
object FhirImportAnalyzer {
    fun analyze(file: FhirFile, fileName: String): FhirImportAnalysis {
        val groups = FhirSourceGrouper.group(file, fileName).map { group ->
            val (supported, unsupported) = group.entries.partition { it.type in FhirResourceTypes.supported }
            val ids = FhirIdAssigner.assign(supported, scope = group.baseUri ?: FhirSourceGrouper.ImportScheme)
            val checked = ids.resources.map { it to FhirPreflight.check(it) }
            FhirImportGroup(
                baseUri = group.baseUri,
                suggestedName = group.suggestedName,
                fhirVersion = FhirVersionDetector.detect(group, file.bundleProfiles),
                ready = checked.filter { it.second == null }.map { it.first },
                rejected = checked.mapNotNull { (resource, problem) -> problem?.let { FhirRejectedResource(resource, it) } },
                skippedTypes = unsupported.groupingBy { it.type ?: "" }.eachCount(),
                notes = ids.notes,
                versionStated = group.fhirVersion != null,
            )
        }
        return FhirImportAnalysis(groups, file.isOpenVitalsExport)
    }
}
