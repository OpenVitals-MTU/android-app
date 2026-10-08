package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.minutes
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.table
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.worn
import tech.mmarca.openvitals.domain.model.WearSleepMinute

class SptWindowDetectorTest {

    private val start: Instant = Instant.parse("2026-10-07T20:30:00Z")

    private fun detect(rows: List<WearSleepMinute>, config: SptWindowDetector.Config = SptWindowDetector.Config()): SptWindowDetector.SptWindow? {
        val grid = WearMinuteGrid.of(rows)!!
        return SptWindowDetector.detect(grid, WearStateDetector.detect(grid), config)
    }

    @Test
    fun `the still stretch of a worn night is the window`() {
        val window = detect(WearSleepFixtures.wornNight(start))

        assertNotNull(window)
        window!!
        // Thirty restless minutes, 450 still ones, an hour of restlessness: the window covers the still ones.
        assertTrue("onset ${window.onset}", window.onset in 25..40)
        assertTrue("end ${window.end}", window.end in 460..485)
    }

    @Test
    fun `a short restless break inside the night is merged, a long one is not`() {
        val short = minutes(start, 600) { index, time ->
            if (index in 30 until 270 || index in 300 until 540) worn(time, 0f, 52f) else worn(time, 9f, 66f)
        }
        val long = minutes(start, 600) { index, time ->
            if (index in 30 until 240 || index in 330 until 540) worn(time, 0f, 52f) else worn(time, 9f, 66f)
        }

        val merged = SptWindowDetector.angleWindow(WearMinuteGrid.of(short)!!, WearStateDetector.detect(WearMinuteGrid.of(short)!!), SptWindowDetector.Config())
        val split = SptWindowDetector.angleWindow(WearMinuteGrid.of(long)!!, WearStateDetector.detect(WearMinuteGrid.of(long)!!), SptWindowDetector.Config())

        assertEquals(30..539, merged)
        // Two blocks of the same length: the earlier one wins.
        assertEquals(30..239, split)
    }

    @Test
    fun `a charger gap breaks the window even when short`() {
        val rows = minutes(start, 600) { index, time ->
            when (index) {
                in 30 until 270 -> worn(time, 0f, 52f)
                in 270 until 290 -> table(time, charging = true)
                in 290 until 540 -> worn(time, 0f, 52f)
                else -> worn(time, 9f, 66f)
            }
        }

        val window = detect(rows)

        assertNotNull(window)
        assertEquals(290, window!!.onset)
    }

    @Test
    fun `without a pulse the angle alone finds the window`() {
        val rows = minutes(start, 600) { index, time ->
            val movement = if (index in 30 until 540) 0f else 9f
            worn(time, movement, 0f, flags = 0).copy(heartRate = null, heartRateSamples = 0, heartRateSd = null)
        }

        val window = detect(rows)

        assertEquals(SptWindowDetector.Source.ANGLE, window?.source)
    }

    @Test
    fun `the heart rate tightens the end of the angle window, never its onset`() {
        // The arm is still from minute 30 to 540; the pulse settles only at 60 and rises again at 500.
        val rows = minutes(start, 600) { index, time ->
            when {
                index < 30 -> worn(time, 9f, 66f)
                index < 60 -> worn(time, 0f, 66f)
                index < 500 -> worn(time, 0f, 50f)
                index < 540 -> worn(time, 0f, 66f)
                else -> worn(time, 9f, 66f)
            }
        }

        // Three quarters of the minutes carry the low rate: a quantile under that share cuts between the two levels.
        val window = detect(rows, SptWindowDetector.Config(heartRateQuantile = 0.6f))

        assertNotNull(window)
        assertEquals(SptWindowDetector.Source.BOTH, window!!.source)
        assertTrue("onset ${window.onset}", window.onset in 28..34)
        assertTrue("end ${window.end}", window.end in 496..506)
    }

    @Test
    fun `a window with too few worn minutes is none`() {
        val rows = minutes(start, 400) { index, time ->
            if (index in 30 until 150) worn(time, 0f, 52f) else table(time)
        }

        assertNull(detect(rows))
    }
}
