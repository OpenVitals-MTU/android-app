package tech.mmarca.openvitals.devices.wearos

import java.time.Instant

/**
 * What the phone can tell about the OpenVitals Wear OS app on a watch.
 *
 * A bond says nothing about reach: a paired watch can be off or out of range.
 * Only an answer to the ping proves the link, so a paired watch that stays
 * silent is [NO_ANSWER], never "connected".
 */
enum class WearOsAppStatus {
    /** No bonded Wear OS watch matches the registered one. */
    NOT_PAIRED,

    /** Paired, but no answer: the watch is off or out of range, or the app on it is not running. */
    NO_ANSWER,

    /** The OpenVitals Wear OS app answered the ping. */
    APP_RUNNING,
}

/**
 * Pairing and app responsiveness of a Wear OS watch.
 */
data class WearOsCompanionStatus(
    val isPaired: Boolean = false,
    val connectedNodeName: String? = null,
    val connectedNodeAddress: String? = null,
    val appStatus: WearOsAppStatus = WearOsAppStatus.NOT_PAIRED,
    val lastCheckedAt: Instant? = null,
) {
    val isAppRunning: Boolean
        get() = appStatus == WearOsAppStatus.APP_RUNNING
}

/**
 * Port for querying a Wear OS watch's pairing and app responsiveness.
 *
 * A missing Bluetooth permission surfaces as a [SecurityException], so the
 * screen can turn it into a grant affordance.
 */
interface WearOsNodePort {
    /**
     * [targetAddress] is the bonded Classic address when onboarding found it,
     * else the BLE scan address. [targetName] is only a fallback for watches
     * registered before the Classic address was stored; with it null, only an
     * exact address matches.
     */
    suspend fun checkStatus(
        targetAddress: String? = null,
        targetName: String? = null,
    ): WearOsCompanionStatus

    /**
     * The bonded Classic address of the watch scanned as [address] and
     * advertised as [name]: the same address, else the same name. Null when
     * nothing matches, Bluetooth is off or the permission is missing.
     */
    suspend fun findBondAddress(address: String, name: String?): String?
}
