package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.OnboardingCategoryId
import tech.mmarca.openvitals.domain.model.OnboardingPermissionCatalog
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState

/**
 * The wizard's steps. An enum rather than routes: the flow is one screen whose
 * content switches, so the lifecycle observer and permission launcher outlive
 * step changes. Steps that do not apply on this device are skipped entirely.
 */
enum class OnboardingStep {
    CATEGORIES,
    MINDFULNESS,
    CYCLE_TRACKING,
    ADDITIONAL_ACCESS,
}

/** One permission row, precomputed for rendering. */
@Immutable
data class OnboardingRow(
    val id: OnboardingCategoryId,
    val permissions: Set<String>,
    val required: Boolean,
    val available: Boolean,
    val grantedCount: Int,
) {
    val total: Int get() = permissions.size
    val fullyGranted: Boolean get() = available && total > 0 && grantedCount == total
    val partial: Boolean get() = grantedCount in 1 until total
}

@Immutable
data class OnboardingUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val step: OnboardingStep = OnboardingStep.CATEGORIES,
    val mindfulnessSupportedByDevice: Boolean = false,
    val mindfulnessOptIn: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val isCheckingPermissions: Boolean = true,
    val catalog: OnboardingPermissionCatalog? = null,
    /**
     * Bumped when a permission request achieved nothing — either the user
     * refused or Health Connect silently stopped asking (it does after two
     * cancels). Both look identical from the contract, and both are fixed the
     * same way: the screen opens Health Connect's permission settings.
     */
    val openSettingsEvent: Long = 0L,
) {
    private fun row(id: OnboardingCategoryId): OnboardingRow? {
        val category = catalog?.category(id) ?: return null
        return OnboardingRow(
            id = category.id,
            permissions = category.permissions,
            required = category.required,
            available = category.available,
            grantedCount = category.permissions.count { it in grantedPermissions },
        )
    }

    /** Step one's rows, in Health Connect's own category order. */
    val categoryRows: List<OnboardingRow>
        get() = listOf(
            OnboardingCategoryId.ACTIVITY,
            OnboardingCategoryId.BODY,
            OnboardingCategoryId.NUTRITION,
            OnboardingCategoryId.SLEEP,
            OnboardingCategoryId.VITALS,
        ).mapNotNull(::row)

    val mindfulnessRow: OnboardingRow? get() = row(OnboardingCategoryId.MINDFULNESS)
    val cycleRow: OnboardingRow? get() = row(OnboardingCategoryId.CYCLE_TRACKING)
    val additionalAccessRow: OnboardingRow? get() = row(OnboardingCategoryId.ADDITIONAL_ACCESS)

    val requiredGranted: Boolean
        get() = catalog?.requiredPermissions?.all { it in grantedPermissions } == true

    /** Health Connect keeps route reads behind a setting no app can request. */
    val routesOutstanding: Boolean
        get() = catalog?.routeReadPermission?.let { it !in grantedPermissions } == true

    fun stepApplies(step: OnboardingStep): Boolean = when (step) {
        OnboardingStep.CATEGORIES -> true
        OnboardingStep.MINDFULNESS -> mindfulnessSupportedByDevice
        OnboardingStep.CYCLE_TRACKING -> cycleRow != null
        OnboardingStep.ADDITIONAL_ACCESS -> additionalAccessRow != null || routesOutstanding
    }

    fun stepAfter(step: OnboardingStep): OnboardingStep? =
        OnboardingStep.entries.drop(step.ordinal + 1).firstOrNull(::stepApplies)

    fun stepBefore(step: OnboardingStep): OnboardingStep? =
        OnboardingStep.entries.take(step.ordinal).lastOrNull(::stepApplies)

    val isFirstStep: Boolean get() = stepBefore(step) == null
    val isLastStep: Boolean get() = stepAfter(step) == null

    /** Step one is the only gate: Activity and Sleep must be granted. */
    val canAdvance: Boolean get() = step != OnboardingStep.CATEGORIES || requiredGranted

    /** Whether the current step's ask has been answered (drives Next vs Not now). */
    val currentStepSatisfied: Boolean
        get() = when (step) {
            OnboardingStep.CATEGORIES -> requiredGranted
            OnboardingStep.MINDFULNESS -> mindfulnessOptIn && mindfulnessRow?.fullyGranted == true
            OnboardingStep.CYCLE_TRACKING -> cycleRow?.fullyGranted == true
            OnboardingStep.ADDITIONAL_ACCESS ->
                additionalAccessRow?.fullyGranted != false && !routesOutstanding
        }
}

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

    /** The set launched by the in-flight permission request, for the grant diff. */
    private var pendingRequest: Set<String>? = null
    private var grantedBeforeRequest: Set<String> = emptySet()

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
            val catalog = repository.onboardingPermissionCatalog()
            val granted = repository.grantedPermissions()
            Log.d(TAG, "checkState grantedCount=${granted.size}")
            _uiState.value = OnboardingUiState(
                availability = avail,
                grantedPermissions = granted,
                step = _uiState.value.step,
                mindfulnessSupportedByDevice = catalog.mindfulnessSupportedByDevice,
                mindfulnessOptIn = preferencesRepository.healthConnectMindfulnessEnabled,
                appLanguage = preferencesRepository.appLanguage,
                isCheckingPermissions = false,
                catalog = catalog,
                openSettingsEvent = _uiState.value.openSettingsEvent,
            )
        }
    }

    /** The requestable permissions a row is still missing. */
    fun missingRequestableFor(id: OnboardingCategoryId): Set<String> {
        val state = _uiState.value
        val category = state.catalog?.category(id) ?: return emptySet()
        return category.permissions - state.grantedPermissions
    }

    /** Called just before the screen launches the permission contract. */
    fun beginPermissionRequest(permissions: Set<String>) {
        pendingRequest = permissions
        grantedBeforeRequest = _uiState.value.grantedPermissions
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
            val requested = pendingRequest
            pendingRequest = null
            // A refusal and a permission Health Connect will no longer ask for
            // are indistinguishable from the contract; the before/after diff is
            // the only signal. No gain and not already granted → the dialog
            // achieved nothing, so send the user to the settings page instead.
            val gainedAny = (allGranted - grantedBeforeRequest).isNotEmpty()
            val alreadyGranted = requested != null && requested.all { it in grantedBeforeRequest }
            val needsManualGrant = requested != null && !gainedAny && !alreadyGranted
            _uiState.value = _uiState.value.copy(
                grantedPermissions = allGranted,
                openSettingsEvent = if (needsManualGrant) {
                    _uiState.value.openSettingsEvent + 1
                } else {
                    _uiState.value.openSettingsEvent
                },
            )
        }
    }

    fun setMindfulnessOptIn(enabled: Boolean) {
        // Both keys: the legacy opt-in for older readers, and the settings
        // toggle that now gates the whole mindfulness integration.
        preferencesRepository.mindfulnessOptIn = enabled
        preferencesRepository.healthConnectMindfulnessEnabled = enabled
        _uiState.value = _uiState.value.copy(mindfulnessOptIn = enabled)
        // The catalog derives the mindfulness permissions from this very
        // preference, so the cached one is now stale: it still holds no
        // mindfulness category, which would leave the row missing and the Grant
        // button requesting an empty set. Rebuilding is one cheap round-trip on
        // a deliberate toggle, and keeps the catalog the single source of truth.
        checkState()
    }

    fun next() {
        val state = _uiState.value
        val next = state.stepAfter(state.step) ?: return
        _uiState.value = state.copy(step = next)
    }

    fun back() {
        val state = _uiState.value
        val previous = state.stepBefore(state.step) ?: return
        _uiState.value = state.copy(step = previous)
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
}
