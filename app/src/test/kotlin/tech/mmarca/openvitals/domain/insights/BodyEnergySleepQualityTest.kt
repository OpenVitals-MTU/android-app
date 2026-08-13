package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage

/**
 * What a night's QUALITY does to the Body Energy it charges.
 *
 * Reported from a real pair of nights: one slept well and scored 92, one slept
 * badly and scored 66, and they charged +47 and +44. Three points, for two days
 * that felt nothing alike. The charge counted the minutes and read overnight
 * HRV, and never once asked how those minutes went — so an unbroken night with
 * a full deep and REM share was worth almost exactly the same as a shallow,
 * repeatedly interrupted one of the same length.
 */
class BodyEnergySleepQualityTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val night: Instant = LocalDate.of(2026, 5, 4).atStartOfDay(zone).toInstant()

    @Test fun `a well slept night charges more than a broken one of the same length`() {
        val good = sleepChargeQualityFactor(goodNight())
        val poor = sleepChargeQualityFactor(poorNight())

        assertTrue("a good night must charge above neutral, was $good", good > 1.0)
        assertTrue("a broken night must charge below neutral, was $poor", poor < 1.0)

        // The eight hours behind these factors charge 0.10 * 480 = 48 points
        // before quality; the spread between them is what the report was about.
        val spread = (good - poor) * SleepPointsPerMinuteForTest * EightHoursOfMinutes
        assertTrue(
            "a good and a poor night should land well over ten points apart " +
                "over eight hours, were $spread",
            spread > 12.0 && spread < 22.0,
        )
    }

    @Test fun `a flawless night reads above a merely good one`() {
        // The scored sleep pillar stops distinguishing nights once they clear
        // its clinical thresholds — efficiency at 85%, twenty minutes awake —
        // so a perfect night and a good one both earned full marks and charged
        // identically. Recovery does not work that way, and the charge reads a
        // continuous quality instead.
        val flawless = sleepChargeQualityFactor(goodNight())
        val merelyGood = sleepChargeQualityFactor(decentNight())

        assertTrue(
            "a flawless night must out-charge a good one, were $flawless and $merelyGood",
            flawless > merelyGood,
        )
        val gap = (flawless - merelyGood) * SleepPointsPerMinuteForTest * EightHoursOfMinutes
        assertTrue("the gap should be visible but modest, was $gap", gap > 1.5 && gap < 6.0)

        // And both still sit above an ordinary night, rather than the pair of
        // them being dragged apart around it.
        assertTrue("a good night still charges above neutral", merelyGood > 1.0)
    }

    @Test fun `an ordinary night charges what it always did`() {
        // The factor has to be neutral on the ordinary night or it inflates
        // every night instead of telling them apart — see the constant's note.
        val ordinary = sleepChargeQualityFactor(fairNight())

        assertEquals(1.0, ordinary, 0.05)
    }

    @Test fun `the factor is bounded either side of neutral`() {
        // A single night of broken staging must not undo eight hours actually
        // slept, and a perfect one must not mint a day's worth of charge.
        val perfect = sleepChargeQualityFactor(goodNight())
        val dreadful = sleepChargeQualityFactor(worstNight())

        assertTrue("upper bound, was $perfect", perfect <= 1.2001)
        assertTrue("lower bound, was $dreadful", dreadful >= 0.7999)
    }

    @Test fun `a night with no staging charges exactly what it always did`() {
        // A source that writes only a start and an end has its sleep duration
        // EQUAL its time in bed, so efficiency reads 100% and wake time zero.
        // Read literally that is a flawless night, and it would be handed the
        // full bonus for recording nothing at all.
        val start = night.plus(Duration.ofHours(23))
        val boundsOnly = SleepData(
            id = "bounds",
            startTime = start,
            endTime = start.plus(Duration.ofHours(8)),
            durationMs = Duration.ofHours(8).toMillis(),
            source = "test",
        )

        assertEquals(1.0, sleepChargeQualityFactor(boundsOnly), 0.0)
    }

    @Test fun `a nap is too short to judge`() {
        val start = night.plus(Duration.ofHours(14))
        val nap = SleepData(
            id = "nap",
            startTime = start,
            endTime = start.plus(Duration.ofMinutes(40)),
            durationMs = Duration.ofMinutes(40).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, start.plus(Duration.ofMinutes(20)), SleepStage.STAGE_DEEP),
                SleepStage(
                    start.plus(Duration.ofMinutes(20)),
                    start.plus(Duration.ofMinutes(40)),
                    SleepStage.STAGE_LIGHT,
                ),
            ),
        )

        assertEquals(1.0, sleepChargeQualityFactor(nap), 0.0)
    }

    /** Eight hours, unbroken, with a healthy deep and REM share. */
    private fun goodNight(): SleepData {
        val start = night.plus(Duration.ofHours(23))
        val deepEnd = start.plus(Duration.ofMinutes(90))
        val remEnd = deepEnd.plus(Duration.ofMinutes(115))
        val end = start.plus(Duration.ofHours(8))
        return SleepData(
            id = "good",
            startTime = start,
            endTime = end,
            durationMs = Duration.ofHours(8).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, end, SleepStage.STAGE_LIGHT),
            ),
        )
    }

    /** Eight hours, a quarter hour awake, a slightly thin deep and REM share. */
    private fun decentNight(): SleepData {
        val start = night.plus(Duration.ofHours(23))
        val deepEnd = start.plus(Duration.ofMinutes(75))
        val remEnd = deepEnd.plus(Duration.ofMinutes(95))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(15))
        val end = start.plus(Duration.ofHours(8))
        return SleepData(
            id = "decent",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, end, SleepStage.STAGE_LIGHT),
            ),
        )
    }

    /** The ordinary night the factor is centred on: neither good nor bad. */
    private fun fairNight(): SleepData {
        val start = night.plus(Duration.ofHours(23))
        val deepEnd = start.plus(Duration.ofMinutes(50))
        val remEnd = deepEnd.plus(Duration.ofMinutes(70))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(35))
        val end = start.plus(Duration.ofHours(8))
        return SleepData(
            id = "fair",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, end, SleepStage.STAGE_LIGHT),
            ),
        )
    }

    /** The same eight hours in bed, an hour of it awake and little deep sleep. */
    private fun poorNight(): SleepData {
        val start = night.plus(Duration.ofHours(23))
        val deepEnd = start.plus(Duration.ofMinutes(25))
        val remEnd = deepEnd.plus(Duration.ofMinutes(45))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(60))
        val end = start.plus(Duration.ofHours(8))
        return SleepData(
            id = "poor",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, end, SleepStage.STAGE_LIGHT),
            ),
        )
    }

    /** Barely asleep at all: mostly awake, almost no restorative sleep. */
    private fun worstNight(): SleepData {
        val start = night.plus(Duration.ofHours(23))
        val deepEnd = start.plus(Duration.ofMinutes(5))
        val remEnd = deepEnd.plus(Duration.ofMinutes(5))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(230))
        val end = start.plus(Duration.ofHours(8))
        return SleepData(
            id = "worst",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, end, SleepStage.STAGE_LIGHT),
            ),
        )
    }

    private companion object {
        /** Mirrors the model's own rate; the constant itself is private to it. */
        const val SleepPointsPerMinuteForTest = 0.10
        const val EightHoursOfMinutes = 480.0
    }
}
