package tech.mmarca.openvitals.features.imports.medical

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.local.medical.FakeMedicalDocumentDao
import tech.mmarca.openvitals.data.repository.MedicalDocumentsRepositoryImpl
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.domain.medical.FhirFileImportSource
import tech.mmarca.openvitals.domain.medical.FhirTestFiles
import tech.mmarca.openvitals.domain.medical.MedicalImportTarget
import tech.mmarca.openvitals.domain.medical.PatientCheckResult
import tech.mmarca.openvitals.domain.medical.cda.EstonianCdaFixtures
import tech.mmarca.openvitals.domain.medical.cda.CdaImportSource
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCardFixtures
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCardImportSource
import tech.mmarca.openvitals.domain.usecase.ImportMedicalRecordsUseCase
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private class FakePreferences(override var firstPermissionRequestDone: Boolean = true) : MedicalRecordsPreferences

    private val repository = FakeMedicalRecordsRepository()
    private val preferences = FakePreferences()
    private val sample = FhirTestFiles.text("spike-sample-bundle.json")

    private fun viewModel() = MedicalImportViewModel(
        repository = repository,
        preferences = preferences,
        importRecords = ImportMedicalRecordsUseCase(repository),
        dispatchers = mainDispatcherRule.dispatcherProvider,
        source = FhirFileImportSource,
        today = { LocalDate.of(2026, 9, 29) },
    )

    @Test
    fun `a FHIR file is analysed into groups to review, and nothing is written`() = runTest {
        val vm = viewModel()

        vm.analyze("sample.json") { sample.byteInputStream() }

        val state = vm.uiState.value
        assertThat(state.step).isEqualTo(MedicalImportStep.REVIEW)
        assertThat(state.plans.single().target).isInstanceOf(MedicalImportTarget.New::class.java)
        assertThat(state.recordsToWrite).isEqualTo(12)
        assertThat(state.patientCheck).isEqualTo(PatientCheckResult.Pass)
        assertThat(state.canContinue).isTrue()
        assertThat(repository.records).isEmpty()
    }

    @Test
    fun `a file that is not FHIR, or holds nothing Health Connect stores, is refused`() = runTest {
        val vm = viewModel()

        vm.analyze("steps.csv") { "date,steps\n2024-01-01,100".byteInputStream() }
        assertThat(vm.uiState.value.pickError).isEqualTo(MedicalImportPickError.NOT_SUPPORTED)

        vm.analyze("report.json") { """{"resourceType":"DiagnosticReport","id":"r1","status":"final","code":{"text":"x"}}""".byteInputStream() }
        assertThat(vm.uiState.value.pickError).isEqualTo(MedicalImportPickError.NO_RECORDS)
        assertThat(vm.uiState.value.step).isEqualTo(MedicalImportStep.PICK)
    }

    @Test
    fun `a file about someone else stops until the user says the records are theirs`() = runTest {
        repository.addSource("mine", "My clinic")
        repository.add(
            "mine", "Patient", "me",
            """{"resourceType":"Patient","id":"me","name":[{"given":["Robin"],"family":"Moss"}],"birthDate":"1990-01-01"}""",
        )
        val vm = viewModel()

        vm.analyze("sample.json") { sample.byteInputStream() }
        assertThat(vm.uiState.value.patientCheck).isInstanceOf(PatientCheckResult.Mismatch::class.java)
        assertThat(vm.uiState.value.canContinue).isFalse()

        vm.confirmPatient()
        assertThat(vm.uiState.value.canContinue).isTrue()
    }

    @Test
    fun `without access to personal details the check asks instead of comparing`() = runTest {
        repository.readable = repository.readable - tech.mmarca.openvitals.domain.model.MedicalCategory.PERSONAL_DETAILS
        val vm = viewModel()

        vm.analyze("sample.json") { sample.byteInputStream() }

        assertThat(vm.uiState.value.patientCheck).isInstanceOf(PatientCheckResult.CannotCompare::class.java)
    }

    @Test
    fun `an origin another app holds starts left out`() = runTest {
        repository.addSource("theirs", "Clinic portal", packageName = "com.example.portal", baseUri = "https://clinic.example/fhir")
        val vm = viewModel()

        vm.analyze("sample.json") { sample.byteInputStream() }

        assertThat(vm.uiState.value.plans.single().include).isFalse()
        assertThat(vm.uiState.value.canContinue).isFalse()
        vm.setIncluded(0, true)
        assertThat(vm.uiState.value.recordsToWrite).isEqualTo(12)
    }

    @Test
    fun `the import writes, reports, and a second run of the same file only updates`() = runTest {
        val vm = viewModel()
        vm.analyze("sample.json") { sample.byteInputStream() }
        vm.goToConfirm()
        vm.rename(0, "City Clinic")

        vm.startImport()

        val result = requireNotNull(vm.uiState.value.result)
        assertThat(vm.uiState.value.step).isEqualTo(MedicalImportStep.DONE)
        assertThat(result.written).isEqualTo(12)
        assertThat(repository.sources.single().displayName).isEqualTo("City Clinic")
        assertThat(vm.reportText()).contains("sample.json")

        vm.reset()
        vm.analyze("sample.json") { sample.byteInputStream() }
        assertThat(vm.uiState.value.plans.single().target).isInstanceOf(MedicalImportTarget.Existing::class.java)
        vm.goToConfirm()
        vm.startImport()
        assertThat(vm.uiState.value.result?.updated).isEqualTo(12)
        assertThat(repository.sources).hasSize(1)
    }

    @Test
    fun `an Apple Health export goes through the same review, one source per provider and version, DSTU2 converted`() = runTest {
        val export = AppleExportZips.zip(
            AppleExportZips.exportXml(
                AppleExportZips.clinicalRecord("A-1", "Hospital A", "https://a.example/r4", "4.0.1"),
                AppleExportZips.clinicalRecord("A-2", "Hospital A", "https://a.example/r4", "4.3.0"),
                AppleExportZips.clinicalRecord("B-1", "Hospital B", "https://b.example/r4", "4.0.1"),
                AppleExportZips.clinicalRecord("OLD-1", "Old Clinic", "https://old.example/dstu2", "1.0.2"),
            ),
            mapOf(
                "A-1" to AppleExportZips.immunization("imm-1"),
                "A-2" to AppleExportZips.immunization("imm-2"),
                "B-1" to AppleExportZips.immunization("imm-1"),
                "OLD-1" to AppleExportZips.immunization("old"),
            ),
        )
        val vm = MedicalImportViewModel(
            repository, preferences, ImportMedicalRecordsUseCase(repository), mainDispatcherRule.dispatcherProvider,
            PickedFileImportSource(SmartHealthCardImportSource(SmartHealthCardFixtures.FixedRasterizer())), { LocalDate.of(2026, 9, 29) },
        )

        vm.analyze("export.zip") { export.inputStream() }
        vm.goToConfirm()
        vm.startImport()

        val state = vm.uiState.value
        assertThat(state.plans.map { it.targetName }).containsExactly("Hospital A", "Hospital A (FHIR 4.3.0)", "Hospital B", "Old Clinic")
        assertThat(state.sourceSkips).isEmpty()
        assertThat(state.result?.written).isEqualTo(4)
        assertThat(state.result?.skipped).isEqualTo(0)
        assertThat(repository.sources.map { it.fhirBaseUri to it.fhirVersion }).containsExactly(
            "https://a.example/r4" to "4.0.1",
            "https://a.example/r4" to "4.3.0",
            "https://b.example/r4" to "4.0.1",
            "https://old.example/dstu2" to "4.0.1",
        )

        // A second import of the same export finds each source again.
        vm.reset()
        vm.analyze("export.zip") { export.inputStream() }
        vm.goToConfirm()
        vm.startImport()
        assertThat(vm.uiState.value.result?.updated).isEqualTo(4)
        assertThat(repository.sources).hasSize(4)
    }

    @Test
    fun `the copy is kept only when asked, and links to the records the import wrote`() = runTest {
        val documents = MedicalDocumentsRepositoryImpl(folder.root, FakeMedicalDocumentDao(), mainDispatcherRule.dispatcherProvider)
        val vm = MedicalImportViewModel(
            repository, preferences, ImportMedicalRecordsUseCase(repository), mainDispatcherRule.dispatcherProvider,
            FhirFileImportSource, { LocalDate.of(2026, 9, 29) }, documents,
        )
        vm.analyze("sample.json") { sample.byteInputStream() }
        assertThat(vm.uiState.value.documentSize).isEqualTo(sample.toByteArray().size.toLong())
        assertThat(vm.uiState.value.keepDocument).isFalse()

        vm.goToConfirm()
        vm.setKeepDocument(true)
        vm.startImport()

        assertThat(vm.uiState.value.documentKept).isTrue()
        val kept = documents.documents().single()
        assertThat(kept.fileName).isEqualTo("sample.json")
        assertThat(kept.recordCount).isEqualTo(12)

        // The next import starts with the switch off again.
        vm.reset()
        vm.analyze("sample.json") { sample.byteInputStream() }
        assertThat(vm.uiState.value.keepDocument).isFalse()
        vm.goToConfirm()
        vm.startImport()
        assertThat(vm.uiState.value.documentKept).isNull()
        assertThat(documents.documents()).hasSize(1)
    }

    @Test
    fun `a portal export keeps each PDF with only the records of its own document`() = runTest {
        val documents = MedicalDocumentsRepositoryImpl(folder.root, FakeMedicalDocumentDao(), mainDispatcherRule.dispatcherProvider)
        val zip = EstonianCdaFixtures.zip(EstonianCdaFixtures.all)
        val vm = MedicalImportViewModel(
            repository, preferences, ImportMedicalRecordsUseCase(repository), mainDispatcherRule.dispatcherProvider,
            CdaImportSource, { LocalDate.of(2026, 9, 30) }, documents,
        )
        vm.analyze("terviseportaal.zip") { zip.inputStream() }
        assertThat(vm.uiState.value.documentCount).isEqualTo(7)

        vm.goToConfirm()
        vm.setKeepDocument(true)
        vm.startImport()

        assertThat(vm.uiState.value.documentKept).isTrue()
        assertThat(vm.uiState.value.keptCount).isEqualTo(7)
        val dental = documents.documents().single { it.fileName == "hambaravi.pdf" }
        assertThat(dental.recordCount).isEqualTo(3)
        assertThat(dental.sourceName).isEqualTo("Näidishambaravi OÜ")
        // A declaration holds no record and is kept on its own.
        assertThat(documents.documents().single { it.fileName == "tahteavaldus.pdf" }.recordCount).isEqualTo(0)
    }

    @Test
    fun `nothing is imported without write access`() = runTest {
        repository.writable = false
        val vm = viewModel()
        vm.analyze("sample.json") { sample.byteInputStream() }
        vm.goToConfirm()

        vm.startImport()

        assertThat(vm.uiState.value.step).isEqualTo(MedicalImportStep.CONFIRM)
        assertThat(repository.records).isEmpty()
    }

    @Test
    fun `the first request asks for every medical permission, later ones only for write`() = runTest {
        preferences.firstPermissionRequestDone = false
        val vm = viewModel()

        assertThat(vm.permissionsToRequest()).isEqualTo(repository.permissions)
        vm.onPermissionsRequested()

        assertThat(preferences.firstPermissionRequestDone).isTrue()
        assertThat(vm.permissionsToRequest()).isEqualTo(repository.writePermissions)
    }
}
