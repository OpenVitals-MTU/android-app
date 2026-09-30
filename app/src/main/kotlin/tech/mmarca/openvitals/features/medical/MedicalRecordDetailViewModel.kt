package tech.mmarca.openvitals.features.medical

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalDocumentsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.Dstu2ToR4
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.FhirResourceTypes
import tech.mmarca.openvitals.domain.medical.FhirSummaries
import tech.mmarca.openvitals.domain.medical.MedicalRecordRows
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.medical.SummaryValue
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCards
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalDocument
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.navigation.MEDICAL_ID_ARG
import tech.mmarca.openvitals.navigation.MEDICAL_SOURCE_ARG
import tech.mmarca.openvitals.navigation.MEDICAL_TYPE_ARG

/**
 * A value to show. [text] is null when it names a record this app may not read: the
 * screen then says access to [needsAccess] is needed. [caption] names a code system.
 */
data class MedicalValue(
    val text: String?,
    val caption: String? = null,
    val needsAccess: MedicalCategory? = null,
)

/** A detail row. [label] replaces the field's own label, as each part of a blood pressure reading does. */
data class MedicalDetailRow(val field: SummaryField, val value: MedicalValue, val label: String? = null)

/** [deleted] is true once the record is gone, so the screen can close. */
data class MedicalRecordDetailUiState(
    val ref: MedicalRecordRef? = null,
    val isLoading: Boolean = true,
    val error: ScreenError? = null,
    val resourceType: String = "",
    val title: MedicalValue? = null,
    val status: String? = null,
    val date: FhirDate? = null,
    val value: String? = null,
    val flag: String? = null,
    val details: List<MedicalDetailRow> = emptyList(),
    val sourceName: String? = null,
    val sourceUri: String? = null,
    val writtenByThisApp: Boolean = false,
    val rawJson: String = "",
    val deleted: Boolean = false,
    val deleteError: ScreenError? = null,
    /** The kept file this record came from, when there is one. */
    val document: MedicalDocument? = null,
    /** The record came from a SMART Health Card whose signature was not checked. */
    val unverifiedCard: Boolean = false,
    /** OpenVitals converted the record from FHIR DSTU2, which Health Connect does not store. */
    val convertedFromDstu2: Boolean = false,
)

