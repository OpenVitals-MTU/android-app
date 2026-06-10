package tech.mmarca.openvitals.features.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.dailySleepSummary

class SleepDailySummaryTest {

    @Test fun `dailySleepSummary sums separate sessions ending on the selected date`() {
        val zone = ZoneId.of("UTC")
        val earlySleep = sleep(
            id = "early",
            start = "2026-05-03T00:00:00Z",
            end = "2026-05-03T09:38:00Z",
            duration = Duration.ofHours(8).plusMinutes(15),
            stages = listOf(
                stage("2026-05-03T00:00:00Z", "2026-05-03T04:21:00Z", SleepStage.STAGE_LIGHT),
                stage("2026-05-03T04:21:00Z", "2026-05-03T06:56:00Z", SleepStage.STAGE_DEEP),
                stage("2026-05-03T06:56:00Z", "2026-05-03T08:15:00Z", SleepStage.STAGE_REM),
            ),
        )
        val eveningSleep = sleep(
            id = "evening",
            start = "2026-05-03T21:46:00Z",
            end = "2026-05-03T22:22:00Z",
            duration = Duration.ofMinutes(36),
            stages = listOf(
                stage("2026-05-03T21:46:00Z", "2026-05-03T22:22:00Z", SleepStage.STAGE_LIGHT),
            ),
        )
        val nextDaySleep = sleep(
            id = "next-day",
            start = "2026-05-04T01:11:00Z",
            end = "2026-05-04T08:13:00Z",
            duration = Duration.ofHours(7).plusMinutes(3),
        )

        val summary = dailySleepSummary(
            sessions = listOf(nextDaySleep, eveningSleep, earlySleep),
            selectedDate = LocalDate.of(2026, 5, 3),
            sleepRangeMode = SleepRangeMode.ROLLING_24H,
            zone = zone,
        )

        assertNotNull(summary)
        assertEquals(earlySleep.startTime, summary!!.startTime)
        assertEquals(eveningSleep.endTime, summary.endTime)
        assertEquals(Duration.ofHours(8).plusMinutes(51).toMillis(), summary.durationMs)
        assertEquals(4, summary.stages.size)
    }

    @Test fun `dailySleepSummary keeps the following early morning sleep on its own date`() {
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
            duration = Duration.ofHours(7).plusMinutes(3),
        )

        val summary = dailySleepSummary(
            sessions = listOf(eveningSleep, nextDaySleep),
            selectedDate = LocalDate.of(2026, 5, 4),
            sleepRangeMode = SleepRangeMode.ROLLING_24H,
            zone = zone,
        )

        assertEquals(nextDaySleep, summary)
    }

    @Test fun `dailySleepSummary can assign previous evening sleep to the selected date`() {
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
            duration = Duration.ofHours(7).plusMinutes(3),
        )

        val summary = dailySleepSummary(
            sessions = listOf(eveningSleep, nextDaySleep),
            selectedDate = LocalDate.of(2026, 5, 4),
            zone = zone,
        )

        assertNotNull(summary)
        assertEquals(eveningSleep.startTime, summary!!.startTime)
        assertEquals(nextDaySleep.endTime, summary.endTime)
        assertEquals(Duration.ofHours(7).plusMinutes(39).toMillis(), summary.durationMs)
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
