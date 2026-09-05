package tech.mmarca.openvitals.devices.core

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The holding discipline over [RadioLeases]: acquire, renew on a timer, release however the work ends. */

/** The radio is held by something else. */
class RadioLeaseBusyException(
    /** The [withRadioLease] owner tag of whatever holds it. */
    val holder: String,
) : Exception("The watch is busy ($holder)")

/** Owner tags. Strings, because they appear in logcat. */
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

/** How long a user action waits for the holder to let go. Longer than the renew interval. */
private const val HANDOVER_WAIT_MILLIS = 8_000L

private const val RETRY_STEP_MILLIS = 250L

/**
 * Runs [body] holding the lease on [address]. Throws
 * [RadioLeaseBusyException] when it cannot be taken.
 */
suspend fun <T> withRadioLease(address: String, owner: String, body: suspend () -> T): T {
    if (!RadioLeases.acquire(address, owner, LEASE_TTL_MILLIS)) {
        // Ask, then wait: the likely holder is notification forwarding, which
        // yields on its next renew tick.
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
        // Renewed on a timer: a download can sit inside one await for seconds.
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
