package tech.mmarca.openvitals.domain.usecase

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.ManualFhirWriter
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.PatientIdentity

/** Manual entries share one source, and its owner's Patient is written once, with the first entry. */
class SaveManualMedicalRecordUseCaseTest {

    private val repository = FakeMedicalRecordsRepository()
    private val save = SaveManualMedicalRecordUseCase(repository)
    private val today = LocalDate.of(2026, 9, 29)
    private val robin = PatientIdentity(listOf("Robin"), listOf("Moss"), "1990-01-01")
    private val vaccine = ManualRecordDraft(ManualRecordKind.VACCINE, name = "Tetanus", date = today)

    private fun typesIn(sourceId: String) = repository.records.filter { it.ref.dataSourceId == sourceId }.map { it.ref.resourceType }

    @Test
    fun `the first entry makes the manual source and writes the owner with it`() = runTest {
        assertThat(save.needsIdentity()).isTrue()

        val result = save(vaccine, today, "Entered in OpenVitals", identity = robin) as ManualSaveResult.Saved

        val source = repository.sources.single()
        assertThat(source.fhirBaseUri).isEqualTo(ManualFhirWriter.SourceBaseUri)
        assertThat(source.displayName).isEqualTo("Entered in OpenVitals")
        assertThat(typesIn(source.id)).containsExactly("Patient", "Immunization")
        assertThat(result.ref.dataSourceId).isEqualTo(source.id)
        assertThat(save.needsIdentity()).isFalse()
    }

    @Test
    fun `later entries reuse the source and write no second owner`() = runTest {
        save(vaccine, today, "Entered in OpenVitals", identity = robin)

        save(ManualRecordDraft(ManualRecordKind.ALLERGY, name = "Peanut"), today, "Entered in OpenVitals")

        assertThat(repository.sources).hasSize(1)
        assertThat(typesIn(repository.sources.single().id)).containsExactly("Patient", "Immunization", "AllergyIntolerance")
    }

    @Test
    fun `the owner comes from a Patient record this app can read when there is one`() = runTest {
        repository.addSource("clinic", "Clinic", packageName = "com.example.clinic")
        repository.add("clinic", "Patient", "p1", """{"resourceType":"Patient","id":"p1","name":[{"given":["Sam"],"family":"Lee"}]}""")

        assertThat(save.needsIdentity()).isFalse()
        save(vaccine, today, "Entered in OpenVitals")

        val owner = repository.records.single { it.ref.resourceId == ManualFhirWriter.PatientId }
        assertThat(owner.json).contains("\"family\":\"Lee\"")
    }

    @Test
    fun `with no name to use, nothing is written and the form is asked to get one`() = runTest {
        val result = save(vaccine, today, "Entered in OpenVitals")

        assertThat(result).isEqualTo(ManualSaveResult.NeedsIdentity)
        assertThat(repository.sources).isEmpty()
        assertThat(repository.records).isEmpty()
    }

    @Test
    fun `an edit keeps the record's id and updates it in place`() = runTest {
        val first = save(vaccine, today, "Entered in OpenVitals", identity = robin) as ManualSaveResult.Saved

        save(vaccine.copy(name = "Tetanus and diphtheria"), today, "Entered in OpenVitals", editId = first.ref.resourceId)

        val stored = repository.records.filter { it.ref.resourceType == "Immunization" }
        assertThat(stored.map { it.ref }).containsExactly(first.ref)
        assertThat(stored.single().json).contains("Tetanus and diphtheria")
    }

    @Test
    fun `a taken name gets the date, as an import's does`() = runTest {
        repository.addSource("other", "Entered in OpenVitals", baseUri = "https://elsewhere.example/fhir")

        save(vaccine, today, "Entered in OpenVitals", identity = robin)

        assertThat(repository.sources.last().displayName).isEqualTo("Entered in OpenVitals (2026-09-29)")
    }
}
