package tech.mmarca.openvitals.devices.core

import android.os.SystemClock

/**
 * A process-wide exclusive lease on one watch's radio.
 *
 * Ported nearly verbatim from the Flutter build's `RadioLeases.kt`
 * (notification_listener_native). There it had to be native because two
 * Flutter engines each carried their own BLE plugin instance and no Dart mutex
 * could span them; here the whole app is one Kotlin process, so this singleton
 * IS the entire radio discipline: every opener (sync, find/ring, settings
 * link, notification forwarder) acquires a lease per address before touching
 * the radio.
 *
 * **Keyed by address, not global.** Two holders talking to DIFFERENT
 * peripherals is fine. A single global lock would starve notifications for the
 * whole of a three-hour ride with a heart-rate strap connected, which is the
 * wrong answer for a wrist device.
 *
 * **Leases expire.** A holder can be killed at any moment, and a lease that
 * outlived its holder would wedge the radio until reboot. That failure existed
 * in the Flutter codebase once (a connect against a radio still in use that
 * never returns: no error, no log, just a spinner), and it is not worth
 * repeating.
 */
object RadioLeases {

    private data class Lease(val owner: String, val expiresAtMillis: Long)

    private val leases = HashMap<String, Lease>()

    /**
     * Who is waiting for a lease somebody else holds.
     *
     * Notification forwarding holds its lease for as long as the watch is in
     * range, which is indefinitely — so without a way to ask for it back,
     * tapping Sync would block until the user walked away from their own
     * watch. A waiter makes the holder's next [renew] fail, and a holder that
     * cannot renew gives the radio up.
     */
    private val waiters = HashMap<String, String>()

    /**
     * How long after a release the same address stays unavailable.
     *
     * Android closes a `BluetoothGatt` asynchronously. Opening a new one
     * before the old one has finished tearing down is exactly the collision
     * this class exists to prevent, so the lease outlives the release by a
     * moment.
     */
    private const val SETTLE_MILLIS = 300L

    private fun now(): Long = SystemClock.elapsedRealtime()

    /** Drops expired leases so a stale holder cannot block a new one. */
    private fun prune() {
        val cutoff = now()
        leases.entries.removeAll { it.value.expiresAtMillis <= cutoff }
    }

    /**
     * Takes the lease on [address], or returns false when somebody else holds
     * it. Re-acquiring a lease already held by [owner] succeeds and extends
     * it, so a caller need not track whether it already has one.
     */
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

    /**
     * Announces that [owner] wants the lease on [address].
     *
     * Does not grant anything — it makes the current holder's next [renew]
     * fail, which is how an indefinitely-held lease is given up. The caller
     * then retries [acquire].
     */
    @Synchronized
    fun request(address: String, owner: String) {
        prune()
        val held = leases[address]
        if (held == null || held.owner == owner) return
        waiters[address] = owner
    }

    /**
     * Extends a held lease. False once it has expired, been taken, or somebody
     * else has asked for it — the last of which is the holder's cue to stop.
     */
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

    /**
     * Releases a lease held by [owner].
     *
     * A no-op when [owner] is not the holder — a release arriving late, after
     * the lease expired and somebody else took it, must not cancel their work.
     */
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
