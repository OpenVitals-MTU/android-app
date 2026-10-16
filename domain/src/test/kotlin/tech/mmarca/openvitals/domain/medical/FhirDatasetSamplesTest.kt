package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.references
import tech.mmarca.openvitals.domain.model.MedicalCategory

/**
 * Files from published synthetic datasets, each read as the wizard reads a picked file: a Synthea
 * transaction bundle, one patient of a SMART bulk export, and the HL7 AU test data, whose
 * missing and suppressed data cases are what a real server sends when it holds a value back.
 * `src/test/resources/fhir/datasets/README.md` says where each file comes from.
 * [FhirCorpusCheck] runs the same steps over the whole datasets, by hand.
 */
class FhirDatasetSamplesTest {

    @Test
    fun `no record of the R4 datasets is taken for DSTU2`() {
        val converted = datasetFiles("").flatMap { file -> analyze(file).groups.flatMap { it.ready } }
            .filter { Dstu2ToR4.isConverted(it.json) }

        assertThat(converted.map { "${it.type}/${it.id}" }).isEmpty()
    }

    @Test
    fun `a Synthea transaction bundle imports whole, with its urn references pointing at the records they name`() {
        val analysis = analyze(datasetFiles("synthea").single())
        val group = analysis.groups.single()

        assertThat(group.baseUri).isNull()
        assertThat(group.suggestedName).isEqualTo("transaction-bundle")
        assertThat(group.ready).hasSize(23)
        assertThat(group.rejected).isEmpty()
        assertThat(group.notes).isEmpty()
        assertThat(group.skippedTypes).containsExactly("Claim", 1, "DiagnosticReport", 1, "ExplanationOfBenefit", 1)
        assertThat(unresolvedReferences(group)).isEmpty()
        assertThat(group.likelyCategories).containsAtLeast(MedicalCategory.LAB_RESULTS, 11, MedicalCategory.VITAL_SIGNS, 5)
        assertThat(PatientCheck.check(analysis.patients.map { it.json }, emptyList())).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `one patient of a bulk export imports whole, every Observation sorted by its category`() {
        val analysis = analyze(datasetFiles("smart-bulk").single())
        val group = analysis.groups.single()

        assertThat(group.ready).hasSize(207)
        assertThat(group.rejected).isEmpty()
        assertThat(group.skippedTypes.keys).containsExactly("Device", "DiagnosticReport", "DocumentReference", "EpisodeOfCare", "ServiceRequest", "Specimen")
        assertThat(group.likelyCategories.keys).doesNotContain(null)
        assertThat(group.likelyCategories).containsAtLeast(MedicalCategory.SOCIAL_HISTORY, 11, MedicalCategory.VITAL_SIGNS, 101)
        // Synthea names practitioners, places and organisations by identifier, which no record of the file answers,
        // and each visit names its episode of care, a type Health Connect does not store.
        val unresolved = unresolvedReferences(group).filterNot { '?' in it }
        assertThat(unresolved.map { it.substringBefore('/') }.toSet()).containsExactly("EpisodeOfCare")
        assertThat(PatientCheck.check(analysis.patients.map { it.json }, emptyList())).isEqualTo(PatientCheckResult.Pass)
    }

    @Test
    fun `the AU missing and suppressed data cases import, but for the Observations Health Connect cannot classify`() {
        val files = datasetFiles("hl7au/au-core")
        val analyses = files.associate { it.name to analyze(it) }
        val rejected = analyses.flatMap { (name, analysis) -> analysis.groups.flatMap { it.rejected }.map { name to it.problem.reason } }

        assertThat(files).hasSize(64)
        assertThat(rejected).containsExactly(
            "Observation-glasgow-coma-scale-motor-not-performed.json" to FhirRejection.UNCLASSIFIABLE_OBSERVATION,
            "Observation-glasgow-coma-scale.json" to FhirRejection.UNCLASSIFIABLE_OBSERVATION,
            "Observation-masked.json" to FhirRejection.UNCLASSIFIABLE_OBSERVATION,
            "Observation-visualacuity.json" to FhirRejection.UNCLASSIFIABLE_OBSERVATION,
        )
        // One record per file, and one more for each contained Medication lifted out of its parent.
        assertThat(analyses.values.sumOf { analysis -> analysis.readyCount }).isEqualTo(60 + 2)
    }

    @Test
    fun `a value held back by a data absent reason stays, and a contained medication becomes a record of its own`() {
        val patient = readyIn("hl7au/au-core/Patient-italia-sofia-suppressed-birthDate.json").single()
        val request = analyze(datasetFile("hl7au/au-core/MedicationRequest-metformin-contained.json")).groups.single()
        val order = request.ready.single { it.type == "MedicationRequest" }.json

        assertThat(patient.json.keys).doesNotContain("birthDate")
        assertThat(patient.json.getValue("_birthDate").jsonObject.getValue("extension").jsonArray.single().jsonObject.string("valueCode"))
            .isEqualTo("masked")
        assertThat(order.keys).doesNotContain("contained")
        assertThat(order.getValue("medicationReference").jsonObject.string("reference")).isEqualTo("Medication/metformin-contained-medication")
        assertThat(request.ready.map { "${it.type}/${it.id}" }).contains("Medication/metformin-contained-medication")
        assertThat(request.notes.single()).isInstanceOf(FhirIdNote.ContainedLifted::class.java)
    }

    @Test
    fun `an AU patient summary imports without its Composition, every reference pointing at a record in it`() {
        val groups = datasetFiles("hl7au/au-patient-summary").map { analyze(it).groups.single() }

        assertThat(groups).hasSize(3)
        groups.forEach { group ->
            assertThat(group.rejected).isEmpty()
            assertThat(group.skippedTypes).containsEntry("Composition", 1)
            assertThat(group.ready.count { it.type == "Patient" }).isEqualTo(1)
            assertThat(unresolvedReferences(group)).isEmpty()
        }
    }

    /** Through the wizard's own reader, as a picked file. */
    private fun analyze(file: File): FhirImportAnalysis {
        val read = FhirFileImportSource.read { file.inputStream() } as MedicalImportSourceResult.Entries
        return FhirImportAnalyzer.analyze(read.file, file.name)
    }

    private fun readyIn(path: String): List<PreparedResource> = analyze(datasetFile(path)).groups.flatMap { it.ready }

    /** The references of [group]'s ready records that name no ready record of it. */
    private fun unresolvedReferences(group: FhirImportGroup): List<String> {
        val keys = group.ready.mapTo(mutableSetOf()) { "${it.type}/${it.id}" }
        return group.ready.flatMap { references(it.json) }.filterNot { it in keys }
    }

    private fun datasetFile(path: String): File =
        File(checkNotNull(javaClass.classLoader?.getResource("fhir/datasets/$path")) { "missing dataset file $path" }.toURI())

    private fun datasetFiles(folder: String): List<File> =
        datasetFile(folder).walkTopDown().filter { it.isFile && it.extension in setOf("json", "ndjson") }.sortedBy { it.name }.toList()
}
