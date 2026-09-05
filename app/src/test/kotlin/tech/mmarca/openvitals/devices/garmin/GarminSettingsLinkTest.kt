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
 * Regression for "stop waiting on a link that has gone" (7b7858978): a dropped link must
 * resolve everything in flight at once. These assert the read completes without advancing the clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GarminSettingsLinkTest {

    private fun link(scope: CoroutineScope): GarminSettingsLink =
        GarminSettingsLink.forTest(
            scope,
            GarminSession(
                scope = scope,
                // The watch never answers.
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

        // Resolved with no time advanced. On regression this waits out the 30-second timeout.
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
