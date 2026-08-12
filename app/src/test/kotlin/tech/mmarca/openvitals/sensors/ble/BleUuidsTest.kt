package tech.mmarca.openvitals.sensors.ble

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminUuids

class BleUuidsTest {

    @Test
    fun `the Garmin scan filter filters on the ADVERTISED member service, not GFDI`() {
        // Regression, found on a real vívoactive 5 (2026-07-22). Its
        // advertisement is:
        //   mServiceUuids=[0000fe1f-0000-1000-8000-00805f9b34fb]
        //   mManufacturerSpecificData={135=[...]}   // 0x0087 = Garmin
        //   mDeviceName=vívoactive 5
        // and carries NO GFDI UUID — GFDI is a GATT service that appears only
        // after connecting. Filtering the scan on it matched nothing, so the
        // watch was invisible unless the user toggled "Show all devices".
        assertTrue(BleUuids.SCAN_SERVICE_UUIDS.contains(BleUuids.GARMIN_MEMBER_SERVICE))
        assertFalse(
            "a scan filter on a connect-only GATT service hides every Garmin " +
                "watch from discovery",
            BleUuids.SCAN_SERVICE_UUIDS.contains(UUID.fromString(GarminUuids.GFDI_SERVICE_V1)),
        )
    }

    @Test
    fun `the member service grants no sensor capabilities`() {
        // A watch streams nothing live. If this ever returned a capability, the
        // watch would enter capability assignment and the recording coordinator
        // would connect to it and wait for notifications it never sends.
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
