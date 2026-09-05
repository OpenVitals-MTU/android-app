package tech.mmarca.openvitals.features.watches

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * Row-state discipline: one sync at a time, progress scoped to the syncing device,
 * failures as a banner, non-claimable devices a no-op. The pull, import and stamp are in `GarminWatchSyncService`.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DeviceSyncControllerTest {

    /** Stands in for the whole radio + protocol + import stack. */
    private class FakeSyncPort : DeviceSyncPort {
        var result: DeviceSyncResult = DeviceSyncResult.Succeeded(0)
        var calls = 0
        var seenDevice: BleSensorDevice? = null
        var seenListenAfter: Duration? = null
        var progressToReport: DeviceSyncProgress? = DeviceSyncProgress(
            phase = DeviceSyncPhase.DOWNLOADING,
            filesTotal = 2,
            filesDone = 1,
        )

        /** Left incomplete by a test to hold a sync mid-flight. */
        var gate: CompletableDeferred<Unit>? = null

        /** Set by the test that breaks the never-throws contract on purpose. */
        var thrown: Exception? = null

        override fun canSync(device: BleSensorDevice): Boolean = device.isGarminGfdi

        override suspend fun sync(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): DeviceSyncResult {
            calls++
            seenDevice = device
            seenListenAfter = listenAfter
            progressToReport?.let { onProgress?.invoke(it) }
            gate?.await()
            thrown?.let { throw it }
            return result
        }
    }

    private lateinit var repo: BleDeviceRepository
    private lateinit var port: FakeSyncPort

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        port = FakeSyncPort()
    }

    private fun addWatch(): BleSensorDevice = repo.addDevice(
        displayName = "vívoactive 5",
        address = "E0:48:24:D5:F7:10",
        bluetoothName = "vívoactive 5",
        capabilities = emptySet(),
        kind = BleDeviceKind.WATCH,
        integration = DeviceIntegration.GARMIN,
    )

    private fun kotlinx.coroutines.test.TestScope.controller() =
        DeviceSyncController(repo, port, backgroundScope)

    @Test
    fun `a successful sync ends idle with the file count`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Succeeded(3)
        val controller = controller()

        controller.syncDevice(watch.id)!!.join()

        assertEquals(1, port.calls)
        assertEquals(watch.id, port.seenDevice?.id)
        assertFalse(controller.state.value.isSyncing)
        assertEquals(DeviceSyncPhase.COMPLETE, controller.state.value.phase)
        assertEquals(3, controller.state.value.lastFileCount)
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun `the radio reads busy the instant the tap returns`() = runTest {
        val watch = addWatch()
        port.gate = CompletableDeferred()
        val controller = controller()

        val job = controller.syncDevice(watch.id)

        assertNotNull(job)
        assertTrue(controller.state.value.isSyncing)
        assertTrue(controller.state.value.isSyncingDevice(watch.id))
        port.gate!!.complete(Unit)
        job!!.join()
    }

    @Test
    fun `reports progress scoped to the syncing device`() = runTest {
        val watch = addWatch()
        val other = repo.addDevice(
            displayName = "Other watch",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Other",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.GARMIN,
        )
        port.gate = CompletableDeferred()
        val controller = controller()

        val job = controller.syncDevice(watch.id)!!
        runCurrent()

        assertTrue(controller.state.value.isSyncingDevice(watch.id))
        assertFalse(controller.state.value.isSyncingDevice(other.id))
        assertEquals(2, controller.state.value.filesTotal)
        assertEquals(1, controller.state.value.filesDone)
        port.gate!!.complete(Unit)
        job.join()
    }

    @Test
    fun `refuses a second sync while one is running`() = runTest {
        val watch = addWatch()
        port.gate = CompletableDeferred()
        val controller = controller()

        val first = controller.syncDevice(watch.id)
        val second = controller.syncDevice(watch.id)

        // One radio: the second call is dropped rather than queued.
        assertNotNull(first)
        assertNull(second)
        port.gate!!.complete(Unit)
        first!!.join()
        assertEquals(1, port.calls)
    }

    @Test
    fun `a failure surfaces its message and ends idle`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Failed("Could not connect: timeout")
        val controller = controller()

        controller.syncDevice(watch.id)!!.join()

        assertFalse(controller.state.value.isSyncing)
        assertEquals("Could not connect: timeout", controller.state.value.errorMessage)
        assertNull(controller.state.value.lastFileCount)
    }

    @Test
    fun `ignores a device the port does not claim`() = runTest {
        val sensor = repo.addDevice(
            displayName = "Chest strap",
            address = "11:22:33:44:55:66",
            bluetoothName = "TICKR",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val controller = controller()

        assertNull(controller.syncDevice(sensor.id))
        assertEquals(0, port.calls)
        assertFalse(controller.state.value.isSyncing)
    }

    @Test
    fun `ignores an unknown device id`() = runTest {
        val controller = controller()

        assertNull(controller.syncDevice("does-not-exist"))
        assertEquals(0, port.calls)
    }

    @Test
    fun `clear resets the banner but never a running sync`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Succeeded(2)
        val controller = controller()
        controller.syncDevice(watch.id)!!.join()
        assertNotNull(controller.state.value.lastFileCount)

        controller.clear()

        assertNull(controller.state.value.lastFileCount)
        assertNull(controller.state.value.errorMessage)

        // A running sync's state must survive a stray clear().
        port.gate = CompletableDeferred()
        val job = controller.syncDevice(watch.id)!!
        controller.clear()
        assertTrue(controller.state.value.isSyncing)
        port.gate!!.complete(Unit)
        job.join()
    }

    @Test
    fun `a port that throws still leaves the radio free`() = runTest {
        val watch = addWatch()
        port.thrown = IllegalStateException("the port broke its contract")
        val controller = controller()

        controller.syncDevice(watch.id)!!.join()

        // A sync never throws. If one does, the radio must not read busy forever.
        assertFalse(controller.state.value.isSyncing)
        assertEquals("the port broke its contract", controller.state.value.errorMessage)
        assertNotNull(controller.syncDevice(watch.id))
    }

    @Test
    fun `a silent failure ends idle without a banner`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Failed("Could not connect: timeout")
        val controller = controller()

        // A scheduled sync out of range at 3am must not leave a red message for later.
        controller.syncDevice(watch.id, silent = true)!!.join()

        assertFalse(controller.state.value.isSyncing)
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun `a silent success still reports the files it took`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Succeeded(4)
        val controller = controller()

        controller.syncDevice(watch.id, silent = true)!!.join()

        assertEquals(4, controller.state.value.lastFileCount)
    }

    @Test
    fun `the outcome comes back to the caller that awaits it`() = runTest {
        val watch = addWatch()
        port.result = DeviceSyncResult.Failed("The watch is busy (notifications)")
        val controller = controller()

        // What the scheduled run reads to decide between a retry and waiting.
        val result = controller.syncDevice(watch.id, silent = true)!!.await()

        assertEquals(DeviceSyncResult.Failed("The watch is busy (notifications)"), result)
    }

    @Test
    fun `passes the listen-after diagnostic window down to the port`() = runTest {
        val watch = addWatch()
        val controller = controller()

        controller.syncDevice(watch.id, listenAfter = Duration.ZERO)!!.join()

        assertEquals(Duration.ZERO, port.seenListenAfter)
    }
}
