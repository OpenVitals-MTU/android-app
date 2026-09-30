package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.entry
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

class FhirImportAnalyzerTest {

    @Test
    fun `a clean file is ready apart from the types Health Connect refuses`() {
        val analysis = FhirImportAnalyzer.analyze(FhirTestFiles.parsed("spike-sample-bundle.json"), "sample.json")
        val group = analysis.groups.single()

        assertThat(group.ready).hasSize(12)
        assertThat(group.rejected).isEmpty()
        assertThat(group.skippedTypes).containsExactly("DiagnosticReport", 1)
        assertThat(group.notes).isEmpty()
        assertThat(group.fhirVersion).isEqualTo(FhirVersionDetector.R4)
        assertThat(group.likelyCategories[MedicalCategory.VACCINES]).isEqualTo(2)
        assertThat(group.likelyCategories[MedicalCategory.VISITS]).isEqualTo(2)
        assertThat(analysis.patients.map { it.id }).containsExactly("pat-1")
    }

    @Test
    fun `a document bundle with no ids comes out ready, with every repair noted`() {
        val group = FhirImportAnalyzer.analyze(FhirTestFiles.parsed("ips-document-bundle.json"), "summary.json").groups.single()

        assertThat(group.ready.map { it.type }).containsExactly(
            "Patient", "AllergyIntolerance", "MedicationStatement", "Medication", "Condition", "Immunization",
            "Observation", "Practitioner",
        )
        assertThat(group.rejected).isEmpty()
        assertThat(group.skippedTypes).containsExactly("Composition", 1)
        // The weight has no category: Health Connect classifies it by its LOINC code.
        assertThat(group.likelyCategories[null]).isEqualTo(1)
        assertThat(group.notes.filterIsInstance<FhirIdNote.ContainedDropped>()).hasSize(1)
    }

    @Test
    fun `a record pre-flight refuses is listed with the reason, the rest stay ready`() {
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"}}"""),
                entry("""{"resourceType":"Observation","id":"o1","status":"final","code":{"text":"Mood"}}"""),
            ),
        )

        val group = FhirImportAnalyzer.analyze(file, "x.json").groups.single()

        assertThat(group.ready.map { it.id }).containsExactly("c1")
        assertThat(group.rejected.single().problem.reason).isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
    }

    @Test
    fun `an export re-imports to the same sources, ids and JSON`() {
        val clinic = MedicalRecordSource("s1", "tech.mmarca.openvitals", "https://clinic.example/fhir", "Clinic", "4.0.1", null)
        val manual = MedicalRecordSource("s2", "tech.mmarca.openvitals", "openvitals://manual", "Entered in OpenVitals", "4.0.1", null)
        val sharedIdA = """{"resourceType":"Immunization","id":"1","status":"completed","vaccineCode":{"text":"Tdap"},""" +
            """"patient":{"reference":"Patient/self"},"occurrenceDateTime":"2019-03-14"}"""
        val sharedIdB = sharedIdA.replace("Tdap", "Measles")
        val records = listOf(
            MedicalRecord(MedicalRecordRef("s1", "Immunization", "1"), MedicalCategory.VACCINES, "4.0.1", sharedIdA),
            MedicalRecord(MedicalRecordRef("s2", "Immunization", "1"), MedicalCategory.VACCINES, "4.0.1", sharedIdB),
        )

        val exported = FhirBundleWriter.write(records, listOf(clinic, manual), "2.12.0", Instant.parse("2026-09-29T10:00:00Z"))
        val file = (FhirFileParser.parse(exported) as FhirParseResult.Parsed).file
        val analysis = FhirImportAnalyzer.analyze(file, "openvitals-medical-records-2026-09-29.json")

        assertThat(analysis.isOpenVitalsExport).isTrue()
        assertThat(analysis.groups.map { it.baseUri to it.suggestedName }).containsExactly(
            "https://clinic.example/fhir" to "Clinic",
            "openvitals://manual" to "Entered in OpenVitals",
        )
        analysis.groups.forEach { group ->
            assertThat(group.notes).isEmpty()
            assertThat(group.ready.single().id).isEqualTo("1")
        }
        val reimported = analysis.groups.map { it.ready.single().json.canonical() }
        assertThat(reimported).containsExactly(
            FhirTestFiles.json(sharedIdA).canonical(),
            FhirTestFiles.json(sharedIdB).canonical(),
        )
    }

    @Test
    fun `an R4B profile hint sets the version, and an origin's version wins`() {
        val r4b = FhirFile(listOf(entry("""{"resourceType":"Condition","id":"c","meta":{"profile":["http://hl7.org/fhir/R4B/StructureDefinition/Condition"]}}""")))
        val origin = FhirFile(listOf(entry("""{"resourceType":"Condition","id":"c"}""", origin = FhirOrigin("https://h.example", "H", "4.3.0"))))

        assertThat(FhirImportAnalyzer.analyze(r4b, "x.json").groups.single().fhirVersion).isEqualTo(FhirVersionDetector.R4B)
        assertThat(FhirImportAnalyzer.analyze(origin, "x.json").groups.single().fhirVersion).isEqualTo("4.3.0")
    }
}
