package tech.mmarca.openvitals.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertFalse(device.isWatch)
        assertFalse(device.isGarminGfdi)
        assertTrue(device.isLiveSensorCapable)
        // A sensor is neither kind of watch, nor a bike computer, whatever
        // integration it is stamped with.
        assertFalse(device.isGarminWatch)
        assertFalse(device.isWearosWatch)
        assertFalse(device.isBikeComputer)
        val stamped = device(integration = DeviceIntegration.GARMIN)
        assertFalse(stamped.isGarminWatch)
        assertFalse(stamped.isWearosWatch)
        assertFalse(stamped.isBikeComputer)
    }

    @Test
    fun `an explicit Garmin watch is a Garmin watch`() {
        val watch = device(kind = BleDeviceKind.WATCH, integration = DeviceIntegration.GARMIN)
        assertTrue(watch.isGarminWatch)
        assertFalse(watch.isWearosWatch)
        // A watch pulls FIT files over GFDI and streams nothing live.
        assertTrue(watch.isGarminGfdi)
        assertFalse(watch.isLiveSensorCapable)
    }

    @Test
    fun `a null-integration watch is legacy Garmin`() {
        // A Garmin watch stored before the integration field existed — it must
        // keep reading as Garmin, the only watch integration that existed then.
        val watch = device(kind = BleDeviceKind.WATCH)
        assertTrue(watch.isWatch)
        assertTrue(watch.isGarminWatch)
        assertTrue(watch.isGarminGfdi)
        assertFalse(watch.isWearosWatch)
        assertFalse(watch.isLiveSensorCapable)
    }

    @Test
    fun `a wearos watch is a watch but never on the Garmin sync path`() {
        val watch = device(kind = BleDeviceKind.WATCH, integration = DeviceIntegration.WEAROS)
        assertTrue(watch.isWatch)
        assertTrue(watch.isWearosWatch)
        assertFalse(watch.isGarminWatch)
        assertFalse(watch.isGarminGfdi)
        assertFalse(watch.isLiveSensorCapable)
    }

    @Test
    fun `a bike computer syncs over GFDI and can still be a live sensor`() {
        val edge = device(kind = BleDeviceKind.BIKE_COMPUTER, integration = DeviceIntegration.GARMIN)
        assertFalse(edge.isWatch)
        assertTrue(edge.isBikeComputer)
        assertTrue(edge.isGarminGfdi)
        assertFalse(edge.isGarminWatch)
        assertFalse(edge.isWearosWatch)
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
