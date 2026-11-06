package tech.mmarca.openvitals.domain.cycle

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightlyVitalsCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val start = LocalDate.parse("2026-03-01")
    private val today = LocalDate.parse("2026-03-10")

    private fun at(date: String, hour: Int, minute: Int = 0): Instant =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant()

    /** Sleeps from 23:00 the evening before [wakeDate] to 07:00 on it. */
    private fun night(wakeDate: String): NightWindow {
        val wake = LocalDate.parse(wakeDate)
        return NightWindow(wake.minusDays(1).atTime(23, 0).atZone(zone).toInstant(), at(wakeDate, 7))
    }

    private fun sample(date: String, hour: Int, value: Double, source: String = "watch") =
        NightSample(at(date, hour), value, source)

    private fun build(
        sleep: List<NightWindow>,
        pulse: List<NightSample> = emptyList(),
        hrv: List<NightSample> = emptyList(),
        breathing: List<NightSample> = emptyList(),
    ) = NightlyVitalsCalculator.build(
        cycleStart = start,
        today = today,
        sleep = sleep,
        samples = mapOf(
            NightlySignal.RESTING_HEART_RATE to pulse,
            NightlySignal.HRV_RMSSD to hrv,
            NightlySignal.RESPIRATORY_RATE to breathing,
        ),
        zone = zone,
    )

    private fun NightlyVitals.of(signal: NightlySignal) = series.first { it.signal == signal }

    @Test
    fun `the night value is the median of the readings in the session`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            pulse = listOf(
                sample("2026-03-02", 1, 50.0),
                sample("2026-03-02", 3, 70.0),
                sample("2026-03-02", 5, 52.0),
            ),
        )

        val point = result.of(NightlySignal.RESTING_HEART_RATE).points.single()
        assertEquals(52.0, point.value, 0.0)
        assertEquals(3, point.sampleCount)
    }

    @Test
    fun `an even count takes the mean of the two middle readings`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            hrv = listOf(sample("2026-03-02", 1, 40.0), sample("2026-03-02", 2, 60.0), sample("2026-03-02", 3, 100.0), sample("2026-03-02", 4, 10.0)),
        )

        assertEquals(50.0, result.of(NightlySignal.HRV_RMSSD).points.single().value, 0.0)
    }

    @Test
    fun `the cycle day is the wake-up day counted from the start as day one`() {
        val result = build(
            sleep = listOf(night("2026-03-01"), night("2026-03-05")),
            pulse = listOf(sample("2026-03-01", 2, 55.0), sample("2026-03-05", 2, 58.0)),
        )

        assertEquals(listOf(1, 5), result.of(NightlySignal.RESTING_HEART_RATE).points.map { it.cycleDay })
    }

    @Test
    fun `readings outside any sleep session are ignored`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            hrv = listOf(sample("2026-03-02", 12, 80.0), sample("2026-03-01", 20, 90.0)),
        )

        assertTrue(result.of(NightlySignal.HRV_RMSSD).isMissing)
        assertNull(result.of(NightlySignal.HRV_RMSSD).source)
    }

    @Test
    fun `a resting pulse stamped on the wake-up day outside the night is a labelled daily value`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            pulse = listOf(sample("2026-03-02", 12, 57.0)),
        )

        val point = result.of(NightlySignal.RESTING_HEART_RATE).points.single()
        assertEquals(57.0, point.value, 0.0)
        assertTrue(point.isDailyValue)
        assertEquals(1, result.of(NightlySignal.RESTING_HEART_RATE).dailyValueCount)
    }

    @Test
    fun `a reading inside the night wins over the daily value of the same source`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            pulse = listOf(sample("2026-03-02", 3, 52.0), sample("2026-03-02", 12, 60.0)),
        )

        val point = result.of(NightlySignal.RESTING_HEART_RATE).points.single()
        assertEquals(52.0, point.value, 0.0)
        assertFalse(point.isDailyValue)
    }

    @Test
    fun `only resting pulse falls back to a daily value`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            breathing = listOf(sample("2026-03-02", 12, 15.0)),
        )

        assertTrue(result.of(NightlySignal.RESPIRATORY_RATE).isMissing)
    }

    @Test
    fun `pieces of one night split by the source are one night`() {
        val first = NightWindow(at("2026-03-01", 23), at("2026-03-02", 2))
        val second = NightWindow(at("2026-03-02", 3), at("2026-03-02", 7))
        val result = build(
            sleep = listOf(second, first),
            hrv = listOf(sample("2026-03-02", 1, 40.0), sample("2026-03-02", 5, 60.0), sample("2026-03-02", 6, 50.0)),
        )

        assertEquals(1, result.nightCount)
        val point = result.of(NightlySignal.HRV_RMSSD).points.single()
        assertEquals(3, point.sampleCount)
        assertEquals(50.0, point.value, 0.0)
    }

    @Test
    fun `a nap far from the night stays a nap`() {
        val nap = NightWindow(at("2026-03-02", 13), at("2026-03-02", 14))
        val result = build(
            sleep = listOf(night("2026-03-02"), nap),
            hrv = listOf(sample("2026-03-02", 13, 99.0), sample("2026-03-02", 2, 40.0)),
        )

        assertEquals(40.0, result.of(NightlySignal.HRV_RMSSD).points.single().value, 0.0)
    }

    @Test
    fun `a night across the spring DST change keeps its cycle day and its readings`() {
        val vienna = ZoneId.of("Europe/Vienna")
        val cycleStart = LocalDate.parse("2026-03-27")
        // 28 March 23:00 to 29 March 07:00 local: the clocks jump at 02:00, so the night is seven hours long.
        val window = NightWindow(
            LocalDate.parse("2026-03-28").atTime(23, 0).atZone(vienna).toInstant(),
            LocalDate.parse("2026-03-29").atTime(7, 0).atZone(vienna).toInstant(),
        )
        val before = NightSample(LocalDate.parse("2026-03-29").atTime(1, 30).atZone(vienna).toInstant(), 50.0, "watch")
        val after = NightSample(LocalDate.parse("2026-03-29").atTime(3, 30).atZone(vienna).toInstant(), 54.0, "watch")

        val result = NightlyVitalsCalculator.build(
            cycleStart = cycleStart,
            today = LocalDate.parse("2026-03-31"),
            sleep = listOf(window),
            samples = mapOf(NightlySignal.HRV_RMSSD to listOf(before, after)),
            zone = vienna,
        )

        val point = result.series.first { it.signal == NightlySignal.HRV_RMSSD }.points.single()
        assertEquals(3, point.cycleDay)
        assertEquals(LocalDate.parse("2026-03-29"), point.date)
        assertEquals(52.0, point.value, 0.0)
    }

    @Test
    fun `a night across the autumn DST change keeps its cycle day`() {
        val vienna = ZoneId.of("Europe/Vienna")
        // 24 October 23:00 to 25 October 07:00: the 02:00 hour happens twice, the night is nine hours long.
        val window = NightWindow(
            LocalDate.parse("2026-10-24").atTime(23, 0).atZone(vienna).toInstant(),
            LocalDate.parse("2026-10-25").atTime(7, 0).atZone(vienna).toInstant(),
        )
        val sample = NightSample(window.start.plusSeconds(4 * 3600), 61.0, "watch")

        val result = NightlyVitalsCalculator.build(
            cycleStart = LocalDate.parse("2026-10-20"),
            today = LocalDate.parse("2026-10-26"),
            sleep = listOf(window),
            samples = mapOf(NightlySignal.HRV_RMSSD to listOf(sample)),
            zone = vienna,
        )

        assertEquals(6, result.series.first { it.signal == NightlySignal.HRV_RMSSD }.points.single().cycleDay)
    }

    @Test
    fun `a permission failure is carried on the signal`() {
        val result = NightlyVitalsCalculator.build(
            cycleStart = start,
            today = today,
            sleep = listOf(night("2026-03-02")),
            samples = emptyMap(),
            zone = zone,
            permissionDenied = setOf(NightlySignal.HRV_RMSSD),
        )

        assertTrue(result.series.first { it.signal == NightlySignal.HRV_RMSSD }.permissionDenied)
        assertFalse(result.series.first { it.signal == NightlySignal.RESPIRATORY_RATE }.permissionDenied)
    }

    @Test
    fun `a missing signal stays empty while the others are shown`() {
        val result = build(
            sleep = listOf(night("2026-03-02")),
            pulse = listOf(sample("2026-03-02", 2, 55.0)),
            breathing = listOf(sample("2026-03-02", 2, 14.5)),
        )

        assertTrue(result.hasAnySignal)
        assertEquals(listOf(NightlySignal.HRV_RMSSD), result.missingSignals)
        assertEquals(1, result.nightCount)
    }

    @Test
    fun `without sleep nothing is placed on a night`() {
        val result = build(sleep = emptyList(), pulse = listOf(sample("2026-03-02", 2, 55.0)))

        assertEquals(0, result.nightCount)
        assertFalse(result.hasAnySignal)
        assertEquals(NightlySignal.entries, result.missingSignals)
    }

    @Test
    fun `sources are not mixed, the one covering most nights wins`() {
        val result = build(
            sleep = listOf(night("2026-03-02"), night("2026-03-03"), night("2026-03-04")),
            pulse = listOf(
                sample("2026-03-02", 2, 52.0, "ring"),
                sample("2026-03-03", 2, 53.0, "ring"),
                sample("2026-03-04", 3, 54.0, "ring"),
                // The band wrote on two nights only, at a different level.
                sample("2026-03-04", 2, 63.0, "band"),
                sample("2026-03-02", 3, 64.0, "band"),
            ),
        )

        val series = result.of(NightlySignal.RESTING_HEART_RATE)
        assertEquals("ring", series.source)
        assertEquals(setOf("band"), series.otherSources)
        assertEquals(listOf(52.0, 53.0, 54.0), series.points.map { it.value })
    }

    @Test
    fun `a tie between sources goes to the one with more readings, then the name`() {
        val sleep = listOf(night("2026-03-02"))
        val moreReadings = build(
            sleep = sleep,
            pulse = listOf(sample("2026-03-02", 1, 50.0, "b"), sample("2026-03-02", 2, 51.0, "b"), sample("2026-03-02", 3, 60.0, "a")),
        )
        val sameCount = build(
            sleep = sleep,
            pulse = listOf(sample("2026-03-02", 1, 50.0, "b"), sample("2026-03-02", 3, 60.0, "a")),
        )

        assertEquals("b", moreReadings.of(NightlySignal.RESTING_HEART_RATE).source)
        assertEquals("a", sameCount.of(NightlySignal.RESTING_HEART_RATE).source)
    }

    @Test
    fun `of several sessions on one wake-up day the longest is the night`() {
        val nap = NightWindow(at("2026-03-02", 13), at("2026-03-02", 14))
        val result = build(
            sleep = listOf(night("2026-03-02"), nap),
            pulse = listOf(sample("2026-03-02", 2, 50.0), sample("2026-03-02", 13, 70.0)),
        )

        val points = result.of(NightlySignal.RESTING_HEART_RATE).points
        assertEquals(1, points.size)
        assertEquals(50.0, points.single().value, 0.0)
    }

    @Test
    fun `nights before the cycle start or after today are left out`() {
        val result = build(
            sleep = listOf(night("2026-02-28"), night("2026-03-11")),
            pulse = listOf(sample("2026-02-28", 2, 50.0), sample("2026-03-11", 2, 50.0)),
        )

        assertEquals(0, result.nightCount)
    }
}
