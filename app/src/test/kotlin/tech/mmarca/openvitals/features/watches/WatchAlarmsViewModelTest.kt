package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
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
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.garmin.GarminAlarm
import tech.mmarca.openvitals.devices.garmin.GarminAlarmsFile
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminSendFileResult
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The list lives on the phone, and nothing reaches the watch until Send. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WatchAlarmsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: BleDeviceRepository
    private lateinit var stateStore: GarminDeviceStateStore
    private lateinit var watch: BleSensorDevice
    private val sent = mutableListOf<List<GarminAlarm>>()
    private val sendGate = CompletableDeferred<GarminSendFileResult>()

    private val syncPort = object : DeviceSyncPort {
        override fun canSync(device: BleSensorDevice): Boolean = true

        override suspend fun sync(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): DeviceSyncResult = DeviceSyncResult.Succeeded(0)
    }

    private val morning = GarminAlarm(hour = 6, minute = 30, days = setOf(DayOfWeek.MONDAY))
    private val evening = GarminAlarm(hour = 22, minute = 0)

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        stateStore = GarminDeviceStateStore(FakeSharedPreferences())
        watch = repo.addDevice(
            displayName = "Instinct",
            address = "E0:48:24:D5:F7:10",
            bluetoothName = "Instinct",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.GARMIN,
        )
    }

    private fun TestScope.viewModel(): WatchAlarmsViewModel {
        val syncController = DeviceSyncController(repo, syncPort, backgroundScope)
        val controller = GarminWatchActionsController(
            deviceRepository = repo,
            syncController = syncController,
            findWatch = { _, _ -> true },
            sendPoint = { _, _, _ -> GarminSendFileResult.Sent },
            // As the real one: an accepted list is recorded before the result shows.
            sendAlarms = { device, alarms, _ ->
                sent += alarms
                sendGate.await().also {
                    if (it == GarminSendFileResult.Sent) stateStore.recordSentAlarms(device.id, alarms)
                }
            },
            scope = backgroundScope,
        )
        return WatchAlarmsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(WATCH_DEVICE_ID_ARG to watch.id)),
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
    fun `a new watch has no alarms and nothing to send`() = runTest {
        val state = viewModel().uiState.value

        assertEquals(watch.id, state.device?.id)
        assertTrue(state.alarms.isEmpty())
        assertFalse(state.unsent)
        assertFalse(state.canSend)
        assertTrue(state.canAdd)
    }

    @Test
    fun `saved alarms are stored, kept in time order, and wait for a send`() = runTest {
        val model = viewModel()

        model.save(index = null, alarm = evening)
        model.save(index = null, alarm = morning)
        runCurrent()

        val state = model.uiState.value
        assertEquals(listOf(morning, evening), state.alarms)
        assertEquals(listOf(morning, evening), stateStore.alarms(watch.id))
        assertTrue(state.unsent)
        assertTrue(state.canSend)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `editing replaces the alarm and switching it off keeps the rest`() = runTest {
        val model = viewModel()
        model.save(index = null, alarm = morning)
        model.save(index = null, alarm = evening)

        model.save(index = 0, alarm = morning.copy(hour = 23))
        model.setEnabled(index = 0, enabled = false)
        runCurrent()

        // 23:30 now sorts after 22:00.
        assertEquals(
            listOf(evening.copy(enabled = false), morning.copy(hour = 23)),
            model.uiState.value.alarms,
        )
    }

    @Test
    fun `a send carries the whole list and a sent list is no longer unsent`() = runTest {
        val model = viewModel()
        model.save(index = null, alarm = morning)
        runCurrent()

        model.send()
        runCurrent()
        assertEquals(listOf(listOf(morning)), sent)
        assertTrue(model.uiState.value.isSending)
        assertFalse(model.uiState.value.canSend)

        sendGate.complete(GarminSendFileResult.Sent)
        runCurrent()

        val state = model.uiState.value
        assertEquals(GarminSendFileResult.Sent, state.result)
        assertFalse(state.unsent)
        assertFalse(state.canSend)
    }

    @Test
    fun `a failed send leaves the list unsent`() = runTest {
        val model = viewModel()
        model.save(index = null, alarm = morning)
        runCurrent()

        model.send()
        runCurrent()
        sendGate.complete(GarminSendFileResult.NoAnswer)
        runCurrent()

        val state = model.uiState.value
        assertEquals(GarminSendFileResult.NoAnswer, state.result)
        assertTrue(state.unsent)
        assertTrue(state.canSend)
    }

    @Test
    fun `deleting the last sent alarm is a change to send`() = runTest {
        stateStore.setAlarms(watch.id, listOf(morning))
        stateStore.recordSentAlarms(watch.id, listOf(morning))
        val model = viewModel()
        assertFalse(model.uiState.value.unsent)

        model.delete(index = 0)
        runCurrent()

        assertTrue(model.uiState.value.alarms.isEmpty())
        assertTrue(model.uiState.value.canSend)
    }

    @Test
    fun `an edit clears the last result`() = runTest {
        val model = viewModel()
        model.save(index = null, alarm = morning)
        runCurrent()
        model.send()
        runCurrent()
        sendGate.complete(GarminSendFileResult.Sent)
        runCurrent()

        model.save(index = null, alarm = evening)
        runCurrent()

        assertNull(model.uiState.value.result)
        assertTrue(model.uiState.value.unsent)
    }

    @Test
    fun `the list stops at what the watch holds`() = runTest {
        val model = viewModel()
        repeat(GarminAlarmsFile.MaxAlarms + 1) { model.save(index = null, alarm = GarminAlarm(hour = it, minute = 0)) }
        runCurrent()

        assertEquals(GarminAlarmsFile.MaxAlarms, model.uiState.value.alarms.size)
        assertFalse(model.uiState.value.canAdd)
    }

    @Test
    fun `a watch that is gone shows no device and cannot send`() = runTest {
        stateStore.setAlarms(watch.id, listOf(morning))
        repo.removeDevice(watch.id)

        val state = viewModel().uiState.value

        assertNull(state.device)
        assertFalse(state.canSend)
    }
}
