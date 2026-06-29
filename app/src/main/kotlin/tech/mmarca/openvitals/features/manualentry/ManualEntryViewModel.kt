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
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.VitalsRepository

@Immutable
data class ManualEntryUiState(
    val widgets: List<ManualEntryWidgetId> = DefaultManualEntryWidgetIds,
    val isEditingWidgets: Boolean = false,
    val isCheckingHydrationWritePermission: Boolean = false,
    val hydrationWritePermissions: Set<String> = emptySet(),
    val canWriteHydration: Boolean = false,
    val showHydrationWritePermissionPrompt: Boolean = false,
    val pendingHydrationEntryNavigation: Boolean = false,
    val nutritionWritePermissions: Set<String> = emptySet(),
    val isCheckingNutritionWritePermission: Boolean = false,
    val canWriteNutrition: Boolean = false,
    val showNutritionWritePermissionPrompt: Boolean = false,
    val pendingCarbsEntryNavigation: Boolean = false,
    val activityWritePermissions: Set<String> = emptySet(),
    val isCheckingActivityWritePermission: Boolean = false,
    val showActivityWritePermissionPrompt: Boolean = false,
    val pendingActivityEntryNavigation: Boolean = false,
    val bodyWritePermissions: Set<String> = emptySet(),
    val isCheckingBodyWritePermission: Boolean = false,
    val showBodyWritePermissionPrompt: Boolean = false,
    val bodyWritePermissionPromptType: BodyMeasurementType? = null,
    val bodyWritePermissionRequestType: BodyMeasurementType? = null,
    val pendingBodyEntryNavigation: BodyMeasurementType? = null,
    val vitalsWritePermissions: Set<String> = emptySet(),
    val isCheckingVitalsWritePermission: Boolean = false,
    val showVitalsWritePermissionPrompt: Boolean = false,
    val vitalsWritePermissionPromptType: VitalsMeasurementType? = null,
    val vitalsWritePermissionRequestType: VitalsMeasurementType? = null,
    val pendingVitalsEntryNavigation: VitalsMeasurementType? = null,
    val mindfulnessWritePermissions: Set<String> = emptySet(),
    val isCheckingMindfulnessWritePermission: Boolean = false,
    val showMindfulnessWritePermissionPrompt: Boolean = false,
    val pendingMindfulnessEntryNavigation: Boolean = false,
)

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository,
    private val bodyRepository: BodyRepository,
    private val vitalsRepository: VitalsRepository,
    private val mindfulnessRepository: MindfulnessRepository,
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
                showHydrationWritePermissionPrompt = false,
                pendingHydrationEntryNavigation = false,
            )
            runCatching {
                hydrationRepository.hasHydrationWritePermission()
            }.onSuccess { canWriteHydration ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWriteHydration && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingHydrationWritePermission = false,
                    canWriteHydration = canWriteHydration,
                    showHydrationWritePermissionPrompt = shouldShowPrompt,
                    pendingHydrationEntryNavigation = !shouldShowPrompt,
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
                showNutritionWritePermissionPrompt = false,
                pendingCarbsEntryNavigation = false,
            )
            runCatching {
                nutritionRepository.hasNutritionWritePermission()
            }.onSuccess { canWriteNutrition ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWriteNutrition && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingNutritionWritePermission = false,
                    canWriteNutrition = canWriteNutrition,
                    showNutritionWritePermissionPrompt = shouldShowPrompt,
                    pendingCarbsEntryNavigation = !shouldShowPrompt,
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
                showActivityWritePermissionPrompt = false,
                pendingActivityEntryNavigation = false,
            )
            runCatching {
                activityRepository.hasActivityWritePermission()
            }.onSuccess { canWriteActivity ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWriteActivity && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingActivityWritePermission = false,
                    showActivityWritePermissionPrompt = shouldShowPrompt,
                    pendingActivityEntryNavigation = !shouldShowPrompt,
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
                showBodyWritePermissionPrompt = false,
                bodyWritePermissionPromptType = null,
                pendingBodyEntryNavigation = null,
            )
            runCatching {
                bodyRepository.hasBodyWritePermission(type)
            }.onSuccess { canWrite ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWrite && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingBodyWritePermission = false,
                    showBodyWritePermissionPrompt = shouldShowPrompt,
                    bodyWritePermissionPromptType = if (shouldShowPrompt) type else null,
                    pendingBodyEntryNavigation = if (shouldShowPrompt) null else type,
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
                showVitalsWritePermissionPrompt = false,
                vitalsWritePermissionPromptType = null,
                pendingVitalsEntryNavigation = null,
            )
            runCatching {
                vitalsRepository.hasVitalsWritePermission(type)
            }.onSuccess { canWrite ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWrite && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingVitalsWritePermission = false,
                    showVitalsWritePermissionPrompt = shouldShowPrompt,
                    vitalsWritePermissionPromptType = if (shouldShowPrompt) type else null,
                    pendingVitalsEntryNavigation = if (shouldShowPrompt) null else type,
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
                showMindfulnessWritePermissionPrompt = false,
                pendingMindfulnessEntryNavigation = false,
            )
            runCatching {
                mindfulnessRepository.hasMindfulnessWritePermission()
            }.onSuccess { canWrite ->
                val unacknowledgedWritePermissions = writePermissions - preferencesRepository.acknowledgedPermissions()
                val shouldShowPrompt = !canWrite && unacknowledgedWritePermissions.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isCheckingMindfulnessWritePermission = false,
                    showMindfulnessWritePermissionPrompt = shouldShowPrompt,
                    pendingMindfulnessEntryNavigation = !shouldShowPrompt,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isCheckingMindfulnessWritePermission = false,
                    pendingMindfulnessEntryNavigation = true,
                )
            }
        }
    }

    fun continueHydrationEntryFromWritePermissionPrompt() {
        acknowledgeHydrationWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showHydrationWritePermissionPrompt = false,
            pendingHydrationEntryNavigation = true,
        )
    }

    fun dismissHydrationWritePermissionPrompt() {
        acknowledgeHydrationWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showHydrationWritePermissionPrompt = false)
    }

    fun grantHydrationWritePermissionFromPrompt() {
        acknowledgeHydrationWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showHydrationWritePermissionPrompt = false)
    }

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

    fun continueCarbsEntryFromWritePermissionPrompt() {
        acknowledgeNutritionWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showNutritionWritePermissionPrompt = false,
            pendingCarbsEntryNavigation = true,
        )
    }

    fun dismissNutritionWritePermissionPrompt() {
        acknowledgeNutritionWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showNutritionWritePermissionPrompt = false)
    }

    fun grantNutritionWritePermissionFromPrompt() {
        acknowledgeNutritionWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showNutritionWritePermissionPrompt = false)
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

    fun continueActivityEntryFromWritePermissionPrompt() {
        acknowledgeActivityWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showActivityWritePermissionPrompt = false,
            pendingActivityEntryNavigation = true,
        )
    }

    fun dismissActivityWritePermissionPrompt() {
        acknowledgeActivityWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showActivityWritePermissionPrompt = false)
    }

    fun grantActivityWritePermissionFromPrompt() {
        acknowledgeActivityWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showActivityWritePermissionPrompt = false)
    }

    fun onActivityWritePermissionResult() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingActivityWritePermission = false,
                pendingActivityEntryNavigation = true,
            )
        }
    }

    fun onActivityEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingActivityEntryNavigation = false)
    }

    fun continueBodyEntryFromWritePermissionPrompt() {
        val type = _uiState.value.bodyWritePermissionPromptType ?: return
        acknowledgeBodyWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showBodyWritePermissionPrompt = false,
            bodyWritePermissionPromptType = null,
            pendingBodyEntryNavigation = type,
        )
    }

    fun dismissBodyWritePermissionPrompt() {
        acknowledgeBodyWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showBodyWritePermissionPrompt = false,
            bodyWritePermissionPromptType = null,
        )
    }

    fun grantBodyWritePermissionFromPrompt() {
        val type = _uiState.value.bodyWritePermissionPromptType ?: return
        acknowledgeBodyWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showBodyWritePermissionPrompt = false,
            bodyWritePermissionPromptType = null,
            bodyWritePermissionRequestType = type,
        )
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

    fun continueVitalsEntryFromWritePermissionPrompt() {
        val type = _uiState.value.vitalsWritePermissionPromptType ?: return
        acknowledgeVitalsWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showVitalsWritePermissionPrompt = false,
            vitalsWritePermissionPromptType = null,
            pendingVitalsEntryNavigation = type,
        )
    }

    fun dismissVitalsWritePermissionPrompt() {
        acknowledgeVitalsWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showVitalsWritePermissionPrompt = false,
            vitalsWritePermissionPromptType = null,
        )
    }

    fun grantVitalsWritePermissionFromPrompt() {
        val type = _uiState.value.vitalsWritePermissionPromptType ?: return
        acknowledgeVitalsWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showVitalsWritePermissionPrompt = false,
            vitalsWritePermissionPromptType = null,
            vitalsWritePermissionRequestType = type,
        )
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

    fun continueMindfulnessEntryFromWritePermissionPrompt() {
        acknowledgeMindfulnessWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(
            showMindfulnessWritePermissionPrompt = false,
            pendingMindfulnessEntryNavigation = true,
        )
    }

    fun dismissMindfulnessWritePermissionPrompt() {
        acknowledgeMindfulnessWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showMindfulnessWritePermissionPrompt = false)
    }

    fun grantMindfulnessWritePermissionFromPrompt() {
        acknowledgeMindfulnessWritePermissionPrompt()
        _uiState.value = _uiState.value.copy(showMindfulnessWritePermissionPrompt = false)
    }

    fun onMindfulnessWritePermissionResult() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingMindfulnessWritePermission = false,
                pendingMindfulnessEntryNavigation = true,
            )
        }
    }

    fun onMindfulnessEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingMindfulnessEntryNavigation = false)
    }

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

    private fun acknowledgeHydrationWritePermissionPrompt() {
        val writePermissions = _uiState.value.hydrationWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }

    private fun acknowledgeNutritionWritePermissionPrompt() {
        val writePermissions = _uiState.value.nutritionWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }

    private fun acknowledgeActivityWritePermissionPrompt() {
        val writePermissions = _uiState.value.activityWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }

    private fun acknowledgeBodyWritePermissionPrompt() {
        val writePermissions = _uiState.value.bodyWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }

    private fun acknowledgeVitalsWritePermissionPrompt() {
        val writePermissions = _uiState.value.vitalsWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }

    private fun acknowledgeMindfulnessWritePermissionPrompt() {
        val writePermissions = _uiState.value.mindfulnessWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }
}
