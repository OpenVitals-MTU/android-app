package tech.mmarca.openvitals.devices.garmin

import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port of the Flutter build's `garmin_notification_forwarder_test.dart`. The
 * Dart suite ran under `fakeAsync`; here the forwarder's timers are `delay`
 * jobs on the test scope, so `advanceTimeBy` plays the same role — no radio
 * and no real time anywhere.
 */
class GarminNotificationForwarderTest {

    private companion object {
        const val ADDRESS = "AA:BB:CC:DD:EE:FF"
    }

    /** A link that opens instantly and records what it was asked to send. */
    private class FakeLink : GarminNotificationLink {

        val pushed = mutableListOf<GarminNotification>()
        val withdrawn = mutableListOf<Long>()
        var closed = false

        private val gone = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

        /**
         * A real handler, because the forwarder reads `handler.held` on close
         * to take back anything the watch never subscribed for. Its `send`
         * goes nowhere: these tests assert on [pushed], not on wire bytes.
         */
        override val handler = GarminGncsHandler(send = {})

        override val isOpen: Boolean get() = !closed

        override val subscribed: Boolean get() = handler.enabled

        override val onGone: Flow<Unit> = gone

        /** Simulates the watch walking out of range. */
        fun drop() {
            gone.tryEmit(Unit)
        }

        override suspend fun push(notification: GarminNotification) {
            pushed.add(notification)
            // Routed through the real handler, as the real link does, so
            // `handler.held` reflects what has actually been announced versus
            // what is still waiting on the watch to subscribe.
            handler.post(notification)
        }

        override suspend fun withdraw(notificationId: Long) {
            withdrawn.add(notificationId)
        }

        override suspend fun close() {
            closed = true
        }
    }

    /** A lease that can be held by somebody else, to test priority. */
    private class FakeLease : GarminRadioLease {
        var holder: String? = null
        var acquireAttempts = 0

        /** Set by a test to simulate a sync asking for the radio. */
        var requestedBy: String? = null

        override fun acquire(address: String, owner: String): Boolean {
            acquireAttempts++
            if (holder != null && holder != owner) return false
            holder = owner
            return true
        }

        override fun request(address: String, owner: String) {
            requestedBy = owner
        }

        override fun renew(address: String, owner: String): Boolean =
            holder == owner && (requestedBy == null || requestedBy == owner)

        override fun release(address: String, owner: String) {
            if (holder == owner) holder = null
        }

        override fun owner(address: String): String? = holder
    }

    private fun notification(id: Long) = GarminNotification(
        id = id,
        packageName = "com.example.chat",
        title = "Ada",
        body = "On my way",
        postedAt = LocalDateTime.of(2026, 7, 28, 9, 5, 3),
    )

    private class Fixture(
        val forwarder: GarminNotificationForwarder,
        val links: MutableList<FakeLink>,
        val lease: FakeLease,
    )

    /** Builds a forwarder handing out one [FakeLink] per open() call. */
    private fun TestScope.build(
        lease: FakeLease = FakeLease(),
        openOverride: (suspend () -> GarminNotificationLink)? = null,
        onIdle: (() -> Unit)? = null,
    ): Fixture {
        val links = mutableListOf<FakeLink>()
        val forwarder = GarminNotificationForwarder(
            scope = backgroundScope,
            address = ADDRESS,
            phoneName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            lease = lease,
            openLink = {
                if (openOverride != null) {
                    openOverride()
                } else {
                    FakeLink().also { links.add(it) }
                }
            },
            onIdle = onIdle,
        )
        return Fixture(forwarder, links, lease)
    }

    private fun TestScope.elapse(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    // ── coalescing ──────────────────────────────────────────────────────────

    @Test
    fun `three notifications 200ms apart open ONE link, not three`() = runTest {
        val f = build()
        for (i in 0L until 3L) {
            f.forwarder.post(notification(i))
            runCurrent()
            elapse(200)
        }
        elapse(2_000)

        assertEquals(1, f.links.size)
        assertEquals(listOf(0L, 1L, 2L), f.links.single().pushed.map { it.id })
    }

    @Test
    fun `a steady drip cannot postpone the connect past the ceiling`() = runTest {
        val f = build()
        // One arrival per second forever would reset a pure debounce every
        // time.
        for (i in 0L until 10L) {
            f.forwarder.post(notification(i))
            runCurrent()
            elapse(1_000)
        }

        assertTrue(
            "the max coalesce wait must force the connect",
            f.links.isNotEmpty(),
        )
    }

    @Test
    fun `a notification arriving while the link is open is sent immediately`() = runTest {
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)
        assertEquals(1, f.links.single().pushed.size)

        f.forwarder.post(notification(2))
        runCurrent()
        elapse(10)

        assertEquals(2, f.links.single().pushed.size)
        assertEquals(1, f.links.size)
    }

    @Test
    fun `a dismissal is forwarded as a withdrawal`() = runTest {
        val f = build()
        f.forwarder.withdraw(42)
        runCurrent()
        elapse(2_000)

        assertEquals(listOf(42L), f.links.single().withdrawn)
    }

    // ── the link is held ────────────────────────────────────────────────────

    @Test
    fun `the link is still open minutes after the last notification`() = runTest {
        // The whole point of the change: a Garmin watch expects a continuously
        // connected phone, and a link that closes leaves it saying "reconnect
        // to phone to refresh data" and sometimes failing to re-subscribe.
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(10 * 60 * 1_000)

        assertTrue(f.forwarder.isLinkOpen)
        assertEquals("and it was never re-opened", 1, f.links.size)
    }

