package tech.mmarca.openvitals.features.settings

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.contract.HealthConnectPreferences
import tech.mmarca.openvitals.data.repository.contract.HeartThresholdPreferences
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val permissionCategories: List<SettingsPermissionCategory> = emptyList(),
    val allPermissions: Set<String> = emptySet(),
    val manualOnlyPermissions: Set<String> = emptySet(),
    val bloodPressureGuideline: BloodPressureGuideline = BloodPressureGuideline.ACC_AHA_2017,
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectMindfulnessEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val healthConnectSources: List<HealthConnectSource> = emptyList(),
) {
    val visiblePermissions: Set<String>
        get() = permissionCategories.flatMap { it.permissions }.toSet()

    val missingVisiblePermissions: Set<String>
        get() = visiblePermissions - grantedPermissions

    val missingManualVisiblePermissions: Set<String>
        get() = missingVisiblePermissions.intersect(manualOnlyPermissions)
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

/**
 * The Settings root and the sections without a ViewModel of their own: Health Connect,
 * Vitals and Diagnostics.
 *
 * Display, Activities, Nutrition, Body profile, Recovery, Data transfer, Watches and Sync
 * with another phone each have one, so a section route builds only what it shows.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val heartRepository: HeartRepository,
    private val sleepRepository: SleepRepository,
    private val hydrationReminderController: HydrationReminderController,
    private val healthConnectPreferences: HealthConnectPreferences,
    private val heartThresholdPreferences: HeartThresholdPreferences,
    private val permissionUxState: HealthConnectPermissionUxState,
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
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
                manualOnlyPermissions = repository.manualOnlyPermissions,
                bloodPressureGuideline = heartThresholdPreferences.bloodPressureGuideline,
                healthConnectSyncEnabled = healthConnectPreferences.healthConnectSyncEnabled,
                healthConnectMindfulnessEnabled = healthConnectPreferences.healthConnectMindfulnessEnabled,
                appLockEnabled = healthConnectPreferences.appLockEnabled,
            )
            loadHealthConnectSources()
        }
    }

    /** Diagnostics: the contributors seen in the last week of heart-rate and sleep data. */
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

    fun setBloodPressureGuideline(guideline: BloodPressureGuideline) {
        heartThresholdPreferences.bloodPressureGuideline = guideline
        _uiState.value = _uiState.value.copy(bloodPressureGuideline = guideline)
    }

    /** Diagnostics: posts the hydration reminder immediately via the real path. */
    fun showTestHydrationReminder() {
        hydrationReminderController.showTestReminder()
    }

    fun setHealthConnectMindfulnessEnabled(enabled: Boolean) {
        healthConnectPreferences.healthConnectMindfulnessEnabled = enabled
        _uiState.value = _uiState.value.copy(healthConnectMindfulnessEnabled = enabled)
        // The declared mindfulness permission sets changed shape; re-read everything.
        refresh()
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
        healthConnectPreferences.healthConnectSyncEnabled = enabled
        _uiState.value = _uiState.value.copy(healthConnectSyncEnabled = enabled)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        healthConnectPreferences.appLockEnabled = enabled
        _uiState.value = _uiState.value.copy(appLockEnabled = enabled)
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
