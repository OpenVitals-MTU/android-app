package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PermissionGrantMode
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import javax.inject.Inject
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
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val isCheckingPermissions: Boolean = true,
)

data class OnboardingPermissionCategory(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val permissions: Set<String>,
    val manualPermissions: Set<String> = emptySet(),
    val required: Boolean = false,
    val grantMode: PermissionGrantMode = PermissionGrantMode.REQUESTABLE,
    val available: Boolean = true,
    @param:StringRes val unavailableReasonRes: Int? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val permissionUxState: HealthConnectPermissionUxState,
) : ViewModel() {
    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val phase1Permissions get() = repository.phase1Permissions
    val minimumOnboardingPermissions get() = repository.minimumOnboardingPermissions
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
                id = "heart_recovery",
                titleRes = R.string.onboarding_category_heart_recovery,
                descriptionRes = R.string.onboarding_category_heart_recovery_desc,
                permissions = repository.heartPermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = "vitals",
                titleRes = R.string.onboarding_category_vitals,
                descriptionRes = R.string.onboarding_category_vitals_desc,
                permissions = repository.vitalsPermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = "body",
                titleRes = R.string.onboarding_category_body,
                descriptionRes = R.string.onboarding_category_body_desc,
                permissions = repository.bodyPermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "activity_extras",
                titleRes = R.string.onboarding_category_activity_extras,
                descriptionRes = R.string.onboarding_category_activity_extras_desc,
                permissions = repository.activityExtrasPermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "nutrition_hydration",
                titleRes = R.string.onboarding_category_nutrition_hydration,
                descriptionRes = R.string.onboarding_category_nutrition_hydration_desc,
                permissions = repository.nutritionHydrationPermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "manual_entry_write",
                titleRes = R.string.onboarding_category_manual_entry_write,
                descriptionRes = R.string.onboarding_category_manual_entry_write_desc,
                permissions = repository.requestableWritePermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "data_import_write",
                titleRes = R.string.onboarding_category_data_import_write,
                descriptionRes = R.string.onboarding_category_data_import_write_desc,
                permissions = repository.dataImportWritePermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "mindfulness",
                titleRes = R.string.onboarding_category_mindfulness,
                descriptionRes = R.string.onboarding_category_mindfulness_desc,
                permissions = repository.mindfulnessPermissions,
                available = _uiState.value.mindfulnessAvailable,
                unavailableReasonRes = R.string.onboarding_category_mindfulness_unavailable,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "additional_data_access",
                titleRes = R.string.onboarding_category_additional_data_access,
                descriptionRes = R.string.onboarding_category_additional_data_access_desc,
                permissions = repository.additionalDataAccessPermissions + repository.routePermissions,
                manualPermissions = repository.routePermissions,
                required = false,
            ),
            OnboardingPermissionCategory(
                id = "cycle_tracking",
                titleRes = R.string.onboarding_category_cycle_tracking,
                descriptionRes = R.string.onboarding_category_cycle_tracking_desc,
                permissions = repository.cyclePermissions,
                required = false,
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
            Log.d(TAG, "checkState grantedCount=${granted.size}")
            _uiState.value = OnboardingUiState(
                availability = avail,
                grantedPermissions = granted,
                mindfulnessAvailable = mindfulnessAvailable,
                phase1Granted = repository.phase1Permissions.all { it in granted },
                phase2Granted = repository.phase2Permissions.all { it in granted },
                phase3Granted = repository.phase3Permissions.all { it in granted },
                phase4Granted = repository.phase4Permissions.all { it in granted },
                appLanguage = preferencesRepository.appLanguage,
                isCheckingPermissions = false,
            )
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            Log.d(TAG, "onPermissionsResult callbackGrantedCount=${granted.size}")
            if (granted.isEmpty()) {
                permissionUxState.recordPermissionRequestCancelled()
            } else {
                permissionUxState.recordPermissionRequestGranted()
            }
            val allGranted = repository.grantedPermissions()
            Log.d(TAG, "onPermissionsResult allGrantedCount=${allGranted.size}")
            _uiState.value = _uiState.value.copy(
                grantedPermissions = allGranted,
                phase1Granted = repository.phase1Permissions.all { it in allGranted },
                phase2Granted = repository.phase2Permissions.all { it in allGranted },
                phase3Granted = repository.phase3Permissions.all { it in allGranted },
                phase4Granted = repository.phase4Permissions.all { it in allGranted },
            )
        }
    }

    fun selectAppLanguage(appLanguage: AppLanguage) {
        preferencesRepository.appLanguage = appLanguage
        _uiState.value = _uiState.value.copy(appLanguage = appLanguage)
    }

    fun completeOnboarding() {
        preferencesRepository.acceptedPrivacyPolicyVersion = PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION
        preferencesRepository.privacyPolicyAcceptedAtMillis = System.currentTimeMillis()
        preferencesRepository.onboardingDone = true
    }

    fun shouldShowDoubleCancelRecovery(): Boolean = permissionUxState.shouldShowDoubleCancelRecovery()
}
