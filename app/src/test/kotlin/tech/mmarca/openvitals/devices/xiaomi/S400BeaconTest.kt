package tech.mmarca.openvitals.devices.xiaomi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ScaleReading

/**
 * Frames captured from real scales, with the keys their owners published:
 * the test vectors of Home Assistant's xiaomi-ble (Apache-2.0) and of
 * openScale (GPL-3.0, from lswiderski's MiScaleBodyComposition).
 */
class S400BeaconTest {

    private class Scale(val address: String, key: String) {
        val key: ByteArray = key.hexToByteArray()
    }

    private val beef = Scale("8C:D0:B2:F6:BE:EF", "0728974d657a4b60964c1b1677f35f7c")
    private val a5e6 = Scale("84:46:93:64:A5:E6", "58305740b64e4b425e518aa1f4e51339")
    private val c67c = Scale("04:AE:47:67:C6:7C", "02d2900363ef629c736a4549677acbee")

    /** A frame this file built: profile 3, 80.0 kg, 70 bpm, 900.0 ohm, sealed for [synthetic]. */
    private val synthetic = Scale("02:00:00:00:00:01", "00112233445566778899aabbccddeeff")
    private val highImpedanceFrame = "4859d53b072ee59431ca8a3a99837c0e64000000e045abeb"
    private val weightFrame = "4859d53b0abc078ff2348c844138e930220000009e538599"

    private fun decode(scale: Scale, frame: String, address: String = scale.address, key: ByteArray = scale.key) =
        S400Beacon.decode(frame.hexToByteArray(), address, key)

    @Test
    fun `every captured frame decodes to what its scale measured`() {
        val expected = listOf(
            // The weight frame carries the 50 kHz impedance and, last of all, the heart rate.
            Triple(beef, weightFrame, S400Frame(1, 1744250605, ScaleReading(69.9, 92, impedanceLowOhm = 543.2))),
            // Its second frame: the same timestamp, the 250 kHz impedance and nothing else.
            Triple(
                beef,
                "4859d53b0bd6ef0b25db72785e7e2f46d6000000d8642df6",
                S400Frame(1, 1744250605, ScaleReading(impedanceHighOhm = 497.6)),
            ),
            Triple(
                a5e6,
                "4859d53b2d3314943c58b133638c7457a4000000c3e670dc",
                S400Frame(1, 1751914873, ScaleReading(74.2, 52, impedanceLowOhm = 400.9)),
            ),
            Triple(
                a5e6,
                "4859d53b3bde6bc8d05b51c0cdfd9021c9000000925c5039",
                S400Frame(1, 1752500249, ScaleReading(73.2, 75, impedanceLowOhm = 414.3)),
            ),
            // Socks on: a weight, heart rate "not measured", no impedance.
            Triple(
                a5e6,
                "4859d53b4d6f359ce56f1f7e7e0adddc260000000c13d3c4",
                S400Frame(1, 1752586470, ScaleReading(73.3)),
            ),
            Triple(
                a5e6,
                "4859d53b63bb587950e1042cac1c5f18f6000000dbe97034",
                S400Frame(1, 1752677949, ScaleReading(75.2)),
            ),
            Triple(
                c67c,
                "4859d53b71530438b5894b242c209908da000000479ecda3",
                S400Frame(1, 1779961813, ScaleReading(74.7)),
            ),
            // Stepped off: the last weigh-in's timestamp and an empty reading.
            Triple(
                c67c,
                "4859d53b72036c6794355a19dbc864bfb3000000e4151dc8",
                S400Frame(1, 1779961813, ScaleReading()),
            ),
        )

        assertEquals(
            expected.map { (_, _, frame) -> S400BeaconResult.Frame(frame) },
            expected.map { (scale, frame, _) -> decode(scale, frame) },
        )
    }

    @Test
    fun `an impedance that fills the top bit is not read as negative`() {
        assertEquals(
            S400BeaconResult.Frame(S400Frame(3, 1767225600, ScaleReading(80.0, 70, impedanceLowOhm = 900.0))),
            decode(synthetic, highImpedanceFrame),
        )
    }

    @Test
    fun `a key or an address that is not the scale's opens nothing`() {
        assertEquals(S400BeaconResult.BadTag, decode(beef, weightFrame, key = a5e6.key))
        // The address seeds the nonce, so a frame replayed from another device fails the same way.
        assertEquals(S400BeaconResult.BadTag, decode(beef, weightFrame, address = a5e6.address))
    }

    @Test
    fun `what the key did not seal is never a reading`() {
        val idleBeaconWithMac = "1059d53b06e6a564934684"
        // The weigh-in object in the clear, as anyone nearby could broadcast it.
        val unsealedObject = "4059d53b08166e090320a3a08c00b95569"

        assertEquals(S400BeaconResult.NoReading, decode(a5e6, idleBeaconWithMac))
        assertEquals(S400BeaconResult.NoReading, decode(synthetic, unsealedObject))
    }

    @Test
    fun `frames that are not this scale's are turned away before the key is tried`() {
        val rejected = mapOf(
            "another Xiaomi product" to "4859aa010abc078ff2348c844138e930220000009e538599",
            "a mesh frame" to "c859d53b0abc078ff2348c844138e930220000009e538599",
            "a legacy version" to "4839d53b0abc078ff2348c844138e930220000009e538599",
            "an embedded address that is not the sender's" to "1059d53b06e6a564934685",
            "too short for a header" to "4859d53b",
            "too short for a sealed object" to "4859d53b0a0000000000000000",
        )

        assertEquals(
            rejected.mapValues { S400BeaconResult.NotS400 },
            rejected.mapValues { (_, frame) -> decode(a5e6, frame) },
        )
        assertEquals(S400BeaconResult.NotS400, decode(beef, weightFrame, address = "not an address"))
    }

    @Test
    fun `the scan filter lets measurement frames through and idle beacons not`() {
        fun passes(frame: String) = S400Beacon.scanFilters.any { it.matches(frame.hexToByteArray()) }

        assertTrue(passes(weightFrame))
        assertTrue(passes(highImpedanceFrame))
        assertFalse("an idle beacon", passes("1059d53b06e6a564934684"))
        assertFalse("another product", passes("4859aa010abc078ff2348c844138e930220000009e538599"))
    }
}
