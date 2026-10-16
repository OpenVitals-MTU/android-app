package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirTestFiles.json

/** Manual entries are written as valid FHIR: required fields filled, empty ones left out, other fields kept on edit. */
class ManualFhirWriterTest {

    private val today = LocalDate.of(2026, 9, 29)

    private fun write(draft: ManualRecordDraft, base: JsonObject? = null) = ManualFhirWriter.resource(draft, "id-1", today, base)

    private fun hasEmptyValue(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element.isEmpty() || element.values.any(::hasEmptyValue)
        is JsonArray -> element.isEmpty() || element.any(::hasEmptyValue)
        is JsonPrimitive -> element.isString && element.content.isEmpty()
    }

    @Test
    fun `a save stamps its time, to the second, beside the source`() {
        val record = ManualFhirWriter.resource(
            ManualRecordDraft(ManualRecordKind.ALLERGY, name = "Pollen"),
            "id-1",
            today,
            savedAt = Instant.parse("2026-09-29T10:15:30.123Z"),
        )

        assertThat(record.obj("meta")?.string("lastUpdated")).isEqualTo("2026-09-29T10:15:30Z")
        assertThat(record.obj("meta")?.string("source")).isEqualTo(ManualFhirWriter.SourceBaseUri)
    }

    @Test
    fun `every kind passes pre-flight, points at the owner, and has no empty value`() {
        ManualRecordKind.entries.forEach { kind ->
            val record = write(ManualRecordDraft(kind, name = "Something", date = today))

            assertThat(FhirPreflight.check(PreparedResource(kind.resourceType, "id-1", record))).isNull()
            assertThat(FhirResourceTypes.likelyCategory(record)).isEqualTo(kind.category)
            assertThat(record.toString()).contains("\"reference\":\"Patient/self\"")
            assertThat(record.obj("meta")?.string("source")).isEqualTo(ManualFhirWriter.SourceBaseUri)
            assertThat(hasEmptyValue(record)).isFalse()
        }
    }

    @Test
    fun `a vaccine carries its status, date, name, lot number and code`() {
        val record = write(
            ManualRecordDraft(
                ManualRecordKind.VACCINE,
                name = "Influenza",
                date = LocalDate.of(2025, 10, 2),
                detail = "AB12",
                code = ManualCode("http://hl7.org/fhir/sid/cvx", "140"),
            ),
        )

        assertThat(record.string("status")).isEqualTo("completed")
        assertThat(record.string("occurrenceDateTime")).isEqualTo("2025-10-02")
        assertThat(record.string("lotNumber")).isEqualTo("AB12")
        assertThat(record.obj("vaccineCode")).isEqualTo(
            json("""{"coding":[{"system":"http://hl7.org/fhir/sid/cvx","code":"140"}],"text":"Influenza"}"""),
        )
    }

    @Test
    fun `an allergy and a condition carry a coded clinical status and the day they were recorded`() {
        val allergy = write(ManualRecordDraft(ManualRecordKind.ALLERGY, name = "Peanut", criticality = "high", detail = "Hives"))
        val condition = write(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma", status = "remission"))

        assertThat(allergy.obj("clinicalStatus")?.objects("coding")?.single()?.string("code")).isEqualTo("active")
        assertThat(allergy.string("criticality")).isEqualTo("high")
        assertThat(allergy.objects("reaction").single().objects("manifestation").single().string("text")).isEqualTo("Hives")
        assertThat(allergy.string("recordedDate")).isEqualTo("2026-09-29")
        assertThat(condition.obj("clinicalStatus")?.objects("coding")?.single()?.string("system"))
            .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-clinical")
        assertThat(condition.obj("clinicalStatus")?.objects("coding")?.single()?.string("code")).isEqualTo("remission")
    }

    @Test
    fun `a medication without a start date or dosage leaves both out`() {
        val record = write(ManualRecordDraft(ManualRecordKind.MEDICATION, name = "Metformin"))

        assertThat(record.string("status")).isEqualTo("active")
        assertThat(record.keys).containsNoneOf("effectivePeriod", "dosage", "note")
        assertThat(record.obj("medicationCodeableConcept")).isEqualTo(json("""{"text":"Metformin"}"""))
    }

    @Test
    fun `an edit replaces what the form holds and keeps every other field`() {
        val stored = json(
            """{"resourceType":"Condition","id":"id-1","meta":{"source":"openvitals://manual","versionId":"3"},
               "clinicalStatus":{"coding":[{"system":"x","code":"active"}]},"code":{"text":"Asthma"},
               "subject":{"reference":"Patient/self"},"recordedDate":"2024-01-01","severity":{"text":"Mild"},
               "note":[{"text":"Old note"}]}""",
        )

        val edited = write(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma, allergic", status = "resolved"), base = stored)

        assertThat(edited.obj("code")?.string("text")).isEqualTo("Asthma, allergic")
        assertThat(edited.obj("severity")).isEqualTo(json("""{"text":"Mild"}"""))
        assertThat(edited.string("recordedDate")).isEqualTo("2024-01-01")
        assertThat(edited.obj("meta")?.string("versionId")).isEqualTo("3")
        assertThat(edited.keys).doesNotContain("note")
    }

    @Test
    fun `a stored record reads back into the same form`() {
        val draft = ManualRecordDraft(
            ManualRecordKind.ALLERGY,
            name = "Penicillin",
            status = "inactive",
            date = LocalDate.of(2010, 5, 1),
            detail = "Rash",
            criticality = "low",
            code = ManualCode("http://snomed.info/sct", "91936005"),
            note = "Seen once",
        )

        assertThat(ManualFhirWriter.draftOf(ManualRecordKind.ALLERGY, write(draft))).isEqualTo(draft)
    }

    @Test
    fun `the owner's record leaves out what is not known`() {
        val full = ManualFhirWriter.patient(PatientIdentity(listOf("Robin", "Ann"), listOf("Moss"), "1990-01-01"))
        val nameOnly = ManualFhirWriter.patient(PatientIdentity(emptyList(), listOf("Moss"), null))

        assertThat(full).isEqualTo(
            json(
                """{"resourceType":"Patient","id":"self","meta":{"source":"openvitals://manual"},
                   "name":[{"use":"usual","given":["Robin","Ann"],"family":"Moss"}],"birthDate":"1990-01-01"}""",
            ),
        )
        assertThat(nameOnly.keys).doesNotContain("birthDate")
        assertThat(hasEmptyValue(nameOnly)).isFalse()
    }

    @Test
    fun `a name is needed, a vaccine needs its date, and no date lies ahead`() {
        assertThat(ManualRecordDraft(ManualRecordKind.CONDITION).isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.CONDITION, name = "Asthma").isComplete(today)).isTrue()
        assertThat(ManualRecordDraft(ManualRecordKind.VACCINE, name = "Tetanus").isComplete(today)).isFalse()
        assertThat(ManualRecordDraft(ManualRecordKind.MEDICATION, name = "X", date = today.plusDays(1)).isComplete(today)).isFalse()
    }
}
