package tech.mmarca.openvitals.devices.core

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The holding discipline over [RadioLeases]: acquire (waiting out a holder
 * that yields), renew on a timer while working, release however the work
 * ends. Port of the Flutter build's `withGarminRadio` from
 * `garmin_radio_lease.dart`, minus the cross-isolate plumbing the single
 * Kotlin process no longer needs.
 */

/** The radio is held by something else. */
class RadioLeaseBusyException(
    /** The [withRadioLease] owner tag of whatever holds it. */
    val holder: String,
) : Exception("The watch is busy ($holder)")

/**
 * Owner tags. Stable strings rather than an enum because they appear in
 * logcat, where a readable name is the whole point.
 */
object RadioLeaseOwner {
    const val SYNC = "sync"
    const val FIND = "find"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
}

/** How long a lease survives without a renewal. */
private const val LEASE_TTL_MILLIS = 15_000L

/** The holder renews at this cadence while it is working. */
private const val RENEW_INTERVAL_MILLIS = 5_000L

/**
 * How long a user-initiated action waits for the holder to let go.
 *
 * Comfortably longer than the renew interval, which bounds how quickly an
 * indefinite holder (the notification forwarder) notices it has been asked to
 * stop.
 */
private const val HANDOVER_WAIT_MILLIS = 8_000L

private const val RETRY_STEP_MILLIS = 250L

/**
 * Runs [body] holding the lease on [address], renewing it throughout, and
 * releases it however [body] ends.
 *
 * Throws [RadioLeaseBusyException] when the lease cannot be taken. Callers
 * decide what that means: a user-initiated action reports it, a background
 * holder backs off and tries again.
 */
suspend fun <T> withRadioLease(address: String, owner: String, body: suspend () -> T): T {
    if (!RadioLeases.acquire(address, owner, LEASE_TTL_MILLIS)) {
        // Ask, then wait, rather than failing outright. The likely holder is
        // notification forwarding, which gives the radio up on its next renew
        // tick — a second or two — and a user who tapped Sync should not be
        // told the watch is busy with something they never asked for.
        RadioLeases.request(address, owner)
        var waited = 0L
        while (waited < HANDOVER_WAIT_MILLIS) {
            delay(RETRY_STEP_MILLIS)
            waited += RETRY_STEP_MILLIS
            if (RadioLeases.acquire(address, owner, LEASE_TTL_MILLIS)) {
                return runHolding(address, owner, body)
            }
        }
        throw RadioLeaseBusyException(RadioLeases.owner(address) ?: "another task")
    }
    return runHolding(address, owner, body)
}

private suspend fun <T> runHolding(address: String, owner: String, body: suspend () -> T): T =
    coroutineScope {
        // Renewed on a timer rather than at each protocol step: a file
        // download can sit inside one await for several seconds, and a lease
        // that expired mid-sync would let another holder open a second link to
        // the same watch.
        val renewals = launch {
            while (isActive) {
                delay(RENEW_INTERVAL_MILLIS)
                RadioLeases.renew(address, owner, LEASE_TTL_MILLIS)
            }
        }
        try {
            body()
        } finally {
            renewals.cancel()
            RadioLeases.release(address, owner)
        }
    }
