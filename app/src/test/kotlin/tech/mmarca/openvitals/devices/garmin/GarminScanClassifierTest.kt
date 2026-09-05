package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminScanClassifierTest {

    private val classifier = GarminScanClassifier()

    private val memberService = "0000fe1f-0000-1000-8000-00805f9b34fb"

    @Test
    fun `claims an advertisement carrying the member service`() {
        assertTrue(classifier.advertisesSyncService(listOf(memberService)))
    }

    @Test
    fun `claims it alongside unrelated advertised services`() {
        assertTrue(
            classifier.advertisesSyncService(
                listOf(
                    "0000180d-0000-1000-8000-00805f9b34fb", // heart rate
                    memberService,
                ),
            ),
        )
    }

    @Test
    fun `does not claim a live sensor advertisement`() {
        assertFalse(
            classifier.advertisesSyncService(
                listOf(
                    "0000180d-0000-1000-8000-00805f9b34fb", // heart rate
                    "00001816-0000-1000-8000-00805f9b34fb", // cycling speed/cadence
                ),
            ),
        )
    }

    @Test
    fun `does not claim an empty advertisement`() {
        assertFalse(classifier.advertisesSyncService(emptyList()))
    }

    // The classifier keys on the advertised member service (0xFE1F), never the GATT-only GFDI UUID.
    @Test
    fun `does not key on the connect-only GFDI service`() {
        assertFalse(
            classifier.advertisesSyncService(listOf(GarminUuids.GFDI_SERVICE_V1)),
        )
    }
}
