package tech.mmarca.openvitals.features.watches

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Immutable
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
import tech.mmarca.openvitals.core.permissions.OsPermissionsService
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.garmin.GarminOnboardOutcome
import tech.mmarca.openvitals.devices.garmin.GarminOnboardStep
import tech.mmarca.openvitals.devices.garmin.GarminTransportVariant
import tech.mmarca.openvitals.devices.garmin.OnboardGarminWatchUseCase
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.wearos.OnboardWearOsWatchUseCase
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator

/**
 * What the last onboarding wants the list screen to say, after the sheet
 * closes. An unsupported transport outranks a missing association: a watch
 * that cannot sync at all makes the background-reliability note moot.
 */
enum class WatchOnboardNotice { UNSUPPORTED_TRANSPORT, NO_COMPANION }

@Immutable
data class WatchesUiState(
    /** The paired watches — never sensors, never bike computers' sensor role. */
    val watches: List<BleSensorDevice> = emptyList(),
    /**
     * Scanned devices that classify as a watch or bike computer. The Sensors
     * screen's scan shows everything; this one shows only what this screen
     * can onboard.
     */
    val discoveredWatches: List<BleDiscoveredDevice> = emptyList(),
    val isScanning: Boolean = false,
    val showAddFlow: Boolean = false,
    val selectedDevice: BleDiscoveredDevice? = null,
    val selectedClassification: DeviceClassification? = null,
    val addDisplayName: String = "",
    val isOnboarding: Boolean = false,
    val onboardStep: GarminOnboardStep? = null,
    val onboardNotice: WatchOnboardNotice? = null,
    val errorMessage: String? = null,
    /** Android's own permissions for watch pairing and background sync. */
    val osPermissions: OsPermissionCatalog = OsPermissionCatalog(),
    /**
     * Shown instead of the scan when the user goes to add a watch with
     * permissions still outstanding — in practice, the first watch. Scanning
     * without Bluetooth finds nothing, and pairing without the background
     * grants sets up a watch that quietly stops syncing later.
     */
    val showPermissionsGate: Boolean = false,
    /**
     * Settings-screen walks still queued. They cannot go in one dialog — each
     * is its own screen — so they drain one per resume, and the queue is what
     * survives leaving the app in between.
     */
    val pendingSpecialPermissions: List<OsPermissionId> = emptyList(),
) {
    val addingIntegration: DeviceIntegration?
        get() = selectedClassification?.integration
}

/**
 * The Watches settings screen's state: the paired-watches list plus the scan →
 * classify → onboard add flow.
 *
 * Deliberately its own view-model rather than a mode on [tech.mmarca.openvitals
 * .features.settings.BleDevicesViewModel]: sensors and watches share the radio,
 * the registry and the add flow, and nothing else — the Flutter build tried a
 * shared screen and rejected it. Port of the watch half of
 * `ble_devices_view_model.dart` / `ble_devices_screen.dart (kind: watch)`.
 */
