package tech.mmarca.openvitals.features.watches

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminGattClientException
import tech.mmarca.openvitals.devices.garmin.GarminSession
import tech.mmarca.openvitals.devices.garmin.GarminSettingsLink

/**
 * The held-link registry: one link per watch, shared by every screen browsing it, surviving
 * the last screen by a grace window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchSettingsLinksTest {

    private class Opens {
        var count = 0
        val links = mutableListOf<GarminSettingsLink>()
    }

    private fun TestScope.registry(
        opens: Opens,
        failFirst: Boolean = false,
    ): WatchSettingsLinks = WatchSettingsLinks(
        // Its own supervisor scope on the test clock, as in production.
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
        grace = 20.seconds,
        opener = { scope, _ ->
            opens.count++
            if (failFirst && opens.count == 1) {
                throw GarminGattClientException("out of range")
            }
            newLink(scope).also { opens.links.add(it) }
        },
    )

    private fun newLink(scope: CoroutineScope): GarminSettingsLink =
        GarminSettingsLink.forTest(
            scope,
            GarminSession(
                scope = scope,
                send = {},
                bluetoothName = "Pixel 6 Pro",
                manufacturer = "Google",
                model = "raven",
                syncFiles = false,
            ),
        )

    @Test
    fun `screens of one watch share one link`() = runTest {
        val opens = Opens()
        val links = registry(opens)

        links.retain("watch-1") // the list screen
        links.retain("watch-1") // one alarm, pushed on top
        val first = links.link("watch-1")
        val second = links.link("watch-1")

        assertSame(first, second)
        assertEquals(1, opens.count)
    }

    @Test
    fun `the link outlives the last screen by the grace window then closes`() = runTest {
        val opens = Opens()
        val links = registry(opens)

        links.retain("watch-1")
        val link = links.link("watch-1")
        links.release("watch-1")

        advanceTimeBy(19.seconds)
        runCurrent()
        assertTrue(link.isOpen)
        assertTrue(links.isHeld("watch-1"))

        advanceTimeBy(2.seconds)
        runCurrent()
        assertFalse(link.isOpen)
        assertFalse(links.isHeld("watch-1"))
    }

    @Test
    fun `re-retaining within the grace keeps the link and its handshake`() = runTest {
        val opens = Opens()
        val links = registry(opens)

        links.retain("watch-1")
        val link = links.link("watch-1")
        links.release("watch-1")

        advanceTimeBy(10.seconds)
        runCurrent()
        links.retain("watch-1") // came back before the grace ran out

        advanceTimeBy(60.seconds)
        runCurrent()
        assertTrue(link.isOpen)
        assertSame(link, links.link("watch-1"))
        assertEquals(1, opens.count)
    }

    @Test
    fun `a failed open is retried on the next ask`() = runTest {
        val opens = Opens()
        val links = registry(opens, failFirst = true)

        links.retain("watch-1")
        try {
            links.link("watch-1")
            fail("expected the first open to fail")
        } catch (_: GarminGattClientException) {
            // Expected: the watch was out of range.
        }

        // "Try again" is asking again: the registry replaces the failed attempt.
        val link = links.link("watch-1")
        assertTrue(link.isOpen)
        assertEquals(2, opens.count)
    }

    @Test
    fun `releaseNow closes the held link at once`() = runTest {
        // A caller that wants the radio takes the link down and waits for it.
        val opens = Opens()
        val links = registry(opens)

        links.retain("watch-1")
        val link = links.link("watch-1")

        links.releaseNow("watch-1")
        assertFalse(link.isOpen)
        assertFalse(links.isHeld("watch-1"))
    }
}
