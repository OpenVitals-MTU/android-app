package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.usecase.ExportMedicalRecordsUseCase
import tech.mmarca.openvitals.domain.usecase.MedicalExportScope
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalExportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val repository = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "Clinic")
        add("clinic", "Immunization", "i1", """{"resourceType":"Immunization","id":"i1"}""")
    }

    private fun viewModel() = MedicalExportViewModel(
        exportRecords = ExportMedicalRecordsUseCase(repository),
        files = MedicalExportFiles(folder.root),
        dispatchers = mainDispatcherRule.dispatcherProvider,
        appVersion = "2.12.0",
        now = { Instant.parse("2026-09-29T10:00:00Z") },
    )

    @Test
    fun `an export stages one dated FHIR file and says how many records it holds`() = runTest {
        val vm = viewModel()

        vm.export(MedicalExportScope.All)

        val ready = vm.state.value as MedicalExportUiState.Ready
        assertThat(ready.file.name).isEqualTo("openvitals-medical-records-2026-09-29.json")
        assertThat(ready.file.readText()).contains("\"fullUrl\":\"https://clinic.example/fhir/Immunization/i1\"")
        assertThat(ready.recordCount).isEqualTo(1)
    }

    @Test
    fun `a category export is named after it and replaces the file before`() = runTest {
        val vm = viewModel()
        vm.export(MedicalExportScope.All)

        vm.export(MedicalExportScope.Category(MedicalCategory.VACCINES))

        val ready = vm.state.value as MedicalExportUiState.Ready
        assertThat(ready.file.name).isEqualTo("openvitals-medical-records-vaccines-2026-09-29.json")
        assertThat(folder.root.listFiles().orEmpty().map { it.name }).containsExactly(ready.file.name)
    }

    @Test
    fun `a record that can no longer be read exports nothing, and the dialog closes`() = runTest {
        repository.readable = emptySet()
        repository.writable = false
        val vm = viewModel()

        vm.export(MedicalExportScope.Record(MedicalRecordRef("clinic", "Immunization", "i1")))
        val ready = vm.state.value as MedicalExportUiState.Ready
        vm.dismiss()

        assertThat(ready.recordCount).isEqualTo(0)
        assertThat(vm.state.value).isNull()
    }
}
