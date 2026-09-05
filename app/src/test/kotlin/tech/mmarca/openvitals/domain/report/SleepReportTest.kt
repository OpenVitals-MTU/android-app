package tech.mmarca.openvitals.domain.report

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage

class SleepReportTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun at(dayOfMonth: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 6, dayOfMonth, hour, minute).atZone(zone).toInstant()

    private fun session(
        id: String,
        start: java.time.Instant,
        end: java.time.Instant,
        stages: List<SleepStage> = emptyList(),
    ) = SleepData(
        id = id,
        startTime = start,
        endTime = end,
        durationMs = Duration.between(start, end).toMillis(),
        source = "test",
        stages = stages,
    )

    @Test fun `no nights means no detail`() {
        assertNull(sleepDetail(emptyList(), zone))
    }

    @Test fun `naps are excluded from the table and the averages`() {
        val detail = sleepDetail(
            listOf(
                session("night", at(1, 23), at(2, 7)),
                session("nap", at(2, 14), at(2, 15)), // one hour: a nap
            ),
            zone,
        )!!

        assertEquals(1, detail.nights.size)
        assertEquals(1, detail.nightsWithData)
    }

    @Test fun `bedtimes straddling midnight average on the circle, not the number line`() {
        val detail = sleepDetail(
            listOf(
                session("a", at(1, 23, 30), at(2, 7)),
                session("b", at(3, 0, 30), at(3, 8)),
            ),
            zone,
        )!!

        // 23:30 and 00:30 average to midnight — a plain mean would say noon.
        assertEquals(0, detail.averageBedtimeMinutes)
        assertEquals(7 * 60 + 30, detail.averageWakeMinutes)
    }

    @Test fun `stage mix uses only sessions with reliable stage coverage`() {
        val stagedStart = at(1, 23)
        val staged = session(
            "staged",
            stagedStart,
            at(2, 7),
            stages = listOf(
                SleepStage(stagedStart, stagedStart.plus(Duration.ofHours(2)), SleepStage.STAGE_DEEP),
                SleepStage(
                    stagedStart.plus(Duration.ofHours(2)),
                    stagedStart.plus(Duration.ofHours(4)),
                    SleepStage.STAGE_REM,
                ),
                SleepStage(
                    stagedStart.plus(Duration.ofHours(4)),
                    stagedStart.plus(Duration.ofHours(8)),
                    SleepStage.STAGE_LIGHT,
                ),
            ),
        )
        val barelyStagedStart = at(3, 23)
        val barelyStaged = session(
            "barely",
            barelyStagedStart,
            at(4, 7),
            // One 30-minute stage over 8 hours: coverage far below the floor, so it must not pollute the mix.
            stages = listOf(
                SleepStage(barelyStagedStart, barelyStagedStart.plus(Duration.ofMinutes(30)), SleepStage.STAGE_AWAKE),
            ),
        )

        val detail = sleepDetail(listOf(staged, barelyStaged), zone)!!

        val mix = detail.stageMix!!
        assertEquals(25.0, mix.deepPct, 1e-9)
        assertEquals(25.0, mix.remPct, 1e-9)
        assertEquals(50.0, mix.lightPct, 1e-9)
        assertEquals(0.0, mix.awakePct, 1e-9)
        // The unreliable night still appears in the table — just without stage cells.
        assertEquals(2, detail.nights.size)
        assertNull(detail.nights.last().deepMs)
        assertNull(detail.nights.last().remMs)
    }

    @Test fun `no reliable stages anywhere leaves the mix null`() {
        val detail = sleepDetail(listOf(session("plain", at(1, 23), at(2, 7))), zone)!!

        assertNull(detail.stageMix)
        assertNull(detail.nights.single().deepMs)
    }

    @Test fun `a night's row carries its date, times and stage durations`() {
        val start = at(1, 23)
        val detail = sleepDetail(
            listOf(
                session(
                    "night",
                    start,
                    at(2, 7),
                    stages = listOf(
                        SleepStage(start, start.plus(Duration.ofHours(3)), SleepStage.STAGE_DEEP),
                        SleepStage(start.plus(Duration.ofHours(3)), start.plus(Duration.ofHours(8)), SleepStage.STAGE_LIGHT),
                    ),
                ),
            ),
            zone,
        )!!

        val night = detail.nights.single()
        // The night is filed under its WAKE date — that's the morning you talk about.
        assertEquals(java.time.LocalDate.of(2026, 6, 2), night.date)
        assertEquals(start, night.bedtime)
        assertEquals(Duration.ofHours(3).toMillis(), night.deepMs)
        assertEquals(0L, night.remMs)
    }

    @Test fun `circular mean of an empty list is null`() {
        assertNull(circularMeanMinutes(emptyList()))
    }
}
