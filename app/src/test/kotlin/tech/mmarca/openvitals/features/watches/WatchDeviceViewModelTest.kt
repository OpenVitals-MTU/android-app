package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminAgpsState
import tech.mmarca.openvitals.devices.garmin.GarminAgpsStore
import tech.mmarca.openvitals.devices.garmin.GarminRealtimeStore
import tech.mmarca.openvitals.devices.garmin.GarminGattReport
import tech.mmarca.openvitals.devices.garmin.GarminTransportProbe
import tech.mmarca.openvitals.devices.garmin.GarminTransportVariant
import tech.mmarca.openvitals.devices.garmin.OnboardGarminWatchUseCase
import tech.mmarca.openvitals.devices.wearos.OnboardWearOsWatchUseCase
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.navigation.WATCH_DEVICE_ID_ARG
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Port of the Flutter build's `watch_device_screen_test.dart` rename case and
 * of the forget branches of `ble_devices_view_model_test.dart`: renaming a
 * watch that has no capabilities at all, and what forgetting a device does —
 * and does NOT do — at the OS level.
 */
class WatchDeviceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePairing : WatchPairingPort {
        val calls = mutableListOf<String>()

        @Volatile
        var bondResult = WatchBondResult.BONDED

        override suspend fun bond(address: String): WatchBondResult = bondResult

        override suspend fun removeBond(address: String) {
            synchronized(calls) { calls.add("removeBond:$address") }
        }

        override suspend fun associateCompanion(address: String, displayName: String?) = true

        override suspend fun disassociateCompanion(address: String) {
            synchronized(calls) { calls.add("disassociate:$address") }
        }

