package tech.mmarca.openvitals.features.imports.medical

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.contract.MedicalDocumentsRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsPreferences
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.MedicalImportGroupPlan
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.medical.MedicalImportProgress
import tech.mmarca.openvitals.domain.medical.MedicalImportReport
import tech.mmarca.openvitals.domain.medical.MedicalImportResult
import tech.mmarca.openvitals.domain.medical.MedicalImportSource
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceDocument
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkip
import tech.mmarca.openvitals.domain.medical.PatientCheck
import tech.mmarca.openvitals.domain.medical.PatientCheckResult
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCardImportSource
import tech.mmarca.openvitals.domain.medical.shc.SmartHealthCards
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordSource
import tech.mmarca.openvitals.domain.usecase.ImportMedicalRecordsUseCase

enum class MedicalImportStep { PICK, REVIEW, CONFIRM, IMPORTING, DONE }

/** Why a picked file cannot be imported. Nothing is written for any of these. */
enum class MedicalImportPickError {
    /** No source reads the file: it holds no FHIR records, CDA document or SMART Health Card. */
    NOT_SUPPORTED,
    TOO_LARGE,
    UNREADABLE,

    /** FHIR, but nothing in it Health Connect stores. */
    NO_RECORDS,

    /** The user refused the camera, so a QR code cannot be scanned. A photo of it can still be picked. */
    CAMERA_DENIED,
}

data class MedicalImportUiState(
    val available: Boolean = true,
    val step: MedicalImportStep = MedicalImportStep.PICK,
    val isReading: Boolean = false,
    val fileName: String? = null,
    val pickError: MedicalImportPickError? = null,
    val error: ScreenError? = null,
    val plans: List<MedicalImportGroupPlan> = emptyList(),
    /** Records the source left out before analysis, such as FHIR DSTU2 in an Apple export. */
    val sourceSkips: List<MedicalSourceSkip> = emptyList(),
    /** The file holds a SMART Health Card, whose signature cannot be checked offline. */
    val unverifiedCard: Boolean = false,
    val patientCheck: PatientCheckResult = PatientCheckResult.Pass,
    val patientConfirmed: Boolean = false,
    val canWrite: Boolean = false,
    val firstRequestDone: Boolean = true,
    val progress: MedicalImportProgress? = null,
    val result: MedicalImportResult? = null,
    /** The size of the files the user may keep, or null when there is nothing to keep. */
    val documentSize: Long? = null,
    /** How many files the user may keep: one picked file, or each document's PDF from a portal export. */
    val documentCount: Int = 0,
    /** Space the saved documents already take. */
    val documentsBytes: Long = 0,
    /** Off on every import; nothing remembers it. */
    val keepDocument: Boolean = false,
    /** After the import: true when every copy was kept, false when one failed, null when not asked. */
    val documentKept: Boolean? = null,
    /** After the import: how many files were kept. */
    val keptCount: Int = 0,
) {
    val isImporting: Boolean get() = step == MedicalImportStep.IMPORTING

    /** A patient check that stopped the import has been answered with "these records are mine". */
    val patientCleared: Boolean get() = patientCheck == PatientCheckResult.Pass || patientConfirmed

    val recordsToWrite: Int get() = plans.filter { it.include }.sumOf { it.group.ready.size }

    val canContinue: Boolean get() = patientCleared && recordsToWrite > 0
}

/**
 * The medical records import wizard: pick a file, review what it holds, name new sources,
 * import, and read the result. Nothing is written before the import step.
 */
