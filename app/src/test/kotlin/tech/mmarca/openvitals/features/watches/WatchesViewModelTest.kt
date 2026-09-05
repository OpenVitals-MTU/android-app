package tech.mmarca.openvitals.features.watches

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.permissions.OsPermissionsService
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.devices.garmin.GarminGattReport
import tech.mmarca.openvitals.devices.garmin.GarminTransportProbe
import tech.mmarca.openvitals.devices.garmin.GarminTransportVariant
import tech.mmarca.openvitals.devices.garmin.OnboardGarminWatchUseCase
import tech.mmarca.openvitals.devices.wearos.OnboardWearOsWatchUseCase
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.domain.model.OsPermissionRow
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * The sheet-level flow: no capability probe for a GFDI device, what a refused pairing leaves
 * on screen, and the no-companion notice's lifetime. Registry writes are `OnboardGarminWatchUseCaseTest`.
 */
class WatchesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Records what onboarding asked the platform to do; answers as set up. */
    private class FakePairing : WatchPairingPort {
        var bondResult = WatchBondResult.BONDED
        var associateResult = true
        val calls = mutableListOf<String>()

        override suspend fun bond(address: String): WatchBondResult {
            calls.add("bond:$address")
            return bondResult
        }

        override suspend fun removeBond(address: String) {
            calls.add("removeBond:$address")
        }

        override suspend fun associateCompanion(address: String, displayName: String?): Boolean {
            calls.add("associate:$address")
            return associateResult
        }

        override suspend fun disassociateCompanion(address: String) {
            calls.add("disassociate:$address")
        }
    }

    /** Canned GATT verdict; never opens a connection. */
    private class FakeProbe : GarminTransportProbe {
        var variant = GarminTransportVariant.V1

        override suspend fun probe(address: String): GarminGattReport = GarminGattReport(
            address = address,
            variant = variant,
            services = emptyList(),
        )
    }

    private lateinit var repo: BleDeviceRepository
    private lateinit var pairing: FakePairing
    private lateinit var probe: FakeProbe
    private lateinit var coordinator: BleSensorCoordinator

    /** How the coordinator classifies whatever the test selects. */
    private var classification = DeviceClassification(
        integration = DeviceIntegration.GARMIN,
        kind = BleDeviceKind.WATCH,
    )

    /** How many times a capability probe was asked for. */
    private var capabilityProbes = 0

    private val watch = BleDiscoveredDevice(
        address = "E0:48:24:D5:F7:10",
        name = "vívoactive 5",
        rssi = -55,
        suggestedCapabilities = emptySet(),
        advertisesSyncService = true,
    )

    /** A Garmin Edge: GFDI like a watch, but its own kind. */
    private val bikeComputer = BleDiscoveredDevice(
        address = "E0:48:24:D5:F7:20",
        name = "Edge 840",
        rssi = -55,
        suggestedCapabilities = emptySet(),
        advertisesSyncService = true,
    )

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        pairing = FakePairing()
        probe = FakeProbe()
        coordinator = mockk<BleSensorCoordinator>().also { fake ->
            every { fake.discoveredDevices } returns MutableStateFlow(emptyList())
            every { fake.startScan(any()) } just runs
            every { fake.stopScan() } just runs
            every { fake.classifyDiscoveredDevice(any()) } answers { classification }
            io.mockk.coEvery { fake.discoverCapabilities(any()) } answers {
                capabilityProbes++
                emptySet()
            }
        }
    }

    /** Granted by default, so pairing tests are not diverted into the checklist. */
    private fun osPermissions(
        catalog: OsPermissionCatalog = OsPermissionCatalog(
            rows = listOf(
                OsPermissionRow(OsPermissionId.BLUETOOTH, listOf("scan", "connect"), granted = true),
            ),
        ),
    ): OsPermissionsService = mockk<OsPermissionsService>(relaxed = true).also { service ->
        every { service.watchSetupCatalog() } returns catalog
    }

    private fun viewModel(
        os: OsPermissionsService = osPermissions(),
    ) = WatchesViewModel(
        deviceRepository = repo,
        sensorCoordinator = coordinator,
        onboardGarminWatch = OnboardGarminWatchUseCase(pairing, repo, probe),
        onboardWearOsWatch = mockk<OnboardWearOsWatchUseCase>(relaxed = true),
        osPermissionsService = os,
    )

    // Android's own permissions, asked when the first watch is added.

    private fun osRow(id: OsPermissionId, granted: Boolean, vararg permissions: String) =
        OsPermissionRow(id = id, permissions = permissions.toList(), granted = granted)

    @Test
    fun `adding a watch with everything granted goes straight to the scan`() = runTest {
        val vm = viewModel()

        vm.startAdd()

        assertTrue(vm.uiState.value.showAddFlow)
        assertFalse(vm.uiState.value.showPermissionsGate)
    }

    @Test
    fun `an outstanding permission shows the checklist instead of scanning`() = runTest {
        val vm = viewModel(
            os = osPermissions(
                OsPermissionCatalog(
                    rows = listOf(
                        osRow(OsPermissionId.BLUETOOTH, granted = false, "scan", "connect"),
                        osRow(OsPermissionId.NOTIFICATIONS, granted = true, "post"),
                        osRow(OsPermissionId.BATTERY_OPTIMIZATION, granted = false),
                    ),
                ),
            ),
        )

        vm.startAdd()

        // Scanning without Bluetooth finds nothing, so the ask comes first.
        assertTrue(vm.uiState.value.showPermissionsGate)
        assertFalse(vm.uiState.value.showAddFlow)
        // One dialog for what the runtime can ask; the granted row adds nothing.
        assertEquals(listOf("scan", "connect"), vm.uiState.value.osPermissions.requestablePermissions)
        // Settings screens cannot be batched, so they queue instead.
        assertEquals(
            listOf(OsPermissionId.BATTERY_OPTIMIZATION),
            vm.uiState.value.osPermissions.outstandingSpecials,
        )
    }

    @Test
    fun `continuing anyway leaves the checklist for the scan`() = runTest {
        val vm = viewModel(
            os = osPermissions(
                OsPermissionCatalog(rows = listOf(osRow(OsPermissionId.NOTIFICATION_FORWARDING, false))),
            ),
        )
        vm.startAdd()

        vm.openAddFlow()

        // Only Bluetooth blocks pairing; a refused extra must not trap the user.
        assertFalse(vm.uiState.value.showPermissionsGate)
        assertTrue(vm.uiState.value.showAddFlow)
    }

    @Test
    fun `the queue opens one settings screen per call`() = runTest {
        val service = osPermissions(
            OsPermissionCatalog(
                rows = listOf(
                    osRow(OsPermissionId.BATTERY_OPTIMIZATION, granted = false),
                    osRow(OsPermissionId.NOTIFICATION_FORWARDING, granted = false),
                ),
            ),
        )
        every { service.openSettingsFor(any()) } returns true
        val vm = viewModel(os = service)
        vm.startAdd()

        vm.queueAllSpecialPermissions()
        vm.openNextSpecialPermission()

        // Only the first: the app leaves the foreground to show it, so a second would never be seen.
        verify(exactly = 1) { service.openSettingsFor(OsPermissionId.BATTERY_OPTIMIZATION) }
        verify(exactly = 0) { service.openSettingsFor(OsPermissionId.NOTIFICATION_FORWARDING) }

        // Coming back from that screen hands over the next one.
        vm.openNextSpecialPermission()
        verify(exactly = 1) { service.openSettingsFor(OsPermissionId.NOTIFICATION_FORWARDING) }
        assertTrue(vm.uiState.value.pendingSpecialPermissions.isEmpty())
    }

    @Test
    fun `a queued permission granted elsewhere is dropped, not reopened`() = runTest {
        val service = osPermissions(
            OsPermissionCatalog(
                rows = listOf(
                    osRow(OsPermissionId.BATTERY_OPTIMIZATION, granted = false),
                    osRow(OsPermissionId.NOTIFICATION_FORWARDING, granted = false),
                ),
            ),
        )
        every { service.openSettingsFor(any()) } returns true
        val vm = viewModel(os = service)
        vm.startAdd()
        vm.queueAllSpecialPermissions()

        // The user grants the battery one before the queue reaches it.
        every { service.watchSetupCatalog() } returns OsPermissionCatalog(
            rows = listOf(
                osRow(OsPermissionId.BATTERY_OPTIMIZATION, granted = true),
                osRow(OsPermissionId.NOTIFICATION_FORWARDING, granted = false),
            ),
        )
        vm.openNextSpecialPermission()

        // A settled screen is skipped.
        verify(exactly = 0) { service.openSettingsFor(OsPermissionId.BATTERY_OPTIMIZATION) }
        verify(exactly = 1) { service.openSettingsFor(OsPermissionId.NOTIFICATION_FORWARDING) }
    }

    @Test
    fun `an empty queue opens nothing`() = runTest {
        val service = osPermissions(
            OsPermissionCatalog(rows = listOf(osRow(OsPermissionId.BATTERY_OPTIMIZATION, false))),
        )
        val vm = viewModel(os = service)

        // Every resume calls this; outside a walk there is nothing to open.
        vm.openNextSpecialPermission()

        verify(exactly = 0) { service.openSettingsFor(any()) }
    }

    @Test
    fun `selecting a watch skips the capability probe entirely`() = runTest {
        val vm = viewModel()

        vm.selectDiscoveredDevice(watch)

        // The sheet asks the user to bond it, not to pick capabilities.
        assertEquals(0, capabilityProbes)
        assertEquals(DeviceIntegration.GARMIN, vm.uiState.value.addingIntegration)
        assertEquals(BleDeviceKind.WATCH, vm.uiState.value.selectedClassification?.kind)
        assertEquals("vívoactive 5", vm.uiState.value.addDisplayName)
    }

    @Test
    fun `selecting an Edge skips the probe like a watch`() = runTest {
        classification = DeviceClassification(
            integration = DeviceIntegration.GARMIN,
            kind = BleDeviceKind.BIKE_COMPUTER,
        )
        val vm = viewModel()

        vm.selectDiscoveredDevice(bikeComputer)

        assertEquals(0, capabilityProbes)
        assertEquals(BleDeviceKind.BIKE_COMPUTER, vm.uiState.value.selectedClassification?.kind)
        assertEquals("Edge 840", vm.uiState.value.addDisplayName)
    }

    @Test
    fun `onboarding registers the watch and closes the sheet`() = runTest {
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(watch)

        var closed: Boolean? = null
        vm.onboardSelectedWatch { closed = it }

        assertEquals(true, closed)
        assertEquals(1, repo.devices.size)
        assertEquals(BleDeviceKind.WATCH, repo.devices.single().kind)
        assertFalse(vm.uiState.value.isOnboarding)
        assertNull(vm.uiState.value.onboardStep)
        assertFalse(vm.uiState.value.showAddFlow)
    }

    @Test
    fun `onboarding an Edge registers it as a bike computer`() = runTest {
        classification = DeviceClassification(
            integration = DeviceIntegration.GARMIN,
            kind = BleDeviceKind.BIKE_COMPUTER,
        )
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(bikeComputer)

        vm.onboardSelectedWatch()

        val registered = repo.devices.single()
        assertEquals(BleDeviceKind.BIKE_COMPUTER, registered.kind)
        assertTrue(registered.isBikeComputer)
        assertTrue(registered.isGarminGfdi)
        assertTrue(registered.capabilities.isEmpty())
    }

    @Test
    fun `a refused pairing keeps the sheet open and explains why`() = runTest {
        pairing.bondResult = WatchBondResult.REFUSED
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(watch)

        var closed: Boolean? = null
        vm.onboardSelectedWatch { closed = it }

        assertEquals(false, closed)
        assertTrue(repo.devices.isEmpty())
        // The sheet must survive: re-scanning to retry a mistyped code is a pointless round trip.
        assertNotNull(vm.uiState.value.selectedDevice)
        assertTrue(vm.uiState.value.showAddFlow)
        assertFalse(vm.uiState.value.isOnboarding)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `a declined companion association is recorded, not failed`() = runTest {
        pairing.associateResult = false
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(watch)

        var closed: Boolean? = null
        vm.onboardSelectedWatch { closed = it }

        assertEquals(true, closed)
        assertEquals(1, repo.devices.size)
        assertEquals(WatchOnboardNotice.NO_COMPANION, vm.uiState.value.onboardNotice)
    }

    @Test
    fun `the no-companion flag survives the sheet closing, then clears`() = runTest {
        pairing.associateResult = false
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(watch)

        vm.onboardSelectedWatch()

        // The screen reads the notice after the sheet pops, so closing must not consume it.
        assertFalse(vm.uiState.value.showAddFlow)
        assertEquals(WatchOnboardNotice.NO_COMPANION, vm.uiState.value.onboardNotice)

        // Starting a fresh add is what clears it, so the notice fires once.
        vm.openAddFlow()
        assertNull(vm.uiState.value.onboardNotice)
    }

    @Test
    fun `a blank name falls back to the advertised one`() = runTest {
        val vm = viewModel()
        vm.openAddFlow()
        vm.selectDiscoveredDevice(watch)
        vm.updateAddDisplayName("   ")

        vm.onboardSelectedWatch()

        assertEquals("vívoactive 5", repo.devices.single().displayName)
    }
}
