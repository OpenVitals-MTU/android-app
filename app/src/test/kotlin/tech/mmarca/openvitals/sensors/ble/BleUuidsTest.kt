package tech.mmarca.openvitals.sensors.ble

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminUuids

class BleUuidsTest {

    @Test
    fun `the Garmin scan filter filters on the ADVERTISED member service, not GFDI`() {
        // A real vívoactive 5 advertises only the Garmin member service (fe1f) and no GFDI UUID.
        // Filtering the scan on GFDI matched nothing.
        assertTrue(BleUuids.SCAN_SERVICE_UUIDS.contains(BleUuids.GARMIN_MEMBER_SERVICE))
        assertFalse(
            "a scan filter on a connect-only GATT service hides every Garmin " +
                "watch from discovery",
            BleUuids.SCAN_SERVICE_UUIDS.contains(UUID.fromString(GarminUuids.GFDI_SERVICE_V1)),
        )
    }

    @Test
    fun `the member service grants no sensor capabilities`() {
        // A watch streams nothing live; a capability here would make the coordinator connect and wait forever.
        assertTrue(BleUuids.capabilitiesForService(BleUuids.GARMIN_MEMBER_SERVICE).isEmpty())
    }

    @Test
    fun `the standard sensor services are still in the filter`() {
        // The Garmin entry must be an addition, not a replacement.
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
