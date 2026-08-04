package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/**
 * Direct unit tests for the sensor-status mapping the dashboard's top-bar
 * battery action reads, mirroring Flutter's `dashboard_sensor_status_test.dart`.
 * The mapping is a pure list-to-list function, so it is exercised here rather
 * than through the ViewModel's device/metrics flows.
 */
class DashboardSensorStatusTest {

    private fun device(
        id: String,
        address: String = "AA:BB:CC:DD:EE:FF",
        enabled: Boolean = true,
        batteryPercent: Int? = null,
        kind: BleDeviceKind = BleDeviceKind.SENSOR,
    ) = BleSensorDevice(
        id = id,
        displayName = "Sensor $id",
        address = address,
        bluetoothName = null,
        capabilities = setOf(BleSensorCapability.HEART_RATE),
        enabled = enabled,
        wheelCircumferenceMm = null,
        batteryPercent = batteryPercent,
        addedAt = Instant.EPOCH,
        kind = kind,
    )

    private fun status(
        deviceId: String,
        address: String = "AA:BB:CC:DD:EE:FF",
        status: BleConnectionStatus = BleConnectionStatus.CONNECTED,
        batteryPercent: Int? = null,
    ) = BleDeviceConnectionStatus(
        deviceId = deviceId,
        displayName = "Sensor $deviceId",
        address = address,
        status = status,
        capabilities = setOf(BleSensorCapability.HEART_RATE),
        batteryPercent = batteryPercent,
    )

    // ─── toDashboardSensorStatus ──────────────────────────────────────────────

    @Test
    fun `the live battery wins over the persisted one`() {
        val result = listOf(device(id = "a", batteryPercent = 90))
            .toDashboardSensorStatus(listOf(status(deviceId = "a", batteryPercent = 42)))

        assertEquals(42, result.devices.single().batteryPercent)
        assertEquals(BleConnectionStatus.CONNECTED, result.devices.single().connectionStatus)
    }

    @Test
    fun `the persisted battery is the fallback when no live one is reported`() {
        // A live status that simply never carried a battery reading — not the
        // easier "no live status at all" case.
        val result = listOf(device(id = "a", batteryPercent = 90))
            .toDashboardSensorStatus(listOf(status(deviceId = "a")))

        assertEquals(90, result.devices.single().batteryPercent)
        assertEquals(BleConnectionStatus.CONNECTED, result.devices.single().connectionStatus)
    }

    @Test
    fun `the lookup falls back from device id to address`() {
        // The live status was keyed by the raw MAC, not the registry id.
        val result = listOf(device(id = "registry-id", address = "AA:11", batteryPercent = 90))
            .toDashboardSensorStatus(
                listOf(
                    status(
                        deviceId = "runtime-id",
                        address = "AA:11",
                        status = BleConnectionStatus.RECONNECTING,
                        batteryPercent = 33,
                    ),
                ),
            )

        val device = result.devices.single()
        assertEquals(33, device.batteryPercent)
        assertEquals(BleConnectionStatus.RECONNECTING, device.connectionStatus)
    }

    @Test
    fun `a device with no live status at all reads as disconnected`() {
        val result = listOf(device(id = "a", address = "AA:11", batteryPercent = 55))
            .toDashboardSensorStatus(emptyList())

        val device = result.devices.single()
        assertEquals(BleConnectionStatus.DISCONNECTED, device.connectionStatus)
        assertEquals(55, device.batteryPercent)
    }

    // ─── derived getters ──────────────────────────────────────────────────────

    @Test
    fun `a paired watch alone puts up no icon`() {
        // The bug this pins: pairing a smartwatch made the top-bar battery icon
        // appear, and tapping it opened an empty Sensors & devices screen — a
        // watch is listed under Settings > Watches, not there.
        val status = listOf(
            device(id = "watch", kind = BleDeviceKind.WATCH, batteryPercent = 80),
        ).toDashboardSensorStatus(emptyList())

        assertFalse(status.hasDevices)
        assertNull(status.lowestBatteryPercent)
    }

    @Test
    fun `a watch beside a sensor adds neither counts nor battery`() {
        val status = listOf(
            device(id = "hrm", address = "AA:11", batteryPercent = 60),
            device(
                id = "watch",
                address = "AA:22",
                kind = BleDeviceKind.WATCH,
                batteryPercent = 20,
            ),
        ).toDashboardSensorStatus(emptyList())

        assertEquals(1, status.devices.size)
        // The watch's lower battery must not headline the sensors action.
        assertEquals(60, status.lowestBatteryPercent)
        assertEquals(1, status.enabledCount)
    }

    @Test
    fun `a bike computer still counts and the screen lists it`() {
        // An Edge broadcasts standard GATT like any sensor, so it belongs to the
        // screen the battery icon opens — unlike a watch.
        val status = listOf(
            device(id = "edge", kind = BleDeviceKind.BIKE_COMPUTER),
        ).toDashboardSensorStatus(emptyList())

        assertTrue(status.hasDevices)
    }

    @Test
    fun `an empty status has no devices and no battery`() {
        val status = DashboardSensorStatus()

        assertFalse(status.hasDevices)
        assertEquals(0, status.enabledCount)
        assertEquals(0, status.connectedCount)
        assertNull(status.lowestBatteryPercent)
    }

    @Test
    fun `counts enabled and connected devices and the lowest battery`() {
        val status = listOf(
            device(id = "a", address = "AA:11"),
            device(id = "b", address = "AA:22"),
            device(id = "c", address = "AA:33", enabled = false),
        ).toDashboardSensorStatus(
            listOf(
                status(deviceId = "a", address = "AA:11", batteryPercent = 80),
                status(
                    deviceId = "b",
                    address = "AA:22",
                    status = BleConnectionStatus.CONNECTING,
                    batteryPercent = 17,
                ),
                // 'c' is disabled and never connects: no live status, no battery.
            ),
        )

        assertTrue(status.hasDevices)
        assertEquals(2, status.enabledCount)
        // Connecting is not connected: only 'a' counts.
        assertEquals(1, status.connectedCount)
        assertEquals(17, status.lowestBatteryPercent)
    }

    @Test
    fun `the lowest battery ignores devices that never reported one`() {
        // A null battery must not read as a zero and headline the top bar.
        val status = listOf(
            device(id = "a", address = "AA:11"),
            device(id = "b", address = "AA:22", batteryPercent = 64),
        ).toDashboardSensorStatus(emptyList())

        assertEquals(64, status.lowestBatteryPercent)
    }
}
