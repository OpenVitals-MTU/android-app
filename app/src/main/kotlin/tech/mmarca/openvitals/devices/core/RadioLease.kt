package tech.mmarca.openvitals.devices.core

import android.os.SystemClock

/**
 * A process-wide exclusive lease on one watch's radio. Every opener (sync,
 * find, settings, notifications) acquires one per address first. Keyed by
 * address: two holders on different peripherals is fine. Leases expire, so
 * a killed holder cannot wedge the radio until reboot.
 */
object RadioLeases {

    private data class Lease(val owner: String, val expiresAtMillis: Long)

    private val leases = HashMap<String, Lease>()

    /**
     * Who is waiting for a held lease. A waiter makes the holder's next
     * [renew] fail, which is how an indefinitely held lease is given up.
     */
    private val waiters = HashMap<String, String>()

    /** How long after a release the address stays unavailable: GATT closes asynchronously. */
    private const val SETTLE_MILLIS = 300L

    private fun now(): Long = SystemClock.elapsedRealtime()

    /** Drops expired leases so a stale holder cannot block a new one. */
    private fun prune() {
        val cutoff = now()
        leases.entries.removeAll { it.value.expiresAtMillis <= cutoff }
    }

    /** Takes the lease, or false when someone else holds it. Re-acquiring by [owner] extends it. */
    @Synchronized
    fun acquire(address: String, owner: String, ttlMillis: Long): Boolean {
        prune()
        val held = leases[address]
        if (held != null && held.owner != owner) return false
        // Taking the lease satisfies this owner's own request, if it made one.
        if (waiters[address] == owner) waiters.remove(address)
        leases[address] = Lease(owner, now() + ttlMillis)
        return true
    }

    /** Announces that [owner] wants the lease. Makes the holder's next [renew] fail. */
    @Synchronized
    fun request(address: String, owner: String) {
        prune()
        val held = leases[address]
        if (held == null || held.owner == owner) return
        waiters[address] = owner
    }

    /** Extends a held lease. False once expired, taken, or requested by someone else. */
    @Synchronized
    fun renew(address: String, owner: String, ttlMillis: Long): Boolean {
        prune()
        val held = leases[address] ?: return false
        if (held.owner != owner) return false
        val waiter = waiters[address]
        if (waiter != null && waiter != owner) return false
        leases[address] = Lease(owner, now() + ttlMillis)
        return true
    }

    /** Releases a lease held by [owner]. A late release must not cancel someone else's work. */
    @Synchronized
    fun release(address: String, owner: String) {
        val held = leases[address] ?: return
        if (held.owner != owner) return
        // Not removed outright: see SETTLE_MILLIS.
        leases[address] = Lease(owner, now() + SETTLE_MILLIS)
    }

    /** Who holds [address], or null when it is free. Diagnostic. */
    @Synchronized
    fun owner(address: String): String? {
        prune()
        return leases[address]?.owner
    }

    /** Test seam: forgets every lease. */
    @Synchronized
    fun clear() {
        leases.clear()
        waiters.clear()
    }
}
