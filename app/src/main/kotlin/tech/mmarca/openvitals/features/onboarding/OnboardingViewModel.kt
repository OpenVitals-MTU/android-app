package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.PermissionGrantMode
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val mindfulnessAvailable: Boolean = false,
    val phase1Granted: Boolean = false,
    val phase2Granted: Boolean = false,
    val phase3Granted: Boolean = false,
    val phase4Granted: Boolean = false,
    val cycleTrackingEnabled: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val isCheckingPermissions: Boolean = true,
)

data class OnboardingPermissionCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val permissions: Set<String>,
    val required: Boolean = false,
    val optIn: Boolean = false,
    val grantMode: PermissionGrantMode = PermissionGrantMode.REQUESTABLE,
    val available: Boolean = true,
    @StringRes val unavailableReasonRes: Int? = null,
)

class OnboardingViewModel(
    private val repository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val phase1Permissions get() = repository.phase1Permissions
    val phase2Permissions get() = repository.phase2Permissions
    val phase3Permissions get() = repository.phase3Permissions
    val phase4Permissions get() = repository.phase4Permissions
    val onboardingPermissions get() = repository.onboardingPermissions
    val permissionCategories: List<OnboardingPermissionCategory>
        get() = listOf(
            OnboardingPermissionCategory(
                id = "activity_sleep",
                titleRes = R.string.onboarding_category_activity_sleep,
                descriptionRes = R.string.onboarding_category_activity_sleep_desc,
                permissions = repository.corePermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = "workout_routes",
                titleRes = R.string.onboarding_category_workout_routes,
                descriptionRes = R.string.onboarding_category_workout_routes_desc,
                permissions = repository.routePermissions,
                grantMode = grantModeFor(repository.routePermissions),
            ),
            OnboardingPermissionCategory(
                id = "heart_recovery",
                titleRes = R.string.onboarding_category_heart_recovery,
                descriptionRes = R.string.onboarding_category_heart_recovery_desc,
                permissions = repository.heartPermissions,
            ),
            OnboardingPermissionCategory(
                id = "body",
                titleRes = R.string.onboarding_category_body,
                descriptionRes = R.string.onboarding_category_body_desc,
                permissions = repository.bodyPermissions,
            ),
            OnboardingPermissionCategory(
                id = "activity_extras",
                titleRes = R.string.onboarding_category_activity_extras,
                descriptionRes = R.string.onboarding_category_activity_extras_desc,
                permissions = repository.activityExtrasPermissions,
            ),
            OnboardingPermissionCategory(
                id = "nutrition_hydration",
                titleRes = R.string.onboarding_category_nutrition_hydration,
                descriptionRes = R.string.onboarding_category_nutrition_hydration_desc,
                permissions = repository.nutritionHydrationPermissions,
            ),
            OnboardingPermissionCategory(
                id = "mindfulness",
                titleRes = R.string.onboarding_category_mindfulness,
                descriptionRes = R.string.onboarding_category_mindfulness_desc,
                permissions = repository.mindfulnessPermissions,
                available = _uiState.value.mindfulnessAvailable,
                unavailableReasonRes = R.string.onboarding_category_mindfulness_unavailable,
            ),
            OnboardingPermissionCategory(
                id = "vitals",
                titleRes = R.string.onboarding_category_vitals,
                descriptionRes = R.string.onboarding_category_vitals_desc,
                permissions = repository.vitalsPermissions,
            ),
            OnboardingPermissionCategory(
                id = "cycle_tracking",
                titleRes = R.string.onboarding_category_cycle_tracking,
                descriptionRes = R.string.onboarding_category_cycle_tracking_desc,
                permissions = repository.cyclePermissions,
                optIn = true,
            ),
        ).filter { it.permissions.isNotEmpty() }

    private fun grantModeFor(permissions: Set<String>): PermissionGrantMode =
        if (permissions.isNotEmpty() && permissions.all { repository.grantModeFor(it) == PermissionGrantMode.MANUAL }) {
            PermissionGrantMode.MANUAL
        } else {
            PermissionGrantMode.REQUESTABLE
        }

    init {
        checkState()
    }

    fun checkState() {
        viewModelScope.launch {
            val avail = repository.availability()
            Log.d(TAG, "checkState availability=$avail")
            if (avail != HealthConnectAvailability.AVAILABLE) {
                _uiState.value = OnboardingUiState(
                    availability = avail,
                    appLanguage = preferencesRepository.appLanguage,
                    isCheckingPermissions = false,
                )
                return@launch
            }
            val mindfulnessAvailable = repository.isMindfulnessAvailable()
            val granted = repository.grantedPermissions()
            Log.d(TAG, "checkState granted=${granted.sorted()}")
            _uiState.value = OnboardingUiState(
                availability = avail,
                grantedPermissions = granted,
                mindfulnessAvailable = mindfulnessAvailable,
                phase1Granted = repository.phase1Permissions.all { it in granted },
                phase2Granted = repository.phase2Permissions.all { it in granted },
                phase3Granted = repository.phase3Permissions.all { it in granted },
                phase4Granted = repository.phase4Permissions.all { it in granted },
                cycleTrackingEnabled = preferencesRepository.trackCycle,
                appLanguage = preferencesRepository.appLanguage,
                isCheckingPermissions = false,
            )
        }
    }

    fun enableCycleTracking() {
        preferencesRepository.trackCycle = true
        _uiState.value = _uiState.value.copy(cycleTrackingEnabled = true)
    }

    fun onPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            Log.d(TAG, "onPermissionsResult callbackGranted=${granted.sorted()}")
            val allGranted = repository.grantedPermissions()
            Log.d(TAG, "onPermissionsResult allGranted=${allGranted.sorted()}")
            _uiState.value = _uiState.value.copy(
                grantedPermissions = allGranted,
                phase1Granted = repository.phase1Permissions.all { it in allGranted },
                phase2Granted = repository.phase2Permissions.all { it in allGranted },
                phase3Granted = repository.phase3Permissions.all { it in allGranted },
                phase4Granted = repository.phase4Permissions.all { it in allGranted },
                cycleTrackingEnabled = preferencesRepository.trackCycle,
            )
        }
    }

    fun selectAppLanguage(appLanguage: AppLanguage) {
        preferencesRepository.appLanguage = appLanguage
        _uiState.value = _uiState.value.copy(appLanguage = appLanguage)
    }
}
