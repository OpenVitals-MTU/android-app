package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalCategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMedicalRecordsRepository().apply { addSource("clinic", "Clinic") }

    private fun viewModel(category: MedicalCategory?) =
        MedicalCategoryViewModel(repository, mainDispatcherRule.dispatcherProvider, category)

    private fun immunization(id: String, vaccine: String, date: String?) =
        """{"resourceType":"Immunization","id":"$id","status":"completed","vaccineCode":{"text":"$vaccine"}""" +
            (date?.let { ""","occurrenceDateTime":"$it"""" } ?: "") + "}"

    @Test
    fun `records are newest first, undated last, each with its source and status`() = runTest {
        repository.add("clinic", "Immunization", "a", immunization("a", "Tetanus", "2019-03-14"))
        repository.add("clinic", "Immunization", "b", immunization("b", "Influenza", "2023-10-02"))
        repository.add("clinic", "Immunization", "c", immunization("c", "Measles", null))

        val vm = viewModel(MedicalCategory.VACCINES)

        val rows = vm.uiState.value.rows
        assertThat(rows.map { it.title }).containsExactly("Influenza", "Tetanus", "Measles").inOrder()
        assertThat(rows.first().sourceName).isEqualTo("Clinic")
        assertThat(rows.first().status).isEqualTo("completed")
        assertThat(vm.uiState.value.readable).isTrue()
    }

    @Test
    fun `a drug definition shows inside the statement that names it, not as a row`() = runTest {
        repository.add(
            "clinic", "MedicationStatement", "s1",
            """{"resourceType":"MedicationStatement","id":"s1","status":"active","medicationReference":{"reference":"Medication/m1"}}""",
        )
        repository.add("clinic", "Medication", "m1", """{"resourceType":"Medication","id":"m1","code":{"text":"Metformin"}}""")
        repository.add("clinic", "Medication", "m2", """{"resourceType":"Medication","id":"m2","code":{"text":"Unused drug"}}""")

        val rows = viewModel(MedicalCategory.MEDICATIONS).uiState.value.rows

        assertThat(rows.map { it.ref.resourceId }).containsExactly("s1", "m2")
        assertThat(rows.first { it.ref.resourceId == "s1" }.title).isEqualTo("Metformin")
    }

    @Test
    fun `a declined category lists this app's own records and says others need access`() = runTest {
        repository.readable = emptySet()
        repository.add("clinic", "Immunization", "a", immunization("a", "Tetanus", "2019-03-14"))

        val state = viewModel(MedicalCategory.VACCINES).uiState.value

        assertThat(state.rows).hasSize(1)
        assertThat(state.readable).isFalse()
    }

    @Test
    fun `no access at all is a permission error the screen can offer a way out of`() = runTest {
        repository.readable = emptySet()
        repository.writable = false

        val state = viewModel(MedicalCategory.VACCINES).uiState.value

        assertThat(state.error).isEqualTo(ScreenError.PermissionDenied)
    }

    @Test
    fun `a refresh drops a record deleted since the list was read`() = runTest {
        repository.add("clinic", "Immunization", "a", immunization("a", "Tetanus", "2019-03-14"))
        repository.add("clinic", "Immunization", "b", immunization("b", "Influenza", "2023-10-02"))
        val vm = viewModel(MedicalCategory.VACCINES)

        repository.records.removeAll { it.ref.resourceId == "a" }
        vm.refresh()

        assertThat(vm.uiState.value.rows.map { it.title }).containsExactly("Influenza")
    }

    @Test
    fun `an unknown category is a missing argument`() = runTest {
        assertThat(viewModel(null).uiState.value.error).isEqualTo(ScreenError.MissingArgument)
    }
}
