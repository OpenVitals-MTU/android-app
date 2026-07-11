package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.manualentry.activity.buildWriteRequest
import tech.mmarca.openvitals.features.manualentry.activity.initialActivityEntryState
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImporter
import tech.mmarca.openvitals.features.manualentry.activity.withRouteImport
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportPhase
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportProgress
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportResult
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPack
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPackFormat
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportPhase
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthExportFingerprint
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportAnalysisResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportCategory
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportErrorFormatter
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportProgress
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportService
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorker
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.work.WorkInfo
import androidx.compose.runtime.Immutable
import java.time.Clock

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val permissionCategories: List<SettingsPermissionCategory> = emptyList(),
    val allPermissions: Set<String> = emptySet(),
    val dataImportWritePermissions: Set<String> = emptySet(),
    val routeImportWritePermissions: Set<String> = emptySet(),
    val manualOnlyPermissions: Set<String> = emptySet(),
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
    val offlineMapPacks: List<OfflineMapPack> = emptyList(),
    val activeOfflineMapFormat: OfflineMapPackFormat? = null,
    val isImportingOfflineMap: Boolean = false,
    val offlineMapImportProgress: OfflineMapImportProgress? = null,
    val offlineMapImportResult: OfflineMapImportResult? = null,
    val offlineMapImportError: String? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val activityRecordingPreferences: ActivityRecordingPreferences = ActivityRecordingPreferences(),
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val favoriteActivityExerciseType: Int? = null,
    val healthConnectSyncEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val bodyEnergyCalibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    val caffeinePreferences: CaffeinePreferences = CaffeinePreferences(),
    val bodyProfile: BodyProfile = BodyProfile(),
) {
    val visiblePermissions: Set<String>
        get() = permissionCategories.flatMap { it.permissions }.toSet()

    val missingVisiblePermissions: Set<String>
        get() = visiblePermissions - grantedPermissions

    val missingManualVisiblePermissions: Set<String>
        get() = missingVisiblePermissions.intersect(manualOnlyPermissions)

    val missingDataImportWritePermissions: Set<String>
        get() = dataImportWritePermissions - grantedPermissions

    val missingRouteImportWritePermissions: Set<String>
        get() = routeImportWritePermissions - grantedPermissions
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

data class SettingsPermissionCategory(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val permissions: Set<String>,
    val manualPermissions: Set<String> = emptySet(),
    val available: Boolean = true,
    @param:StringRes val unavailableReasonRes: Int? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val activityRepository: ActivityRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appleHealthImportService: AppleHealthImportService,
    private val appleHealthImportWorkController: AppleHealthImportWorkController,
    private val routeFileImporter: RouteFileImporter,
    private val offlineMapRepository: OfflineMapRepository,
    private val offlineMapImportWorkController: OfflineMapImportWorkController,
    private val permissionUxState: HealthConnectPermissionUxState,
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var currentAppleHealthImportWorkId: UUID? = null
    private var pendingAppleHealthImportUri: Uri? = null
    private var lastAnalyzedAppleHealthExportFingerprint: AppleHealthExportFingerprint? = null
    private val clock: Clock = Clock.systemDefaultZone()

    init {
        refresh()
        observeOfflineMaps()
        observeAppleHealthImportWork()
        observeOfflineMapImportWork()
    }

    fun refresh() {
        viewModelScope.launch {
            val avail = repository.availability()
            val granted = if (avail == HealthConnectAvailability.AVAILABLE) {
                repository.grantedPermissions()
            } else emptySet()
            Log.d(TAG, "refresh availability=$avail grantedCount=${granted.size}")

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                availability = avail,
                grantedPermissions = granted,
                permissionCategories = permissionCategories(avail),
                allPermissions = repository.allPermissions,
                dataImportWritePermissions = repository.dataImportWritePermissions,
                routeImportWritePermissions = activityRepository.activityWritePermissions(),
                manualOnlyPermissions = repository.manualOnlyPermissions,
                unitSystem = preferencesRepository.unitSystem,
                appLanguage = preferencesRepository.appLanguage,
                appThemeMode = preferencesRepository.appThemeMode,
                dynamicColor = preferencesRepository.dynamicColor,
                sleepRangeMode = preferencesRepository.sleepRangeMode,
                activityWeekMode = preferencesRepository.activityWeekMode,
                activityRecordingPreferences = preferencesRepository.activityRecordingPreferences(),
                showOpenVitalsCalculatedCalories = preferencesRepository.showOpenVitalsCalculatedCalories,
                favoriteActivityExerciseType = preferencesRepository.favoriteActivityExerciseType,
                healthConnectSyncEnabled = preferencesRepository.healthConnectSyncEnabled,
                appLockEnabled = preferencesRepository.appLockEnabled,
                bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration(),
                caffeinePreferences = preferencesRepository.caffeinePreferences(),
                bodyProfile = preferencesRepository.bodyProfile(),
            )
        }
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

    fun importRouteFiles(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isImportingRouteFiles) return

        viewModelScope.launch {
            val totalFiles = uris.size
            var importedFiles = 0
            var failedFiles = 0
            var lastError: String? = null

            _uiState.value = _uiState.value.copy(
                isImportingRouteFiles = true,
                routeImportProgress = RouteBulkImportProgress(totalFiles = totalFiles),
                routeImportResult = null,
                routeImportError = null,
            )

            uris.forEachIndexed { index, uri ->
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
                    val routeState = initialActivityEntryState(
                        clock = clock,
                        repository = activityRepository,
                        selectedActivityType = preferredActivityType(requireGpsRoute = routeImport.points.isNotEmpty()),
                    ).withRouteImport(
                        routeImport = routeImport,
                        unitSystem = _uiState.value.unitSystem,
                        clock = clock,
                    )
                    val request = buildWriteRequest(routeState, _uiState.value.unitSystem)
                        ?: throw IllegalArgumentException("Imported route could not be converted into an activity.")
                    val hasPermission = activityRepository.hasActivityWritePermission(request)
                    if (!hasPermission) {
                        throw SecurityException("Activity import write permissions are missing.")
                    }
                    activityRepository.writeActivityEntry(request)
                    preferencesRepository.lastActivityExerciseType = request.exerciseType
                }.onSuccess {
                    importedFiles += 1
                }.onFailure { error ->
                    failedFiles += 1
                    lastError = error.localizedMessage ?: error.message ?: "Route import failed."
                    Log.e(TAG, "Route bulk import failed index=${index + 1}", error)
                }
            }

            _uiState.value = _uiState.value.copy(
                isImportingRouteFiles = false,
                routeImportProgress = null,
                routeImportResult = RouteBulkImportResult(
                    totalFiles = totalFiles,
                    importedFiles = importedFiles,
                    failedFiles = failedFiles,
                ),
                routeImportError = lastError.takeIf { failedFiles > 0 },
            )
        }
    }

    fun importOfflineMap(uri: Uri) {
        if (_uiState.value.isImportingOfflineMap) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingOfflineMap = true,
                offlineMapImportProgress = OfflineMapImportProgress(phase = OfflineMapImportPhase.QUEUED),
                offlineMapImportResult = null,
                offlineMapImportError = null,
            )

            runCatching { offlineMapImportWorkController.enqueue(uri) }
                .onFailure { error ->
                    Log.e(TAG, "Offline map import enqueue failed type=${error::class.java.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isImportingOfflineMap = false,
                        offlineMapImportProgress = null,
                        offlineMapImportResult = null,
                        offlineMapImportError = error.localizedMessage
                            ?: "Offline map import failed.",
                    )
                }
        }
    }

    private fun observeOfflineMaps() {
        viewModelScope.launch {
            offlineMapRepository.state.collect { libraryState ->
                _uiState.value = _uiState.value.copy(
                    offlineMapPacks = libraryState.mapPacks,
                    activeOfflineMapFormat = libraryState.activeFormat,
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

    private fun observeOfflineMapImportWork() {
        viewModelScope.launch {
            offlineMapImportWorkController.workInfos.collect { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@collect
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING,
                    -> {
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = true,
                            offlineMapImportProgress = offlineMapImportWorkController.progressFor(workInfo)
                                ?: OfflineMapImportProgress(phase = OfflineMapImportPhase.QUEUED),
                            offlineMapImportResult = null,
                            offlineMapImportError = null,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val result = offlineMapImportWorkController.resultFor(workInfo)
                        offlineMapRepository.refresh()
                        Log.d(TAG, "Offline map import completed mapId=${result?.mapId.orEmpty()}")
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = false,
                            offlineMapImportProgress = null,
                            offlineMapImportResult = result,
                            offlineMapImportError = null,
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = offlineMapImportWorkController.errorFor(workInfo)
                            ?: "Offline map import failed."
                        Log.e(TAG, "Offline map import failed")
                        offlineMapRepository.refresh()
                        _uiState.value = _uiState.value.copy(
                            isImportingOfflineMap = false,
                            offlineMapImportProgress = null,
                            offlineMapImportResult = null,
                            offlineMapImportError = error,
                        )
                    }
                    WorkInfo.State.CANCELLED -> {
                        if (_uiState.value.isImportingOfflineMap) {
                            _uiState.value = _uiState.value.copy(
                                isImportingOfflineMap = false,
                                offlineMapImportProgress = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectUnitSystem(unitSystem: UnitSystem) {
        preferencesRepository.unitSystem = unitSystem
        _uiState.value = _uiState.value.copy(unitSystem = unitSystem)
    }

    fun selectAppLanguage(appLanguage: AppLanguage) {
        preferencesRepository.appLanguage = appLanguage
        _uiState.value = _uiState.value.copy(appLanguage = appLanguage)
    }

    fun selectAppThemeMode(appThemeMode: AppThemeMode) {
        preferencesRepository.appThemeMode = appThemeMode
        _uiState.value = _uiState.value.copy(appThemeMode = appThemeMode)
    }

    fun setDynamicColor(enabled: Boolean) {
        preferencesRepository.dynamicColor = enabled
        _uiState.value = _uiState.value.copy(dynamicColor = enabled)
    }

    fun selectSleepRangeMode(sleepRangeMode: SleepRangeMode) {
        preferencesRepository.sleepRangeMode = sleepRangeMode
        _uiState.value = _uiState.value.copy(sleepRangeMode = sleepRangeMode)
    }

    fun selectActivityWeekMode(activityWeekMode: ActivityWeekMode) {
        preferencesRepository.activityWeekMode = activityWeekMode
        _uiState.value = _uiState.value.copy(activityWeekMode = activityWeekMode)
    }

    fun updateActivityRecordingPreferences(preferences: ActivityRecordingPreferences) {
        val normalized = preferences.normalized()
        preferencesRepository.setActivityRecordingPreferences(normalized)
        _uiState.value = _uiState.value.copy(activityRecordingPreferences = normalized)
    }

    fun setShowOpenVitalsCalculatedCalories(enabled: Boolean) {
        preferencesRepository.showOpenVitalsCalculatedCalories = enabled
        _uiState.value = _uiState.value.copy(showOpenVitalsCalculatedCalories = enabled)
    }

    fun selectFavoriteActivity(exerciseType: Int?) {
        preferencesRepository.favoriteActivityExerciseType = exerciseType
        _uiState.value = _uiState.value.copy(favoriteActivityExerciseType = exerciseType)
    }

    private fun preferredActivityType(requireGpsRoute: Boolean = false) =
        DefaultActivityEntryTypes
            .filter { !requireGpsRoute || it.supportsGpsRoute }
            .ifEmpty { DefaultActivityEntryTypes }
            .let { activityTypes ->
                val preferredExerciseType = preferencesRepository.favoriteActivityExerciseType
                    ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                    ?: preferencesRepository.lastActivityExerciseType
                        ?.takeIf { exerciseType -> activityTypes.any { it.exerciseType == exerciseType } }
                activityTypes.firstOrNull { it.exerciseType == preferredExerciseType }
                    ?: activityTypes.first()
            }

    fun selectOfflineMapFormat(format: OfflineMapPackFormat?) {
        offlineMapRepository.setActiveFormat(format)
        val libraryState = offlineMapRepository.state.value
        _uiState.value = _uiState.value.copy(
            offlineMapPacks = libraryState.mapPacks,
            activeOfflineMapFormat = libraryState.activeFormat,
        )
    }

    fun deleteOfflineMap(id: String) {
        viewModelScope.launch {
            runCatching { offlineMapRepository.deleteMap(id) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        offlineMapImportError = error.localizedMessage
                            ?: "Unable to delete offline map.",
                    )
                }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        Log.d(TAG, "onPermissionsResult callbackGrantedCount=${granted.size}")
        if (granted.isNotEmpty()) {
            permissionUxState.recordPermissionRequestGranted()
        } else {
            permissionUxState.recordPermissionRequestCancelled()
        }
        refresh()
    }

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        preferencesRepository.healthConnectSyncEnabled = enabled
        _uiState.value = _uiState.value.copy(healthConnectSyncEnabled = enabled)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        preferencesRepository.appLockEnabled = enabled
        _uiState.value = _uiState.value.copy(appLockEnabled = enabled)
    }

    fun updateBodyEnergyCalibration(calibration: BodyEnergyCalibration) {
        preferencesRepository.setBodyEnergyCalibration(calibration.copy(setupCompleted = true))
        _uiState.value = _uiState.value.copy(bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration())
    }

    fun updateCaffeinePreferences(preferences: CaffeinePreferences) {
        preferencesRepository.setCaffeinePreferences(preferences)
        _uiState.value = _uiState.value.copy(caffeinePreferences = preferencesRepository.caffeinePreferences())
    }

    fun updateBodyProfile(profile: BodyProfile) {
        preferencesRepository.setBodyProfile(profile)
        _uiState.value = _uiState.value.copy(bodyProfile = preferencesRepository.bodyProfile())
    }

    fun resetBodyEnergyCalibration() {
        updateBodyEnergyCalibration(BodyEnergyCalibration.Automatic)
    }

    fun acceptPrivacyPolicy() {
        preferencesRepository.acceptedPrivacyPolicyVersion = PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION
        preferencesRepository.privacyPolicyAcceptedAtMillis = System.currentTimeMillis()
    }

    private fun permissionCategories(availability: HealthConnectAvailability): List<SettingsPermissionCategory> {
        val mindfulnessAvailable = availability == HealthConnectAvailability.AVAILABLE &&
            repository.isMindfulnessAvailable()
        return listOf(
            SettingsPermissionCategory(
                id = "activity_sleep",
                titleRes = R.string.onboarding_category_activity_sleep,
                descriptionRes = R.string.onboarding_category_activity_sleep_desc,
                permissions = repository.corePermissions,
            ),
            SettingsPermissionCategory(
                id = "heart_recovery",
                titleRes = R.string.onboarding_category_heart_recovery,
                descriptionRes = R.string.onboarding_category_heart_recovery_desc,
                permissions = repository.heartPermissions,
            ),
            SettingsPermissionCategory(
                id = "body",
                titleRes = R.string.onboarding_category_body,
                descriptionRes = R.string.onboarding_category_body_desc,
                permissions = repository.bodyPermissions,
            ),
            SettingsPermissionCategory(
                id = "activity_extras",
                titleRes = R.string.onboarding_category_activity_extras,
                descriptionRes = R.string.onboarding_category_activity_extras_desc,
                permissions = repository.activityExtrasPermissions,
            ),
            SettingsPermissionCategory(
                id = "nutrition_hydration",
                titleRes = R.string.onboarding_category_nutrition_hydration,
                descriptionRes = R.string.onboarding_category_nutrition_hydration_desc,
                permissions = repository.nutritionHydrationPermissions,
            ),
            SettingsPermissionCategory(
                id = "manual_entry_write",
                titleRes = R.string.onboarding_category_manual_entry_write,
                descriptionRes = R.string.onboarding_category_manual_entry_write_desc,
                permissions = repository.requestableWritePermissions,
            ),
            SettingsPermissionCategory(
                id = "mindfulness",
                titleRes = R.string.onboarding_category_mindfulness,
                descriptionRes = R.string.onboarding_category_mindfulness_desc,
                permissions = repository.mindfulnessPermissions,
                available = mindfulnessAvailable,
                unavailableReasonRes = R.string.onboarding_category_mindfulness_unavailable,
            ),
            SettingsPermissionCategory(
                id = "additional_data_access",
                titleRes = R.string.onboarding_category_additional_data_access,
                descriptionRes = R.string.onboarding_category_additional_data_access_desc,
                permissions = repository.additionalDataAccessPermissions + repository.routePermissions,
                manualPermissions = repository.routePermissions,
            ),
            SettingsPermissionCategory(
                id = "vitals",
                titleRes = R.string.onboarding_category_vitals,
                descriptionRes = R.string.onboarding_category_vitals_desc,
                permissions = repository.vitalsPermissions,
            ),
            SettingsPermissionCategory(
                id = "cycle_tracking",
                titleRes = R.string.onboarding_category_cycle_tracking,
                descriptionRes = R.string.onboarding_category_cycle_tracking_desc,
                permissions = repository.cyclePermissions,
            ),
        ).filter { it.permissions.isNotEmpty() }
    }
}

internal fun List<WorkInfo>.currentAppleHealthImportWork(currentWorkId: UUID?): WorkInfo? {
    if (currentWorkId != null) {
        firstOrNull { workInfo -> workInfo.id == currentWorkId }?.let { return it }
    }
    return firstOrNull { workInfo -> !workInfo.state.isFinished }
}
