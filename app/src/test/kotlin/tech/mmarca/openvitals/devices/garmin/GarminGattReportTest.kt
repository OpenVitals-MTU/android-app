package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Port of the Flutter build's `garmin_gatt_report_test.dart` — fixtures identical. */
class GarminGattReportTest {

    private fun report(
        variant: GarminTransportVariant,
        services: List<GarminGattService> = emptyList(),
    ) = GarminGattReport(
        address = "E0:48:24:D5:F7:10",
        variant = variant,
        services = services,
    )

    // isSupported.

    @Test
    fun `true for the transports this app can drive`() {
        assertTrue(report(GarminTransportVariant.V1).isSupported)
        assertTrue(report(GarminTransportVariant.V2).isSupported)
    }

    @Test
    fun `false when the verdict says nothing usable`() {
        // Unknown means "enumerated and did not recognise"; unreachable means "learnt nothing", worth a retry.
        assertFalse(report(GarminTransportVariant.UNKNOWN).isSupported)
        assertFalse(report(GarminTransportVariant.UNREACHABLE).isSupported)
    }

    // describe.

    @Test
    fun `renders every service and characteristic under one grep-able tag`() {
        val gattReport = report(
            GarminTransportVariant.V1,
            services = listOf(
                GarminGattService(
                    uuid = "6a4e2401-667b-11e3-949a-0800200c9a66",
                    characteristics = mapOf(
                        "6a4e4c80-667b-11e3-949a-0800200c9a66" to listOf("writeNoRsp"),
                        "6a4ecd28-667b-11e3-949a-0800200c9a66" to listOf("notify"),
                    ),
                ),
            ),
        )

        val lines = gattReport.describe().split('\n')

        // Every line carries the tag: the log is read with grep.
        assertTrue(lines.all { it.startsWith("[GARMIN-GATT]") })
        assertTrue(lines.first().contains("variant=v1"))
        assertTrue(lines.first().contains("services=1"))
        assertTrue(
            gattReport.describe()
                .contains("6a4e4c80-667b-11e3-949a-0800200c9a66 [writeNoRsp]"),
        )
        assertTrue(
            gattReport.describe()
                .contains("6a4ecd28-667b-11e3-949a-0800200c9a66 [notify]"),
        )
    }

    @Test
    fun `an unknown device still dumps what it found`() {
        // The case the dump exists for: the verdict alone cannot be diagnosed.
        val gattReport = report(
            GarminTransportVariant.UNKNOWN,
            services = listOf(
                GarminGattService(
                    uuid = "0000180f-0000-1000-8000-00805f9b34fb",
                    characteristics = mapOf(
                        "00002a19-0000-1000-8000-00805f9b34fb" to listOf("read", "notify"),
                    ),
                ),
            ),
        )

        assertTrue(gattReport.describe().contains("variant=unknown"))
        assertTrue(gattReport.describe().contains("0000180f-0000-1000-8000-00805f9b34fb"))
        assertTrue(gattReport.describe().contains("[read,notify]"))
    }

    // classify (the probe's pure half).

    @Test
    fun `classifies a V2 multi-link table and prefers it over V1`() {
        val v2Table = listOf(
            GarminGattService(
                uuid = "6a4e2800-667b-11e3-949a-0800200c9a66",
                characteristics = mapOf(
                    GarminUuids.uuidForHandle(0x2810) to listOf("notify"),
                    GarminUuids.uuidForHandle(0x2820) to listOf("writeNoRsp"),
                    // A V1 pair as well: V2 must still win.
                    GarminUuids.GFDI_SEND_V1 to listOf("writeNoRsp"),
                    GarminUuids.GFDI_RECEIVE_V1 to listOf("notify"),
                ),
            ),
        )
        assertEquals(GarminTransportVariant.V2, GarminGattProbe.classify(v2Table))
    }

    @Test
    fun `classifies a V1-only table and an unrecognised one`() {
        val v1Table = listOf(
            GarminGattService(
                uuid = "6a4e2401-667b-11e3-949a-0800200c9a66",
                characteristics = mapOf(
                    GarminUuids.GFDI_SEND_V1 to listOf("writeNoRsp"),
                    GarminUuids.GFDI_RECEIVE_V1 to listOf("notify"),
                ),
            ),
        )
        assertEquals(GarminTransportVariant.V1, GarminGattProbe.classify(v1Table))
        assertEquals(GarminTransportVariant.UNKNOWN, GarminGattProbe.classify(emptyList()))
    }
}
