package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeWearOsNodePort : WearOsNodePort {
    var statusToReturn = WearOsCompanionStatus()
    var seenTargetAddress: String? = null
    var seenTargetName: String? = null

    override suspend fun checkStatus(targetAddress: String?, targetName: String?): WearOsCompanionStatus {
        seenTargetAddress = targetAddress
        seenTargetName = targetName
        return statusToReturn
    }
}

class WearOsCompanionManagerTest {

    private lateinit var fakePort: FakeWearOsNodePort
    private lateinit var manager: WearOsCompanionManager

    @Before
    fun setUp() {
        fakePort = FakeWearOsNodePort()
        manager = WearOsCompanionManager(fakePort)
    }

    @Test
    fun `returns NOT_PAIRED when no bonded watch matches`() = runTest {
        fakePort.statusToReturn = WearOsCompanionStatus(
            isPaired = false,
            appStatus = WearOsAppStatus.NOT_PAIRED,
            lastCheckedAt = Instant.now(),
        )

        val status = manager.checkWearOsWatchStatus("A8:D1:62:BE:3A:3B", "Galaxy Watch8 (3A3B) LE")

        assertFalse(status.isPaired)
        assertEquals(WearOsAppStatus.NOT_PAIRED, status.appStatus)
        assertFalse(status.isAppRunning)
        assertEquals("A8:D1:62:BE:3A:3B", fakePort.seenTargetAddress)
        assertEquals("Galaxy Watch8 (3A3B) LE", fakePort.seenTargetName)
    }

    @Test
    fun `returns NO_ANSWER when the watch is paired but the Wear app does not respond`() = runTest {
        fakePort.statusToReturn = WearOsCompanionStatus(
            isPaired = true,
            connectedNodeName = "Galaxy Watch8",
            connectedNodeAddress = "A8:D1:62:BE:3A:3B",
            appStatus = WearOsAppStatus.NO_ANSWER,
            lastCheckedAt = Instant.now(),
        )

        val status = manager.checkWearOsWatchStatus("A8:D1:62:BE:3A:3B")

        assertTrue(status.isPaired)
        assertEquals(WearOsAppStatus.NO_ANSWER, status.appStatus)
        assertFalse(status.isAppRunning)
        assertEquals("Galaxy Watch8", status.connectedNodeName)
    }

    @Test
    fun `returns APP_RUNNING when the Wear app answers`() = runTest {
        fakePort.statusToReturn = WearOsCompanionStatus(
            isPaired = true,
            connectedNodeName = "Pixel Watch 3",
            connectedNodeAddress = "44:55:66:77:88:99",
            appStatus = WearOsAppStatus.APP_RUNNING,
            lastCheckedAt = Instant.now(),
        )

        val status = manager.checkWearOsWatchStatus("44:55:66:77:88:99")

        assertTrue(status.isPaired)
        assertEquals(WearOsAppStatus.APP_RUNNING, status.appStatus)
        assertTrue(status.isAppRunning)
        assertEquals("Pixel Watch 3", status.connectedNodeName)
    }
}
