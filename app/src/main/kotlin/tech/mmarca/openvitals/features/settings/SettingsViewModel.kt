package tech.mmarca.openvitals.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val allPermissions: Set<String> = emptySet(),
    val cyclePermissions: Set<String> = emptySet(),
    val trackCycle: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
) {
    val visiblePermissions: Set<String>
        get() = allPermissions + if (trackCycle) cyclePermissions else emptySet()

    val missingVisiblePermissions: Set<String>
        get() = visiblePermissions - grantedPermissions
}

class SettingsViewModel(
    private val repository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val avail = repository.availability()
            val granted = if (avail == HealthConnectAvailability.AVAILABLE) {
                repository.grantedPermissions()
            } else emptySet()
            Log.d(TAG, "refresh availability=$avail granted=${granted.sorted()}")

            _uiState.value = SettingsUiState(
                isLoading = false,
                availability = avail,
                grantedPermissions = granted,
                allPermissions = repository.allPermissions,
                cyclePermissions = repository.cyclePermissions,
                trackCycle = preferencesRepository.trackCycle,
                unitSystem = preferencesRepository.unitSystem,
            )
        }
    }

    fun selectUnitSystem(unitSystem: UnitSystem) {
        preferencesRepository.unitSystem = unitSystem
        _uiState.value = _uiState.value.copy(unitSystem = unitSystem)
    }

    fun setTrackCycle(enabled: Boolean) {
        preferencesRepository.trackCycle = enabled
        _uiState.value = _uiState.value.copy(trackCycle = enabled)
    }

    fun onPermissionsResult(granted: Set<String>) {
        Log.d(TAG, "onPermissionsResult callbackGranted=${granted.sorted()}")
        refresh()
    }
}
