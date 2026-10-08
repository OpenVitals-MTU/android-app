package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.ZoneOffset

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

/** One heart rate sample the watch recorded. */
data class WearOsHeartRateSample(val time: Instant, val beatsPerMinute: Int)

/** One page of a heart rate pull. [hasMore] means the watch cut the reply at its limit. */
data class WearOsHeartRatePage(
    val samples: List<WearOsHeartRateSample>,
    val hasMore: Boolean,
)

/** One minute of sleep input the watch recorded; see `WearLinkProtocol.SleepMinute`. */
data class WearOsSleepMinute(
    val time: Instant,
    val kind: WearLinkProtocol.MinuteKind,
    val movement: Float,
    /** Beats per minute, or null when the minute carried none. */
    val heartRate: Float?,
    val zoneOffset: ZoneOffset,
)

/** One page of a sleep minute pull. [hasMore] means the watch cut the reply at its limit. */
data class WearOsSleepMinutePage(
    val minutes: List<WearOsSleepMinute>,
    val hasMore: Boolean,
)

/** The watch could not be reached, or answered with something other than the protocol. */
class WearOsLinkException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
     * page limit. Null when no bonded watch matches. Throws
     * [WearOsLinkException] when the watch does not answer.
     */
    suspend fun pullHeartRate(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsHeartRatePage?

    /**
     * Sleep minutes newer than [since], oldest first, up to the watch's page
     * limit. Null when no bonded watch matches. Throws [WearOsLinkException]
     * when the watch does not answer.
     */
    suspend fun pullSleepMinutes(
        targetAddress: String?,
        targetName: String?,
        since: Instant,
    ): WearOsSleepMinutePage?
}
