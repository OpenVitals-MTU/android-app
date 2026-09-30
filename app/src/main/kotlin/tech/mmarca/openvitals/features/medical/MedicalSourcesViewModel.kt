package tech.mmarca.openvitals.features.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/** One of this app's sources. [recordCount] is null when it could not be counted. */
data class MedicalSourceRow(val source: MedicalRecordSource, val recordCount: Int?)

data class MedicalSourcesUiState(
    val isLoading: Boolean = true,
    val rows: List<MedicalSourceRow> = emptyList(),
    val error: ScreenError? = null,
    val deleteError: ScreenError? = null,
)

/** The sources this app wrote, each with its record count. Deleting one deletes its records. */
@HiltViewModel
class MedicalSourcesViewModel @Inject constructor(
    private val repository: MedicalRecordsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalSourcesUiState())
    val uiState: StateFlow<MedicalSourcesUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val sources = repository.ownSources().sortedBy { it.displayName.lowercase() }
                val counts = countBySource(sources.mapTo(mutableSetOf()) { it.id })
                sources.map { source -> MedicalSourceRow(source, counts?.let { it[source.id] ?: 0 }) }
            }.onSuccess { rows ->
                if (isCurrent) _uiState.update { it.copy(isLoading = false, rows = rows) }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (isCurrent) _uiState.update { it.copy(isLoading = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    fun delete(sourceId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteSource(sourceId) }
                .onSuccess { load() }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _uiState.update { it.copy(deleteError = failure.toScreenError(logTag = TAG)) }
                }
        }
    }

    fun consumeDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    /** Reads only these sources, one read per category. Null when a read fails: a wrong count is worse than none. */
    private suspend fun countBySource(sourceIds: Set<String>): Map<String, Int>? {
        if (sourceIds.isEmpty()) return emptyMap()
        return runCatching {
            coroutineScope {
                MedicalCategory.entries.map { async { repository.readCategory(it, sourceIds) } }.awaitAll()
            }.flatten().groupingBy { it.ref.dataSourceId }.eachCount()
        }.getOrElse { if (it is CancellationException) throw it else null }
    }

    private companion object {
        const val TAG = "MedicalSources"
    }
}
