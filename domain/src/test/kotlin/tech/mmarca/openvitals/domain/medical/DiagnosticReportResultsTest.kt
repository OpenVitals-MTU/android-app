package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.entry
import tech.mmarca.openvitals.domain.model.MedicalCategory

/** Health Connect stores no DiagnosticReport, so the results one holds inside itself are imported as records of their own. */
class DiagnosticReportResultsTest {

    private fun analyze(vararg json: String): FhirImportGroup =
        FhirImportAnalyzer.analyze(FhirFile(json.map { entry(it) }), "report.json").groups.single()

    private fun FhirImportGroup.record(key: String): JsonObject = ready.single { "${it.type}/${it.id}" == key }.json

    private fun JsonObject.reference(key: String): String? = obj(key)?.string("reference")

    @Test
    fun `a report's contained results become lab results, with the report's patient, time and performer where they have none`() {
        val group = analyze(
            """{"resourceType":"DiagnosticReport","id":"r1","status":"final",
               "category":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0074","code":"LAB"}]}],
               "code":{"text":"Lipid panel"},"subject":{"reference":"Patient/p"},
               "effectiveDateTime":"2026-09-01T08:00:00Z","issued":"2026-09-02T10:00:00Z","performer":[{"reference":"#lab"}],
               "result":[{"reference":"#panel"}],
               "contained":[
                 {"resourceType":"Organization","id":"lab","name":"City Lab"},
                 {"resourceType":"Specimen","id":"s","type":{"text":"Serum"}},
                 {"resourceType":"Observation","id":"panel","status":"final","code":{"text":"Lipids"},"hasMember":[{"reference":"#ldl"}]},
                 {"resourceType":"Observation","id":"ldl","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"13457-7"}]},
                  "valueQuantity":{"value":2.9,"unit":"mmol/L"},"specimen":{"reference":"#s"},"effectiveDateTime":"2026-08-31T07:30:00Z"}]}""",
        )
        val panel = group.record("Observation/r1-panel")
        val ldl = group.record("Observation/r1-ldl")

        assertThat(group.ready.map { "${it.type}/${it.id}" }).containsExactly("Observation/r1-panel", "Observation/r1-ldl", "Organization/r1-lab")
        assertThat(group.rejected).isEmpty()
        assertThat(group.skippedTypes).containsExactly("DiagnosticReport", 1)
        assertThat(group.likelyCategories).containsExactly(MedicalCategory.LAB_RESULTS, 2, MedicalCategory.VISITS, 1)
        assertThat(panel.getValue("hasMember").jsonArray.single().jsonObject.string("reference")).isEqualTo("Observation/r1-ldl")
        assertThat(panel.string("effectiveDateTime")).isEqualTo("2026-09-01T08:00:00Z")
        assertThat(ldl.string("effectiveDateTime")).isEqualTo("2026-08-31T07:30:00Z")
        assertThat(listOf(panel, ldl).map { it.reference("subject") }).containsExactly("Patient/p", "Patient/p")
        assertThat(ldl.getValue("performer").jsonArray.single().jsonObject.string("reference")).isEqualTo("Organization/r1-lab")
        // Health Connect stores no Specimen: the reference keeps its name only.
        assertThat(ldl.getValue("specimen").jsonObject).isEqualTo(FhirTestFiles.json("""{"display":"Serum"}"""))
        assertThat(group.notes.filterIsInstance<FhirIdNote.ContainedLifted>().map { it.parentType to it.id })
            .containsExactly("DiagnosticReport" to "r1-panel", "DiagnosticReport" to "r1-ldl", "DiagnosticReport" to "r1-lab")
    }

    @Test
    fun `the results of a DSTU2 lab report are converted with it, and sorted as lab results though their codes are the lab's own`() {
        val group = analyze(
            """{"resourceType":"DiagnosticReport","id":"lc1","status":"final",
               "category":{"coding":[{"system":"http://hl7.org/fhir/v2/0074","code":"LAB"}]},
               "code":{"coding":[{"system":"http://labcorp.com/ers/testCodes","code":"322000","display":"Comp. Metabolic Panel (14)"}]},
               "subject":{"reference":"Patient/p"},"effectiveDateTime":"2026-05-01","performer":{"display":"LabCorp"},
               "result":[{"reference":"#o1"}],
               "contained":[{"resourceType":"Observation","id":"o1","status":"final",
                 "code":{"coding":[{"system":"http://labcorp.com/ers/testCodes","code":"001370","display":"Glucose"}]},
                 "valueQuantity":{"value":92,"unit":"mg/dL"}}]}""",
        )
        val glucose = group.record("Observation/lc1-o1")

        assertThat(group.rejected).isEmpty()
        assertThat(Dstu2ToR4.isConverted(glucose)).isTrue()
        assertThat(FhirResourceTypes.likelyCategory(glucose)).isEqualTo(MedicalCategory.LAB_RESULTS)
        assertThat(glucose.getValue("performer").jsonArray.single().jsonObject.string("display")).isEqualTo("LabCorp")
        assertThat(glucose.string("effectiveDateTime")).isEqualTo("2026-05-01")
    }

    @Test
    fun `a report that points at its results lifts nothing, and one outside the lab gives its results no category`() {
        val pointing = analyze(
            """{"resourceType":"DiagnosticReport","id":"r2","status":"final","code":{"text":"Lipid panel"},"result":[{"reference":"Observation/ldl"}]}""",
        )
        val imaging = analyze(
            """{"resourceType":"DiagnosticReport","id":"r3","status":"final",
               "category":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0074","code":"RAD"}]}],"code":{"text":"Chest X-ray"},
               "contained":[{"resourceType":"Observation","id":"f","status":"final","code":{"coding":[{"system":"http://snomed.info/sct","code":"168731009"}]}}],
               "result":[{"reference":"#f"}]}""",
        )

        assertThat(pointing.ready).isEmpty()
        assertThat(pointing.notes).isEmpty()
        assertThat(pointing.skippedTypes).containsExactly("DiagnosticReport", 1)
        assertThat(imaging.rejected.single().problem.reason).isEqualTo(FhirRejection.UNCLASSIFIABLE_OBSERVATION)
        assertThat(imaging.rejected.single().resource.json.keys).doesNotContain("category")
    }
}
