package tech.mmarca.openvitals.devices.core.pairing

/**
 * Where an onboarding attempt got to. Ordered by progress, so a UI can render
 * it as a checklist.
 */
enum class WatchBondResult {
    /** The device was already bonded; no dialog was shown. */
    ALREADY_BONDED,

    /** The user accepted the OS pairing dialog. */
    BONDED,

    /** The user dismissed the pairing dialog, or it timed out. */
    REFUSED,

    /** The watch could not be reached to begin with. */
    UNREACHABLE,
}

/**
 * The two platform steps that turn a scanned watch into one this app is
 * allowed to talk to: an OS-level Bluetooth **bond**, and an optional
 * **companion association**.
 *
 * A **port**, for the same reason as `GarminTransportProbe`: the onboarding
 * use case is a sequence of decisions (bond, then try to associate, then
 * register), and that sequence is domain logic worth testing without a radio.
 * Behind it sit two platform layers — `BluetoothDevice.createBond` and
 * [CompanionDevicePairing] — which the domain has no business knowing apart.
 */
interface WatchPairingPort {

    /**
     * Creates an OS Bluetooth bond with [address], showing the system
     * pairing-code dialog if one is needed.
     *
     * Returns [WatchBondResult.ALREADY_BONDED] without prompting when a bond
     * exists — re-onboarding a watch the user already paired must not make
     * them confirm a pairing code again.
     */
    suspend fun bond(address: String): WatchBondResult

    /**
     * Removes the OS bond for [address]. Best-effort: a bond that is already
     * gone is not an error.
     */
    suspend fun removeBond(address: String)

    /**
     * Asks the OS to associate [address] as a companion device, showing the
     * `Allow <app> to access <device>?` dialog.
     *
     * Returns whether the association exists afterwards. **False is not a
     * failure**: the user may decline, and the platform API needs an Activity
     * and an API level that may not be there. The watch is fully usable either
     * way — the association only buys background process priority — so the
     * caller carries on regardless.
     */
    suspend fun associateCompanion(address: String, displayName: String?): Boolean

    /** Drops the companion association for [address]. Best-effort, as above. */
    suspend fun disassociateCompanion(address: String)
}