@HiltViewModel
class MedicalImportViewModel(
    private val repository: MedicalRecordsRepository,
    private val preferences: MedicalRecordsPreferences,
    private val importRecords: ImportMedicalRecordsUseCase,
    private val dispatchers: DispatcherProvider,
    private val source: MedicalImportSource,
    private val today: () -> LocalDate,
    private val documents: MedicalDocumentsRepository? = null,
) : ViewModel() {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        repository: MedicalRecordsRepository,
        preferences: MedicalRecordsPreferences,
        importRecords: ImportMedicalRecordsUseCase,
        dispatchers: DispatcherProvider,
        documents: MedicalDocumentsRepository,
    ) : this(
        repository, preferences, importRecords, dispatchers,
        PickedFileImportSource(SmartHealthCardImportSource(AndroidCardRasterizer(context))), LocalDate::now, documents,
    )

    private val _uiState = MutableStateFlow(MedicalImportUiState(firstRequestDone = preferences.firstPermissionRequestDone))
    val uiState: StateFlow<MedicalImportUiState> = _uiState.asStateFlow()

    /** Kept from the analyze step, so a rename matches against the same sources. */
    private var ownSources: List<MedicalRecordSource> = emptyList()

    /** The files the user may keep, held until the import ends. */
    private var sourceDocuments: List<MedicalSourceDocument> = emptyList()

    init {
        refresh()
    }

    /** Availability and write access. Call again when a permission request returns. */
    fun refresh() {
        viewModelScope.launch {
            val available = repository.isAvailable()
            val canWrite = available && runCatching { repository.canWrite() }.getOrElse { if (it is CancellationException) throw it else false }
            _uiState.update { it.copy(available = available, canWrite = canWrite) }
        }
    }

    /**
     * What to ask for before a file can be read. The first time the medical area opens, all
     * thirteen permissions go in one request. After that, only write, which matching existing
     * sources needs.
     */
    fun permissionsToRequest(): Set<String> =
        if (_uiState.value.firstRequestDone) repository.writePermissions else repository.permissions

    fun onPermissionsRequested() {
        if (!_uiState.value.firstRequestDone) {
            preferences.firstPermissionRequestDone = true
            _uiState.update { it.copy(firstRequestDone = true) }
        }
    }

    /** The screen could not read the picked file. */
    fun onPickFailed(error: MedicalImportPickError) {
        _uiState.update { it.copy(isReading = false, pickError = error) }
    }

    /**
     * The analyze step: read, parse, repair, group, match sources, and check the patient.
     * Writes nothing. [open] opens the picked file; a source may open it more than once.
     */
    fun analyze(fileName: String?, open: () -> InputStream) {
        _uiState.update { it.copy(isReading = true, pickError = null, error = null, fileName = fileName) }
        viewModelScope.launch {
            val read = try {
                withContext(dispatchers.io) { source.read(open) }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                return@launch onPickFailed(MedicalImportPickError.UNREADABLE)
            }
            val entries = when (read) {
                is MedicalImportSourceResult.Entries -> read
                MedicalImportSourceResult.NotSupported -> return@launch onPickFailed(MedicalImportPickError.NOT_SUPPORTED)
                MedicalImportSourceResult.TooLarge -> return@launch onPickFailed(MedicalImportPickError.TOO_LARGE)
                MedicalImportSourceResult.Empty -> return@launch onPickFailed(MedicalImportPickError.NO_RECORDS)
            }
            try {
                val analysis = withContext(dispatchers.default) { FhirImportAnalyzer.analyze(entries.file, fileName ?: DefaultFileName) }
                if (analysis.groups.none { it.ready.isNotEmpty() || it.rejected.isNotEmpty() } && entries.skipped.isEmpty()) {
                    return@launch onPickFailed(MedicalImportPickError.NO_RECORDS)
                }
                ownSources = repository.ownSources()
                val ownIds = ownSources.map { it.id }.toSet()
                val otherSources = runCatching { repository.allSources() }
                    .getOrElse { if (it is CancellationException) throw it else emptyList() }
                    .filterNot { it.id in ownIds }
                val plans = MedicalImportPlanner.plan(analysis, ownSources, otherSources)
                val patientCheck = PatientCheck.check(analysis.patients.map { it.json }, storedPatients())
                sourceDocuments = if (documents != null) entries.documents else emptyList()
                val documentsBytes = documents?.let { runCatching { it.totalBytes() }.getOrDefault(0L) } ?: 0L
                _uiState.update {
                    it.copy(
                        isReading = false,
                        step = MedicalImportStep.REVIEW,
                        plans = plans,
                        sourceSkips = entries.skipped,
                        unverifiedCard = entries.file.entries.any { SmartHealthCards.isUnverified(it.resource) },
                        patientCheck = patientCheck,
                        patientConfirmed = false,
                        documentSize = sourceDocuments.takeIf { it.isNotEmpty() }?.sumOf { it.size },
                        documentCount = sourceDocuments.size,
                        documentsBytes = documentsBytes,
                        keepDocument = false,
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _uiState.update { it.copy(isReading = false, error = failure.toScreenError(logTag = TAG)) }
            }
        }
    }

    /** The user answered the patient check with "these records are mine". */
    fun confirmPatient() {
        _uiState.update { it.copy(patientConfirmed = true) }
    }

    /** Includes or leaves out a group; groups another app holds start left out. */
    fun setIncluded(index: Int, included: Boolean) {
        updatePlan(index) { it.copy(include = included) }
    }

    fun rename(index: Int, name: String) {
        updatePlan(index) { MedicalImportPlanner.rename(it, name, ownSources) }
    }

    fun goToConfirm() {
        if (_uiState.value.canContinue) _uiState.update { it.copy(step = MedicalImportStep.CONFIRM) }
    }

    fun back() {
        when (_uiState.value.step) {
            MedicalImportStep.REVIEW -> reset()
            MedicalImportStep.CONFIRM -> _uiState.update { it.copy(step = MedicalImportStep.REVIEW) }
            else -> Unit
        }
    }

    fun setKeepDocument(keep: Boolean) {
        _uiState.update { it.copy(keepDocument = keep && it.documentSize != null) }
    }

    fun startImport() {
        val state = _uiState.value
        if (!state.canContinue || !state.canWrite) return
        _uiState.update { it.copy(step = MedicalImportStep.IMPORTING, progress = MedicalImportProgress(0, state.recordsToWrite)) }
        viewModelScope.launch {
            val result = importRecords(state.plans, today()) { progress -> _uiState.update { it.copy(progress = progress) } }
            val kept = if (state.keepDocument) keepDocuments(state.fileName, result) else null
            sourceDocuments = emptyList()
            _uiState.update {
                it.copy(
                    step = MedicalImportStep.DONE,
                    result = result.copy(sourceSkips = state.sourceSkips),
                    documentKept = kept?.first,
                    keptCount = kept?.second ?: 0,
                )
            }
        }
    }

    /**
     * Keeps each file with the records it holds. A file that gave no records is not kept:
     * OpenVitals is not a document store. The exception is a file the source marks as worth
     * keeping alone, such as an Estonian declaration of intent. Returns whether every file was
     * kept, and how many were.
     */
    private suspend fun keepDocuments(fileName: String?, result: MedicalImportResult): Pair<Boolean, Int>? {
        val store = documents ?: return null
        var kept = 0
        var failed = false
        for (copy in sourceDocuments) {
            val refs = copy.links?.let { links -> result.refs.filter { (it.resourceType to it.resourceId) in links } } ?: result.refs
            if (refs.isEmpty() && !copy.keepWithoutRecords) continue
            val refSet = refs.toSet()
            val sourceName = result.groups.filter { group -> group.refs.any { it in refSet } }.map { it.sourceName }.distinct().singleOrNull()
                ?: copy.sourceName
            runCatching { store.keep(copy.fileName(fileName), copy.mimeType, copy.extension, copy.bytes, sourceName, refs) }
                .onSuccess { kept++ }
                .onFailure { if (it is CancellationException) throw it else failed = true }
        }
        if (kept == 0 && !failed) return null
        return !failed to kept
    }

    fun reset() {
        ownSources = emptyList()
        sourceDocuments = emptyList()
        _uiState.update {
            MedicalImportUiState(available = it.available, canWrite = it.canWrite, firstRequestDone = it.firstRequestDone)
        }
    }

    fun reportText(): String? {
        val state = _uiState.value
        return state.result?.let { MedicalImportReport.text(state.fileName, it, Instant.now()) }
    }

    /** The owner's Patient records, or null when personal details cannot be read, so the check asks instead. */
    private suspend fun storedPatients(): List<JsonObject>? {
        val readable = runCatching { repository.readableCategories() }
            .getOrElse { if (it is CancellationException) throw it else emptySet() }
        if (MedicalCategory.PERSONAL_DETAILS !in readable) return null
        return repository.readCategory(MedicalCategory.PERSONAL_DETAILS)
            .filter { it.ref.resourceType == "Patient" }
            .mapNotNull { runCatching { Json.parseToJsonElement(it.json) as? JsonObject }.getOrNull() }
    }

    private fun updatePlan(index: Int, change: (MedicalImportGroupPlan) -> MedicalImportGroupPlan) {
        _uiState.update { state ->
            state.copy(plans = state.plans.mapIndexed { i, plan -> if (i == index) change(plan) else plan })
        }
    }

    private companion object {
        const val TAG = "MedicalImport"
        const val DefaultFileName = "import.json"
    }
}
