package tech.mmarca.openvitals.devices.wearos

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.pairing.CompanionFilter
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/** The one platform step WearOS onboarding shows. No bond, no probe. */
enum class WearOsOnboardStep { ASSOCIATING }

/** WearOS onboarding always succeeds. [associated] records the optional association. */
data class WearOsOnboardOutcome(
    val device: BleSensorDevice,
    val associated: Boolean,
)

/**
 * Turns a scanned WearOS watch into a registered `(WATCH, WEAROS)` device.
 * Associate (optional), then register with no capabilities, off the Garmin
 * sync path, and keep the bonded Classic address for the status ping. Live
 * heart rate comes over GATT, recorded data via Health Connect.
 */
@Singleton
class OnboardWearOsWatchUseCase @Inject constructor(
    private val pairing: WatchPairingPort,
    private val bleDeviceRepository: BleDeviceRepository,
    private val nodePort: WearOsNodePort,
) {

    /** [onStep] fires as the companion dialog is about to be shown. */
    suspend operator fun invoke(
        device: BleDiscoveredDevice,
        displayName: String,
        onStep: ((WearOsOnboardStep) -> Unit)? = null,
    ): WearOsOnboardOutcome {
        onStep?.invoke(WearOsOnboardStep.ASSOCIATING)
        val associated = try {
            // The address may be the Classic bond's, found in the bonded list.
            pairing.associateCompanion(device.address, displayName, CompanionFilter.BLE_OR_CLASSIC)
        } catch (_: Exception) {
            // The association is best-effort; the watch onboards regardless.
            false
        }

        val registered = bleDeviceRepository.addDevice(
            displayName = displayName,
            address = device.address,
            bluetoothName = device.name,
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )
        // Resolved now, while the scan's name still matches the bond's.
        val classicAddress = runCatching { nodePort.findBondAddress(device.address, device.name) }.getOrNull()
        val stored = if (classicAddress != null) {
            bleDeviceRepository.setClassicAddress(registered.id, classicAddress)
            bleDeviceRepository.devices.first { it.id == registered.id }
        } else {
            registered
        }
        return WearOsOnboardOutcome(device = stored, associated = associated)
    }

    /** Undoes [invoke] at the OS level: drops the association. */
    suspend fun forget(address: String) {
        pairing.disassociateCompanion(address)
    }
}
