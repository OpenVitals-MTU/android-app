package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.core.sync.sleepNightWindow

/**
 * The night bookkeeping around the estimator: which nights a pull touched,
 * where the next pull starts, and that a synthetic night comes out as one
 * session under the night's id.
 */
class WearOsSleepImportTest {

    private val zone: ZoneOffset = ZoneOffset.ofHours(2)
    private val night: LocalDate = LocalDate.of(2026, 10, 8)

    /** A minute at [hour]:[minute] local time on the evening before or the morning of [night]. */
    private fun at(hour: Int, minute: Int = 0): Instant {
        val date = if (hour >= 18) night.minusDays(1) else night
        return date.atTime(hour, minute).toInstant(zone)
    }

    private fun raw(time: Instant, movement: Float, heartRate: Float?) =
        WearOsSleepMinute(time, WearLinkProtocol.MinuteKind.RAW, movement, heartRate, zone)

    /** A synthetic night: restless evening, seven still hours with a low heart rate, restless morning. */
    private fun syntheticNight(): List<WearOsSleepMinute> {
        val minutes = ArrayList<WearOsSleepMinute>()
        var time = at(21, 0)
        val onset = at(23, 0)
        val wake = at(6, 0)
        val end = at(8, 0)
        var index = 0
        while (time.isBefore(end)) {
            minutes += when {
                time.isBefore(onset) || !time.isBefore(wake) -> raw(time, 8f + (index * 5) % 8, 70f)
                else -> raw(time, 0f, 50f + (index % 40) / 10f)
            }
            time = time.plusSeconds(60)
            index++
        }
        return minutes
    }

    @Test
    fun `touched nights come from minutes newer than the cursor, with the last offset`() {
        val minutes = listOf(
            raw(at(22), 3f, 60f),
            raw(at(2), 0f, 50f),
            WearOsSleepMinute(at(3), WearLinkProtocol.MinuteKind.RAW, 0f, 50f, ZoneOffset.ofHours(3)),
            raw(at(15), 5f, 70f),
        )

        val nights = WearOsSleepImport.touchedNights(minutes, cursor = at(22))

        // The 22:00 minute is the cursor itself, the 15:00 one falls in the afternoon gap.
        assertEquals(mapOf(night to ZoneOffset.ofHours(3)), nights)
    }

    @Test
    fun `the pull starts at the night window of the cursor, or at the cursor in the afternoon`() {
        val (windowStart, _) = sleepNightWindow(night, zone)

        assertEquals(windowStart.minusMillis(1), WearOsSleepImport.pullStart(at(3), zone))
        assertEquals(at(15), WearOsSleepImport.pullStart(at(15), zone))
        assertEquals(Instant.EPOCH, WearOsSleepImport.pullStart(Instant.EPOCH, zone))
    }

    @Test
    fun `a synthetic night is one session under the night's id`() {
        val record = WearOsSleepImport.record(syntheticNight(), night, zone, version = 7L)

        assertNotNull(record)
        record!!
        assertEquals("wearos_sleep_est_2026-10-08", record.metadata.clientRecordId)
        assertEquals(7L, record.metadata.clientRecordVersion)
        assertTrue(record.startTime.isAfter(at(22, 30)) && record.startTime.isBefore(at(23, 30)))
        assertTrue(record.endTime.isAfter(at(5, 30)) && record.endTime.isBefore(at(6, 30)))
        assertTrue(record.stages.isNotEmpty())
        assertEquals(zone, record.startZoneOffset)
    }

    @Test
    fun `minutes outside the window do not reach the estimator`() {
        val inside = WearOsSleepImport.minutesOfNight(syntheticNight(), night, zone)
        val (from, to) = sleepNightWindow(night, zone)

        assertTrue(inside.all { !it.time.isBefore(from) && it.time.isBefore(to) })
        assertEquals(0, WearOsSleepImport.minutesOfNight(syntheticNight(), night.plusDays(3), zone).size)
    }

    @Test
    fun `a day of restless minutes is no night`() {
        val minutes = (0 until 600).map { raw(at(20).plusSeconds(it * 60L), 12f, 75f) }

        assertNull(WearOsSleepImport.record(minutes, night, zone, version = 1L))
    }

    @Test
    fun `only this import's ids count as its own`() {
        assertTrue(WearOsSleepImport.isOwnRecordId("wearos_sleep_est_2026-10-08"))
        assertFalse(WearOsSleepImport.isOwnRecordId("garmin_fit_sleep_est_2026-10-08"))
        assertFalse(WearOsSleepImport.isOwnRecordId(null))
    }
}
