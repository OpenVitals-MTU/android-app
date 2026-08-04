package tech.mmarca.openvitals.features.settings

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Port of the sensor half of the Flutter build's
 * `ble_devices_view_model_test.dart`: the add flow (probe → fall back to the
 * advertisement → conflict check → save) and the edit flow, against a real
 * registry and a coordinator that never touches a radio.
 */
class BleDevicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: BleDeviceRepository
    private lateinit var coordinator: BleSensorCoordinator

    /** What the fake GATT probe answers with. */
    private var discoverResult: Set<BleSensorCapability> = emptySet()

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        coordinator = mockk<BleSensorCoordinator>().also { fake ->
            every { fake.discoveredDevices } returns MutableStateFlow(emptyList())
            every { fake.startScan(any()) } just runs
            every { fake.stopScan() } just runs
            coEvery { fake.discoverCapabilities(any()) } answers { discoverResult }
        }
    }

    private fun viewModel() = BleDevicesViewModel(repo, coordinator)

    /** The chest strap in the screenshots this feature was built from. */
    private fun discovered(
        suggested: Set<BleSensorCapability> = setOf(BleSensorCapability.HEART_RATE),
    ) = BleDiscoveredDevice(
        address = "AA:BB:CC:DD:EE:FF",
        name = "Wahoo TICKR",
        rssi = -50,
        suggestedCapabilities = suggested,
    )

    @Test
    fun `selecting a device auto-discovers capabilities via the GATT probe`() = runTest {
        discoverResult = setOf(
            BleSensorCapability.HEART_RATE,
            BleSensorCapability.CYCLING_POWER,
        )
        val vm = viewModel()

        vm.selectDiscoveredDevice(discovered())

        assertEquals(
            setOf(BleSensorCapability.HEART_RATE, BleSensorCapability.CYCLING_POWER),
            vm.uiState.value.addCapabilities,
        )
        assertFalse(vm.uiState.value.isDiscoveringCapabilities)
    }

    @Test
    fun `falls back to advertised capabilities when the probe finds none`() = runTest {
        discoverResult = emptySet()
        val vm = viewModel()

        vm.selectDiscoveredDevice(
            discovered(suggested = setOf(BleSensorCapability.RUNNING_SPEED_CADENCE)),
        )

        assertEquals(
            setOf(BleSensorCapability.RUNNING_SPEED_CADENCE),
            vm.uiState.value.addCapabilities,
        )
    }

    @Test
    fun `flags a capability conflict against an already-paired device`() = runTest {
        // Pre-pair an enabled HR strap that owns HEART_RATE.
        repo.addDevice(
            displayName = "Old strap",
            address = "11:22:33:44:55:66",
            bluetoothName = "Old strap",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        discoverResult = setOf(BleSensorCapability.HEART_RATE)
        val vm = viewModel()

        vm.selectDiscoveredDevice(discovered())

        val conflicts = vm.uiState.value.capabilityConflicts
        assertTrue(BleSensorCapability.HEART_RATE in conflicts)
        assertEquals(
            "11:22:33:44:55:66",
            conflicts[BleSensorCapability.HEART_RATE]?.address,
        )
    }

    @Test
    fun `saving a speed sensor persists the wheel circumference`() = runTest {
        discoverResult = setOf(BleSensorCapability.CYCLING_SPEED_DISTANCE)
        val vm = viewModel()
        vm.selectDiscoveredDevice(
            discovered(suggested = setOf(BleSensorCapability.CYCLING_SPEED_DISTANCE)),
        )
        vm.updateAddWheelCircumference("2200")

        vm.saveAddedDevice()

        val saved = repo.devices.single()
        assertEquals(setOf(BleSensorCapability.CYCLING_SPEED_DISTANCE), saved.capabilities)
        assertEquals(2200, saved.wheelCircumferenceMm)
        // Add flow closed after a successful save.
        assertFalse(vm.uiState.value.showAddFlow)
    }

    @Test
    fun `non-speed sensors are saved without a wheel circumference`() = runTest {
        discoverResult = setOf(BleSensorCapability.HEART_RATE)
        val vm = viewModel()
        vm.selectDiscoveredDevice(discovered())

        vm.saveAddedDevice()

        assertNull(repo.devices.single().wheelCircumferenceMm)
    }

    @Test
    fun `saving with no capabilities surfaces an error and does not persist`() = runTest {
        discoverResult = emptySet()
        val vm = viewModel()
        vm.selectDiscoveredDevice(discovered(suggested = emptySet()))

        vm.saveAddedDevice()

        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(repo.devices.isEmpty())
    }

    @Test
    fun `toggling a capability recomputes conflicts`() = runTest {
        repo.addDevice(
            displayName = "Power meter",
            address = "11:22:33:44:55:66",
            bluetoothName = "Power meter",
            capabilities = setOf(BleSensorCapability.CYCLING_POWER),
        )
        discoverResult = setOf(BleSensorCapability.HEART_RATE)
        val vm = viewModel()
        vm.selectDiscoveredDevice(discovered())
        assertTrue(vm.uiState.value.capabilityConflicts.isEmpty())

        vm.toggleAddCapability(BleSensorCapability.CYCLING_POWER)

        assertTrue(BleSensorCapability.CYCLING_POWER in vm.uiState.value.capabilityConflicts)
        assertTrue(BleSensorCapability.CYCLING_POWER in vm.uiState.value.addCapabilities)
    }

    @Test
    fun `edit flow loads the device and saves changes`() = runTest {
        val device = repo.addDevice(
            displayName = "Strap",
            address = "11:22:33:44:55:66",
            bluetoothName = "Strap",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val vm = viewModel()

        vm.openEditDevice(device.id)
        assertEquals("Strap", vm.uiState.value.editDisplayName)
        assertEquals(setOf(BleSensorCapability.HEART_RATE), vm.uiState.value.editCapabilities)

        vm.updateEditDisplayName("My chest strap")
        vm.setEditEnabled(false)
        vm.saveEditedDevice()

        val updated = repo.devices.single()
        assertEquals("My chest strap", updated.displayName)
        assertFalse(updated.enabled)
        assertNull(vm.uiState.value.editingDeviceId)
    }

    @Test
    fun `removing the edited device closes the edit flow`() = runTest {
        val device = repo.addDevice(
            displayName = "Strap",
            address = "11:22:33:44:55:66",
            bluetoothName = "Strap",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val vm = viewModel()
        vm.openEditDevice(device.id)

        vm.removeDevice(device.id)

        assertTrue(repo.devices.isEmpty())
        assertNull(vm.uiState.value.editingDeviceId)
    }

    @Test
    fun `enabledDeviceCount counts only the enabled devices`() = runTest {
        repo.addDevice(
            displayName = "On",
            address = "AA:AA:AA:AA:AA:AA",
            bluetoothName = "On",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val off = repo.addDevice(
            displayName = "Off",
            address = "BB:BB:BB:BB:BB:BB",
            bluetoothName = "Off",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        val vm = viewModel()

        vm.setDeviceEnabled(off.id, false)

        assertEquals(2, vm.uiState.value.devices.size)
        assertEquals(1, vm.uiState.value.enabledDeviceCount)
    }
}
