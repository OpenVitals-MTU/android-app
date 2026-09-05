package tech.mmarca.openvitals.features.sleep

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepStage

/** The fraction-to-clock-time mapping and the stage lookup under the finger. */
class SleepStageScrubberTest {

    // A night that straddles midnight: 470 minutes from 23:15 to 07:05.
    private val bedtime: Instant = Instant.parse("2026-06-21T23:15:00Z")
    private val wakeUp: Instant = Instant.parse("2026-06-22T07:05:00Z")
    private val totalMs = Duration.between(bedtime, wakeUp).toMillis()

    private fun stage(type: Int, startMinute: Long, endMinute: Long) = SleepStage(
        startTime = bedtime.plus(Duration.ofMinutes(startMinute)),
        endTime = bedtime.plus(Duration.ofMinutes(endMinute)),
        stageType = type,
    )

    private val night = listOf(
        stage(SleepStage.STAGE_AWAKE, 0, 10),
        stage(SleepStage.STAGE_LIGHT, 10, 145),
        stage(SleepStage.STAGE_DEEP, 145, 275),
        stage(SleepStage.STAGE_REM, 275, 355),
        stage(SleepStage.STAGE_LIGHT, 355, 470),
    )

    @Test fun `a horizontal drag reveals the clock time and stage at the finger`() {
        // A quarter across: 01:12:30, in the Light block (10 to 145 min).
        val time = sleepScrubTimeAt(bedtime, totalMs, 0.25f)

        assertEquals(Instant.parse("2026-06-22T01:12:30Z"), time)
        assertEquals(SleepStage.STAGE_LIGHT, sleepStageTypeAt(night, time))
    }

    @Test fun `the time tracks the finger across the night`() {
        assertEquals(bedtime, sleepScrubTimeAt(bedtime, totalMs, 0f))
        assertEquals(
            Instant.parse("2026-06-22T03:10:00Z"),
            sleepScrubTimeAt(bedtime, totalMs, 0.5f),
        )
        // Three-quarters across: 05:07:30, in the REM block (275 to 355 min).
        val threeQuarters = sleepScrubTimeAt(bedtime, totalMs, 0.75f)
        assertEquals(Instant.parse("2026-06-22T05:07:30Z"), threeQuarters)
        assertEquals(SleepStage.STAGE_REM, sleepStageTypeAt(night, threeQuarters))
        assertEquals(wakeUp, sleepScrubTimeAt(bedtime, totalMs, 1f))
    }

    @Test fun `a stage owns its start and its neighbour owns the boundary`() {
        val boundary = bedtime.plus(Duration.ofMinutes(145))

        assertEquals(SleepStage.STAGE_DEEP, sleepStageTypeAt(night, boundary))
        assertEquals(
            SleepStage.STAGE_LIGHT,
            sleepStageTypeAt(night, boundary.minusMillis(1)),
        )
    }

    @Test fun `a scrub landing in a gap reads no stage at all`() {
        val gapped = listOf(
            stage(SleepStage.STAGE_LIGHT, 0, 60),
            stage(SleepStage.STAGE_DEEP, 120, 180),
        )

        assertNull(sleepStageTypeAt(gapped, bedtime.plus(Duration.ofMinutes(90))))
        // …and so does one past the end of the night.
        assertNull(sleepStageTypeAt(night, wakeUp))
    }
}
