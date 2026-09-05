package tech.mmarca.openvitals.devices.garmin.wellness

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/** The `stress_level` (227) message: the stress score and Body Battery, which has no message of its own. */
class FitStressBodyEnergyTest {

    /** `stress_level`: field 0 stress (sint8), 1 time (uint32), 3 body energy (uint8). */
    private fun stressFile(samples: List<Triple<Instant, Int, Int>>): ByteArray {
        val d = FitW().fileId(32) // FILE_TYPE monitoring
        d.def(
            1,
            227,
            listOf(
                listOf(0, 1, 1), // stress value, sint8
                listOf(1, 4, 134), // stress time, uint32
                listOf(3, 1, 2), // body energy, uint8
            ),
        )
        for ((at, stress, energy) in samples) {
            d.u8(0x01)
                .u8(stress and 0xFF)
                .u32(fitTimestamp(at))
                .u8(energy)
        }
        return fitWrap(d.toBytes())
    }

    @Test
    fun `extracts stress and body energy from the stress_level message`() {
        val t0 = Instant.parse("2026-07-22T10:01:00Z")
        val m = parseGarminWellness(
            stressFile(
                listOf(
                    Triple(t0, 42, 72),
                    Triple(t0.plusSeconds(60), 51, 72),
                ),
            ),
        ).monitoring!!

        // Both series come off one message.
        assertEquals(listOf(t0 to 42, t0.plusSeconds(60) to 51), m.stress)
        assertEquals(listOf(t0 to 72, t0.plusSeconds(60) to 72), m.bodyEnergy)
    }

    @Test
    fun `a negative stress score is dropped not clamped`() {
        val t0 = Instant.parse("2026-07-22T10:01:00Z")
        val m = parseGarminWellness(
            stressFile(
                listOf(
                    // Garmin's "not measurable": asleep, moving, poor contact.
                    Triple(t0, -23, 72),
                    Triple(t0.plusSeconds(60), 30, 71),
                ),
            ),
        ).monitoring!!

        // Recording it as 0 would read as "completely relaxed", which is a lie.
        assertEquals(listOf(t0.plusSeconds(60) to 30), m.stress)
        // Body energy is still valid on that same record, so it survives.
        assertEquals(2, m.bodyEnergy.size)
    }

    @Test
    fun `the stress message alone makes a file non-empty`() {
        // Without this the monitoring summary would be dropped as empty.
        val m = parseGarminWellness(
            stressFile(listOf(Triple(Instant.parse("2026-07-22T10:00:00Z"), 40, 70))),
        ).monitoring
        assertNotNull(m)
        assertFalse(m!!.isEmpty)
    }

    @Test
    fun `uses the message's own time field not the record header`() {
        // stress_level carries its own timestamp; Gadgetbridge prefers it too.
        val at = Instant.parse("2026-07-22T03:45:00Z")
        val m = parseGarminWellness(stressFile(listOf(Triple(at, 20, 90)))).monitoring!!
        assertEquals(at, m.stress.single().first)
    }
}
