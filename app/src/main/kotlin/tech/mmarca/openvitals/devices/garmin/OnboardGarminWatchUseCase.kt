package tech.mmarca.openvitals.devices.garmin

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/** Which platform step onboarding is on, so the sheet can announce the OS dialog. */
enum class GarminOnboardStep { BONDING, ASSOCIATING, PROBING }

/** How onboarding ended. */
sealed class GarminOnboardOutcome {

    /** Bonded and registered. [associated] is worth surfacing, never a failure. */
    data class Succeeded(
        val device: BleSensorDevice,
        val associated: Boolean,
        /** Which GFDI transport the watch speaks. Recorded, never enforced. */
        val transport: GarminGattReport,
    ) : GarminOnboardOutcome()

    /** Onboarding stopped at [step]. Nothing was written to the registry. */
    data class Failed(
        val step: GarminOnboardStep,
        val reason: WatchBondResult,
    ) : GarminOnboardOutcome()
}

/**
 * Turns a scanned Garmin watch into a registered device. Bond first: the
 * bond is the security boundary, GFDI has no auth. Then associate
 * (optional), probe the GATT services (after the bond, non-fatal), and only
 * then register, so a refused pairing leaves nothing in the list.
 * Registered with no capabilities; a bike computer gets its live ones later.
 */
@Singleton
class OnboardGarminWatchUseCase @Inject constructor(
    private val pairing: WatchPairingPort,
    private val bleDeviceRepository: BleDeviceRepository,
    private val transportProbe: GarminTransportProbe,
    private val stateStore: GarminDeviceStateStore? = null,
) {

    init {
        // Idempotent, and a no-op outside debug builds.
        GarminLog.installLogcatSink()
    }

    /** [onStep] fires before each platform dialog. [kind] is WATCH or BIKE_COMPUTER. */
    suspend operator fun invoke(
        device: BleDiscoveredDevice,
        displayName: String,
        kind: BleDeviceKind = BleDeviceKind.WATCH,
        onStep: ((GarminOnboardStep) -> Unit)? = null,
    ): GarminOnboardOutcome {
        onStep?.invoke(GarminOnboardStep.BONDING)
        when (val bond = pairing.bond(device.address)) {
            WatchBondResult.REFUSED,
            WatchBondResult.UNREACHABLE,
            -> return GarminOnboardOutcome.Failed(
                step = GarminOnboardStep.BONDING,
                reason = bond,
            )

            WatchBondResult.BONDED,
            WatchBondResult.ALREADY_BONDED,
            -> Unit
        }

        onStep?.invoke(GarminOnboardStep.ASSOCIATING)
        val associated = pairing.associateCompanion(device.address, displayName)

        onStep?.invoke(GarminOnboardStep.PROBING)
        val transport = transportProbe.probe(device.address)

        val registered = bleDeviceRepository.addDevice(
            displayName = displayName,
            address = device.address,
            bluetoothName = device.name,
            capabilities = emptySet(),
            kind = kind,
            integration = DeviceIntegration.GARMIN,
        )
        // A just-paired watch may be waiting on its setup wizard.
        stateStore?.setSetupWizardPending(registered.id, true)
        return GarminOnboardOutcome.Succeeded(
            device = registered,
            associated = associated,
            transport = transport,
        )
    }

    /** Undoes [invoke] at the OS level: drops the bond and association. */
    suspend fun forget(address: String) {
        pairing.disassociateCompanion(address)
        pairing.removeBond(address)
    }
}
