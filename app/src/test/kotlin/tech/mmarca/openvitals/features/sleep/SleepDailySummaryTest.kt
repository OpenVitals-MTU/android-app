package tech.mmarca.openvitals.features.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MERGED_NIGHT_ID_PREFIX
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.dailySleepSummary

class SleepDailySummaryTest {

    @Test fun `dailySleepSummary merges a wake-split night into one union summary`() {
        val zone = ZoneId.of("UTC")
        val firstSegment = sleep(
            id = "first",
            start = "2026-05-03T00:00:00Z",
            end = "2026-05-03T04:21:00Z",
            duration = Duration.ofHours(4).plusMinutes(21),
            stages = listOf(
                stage("2026-05-03T00:00:00Z", "2026-05-03T02:00:00Z", SleepStage.STAGE_LIGHT),
                stage("2026-05-03T02:00:00Z", "2026-05-03T03:30:00Z", SleepStage.STAGE_DEEP),
                stage("2026-05-03T03:30:00Z", "2026-05-03T04:21:00Z", SleepStage.STAGE_REM),
            ),
        )
        val secondSegment = sleep(
            id = "second",
            start = "2026-05-03T05:00:00Z",
            end = "2026-05-03T09:38:00Z",
            duration = Duration.ofHours(4).plusMinutes(38),
            stages = listOf(
                stage("2026-05-03T05:00:00Z", "2026-05-03T09:38:00Z", SleepStage.STAGE_LIGHT),
            ),
        )
        val nextDaySleep = sleep(
            id = "next-day",
            start = "2026-05-04T01:11:00Z",
            end = "2026-05-04T08:13:00Z",
            duration = Duration.ofHours(7).plusMinutes(2),
        )

        val summary = dailySleepSummary(
            sessions = listOf(nextDaySleep, secondSegment, firstSegment),
            selectedDate = LocalDate.of(2026, 5, 3),
            zone = zone,
        )

        assertNotNull(summary)
        assertEquals("${MERGED_NIGHT_ID_PREFIX}2026-05-03", summary!!.id)
        assertEquals(firstSegment.startTime, summary.startTime)
        assertEquals(secondSegment.endTime, summary.endTime)
        assertEquals(Duration.ofHours(8).plusMinutes(59).toMillis(), summary.durationMs)
        // 3 + 1 real stages plus one gap-filled awake stage between the segments.
        assertEquals(5, summary.stages.size)
    }

    @Test fun `dailySleepSummary keeps early morning sleep on its own date`() {
        val zone = ZoneId.of("UTC")
        val previousNight = sleep(
            id = "previous-night",
            start = "2026-05-03T01:00:00Z",
            end = "2026-05-03T08:00:00Z",
            duration = Duration.ofHours(7),
        )
        val earlyMorning = sleep(
            id = "early-morning",
            start = "2026-05-04T02:00:00Z",
            end = "2026-05-04T08:00:00Z",
            duration = Duration.ofHours(6),
        )

        val summary = dailySleepSummary(
            sessions = listOf(previousNight, earlyMorning),
            selectedDate = LocalDate.of(2026, 5, 4),
            zone = zone,
        )

        assertEquals(earlyMorning, summary)
    }

    @Test fun `dailySleepSummary assigns previous evening sleep to the wake-up date`() {
        val zone = ZoneId.of("UTC")
        val eveningSleep = sleep(
            id = "evening",
            start = "2026-05-03T21:46:00Z",
            end = "2026-05-03T22:22:00Z",
            duration = Duration.ofMinutes(36),
        )
        val nextDaySleep = sleep(
            id = "next-day",
            start = "2026-05-04T01:11:00Z",
            end = "2026-05-04T08:13:00Z",
            duration = Duration.ofHours(7).plusMinutes(2),
        )

        val summary = dailySleepSummary(
            sessions = listOf(eveningSleep, nextDaySleep),
            selectedDate = LocalDate.of(2026, 5, 4),
            zone = zone,
        )

        assertNotNull(summary)
        assertEquals(eveningSleep.startTime, summary!!.startTime)
        assertEquals(nextDaySleep.endTime, summary.endTime)
        assertEquals(Duration.ofHours(7).plusMinutes(38).toMillis(), summary.durationMs)
    }

    private fun sleep(
        id: String,
        start: String,
        end: String,
        duration: Duration,
        stages: List<SleepStage> = emptyList(),
    ) = SleepData(
        id = id,
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        durationMs = duration.toMillis(),
        source = "gadgetbridge",
        stages = stages,
    )

    private fun stage(start: String, end: String, type: Int) = SleepStage(
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        stageType = type,
    )
}
