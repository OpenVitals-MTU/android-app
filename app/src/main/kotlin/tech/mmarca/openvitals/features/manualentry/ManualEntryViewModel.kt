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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.repository.PreferencesRepository

/**
 * The log grid. A tile only opens its entry screen: Health Connect can
 * stop asking after one decline, so the grid never spends that dialog.
 */
@Immutable
data class ManualEntryUiState(
    val widgets: List<ManualEntryWidgetId> = DefaultManualEntryWidgetIds,
    val isEditingWidgets: Boolean = false,
    val pendingHydrationEntryNavigation: Boolean = false,
    val pendingCarbsEntryNavigation: Boolean = false,
    val pendingActivityEntryNavigation: Boolean = false,
    val pendingBodyEntryNavigation: BodyMeasurementType? = null,
    val pendingVitalsEntryNavigation: VitalsMeasurementType? = null,
    val pendingMindfulnessEntryNavigation: Boolean = false,
    val pendingCycleEntryNavigation: Boolean = false,
)

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ManualEntryUiState(
            widgets = manualEntryWidgetIdsFromStored(preferencesRepository.manualEntryWidgetOrder()),
        )
    )
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    // Tiles: each opens its entry screen.

    fun onHydrationWidgetTapped() {
        _uiState.value = _uiState.value.copy(pendingHydrationEntryNavigation = true)
    }

    fun onHydrationEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingHydrationEntryNavigation = false)
    }

    fun onCarbsWidgetTapped() {
        _uiState.value = _uiState.value.copy(pendingCarbsEntryNavigation = true)
    }

    fun onCarbsEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingCarbsEntryNavigation = false)
    }

    fun onActivityWidgetTapped() {
        _uiState.value = _uiState.value.copy(pendingActivityEntryNavigation = true)
    }

    fun onActivityEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingActivityEntryNavigation = false)
    }

    fun onBodyMeasurementWidgetTapped(type: BodyMeasurementType) {
        _uiState.value = _uiState.value.copy(pendingBodyEntryNavigation = type)
    }

    fun onBodyEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingBodyEntryNavigation = null)
    }

    fun onVitalsMeasurementWidgetTapped(type: VitalsMeasurementType) {
        _uiState.value = _uiState.value.copy(pendingVitalsEntryNavigation = type)
    }

    fun onVitalsEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingVitalsEntryNavigation = null)
    }

    fun onMindfulnessWidgetTapped() {
        _uiState.value = _uiState.value.copy(pendingMindfulnessEntryNavigation = true)
    }

    fun onMindfulnessEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingMindfulnessEntryNavigation = false)
    }

    fun onCycleWidgetTapped() {
        _uiState.value = _uiState.value.copy(pendingCycleEntryNavigation = true)
    }

    fun onCycleEntryNavigationHandled() {
        _uiState.value = _uiState.value.copy(pendingCycleEntryNavigation = false)
    }

    // Widget layout.

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
}
