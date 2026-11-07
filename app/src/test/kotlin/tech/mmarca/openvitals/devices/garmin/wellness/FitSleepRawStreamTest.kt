package tech.mmarca.openvitals.devices.garmin.wellness

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A watch that leaves staging to Garmin's servers yields a per-minute stream, never a session. */
class FitSleepRawStreamTest {

    private val start: Instant = Instant.parse("2024-01-01T21:00:00Z")

    private val rows = listOf(
        FitSleepStreamRow.Stage(0),
        rawRow(heartRate = 69f, movement = 1.5f, activity = -0.5f),
        FitSleepStreamRow.Stage(1),
        rawRow(heartRate = 55f, movement = 0f, activity = 0.25f),
    )

    @Test
    fun `rows sit one sample apart from the info timestamp`() {
        val wellness = parseGarminWellness(fitSleepRawBytes(start, offsetSeconds = 7200, rows = rows))

        val minutes = wellness.sleepMinutes
        assertEquals(4, minutes.size)
        assertEquals(listOf(0L, 60L, 120L, 180L), minutes.map { it.time.epochSecond - start.epochSecond })
        assertEquals(
            listOf(
                FitSleepMinuteKind.UNMEASURABLE,
                FitSleepMinuteKind.RAW,
                FitSleepMinuteKind.AWAKE,
                FitSleepMinuteKind.RAW,
            ),
            minutes.map { it.kind },
        )
    }

    @Test
    fun `raw rows decode heart rate, movement and activity`() {
        val wellness = parseGarminWellness(fitSleepRawBytes(start, offsetSeconds = 7200, rows = rows))

        val first = wellness.sleepMinutes[1]
        assertEquals(69.0, first.heartRate!!, 0.0)
        assertEquals(1.5, first.movement!!, 0.0)
        assertEquals(-0.5, first.activity!!, 0.0)
        assertEquals(10, first.features!!.size)
        val stage = wellness.sleepMinutes[2]
        assertNull(stage.heartRate)
        assertNull(stage.features)
    }

    @Test
    fun `the zone offset comes from local_timestamp`() {
        val wellness = parseGarminWellness(fitSleepRawBytes(start, offsetSeconds = -18000, rows = rows))

        assertTrue(wellness.sleepMinutes.all { it.zoneOffset == ZoneOffset.ofHours(-5) })
    }

    @Test
    fun `no session is built from awake-only levels`() {
        val wellness = parseGarminWellness(fitSleepRawBytes(start, offsetSeconds = 7200, rows = rows))

        assertNull(wellness.sleep)
        assertTrue(wellness.sleepMinutes.isNotEmpty())
        assertEquals(49, wellness.fileType)
    }

    @Test
    fun `stage rows without a timestamp take their slot from the stream`() {
        val wellness = parseGarminWellness(
            fitSleepRawBytes(start, offsetSeconds = 0, rows = rows, stageTimestamps = false),
        )

        assertEquals(listOf(0L, 60L, 120L, 180L), wellness.sleepMinutes.map { it.time.epochSecond - start.epochSecond })
    }

    @Test
    fun `chained files concatenate their streams`() {
        val second = start.plusSeconds(4 * 60)
        val bytes = fitSleepRawBytes(start, 7200, rows) +
            fitSleepRawBytes(second, 7200, listOf(rawRow(heartRate = 50f)))

        val wellness = parseGarminWellness(bytes)

        assertEquals(5, wellness.sleepMinutes.size)
        assertEquals(second, wellness.sleepMinutes.last().time)
    }

    @Test
    fun `a file with real stages keeps the staged path and yields no minutes`() {
        val stop = start.plusSeconds(3 * 3600)
        val bytes = fitSleepBytes(
            start,
            stop,
            listOf(start.plusSeconds(3600) to 2, start.plusSeconds(7200) to 3, stop to 4),
        )

        val wellness = parseGarminWellness(bytes)

        assertNotNull(wellness.sleep)
        assertTrue(wellness.sleepMinutes.isEmpty())
    }

    @Test
    fun `an out-of-range heart rate is dropped, the row is kept`() {
        val wellness = parseGarminWellness(
            fitSleepRawBytes(start, 0, listOf(rawRow(heartRate = 0f), rawRow(heartRate = 300f))),
        )

        assertEquals(2, wellness.sleepMinutes.size)
        assertTrue(wellness.sleepMinutes.all { it.heartRate == null })
    }
}
