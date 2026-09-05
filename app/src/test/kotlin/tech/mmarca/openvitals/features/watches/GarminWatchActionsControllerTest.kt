package tech.mmarca.openvitals.features.watches

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/** Find is a toggle, a double-stop must not throw, a refusal is a flag, and a find stands down during a sync. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GarminWatchActionsControllerTest {

    private lateinit var repo: BleDeviceRepository

    /** Records a find, so the toggle can be asserted without a radio. */
    private var seenFindAddress: String? = null
    private var findAccepted = true

    /** Blocks the sync until released, so a find can be attempted mid-sync. */
    private val syncGate = CompletableDeferred<Unit>()

    private val syncPort = object : DeviceSyncPort {
        override fun canSync(device: BleSensorDevice): Boolean = device.isGarminGfdi

        override suspend fun sync(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): DeviceSyncResult {
            syncGate.await()
            return DeviceSyncResult.Succeeded(0)
        }
    }

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
    }

    private fun addWatch(): BleSensorDevice = repo.addDevice(
        displayName = "vívoactive 5",
        address = "E0:48:24:D5:F7:10",
        bluetoothName = "vívoactive 5",
        capabilities = emptySet(),
        kind = BleDeviceKind.WATCH,
        integration = DeviceIntegration.GARMIN,
    )

    private fun TestScope.harness(): Pair<GarminWatchActionsController, DeviceSyncController> {
        val syncController = DeviceSyncController(repo, syncPort, backgroundScope)
        val controller = GarminWatchActionsController(
            deviceRepository = repo,
            syncController = syncController,
            findWatch = { address, cancelled ->
                seenFindAddress = address
                // Ends when the caller cancels, as the real one does.
                cancelled.await()
                findAccepted
            },
            scope = backgroundScope,
        )
        return controller to syncController
    }

    @Test
    fun `find is a toggle - a second tap stops it`() = runTest {
        // The watch alerts for a minute unless cancelled, so the control that starts it stops it.
        val watch = addWatch()
        val (controller, _) = harness()

        val running = controller.toggleFind(watch.id)
        runCurrent()
        assertTrue(controller.state.value.isFindingDevice(watch.id))
        assertEquals(watch.address, seenFindAddress)

        controller.toggleFind(watch.id) // stop
        running!!.join()
        assertNull(controller.state.value.findingDeviceId)
        assertFalse(controller.state.value.findFailed)
    }

    @Test
    fun `stopping twice before the watch answers does not throw`() = runTest {
        // Stop stays enabled until the watch acknowledges, so a second tap lands inside that window.
        val watch = addWatch()
        val (controller, _) = harness()

        val running = controller.toggleFind(watch.id)
        runCurrent()
        assertTrue(controller.state.value.isFindingDevice(watch.id))

        // Both taps before the first stop has come back.
        controller.toggleFind(watch.id)
        controller.toggleFind(watch.id)
        running!!.join()

        assertNull(controller.state.value.findingDeviceId)
    }

    @Test
    fun `a refused find is reported as a flag, not a message`() = runTest {
        // The wording belongs to the screen; this layer has no localizations.
        val watch = addWatch()
        findAccepted = false
        val (controller, _) = harness()

        val running = controller.toggleFind(watch.id)
        runCurrent()
        controller.toggleFind(watch.id)
        running!!.join()

        assertTrue(controller.state.value.findFailed)
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun `a find is refused while a sync holds the radio`() = runTest {
        // One radio: a find must stand down while a sync runs.
        val watch = addWatch()
        val (controller, syncController) = harness()
        val syncing = syncController.syncDevice(watch.id)
        // The sync sets its state before the first await, so the radio reads busy at once.
        assertTrue(syncController.state.value.isSyncing)

        val find = controller.toggleFind(watch.id)

        assertNull(find)
        assertNull(controller.state.value.findingDeviceId)
        assertNull(seenFindAddress) // the find never reached the radio
        syncGate.complete(Unit)
        syncing!!.join()
    }

    @Test
    fun `a find on a non-GFDI device is a no-op`() = runTest {
        val wearos = repo.addDevice(
            displayName = "Galaxy Watch",
            address = "A8:D1:62:BE:3A:3B",
            bluetoothName = "Galaxy Watch8",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )
        val sensor = repo.addDevice(
            displayName = "Strap",
            address = "11:22:33:44:55:66",
            bluetoothName = "TICKR",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val (controller, _) = harness()

        assertNull(controller.toggleFind(wearos.id))
        assertNull(controller.toggleFind(sensor.id))
        assertNull(controller.state.value.findingDeviceId)
        assertNull(seenFindAddress)
    }
}
