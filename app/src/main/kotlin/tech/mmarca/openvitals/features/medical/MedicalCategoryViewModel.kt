package tech.mmarca.openvitals.features.medical

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.FhirSummaries
import tech.mmarca.openvitals.domain.medical.MedicalRecordOrder
import tech.mmarca.openvitals.domain.medical.MedicalRecordRows
import tech.mmarca.openvitals.domain.medical.MedicalRecordSummary
import tech.mmarca.openvitals.domain.medical.SummaryValue
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.domain.model.MedicalRecordSource
import tech.mmarca.openvitals.navigation.MEDICAL_CATEGORY_ARG

/** One record in a category list. A null [title] shows as untitled. [status] is the FHIR code. */
data class MedicalRecordRow(
    val ref: MedicalRecordRef,
    val title: String?,
    val date: FhirDate?,
    val sourceName: String?,
    val status: String?,
)

/** [readable] is false when the category's read permission is declined: the list then holds this app's own records only. */
data class MedicalCategoryUiState(
    val category: MedicalCategory? = null,
    val isLoading: Boolean = true,
    val rows: List<MedicalRecordRow> = emptyList(),
    val readable: Boolean = true,
    val error: ScreenError? = null,
)

/** Every record of one category, newest first. Health Connect has no sort order, so this sorts. */
@HiltViewModel
class MedicalCategoryViewModel(
    private val repository: MedicalRecordsRepository,
    private val dispatchers: DispatcherProvider,
    private val category: MedicalCategory?,
) : ViewModel() {

    @Inject
    constructor(
        repository: MedicalRecordsRepository,
        dispatchers: DispatcherProvider,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        dispatchers = dispatchers,
        category = savedStateHandle.get<String>(MEDICAL_CATEGORY_ARG)
            ?.let { name -> MedicalCategory.entries.firstOrNull { it.name == name } },
    )

    private val _uiState = MutableStateFlow(MedicalCategoryUiState(category = category))
    val uiState: StateFlow<MedicalCategoryUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    /** Reads the list again when the screen returns, unless a read is already running. The rows stay while it runs. */
    fun refresh() {
        if (!_uiState.value.isLoading) load()
    }

    fun load() {
        val category = category ?: run {
            _uiState.update { it.copy(isLoading = false, error = ScreenError.MissingArgument) }
            return
        }
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val records = repository.readCategory(category)
                val readable = category in readOrNothing { repository.readableCategories() }
                val sources = sourceNames(readOrNothing { repository.allSources() }.ifEmpty { readOrNothing { repository.ownSources() } })
                val rows = withContext(dispatchers.default) { medicalRecordRows(records, sources) }
                rows to readable
            }.onSuccess { (rows, readable) ->
                if (!isCurrent) return@load
                _uiState.update { it.copy(isLoading = false, rows = rows, readable = readable) }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                if (!isCurrent) return@load
                _uiState.update { it.copy(isLoading = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    private companion object {
        const val TAG = "MedicalCategory"
    }
}

/** The list rows for [records]: events only, newest first, each titled by itself or by the record it names. */
internal fun medicalRecordRows(records: List<MedicalRecord>, sourceNames: Map<String, String>): List<MedicalRecordRow> {
    val summaries = records.associate { it.ref to FhirSummaries.summarize(it.json) }
    return MedicalRecordRows.rows(records)
        .map { record -> record to summaries.getValue(record.ref) }
        .sortedWith(compareBy(MedicalRecordOrder) { it.second })
        .map { (record, summary) ->
            MedicalRecordRow(
                ref = record.ref,
                title = titleOf(summary, record.ref.dataSourceId, summaries),
                date = summary.date,
                sourceName = sourceNames[record.ref.dataSourceId],
                status = summary.status,
            )
        }
}

/** A reference title shows the named record's own title, else the reference's display text. */
private fun titleOf(
    summary: MedicalRecordSummary,
    dataSourceId: String,
    summaries: Map<MedicalRecordRef, MedicalRecordSummary>,
): String? = when (val title = summary.title) {
    is SummaryValue.Reference ->
        MedicalRecordRows.refOf(dataSourceId, title.reference)?.let { summaries[it]?.titleText } ?: title.display
    is SummaryValue.Text -> title.text
    null -> null
}

internal fun sourceNames(sources: Collection<MedicalRecordSource>): Map<String, String> =
    sources.associate { it.id to it.displayName }

/** A secondary read whose failure should not fail the screen: its answer is only a nicety. */
internal suspend fun <T> readOrNothing(block: suspend () -> Collection<T>): Collection<T> =
    runCatching { block() }.getOrElse { if (it is CancellationException) throw it else emptyList() }
