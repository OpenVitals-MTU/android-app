package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The watch integration is retired, but its storage vocabulary lives on as
 * parsing tolerance: migrated Flutter registries (and Kotlin watch-era ones)
 * contain `WATCH`/`GARMIN`/`WEAROS` entries that must keep decoding losslessly
 * and stay out of everything live-sensor-shaped.
 */
class BleDeviceKindTest {

    @Test
    fun `storage names match what the Flutter build persisted`() {
        assertEquals("SENSOR", BleDeviceKind.SENSOR.storageName)
        assertEquals("WATCH", BleDeviceKind.WATCH.storageName)
        assertEquals("BIKE_COMPUTER", BleDeviceKind.BIKE_COMPUTER.storageName)
        assertEquals("GARMIN", DeviceIntegration.GARMIN.storageName)
        assertEquals("WEAROS", DeviceIntegration.WEAROS.storageName)
    }

    @Test
    fun `fromStorage resolves known names and rejects unknown ones`() {
        assertEquals(BleDeviceKind.WATCH, BleDeviceKind.fromStorage("WATCH"))
        assertNull(BleDeviceKind.fromStorage("HOLOGRAPH"))
        assertEquals(DeviceIntegration.WEAROS, DeviceIntegration.fromStorage("WEAROS"))
        assertNull(DeviceIntegration.fromStorage(""))
    }

    @Test
    fun `a default device is a plain live sensor`() {
        val device = device()
        assertEquals(BleDeviceKind.SENSOR, device.kind)
        assertNull(device.integration)
        assertNull(device.lastSyncedAt)
        assertTrue(device.isLiveSensorCapable)
        // A stale integration stamp alone changes nothing.
        assertTrue(device(integration = DeviceIntegration.GARMIN).isLiveSensorCapable)
    }

    @Test
    fun `a stored watch-era entry is never live-sensor capable`() {
        assertFalse(device(kind = BleDeviceKind.WATCH).isLiveSensorCapable)
        assertFalse(
            device(kind = BleDeviceKind.WATCH, integration = DeviceIntegration.GARMIN)
                .isLiveSensorCapable,
        )
        assertFalse(
            device(kind = BleDeviceKind.WATCH, integration = DeviceIntegration.WEAROS)
                .isLiveSensorCapable,
        )
    }

    @Test
    fun `a bike computer can still be a live sensor`() {
        val edge = device(kind = BleDeviceKind.BIKE_COMPUTER, integration = DeviceIntegration.GARMIN)
        assertTrue(edge.isLiveSensorCapable)
    }

    private fun device(
        kind: BleDeviceKind = BleDeviceKind.SENSOR,
        integration: DeviceIntegration? = null,
    ): BleSensorDevice = BleSensorDevice(
        id = "id",
        displayName = "Device",
        address = "AA:BB:CC:DD:EE:01",
        bluetoothName = null,
        capabilities = emptySet(),
        enabled = true,
        wheelCircumferenceMm = null,
        addedAt = Instant.EPOCH,
        kind = kind,
        integration = integration,
    )
}
