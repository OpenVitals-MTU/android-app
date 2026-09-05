package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/** The sensor-status mapping the dashboard's battery action reads. */
class DashboardSensorStatusTest {

    private fun device(
        id: String,
        address: String = "AA:BB:CC:DD:EE:FF",
        enabled: Boolean = true,
        batteryPercent: Int? = null,
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

    @Test
    fun `the live battery wins over the persisted one`() {
        val result = listOf(device(id = "a", batteryPercent = 90))
            .toDashboardSensorStatus(listOf(status(deviceId = "a", batteryPercent = 42)))

        assertEquals(42, result.devices.single().batteryPercent)
        assertEquals(BleConnectionStatus.CONNECTED, result.devices.single().connectionStatus)
    }

    @Test
    fun `the persisted battery is the fallback when no live one is reported`() {
        val result = listOf(device(id = "a", batteryPercent = 90))
            .toDashboardSensorStatus(listOf(status(deviceId = "a")))

        assertEquals(90, result.devices.single().batteryPercent)
        assertEquals(BleConnectionStatus.CONNECTED, result.devices.single().connectionStatus)
    }

    @Test
    fun `the lookup falls back from device id to address`() {
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

        val mapped = result.devices.single()
        assertEquals(33, mapped.batteryPercent)
        assertEquals(BleConnectionStatus.RECONNECTING, mapped.connectionStatus)
    }

    @Test
    fun `a device with no live status at all reads as disconnected`() {
        val result = listOf(device(id = "a", address = "AA:11", batteryPercent = 55))
            .toDashboardSensorStatus(emptyList())

        val mapped = result.devices.single()
        assertEquals(BleConnectionStatus.DISCONNECTED, mapped.connectionStatus)
        assertEquals(55, mapped.batteryPercent)
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
            ),
        )

        assertTrue(status.hasDevices)
        assertEquals(2, status.enabledCount)
        assertEquals(1, status.connectedCount)
        assertEquals(17, status.lowestBatteryPercent)
    }

    @Test
    fun `the lowest battery ignores devices that never reported one`() {
        val status = listOf(
            device(id = "a", address = "AA:11"),
            device(id = "b", address = "AA:22", batteryPercent = 64),
        ).toDashboardSensorStatus(emptyList())

        assertEquals(64, status.lowestBatteryPercent)
    }
}