    @Test
    fun `a watch that walks out of range is reconnected to`() = runTest {
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)
        f.links.single().drop()
        elapse(20_000)

        assertEquals(2, f.links.size)
        assertTrue(f.forwarder.isLinkOpen)
    }

    @Test
    fun `a notification the watch never subscribed for survives the link dropping`() = runTest {
        // The handler lives and dies with its link, and the forwarder has
        // already dropped the item from its own queue by then — so without
        // taking the unannounced ones back, a watch that walks away in the
        // second between "queued" and "subscribed" loses exactly the
        // notification the link was opened for.
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)

        // The link is up but the watch has not subscribed, so the handler is
        // holding it rather than having announced it.
        val first = f.links.single()
        assertEquals(1L, first.pushed.single().id)
        assertEquals(
            "held, because the watch has not subscribed",
            listOf(1L),
            first.handler.held.map { it.id },
        )

        first.drop()
        elapse(20_000)

        assertEquals(2, f.links.size)
        assertTrue(f.links.last().pushed.map { it.id }.contains(1L))
    }

    @Test
    fun `a watch that stays away is retried on a growing backoff, not in a tight loop`() =
        runTest {
            var attempts = 0
            val f = build(openOverride = {
                attempts++
                throw IllegalStateException("out of range")
            })
            f.forwarder.post(notification(1))
            runCurrent()
            elapse(30 * 60 * 1_000)

            // Unbounded retries at the first backoff would be ~120 in half an
            // hour.
            assertTrue("$attempts attempts", attempts < 15)
            assertTrue("but it must keep trying ($attempts attempts)", attempts > 3)
        }

    @Test
    fun `a notification that arrives while the watch is away is kept for when it returns`() =
        runTest {
            var fail = true
            val links = mutableListOf<FakeLink>()
            val forwarder = GarminNotificationForwarder(
                scope = backgroundScope,
                address = ADDRESS,
                phoneName = "Pixel 6 Pro",
                manufacturer = "Google",
                model = "raven",
                lease = FakeLease(),
                openLink = {
                    if (fail) throw IllegalStateException("out of range")
                    FakeLink().also { links.add(it) }
                },
            )

            forwarder.post(notification(1))
            runCurrent()
            elapse(5_000)
            assertTrue(links.isEmpty())

            fail = false // the watch comes back
            elapse(30_000)

            assertEquals(
                "the notification the link was opened for must survive",
                1L,
                links.single().pushed.single().id,
            )
        }

    // ── the radio lease ─────────────────────────────────────────────────────

    @Test
    fun `the lease is taken before connecting and held with the link`() = runTest {
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)
        assertEquals(GarminRadioOwners.NOTIFICATIONS, f.lease.holder)

        elapse(5 * 60 * 1_000)
        assertEquals(GarminRadioOwners.NOTIFICATIONS, f.lease.holder)
    }

    @Test
    fun `the radio is given up when a sync asks for it, and taken back after`() = runTest {
        // Without this a permanently held link would block every sync, find
        // and settings browse — things the user actively initiated.
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)
        assertTrue(f.forwarder.isLinkOpen)

        f.lease.requestedBy = GarminRadioOwners.SYNC
        elapse(10_000)

        assertFalse(f.forwarder.isLinkOpen)
        assertNull("so the sync can take it", f.lease.holder)

        // The sync finishes and stops asking.
        f.lease.requestedBy = null
        elapse(30_000)

        assertTrue(f.forwarder.isLinkOpen)
    }

    @Test
    fun `a sync holding the radio defers the notification instead of interrupting it`() =
        runTest {
            val lease = FakeLease().apply { holder = GarminRadioOwners.SYNC }
            val f = build(lease = lease)
            f.forwarder.post(notification(1))
            runCurrent()
            elapse(2_000)

            assertTrue("the sync must not be interrupted", f.links.isEmpty())
        }

    @Test
    fun `the deferred notification is sent once the sync releases the radio`() = runTest {
        val lease = FakeLease().apply { holder = GarminRadioOwners.SYNC }
        val f = build(lease = lease)
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)

        lease.holder = null // the sync finished
        elapse(15_000)

        assertEquals(1, f.links.size)
        assertEquals(1L, f.links.single().pushed.single().id)
    }

    // ── failure ─────────────────────────────────────────────────────────────

    @Test
    fun `a failed connect does not leave the lease held`() = runTest {
        val f = build(openOverride = { throw IllegalStateException("out of range") })
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(5_000)

        assertNull(f.lease.holder)
    }

    @Test
    fun `a held link never reports idle, so the forwarder is not torn down`() = runTest {
        // The bridge disposes the forwarder on onIdle. Firing it while a link
        // is held would kill the forwarder underneath a connected watch.
        var idleCount = 0
        val f = build(onIdle = { idleCount++ })
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(5 * 60 * 1_000)

        assertEquals(0, idleCount)
        assertTrue(f.forwarder.isLinkOpen)
    }

    @Test
    fun `disposing closes an open link and releases the radio`() = runTest {
        val f = build()
        f.forwarder.post(notification(1))
        runCurrent()
        elapse(2_000)

        f.forwarder.dispose()
        elapse(10)

        assertTrue(f.links.single().closed)
        assertNull(f.lease.holder)
    }
}
