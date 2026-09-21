package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.RecordingPreferences
import tech.mmarca.openvitals.data.repository.contract.UnitPreferences
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryUnits
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.manualentry.activity.buildWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.initialActivityEntryState
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImporter
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFolderScanner
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.toImportWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.withRouteImport
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportPhase
import tech.mmarca.openvitals.features.imports.garmin.FitHrvImportService
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitHrvReading
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthExportFingerprint
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportAnalysisResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportErrorFormatter
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportProgress
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportService
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorker
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.work.WorkInfo
import androidx.compose.runtime.Immutable
import java.time.Clock

/**
 * What the Data transfer screen shows: the Apple Health export, and the bulk route
 * and FIT imports.
 *
 * Its own ViewModel, as the Watches and Sync with another phone sections already are,
 * so Settings does not carry the import workflows as well. See architecture.md.
 */
/** Which Data transfer card started the running (or last finished) bulk import. */
enum class RouteBulkImportSource {
    /** The GPX/KML/KMZ/TCX card's multi-select picker. */
    ROUTE_FILES,

    /** The FIT card's folder import. */
    FIT_FOLDER,
}

@Immutable
data class RouteBulkImportProgress(
    val totalFiles: Int,
    val importedFiles: Int = 0,
    val failedFiles: Int = 0,
    val currentFileIndex: Int = 0,
)

@Immutable
data class RouteBulkImportResult(
    val totalFiles: Int,
    val importedFiles: Int,
    val failedFiles: Int,
)

@Immutable
data class DataImportUiState(
    val isLoading: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val dataImportWritePermissions: Set<String> = emptySet(),
    val routeImportWritePermissions: Set<String> = emptySet(),
    val isAnalyzingAppleHealth: Boolean = false,
    val isImportingAppleHealth: Boolean = false,
    val appleHealthAnalysisProgress: AppleHealthImportProgress? = null,
    val appleHealthImportAnalysis: AppleHealthImportAnalysisResult? = null,
    val selectedAppleHealthImportCategories: Set<AppleHealthImportCategory> = emptySet(),
    val appleHealthImportProgress: AppleHealthImportProgress? = null,
    val appleHealthImportResult: AppleHealthImportResult? = null,
    val appleHealthImportError: String? = null,
    val appleHealthImportPermissionDenied: Boolean = false,
    val isImportingRouteFiles: Boolean = false,
    val routeImportProgress: RouteBulkImportProgress? = null,
    val routeImportResult: RouteBulkImportResult? = null,
    val routeImportError: String? = null,
    /** Which card owns the bulk import surface. Two cards share one importer. */
    val routeImportSource: RouteBulkImportSource = RouteBulkImportSource.ROUTE_FILES,
    /** The FIT folder picker is up, or the tree is being walked. */
    val isScanningFitFolder: Boolean = false,
    /** The folder was readable and had no FIT files. Not an error. */
    val fitFolderHadNoFitFiles: Boolean = false,
    /** How many files were listed when more were found than the scan takes. */
    val fitFolderTruncatedAt: Int? = null,
    /** The scan itself failed. Import failures land in [routeImportError]. */
    val fitFolderScanError: String? = null,
) {
    val missingDataImportWritePermissions: Set<String>
        get() = dataImportWritePermissions - grantedPermissions

    val missingRouteImportWritePermissions: Set<String>
        get() = routeImportWritePermissions - grantedPermissions
}

