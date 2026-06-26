package tech.mmarca.openvitals.features.settings

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

data class BleDevicesUiState(
    val devices: List<BleSensorDevice> = emptyList(),
    val discoveredDevices: List<BleDiscoveredDevice> = emptyList(),
    val isScanning: Boolean = false,
    val showAllDevices: Boolean = false,
    val selectedDevice: BleDiscoveredDevice? = null,
    val discoveredCapabilities: Set<BleSensorCapability> = emptySet(),
    val isDiscoveringCapabilities: Boolean = false,
    val addDisplayName: String = "",
    val addCapabilities: Set<BleSensorCapability> = emptySet(),
    val addWheelCircumferenceMm: String = "",
    val capabilityConflicts: Map<BleSensorCapability, BleSensorDevice> = emptyMap(),
    val editingDeviceId: String? = null,
    val editDisplayName: String = "",
    val editCapabilities: Set<BleSensorCapability> = emptySet(),
    val editEnabled: Boolean = true,
    val editWheelCircumferenceMm: String = "",
    val errorMessage: String? = null,
    val showAddFlow: Boolean = false,
) {
    val enabledDeviceCount: Int
        get() = devices.count { it.enabled }
}

@HiltViewModel
class BleDevicesViewModel @Inject constructor(
    private val deviceRepository: BleDeviceRepository,
    private val sensorCoordinator: BleSensorCoordinator,
) : ViewModel() {
    private val localState = MutableStateFlow(BleDevicesUiState())
    val uiState: StateFlow<BleDevicesUiState> = combine(
        deviceRepository.devicesFlow,
        sensorCoordinator.discoveredDevices,
        localState,
    ) { devices, discovered, local ->
        local.copy(
            devices = devices,
            discoveredDevices = discovered,
        )
    }.stateInViewModel(initial = BleDevicesUiState())

    val requiredBluetoothPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            emptyArray()
        }

    fun refresh() {
        deviceRepository.refresh()
    }

    fun openAddFlow() {
        localState.update {
            it.copy(
                showAddFlow = true,
                selectedDevice = null,
                discoveredCapabilities = emptySet(),
                addDisplayName = "",
                addCapabilities = emptySet(),
                addWheelCircumferenceMm = BleSensorDevice.DefaultWheelCircumferenceMm.toString(),
                capabilityConflicts = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun closeAddFlow() {
        stopScan()
        localState.update {
            it.copy(
                showAddFlow = false,
                selectedDevice = null,
                discoveredCapabilities = emptySet(),
                isDiscoveringCapabilities = false,
                errorMessage = null,
            )
        }
    }

    fun setShowAllDevices(enabled: Boolean) {
        localState.update { it.copy(showAllDevices = enabled) }
        if (localState.value.isScanning) {
            startScan()
        }
    }

    fun startScan() {
        localState.update { it.copy(isScanning = true, errorMessage = null) }
        sensorCoordinator.startScan(showAllDevices = localState.value.showAllDevices)
    }

    fun stopScan() {
        sensorCoordinator.stopScan()
        localState.update { it.copy(isScanning = false) }
    }

    fun selectDiscoveredDevice(device: BleDiscoveredDevice) {
        localState.update {
            it.copy(
                selectedDevice = device,
                addDisplayName = device.name ?: device.address,
                addCapabilities = device.suggestedCapabilities,
                isDiscoveringCapabilities = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val discovered = sensorCoordinator.discoverCapabilities(device.address)
            val capabilities = if (discovered.isNotEmpty()) {
                discovered
            } else {
                device.suggestedCapabilities
            }
            val conflicts = deviceRepository.capabilityConflicts(capabilities)
            localState.update {
                it.copy(
                    discoveredCapabilities = capabilities,
                    addCapabilities = capabilities,
                    capabilityConflicts = conflicts,
                    isDiscoveringCapabilities = false,
                )
            }
        }
    }

    fun updateAddDisplayName(value: String) {
        localState.update { it.copy(addDisplayName = value) }
    }

    fun toggleAddCapability(capability: BleSensorCapability) {
        localState.update { state ->
            val next = if (capability in state.addCapabilities) {
                state.addCapabilities - capability
            } else {
                state.addCapabilities + capability
            }
            state.copy(
                addCapabilities = next,
                capabilityConflicts = deviceRepository.capabilityConflicts(next),
            )
        }
    }

    fun updateAddWheelCircumference(value: String) {
        localState.update { it.copy(addWheelCircumferenceMm = value) }
    }

    fun saveAddedDevice() {
        val state = localState.value
        val selected = state.selectedDevice ?: return
        if (state.addCapabilities.isEmpty()) {
            localState.update { it.copy(errorMessage = "Select at least one capability.") }
            return
        }
        val wheelCircumference = if (BleSensorCapability.CYCLING_SPEED_DISTANCE in state.addCapabilities) {
            state.addWheelCircumferenceMm.toIntOrNull()
                ?: BleSensorDevice.DefaultWheelCircumferenceMm
        } else {
            null
        }
        deviceRepository.addDevice(
            displayName = state.addDisplayName,
            address = selected.address,
            bluetoothName = selected.name,
            capabilities = state.addCapabilities,
            wheelCircumferenceMm = wheelCircumference,
        )
        stopScan()
        closeAddFlow()
    }

    fun openEditDevice(deviceId: String) {
        val device = deviceRepository.devices.firstOrNull { it.id == deviceId } ?: return
        localState.update {
            it.copy(
                editingDeviceId = device.id,
                editDisplayName = device.displayName,
                editCapabilities = device.capabilities,
                editEnabled = device.enabled,
                editWheelCircumferenceMm = device.wheelCircumferenceMm?.toString()
                    ?: BleSensorDevice.DefaultWheelCircumferenceMm.toString(),
                capabilityConflicts = deviceRepository.capabilityConflicts(device.capabilities, device.id),
                errorMessage = null,
            )
        }
    }

    fun closeEditDevice() {
        localState.update {
            it.copy(
                editingDeviceId = null,
                capabilityConflicts = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun updateEditDisplayName(value: String) {
        localState.update { it.copy(editDisplayName = value) }
    }

    fun toggleEditCapability(capability: BleSensorCapability) {
        localState.update { state ->
            val next = if (capability in state.editCapabilities) {
                state.editCapabilities - capability
            } else {
                state.editCapabilities + capability
            }
            state.copy(
                editCapabilities = next,
                capabilityConflicts = deviceRepository.capabilityConflicts(
                    next,
                    state.editingDeviceId,
                ),
            )
        }
    }

    fun setEditEnabled(enabled: Boolean) {
        localState.update { it.copy(editEnabled = enabled) }
    }

    fun updateEditWheelCircumference(value: String) {
        localState.update { it.copy(editWheelCircumferenceMm = value) }
    }

    fun saveEditedDevice() {
        val state = localState.value
        val deviceId = state.editingDeviceId ?: return
        if (state.editCapabilities.isEmpty()) {
            localState.update { it.copy(errorMessage = "Select at least one capability.") }
            return
        }
        val wheelCircumference = if (BleSensorCapability.CYCLING_SPEED_DISTANCE in state.editCapabilities) {
            state.editWheelCircumferenceMm.toIntOrNull()
                ?: BleSensorDevice.DefaultWheelCircumferenceMm
        } else {
            null
        }
        deviceRepository.updateDevice(
            deviceId = deviceId,
            displayName = state.editDisplayName,
            capabilities = state.editCapabilities,
            enabled = state.editEnabled,
            wheelCircumferenceMm = wheelCircumference,
        )
        closeEditDevice()
    }

    fun removeDevice(deviceId: String) {
        deviceRepository.removeDevice(deviceId)
        if (localState.value.editingDeviceId == deviceId) {
            closeEditDevice()
        }
    }

    fun setDeviceEnabled(deviceId: String, enabled: Boolean) {
        deviceRepository.setDeviceEnabled(deviceId, enabled)
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInViewModel(initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch {
            collect { value -> state.value = value }
        }
        return state.asStateFlow()
    }
}
