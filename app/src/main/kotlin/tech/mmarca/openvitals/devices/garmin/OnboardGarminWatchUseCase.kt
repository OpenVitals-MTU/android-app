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

/**
 * Which platform step the onboarding is on, so the sheet can tell the user
 * which OS dialog is about to appear over it. The first two show a system
 * dialog the app does not own, and an unexplained one reads as the app
 * misbehaving; [PROBING] shows nothing but takes a few seconds over the air.
 */
enum class GarminOnboardStep { BONDING, ASSOCIATING, PROBING }

/** How onboarding ended. */
sealed class GarminOnboardOutcome {

    /**
     * The watch is bonded and in the registry. [associated] records whether
     * the companion association was also granted — worth surfacing, because
     * without it a long sync is likelier to be killed in the background, but
     * never a reason to fail the onboarding.
     */
    data class Succeeded(
        val device: BleSensorDevice,
        val associated: Boolean,
        /**
         * Which GFDI transport the watch turned out to speak. Recorded, never
         * enforced: an [GarminTransportVariant.UNKNOWN] watch still onboards,
         * because the user's watch being unsupported is a thing to TELL them,
         * not a reason to refuse a pairing they just confirmed on the device.
         */
        val transport: GarminGattReport,
    ) : GarminOnboardOutcome()

    /** Onboarding stopped at [step]. Nothing was written to the registry. */
    data class Failed(
        val step: GarminOnboardStep,
        val reason: WatchBondResult,
    ) : GarminOnboardOutcome()
}

/**
 * Turns a scanned Garmin watch into a registered device.
 *
 * Four steps, in this order and no other:
 *
 *   1. **Bond.** The OS pairing dialog. Mandatory — GFDI carries no
 *      authentication of its own (Gadgetbridge's `AuthNegotiationMessage`
 *      answers every challenge with zeroes), so the Bluetooth bond IS the
 *      security boundary for the watch's health data. No bond, no onboarding.
 *   2. **Associate.** The companion dialog. Optional in every direction: the
 *      user may decline it, and the platform may not offer it at all. A false
 *      here is recorded, not raised.
 *   3. **Probe.** Enumerate the GATT services to learn which GFDI transport
 *      this watch speaks. Must come after the bond — the characteristics sit
 *      behind an encrypted link. Also non-fatal: the answer is recorded.
 *   4. **Register.** Only now, so a refused pairing cannot leave a watch in
 *      the list that the app can never actually reach.
 *
 * Registered with NO capabilities, as [BleDeviceKind.WATCH] or (for a Garmin
 * Edge) [BleDeviceKind.BIKE_COMPUTER] per [invoke]'s `kind`, under
 * [DeviceIntegration.GARMIN]. A watch streams nothing live. A bike computer
 * CAN, but its live standard-BLE capabilities are added later from its device
 * card — broadcast mode is usually only on during a ride — so it too starts
 * capability-less and out of the recording coordinator's assignment until the
 * user opts it in.
 */
@Singleton
class OnboardGarminWatchUseCase @Inject constructor(
    private val pairing: WatchPairingPort,
    private val bleDeviceRepository: BleDeviceRepository,
    private val transportProbe: GarminTransportProbe,
    private val stateStore: GarminDeviceStateStore? = null,
) {

    init {
        // Protocol logging for everything the onboarding + sync stack does.
        // Idempotent, and a no-op outside debug builds.
        GarminLog.installLogcatSink()
    }

    /**
     * [onStep] fires as each platform dialog is about to be shown. [kind] is
     * the classified GFDI kind — [BleDeviceKind.WATCH] (default) or, for a
     * Garmin Edge, [BleDeviceKind.BIKE_COMPUTER].
     */
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
        // A just-paired watch may be sitting on its setup wizard waiting for
        // the phone to declare the pairing done; the first session answers.
        stateStore?.setSetupWizardPending(registered.id, true)
        return GarminOnboardOutcome.Succeeded(
            device = registered,
            associated = associated,
            transport = transport,
        )
    }

    /**
     * Undoes [invoke] at the OS level. The registry entry is removed by the
     * forget-device flow as usual; this drops the bond and association that
     * would otherwise outlive it, leaving the watch paired to an app that no
     * longer knows about it.
     */
    suspend fun forget(address: String) {
        pairing.disassociateCompanion(address)
        pairing.removeBond(address)
    }
}
