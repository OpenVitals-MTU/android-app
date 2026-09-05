package tech.mmarca.openvitals.features.sleep

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage

/** `toSleepScheduleDays`: one schedule bar per overview day, with no night leaving a bedtime behind. */
class SleepScheduleDaysTest {

    private fun at(day: Int, hour: Int, minute: Int = 0): Instant =
        Instant.parse("2026-07-%02dT%02d:%02d:00Z".format(day, hour, minute))

    private fun stage(start: Instant, end: Instant, type: Int) =
        SleepStage(startTime = start, endTime = end, stageType = type)

    private fun session(
        start: Instant,
        end: Instant,
        stages: List<SleepStage> = emptyList(),
    ) = SleepData(
        id = "${start.toEpochMilli()}",
        startTime = start,
        endTime = end,
        durationMs = end.toEpochMilli() - start.toEpochMilli(),
        source = "test",
        stages = stages,
    )

    private fun day(date: String, sessions: List<SleepData> = emptyList()) = SleepOverviewDay(
        date = LocalDate.parse(date),
        sessions = sessions,
    )

    @Test fun `maps a merged night to its span and stages`() {
        val night = session(
            at(5, 23, 30),
            at(6, 7),
            stages = listOf(
                stage(at(5, 23, 30), at(6, 3), SleepStage.STAGE_DEEP),
                stage(at(6, 3), at(6, 7), SleepStage.STAGE_REM),
            ),
        )

        val days = listOf(day("2026-07-05", listOf(night))).toSleepScheduleDays()

        assertEquals(1, days.size)
        assertEquals(at(5, 23, 30), days.single().inBedStart)
        assertEquals(at(6, 7), days.single().inBedEnd)
        assertEquals(
            listOf(SleepStage.STAGE_DEEP, SleepStage.STAGE_REM),
            days.single().stages.map { it.stageType },
        )
    }

    @Test fun `a night with no stages carries an empty stage list`() {
        val days = listOf(
            day("2026-07-05", listOf(session(at(5, 23), at(6, 7)))),
        ).toSleepScheduleDays()

        assertEquals(at(5, 23), days.single().inBedStart)
        assertTrue(days.single().stages.isEmpty())
    }

    @Test fun `a date with no night has no bedtime`() {
        val days = listOf(day("2026-07-05")).toSleepScheduleDays()

        assertNull(days.single().inBedStart)
        assertNull(days.single().inBedEnd)
        assertTrue(days.single().stages.isEmpty())
    }

    @Test fun `days come out in date order`() {
        val days = listOf(
            day("2026-07-05", listOf(session(at(5, 23), at(6, 7)))),
            day("2026-07-06", listOf(session(at(6, 23), at(7, 7)))),
        ).toSleepScheduleDays()

        assertEquals(
            listOf(LocalDate.parse("2026-07-05"), LocalDate.parse("2026-07-06")),
            days.map { it.date },
        )
    }

    @Test fun `a wake-split night spans the earliest start to the latest end`() {
        val days = listOf(
            day(
                "2026-07-05",
                listOf(
                    session(at(5, 2), at(5, 6)),
                    session(at(4, 23), at(5, 1)),
                ),
            ),
        ).toSleepScheduleDays()

        assertEquals(at(4, 23), days.single().inBedStart)
        assertEquals(at(5, 6), days.single().inBedEnd)
    }
}
