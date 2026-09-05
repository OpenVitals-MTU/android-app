package tech.mmarca.openvitals.devices.core.pairing

/** Where an onboarding attempt got to, ordered by progress. */
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
 * The two platform steps that make a scanned watch usable: an OS bond, and
 * an optional companion association. A port, so onboarding is testable
 * without a radio.
 */
interface WatchPairingPort {

    /** Creates an OS bond, showing the pairing dialog if needed. ALREADY_BONDED without prompting. */
    suspend fun bond(address: String): WatchBondResult

    /** Removes the OS bond. Best-effort. */
    suspend fun removeBond(address: String)

    /**
     * Asks the OS to associate [address] as a companion. False is not a
     * failure: the watch is fully usable without it.
     */
    suspend fun associateCompanion(address: String, displayName: String?): Boolean

    /** Drops the companion association for [address]. Best-effort, as above. */
    suspend fun disassociateCompanion(address: String)
}
