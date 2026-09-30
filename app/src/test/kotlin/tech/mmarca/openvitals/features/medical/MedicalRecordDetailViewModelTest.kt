package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalRecordDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "Clinic", baseUri = "https://clinic.example/fhir")
        addSource("portal", "Portal", packageName = "com.example.portal")
        add(
            "clinic", "Immunization", "i1",
            """{"resourceType":"Immunization","id":"i1","status":"completed","vaccineCode":{"text":"Tdap"},""" +
                """"occurrenceDateTime":"2019-03-14","lotNumber":"L1",""" +
                """"performer":[{"actor":{"reference":"Practitioner/p1"}},{"actor":{"reference":"Practitioner/p2","display":"Nurse Kim"}},""" +
                """{"actor":{"reference":"Practitioner/p3"}}]}""",
        )
        add("clinic", "Practitioner", "p1", """{"resourceType":"Practitioner","id":"p1","name":[{"given":["Sam"],"family":"Rivera"}]}""")
        add("portal", "Condition", "c1", """{"resourceType":"Condition","id":"c1","code":{"text":"Asthma"}}""")
    }

    private fun viewModel(ref: MedicalRecordRef?) =
        MedicalRecordDetailViewModel(repository, mainDispatcherRule.dispatcherProvider, ref)

    private val immunization = MedicalRecordRef("clinic", "Immunization", "i1")

    @Test
    fun `a record shows its title, status, date, details and source`() = runTest {
        val state = viewModel(immunization).uiState.value

        assertThat(state.title).isEqualTo(MedicalValue("Tdap"))
        assertThat(state.status).isEqualTo("completed")
        assertThat(state.date?.text).isEqualTo("2019-03-14")
        assertThat(state.details).contains(MedicalDetailRow(SummaryField.LOT_NUMBER, MedicalValue("L1")))
        assertThat(state.sourceName).isEqualTo("Clinic")
        assertThat(state.sourceUri).isEqualTo("https://clinic.example/fhir")
        assertThat(state.writtenByThisApp).isTrue()
        assertThat(state.rawJson).contains("\n")
    }

    @Test
    fun `references show the named record's name, else its display, else the reference`() = runTest {
        val performers = viewModel(immunization).uiState.value.details.filter { it.field == SummaryField.PERFORMER }

        assertThat(performers.map { it.value.text }).containsExactly("Sam Rivera", "Nurse Kim", "Practitioner/p3").inOrder()
    }

    @Test
    fun `a reference into a declined category says access is needed`() = runTest {
        repository.readable = MedicalCategory.entries.toSet() - MedicalCategory.PRACTITIONER_DETAILS
        repository.writable = false

        val performers = viewModel(immunization).uiState.value.details.filter { it.field == SummaryField.PERFORMER }

        assertThat(performers[0].value).isEqualTo(MedicalValue(null, needsAccess = MedicalCategory.PRACTITIONER_DETAILS))
        assertThat(performers[1].value.text).isEqualTo("Nurse Kim")
    }

    @Test
    fun `a record another app wrote says so`() = runTest {
        val state = viewModel(MedicalRecordRef("portal", "Condition", "c1")).uiState.value

        assertThat(state.writtenByThisApp).isFalse()
        assertThat(state.sourceName).isEqualTo("Portal")
    }

    @Test
    fun `deleting this app's record removes it and closes the screen`() = runTest {
        val vm = viewModel(immunization)

        vm.delete()

        assertThat(vm.uiState.value.deleted).isTrue()
        assertThat(repository.records.map { it.ref }).doesNotContain(immunization)
    }

    @Test
    fun `a delete Health Connect refuses keeps the record and reports why`() = runTest {
        val vm = viewModel(MedicalRecordRef("portal", "Condition", "c1"))

        vm.delete()

        assertThat(vm.uiState.value.deleted).isFalse()
        assertThat(vm.uiState.value.deleteError).isNotNull()
        vm.consumeDeleteError()
        assertThat(vm.uiState.value.deleteError).isNull()
        assertThat(repository.records.map { it.ref }).contains(MedicalRecordRef("portal", "Condition", "c1"))
    }

    @Test
    fun `a record that is gone is not found, and a broken link is a missing argument`() = runTest {
        assertThat(viewModel(MedicalRecordRef("clinic", "Immunization", "gone")).uiState.value.error)
            .isEqualTo(ScreenError.NotFound)
        assertThat(viewModel(null).uiState.value.error).isEqualTo(ScreenError.MissingArgument)
    }
}
