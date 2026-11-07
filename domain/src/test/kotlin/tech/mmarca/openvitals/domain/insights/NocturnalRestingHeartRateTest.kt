package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.table
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.worn

class NocturnalRestingHeartRateTest {

    private val onset = Instant.parse("2026-10-07T21:00:00Z")
    private val end = onset.plusSeconds(4 * 3600)

    private fun night(heartRate: (Int) -> Float?) = (0 until 240).map { minute ->
        val time = onset.plusSeconds(minute * 60L)
        heartRate(minute)?.let { worn(time, movement = 0f, heartRate = it) } ?: table(time)
    }

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
        // Ten readings at 45 and a gap; the rest of the night at 58.
        val minutes = night { minute ->
            when (minute) {
                in 60 until 70 -> 45f
                in 70 until 90 -> null
                else -> 58f
            }
        }

        // The lowest window that still holds 20 readings is [50, 80): ten 58s and ten 45s, a mean of 51.5.
        assertEquals(52, NocturnalRestingHeartRate.of(minutes, onset, end))
    }

    @Test
    fun `zero and NaN readings are not readings`() {
        val minutes = night { minute ->
            when (minute) {
                in 100 until 130 -> 0f
                in 130 until 160 -> Float.NaN
                else -> 60f
            }
        }

        assertEquals(60, NocturnalRestingHeartRate.of(minutes, onset, end))
    }

    @Test
    fun `minutes outside the night and a sparse night give nothing`() {
        val minutes = night { minute -> if (minute < 15) 50f else null }

        assertNull(NocturnalRestingHeartRate.of(minutes, onset, end))
        assertNull(NocturnalRestingHeartRate.of(night { 50f }, end, end.plusSeconds(3600)))
    }
}
