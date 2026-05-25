package tech.mmarca.openvitals.features.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.model.BodyMeasurementType
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

data class ManualEntryUiState(
    val widgets: List<ManualEntryWidgetId> = DefaultManualEntryWidgetIds,
    val isEditingWidgets: Boolean = false,
    val isCheckingHydrationWritePermission: Boolean = false,
    val hydrationWritePermissions: Set<String> = emptySet(),
    val canWriteHydration: Boolean = false,
    val showHydrationWritePermissionPrompt: Boolean = false,
    val pendingHydrationEntryNavigation: Boolean = false,
    val bodyWritePermissions: Set<String> = emptySet(),
    val isCheckingBodyWritePermission: Boolean = false,
    val showBodyWritePermissionPrompt: Boolean = false,
    val bodyWritePermissionPromptType: BodyMeasurementType? = null,
    val bodyWritePermissionRequestType: BodyMeasurementType? = null,
    val pendingBodyEntryNavigation: BodyMeasurementType? = null,
)

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val bodyRepository: BodyRepository,
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

    private fun acknowledgeBodyWritePermissionPrompt() {
        val writePermissions = _uiState.value.bodyWritePermissions
        if (writePermissions.isNotEmpty()) {
            preferencesRepository.acknowledgePermissions(writePermissions)
        }
    }
}
