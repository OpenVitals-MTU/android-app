package tech.mmarca.openvitals.devices.core

import android.os.SystemClock
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The single-process radio discipline: one exclusive, expiring, per-address lease. The clock is mocked. */
class RadioLeasesTest {

    private var nowMillis = 0L

    private companion object {
        const val ADDRESS = "AA:BB:CC:DD:EE:01"
        const val OTHER_ADDRESS = "AA:BB:CC:DD:EE:02"
    }

    @Before
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { nowMillis }
        RadioLeases.clear()
    }

    @After
    fun tearDown() {
        RadioLeases.clear()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `a held lease excludes another owner on the same address`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 10_000))
        assertFalse(RadioLeases.acquire(ADDRESS, "forwarder", 10_000))
        assertEquals("sync", RadioLeases.owner(ADDRESS))
    }

    @Test
    fun `leases are keyed by address, not global`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 10_000))
        assertTrue(RadioLeases.acquire(OTHER_ADDRESS, "forwarder", 10_000))
    }

    @Test
    fun `re-acquiring extends a lease the owner already holds`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        nowMillis += 900
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        // Without the extension the original TTL would have lapsed here.
        nowMillis += 900
        assertEquals("sync", RadioLeases.owner(ADDRESS))
    }

    @Test
    fun `an expired lease cannot block a new holder`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        nowMillis += 1_000
        assertNull(RadioLeases.owner(ADDRESS))
        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
    }

    @Test
    fun `renew extends only while held and unwanted`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
        assertTrue(RadioLeases.renew(ADDRESS, "forwarder", 1_000))

        // A waiter makes the holder's next renew fail: that is how an indefinite lease is given up.
        RadioLeases.request(ADDRESS, "sync")
        assertFalse(RadioLeases.renew(ADDRESS, "forwarder", 1_000))
    }

    @Test
    fun `renew fails once the lease expired or was taken`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        nowMillis += 1_000
        assertFalse(RadioLeases.renew(ADDRESS, "sync", 1_000))

        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
        assertFalse(RadioLeases.renew(ADDRESS, "sync", 1_000))
    }

    @Test
    fun `acquiring satisfies the owner's own outstanding request`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
        RadioLeases.request(ADDRESS, "sync")
        nowMillis += 1_000

        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        // The waiter entry is gone: sync can renew its own fresh lease.
        assertTrue(RadioLeases.renew(ADDRESS, "sync", 1_000))
    }

    @Test
    fun `request against a free or own lease is a no-op`() {
        RadioLeases.request(ADDRESS, "sync")
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        RadioLeases.request(ADDRESS, "sync")
        assertTrue(RadioLeases.renew(ADDRESS, "sync", 1_000))
    }

    @Test
    fun `release keeps the address unavailable for the settle window`() {
        // Android closes a BluetoothGatt asynchronously; a connect inside that window is the collision the lease prevents.
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 10_000))
        RadioLeases.release(ADDRESS, "sync")

        assertFalse(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
        nowMillis += 300
        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 1_000))
    }

    @Test
    fun `a late release by a non-holder does not cancel the new holder`() {
        assertTrue(RadioLeases.acquire(ADDRESS, "sync", 1_000))
        nowMillis += 1_000
        assertTrue(RadioLeases.acquire(ADDRESS, "forwarder", 10_000))

        RadioLeases.release(ADDRESS, "sync")

        assertEquals("forwarder", RadioLeases.owner(ADDRESS))
        // And the new holder's lease was not shortened to the settle window.
        nowMillis += 5_000
        assertEquals("forwarder", RadioLeases.owner(ADDRESS))
    }
}
