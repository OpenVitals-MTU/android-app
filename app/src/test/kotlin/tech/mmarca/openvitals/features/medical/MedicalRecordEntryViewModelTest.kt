package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.usecase.SaveManualMedicalRecordUseCase
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalRecordEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePreferences(override var firstPermissionRequestDone: Boolean = true) : MedicalRecordsPreferences

    private val repository = FakeMedicalRecordsRepository()
    private val preferences = FakePreferences()
    private val today = LocalDate.of(2026, 9, 29)

    private fun viewModel(kind: ManualRecordKind?, editId: String? = null) =
        MedicalRecordEntryViewModel(repository, preferences, SaveManualMedicalRecordUseCase(repository), kind, editId) { today }

    @Test
    fun `a new vaccine starts dated today and cannot be saved without a name`() = runTest {
        repository.addSource("clinic", "Clinic", packageName = "com.example.clinic")
        repository.add("clinic", "Patient", "p1", """{"resourceType":"Patient","id":"p1","name":[{"family":"Lee"}]}""")
        val vm = viewModel(ManualRecordKind.VACCINE)

        assertThat(vm.uiState.value.draft?.date).isEqualTo(today)
        assertThat(vm.uiState.value.needsIdentity).isFalse()
        assertThat(vm.uiState.value.canSave(today)).isFalse()

        vm.update { it.copy(name = "Tetanus") }
        vm.save("Entered in OpenVitals")

        assertThat(vm.uiState.value.saved).isTrue()
        assertThat(repository.records.map { it.ref.resourceType }).containsAtLeast("Patient", "Immunization")
    }

    @Test
    fun `with no Patient to read, the first entry asks for the owner's name before it saves`() = runTest {
        val vm = viewModel(ManualRecordKind.CONDITION)
        vm.update { it.copy(name = "Asthma") }

        assertThat(vm.uiState.value.needsIdentity).isTrue()
        assertThat(vm.uiState.value.canSave(today)).isFalse()

        vm.updateOwner { it.copy(givenName = "Robin Ann", familyName = "Moss") }
        vm.save("Entered in OpenVitals")

        assertThat(vm.uiState.value.saved).isTrue()
        val owner = repository.records.single { it.ref.resourceType == "Patient" }
        assertThat(owner.json).contains("\"given\":[\"Robin\",\"Ann\"]")
    }

    @Test
    fun `an edit opens with the stored record's fields`() = runTest {
        val first = viewModel(ManualRecordKind.MEDICATION).apply {
            updateOwner { it.copy(familyName = "Moss") }
            update { it.copy(name = "Metformin", detail = "500 mg twice a day", status = "stopped") }
            save("Entered in OpenVitals")
        }
        assertThat(first.uiState.value.saved).isTrue()
        val id = repository.records.single { it.ref.resourceType == "MedicationStatement" }.ref.resourceId

        val edit = viewModel(ManualRecordKind.MEDICATION, editId = id)

        assertThat(edit.uiState.value.isEdit).isTrue()
        assertThat(edit.uiState.value.draft?.name).isEqualTo("Metformin")
        assertThat(edit.uiState.value.draft?.detail).isEqualTo("500 mg twice a day")
        assertThat(edit.uiState.value.draft?.status).isEqualTo("stopped")
    }

    @Test
    fun `without write access nothing saves, and the first request asks for every permission`() = runTest {
        repository.writable = false
        preferences.firstPermissionRequestDone = false
        val vm = viewModel(ManualRecordKind.ALLERGY)
        vm.update { it.copy(name = "Peanut") }

        assertThat(vm.uiState.value.canWrite).isFalse()
        assertThat(vm.uiState.value.canSave(today)).isFalse()
        assertThat(vm.permissionsToRequest()).isEqualTo(repository.permissions)
        vm.onPermissionsRequested()
        assertThat(vm.permissionsToRequest()).isEqualTo(repository.writePermissions)
    }

    @Test
    fun `a link with no kind is a missing argument`() = runTest {
        assertThat(viewModel(null).uiState.value.error).isEqualTo(ScreenError.MissingArgument)
    }
}
