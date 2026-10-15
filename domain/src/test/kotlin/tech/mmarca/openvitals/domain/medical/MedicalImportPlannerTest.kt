package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.entry
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/** A source is matched by base URI, never by name, so a re-import updates rather than duplicates. */
class MedicalImportPlannerTest {

    private fun source(id: String, baseUri: String, name: String, version: String = "4.0.1", packageName: String = "tech.mmarca.openvitals") =
        MedicalRecordSource(id, packageName, baseUri, name, version, null)

    private val clinicFile = FhirTestFiles.parsed("spike-sample-bundle.json")
    private val clinicAnalysis = FhirImportAnalyzer.analyze(clinicFile, "sample.json")

    @Test
    fun `a file from an unknown origin gets a new source named after it`() {
        val plan = MedicalImportPlanner.plan(clinicAnalysis, ownSources = emptyList(), otherSources = emptyList()).single()

        assertThat(plan.target).isEqualTo(MedicalImportTarget.New("https://clinic.example/fhir", "clinic.example", fromName = false))
        assertThat(plan.include).isTrue()
        assertThat(plan.fhirVersion).isEqualTo(FhirVersionDetector.R4)
    }

    @Test
    fun `a source this app made for the same origin is reused, and its version wins`() {
        val existing = source("s1", "https://clinic.example/fhir/", "Clinic", version = "4.3.0")

        val plan = MedicalImportPlanner.plan(clinicAnalysis, ownSources = listOf(existing), otherSources = emptyList()).single()

        assertThat(plan.target).isEqualTo(MedicalImportTarget.Existing(existing))
        assertThat(plan.fhirVersion).isEqualTo("4.3.0")
    }

    @Test
    fun `a version the file states picks the source too, so an origin's R4 and R4B records stay apart`() {
        val base = "https://hospital.example/r4"
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Condition","id":"1"}""", origin = FhirOrigin(base, "Hospital", "4.0.1")),
                entry("""{"resourceType":"Condition","id":"2"}""", origin = FhirOrigin(base, "Hospital", "4.3.0")),
            ),
        )
        val r4 = source("s1", base, "Hospital")

        val plans = MedicalImportPlanner.plan(FhirImportAnalyzer.analyze(file, "export.zip"), listOf(r4), emptyList())

        assertThat(plans.map { it.target }).containsExactly(
            MedicalImportTarget.Existing(r4),
            MedicalImportTarget.New(base, "Hospital (FHIR 4.3.0)", fromName = false),
        ).inOrder()
        assertThat(plans.map { it.fhirVersion }).containsExactly("4.0.1", "4.3.0").inOrder()
    }

    @Test
    fun `an origin another app already holds is left out unless the user includes it`() {
        val theirs = source("x1", "https://clinic.example/fhir", "Clinic portal", packageName = "com.example.portal")

        val plan = MedicalImportPlanner.plan(clinicAnalysis, ownSources = emptyList(), otherSources = listOf(theirs)).single()

        assertThat(plan.otherApp).isEqualTo(theirs)
        assertThat(plan.include).isFalse()
    }

    @Test
    fun `a file that names no origin is matched by the name the user gives it`() {
        val analysis = FhirImportAnalyzer.analyze(FhirTestFiles.parsed("ips-document-bundle.json"), "summary.json")
        val earlier = source("s1", FhirSourceGrouper.importBaseUri("My GP"), "My GP")

        val planned = MedicalImportPlanner.plan(analysis, ownSources = listOf(earlier), otherSources = emptyList()).single()
        val renamed = MedicalImportPlanner.rename(planned, "My GP", ownSources = listOf(earlier))

        assertThat(planned.target).isEqualTo(MedicalImportTarget.New(FhirSourceGrouper.importBaseUri("summary"), "summary", fromName = true))
        assertThat(renamed.target).isEqualTo(MedicalImportTarget.Existing(earlier))
    }

    @Test
    fun `renaming a source the file names keeps its base URI`() {
        val planned = MedicalImportPlanner.plan(clinicAnalysis, ownSources = emptyList(), otherSources = emptyList()).single()

        val renamed = MedicalImportPlanner.rename(planned, "City Clinic", ownSources = emptyList())

        assertThat(renamed.target).isEqualTo(MedicalImportTarget.New("https://clinic.example/fhir", "City Clinic", fromName = false))
    }

    @Test
    fun `a name this app already uses gets the date, then a number`() {
        val today = LocalDate.of(2026, 9, 29)
        val taken = listOf(source("s1", "https://a.example", "Clinic"))

        assertThat(MedicalImportPlanner.uniqueName("Lab", taken, today)).isEqualTo("Lab")
        assertThat(MedicalImportPlanner.uniqueName("Clinic", taken, today)).isEqualTo("Clinic (2026-09-29)")
        assertThat(MedicalImportPlanner.uniqueName("Clinic", taken + source("s2", "https://b.example", "Clinic (2026-09-29)"), today))
            .isEqualTo("Clinic (2026-09-29) 2")
    }

    @Test
    fun `groups from one file are planned one by one`() {
        val file = FhirFile(
            listOf(
                entry("""{"resourceType":"Condition","id":"1"}""", "https://a.example/fhir/Condition/1"),
                entry("""{"resourceType":"Condition","id":"1"}""", "https://b.example/fhir/Condition/1"),
            ),
        )

        val plans = MedicalImportPlanner.plan(FhirImportAnalyzer.analyze(file, "x.json"), emptyList(), emptyList())

        assertThat(plans.map { it.targetName }).containsExactly("a.example", "b.example")
    }
}
