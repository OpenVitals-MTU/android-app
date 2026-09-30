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
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.contract.MedicalDocumentsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.model.MedicalCategory

/**
 * One category on the records home. [count] is null when it could not be read. [readable]
 * is false when its read permission is declined: the count is then this app's own records.
 * [noAccess] means neither read nor write access, so nothing can be listed.
 */
data class MedicalCategoryRow(
    val category: MedicalCategory,
    val count: Int?,
    val readable: Boolean,
    val noAccess: Boolean,
)

/** [ownSourceCount] counts the sources this app wrote, which the user can delete. [documentCount] the files it kept. */
data class MedicalRecordsHomeUiState(
    val isLoading: Boolean = true,
    val available: Boolean = true,
    val rows: List<MedicalCategoryRow> = emptyList(),
    val firstRequestDone: Boolean = true,
    val ownSourceCount: Int = 0,
    val documentCount: Int = 0,
    val documentsBytes: Long = 0,
)

/** The records home: one row per category, with its count. The tile opens it. */
@HiltViewModel
class MedicalRecordsHomeViewModel @Inject constructor(
    private val repository: MedicalRecordsRepository,
    private val preferences: MedicalRecordsPreferences,
    private val dispatchers: DispatcherProvider,
    private val documents: MedicalDocumentsRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MedicalRecordsHomeUiState(firstRequestDone = preferences.firstPermissionRequestDone),
    )
    val uiState: StateFlow<MedicalRecordsHomeUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    /** The screen calls this once Health Connect access is known, and again when it changes. */
    fun load() {
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.update { it.copy(isLoading = true) }
            val available = withContext(dispatchers.io) { repository.isAvailable() }
            if (!available) {
                _uiState.update { it.copy(isLoading = false, available = false, rows = emptyList()) }
                return@load
            }
            val readable = runCatching { repository.readableCategories() }
                .getOrElse { if (it is CancellationException) throw it else emptySet() }
            val rows = coroutineScope {
                MedicalCategory.entries.map { category -> async { row(category, category in readable) } }.awaitAll()
            }
            val ownSources = readOrNothing { repository.ownSources() }.size
            val kept = readOrNothing { documents?.documents().orEmpty() }
            if (!isCurrent) return@load
            _uiState.update {
                it.copy(
                    isLoading = false,
                    available = true,
                    rows = rows,
                    ownSourceCount = ownSources,
                    documentCount = kept.size,
                    documentsBytes = kept.sumOf { document -> document.sizeBytes },
                )
            }
        }
    }

    /** Called as the first request goes out, so it never goes out again by itself. */
    fun onFirstRequestLaunched() {
        preferences.firstPermissionRequestDone = true
        _uiState.update { it.copy(firstRequestDone = true) }
    }

    private suspend fun row(category: MedicalCategory, readable: Boolean): MedicalCategoryRow {
        val result = runCatching { repository.count(category) }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        return MedicalCategoryRow(
            category = category,
            count = result.getOrNull(),
            readable = readable,
            noAccess = failure?.isPermissionFailure() == true,
        )
    }
}