@HiltViewModel
class WatchesViewModel @Inject constructor(
    private val deviceRepository: BleDeviceRepository,
    private val sensorCoordinator: BleSensorCoordinator,
    private val onboardGarminWatch: OnboardGarminWatchUseCase,
    private val onboardWearOsWatch: OnboardWearOsWatchUseCase,
    private val osPermissionsService: OsPermissionsService,
) : ViewModel() {

    private val localState = MutableStateFlow(
        WatchesUiState(osPermissions = osPermissionsService.watchSetupCatalog()),
    )

    val uiState: StateFlow<WatchesUiState> = combine(
        deviceRepository.devicesFlow,
        sensorCoordinator.discoveredDevices,
        localState,
    ) { devices, discovered, local ->
        local.copy(
            watches = devices.filter { it.isWatch },
            discoveredWatches = discovered.filter { device ->
                sensorCoordinator.classifyDiscoveredDevice(device).kind != BleDeviceKind.SENSOR
            },
        )
    }.stateInViewModel(initial = WatchesUiState())

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
        refreshOsPermissions()
    }

    fun refreshOsPermissions() {
        localState.update { it.copy(osPermissions = osPermissionsService.watchSetupCatalog()) }
    }

    /**
     * The entry point for the add button. Outstanding permissions get the
     * checklist first — asked here, where the reason for each is on screen,
     * rather than during first-run onboarding where a watch is hypothetical.
     */
    fun startAdd() {
        val catalog = osPermissionsService.watchSetupCatalog()
        localState.update { it.copy(osPermissions = catalog) }
        if (catalog.allGranted) {
            openAddFlow()
        } else {
            localState.update { it.copy(showPermissionsGate = true, errorMessage = null) }
        }
    }

    fun dismissPermissionsGate() {
        localState.update {
            it.copy(showPermissionsGate = false, pendingSpecialPermissions = emptyList())
        }
    }

    /** Queues every outstanding settings walk, for the gate's "Grant all". */
    fun queueAllSpecialPermissions() {
        localState.update { it.copy(pendingSpecialPermissions = it.osPermissions.outstandingSpecials) }
    }

    fun queueSpecialPermission(id: OsPermissionId) {
        localState.update { it.copy(pendingSpecialPermissions = listOf(id)) }
    }

    /**
     * Opens the next queued settings screen, one per call — the app leaves the
     * foreground to show it, so a second would land behind the first and be
     * missed. Called on every resume, which turns the queue into a walkthrough:
     * grant, come back, get handed the next one.
     *
     * Re-reads the catalog first, so a walk the user just finished is dropped
     * rather than reopened.
     */
    fun openNextSpecialPermission() {
        val queued = localState.value.pendingSpecialPermissions
        if (queued.isEmpty()) return
        val fresh = osPermissionsService.watchSetupCatalog()
        val remaining = queued.filter { id -> fresh.rows.any { it.id == id && !it.granted } }
        localState.update {
            it.copy(osPermissions = fresh, pendingSpecialPermissions = remaining.drop(1))
        }
        val next = remaining.firstOrNull() ?: return
        osPermissionsService.openSettingsFor(next)
    }

    fun openAddFlow() {
        localState.update {
            it.copy(
                showPermissionsGate = false,
                showAddFlow = true,
                selectedDevice = null,
                selectedClassification = null,
                addDisplayName = "",
                errorMessage = null,
                // Describes the LAST onboarding's outcome — cleared when a
                // new flow starts, not when the old one closes.
                onboardNotice = null,
            )
        }
        startScan()
    }

    fun closeAddFlow() {
        stopScan()
        localState.update {
            it.copy(
                showAddFlow = false,
                selectedDevice = null,
                selectedClassification = null,
                errorMessage = null,
            )
        }
    }

    fun dismissNotice() {
        localState.update { it.copy(onboardNotice = null) }
    }

    fun startScan() {
        localState.update { it.copy(isScanning = true, errorMessage = null) }
        sensorCoordinator.startScan(showAllDevices = false)
    }

    fun stopScan() {
        sensorCoordinator.stopScan()
        localState.update { it.copy(isScanning = false) }
    }

    /**
     * A GFDI device answers a different question than a sensor: the sheet
     * asks the user to bond it, not to pick capabilities. The scan competes
     * with the connect that pairing needs, so choosing stops it.
     */
    fun selectDiscoveredDevice(device: BleDiscoveredDevice) {
        val classification = sensorCoordinator.classifyDiscoveredDevice(device)
        localState.update {
            it.copy(
                selectedDevice = device,
                selectedClassification = classification,
                addDisplayName = device.name ?: device.address,
                errorMessage = null,
            )
        }
        stopScan()
    }

    fun updateAddDisplayName(value: String) {
        localState.update { it.copy(addDisplayName = value) }
    }

    /**
     * Bonds and registers the selected watch through whichever integration
     * claimed it. [onDone] fires with `true` when the sheet should close; a
     * refused pairing leaves it open so the user can retry without
     * re-scanning.
     */
    fun onboardSelectedWatch(onDone: (Boolean) -> Unit = {}) {
        val state = localState.value
        val selected = state.selectedDevice
        if (selected == null || state.isOnboarding) {
            onDone(false)
            return
        }
        val displayName = state.addDisplayName.trim()
            .ifBlank { selected.name ?: selected.address }

        // A WearOS watch takes a different, shorter path: no bond, no GFDI
        // probe — just the optional companion association, then register.
        if (state.addingIntegration == DeviceIntegration.WEAROS) {
            localState.update {
                it.copy(isOnboarding = true, errorMessage = null, onboardNotice = null)
            }
            viewModelScope.launch {
                val outcome = onboardWearOsWatch(selected, displayName = displayName)
                localState.update {
                    it.copy(
                        isOnboarding = false,
                        onboardStep = null,
                        onboardNotice = if (outcome.associated) {
                            null
                        } else {
                            WatchOnboardNotice.NO_COMPANION
                        },
                    )
                }
                closeAddFlow()
                onDone(true)
            }
            return
        }

        localState.update {
            it.copy(
                isOnboarding = true,
                onboardStep = GarminOnboardStep.BONDING,
                errorMessage = null,
                onboardNotice = null,
            )
        }
        viewModelScope.launch {
            val outcome = onboardGarminWatch(
                selected,
                displayName = displayName,
                // Register as the classified GFDI kind — watch, or
                // BIKE_COMPUTER for an Edge.
                kind = state.selectedClassification?.kind ?: BleDeviceKind.WATCH,
                onStep = { step ->
                    localState.update { current ->
                        if (current.isOnboarding) current.copy(onboardStep = step) else current
                    }
                },
            )
            when (outcome) {
                is GarminOnboardOutcome.Failed -> {
                    localState.update {
                        it.copy(
                            isOnboarding = false,
                            onboardStep = null,
                            errorMessage = when (outcome.reason) {
                                WatchBondResult.UNREACHABLE ->
                                    "Could not reach the watch. Wake it up and keep it close, then try again."

                                else ->
                                    "Pairing was not completed. Confirm the code on the watch to finish."
                            },
                        )
                    }
                    onDone(false)
                }

                is GarminOnboardOutcome.Succeeded -> {
                    localState.update {
                        it.copy(
                            isOnboarding = false,
                            onboardStep = null,
                            onboardNotice = when {
                                // An unsupported transport outranks a missing
                                // association: a watch that cannot sync at
                                // all makes the reliability note moot.
                                outcome.transport.variant == GarminTransportVariant.UNKNOWN ||
                                    outcome.transport.variant == GarminTransportVariant.UNREACHABLE ->
                                    WatchOnboardNotice.UNSUPPORTED_TRANSPORT

                                !outcome.associated -> WatchOnboardNotice.NO_COMPANION
                                else -> null
                            },
                        )
                    }
                    closeAddFlow()
                    onDone(true)
                }
            }
        }
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
