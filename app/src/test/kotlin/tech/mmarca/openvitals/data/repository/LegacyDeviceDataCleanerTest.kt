package tech.mmarca.openvitals.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.util.FakeSharedPreferences
import tech.mmarca.openvitals.util.MainDispatcherRule

class LegacyDeviceDataCleanerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk {
        every { getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        every { deleteSharedPreferences(any()) } returns true
    }
    private val registry = BleDeviceRepository(context)
    private val cleaner = LegacyDeviceDataCleaner(context, registry, mainDispatcherRule.dispatcherProvider)

    @Test
    fun `deletes every preference file the removed stores used`() = runTest {
        cleaner.clean()

        LegacyDeviceDataCleaner.LegacyPrefsFiles.forEach { file ->
            verify(exactly = 1) { context.deleteSharedPreferences(file) }
        }
        // The sensor registry and the app's own preferences are not among them.
        verify(exactly = 0) { context.deleteSharedPreferences("ble_sensor_devices") }
        verify(exactly = 0) { context.deleteSharedPreferences("openvitals_prefs") }
    }

    @Test
    fun `forgets the watch and the bike computer but keeps the strap`() = runTest {
        val strap = registry.addDevice(
            displayName = "Strap",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Strap",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )
        registry.addDevice(
            displayName = "vívoactive 5",
            address = "E0:48:24:D5:F7:10",
            bluetoothName = "vívoactive 5",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )
        registry.addDevice(
            displayName = "Edge 840",
            address = "E0:48:24:D5:F7:20",
            bluetoothName = "Edge 840",
            capabilities = setOf(BleSensorCapability.CYCLING_POWER),
            kind = BleDeviceKind.BIKE_COMPUTER,
            integration = DeviceIntegration.GARMIN,
        )

        cleaner.clean()

        assertEquals(listOf(strap.id), registry.devices.map { it.id })
    }

    @Test
    fun `running twice is harmless`() = runTest {
        cleaner.clean()
        cleaner.clean()

        assertEquals(emptyList<String>(), registry.devices.map { it.id })
    }
}
