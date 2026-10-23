package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.devices.wearos.WearSleepMinutes

class NocturnalRestingHeartRateTest {

    private val onset = Instant.parse("2026-10-07T21:00:00Z")
    private val end = onset.plusSeconds(4 * 3600)

    private fun night(heartRate: (Int) -> Float?) =
        (0 until 240).map { WearSleepMinutes.minute(onset.plusSeconds(it * 60L), 0f, heartRate(it), ZoneOffset.UTC) }

    @Test
    fun `the lowest half hour wins, not the lowest single minute`() {
        // 60 bpm all night, one dip to 40 for a single minute, and a half hour at 52 from 02:00.
        val minutes = night { minute ->
            when (minute) {
                10 -> 40f
                in 120 until 150 -> 52f
                else -> 60f
            }
        }

        assertEquals(52, NocturnalRestingHeartRate.of(minutes, onset, end))
    }

    @Test
    fun `a window with too few readings cannot be the low`() {
        // A half hour with only ten readings at 45; the rest of the night at 58.
        val minutes = night { minute ->
            when (minute) {
                in 60 until 70 -> 45f
                in 70 until 90 -> null
                else -> 58f
            }
        }

        // Windows that hold the ten low readings also hold enough 58s to pass; none is as low as 45.
        val bpm = NocturnalRestingHeartRate.of(minutes, onset, end)!!
        assertEquals(true, bpm > 45)
    }

    @Test
    fun `minutes outside the night and a sparse night give nothing`() {
        val minutes = night { minute -> if (minute < 15) 50f else null }

        assertNull(NocturnalRestingHeartRate.of(minutes, onset, end))
        assertNull(NocturnalRestingHeartRate.of(night { 50f }, end, end.plusSeconds(3600)))
    }
}
