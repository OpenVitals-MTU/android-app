package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A night split by a wake between 60 min and 3 h (the screenshot's 01:22-05:18
 * | 07:34-09:38) is one night whose segments must combine into a continuous
 * stage timeline — the gap filled as Awake — or the day view hides the
 * hypnogram ("only partly staged") and the week/month bar shows a hole.
 */
class SleepSplitNightTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun t(day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 7, day, hour, minute).atZone(zone).toInstant()

    private val segment1Start = t(19, 1, 22)
    private val segment1End = t(19, 5, 18)
    private val segment2Start = t(19, 7, 34) // 2h16 wake gap
    private val segment2End = t(19, 9, 38)

    private fun stage(type: Int, from: Instant, to: Instant): SleepStage =
        SleepStage(startTime = from, endTime = to, stageType = type)

    private fun segment(
        id: String,
        start: Instant,
        end: Instant,
        stages: List<SleepStage>,
    ): SleepData = SleepData(
        id = id,
        startTime = start,
        endTime = end,
        durationMs = Duration.between(start, end).toMillis(),
        source = "nodomain.freeyourgadget",
        stages = stages,
    )

    private val segment1 = segment(
        "s1",
        segment1Start,
        segment1End,
        listOf(
            stage(
                SleepStage.STAGE_LIGHT,
                segment1Start,
                segment1Start.plus(Duration.ofHours(2).plusMinutes(58)),
            ),
            stage(
                SleepStage.STAGE_DEEP,
                segment1Start.plus(Duration.ofHours(2).plusMinutes(58)),
                segment1End,
            ),
        ),
    )

    private val segment2 = segment(
        "s2",
        segment2Start,
        segment2End,
        listOf(stage(SleepStage.STAGE_LIGHT, segment2Start, segment2End)),
    )

    @Test fun `combineNightStages fills the wake gap with Awake`() {
        val stages = combineNightStages(listOf(segment1, segment2), maxGap = SleepNapGap)

        val awake = stages.filter { it.stageType == SleepStage.STAGE_AWAKE }
        assertEquals(1, awake.size)
        assertEquals(segment1End, awake.single().startTime)
        assertEquals(segment2Start, awake.single().endTime)
        // The combined stages now span the whole night, gap included.
        assertEquals(segment1Start, stages.first().startTime)
        assertEquals(segment2End, stages.last().endTime)
    }

    @Test fun `a gap larger than maxGap (a daytime nap) is not bridged`() {
        val nap = segment(
            "nap",
            t(19, 14, 0),
            t(19, 14, 40),
            listOf(stage(SleepStage.STAGE_LIGHT, t(19, 14, 0), t(19, 14, 40))),
        )
        val stages = combineNightStages(listOf(segment1, nap), maxGap = SleepNapGap)
        // Only the night's own stages — no Awake spanning the >3h gap to the nap.
        assertFalse(stages.any { it.stageType == SleepStage.STAGE_AWAKE })
    }

    @Test fun `the split night is reliable once its gap is filled`() {
        // Before the fix the combined night spanned the gap without a stage there,
        // so coverage fell below 0.5 and the hypnogram was hidden.
        val summary = dailySleepSummary(
            listOf(segment1, segment2),
            LocalDate.of(2026, 7, 19),
            zone = zone,
        )!!
        assertTrue(
            "gap-filled stages cover the span",
            sleepSessionHasReliableStages(summary),
        )
        assertTrue(summary.stages.any { it.stageType == SleepStage.STAGE_AWAKE })
    }
}
