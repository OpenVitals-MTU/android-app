package tech.mmarca.openvitals.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.performance.RefreshMode
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.DashboardMetric
import tech.mmarca.openvitals.data.model.DashboardQuery
import tech.mmarca.openvitals.data.model.mergeLoaded
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showPermissionsCallout: Boolean = false,
    val trackCycle: Boolean = false,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
    val dashboardWidgets: List<DashboardWidgetId> = DefaultDashboardWidgetIds,
    val isEditingDashboard: Boolean = false,
)

class DashboardViewModel(
    private val repository: HealthRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            dashboardWidgets = dashboardWidgetIdsFromStored(prefs.dashboardWidgetOrder()),
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load(_uiState.value.selectedDate)
    }

    fun refresh() {
        load(_uiState.value.selectedDate, RefreshMode.FORCE)
    }

    fun refreshPreferences() {
        val trackCycle = prefs.trackCycle
        val sleepRangeMode = prefs.sleepRangeMode
        val current = _uiState.value
        val sleepRangeChanged = current.sleepRangeMode != sleepRangeMode
        if (current.trackCycle != trackCycle || sleepRangeChanged) {
            _uiState.value = current.copy(
                trackCycle = trackCycle,
                sleepRangeMode = sleepRangeMode,
            )
        }
        if (sleepRangeChanged) {
            load(current.selectedDate)
        }
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        loadCoordinator.launch(viewModelScope) load@{
            val trackCycle = prefs.trackCycle
            val sleepRangeMode = prefs.sleepRangeMode
            val dashboardWidgets = _uiState.value.dashboardWidgets
            val primaryMetrics = dashboardWidgets
                .take(DashboardFixedWidgetCount)
                .mapNotNull { it.toDashboardMetricOrNull() }
                .toSet()
            val remainingMetrics = dashboardWidgets
                .drop(DashboardFixedWidgetCount)
                .mapNotNull { it.toDashboardMetricOrNull() }
                .toSet()
            _uiState.value = _uiState.value.copy(
                selectedDate = clampedDate,
                isLoading = true,
                errorMessage = null,
                trackCycle = trackCycle,
                sleepRangeMode = sleepRangeMode,
            )
            runCatching {
                repository.loadDashboard(
                    DashboardQuery(
                        date = clampedDate,
                        sleepRangeMode = sleepRangeMode,
                        visibleMetrics = primaryMetrics,
                        trackCycle = trackCycle,
                        refreshMode = refreshMode,
                    )
                )
            }
                .onSuccess { data ->
                    if (!isCurrent) return@load
                    val unacknowledged = data.missingPermissions - prefs.acknowledgedPermissions()
                    _uiState.value = _uiState.value.copy(
                        data = data,
                        isLoading = remainingMetrics.isNotEmpty() && data.loadedMetrics.isNotEmpty(),
                        showPermissionsCallout = unacknowledged.isNotEmpty(),
                        trackCycle = prefs.trackCycle,
                        sleepRangeMode = sleepRangeMode,
                    )
                    loadRemainingDashboardMetrics(
                        date = clampedDate,
                        sleepRangeMode = sleepRangeMode,
                        visibleMetrics = remainingMetrics,
                        trackCycle = trackCycle,
                        refreshMode = refreshMode,
                        isCurrentLoad = { isCurrent },
                    )
                }
                .onFailure { error ->
                    if (!isCurrent) return@load
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                    )
                }
        }
    }

    fun previousDay() {
        load(_uiState.value.selectedDate.minusDays(1))
    }

    fun nextDay() {
        val today = LocalDate.now()
        val next = _uiState.value.selectedDate.plusDays(1)
        if (!next.isAfter(today)) {
            load(next)
        }
    }

    fun selectDate(date: LocalDate) {
        load(date)
    }

    fun acknowledgePermissionsCallout() {
        val missing = _uiState.value.data?.missingPermissions ?: return
        prefs.acknowledgePermissions(missing)
        _uiState.value = _uiState.value.copy(showPermissionsCallout = false)
    }

    fun toggleDashboardEdit() {
        _uiState.value = _uiState.value.copy(isEditingDashboard = !_uiState.value.isEditingDashboard)
    }

    fun removeDashboardWidget(widgetId: DashboardWidgetId) {
        updateDashboardWidgets(_uiState.value.dashboardWidgets - widgetId)
    }

    fun addDashboardWidget(widgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        if (widgetId !in current) {
            updateDashboardWidgets(current + widgetId)
        }
    }

    fun moveDashboardWidget(widgetId: DashboardWidgetId, offset: Int) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + offset).coerceIn(current.indices)
        if (fromIndex == toIndex) return

        updateDashboardWidgets(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(toIndex, widgetId)
            }
        )
    }

    fun moveDashboardWidgetToTarget(widgetId: DashboardWidgetId, targetWidgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        val targetIndex = current.indexOf(targetWidgetId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        val fromFixedSection = fromIndex < DashboardFixedWidgetCount
        val targetFixedSection = targetIndex < DashboardFixedWidgetCount
        val updated = current.toMutableList().apply {
            if (fromFixedSection == targetFixedSection) {
                removeAt(fromIndex)
                add(targetIndex, widgetId)
            } else {
                this[fromIndex] = targetWidgetId
                this[targetIndex] = widgetId
            }
        }

        updateDashboardWidgets(updated)
    }

    private fun updateDashboardWidgets(widgets: List<DashboardWidgetId>) {
        val customizableWidgets = customizableDashboardWidgetIds(widgets)
        prefs.setDashboardWidgetOrder(customizableWidgets.map { it.name })
        _uiState.value = _uiState.value.copy(dashboardWidgets = customizableWidgets)
    }

    private suspend fun loadRemainingDashboardMetrics(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
        visibleMetrics: Set<DashboardMetric>,
        trackCycle: Boolean,
        refreshMode: RefreshMode,
        isCurrentLoad: () -> Boolean,
    ) {
        val currentData = _uiState.value.data ?: return
        if (visibleMetrics.isEmpty() || currentData.loadedMetrics.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        runCatching {
            repository.loadDashboard(
                DashboardQuery(
                    date = date,
                    sleepRangeMode = sleepRangeMode,
                    visibleMetrics = visibleMetrics,
                    trackCycle = trackCycle,
                    refreshMode = refreshMode,
                )
            )
        }
            .onSuccess { remainingData ->
                if (!isCurrentLoad()) return
                val mergedData = (_uiState.value.data ?: currentData).mergeLoaded(remainingData)
                val unacknowledged = mergedData.missingPermissions - prefs.acknowledgedPermissions()
                _uiState.value = _uiState.value.copy(
                    data = mergedData,
                    isLoading = false,
                    showPermissionsCallout = unacknowledged.isNotEmpty(),
                )
            }
            .onFailure { error ->
                if (!isCurrentLoad()) return
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error",
                )
            }
    }
}
