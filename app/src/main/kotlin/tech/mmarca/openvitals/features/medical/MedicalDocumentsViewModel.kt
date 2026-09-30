package tech.mmarca.openvitals.features.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalDocumentsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalDocument

data class MedicalDocumentsUiState(
    val isLoading: Boolean = true,
    val documents: List<MedicalDocument> = emptyList(),
    val totalBytes: Long = 0,
    val error: ScreenError? = null,
)

/** The files kept from imports. Deleting one never deletes its records. */
@HiltViewModel
class MedicalDocumentsViewModel @Inject constructor(
    private val documents: MedicalDocumentsRepository,
    private val records: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalDocumentsUiState())
    val uiState: StateFlow<MedicalDocumentsUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            runCatching {
                dropLinksToDeletedRecords()
                documents.documents() to documents.totalBytes()
            }.onSuccess { (list, total) ->
                if (isCurrent) _uiState.update { it.copy(isLoading = false, documents = list, totalBytes = total, error = null) }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (isCurrent) _uiState.update { it.copy(isLoading = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    suspend fun file(id: String): File? = documents.file(id)

    fun delete(id: String) {
        viewModelScope.launch {
            documents.delete(id)
            load()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            documents.deleteAll()
            load()
        }
    }

    /**
     * A link to a record that no longer exists is dropped. Only with write access: without it
     * this app cannot see its own records, and every link would look dead.
     */
    private suspend fun dropLinksToDeletedRecords() {
        if (!runCatching { records.canWrite() }.getOrElse { if (it is CancellationException) throw it else false }) return
        val linked = documents.linkedRefs()
        if (linked.isEmpty()) return
        val existing = runCatching { linked.chunked(ReadChunk).flatMap { records.readRecords(it) }.map { it.ref }.toSet() }
            .getOrElse { if (it is CancellationException) throw it else return }
        documents.dropLinks(linked - existing)
    }

    private companion object {
        const val TAG = "MedicalDocuments"
        const val ReadChunk = 100
    }
}
