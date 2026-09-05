package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository

/**
 * A tile tap checks exactly that tile's Health Connect write set. When it is
 * missing, `pendingXWritePermissionRequest` asks the screen to launch the
 * Health Connect dialog for just those permissions; the screen clears the
 * trigger via `onXWritePermissionRequestLaunched` before launching, so a
 * configuration change during the dialog never launches it twice. The result,
 * granted or denied, always lands on the entry form, which carries its own
 * "needs write permission" state and Grant button.
 */
@Immutable
data class ManualEntryUiState(
    val widgets: List<ManualEntryWidgetId> = DefaultManualEntryWidgetIds,
    val isEditingWidgets: Boolean = false,
    val isCheckingHydrationWritePermission: Boolean = false,
    val hydrationWritePermissions: Set<String> = emptySet(),
    val canWriteHydration: Boolean = false,
    val pendingHydrationWritePermissionRequest: Boolean = false,
    val pendingHydrationEntryNavigation: Boolean = false,
    val nutritionWritePermissions: Set<String> = emptySet(),
    val isCheckingNutritionWritePermission: Boolean = false,
    val canWriteNutrition: Boolean = false,
    val pendingCarbsWritePermissionRequest: Boolean = false,
    val pendingCarbsEntryNavigation: Boolean = false,
    val activityWritePermissions: Set<String> = emptySet(),
    val isCheckingActivityWritePermission: Boolean = false,
    val pendingActivityWritePermissionRequest: Boolean = false,
    val pendingActivityEntryNavigation: Boolean = false,
    val bodyWritePermissions: Set<String> = emptySet(),
    val isCheckingBodyWritePermission: Boolean = false,
    val pendingBodyWritePermissionRequest: BodyMeasurementType? = null,
    val bodyWritePermissionRequestType: BodyMeasurementType? = null,
    val pendingBodyEntryNavigation: BodyMeasurementType? = null,
    val vitalsWritePermissions: Set<String> = emptySet(),
    val isCheckingVitalsWritePermission: Boolean = false,
    val pendingVitalsWritePermissionRequest: VitalsMeasurementType? = null,
    val vitalsWritePermissionRequestType: VitalsMeasurementType? = null,
    val pendingVitalsEntryNavigation: VitalsMeasurementType? = null,
    val mindfulnessWritePermissions: Set<String> = emptySet(),
    val isCheckingMindfulnessWritePermission: Boolean = false,
    val pendingMindfulnessWritePermissionRequest: Boolean = false,
    val pendingMindfulnessEntryNavigation: Boolean = false,
    val cycleWritePermissions: Set<String> = emptySet(),
    val isCheckingCycleWritePermission: Boolean = false,
    val pendingCycleWritePermissionRequest: Boolean = false,
    val pendingCycleEntryNavigation: Boolean = false,
)

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository,
    private val bodyRepository: BodyRepository,
    private val vitalsRepository: VitalsRepository,
    private val mindfulnessRepository: MindfulnessRepository,
    private val cycleRepository: CycleRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ManualEntryUiState(
            widgets = manualEntryWidgetIdsFromStored(preferencesRepository.manualEntryWidgetOrder()),
        )
    )
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    fun onHydrationWidgetTapped() {
        if (_uiState.value.isCheckingHydrationWritePermission) return
        viewModelScope.launch {
            val writePermissions = hydrationRepository.hydrationWritePermissions
            _uiState.value = _uiState.value.copy(
                isCheckingHydrationWritePermission = true,
                hydrationWritePermissions = writePermissions,
                pendingHydrationWritePermissionRequest = false,
                pendingHydrationEntryNavigation = false,
            )
            runCatching {
                hydrationRepository.hasHydrationWritePermission()
            }.onSuccess { canWriteHydration ->
                val request = shouldRequest(canWriteHydration, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingHydrationWritePermission = false,
                    canWriteHydration = canWriteHydration,
                    pendingHydrationWritePermissionRequest = request,
                    pendingHydrationEntryNavigation = !request,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingHydrationWritePermission = false,
                    canWriteHydration = false,
                    pendingHydrationEntryNavigation = true,
                )
            }
        }
    }

    fun onCarbsWidgetTapped() {
        if (_uiState.value.isCheckingNutritionWritePermission) return
        viewModelScope.launch {
            val writePermissions = nutritionRepository.nutritionWritePermissions
            _uiState.value = _uiState.value.copy(
                isCheckingNutritionWritePermission = true,
                nutritionWritePermissions = writePermissions,
                pendingCarbsWritePermissionRequest = false,
                pendingCarbsEntryNavigation = false,
            )
            runCatching {
                nutritionRepository.hasNutritionWritePermission()
            }.onSuccess { canWriteNutrition ->
                val request = shouldRequest(canWriteNutrition, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingNutritionWritePermission = false,
                    canWriteNutrition = canWriteNutrition,
                    pendingCarbsWritePermissionRequest = request,
                    pendingCarbsEntryNavigation = !request,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingNutritionWritePermission = false,
                    canWriteNutrition = false,
                    pendingCarbsEntryNavigation = true,
                )
            }
        }
    }

    fun onActivityWidgetTapped() {
        if (_uiState.value.isCheckingActivityWritePermission) return
        viewModelScope.launch {
            val writePermissions = activityRepository.activityWritePermissions()
            _uiState.value = _uiState.value.copy(
                isCheckingActivityWritePermission = true,
                activityWritePermissions = writePermissions,
                pendingActivityWritePermissionRequest = false,
                pendingActivityEntryNavigation = false,
            )
            runCatching {
                activityRepository.hasActivityWritePermission()
            }.onSuccess { canWriteActivity ->
                val request = shouldRequest(canWriteActivity, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingActivityWritePermission = false,
                    pendingActivityWritePermissionRequest = request,
                    pendingActivityEntryNavigation = !request,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingActivityWritePermission = false,
                    pendingActivityEntryNavigation = true,
                )
            }
        }
    }

    fun onBodyMeasurementWidgetTapped(type: BodyMeasurementType) {
        if (_uiState.value.isCheckingBodyWritePermission) return
        viewModelScope.launch {
            val writePermissions = bodyRepository.bodyWritePermissions(type)
            _uiState.value = _uiState.value.copy(
                isCheckingBodyWritePermission = true,
                bodyWritePermissions = writePermissions,
                pendingBodyWritePermissionRequest = null,
                pendingBodyEntryNavigation = null,
            )
            runCatching {
                bodyRepository.hasBodyWritePermission(type)
            }.onSuccess { canWrite ->
                val request = shouldRequest(canWrite, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingBodyWritePermission = false,
                    pendingBodyWritePermissionRequest = if (request) type else null,
                    bodyWritePermissionRequestType = if (request) type else null,
                    pendingBodyEntryNavigation = if (request) null else type,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingBodyWritePermission = false,
                    pendingBodyEntryNavigation = type,
                )
            }
        }
    }

    fun onVitalsMeasurementWidgetTapped(type: VitalsMeasurementType) {
        if (_uiState.value.isCheckingVitalsWritePermission) return
        viewModelScope.launch {
            val writePermissions = vitalsRepository.vitalsWritePermissions(type)
            _uiState.value = _uiState.value.copy(
                isCheckingVitalsWritePermission = true,
                vitalsWritePermissions = writePermissions,
                pendingVitalsWritePermissionRequest = null,
                pendingVitalsEntryNavigation = null,
            )
            runCatching {
                vitalsRepository.hasVitalsWritePermission(type)
            }.onSuccess { canWrite ->
                val request = shouldRequest(canWrite, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingVitalsWritePermission = false,
                    pendingVitalsWritePermissionRequest = if (request) type else null,
                    vitalsWritePermissionRequestType = if (request) type else null,
                    pendingVitalsEntryNavigation = if (request) null else type,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingVitalsWritePermission = false,
                    pendingVitalsEntryNavigation = type,
                )
            }
        }
    }

    fun onMindfulnessWidgetTapped() {
        if (_uiState.value.isCheckingMindfulnessWritePermission) return
        viewModelScope.launch {
            val writePermissions = mindfulnessRepository.mindfulnessWritePermissions
            _uiState.value = _uiState.value.copy(
                isCheckingMindfulnessWritePermission = true,
                mindfulnessWritePermissions = writePermissions,
                pendingMindfulnessWritePermissionRequest = false,
                pendingMindfulnessEntryNavigation = false,
            )
            runCatching {
                mindfulnessRepository.hasMindfulnessWritePermission()
            }.onSuccess { canWrite ->
                val request = shouldRequest(canWrite, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingMindfulnessWritePermission = false,
                    pendingMindfulnessWritePermissionRequest = request,
                    pendingMindfulnessEntryNavigation = !request,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingMindfulnessWritePermission = false,
                    pendingMindfulnessEntryNavigation = true,
                )
            }
        }
    }

    fun onCycleWidgetTapped() {
        if (_uiState.value.isCheckingCycleWritePermission) return
        viewModelScope.launch {
            val writePermissions = CycleEntryKind.entries
                .flatMapTo(mutableSetOf()) { cycleRepository.cycleWritePermissions(it) }
            _uiState.value = _uiState.value.copy(
                isCheckingCycleWritePermission = true,
                cycleWritePermissions = writePermissions,
                pendingCycleWritePermissionRequest = false,
                pendingCycleEntryNavigation = false,
            )
            runCatching {
                CycleEntryKind.entries.any { cycleRepository.hasCycleWritePermission(it) }
            }.onSuccess { canWrite ->
                val request = shouldRequest(canWrite, writePermissions)
                _uiState.value = _uiState.value.copy(
                    isCheckingCycleWritePermission = false,
                    pendingCycleWritePermissionRequest = request,
                    pendingCycleEntryNavigation = !request,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingCycleWritePermission = false,
                    pendingCycleEntryNavigation = true,
                )
            }
        }
    }

    // ── request launched: clear the trigger so it fires exactly once ────────

    fun onHydrationWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingHydrationWritePermissionRequest = false)
    }

    fun onCarbsWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingCarbsWritePermissionRequest = false)
    }

    fun onActivityWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingActivityWritePermissionRequest = false)
    }

    fun onBodyWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingBodyWritePermissionRequest = null)
    }

    fun onVitalsWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingVitalsWritePermissionRequest = null)
    }

    fun onMindfulnessWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingMindfulnessWritePermissionRequest = false)
    }

    fun onCycleWritePermissionRequestLaunched() {
        _uiState.value = _uiState.value.copy(pendingCycleWritePermissionRequest = false)
    }

    // ── request result: granted or denied, the entry form opens ─────────────

    fun onHydrationWritePermissionResult() {
        viewModelScope.launch {
            val canWriteHydration = runCatching {
                hydrationRepository.hasHydrationWritePermission()
            }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                isCheckingHydrationWritePermission = false,
                canWriteHydration = canWriteHydration,
                pendingHydrationEntryNavigation = true,
            )
        }
    }

    fun onHydrationEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingHydrationEntryNavigation = false)
    }

    fun onNutritionWritePermissionResult() {
        viewModelScope.launch {
            val canWriteNutrition = runCatching {
                nutritionRepository.hasNutritionWritePermission()
            }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                isCheckingNutritionWritePermission = false,
                canWriteNutrition = canWriteNutrition,
                pendingCarbsEntryNavigation = true,
            )
        }
    }

    fun onCarbsEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingCarbsEntryNavigation = false)
    }

    fun onActivityWritePermissionResult() {
        _uiState.value = _uiState.value.copy(
            isCheckingActivityWritePermission = false,
            pendingActivityEntryNavigation = true,
        )
    }

    fun onActivityEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingActivityEntryNavigation = false)
    }

    fun onBodyWritePermissionResult() {
        val type = _uiState.value.bodyWritePermissionRequestType ?: return
        _uiState.value = _uiState.value.copy(
            isCheckingBodyWritePermission = false,
            bodyWritePermissionRequestType = null,
            pendingBodyEntryNavigation = type,
        )
    }

    fun onBodyEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingBodyEntryNavigation = null)
    }

    fun onVitalsWritePermissionResult() {
        val type = _uiState.value.vitalsWritePermissionRequestType ?: return
        _uiState.value = _uiState.value.copy(
            isCheckingVitalsWritePermission = false,
            vitalsWritePermissionRequestType = null,
            pendingVitalsEntryNavigation = type,
        )
    }

    fun onVitalsEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingVitalsEntryNavigation = null)
    }

    fun onMindfulnessWritePermissionResult() {
        _uiState.value = _uiState.value.copy(
            isCheckingMindfulnessWritePermission = false,
            pendingMindfulnessEntryNavigation = true,
        )
    }

    fun onMindfulnessEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingMindfulnessEntryNavigation = false)
    }

    fun onCycleWritePermissionResult() {
        _uiState.value = _uiState.value.copy(
            isCheckingCycleWritePermission = false,
            pendingCycleEntryNavigation = true,
        )
    }

    fun onCycleEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingCycleEntryNavigation = false)
    }

    // ── widget layout ───────────────────────────────────────────────────────

    fun toggleWidgetEdit() {
        _uiState.value = _uiState.value.copy(isEditingWidgets = !_uiState.value.isEditingWidgets)
    }

    fun removeWidget(widgetId: ManualEntryWidgetId) {
        updateWidgets(_uiState.value.widgets - widgetId)
    }

    fun addWidget(widgetId: ManualEntryWidgetId) {
        val current = _uiState.value.widgets
        if (widgetId !in current) {
            updateWidgets(current + widgetId)
        }
    }

    fun moveWidgetToTarget(widgetId: ManualEntryWidgetId, targetWidgetId: ManualEntryWidgetId) {
        val current = _uiState.value.widgets
        val fromIndex = current.indexOf(widgetId)
        val targetIndex = current.indexOf(targetWidgetId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        updateWidgets(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(targetIndex, widgetId)
            }
        )
    }

    private fun updateWidgets(widgets: List<ManualEntryWidgetId>) {
        val customizableWidgets = customizableManualEntryWidgetIds(widgets)
        preferencesRepository.setManualEntryWidgetOrder(customizableWidgets.map { it.name })
        _uiState.value = _uiState.value.copy(widgets = customizableWidgets)
    }

    /**
     * Ask Health Connect every time the write set is missing. A provider that
     * exposes no write permission at all (mindfulness on an older Health
     * Connect) has nothing to ask for and falls straight through to the form.
     */
    private fun shouldRequest(canWrite: Boolean, writePermissions: Set<String>): Boolean =
        !canWrite && writePermissions.isNotEmpty()
}
