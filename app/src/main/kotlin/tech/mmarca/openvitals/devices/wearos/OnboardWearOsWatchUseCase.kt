package tech.mmarca.openvitals.devices.wearos

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * The one platform step WearOS onboarding shows — the companion association
 * dialog. There is no bond step (a WearOS watch has no GFDI auth boundary to
 * protect) and no transport probe (no GFDI to enumerate).
 */
enum class WearOsOnboardStep { ASSOCIATING }

/**
 * WearOS onboarding always succeeds once the user has picked the watch — there
 * is nothing that can fail the way a Garmin bond can. [associated] records
 * whether the optional companion association was granted.
 */
data class WearOsOnboardOutcome(
    val device: BleSensorDevice,
    val associated: Boolean,
)

/**
 * Turns a scanned WearOS smartwatch (Galaxy, Pixel, …) into a registered
 * `(WATCH, WEAROS)` device — the sibling of `OnboardGarminWatchUseCase` built
 * on the same [WatchPairingPort] + [BleDeviceRepository] seam.
 *
 * Two steps, not the Garmin four:
 *   1. **Associate.** The companion dialog — optional in every direction; a
 *      false (declined, or unsupported) is recorded, never raised. It is the
 *      "pair like Garmin" parity, minus the security bond Garmin needs.
 *   2. **Register.** As [BleDeviceKind.WATCH] with [DeviceIntegration.WEAROS]
 *      and NO capabilities, so it is off the Garmin sync path
 *      ([BleSensorDevice.isGarminWatch]) and out of capability assignment.
 *
 * No bond and no GFDI probe: a WearOS watch speaks neither. Its live heart
 * rate comes over standard GATT and its recorded data through Health Connect.
 */
@Singleton
class OnboardWearOsWatchUseCase @Inject constructor(
    private val pairing: WatchPairingPort,
    private val bleDeviceRepository: BleDeviceRepository,
) {

    /** [onStep] fires as the companion dialog is about to be shown. */
    suspend operator fun invoke(
        device: BleDiscoveredDevice,
        displayName: String,
        onStep: ((WearOsOnboardStep) -> Unit)? = null,
    ): WearOsOnboardOutcome {
        onStep?.invoke(WearOsOnboardStep.ASSOCIATING)
        val associated = try {
            pairing.associateCompanion(device.address, displayName)
        } catch (_: Exception) {
            // The companion association is a best-effort nicety; the watch
            // onboards whether or not the platform granted (or even offered)
            // it.
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
        return WearOsOnboardOutcome(device = registered, associated = associated)
    }

    /**
     * Undoes [invoke] at the OS level — drops the companion association. The
     * registry entry is removed by the usual forget path.
     */
    suspend fun forget(address: String) {
        pairing.disassociateCompanion(address)
    }
}
