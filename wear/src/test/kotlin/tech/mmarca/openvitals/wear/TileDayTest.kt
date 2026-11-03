package tech.mmarca.openvitals.wear

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WalkingEnergy
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.AscentMinute
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.HeartRateSample
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.StepMinute

class TileDayTest {

    private val zone = ZoneId.of("Europe/Amsterdam")
    private val minute = TileDay.MINUTE_MILLIS

    private fun at(text: String): Long = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun day(now: String) = TileDay.of(at(now), zone)

    @Test
    fun `a day runs from local midnight to the next in quarter hours, up to the one now is in`() {
        val day = day("2026-11-03T10:20")

        assertEquals(at("2026-11-03T00:00"), day.startMillis)
        assertEquals(at("2026-11-04T00:00"), day.endMillis)
        assertEquals(96, day.bucketsPerDay)
        assertEquals(42, day.bucketsSoFar)
    }

    @Test
    fun `the days the clocks change have 92 and 100 buckets`() {
        assertEquals(92, day("2026-03-29T12:00").bucketsPerDay)
        assertEquals(100, day("2026-10-25T12:00").bucketsPerDay)
    }

    @Test
    fun `just after midnight there is one bucket, and amounts from yesterday are left out`() {
        val day = day("2026-11-03T00:00")
        val amounts = listOf(at("2026-11-02T23:59") to 40.0, at("2026-11-03T00:00") to 10.0)

        assertEquals(listOf(10.0), day.cumulative(amounts))
    }

    @Test
    fun `the running total grows per bucket and holds where nothing happened`() {
        val day = day("2026-11-03T01:05")
        val steps = listOf(
            StepMinute(at("2026-11-03T00:01"), 10),
            StepMinute(at("2026-11-03T00:14"), 5),
            StepMinute(at("2026-11-03T00:45"), 20),
            StepMinute(at("2026-11-03T01:05"), 1),
            StepMinute(at("2026-11-03T01:06"), 99),
        )

        assertEquals(listOf(15.0, 15.0, 15.0, 35.0, 36.0), day.cumulative(TileDay.stepAmounts(steps)))
    }

    @Test
    fun `heart rate buckets are means, and a bucket without samples breaks the line`() {
        val day = day("2026-11-03T00:40")
        val samples = listOf(
            HeartRateSample(at("2026-11-03T00:01"), 60),
            HeartRateSample(at("2026-11-03T00:02"), 70),
            HeartRateSample(at("2026-11-03T00:31"), 90),
        )

        assertEquals(listOf(65, null, 90), day.meanPerBucket(samples))
    }

    @Test
    fun `a climb in decimetres counts as far as the steps around it carry it, in metres`() {
        val start = at("2026-11-03T09:00")
        val ascent = listOf(
            // 6 m with 10 + 10 steps around it: 20 steps carry 6 m, all of it.
            AscentMinute(start + minute, 60),
            // 3 m in an elevator, no steps: nothing.
            AscentMinute(start + 10 * minute, 30),
            // 9 m with 15 steps: 15 steps carry 4.5 m.
            AscentMinute(start + 20 * minute, 90),
        )
        val steps = listOf(
            StepMinute(start, 10),
            StepMinute(start + minute, 10),
            StepMinute(start + 20 * minute, 15),
        )

        assertEquals(
            listOf(start + minute to 6.0, start + 20 * minute to 4.5),
            TileDay.creditedAscent(ascent, steps),
        )
    }

    @Test
    fun `hour marks sit where the hours fell on the day the clocks go forward`() {
        // 2026-03-29 has 23 hours in Amsterdam: noon comes 11 hours after midnight.
        val marks = day("2026-03-29T20:00").hourMarks

        assertEquals(0f, marks.first(), 0f)
        assertEquals(11f / 23f, marks[2], 1e-6f)
        assertEquals(1f, marks.last(), 0f)
    }

