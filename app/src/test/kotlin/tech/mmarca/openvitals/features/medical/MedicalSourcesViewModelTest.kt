package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalSourcesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("lab", "City Lab")
        addSource("clinic", "Clinic")
        addSource("portal", "Portal", packageName = "com.example.portal")
        add("clinic", "Immunization", "i1", "{}")
        add("clinic", "AllergyIntolerance", "a1", "{}")
        add("portal", "Condition", "c1", "{}")
    }

    @Test
    fun `this app's sources are listed by name with their record counts, other apps' are not`() = runTest {
        val rows = MedicalSourcesViewModel(repository).uiState.value.rows

        assertThat(rows.map { it.source.displayName to it.recordCount }).containsExactly("City Lab" to 0, "Clinic" to 2).inOrder()
    }

    @Test
    fun `a declined category still counts, since write access reads this app's own records`() = runTest {
        repository.readable = MedicalCategory.entries.toSet() - MedicalCategory.ALLERGIES

        val rows = MedicalSourcesViewModel(repository).uiState.value.rows

        assertThat(rows.single { it.source.id == "clinic" }.recordCount).isEqualTo(2)
    }

    @Test
    fun `with no way to read the records, the count is unknown rather than zero`() = runTest {
        repository.readable = emptySet()
        repository.writable = false

        val rows = MedicalSourcesViewModel(repository).uiState.value.rows

        assertThat(rows.map { it.recordCount }).containsExactly(null, null)
    }

    @Test
    fun `deleting a source removes it and its records, and the list reloads`() = runTest {
        val vm = MedicalSourcesViewModel(repository)

        vm.delete("clinic")

        assertThat(vm.uiState.value.rows.map { it.source.id }).containsExactly("lab")
        assertThat(repository.records.map { it.ref.dataSourceId }).containsExactly("portal")
    }
}
