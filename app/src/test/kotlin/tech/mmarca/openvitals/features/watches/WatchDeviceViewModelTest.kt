package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

/** Renaming a watch with no capabilities, and what forgetting a device does and does not do at the OS level. */
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
    private val notificationsGateway = mockk<WatchNotificationsGateway>(relaxed = true)
    private val garminLocalData = mockk<tech.mmarca.openvitals.devices.garmin.GarminLocalData>(relaxed = true)
    private val healthConnectManager = mockk<tech.mmarca.openvitals.healthconnect.HealthConnectManager>(relaxed = true)
    private val notificationBridge = mockk<tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge>(relaxed = true)

    /** The scheduler owns the stored interval and the WorkManager side, so it is one stub that remembers. */
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
        notificationBridge = notificationBridge,
        realtimeStore = GarminRealtimeStore(),
        calendarSource = mockk(relaxed = true),
        navigationRelay = mockk(relaxed = true),
        coMapsNavigationRepository = mockk(relaxed = true),
        coMapsGuidanceFeed = tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed(mockk(relaxed = true)),
        agpsStore = mockk<GarminAgpsStore>(relaxed = true).also {
            every { it.agps } returns MutableStateFlow(GarminAgpsState())
        },
        onboardWearOsWatch = mockk<OnboardWearOsWatchUseCase>(relaxed = true),
        autoSyncScheduler = autoSyncScheduler,
        musicRelay = mockk(relaxed = true),
        notificationsGateway = notificationsGateway,
        garminLocalData = garminLocalData,
        healthConnectManager = healthConnectManager,
        // The cleanup after a removal runs on the default dispatcher. Here it runs at once.
        dispatchers = mainDispatcherRule.dispatcherProvider,
    )

    @Test
    fun `switching music controls on without notification access shows the disclosure first`() = runTest {
        val watch = addWatch()
        every { notificationsGateway.isNotificationAccessGranted() } returns false
        val vm = viewModel(watch.id)
        backgroundScope.launch { vm.uiState.collect { } }

        vm.setMusicControls(true)
        runCurrent()

        assertTrue(vm.uiState.value.showMusicDisclosure)
        verify(exactly = 0) { notificationBridge.onMusicControlsChanged(any(), any()) }
        verify(exactly = 0) { notificationsGateway.openNotificationAccessSettings() }
    }

    @Test
    fun `accepting the disclosure switches on and opens Android's settings, declining does neither`() = runTest {
        val watch = addWatch()
        every { notificationsGateway.isNotificationAccessGranted() } returns false
        val vm = viewModel(watch.id)
        backgroundScope.launch { vm.uiState.collect { } }

        vm.setMusicControls(true)
        vm.declineMusicDisclosure()
        runCurrent()
        assertFalse(vm.uiState.value.showMusicDisclosure)
        verify(exactly = 0) { notificationBridge.onMusicControlsChanged(any(), any()) }

        vm.setMusicControls(true)
        vm.acceptMusicDisclosure()
        runCurrent()
        assertFalse(vm.uiState.value.showMusicDisclosure)
        verify(exactly = 1) { notificationBridge.onMusicControlsChanged(watch.id, true) }
        verify(exactly = 1) { notificationsGateway.openNotificationAccessSettings() }
    }

    @Test
    fun `with access granted the switch applies at once, and off never asks`() = runTest {
        val watch = addWatch()
        every { notificationsGateway.isNotificationAccessGranted() } returns true
        val vm = viewModel(watch.id)

        vm.setMusicControls(true)
        every { notificationsGateway.isNotificationAccessGranted() } returns false
        vm.setMusicControls(false)

        verify(exactly = 1) { notificationBridge.onMusicControlsChanged(watch.id, true) }
        verify(exactly = 1) { notificationBridge.onMusicControlsChanged(watch.id, false) }
        verify(exactly = 0) { notificationsGateway.openNotificationAccessSettings() }
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
    fun `the screen learns when Health Connect's background access is missing`() = runTest {
        coEvery { healthConnectManager.isBackgroundReadGrantMissing() } returns true
        val watch = addWatch()
        val vm = viewModel(watch.id)
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
        assertFalse(vm.uiState.value.backgroundReadMissing)

        vm.refreshBackgroundAccess()
        runCurrent()
        assertTrue(vm.uiState.value.backgroundReadMissing)

        // After the permission screen returns with the grant, the notice goes away.
        coEvery { healthConnectManager.isBackgroundReadGrantMissing() } returns false
        vm.refreshBackgroundAccess()
        runCurrent()
        assertFalse(vm.uiState.value.backgroundReadMissing)
    }

    @Test
    fun `removing the last Garmin watch clears what only served a paired watch`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)
        assertTrue(vm.isLastGarminWatch)

        vm.removeDevice()

        // The user did not tick the box, so the watch-only history stays.
        coVerify(timeout = 2_000) { garminLocalData.clearAfterLastWatchRemoved(deleteWellnessHistory = false) }
    }

    @Test
    fun `the user can have the watch-only history deleted with the last watch`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)

        vm.removeDevice(deleteWatchHistory = true)

        coVerify(timeout = 2_000) { garminLocalData.clearAfterLastWatchRemoved(deleteWellnessHistory = true) }
    }

    @Test
    fun `removing one of two Garmin watches clears nothing the other still needs`() = runTest {
        val watch = addWatch()
        repo.addDevice(
            displayName = "Edge 540",
            address = "E0:48:24:00:00:01",
            bluetoothName = "Edge 540",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.GARMIN,
        )
        val vm = viewModel(watch.id)
        assertFalse(vm.isLastGarminWatch)

        vm.removeDevice(deleteWatchHistory = true)

        coVerify(exactly = 0) { garminLocalData.clearAfterLastWatchRemoved(any()) }
    }

    @Test
    fun `forgetting a watch also drops its bond, association and Garmin state`() = runTest {
        val watch = addWatch()
        stateStore.recordSyncedFileKeys(watch.id, listOf("128/49/1"))
        stateStore.recordCapabilities(watch.id, setOf(GarminCapability.SYNC))
        val vm = viewModel(watch.id)

        vm.removeDevice()

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

        assertTrue(repo.devices.isEmpty())
        assertTrue(pairing.snapshot().isEmpty())
    }

    @Test
    fun `automatic sync starts off and shows the interval that was picked`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)

        assertEquals(AutoSyncInterval.OFF, vm.uiState.value.autoSync)

        vm.setAutoSync(AutoSyncInterval.HOURLY)

        // Read back through the scheduler: the row must show what is actually scheduled.
        assertEquals(AutoSyncInterval.HOURLY, vm.uiState.value.autoSync)
        verify { autoSyncScheduler.setInterval(watch.id, AutoSyncInterval.HOURLY) }
    }

    @Test
    fun `forgetting a watch stops its automatic sync`() = runTest {
        val watch = addWatch()
        val vm = viewModel(watch.id)
        vm.setAutoSync(AutoSyncInterval.EVERY_30_MINUTES)

        vm.removeDevice()

        // A schedule outliving the watch would wake the radio for nothing.
        verify { autoSyncScheduler.forget(watch.id) }
    }
}