/** One record: its curated fields, where it came from, and its raw FHIR. Nothing is computed from it. */
@HiltViewModel
class MedicalRecordDetailViewModel(
    private val repository: MedicalRecordsRepository,
    private val dispatchers: DispatcherProvider,
    private val ref: MedicalRecordRef?,
    private val documents: MedicalDocumentsRepository? = null,
) : ViewModel() {

    @Inject
    constructor(
        repository: MedicalRecordsRepository,
        dispatchers: DispatcherProvider,
        savedStateHandle: SavedStateHandle,
        documents: MedicalDocumentsRepository,
    ) : this(
        repository = repository,
        dispatchers = dispatchers,
        documents = documents,
        ref = run {
            val source = savedStateHandle.get<String>(MEDICAL_SOURCE_ARG)
            val type = savedStateHandle.get<String>(MEDICAL_TYPE_ARG)
            val id = savedStateHandle.get<String>(MEDICAL_ID_ARG)
            if (source.isNullOrBlank() || type.isNullOrBlank() || id.isNullOrBlank()) null else MedicalRecordRef(source, type, id)
        },
    )

    private val _uiState = MutableStateFlow(MedicalRecordDetailUiState(ref = ref, resourceType = ref?.resourceType.orEmpty()))
    val uiState: StateFlow<MedicalRecordDetailUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun load() {
        val ref = ref ?: run {
            _uiState.update { it.copy(isLoading = false, error = ScreenError.MissingArgument) }
            return
        }
        loadCoordinator.launch(viewModelScope) load@{
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { build(ref) }
                .onSuccess { state ->
                    if (isCurrent) _uiState.value = state
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    if (isCurrent) _uiState.update { it.copy(isLoading = false, error = failure.toScreenError(logTag = TAG)) }
                }
        }
    }

    /** Reads the record again without a loading screen, as after an edit. A failed read keeps what shows. */
    fun refresh() {
        val ref = ref ?: return
        val current = _uiState.value
        if (current.isLoading || current.error != null || current.deleted) return
        loadCoordinator.launch(viewModelScope) refresh@{
            val fresh = runCatching { build(ref) }.getOrElse { if (it is CancellationException) throw it else return@refresh }
            if (isCurrent) _uiState.value = fresh
        }
    }

    /** Only this app's records offer it: Health Connect refuses to delete another app's. */
    fun delete() {
        val ref = ref ?: return
        viewModelScope.launch {
            runCatching { repository.delete(listOf(ref)) }
                .onSuccess { _uiState.update { it.copy(deleted = true) } }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _uiState.update { it.copy(deleteError = failure.toScreenError(logTag = TAG)) }
                }
        }
    }

    suspend fun documentFile(): File? = _uiState.value.document?.let { documents?.file(it.id) }

    fun consumeDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    private suspend fun build(ref: MedicalRecordRef): MedicalRecordDetailUiState {
        val record = repository.readRecords(listOf(ref)).firstOrNull()
            ?: return MedicalRecordDetailUiState(ref = ref, isLoading = false, error = ScreenError.NotFound, resourceType = ref.resourceType)
        val summary = withContext(dispatchers.default) { FhirSummaries.summarize(record.json) }
        val readable = readOrNothing { repository.readableCategories() }.toSet()
        val references = (listOfNotNull(summary.title) + summary.details.map { it.value })
            .filterIsInstance<SummaryValue.Reference>()
            .mapNotNull { MedicalRecordRows.refOf(ref.dataSourceId, it.reference) }
            .distinct()
        val names = namesOf(references)
        val source = readOrNothing { repository.allSources() }.firstOrNull { it.id == ref.dataSourceId }
            ?: readOrNothing { repository.ownSources() }.firstOrNull { it.id == ref.dataSourceId }
        val own = readOrNothing { repository.ownSources() }.any { it.id == ref.dataSourceId }
        fun value(value: SummaryValue): MedicalValue = medicalValue(value, ref.dataSourceId, names, readable)
        return MedicalRecordDetailUiState(
            ref = ref,
            isLoading = false,
            resourceType = ref.resourceType,
            title = summary.title?.let(::value),
            status = summary.status,
            date = summary.date,
            value = summary.value,
            flag = summary.flag,
            details = summary.details.map { MedicalDetailRow(it.field, value(it.value), it.label) },
            sourceName = source?.displayName,
            sourceUri = source?.fhirBaseUri,
            writtenByThisApp = own,
            rawJson = withContext(dispatchers.default) { prettyJson(record.json) },
            unverifiedCard = tagged(record.json, SmartHealthCards::isUnverified),
            convertedFromDstu2 = tagged(record.json, Dstu2ToR4::isConverted),
            document = documents?.let { store -> runCatching { store.documentFor(ref) }.getOrElse { if (it is CancellationException) throw it else null } },
        )
    }

    /** Whether the record carries one of OpenVitals' own tags. A record that does not parse carries none. */
    private fun tagged(json: String, check: (JsonObject) -> Boolean): Boolean =
        runCatching { check(Json.parseToJsonElement(json).jsonObject) }.getOrDefault(false)

    /** The title of each named record that exists and can be read. One unreadable record does not hide the rest. */
    private suspend fun namesOf(refs: List<MedicalRecordRef>): Map<MedicalRecordRef, String> {
        if (refs.isEmpty()) return emptyMap()
        val records = runCatching { repository.readRecords(refs) }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            refs.flatMap { ref -> readOrNothing { repository.readRecords(listOf(ref)) } }
        }
        return records.mapNotNull { record -> FhirSummaries.summarize(record.json).titleText?.let { record.ref to it } }.toMap()
    }

    private companion object {
        const val TAG = "MedicalRecordDetail"
    }
}

/**
 * A reference shows the named record's title when it can be read, else its own display
 * text, else "needs access" when the target's category is declined, else the reference.
 */
internal fun medicalValue(
    value: SummaryValue,
    dataSourceId: String,
    names: Map<MedicalRecordRef, String>,
    readable: Set<MedicalCategory>,
): MedicalValue = when (value) {
    is SummaryValue.Text -> MedicalValue(value.text, value.caption)
    is SummaryValue.Reference -> {
        val target = MedicalRecordRows.refOf(dataSourceId, value.reference)
        val name = target?.let { names[it] } ?: value.display
        val category = target?.let { FhirResourceTypes.categoryOfType(it.resourceType) }
        when {
            name != null -> MedicalValue(name)
            category != null && category !in readable -> MedicalValue(null, needsAccess = category)
            else -> MedicalValue(value.reference)
        }
    }
}

private val PrettyJson = Json { prettyPrint = true }

internal fun prettyJson(json: String): String =
    runCatching { PrettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(json)) }.getOrDefault(json)
