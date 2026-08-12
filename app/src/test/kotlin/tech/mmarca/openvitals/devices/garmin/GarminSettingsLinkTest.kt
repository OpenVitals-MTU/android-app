package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port of the Flutter build's `garmin_settings_link_test.dart`.
 *
 * Regression for "stop waiting on a link that has gone" (7b7858978).
 *
 * Tapping an alarm hung on "Reading from the watch": the STATE request got no
 * reply, the link dropped ten seconds later, and the pending request sat out
 * its full thirty-second timeout waiting for an answer that could no longer
 * come. A dropped or closed link must resolve everything in flight at once.
 *
 * Where the Dart test bounded the wait with a two-second real-time timeout,
 * virtual time would satisfy that even on regression — so these assert the
 * read completes WITHOUT advancing the clock at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GarminSettingsLinkTest {

    private fun link(scope: CoroutineScope): GarminSettingsLink =
        GarminSettingsLink.forTest(
            scope,
            GarminSession(
                scope = scope,
                // The watch never answers — every request would wait out its
                // timeout.
                send = {},
                bluetoothName = "Pixel 6 Pro",
                manufacturer = "Google",
                model = "raven",
                syncFiles = false,
            ),
        )

    @Test
    fun `closing the link resolves an in-flight screen read at once`() = runTest {
        val l = link(backgroundScope)
        // 65600 is the alarm screen id from the original hang.
        val pending = async { l.screen(65600) }
        runCurrent()

        l.close()
        runCurrent()

        // Resolved with NO time advanced: on regression this stays pending
        // until the 30-second reply timeout is played out.
        assertTrue(pending.isCompleted)
        assertNull(pending.await())
    }

    @Test
    fun `a request on an already-closed link answers null immediately`() = runTest {
        val l = link(backgroundScope)
        l.close()
        runCurrent()

        assertFalse(l.isOpen)
        val pending = async { l.screen(65600) }
        runCurrent()
        assertTrue(pending.isCompleted)
        assertNull(pending.await())
    }
}