    @Test
    fun `the bucket now is in ends at now, not at its quarter hour`() {
        val day = day("2026-11-03T10:05")
        val last = day.bucketsSoFar - 1

        assertEquals(day.fractionAt(at("2026-11-03T10:05")), day.bucketEnd(last), 0f)
        assertEquals(day.fractionAt(at("2026-11-03T10:00")), day.bucketEnd(last - 1), 0f)
        assertEquals(day.fractionAt(at("2026-11-03T10:05")), day.bucketMiddle(last), 0f)
        assertEquals(day.fractionAt(at("2026-11-03T09:52:30")), day.bucketMiddle(last - 1), 0f)
    }

    @Test
    fun `the range spans today's lowest and highest sample, yesterday's left out`() {
        val day = day("2026-11-03T12:00")
        val samples = listOf(
            HeartRateSample(at("2026-11-02T23:59"), 40),
            HeartRateSample(at("2026-11-03T06:00"), 58),
            HeartRateSample(at("2026-11-03T09:00"), 131),
            HeartRateSample(at("2026-11-03T11:00"), 72),
        )

        assertEquals(58..131, day.range(samples))
        assertEquals(null, day.range(samples.take(1)))
    }

    @Test
    fun `hour marks sit where the hours fell on the day the clocks go back`() {
        // 2026-10-25 has 25 hours in Amsterdam: noon comes 13 hours after midnight.
        val marks = day("2026-10-25T20:00").hourMarks

        assertEquals(13f / 25f, marks[2], 1e-6f)
    }

    @Test
    fun `whole counts round down, but not below a sum that only misses by its doubles`() {
        assertEquals(1L, TileDay.wholeDown(0.2 + 0.7 + 0.1))
        assertEquals(2L, TileDay.wholeDown(8.9 / 3.0))
        assertEquals(0L, TileDay.wholeDown(0.0))
    }

    @Test
    fun `a step minute walks its steps times the stride, and costs the walking energy for the weight`() {
        val minute = StepMinute(at("2026-11-03T09:00"), 100)

        assertEquals(listOf(minute.epochMillis to 72.0), TileDay.distanceAmounts(listOf(minute), strideMillimetres = 720))
        assertEquals(
            listOf(minute.epochMillis to WalkingEnergy.activeKcalPerMinute(100, 80.0)),
            TileDay.walkingKcalAmounts(listOf(minute), weightGrams = 80_000),
        )
    }

    @Test
    fun `the phone's total grows with what the watch recorded from the first minute it does not hold`() {
        val day = day("2026-11-03T16:00")
        // The phone wrote the watch's minutes through 14:00.
        val frontier = at("2026-11-03T14:01")
        val amounts = listOf(
            // Before and in the sync's minute: already in the phone's total.
            at("2026-11-03T13:30") to 500.0,
            at("2026-11-03T14:00") to 40.0,
            // After it: the watch's own, on top.
            at("2026-11-03T14:01") to 60.0,
            at("2026-11-03T15:10") to 1_000.0,
        )

        assertEquals(9_060.0, day.shownTotal(1_600.0, amounts, phoneTotal = 8_000, phoneFrontierMillis = frontier), 0.0)
    }

    @Test
    fun `the watch's own count wins when it is larger, and stands alone without a phone total`() {
        val day = day("2026-11-03T16:00")
        val amounts = listOf(at("2026-11-03T15:00") to 100.0)

        assertEquals(12_000.0, day.shownTotal(12_000.0, amounts, phoneTotal = 3_000, phoneFrontierMillis = at("2026-11-03T08:00")), 0.0)
        assertEquals(1_600.0, day.shownTotal(1_600.0, amounts, phoneTotal = null, phoneFrontierMillis = null), 0.0)
    }

    @Test
    fun `when the phone holds none of today's watch minutes, the midnight minute counts on top too`() {
        val day = day("2026-11-03T16:00")
        val amounts = listOf(at("2026-11-03T00:00") to 50.0, at("2026-11-03T09:00") to 10.0)

        assertEquals(1_060.0, day.shownTotal(60.0, amounts, phoneTotal = 1_000, phoneFrontierMillis = at("2026-11-03T00:00")), 0.0)
    }
}
