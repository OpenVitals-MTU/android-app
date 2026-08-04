package tech.mmarca.openvitals.devices.core

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * The ownership-helper matrix per device shape: which of
 * isGarminWatch/isWearosWatch/isWatch/isBikeComputer/isGarminGfdi/
 * isLiveSensorCapable holds for an explicit Garmin watch, a plain sensor and
 * an Edge bike computer.
 */
class DeviceIntegrationParityTest {

    private fun watch(integration: DeviceIntegration? = null): BleSensorDevice =
        BleSensorDevice(
            id = "w",
            displayName = "w",
            address = "AA",
            bluetoothName = "w",
            capabilities = emptySet(),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-01-01T00:00:00Z"),
            kind = BleDeviceKind.WATCH,
            integration = integration,
        )

    @Test
    fun `an explicit Garmin watch is a Garmin watch`() {
        val w = watch(integration = DeviceIntegration.GARMIN)
        assertTrue(w.isGarminWatch)
        assertFalse(w.isWearosWatch)
    }

    @Test
    fun `a sensor is neither, whatever the integration`() {
        val sensor = BleSensorDevice(
            id = "s",
            displayName = "s",
            address = "BB",
            bluetoothName = "s",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        assertFalse(sensor.isGarminWatch)
        assertFalse(sensor.isWearosWatch)
        assertFalse(sensor.isBikeComputer)
        assertFalse(sensor.isGarminGfdi)
        assertTrue(sensor.isLiveSensorCapable)
    }

    @Test
    fun `an Edge bike computer - GFDI + live-sensor, but never a watch`() {
        val edge = BleSensorDevice(
            id = "e",
            displayName = "Edge 840",
            address = "CC",
            bluetoothName = "Edge 840",
            capabilities = emptySet(),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-01-01T00:00:00Z"),
            kind = BleDeviceKind.BIKE_COMPUTER,
            integration = DeviceIntegration.GARMIN,
        )
        assertTrue(edge.isBikeComputer)
        assertTrue("pulls FIT files over GFDI", edge.isGarminGfdi)
        assertTrue("can broadcast live", edge.isLiveSensorCapable)
        assertFalse(edge.isWatch)
        assertFalse(edge.isGarminWatch)
        assertFalse(edge.isWearosWatch)
    }
}
