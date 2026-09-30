package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalRecordsHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePreferences(override var firstPermissionRequestDone: Boolean = false) : MedicalRecordsPreferences

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "Clinic")
        addSource("portal", "Portal", packageName = "com.example.portal")
        add("clinic", "Immunization", "i1", "{}")
        add("clinic", "Immunization", "i2", "{}")
        add("portal", "Immunization", "i3", "{}")
        add("portal", "Condition", "c1", "{}")
    }
    private val preferences = FakePreferences()

    private fun viewModel() = MedicalRecordsHomeViewModel(repository, preferences, mainDispatcherRule.dispatcherProvider)

    private fun MedicalRecordsHomeViewModel.row(category: MedicalCategory) = uiState.value.rows.single { it.category == category }

    @Test
    fun `the home counts only the sources this app wrote`() = runTest {
        val vm = viewModel()

        vm.load()

        assertThat(vm.uiState.value.ownSourceCount).isEqualTo(1)
    }

    @Test
    fun `every category gets a row with its count, in care then sensitive order`() = runTest {
        val vm = viewModel()

        vm.load()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.rows.map { it.category }).containsExactlyElementsIn(MedicalCategory.entries).inOrder()
        assertThat(vm.row(MedicalCategory.VACCINES).count).isEqualTo(3)
        assertThat(vm.row(MedicalCategory.CONDITIONS).count).isEqualTo(1)
        assertThat(vm.row(MedicalCategory.PREGNANCY).count).isEqualTo(0)
    }

    @Test
    fun `a declined category counts this app's own records and says so`() = runTest {
        repository.readable = MedicalCategory.entries.toSet() - MedicalCategory.VACCINES
        val vm = viewModel()

        vm.load()

        val vaccines = vm.row(MedicalCategory.VACCINES)
        assertThat(vaccines.count).isEqualTo(2)
        assertThat(vaccines.readable).isFalse()
        assertThat(vaccines.noAccess).isFalse()
    }

    @Test
    fun `with neither read nor write access a category has no access, not zero records`() = runTest {
        repository.readable = emptySet()
        repository.writable = false
        val vm = viewModel()

        vm.load()

        val vaccines = vm.row(MedicalCategory.VACCINES)
        assertThat(vaccines.noAccess).isTrue()
        assertThat(vaccines.count).isNull()
    }

    @Test
    fun `a count that fails for another reason is unknown, and the rest still load`() = runTest {
        repository.failingCategories = setOf(MedicalCategory.CONDITIONS)
        val vm = viewModel()

        vm.load()

        assertThat(vm.row(MedicalCategory.CONDITIONS).count).isNull()
        assertThat(vm.row(MedicalCategory.CONDITIONS).noAccess).isFalse()
        assertThat(vm.row(MedicalCategory.VACCINES).count).isEqualTo(3)
    }

    @Test
    fun `without the feature the home says so and reads nothing`() = runTest {
        repository.available = false
        val vm = viewModel()

        vm.load()

        assertThat(vm.uiState.value.available).isFalse()
        assertThat(vm.uiState.value.rows).isEmpty()
    }

    @Test
    fun `the first permission request happens once`() = runTest {
        val vm = viewModel()
        assertThat(vm.uiState.value.firstRequestDone).isFalse()

        vm.onFirstRequestLaunched()

        assertThat(vm.uiState.value.firstRequestDone).isTrue()
        assertThat(preferences.firstPermissionRequestDone).isTrue()
        assertThat(viewModel().uiState.value.firstRequestDone).isTrue()
    }
}