        fun snapshot(): List<String> = synchronized(calls) { calls.toList() }
    }

    private class FakeProbe : GarminTransportProbe {
        override suspend fun probe(address: String): GarminGattReport = GarminGattReport(
            address = address,
            variant = GarminTransportVariant.V1,
            services = emptyList(),
        )
    }

    private lateinit var repo: BleDeviceRepository
    private lateinit var stateStore: GarminDeviceStateStore
    private lateinit var pairing: FakePairing

    /**
     * The scheduler owns both the stored interval and the `WorkManager` side,
     * so it is stubbed as one thing that remembers what it was told. The
     * view-model's whole job is to hand the choice over and read it back.
     */
    private lateinit var autoSyncScheduler: WatchAutoSyncScheduler
    private val autoSyncIntervals = mutableMapOf<String, AutoSyncInterval>()

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        stateStore = GarminDeviceStateStore(FakeSharedPreferences())
        pairing = FakePairing()
        autoSyncIntervals.clear()
        autoSyncScheduler = mockk(relaxed = true)
        every { autoSyncScheduler.interval(any()) } answers {
            autoSyncIntervals[firstArg()] ?: AutoSyncInterval.OFF
        }
        every { autoSyncScheduler.setInterval(any(), any()) } answers {
            autoSyncIntervals[firstArg()] = secondArg()
        }
    }

    private fun addWatch(): BleSensorDevice = repo.addDevice(
        displayName = "vívoactive 5",
        address = "E0:48:24:D5:F7:10",
        bluetoothName = "vívoactive 5",
        capabilities = emptySet(),
        kind = BleDeviceKind.WATCH,
        integration = DeviceIntegration.GARMIN,
    )

    private fun viewModel(deviceId: String) = WatchDeviceViewModel(
        savedStateHandle = SavedStateHandle(mapOf(WATCH_DEVICE_ID_ARG to deviceId)),
        deviceRepository = repo,
        stateStore = stateStore,
        syncController = mockk<DeviceSyncController>().also {
            every { it.state } returns MutableStateFlow(DeviceSyncUiState())
        },
        actionsController = mockk<GarminWatchActionsController>().also {
            every { it.state } returns MutableStateFlow(WatchFindUiState())
        },
        sensorCoordinator = mockk<BleSensorCoordinator>().also {
            every { it.discoveredDevices } returns MutableStateFlow(emptyList())
            every { it.stopScan() } just runs
        },
        onboardGarminWatch = OnboardGarminWatchUseCase(pairing, repo, FakeProbe()),
        notificationBridge = mockk(relaxed = true),
        realtimeStore = GarminRealtimeStore(),
        calendarSource = mockk(relaxed = true),
        navigationRelay = mockk(relaxed = true),
        preferencesRepository = mockk<tech.mmarca.openvitals.data.repository.PreferencesRepository>(relaxed = true).also {
            every { it.activityRecordingPreferences() } returns
                tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences()
        },
        agpsStore = mockk<GarminAgpsStore>(relaxed = true).also {
            every { it.agps } returns MutableStateFlow(GarminAgpsState())
        },
        onboardWearOsWatch = mockk<OnboardWearOsWatchUseCase>(relaxed = true),
        autoSyncScheduler = autoSyncScheduler,
    )

    /**
     * The OS-level cleanup is deliberately fire-and-forget on a scope that
     * outlives the screen, so the test waits for it rather than driving it.
     */
    private fun awaitPairingCalls(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (pairing.snapshot().size < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
    }

    @Test
    fun `renaming trims the new name and persists it`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)

        // Trimmed, because a name padded with spaces is a typo, not a choice.
        vm.rename("  Wrist watch  ")

        assertEquals("Wrist watch", repo.devices.single().displayName)
        assertEquals("Wrist watch", vm.uiState.value.device?.displayName)
    }

    @Test
    fun `a watch can be renamed even though it has no capabilities`() = runTest {
        val watch = addWatch()
        assertTrue(watch.capabilities.isEmpty())
        val vm = viewModel(watch.id)

        vm.rename("Running watch")

        // The sensor rule ("select at least one capability") must not fire here.
        assertEquals("Running watch", repo.devices.single().displayName)
        assertTrue(repo.devices.single().capabilities.isEmpty())
    }

    @Test
    fun `a blank rename is refused rather than blanking the row`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)

        vm.rename("   ")

        assertEquals("vívoactive 5", repo.devices.single().displayName)
    }

    @Test
    fun `forgetting a watch also drops its bond, association and Garmin state`() = runTest {
        val watch = addWatch()
        stateStore.recordSyncedFileKeys(watch.id, listOf("128/49/1"))
        stateStore.recordCapabilities(watch.id, setOf(GarminCapability.SYNC))
        val vm = viewModel(watch.id)

        vm.removeDevice()
        awaitPairingCalls(expected = 2)

        assertTrue(repo.devices.isEmpty())
        assertEquals(
            listOf(
                "disassociate:E0:48:24:D5:F7:10",
                "removeBond:E0:48:24:D5:F7:10",
            ),
            pairing.snapshot(),
        )
        // The registry does not hold Garmin state; the forget path clears it.
        assertTrue(stateStore.syncedFileKeys(watch.id).isEmpty())
        assertTrue(stateStore.capabilities(watch.id).isEmpty())
        assertNull(vm.uiState.value.device)
    }

    @Test
    fun `forgetting a sensor touches neither bond nor association`() = runTest {
        val sensor = repo.addDevice(
            displayName = "Chest strap",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Wahoo TICKR",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val vm = viewModel(sensor.id)

        vm.removeDevice()
        // Give a cleanup that must never happen the same chance to happen as
        // the one that must.
        Thread.sleep(100)

        assertTrue(repo.devices.isEmpty())
        assertTrue(pairing.snapshot().isEmpty())
    }

    @Test
    fun `automatic sync starts off and shows the interval that was picked`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)

        assertEquals(AutoSyncInterval.OFF, vm.uiState.value.autoSync)

        vm.setAutoSync(AutoSyncInterval.HOURLY)

        // Read back through the scheduler, not held in the screen: the stored
        // choice and the periodic work are the same fact, and the row must
        // show the one that is actually scheduled.
        assertEquals(AutoSyncInterval.HOURLY, vm.uiState.value.autoSync)
        verify { autoSyncScheduler.setInterval(watch.id, AutoSyncInterval.HOURLY) }
    }

    @Test
    fun `forgetting a watch stops its automatic sync`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)
        vm.setAutoSync(AutoSyncInterval.EVERY_30_MINUTES)

        vm.removeDevice()
        awaitPairingCalls(expected = 2)

        // A schedule outliving the watch would wake the radio to talk to
        // something the registry no longer knows about.
        verify { autoSyncScheduler.forget(watch.id) }
    }
}
