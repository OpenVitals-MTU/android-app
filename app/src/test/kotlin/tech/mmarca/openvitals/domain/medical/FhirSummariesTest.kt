package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Records show as received: values and flags come from the source, nothing is computed. */
class FhirSummariesTest {

    private fun sample(type: String) =
        FhirTestFiles.parsed("spike-sample-bundle.json").entries.first { it.type == type }.resource

    private fun summary(json: String) = FhirSummaries.summarize(FhirTestFiles.json(json))

    @Test
    fun `an immunization shows its vaccine, date, status and lot`() {
        val summary = FhirSummaries.summarize(sample("Immunization"))

        assertThat(summary.title).isEqualTo(SummaryValue.Text("Tetanus, diphtheria and pertussis"))
        assertThat(summary.date).isEqualTo(FhirDate("2019-03-14"))
        assertThat(summary.status).isEqualTo("completed")
        assertThat(summary.details).contains(SummaryDetail(SummaryField.LOT_NUMBER, SummaryValue.Text("TD-2019-44")))
    }

    @Test
    fun `a coded value falls back to the display, then to the code with its system`() {
        val display = summary("""{"resourceType":"Condition","code":{"coding":[{"system":"s","code":"1","display":"Asthma"}]}}""")
        val bare = summary("""{"resourceType":"Condition","code":{"coding":[{"system":"http://snomed.info/sct","code":"195967001"}]}}""")

        assertThat(display.title).isEqualTo(SummaryValue.Text("Asthma"))
        assertThat(bare.title).isEqualTo(SummaryValue.Text("195967001", caption = "http://snomed.info/sct"))
    }

    @Test
    fun `an allergy lists its reactions and criticality`() {
        val summary = FhirSummaries.summarize(sample("AllergyIntolerance"))

        assertThat(summary.status).isEqualTo("active")
        assertThat(summary.details).containsAtLeast(
            SummaryDetail(SummaryField.CRITICALITY, SummaryValue.Text("high")),
            SummaryDetail(SummaryField.REACTION, SummaryValue.Text("Hives")),
        )
    }

    @Test
    fun `a condition marked entered in error says so as its status`() {
        val summary = summary(
            """{"resourceType":"Condition","clinicalStatus":{"coding":[{"code":"active"}]},""" +
                """"verificationStatus":{"coding":[{"code":"entered-in-error"}]},"code":{"text":"Asthma"}}""",
        )

        assertThat(summary.status).isEqualTo("entered-in-error")
    }

    @Test
    fun `a lab result shows value, unit and range as written`() {
        val summary = FhirSummaries.summarize(sample("Observation"))

        assertThat(summary.title).isEqualTo(SummaryValue.Text("Hemoglobin A1c"))
        assertThat(summary.value).isEqualTo("5.4 %")
        assertThat(summary.flag).isNull()
        assertThat(summary.details).contains(SummaryDetail(SummaryField.REFERENCE_RANGE, SummaryValue.Text("4.0 to 5.6 %")))
    }

    @Test
    fun `a flag appears only when the source set one`() {
        val summary = summary(
            """{"resourceType":"Observation","code":{"text":"Glucose"},"valueQuantity":{"value":7.9,"unit":"mmol/L"},""" +
                """"interpretation":[{"coding":[{"code":"H","display":"High"}]}],""" +
                """"referenceRange":[{"low":{"value":3.9,"unit":"mmol/L"},"high":{"value":5.5,"unit":"mmol/L"}}]}""",
        )

        assertThat(summary.flag).isEqualTo("High")
        assertThat(summary.value).isEqualTo("7.9 mmol/L")
        assertThat(summary.details).contains(SummaryDetail(SummaryField.REFERENCE_RANGE, SummaryValue.Text("3.9 mmol/L – 5.5 mmol/L")))
    }

    @Test
    fun `a blood pressure reading shows each component`() {
        val bloodPressure = FhirTestFiles.parsed("spike-sample-bundle.json").entries
            .first { it.resource.fhirId == "obs-vital-1" }.resource

        val summary = FhirSummaries.summarize(bloodPressure)

        assertThat(summary.value).isEqualTo("118 mmHg / 76 mmHg")
        assertThat(summary.details.filter { it.field == SummaryField.COMPONENT }.map { it.label })
            .containsExactly("Systolic", "Diastolic")
    }

    @Test
    fun `a medication statement titled by a reference keeps it for the screen to resolve`() {
        val summary = summary(
            """{"resourceType":"MedicationStatement","status":"active",""" +
                """"medicationReference":{"reference":"Medication/m1"},"dosage":[{"text":"Once a day"}]}""",
        )

        assertThat(summary.title).isEqualTo(SummaryValue.Reference("Medication/m1", null))
        assertThat(summary.details).contains(SummaryDetail(SummaryField.DOSAGE, SummaryValue.Text("Once a day")))
    }

    @Test
    fun `an encounter falls back to its class and shows its provider`() {
        val summary = FhirSummaries.summarize(sample("Encounter"))

        assertThat(summary.title).isEqualTo(SummaryValue.Text("AMB", caption = "http://terminology.hl7.org/CodeSystem/v3-ActCode"))
        assertThat(summary.date?.sortKey).isEqualTo("2024-01-20T09:00:00")
        assertThat(summary.details).contains(SummaryDetail(SummaryField.SERVICE_PROVIDER, SummaryValue.Reference("Organization/org-1", null)))
    }

    @Test
    fun `a patient shows a name and birth date, never identifiers`() {
        val summary = summary(
            """{"resourceType":"Patient","identifier":[{"value":"12345678Z"}],""" +
                """"name":[{"given":["Alex"],"family":"Doe"}],"birthDate":"1985-04-12"}""",
        )

        assertThat(summary.titleText).isEqualTo("Alex Doe")
        assertThat(summary.details.map { it.value }).doesNotContain(SummaryValue.Text("12345678Z"))
    }

    @Test
    fun `a record with none of its fields is untitled, not an error`() {
        val summary = summary("""{"resourceType":"Procedure","id":"p1"}""")

        assertThat(summary.title).isNull()
        assertThat(summary.date).isNull()
        assertThat(summary.details).isEmpty()
    }

    @Test
    fun `newest first, a partial date sorts as its first day, undated records last by title`() {
        val summaries = listOf(
            summary("""{"resourceType":"Condition","code":{"text":"b undated"}}"""),
            summary("""{"resourceType":"Condition","code":{"text":"2020"},"onsetDateTime":"2020"}"""),
            summary("""{"resourceType":"Condition","code":{"text":"2020-03"},"onsetDateTime":"2020-03"}"""),
            summary("""{"resourceType":"Condition","code":{"text":"A undated"}}"""),
            summary("""{"resourceType":"Condition","code":{"text":"2021-01-05"},"recordedDate":"2021-01-05"}"""),
        )

        assertThat(summaries.sortedWith(MedicalRecordOrder).map { it.titleText })
            .containsExactly("2021-01-05", "2020-03", "2020", "A undated", "b undated").inOrder()
        assertThat(FhirDate("2020").sortKey).isEqualTo("2020-01-01")
        assertThat(FhirDate("2020").text).isEqualTo("2020")
    }
}
