package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.geo.GeoCoordinateError
import tech.mmarca.openvitals.core.geo.GeoCoordinateParseResult
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.devices.garmin.GarminWaypoint
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_LATITUDE_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_LONGITUDE_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_NAME_ARG
import tech.mmarca.openvitals.navigation.WATCH_POINT_UNREADABLE_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The form picks a watch, reads the coordinates, and never sends on its own. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WatchSendPointViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: BleDeviceRepository
    private lateinit var stateStore: GarminDeviceStateStore
    private val sent = mutableListOf<Pair<String, GarminWaypoint>>()
    private val sendGate = CompletableDeferred<GarminSendFileResult>()

    private val syncPort = object : DeviceSyncPort {
        override fun canSync(device: BleSensorDevice): Boolean = true

        override suspend fun sync(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): DeviceSyncResult = DeviceSyncResult.Succeeded(0)
    }

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        stateStore = GarminDeviceStateStore(FakeSharedPreferences())
    }

    private fun addGarmin(name: String, address: String): BleSensorDevice = repo.addDevice(
        displayName = name,
        address = address,
        bluetoothName = name,
        capabilities = emptySet(),
        kind = BleDeviceKind.WATCH,
        integration = DeviceIntegration.GARMIN,
    )

    private fun TestScope.viewModel(vararg args: Pair<String, String?>): WatchSendPointViewModel {
        val syncController = DeviceSyncController(repo, syncPort, backgroundScope)
        val controller = GarminWatchActionsController(
            deviceRepository = repo,
            syncController = syncController,
            findWatch = { _, _ -> true },
            sendPoint = { device, point, _ ->
                sent += device.id to point
                sendGate.await()
            },
            scope = backgroundScope,
        )
        return WatchSendPointViewModel(
            savedStateHandle = SavedStateHandle(mapOf(*args)),
            deviceRepository = repo,
            stateStore = stateStore,
            actionsController = controller,
            syncController = syncController,
        ).also { model ->
            // WhileSubscribed: the state only moves while something collects it.
            backgroundScope.launch { model.uiState.collect { } }
            runCurrent()
        }
    }

    @Test
    fun `the only watch is chosen without asking`() = runTest {
        val watch = addGarmin("fēnix 7", "E0:48:24:D5:F7:10")

        val state = viewModel().uiState.value

        assertEquals(listOf(watch.id), state.watches.map { it.id })
        assertEquals(watch.id, state.selectedDeviceId)
    }

    @Test
    fun `several watches and none asked for - the user picks`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val second = addGarmin("Instinct 2", "E0:48:24:D5:F7:11")
        val model = viewModel(WATCH_POINT_LATITUDE_ARG to "48.8584", WATCH_POINT_LONGITUDE_ARG to "2.2945")

        assertNull(model.uiState.value.selectedDeviceId)
        assertFalse(model.uiState.value.canSend)

        model.selectWatch(second.id)
        runCurrent()
        assertEquals(second.id, model.uiState.value.selectedDeviceId)
        assertTrue(model.uiState.value.canSend)
    }

    @Test
    fun `the watch the screen was opened for is chosen`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val second = addGarmin("Instinct 2", "E0:48:24:D5:F7:11")

        val state = viewModel(WATCH_DEVICE_ID_ARG to second.id).uiState.value

        assertEquals(second.id, state.selectedDeviceId)
    }

    @Test
    fun `a watch that says it cannot store points is left out, an unknown one stays`() = runTest {
        val unable = addGarmin("vívosmart 5", "E0:48:24:D5:F7:10")
        val able = addGarmin("fēnix 7", "E0:48:24:D5:F7:11")
        val unknown = addGarmin("Instinct 2", "E0:48:24:D5:F7:12")
        stateStore.recordCapabilities(unable.id, setOf(GarminCapability.SYNC))
        stateStore.recordCapabilities(able.id, setOf(GarminCapability.SYNC, GarminCapability.WAYPOINT_TRANSFER))
        repo.addDevice(
            displayName = "Galaxy Watch",
            address = "A8:D1:62:BE:3A:3B",
            bluetoothName = "Galaxy Watch8",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )

        val state = viewModel().uiState.value

        assertEquals(setOf(able.id, unknown.id), state.watches.map { it.id }.toSet())
    }

    @Test
    fun `no watch at all is an empty list`() = runTest {
        val state = viewModel().uiState.value

        assertTrue(state.watches.isEmpty())
        assertFalse(state.canSend)
    }

    @Test
    fun `a shared position fills the form and is not sent`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")

        val model = viewModel(
            WATCH_POINT_LATITUDE_ARG to "-33.8568",
            WATCH_POINT_LONGITUDE_ARG to "151.2153",
            WATCH_POINT_NAME_ARG to "Opera House",
        )
        val state = model.uiState.value

        assertEquals("Opera House", state.name)
        assertEquals("-33.85680, 151.21530", state.coordinates)
        assertEquals(GeoCoordinateParseResult.Parsed(-33.8568, 151.2153), state.parsed)
        assertTrue(state.canSend)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `a share with no position says so until the user types`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val model = viewModel(WATCH_POINT_UNREADABLE_ARG to "true", WATCH_POINT_NAME_ARG to "Eiffel Tower")

        assertTrue(model.uiState.value.sharedUnreadable)
        assertNull(model.uiState.value.parsed)

        model.updateCoordinates("N 48° 51.504 E 002° 17.670")
        runCurrent()
        assertFalse(model.uiState.value.sharedUnreadable)
        assertTrue(model.uiState.value.canSend)
    }

    @Test
    fun `bad coordinates carry their reason and block the send`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val model = viewModel()

        model.updateCoordinates("91, 2")
        runCurrent()

        assertEquals(
            GeoCoordinateParseResult.Invalid(GeoCoordinateError.LATITUDE_RANGE),
            model.uiState.value.parsed,
        )
        assertFalse(model.uiState.value.canSend)
        model.send()
        runCurrent()
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `send hands the point over, locks the form, then shows the result`() = runTest {
        val watch = addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val model = viewModel()
        model.updateName("  Cache 1 ")
        model.updateCoordinates("48.8584, 2.2945")
        runCurrent()

        model.send()
        runCurrent()

        assertEquals(listOf(watch.id to GarminWaypoint("Cache 1", 48.8584, 2.2945)), sent)
        assertTrue(model.uiState.value.isSending)
        assertFalse(model.uiState.value.canSend)

        sendGate.complete(GarminSendFileResult.Sent)
        val done = model.uiState.first { !it.isSending }
        assertEquals(GarminSendFileResult.Sent, done.result)
    }

    @Test
    fun `a point with no name is named by its position`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val model = viewModel()
        model.updateCoordinates("S 33° 51.408 W 070° 38.898")
        runCurrent()

        model.send()
        runCurrent()

        assertEquals("-33.85680, -70.64830", sent.single().second.name)
    }

    @Test
    fun `editing the coordinates clears the last result`() = runTest {
        addGarmin("fēnix 7", "E0:48:24:D5:F7:10")
        val model = viewModel()
        model.updateCoordinates("48.8584, 2.2945")
        runCurrent()
        model.send()
        sendGate.complete(GarminSendFileResult.NoAnswer)
        model.uiState.first { it.result != null }

        model.updateCoordinates("48.8585, 2.2945")
        runCurrent()

        assertNull(model.uiState.value.result)
    }
}
