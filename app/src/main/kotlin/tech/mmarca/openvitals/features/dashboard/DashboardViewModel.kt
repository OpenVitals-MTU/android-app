package tech.mmarca.openvitals.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showPermissionsCallout: Boolean = false,
    val trackCycle: Boolean = false,
    val sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
)

class DashboardViewModel(
    private val repository: HealthRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load(_uiState.value.selectedDate)
    }

    fun refresh() {
        load(_uiState.value.selectedDate)
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

    fun load(date: LocalDate) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        viewModelScope.launch {
            val trackCycle = prefs.trackCycle
            val sleepRangeMode = prefs.sleepRangeMode
            _uiState.value = _uiState.value.copy(
                selectedDate = clampedDate,
                isLoading = true,
                errorMessage = null,
                trackCycle = trackCycle,
                sleepRangeMode = sleepRangeMode,
            )
            runCatching { repository.loadDashboard(clampedDate, sleepRangeMode) }
                .onSuccess { data ->
                    val unacknowledged = data.missingPermissions - prefs.acknowledgedPermissions()
                    _uiState.value = _uiState.value.copy(
                        data = data,
                        isLoading = false,
                        showPermissionsCallout = unacknowledged.isNotEmpty(),
                        trackCycle = prefs.trackCycle,
                        sleepRangeMode = sleepRangeMode,
                    )
                }
                .onFailure { error ->
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
}
