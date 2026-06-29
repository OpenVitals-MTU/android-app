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
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportPhase
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportProgress
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportResult
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportWorkController
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPack
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPackFormat
import tech.mmarca.openvitals.features.activity.maps.OfflineMapRepository
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportPhase
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportProgress
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.data.cache.MetricSummaryCacheStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.work.WorkInfo
import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val permissionCategories: List<SettingsPermissionCategory> = emptyList(),
    val allPermissions: Set<String> = emptySet(),
    val cyclePermissions: Set<String> = emptySet(),
    val dataImportWritePermissions: Set<String> = emptySet(),
    val manualOnlyPermissions: Set<String> = emptySet(),
    val isImportingAppleHealth: Boolean = false,
    val appleHealthImportProgress: AppleHealthImportProgress? = null,
    val appleHealthImportResult: AppleHealthImportResult? = null,
    val appleHealthImportError: String? = null,
    val offlineMapPacks: List<OfflineMapPack> = emptyList(),
    val activeOfflineMapFormat: OfflineMapPackFormat? = null,
    val isImportingOfflineMap: Boolean = false,
    val offlineMapImportProgress: OfflineMapImportProgress? = null,
    val offlineMapImportResult: OfflineMapImportResult? = null,
    val offlineMapImportError: String? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val activityRecordingPreferences: ActivityRecordingPreferences = ActivityRecordingPreferences(),
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val favoriteActivityExerciseType: Int? = null,
    val healthConnectSyncEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
) {
    val visiblePermissions: Set<String>
        get() = permissionCategories.flatMap { it.permissions }.toSet()

    val missingVisiblePermissions: Set<String>
        get() = visiblePermissions - grantedPermissions

    val missingManualVisiblePermissions: Set<String>
        get() = missingVisiblePermissions.intersect(manualOnlyPermissions)

    val missingDataImportWritePermissions: Set<String>
        get() = dataImportWritePermissions - grantedPermissions
}

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
    private val preferencesRepository: PreferencesRepository,
    private val appleHealthImportWorkController: AppleHealthImportWorkController,
    private val offlineMapRepository: OfflineMapRepository,
    private val offlineMapImportWorkController: OfflineMapImportWorkController,
    private val permissionUxState: HealthConnectPermissionUxState,
    private val metricSummaryCacheStore: MetricSummaryCacheStore,
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
                cyclePermissions = repository.cyclePermissions,
                dataImportWritePermissions = repository.dataImportWritePermissions,
                manualOnlyPermissions = repository.manualOnlyPermissions,
                unitSystem = preferencesRepository.unitSystem,
                appLanguage = preferencesRepository.appLanguage,
                appThemeMode = preferencesRepository.appThemeMode,
                sleepRangeMode = preferencesRepository.sleepRangeMode,
                activityWeekMode = preferencesRepository.activityWeekMode,
                activityRecordingPreferences = preferencesRepository.activityRecordingPreferences(),
                showOpenVitalsCalculatedCalories = preferencesRepository.showOpenVitalsCalculatedCalories,
                favoriteActivityExerciseType = preferencesRepository.favoriteActivityExerciseType,
                healthConnectSyncEnabled = preferencesRepository.healthConnectSyncEnabled,
                appLockEnabled = preferencesRepository.appLockEnabled,
            )
        }
    }

    fun importAppleHealthExport(uri: Uri) {
        if (_uiState.value.isImportingAppleHealth) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingAppleHealth = true,
                appleHealthImportProgress = AppleHealthImportProgress(phase = AppleHealthImportPhase.QUEUED),
                appleHealthImportResult = null,
                appleHealthImportError = null,
            )

            runCatching { appleHealthImportWorkController.enqueue(uri) }
                .onFailure { error ->
                    Log.e(TAG, "Apple Health import enqueue failed type=${error::class.java.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isImportingAppleHealth = false,
                        appleHealthImportProgress = null,
                        appleHealthImportResult = null,
                        appleHealthImportError = error.localizedMessage
                            ?: "Apple Health import failed.",
                    )
                }
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
                val workInfo = workInfos.firstOrNull() ?: return@collect
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING,
                    -> {
                        _uiState.value = _uiState.value.copy(
                            isImportingAppleHealth = true,
                            appleHealthImportProgress = appleHealthImportWorkController.progressFor(workInfo)
                                ?: AppleHealthImportProgress(phase = AppleHealthImportPhase.QUEUED),
                            appleHealthImportResult = null,
                            appleHealthImportError = null,
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
                            appleHealthImportProgress = null,
                            appleHealthImportResult = result,
                            appleHealthImportError = null,
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = appleHealthImportWorkController.errorFor(workInfo)
                            ?: "Apple Health import failed."
                        Log.e(TAG, "Apple Health import failed")
                        _uiState.value = _uiState.value.copy(
                            isImportingAppleHealth = false,
                            appleHealthImportProgress = null,
                            appleHealthImportResult = null,
                            appleHealthImportError = error,
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

    fun clearCachedSummaries() {
        viewModelScope.launch {
            metricSummaryCacheStore.clearAll()
        }
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
