package tech.mmarca.openvitals.features.settings

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.UnitSystemPreference
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateThresholds
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.StepDistanceBackfillService
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryUnits
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
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
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
import java.time.Instant
import java.time.LocalDate

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
    val unitSystemPreference: UnitSystemPreference = UnitSystemPreference.SYSTEM,
    /** Already resolved — never carries the SYSTEM preference itself. */
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** Per-quantity display overrides; an absent quantity follows [unitSystem]. */
    val unitOverrides: Map<UnitQuantity, UnitSystem> = emptyMap(),
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val chartAggregationMode: ChartAggregationMode = ChartAggregationMode.OFF,
    val dashboardSortEmptyTilesLast: Boolean = true,
    val stepDistanceBackfillEnabled: Boolean = false,
    val strideLengthMeters: Double = StrideLength.defaultMeters,
    val nightStartHour: Int = SleepWindow.Default.startHour,
    val nightEndHour: Int = SleepWindow.Default.endHour,
    val highHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
    val lowHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
    val hydrationDailyGoalLiters: Double = PreferencesRepository.DEFAULT_HYDRATION_DAILY_GOAL_LITERS,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val activitySplitDistanceMeters: Double = ActivitySplitDistance.defaultMeters,
    val activityRecordingPreferences: ActivityRecordingPreferences = ActivityRecordingPreferences(),
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val favoriteActivityExerciseType: Int? = null,
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectMindfulnessEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val bodyEnergyCalibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    val caffeinePreferences: CaffeinePreferences = CaffeinePreferences(),
    val bodyProfile: BodyProfile = BodyProfile(),
    val bodyProfileWeightMeasured: Boolean = false,
    val bodyProfileHeightMeasured: Boolean = false,
    val canWriteBodyMeasurements: Boolean = false,
    val healthConnectSources: List<HealthConnectSource> = emptyList(),
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

    /** What one quantity displays in: its override, else the resolved base. */
    fun effectiveUnitSystem(quantity: UnitQuantity): UnitSystem =
        unitOverrides[quantity] ?: unitSystem
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
    private val bodyRepository: BodyRepository,
    private val heartRepository: HeartRepository,
    private val sleepRepository: SleepRepository,
    private val hydrationReminderController: HydrationReminderController,
    private val preferencesRepository: PreferencesRepository,
    private val stepDistanceBackfillService: StepDistanceBackfillService,
    private val appleHealthImportService: AppleHealthImportService,
    private val appleHealthImportWorkController: AppleHealthImportWorkController,
    private val routeFileImporter: RouteFileImporter,
    private val fitHrvImportService: FitHrvImportService,
    private val offlineMapRepository: OfflineMapRepository,
    private val offlineMapImportWorkController: OfflineMapImportWorkController,
    private val permissionUxState: HealthConnectPermissionUxState,
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"

        /**
         * Bulk route import flushes one batched insert per this many files, or
         * sooner once the pending batch carries this many route points — so
         * peak memory is bounded by GPS data rather than file count.
         */
        private const val MaxPendingImportFiles = 25
        private const val MaxPendingImportRoutePoints = 50_000
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
                unitSystemPreference = preferencesRepository.unitSystemPreference,
                unitSystem = preferencesRepository.unitSystem,
                unitOverrides = preferencesRepository.unitOverridesFlow.value,
                appLanguage = preferencesRepository.appLanguage,
                appThemeMode = preferencesRepository.appThemeMode,
                dynamicColor = preferencesRepository.dynamicColor,
                chartAggregationMode = preferencesRepository.chartAggregationMode,
                dashboardSortEmptyTilesLast = preferencesRepository.dashboardSortEmptyTilesLast,
                stepDistanceBackfillEnabled = preferencesRepository.stepDistanceBackfillEnabled,
                strideLengthMeters = preferencesRepository.strideLengthMeters,
                nightStartHour = preferencesRepository.nightStartHour,
                nightEndHour = preferencesRepository.nightEndHour,
                highHeartRateThresholdBpm = preferencesRepository.highHeartRateThresholdBpm,
                lowHeartRateThresholdBpm = preferencesRepository.lowHeartRateThresholdBpm,
                hydrationDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
                activityWeekMode = preferencesRepository.activityWeekMode,
                activitySplitDistanceMeters = preferencesRepository.activitySplitDistanceMeters,
                activityRecordingPreferences = preferencesRepository.activityRecordingPreferences(),
                showOpenVitalsCalculatedCalories = preferencesRepository.showOpenVitalsCalculatedCalories,
                favoriteActivityExerciseType = preferencesRepository.favoriteActivityExerciseType,
                healthConnectSyncEnabled = preferencesRepository.healthConnectSyncEnabled,
                healthConnectMindfulnessEnabled = preferencesRepository.healthConnectMindfulnessEnabled,
                appLockEnabled = preferencesRepository.appLockEnabled,
                bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration(),
                caffeinePreferences = preferencesRepository.caffeinePreferences(),
                bodyProfile = preferencesRepository.bodyProfile(),
            )
            resolveBodyProfileFromHealthConnect()
            loadHealthConnectSources()
        }
    }

    /**
     * Diagnostics only: the contributors seen in the last week of heart-rate
     * and sleep data — the two metrics a watch most reliably writes. Empty
     * when nothing has been read (no permission, or nothing synced yet).
     */
    private suspend fun loadHealthConnectSources() {
        if (!BuildConfig.OPENVITALS_DIAGNOSTICS) return
        val end = LocalDate.now()
        val start = end.minusDays(7)
        val heartRate = runCatching { heartRepository.loadHeartRateSamples(start, end) }
            .getOrDefault(emptyList())
        val sleep = runCatching { sleepRepository.loadSleepSessions(start, end) }
            .getOrDefault(emptyList())
        _uiState.value = _uiState.value.copy(
            healthConnectSources = aggregateHealthConnectSources(
                mapOf(
                    "heart rate" to heartRate.map { it.source to it.time },
                    "sleep" to sleep.map { it.source to it.endTime },
                ),
            ),
        )
    }

    /**
     * Folds the latest measured Health Connect weight/height into the card
     * state. The declared values were already seeded, so the fields are never
     * blank while Health Connect is read.
     */
    private suspend fun resolveBodyProfileFromHealthConnect() {
        val declared = preferencesRepository.bodyProfile()
        val resolved = runCatching { bodyRepository.resolveBodyProfile(declared) }
            .getOrElse { error ->
                Log.w(TAG, "resolveBodyProfile failed", error)
                return
            }
        val canWrite = runCatching {
            bodyRepository.hasBodyWritePermission(BodyMeasurementType.WEIGHT)
        }.getOrDefault(false)
        _uiState.value = _uiState.value.copy(
            bodyProfile = resolved,
            bodyProfileWeightMeasured = resolved.weightKg != null && resolved.weightKg != declared.weightKg,
            bodyProfileHeightMeasured = resolved.heightCm != null && resolved.heightCm != declared.heightCm,
            canWriteBodyMeasurements = canWrite,
        )
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
            var rateLimited = false

            _uiState.value = _uiState.value.copy(
                isImportingRouteFiles = true,
                routeImportProgress = RouteBulkImportProgress(totalFiles = totalFiles),
                routeImportResult = null,
                routeImportError = null,
            )

            // Health Connect rate-limits per API call, not per record: one
            // insert per file exhausted the daily allowance around 1700 files.
            // Parsed activities accumulate and flush as ONE insert per batch,
            // bounded by file count and by route points so peak memory tracks
            // GPS data, not file count.
            val pending = mutableListOf<ActivityWriteRequest>()
            var pendingRoutePoints = 0
            // Garmin wellness FIT files carry nightly HRV instead of an
            // activity; they collect separately and batch the same way.
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
                    preferencesRepository.lastActivityExerciseType = batch.last().exerciseType
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
                // The batched insert is atomic, so one bad file sinks the whole
                // batch — retry file by file so only the guilty one fails.
                for (request in batch) {
                    if (rateLimited) return
                    try {
                        activityRepository.writeActivityEntry(request)
                        importedFiles += 1
                        preferencesRepository.lastActivityExerciseType = request.exerciseType
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
                    // Headless import: the route's texts are generated and
                    // parsed with the same units, so any consistent pair works.
                    val importUnits = ActivityEntryUnits.uniform(_uiState.value.unitSystem)
                    val routeState = initialActivityEntryState(
                        clock = clock,
                        repository = activityRepository,
                        selectedActivityType = preferredActivityType(requireGpsRoute = routeImport.points.isNotEmpty()),
                    ).withRouteImport(
                        routeImport = routeImport,
                        units = importUnits,
                        clock = clock,
                    )
                    val request = buildWriteRequest(routeState, importUnits)
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
                    // A FIT file that is not an activity may be a Garmin
                    // wellness file carrying nightly HRV — import that instead
                    // of failing the file.
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
                // A rate-limited run stops rather than blaming the files it never
                // attempted, so the error surfaces even with zero failed files.
                routeImportError = lastError.takeIf { failedFiles > 0 || rateLimited },
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

    fun selectUnitSystem(preference: UnitSystemPreference) {
        preferencesRepository.unitSystemPreference = preference
        _uiState.value = _uiState.value.copy(
            unitSystemPreference = preference,
            unitSystem = preferencesRepository.unitSystem,
        )
    }

    fun selectUnitOverride(quantity: UnitQuantity, override: UnitSystem?) {
        preferencesRepository.setUnitOverride(quantity, override)
        _uiState.value = _uiState.value.copy(
            unitOverrides = preferencesRepository.unitOverridesFlow.value,
        )
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

    fun saveStepDistanceBackfill(enabled: Boolean, strideMeters: Double) {
        val normalized = StrideLength.normalize(strideMeters)
        val wasEnabled = preferencesRepository.stepDistanceBackfillEnabled
        preferencesRepository.strideLengthMeters = normalized
        preferencesRepository.stepDistanceBackfillEnabled = enabled
        _uiState.value = _uiState.value.copy(
            stepDistanceBackfillEnabled = enabled,
            strideLengthMeters = normalized,
        )
        viewModelScope.launch {
            if (enabled) {
                stepDistanceBackfillService.syncNow()
            } else if (wasEnabled) {
                stepDistanceBackfillService.purgeDerivedRecords()
            }
        }
    }

    fun setDashboardSortEmptyTilesLast(enabled: Boolean) {
        preferencesRepository.dashboardSortEmptyTilesLast = enabled
        _uiState.value = _uiState.value.copy(dashboardSortEmptyTilesLast = enabled)
    }

    fun setChartAggregationMode(mode: ChartAggregationMode) {
        preferencesRepository.chartAggregationMode = mode
        _uiState.value = _uiState.value.copy(chartAggregationMode = mode)
    }

    fun setNightStartHour(value: Int) {
        val hour = Math.floorMod(value, 24)
        preferencesRepository.nightStartHour = hour
        _uiState.value = _uiState.value.copy(nightStartHour = hour)
    }

    fun setNightEndHour(value: Int) {
        val hour = Math.floorMod(value, 24)
        preferencesRepository.nightEndHour = hour
        _uiState.value = _uiState.value.copy(nightEndHour = hour)
    }

    /** Diagnostics: posts the hydration reminder immediately via the real path. */
    fun showTestHydrationReminder() {
        hydrationReminderController.showTestReminder()
    }

    fun setHealthConnectMindfulnessEnabled(enabled: Boolean) {
        preferencesRepository.healthConnectMindfulnessEnabled = enabled
        _uiState.value = _uiState.value.copy(healthConnectMindfulnessEnabled = enabled)
        // The declared mindfulness permission sets just changed shape, so the
        // availability, categories, and granted sets all need a re-read.
        refresh()
    }

    fun setHighHeartRateThresholdBpm(value: Int) {
        val current = _uiState.value
        val normalized = value
            .coerceAtLeast(current.lowHeartRateThresholdBpm + HeartRateThresholds.MINIMUM_GAP_BPM)
        preferencesRepository.highHeartRateThresholdBpm = normalized
        _uiState.value = current.copy(
            highHeartRateThresholdBpm = preferencesRepository.highHeartRateThresholdBpm,
        )
    }

    fun setLowHeartRateThresholdBpm(value: Int) {
        val current = _uiState.value
        val normalized = value
            .coerceAtMost(current.highHeartRateThresholdBpm - HeartRateThresholds.MINIMUM_GAP_BPM)
        preferencesRepository.lowHeartRateThresholdBpm = normalized
        _uiState.value = current.copy(
            lowHeartRateThresholdBpm = preferencesRepository.lowHeartRateThresholdBpm,
        )
    }

    fun setHydrationDailyGoalLiters(liters: Double) {
        preferencesRepository.hydrationDailyGoalLiters = liters
        _uiState.value = _uiState.value.copy(
            hydrationDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
        )
    }

    fun selectActivityWeekMode(activityWeekMode: ActivityWeekMode) {
        preferencesRepository.activityWeekMode = activityWeekMode
        _uiState.value = _uiState.value.copy(activityWeekMode = activityWeekMode)
    }

    fun setActivitySplitDistance(meters: Double) {
        val normalized = ActivitySplitDistance.normalize(meters)
        preferencesRepository.activitySplitDistanceMeters = normalized
        _uiState.value = _uiState.value.copy(activitySplitDistanceMeters = normalized)
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

    /**
     * Commits the zone ladder, and the birth year when the calibration card owns
     * the field. Here it does not — the Body profile card above it does — so
     * [birthYear] is normally null and the profile is left alone.
     */
    fun updateBodyEnergyCalibration(calibration: BodyEnergyCalibration, birthYear: Int? = null) {
        if (birthYear != null) {
            updateBodyProfile(preferencesRepository.bodyProfile().copy(birthYear = birthYear))
        }
        preferencesRepository.setBodyEnergyCalibration(calibration.copy(setupCompleted = true))
        _uiState.value = _uiState.value.copy(bodyEnergyCalibration = preferencesRepository.bodyEnergyCalibration())
    }

    fun updateCaffeinePreferences(preferences: CaffeinePreferences) {
        preferencesRepository.setCaffeinePreferences(preferences)
        _uiState.value = _uiState.value.copy(caffeinePreferences = preferencesRepository.caffeinePreferences())
    }

    fun updateBodyProfile(profile: BodyProfile) {
        val previous = _uiState.value.bodyProfile
        preferencesRepository.setBodyProfile(profile)
        val saved = preferencesRepository.bodyProfile()
        _uiState.value = _uiState.value.copy(bodyProfile = saved)
        if (!_uiState.value.canWriteBodyMeasurements) return
        // A changed weight or height is written to Health Connect as a real
        // measurement, so BMI, FFMI and the caffeine half-life all move
        // together instead of the app holding two of each number. Only on a
        // real change: saving an unchanged card must not litter the body
        // history with a duplicate entry every time it is opened.
        viewModelScope.launch {
            val now = Instant.now()
            suspend fun write(type: BodyMeasurementType, value: Double?) {
                if (value == null) return
                runCatching {
                    bodyRepository.writeBodyMeasurementEntry(
                        BodyMeasurementWriteRequest(type = type, time = now, value = value),
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Body measurement write failed type=$type", error)
                }
            }
            if (saved.weightKg != previous.weightKg) {
                write(BodyMeasurementType.WEIGHT, saved.weightKg)
            }
            if (saved.heightCm != previous.heightCm) {
                write(BodyMeasurementType.HEIGHT, saved.heightCm)
            }
        }
    }

    /**
     * Returns the personal gains to neutral, leaving the user's own zone
     * settings alone — they did not learn anything, so there is nothing there
     * to unlearn.
     */
    fun resetBodyEnergyPersonalTuning() {
        val current = preferencesRepository.bodyEnergyCalibration()
        updateBodyEnergyCalibration(
            current.copy(
                sleepChargeGain = 1.0,
                activityDrainGain = 1.0,
                basalDrainGain = 1.0,
                stressDrainGain = 1.0,
            )
        )
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