@HiltViewModel
class DataImportViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val activityRepository: ActivityRepository,
    private val recordingPreferences: RecordingPreferences,
    private val unitPreferences: UnitPreferences,
    private val appleHealthImportService: AppleHealthImportService,
    private val appleHealthImportWorkController: AppleHealthImportWorkController,
    private val routeFileImporter: RouteFileImporter,
    private val fitHrvImportService: FitHrvImportService,
    private val routeFolderScanner: RouteFolderScanner,
) : ViewModel() {
    companion object {
        private const val TAG = "DataImportViewModel"

        /** Bulk route import flushes one insert per this many files, or sooner by route points. */
        private const val MaxPendingImportFiles = 25
        private const val MaxPendingImportRoutePoints = 50_000
    }

    private val _uiState = MutableStateFlow(DataImportUiState())
    val uiState: StateFlow<DataImportUiState> = _uiState.asStateFlow()
    private var currentAppleHealthImportWorkId: UUID? = null
    private var pendingAppleHealthImportUri: Uri? = null
    private var lastAnalyzedAppleHealthExportFingerprint: AppleHealthExportFingerprint? = null
    private val clock: Clock = Clock.systemDefaultZone()

    private val isBulkImportBusy: Boolean
        get() = _uiState.value.isImportingRouteFiles || _uiState.value.isScanningFitFolder

    init {
        refresh()
        observeAppleHealthImportWork()
    }

    /** Health Connect availability and what it has granted. The cards gate on both. */
    fun refresh() {
        viewModelScope.launch {
            val availability = repository.availability()
            val granted = if (availability == HealthConnectAvailability.AVAILABLE) {
                repository.grantedPermissions()
            } else {
                emptySet()
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                availability = availability,
                grantedPermissions = granted,
                dataImportWritePermissions = repository.dataImportWritePermissions,
                routeImportWritePermissions = activityRepository.activityWritePermissions(),
            )
        }
    }

    /** The grant sheet closed; whatever it granted decides what the cards offer. */
    fun onPermissionsResult() {
        refresh()
    }

    fun analyzeAppleHealthExport(uri: Uri) {
        val state = _uiState.value
        if (state.isAnalyzingAppleHealth || state.isImportingAppleHealth) return

        val previousAnalysis = state.appleHealthImportAnalysis
        val previousCategories = state.selectedAppleHealthImportCategories
        val previousFingerprint = lastAnalyzedAppleHealthExportFingerprint

        viewModelScope.launch {
            val fingerprint = appleHealthImportService.fingerprintOf(uri)
            val canReuseAnalysis = previousAnalysis != null &&
                previousFingerprint != null &&
                fingerprint.isIdentifiable() &&
                fingerprint == previousFingerprint

            if (canReuseAnalysis) {
                reuseAppleHealthAnalysis(uri, previousAnalysis, previousCategories)
            } else {
                runFullAppleHealthAnalysis(uri, fingerprint)
            }
        }
    }

    private suspend fun reuseAppleHealthAnalysis(
        uri: Uri,
        analysis: AppleHealthImportAnalysisResult,
        categories: Set<AppleHealthImportCategory>,
    ) {
        pendingAppleHealthImportUri = uri
        _uiState.value = _uiState.value.copy(
            appleHealthImportAnalysis = analysis,
            selectedAppleHealthImportCategories = categories,
            appleHealthImportError = null,
            appleHealthImportPermissionDenied = false,
        )
        runCatching {
            appleHealthImportWorkController.persistReadPermission(uri)
        }.onFailure { error ->
            Log.e(AppleHealthImportWorker.LogTag, "Apple Health re-selection failed", error)
            pendingAppleHealthImportUri = null
            _uiState.value = _uiState.value.copy(
                appleHealthImportError = AppleHealthImportErrorFormatter.details(error),
                appleHealthImportPermissionDenied = AppleHealthImportErrorFormatter.isPermissionDenied(error),
            )
        }
    }

    private suspend fun runFullAppleHealthAnalysis(uri: Uri, fingerprint: AppleHealthExportFingerprint) {
        pendingAppleHealthImportUri = uri
        lastAnalyzedAppleHealthExportFingerprint = null
        _uiState.value = _uiState.value.copy(
            isAnalyzingAppleHealth = true,
            appleHealthAnalysisProgress = AppleHealthImportProgress(phase = AppleHealthImportPhase.QUEUED),
            appleHealthImportAnalysis = null,
            selectedAppleHealthImportCategories = emptySet(),
            appleHealthImportProgress = null,
            appleHealthImportResult = null,
            appleHealthImportError = null,
            appleHealthImportPermissionDenied = false,
        )

        runCatching {
            appleHealthImportWorkController.persistReadPermission(uri)
            appleHealthImportService.analyzeStagedAppleHealthExport(uri, fingerprint) { progress ->
                _uiState.value = _uiState.value.copy(
                    appleHealthAnalysisProgress = progress,
                )
            }
        }.onSuccess { analysis ->
            val detectedCategories = analysis.categorySummaries
                .mapTo(mutableSetOf()) { it.category }
            lastAnalyzedAppleHealthExportFingerprint = fingerprint.takeIf { it.isIdentifiable() }
            _uiState.value = _uiState.value.copy(
                isAnalyzingAppleHealth = false,
                appleHealthAnalysisProgress = null,
                appleHealthImportAnalysis = analysis,
                selectedAppleHealthImportCategories = detectedCategories,
                appleHealthImportError = null,
                appleHealthImportPermissionDenied = false,
            )
        }.onFailure { error ->
            Log.e(AppleHealthImportWorker.LogTag, "Apple Health analysis failed", error)
            pendingAppleHealthImportUri = null
            _uiState.value = _uiState.value.copy(
                isAnalyzingAppleHealth = false,
                appleHealthAnalysisProgress = null,
                appleHealthImportAnalysis = null,
                selectedAppleHealthImportCategories = emptySet(),
                appleHealthImportResult = null,
                appleHealthImportError = AppleHealthImportErrorFormatter.details(error),
                appleHealthImportPermissionDenied = AppleHealthImportErrorFormatter.isPermissionDenied(error),
            )
        }
    }

    fun setAppleHealthImportCategorySelected(category: AppleHealthImportCategory, selected: Boolean) {
        val current = _uiState.value.selectedAppleHealthImportCategories
        _uiState.value = _uiState.value.copy(
            selectedAppleHealthImportCategories = if (selected) {
                current + category
            } else {
                current - category
            },
        )
    }

    fun importSelectedAppleHealthExport() {
        val state = _uiState.value
        if (state.isAnalyzingAppleHealth || state.isImportingAppleHealth) return
        val uri = pendingAppleHealthImportUri ?: return
        val selectedCategories = state.selectedAppleHealthImportCategories
        if (selectedCategories.isEmpty()) return
        val expectedSelectedRecords = state.appleHealthImportAnalysis
            ?.categorySummaries
            ?.filter { it.category in selectedCategories }
            ?.sumOf { it.convertedRecords }
            ?: 0
        val expectedParsedElements = state.appleHealthImportAnalysis?.parsedElements ?: 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingAppleHealth = true,
                appleHealthImportProgress = AppleHealthImportProgress(
                    phase = AppleHealthImportPhase.QUEUED,
                    expectedSelectedRecords = expectedSelectedRecords,
                    expectedParsedElements = expectedParsedElements,
                ),
                appleHealthImportResult = null,
                appleHealthImportError = null,
                appleHealthImportPermissionDenied = false,
            )

            runCatching {
                appleHealthImportWorkController.enqueue(
                    uri = uri,
                    selectedCategories = selectedCategories,
                    expectedSelectedRecords = expectedSelectedRecords,
                    expectedParsedElements = expectedParsedElements,
                )
            }
                .onSuccess { workId ->
                    currentAppleHealthImportWorkId = workId
                }
                .onFailure { error ->
                    Log.e(AppleHealthImportWorker.LogTag, "Apple Health import enqueue failed", error)
                    _uiState.value = _uiState.value.copy(
                        isImportingAppleHealth = false,
                        appleHealthImportProgress = null,
                        appleHealthImportResult = null,
                        appleHealthImportError = AppleHealthImportErrorFormatter.details(error),
                        appleHealthImportPermissionDenied = AppleHealthImportErrorFormatter.isPermissionDenied(error),
                    )
                }
        }
    }

    private fun observeAppleHealthImportWork() {
        viewModelScope.launch {
            appleHealthImportWorkController.workInfos.collect { workInfos ->
                val workInfo = workInfos.currentAppleHealthImportWork(currentAppleHealthImportWorkId)
                    ?: return@collect
                if (!workInfo.state.isFinished) {
                    currentAppleHealthImportWorkId = workInfo.id
                }
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING,
                    -> {
	                        _uiState.value = _uiState.value.copy(
	                            isImportingAppleHealth = true,
	                            isAnalyzingAppleHealth = false,
	                            appleHealthImportProgress = appleHealthImportWorkController.progressFor(workInfo)
	                                ?: AppleHealthImportProgress(phase = AppleHealthImportPhase.QUEUED),
	                            appleHealthImportResult = null,
                            appleHealthImportError = null,
                            appleHealthImportPermissionDenied = false,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val result = appleHealthImportWorkController.resultFor(workInfo)
                        Log.d(
                            TAG,
                            "Apple Health import completed imported=${result?.importedRecords ?: 0} " +
                                "failed=${result?.failedRecords ?: 0}",
                        )
	                        _uiState.value = _uiState.value.copy(
	                            isImportingAppleHealth = false,
	                            isAnalyzingAppleHealth = false,
	                            appleHealthImportProgress = null,
	                            appleHealthImportAnalysis = null,
	                            selectedAppleHealthImportCategories = emptySet(),
	                            appleHealthImportResult = result,
	                            appleHealthImportError = null,
	                            appleHealthImportPermissionDenied = false,
	                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = appleHealthImportWorkController.errorFor(workInfo)
                            ?: "Apple Health import failed."
                        Log.e(
                            AppleHealthImportWorker.LogTag,
                            "Apple Health import failed workId=${workInfo.id}\n$error",
                        )
	                        _uiState.value = _uiState.value.copy(
	                            isImportingAppleHealth = false,
	                            isAnalyzingAppleHealth = false,
	                            appleHealthImportProgress = null,
	                            appleHealthImportResult = null,
	                            appleHealthImportError = error,
	                            appleHealthImportPermissionDenied = appleHealthImportWorkController.permissionDeniedFor(workInfo),
                        )
                    }
                    WorkInfo.State.CANCELLED -> {
                        if (_uiState.value.isImportingAppleHealth) {
                            _uiState.value = _uiState.value.copy(
                                isImportingAppleHealth = false,
                                appleHealthImportProgress = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun importRouteFiles(uris: List<Uri>) {
        if (uris.isEmpty() || isBulkImportBusy) return

        viewModelScope.launch {
            runBulkImport(uris, RouteBulkImportSource.ROUTE_FILES)
        }
    }

    /**
     * Walks the picked tree and hands every FIT file to the bulk importer.
     * Scan outcomes land in the `fitFolder*` state; the import reports through
     * the shared surface tagged [RouteBulkImportSource.FIT_FOLDER].
     */
    fun importFitFolder(treeUri: Uri) {
        if (isBulkImportBusy) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanningFitFolder = true,
                fitFolderHadNoFitFiles = false,
                fitFolderTruncatedAt = null,
                fitFolderScanError = null,
            )

            val scan = try {
                routeFolderScanner.scan(treeUri, RouteFolderScanner.FitExtensions)
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                Log.e(TAG, "FIT folder scan failed", error)
                _uiState.value = _uiState.value.copy(
                    isScanningFitFolder = false,
                    fitFolderScanError = error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: error.message?.takeIf { it.isNotBlank() }
                        ?: "The folder could not be read.",
                )
                return@launch
            }

            if (scan.files.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isScanningFitFolder = false,
                    fitFolderHadNoFitFiles = true,
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isScanningFitFolder = false,
                fitFolderTruncatedAt = scan.files.size.takeIf { scan.truncated },
            )
            // URIs, not bytes: each file is opened when the importer reaches it.
            runBulkImport(scan.files.map { it.uri }, RouteBulkImportSource.FIT_FOLDER)
        }
    }

    private suspend fun runBulkImport(uris: List<Uri>, source: RouteBulkImportSource) {
        val totalFiles = uris.size
        var importedFiles = 0
        var failedFiles = 0
        var lastError: String? = null
        var rateLimited = false

        _uiState.value = _uiState.value.copy(
            isImportingRouteFiles = true,
            routeImportSource = source,
            routeImportProgress = RouteBulkImportProgress(totalFiles = totalFiles),
            routeImportResult = null,
            routeImportError = null,
        )

        // Health Connect rate-limits per API call: one insert per file exhausted
        // the daily allowance around 1700 files. Flush as one insert per batch.
        val pending = mutableListOf<ActivityWriteRequest>()
        var pendingRoutePoints = 0
        // Garmin wellness FIT files carry nightly HRV; they batch separately.
        val pendingHrvFiles = mutableListOf<List<FitHrvReading>>()

        suspend fun flushHrv() {
            if (pendingHrvFiles.isEmpty() || rateLimited) return
            val files = pendingHrvFiles.toList()
            pendingHrvFiles.clear()
            try {
                val outcome = fitHrvImportService.writeFiles(files)
                importedFiles += outcome.importedFiles
                failedFiles += outcome.failedFiles
                if (outcome.rateLimited) {
                    rateLimited = true
                    lastError = "Health Connect is rate limited."
                }
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                failedFiles += files.size
                lastError = error.localizedMessage ?: error.message ?: "HRV import failed."
                Log.e(TAG, "Garmin HRV import failed", error)
            }
        }

        suspend fun flush() {
            if (pending.isEmpty() || rateLimited) return
            val batch = pending.toList()
            pending.clear()
            pendingRoutePoints = 0
            try {
                activityRepository.writeActivityEntries(batch)
                importedFiles += batch.size
                recordingPreferences.lastActivityExerciseType = batch.last().exerciseType
                return
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(error)) {
                    rateLimited = true
                    lastError = error.localizedMessage ?: error.message ?: "Health Connect is rate limited."
                    Log.e(TAG, "Route bulk import rate limited; stopping", error)
                    return
                }
                Log.w(TAG, "Route bulk import batch failed; retrying file by file", error)
            }
            // The batched insert is atomic; retry file by file so only the bad one fails.
            for (request in batch) {
                if (rateLimited) return
                try {
                    activityRepository.writeActivityEntry(request)
                    importedFiles += 1
                    recordingPreferences.lastActivityExerciseType = request.exerciseType
                } catch (error: Throwable) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    if (HealthConnectRateLimitBackoff.isRateLimitFailure(error)) {
                        rateLimited = true
                        lastError = error.localizedMessage ?: error.message ?: "Health Connect is rate limited."
                        Log.e(TAG, "Route bulk import rate limited; stopping", error)
                        return
                    }
                    failedFiles += 1
                    lastError = error.localizedMessage ?: error.message ?: "Route import failed."
                    Log.e(TAG, "Route bulk import file failed in batch retry", error)
                }
            }
        }

        for ((index, uri) in uris.withIndex()) {
            if (rateLimited) break
            _uiState.value = _uiState.value.copy(
                routeImportProgress = RouteBulkImportProgress(
                    totalFiles = totalFiles,
                    importedFiles = importedFiles,
                    failedFiles = failedFiles,
                    currentFileIndex = index + 1,
                ),
            )

            runCatching {
                val routeImport = routeFileImporter.import(uri)
                val preferredType = preferredActivityType(requireGpsRoute = routeImport.points.isNotEmpty())
                // Straight from the file: exact times, and a client id that is a function of
                // the file, so picking the same folder twice does not store each workout twice.
                val request = routeImport.toImportWriteRequest(preferredType, ActivityRecordSource.FILE)
                    ?: formWriteRequest(routeImport, preferredType)
                    ?: throw IllegalArgumentException("Imported route could not be converted into an activity.")
                val hasPermission = activityRepository.hasActivityWritePermission(request)
                if (!hasPermission) {
                    throw SecurityException("Activity import write permissions are missing.")
                }
                request
            }.onSuccess { request ->
                pending += request
                pendingRoutePoints += request.routePoints.size
                if (pending.size >= MaxPendingImportFiles || pendingRoutePoints >= MaxPendingImportRoutePoints) {
                    flush()
                }
            }.onFailure { error ->
                // A non-activity FIT may be a wellness file with nightly HRV.
                val hrvReadings = routeFileImporter.importFitWellnessHrv(uri)
                if (hrvReadings.isNotEmpty()) {
                    pendingHrvFiles += hrvReadings
                    if (pendingHrvFiles.size >= MaxPendingImportFiles) {
                        flushHrv()
                    }
                } else {
                    failedFiles += 1
                    lastError = error.localizedMessage ?: error.message ?: "Route import failed."
                    Log.e(TAG, "Route bulk import failed index=${index + 1}", error)
                }
            }
        }
        flush()
        flushHrv()

        _uiState.value = _uiState.value.copy(
            isImportingRouteFiles = false,
            routeImportProgress = null,
            routeImportResult = RouteBulkImportResult(
                totalFiles = totalFiles,
                importedFiles = importedFiles,
                failedFiles = failedFiles,
            ),
            // A rate-limited run stops rather than blaming files it never attempted.
            routeImportError = lastError.takeIf { failedFiles > 0 || rateLimited },
        )
    }

    /** A route with no timestamps has no time of its own. The entry form's defaults supply one, as before. */
    private fun formWriteRequest(routeImport: RouteFileImport, preferredType: ActivityEntryType): ActivityWriteRequest? {
        // Any consistent unit pair works: nobody reads the text.
        val importUnits = ActivityEntryUnits.uniform(unitPreferences.unitSystem)
        val routeState = initialActivityEntryState(
            clock = clock,
            repository = activityRepository,
            selectedActivityType = preferredType,
        ).withRouteImport(
            routeImport = routeImport,
            units = importUnits,
            clock = clock,
        )
        return buildWriteRequest(routeState, importUnits)
    }

    private fun preferredActivityType(requireGpsRoute: Boolean = false) =
        DefaultActivityEntryTypes
            .filter { !requireGpsRoute || it.supportsGpsRoute }
            .ifEmpty { DefaultActivityEntryTypes }
            .let { activityTypes ->
                val preferredExerciseType = recordingPreferences.favoriteActivityExerciseType
                    ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                    ?: recordingPreferences.lastActivityExerciseType
                        ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                activityTypes.firstOrNull { it.exerciseType == preferredExerciseType }
                    ?: activityTypes.first()
            }
}

internal fun List<WorkInfo>.currentAppleHealthImportWork(currentWorkId: UUID?): WorkInfo? {
    if (currentWorkId != null) {
        firstOrNull { workInfo -> workInfo.id == currentWorkId }?.let { return it }
    }
    return firstOrNull { workInfo -> !workInfo.state.isFinished }
}
