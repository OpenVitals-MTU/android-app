package tech.mmarca.openvitals.sensors.ble

import org.junit.Assert.assertTrue
import org.junit.Test

class BleUuidsTest {

    @Test
    fun `the standard sensor services are in the scan filter`() {
        assertTrue(
            BleUuids.SCAN_SERVICE_UUIDS.containsAll(
                listOf(
                    BleUuids.HEART_RATE.serviceUuid,
                    BleUuids.CYCLING_SPEED_CADENCE.serviceUuid,
                    BleUuids.CYCLING_POWER.serviceUuid,
                    BleUuids.RUNNING_SPEED_CADENCE.serviceUuid,
                ),
            ),
        )
    }
}
