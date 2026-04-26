package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isCheckingPermissions: Boolean = true,
)

data class OnboardingPermissionCategory(
    val id: String,
    val title: String,
    val description: String,
    val permissions: Set<String>,
    val required: Boolean = false,
    val optIn: Boolean = false,
    val grantMode: PermissionGrantMode = PermissionGrantMode.REQUESTABLE,
    val available: Boolean = true,
    val unavailableReason: String? = null,
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
                title = "Activity & sleep",
                description = "Steps, distance, workouts, and sleep sessions for the dashboard.",
                permissions = repository.corePermissions,
                required = true,
            ),
            OnboardingPermissionCategory(
                id = "workout_routes",
                title = "Workout routes",
                description = "Route previews require manual approval in Health Connect settings.",
                permissions = repository.routePermissions,
                grantMode = grantModeFor(repository.routePermissions),
            ),
            OnboardingPermissionCategory(
                id = "heart_recovery",
                title = "Heart & recovery",
                description = "Heart rate, resting heart rate, and HRV trends.",
                permissions = repository.heartPermissions,
            ),
            OnboardingPermissionCategory(
                id = "body",
                title = "Body",
                description = "Weight, height, body fat, lean mass, bone mass, and BMR.",
                permissions = repository.bodyPermissions,
            ),
            OnboardingPermissionCategory(
                id = "activity_extras",
                title = "Activity extras",
                description = "Calories burned, floors climbed, active calories, and elevation.",
                permissions = repository.activityExtrasPermissions,
            ),
            OnboardingPermissionCategory(
                id = "nutrition_hydration",
                title = "Nutrition & hydration",
                description = "Water intake, calories in, meals, and macros.",
                permissions = repository.nutritionHydrationPermissions,
            ),
            OnboardingPermissionCategory(
                id = "mindfulness",
                title = "Mindfulness",
                description = "Mindfulness session duration and history.",
                permissions = repository.mindfulnessPermissions,
                available = _uiState.value.mindfulnessAvailable,
                unavailableReason = "Mindfulness sessions require a newer Health Connect version.",
            ),
            OnboardingPermissionCategory(
                id = "vitals",
                title = "Vitals",
                description = "Blood pressure, oxygen saturation, respiratory rate, body temperature, and VO2 max.",
                permissions = repository.vitalsPermissions,
            ),
            OnboardingPermissionCategory(
                id = "cycle_tracking",
                title = "Cycle tracking",
                description = "Optional sensitive cycle data: period dates, flow, ovulation, cervical mucus, and basal body temperature.",
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
}
