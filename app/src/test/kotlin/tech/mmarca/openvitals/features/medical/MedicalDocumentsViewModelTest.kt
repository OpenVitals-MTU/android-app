package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.local.medical.FakeMedicalDocumentDao
import tech.mmarca.openvitals.data.repository.MedicalDocumentsRepositoryImpl
import tech.mmarca.openvitals.data.repository.contract.FakeMedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.util.MainDispatcherRule

class MedicalDocumentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val records = FakeMedicalRecordsRepository().apply {
        addSource("clinic", "Clinic")
        add("clinic", "Immunization", "i1", """{"resourceType":"Immunization","id":"i1"}""")
    }
    private val documents by lazy { MedicalDocumentsRepositoryImpl(folder.root, FakeMedicalDocumentDao(), mainDispatcherRule.dispatcherProvider) }
    private val kept = MedicalRecordRef("clinic", "Immunization", "i1")
    private val deleted = MedicalRecordRef("clinic", "Immunization", "gone")

    @Test
    fun `the list drops links to records deleted since, and counts what is left`() = runTest {
        documents.keep("a.json", "application/json", "json", "{}".toByteArray(), "Clinic", listOf(kept, deleted))

        val state = MedicalDocumentsViewModel(documents, records).uiState.value

        assertThat(state.documents.single().recordCount).isEqualTo(1)
        assertThat(state.totalBytes).isEqualTo(2)
    }

    @Test
    fun `without write access no link is dropped, since this app cannot see its own records`() = runTest {
        records.writable = false
        documents.keep("a.json", "application/json", "json", "{}".toByteArray(), null, listOf(kept, deleted))

        val state = MedicalDocumentsViewModel(documents, records).uiState.value

        assertThat(state.documents.single().recordCount).isEqualTo(2)
    }

    @Test
    fun `deleting a document leaves its records in Health Connect`() = runTest {
        val document = documents.keep("a.json", "application/json", "json", "{}".toByteArray(), null, listOf(kept))
        val vm = MedicalDocumentsViewModel(documents, records)

        vm.delete(document.id)

        assertThat(vm.uiState.value.documents).isEmpty()
        assertThat(records.records.map { it.ref }).containsExactly(kept)
    }
}
