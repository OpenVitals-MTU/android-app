package tech.mmarca.openvitals.features.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
)

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
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
}
