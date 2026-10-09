package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * What the phone can tell about the OpenVitals Wear OS app on a watch.
 *
 * A bond says nothing about reach: a paired watch can be off or out of range.
 * Only an answer to the ping proves the link, so a paired watch that stays
 * silent is [NO_ANSWER], never "connected".
 */
enum class WearOsAppStatus {
    /** The phone's Bluetooth is off. */
    BLUETOOTH_OFF,

    /** No bonded Wear OS watch matches the registered one. */
    NOT_PAIRED,

    /** Paired, but no answer: the watch is off or out of range, or the app on it is not installed or not running. */
    NO_ANSWER,

    /** The watch is asking the wearer whether to allow this phone. */
    PENDING_CONFIRMATION,

    /** The watch holds another token for this phone. */
    UNAUTHORIZED_MISMATCH,

    /** The wearer blocked this phone on the watch. */
    UNAUTHORIZED_BLOCKED,

    /** The watch app speaks an older protocol than this phone. */
    UPDATE_WATCH,

    /** The watch app speaks a newer protocol than this phone. */
    UPDATE_PHONE,

    /** The watch answered something the protocol does not allow. */
    PROTOCOL_ERROR,

    /** The OpenVitals Wear OS app answered the hello. */
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
    /** The name the watch gave in its hello, once it has. */
    val watchName: String? = null,
    /** The bond this watch once had with the phone is gone. */
    val bondLost: Boolean = false,
) {
    val isAppRunning: Boolean
        get() = appStatus == WearOsAppStatus.APP_RUNNING
}

/** One heart rate sample the watch recorded. */
data class WearOsHeartRateSample(val time: Instant, val beatsPerMinute: Int)

/** One page of a heart rate pull. [hasMore] means the watch cut the reply at its limit. */
data class WearOsHeartRatePage(
    val samples: List<WearOsHeartRateSample>,
    val hasMore: Boolean,
)

/** One page of a sleep minute pull. [hasMore] means the watch cut the reply at its limit. */
data class WearOsSleepMinutePage(
    val minutes: List<WearSleepMinute>,
    val hasMore: Boolean,
)

/**
 * Port for talking to the OpenVitals app on a Wear OS watch.
 *
 * A missing Bluetooth permission surfaces as a [SecurityException], so the
 * screen can turn it into a grant affordance.
 */
interface WearOsNodePort {
    /**
     * [targetAddress] is the registered address, [targetName] the advertised
     * name. Both are hints: the address stored at onboarding is the BLE scan
     * address, which on most watches differs from the bonded Classic one.
     */
    suspend fun checkStatus(
        targetAddress: String? = null,
        targetName: String? = null,
    ): WearOsCompanionStatus

    /**
     * Heart rate samples newer than [since], oldest first, up to the watch's
     * page limit. Null when no bonded watch matches. Throws a
     * [WearOsLinkFailure] for everything else the watch could not give.
     */
    suspend fun pullHeartRate(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsHeartRatePage?

    /**
     * Sleep minutes newer than [since], oldest first, up to the watch's page
     * limit. Null when no bonded watch matches. Throws a [WearOsLinkFailure]
     * for everything else the watch could not give.
     */
    suspend fun pullSleepMinutes(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsSleepMinutePage?
}
