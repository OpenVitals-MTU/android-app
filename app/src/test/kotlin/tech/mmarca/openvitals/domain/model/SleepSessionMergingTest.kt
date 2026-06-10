package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepSessionMergingTest {

    @Test fun `mergeSleepSessions merges same-source sessions separated by a short gap`() {
        val first = sleep(
            id = "first",
            start = "2026-05-06T00:22:00Z",
            end = "2026-05-06T04:07:00Z",
            stages = listOf(stage("2026-05-06T00:22:00Z", "2026-05-06T04:07:00Z", SleepStage.STAGE_LIGHT)),
        )
        val second = sleep(
            id = "second",
            start = "2026-05-06T04:22:00Z",
            end = "2026-05-06T07:03:00Z",
            stages = listOf(stage("2026-05-06T04:22:00Z", "2026-05-06T07:03:00Z", SleepStage.STAGE_REM)),
        )

        val merged = mergeSleepSessions(listOf(second, first))

        assertEquals(1, merged.size)
        assertEquals(first.startTime, merged.single().startTime)
        assertEquals(second.endTime, merged.single().endTime)
        assertEquals(first.durationMs + second.durationMs, merged.single().durationMs)
        assertNotEquals("first", merged.single().id)
        assertEquals(listOf("first", "second"), mergedSleepSessionComponentIds(merged.single().id))
        assertEquals(
            listOf(SleepStage.STAGE_LIGHT, SleepStage.STAGE_AWAKE, SleepStage.STAGE_REM),
            merged.single().stages.map { it.stageType },
        )
    }

    @Test fun `mergeSleepSessions merges same-source sessions separated by up to sixty minutes`() {
        val first = sleep(
            id = "first",
            start = "2026-05-06T00:22:00Z",
            end = "2026-05-06T04:07:00Z",
        )
        val second = sleep(
            id = "second",
            start = "2026-05-06T05:07:00Z",
            end = "2026-05-06T07:03:00Z",
        )

        val merged = mergeSleepSessions(listOf(first, second))

        assertEquals(1, merged.size)
        assertEquals(first.startTime, merged.single().startTime)
        assertEquals(second.endTime, merged.single().endTime)
    }

    @Test fun `mergeSleepSessions carries a pre-midnight split into the final sleep-ending day`() {
        val first = sleep(
            id = "before-midnight",
            start = "2026-05-05T22:45:00Z",
            end = "2026-05-05T23:59:00Z",
        )
        val second = sleep(
            id = "after-midnight",
            start = "2026-05-06T00:03:00Z",
            end = "2026-05-06T06:50:00Z",
        )

        val merged = mergeSleepSessions(listOf(first, second))

        assertEquals(1, merged.size)
        assertEquals(Instant.parse("2026-05-05T22:45:00Z"), merged.single().startTime)
        assertEquals(Instant.parse("2026-05-06T06:50:00Z"), merged.single().endTime)
    }

    @Test fun `mergeSleepSessions excludes bridged gaps from displayed sleep duration`() {
        val first = sleep(
            id = "first",
            start = "2026-05-06T00:00:00Z",
            end = "2026-05-06T04:00:00Z",
        )
        val second = sleep(
            id = "second",
            start = "2026-05-06T04:30:00Z",
            end = "2026-05-06T06:30:00Z",
        )

        val merged = mergeSleepSessions(listOf(first, second))

        assertEquals(1, merged.size)
        assertEquals(Duration.ofHours(6).toMillis(), merged.single().durationMs)
        assertEquals(
            Duration.ofHours(6).plusMinutes(30).toMillis(),
            Duration.between(first.startTime, second.endTime).toMillis(),
        )
    }

    @Test fun `sleepDurationMsFromStages excludes awake stages when sleep stages are present`() {
        val stages = listOf(
            stage("2026-05-06T00:00:00Z", "2026-05-06T04:00:00Z", SleepStage.STAGE_LIGHT),
            stage("2026-05-06T04:00:00Z", "2026-05-06T04:30:00Z", SleepStage.STAGE_AWAKE),
            stage("2026-05-06T04:30:00Z", "2026-05-06T06:30:00Z", SleepStage.STAGE_REM),
        )

        val durationMs = sleepDurationMsFromStages(
            stages = stages,
            fallbackDurationMs = Duration.ofHours(6).plusMinutes(30).toMillis(),
        )

        assertEquals(Duration.ofHours(6).toMillis(), durationMs)
    }

    @Test fun `mergeSleepSessions does not merge sessions from different sources`() {
        val first = sleep(
            id = "gadgetbridge",
            source = "gadgetbridge",
            start = "2026-05-06T00:22:00Z",
            end = "2026-05-06T04:07:00Z",
        )
        val second = sleep(
            id = "watch",
            source = "watch",
            start = "2026-05-06T04:22:00Z",
            end = "2026-05-06T07:03:00Z",
        )

        val merged = mergeSleepSessions(listOf(first, second))

        assertEquals(2, merged.size)
    }

    @Test fun `mergeSleepSessions does not merge sessions beyond the max gap`() {
        val first = sleep(
            id = "nap",
            start = "2026-05-06T18:00:00Z",
            end = "2026-05-06T19:00:00Z",
        )
        val second = sleep(
            id = "night",
            start = "2026-05-06T22:30:00Z",
            end = "2026-05-07T06:30:00Z",
        )

        val merged = mergeSleepSessions(listOf(first, second))

        assertEquals(2, merged.size)
    }

    @Test fun `mergedSleepSessionComponentIds returns null for raw or invalid ids`() {
        assertNull(mergedSleepSessionComponentIds("raw-id"))
        assertNull(mergedSleepSessionComponentIds("merged:not valid base64"))
    }

    @Test fun `mergedSleepSessionComponentIds returns encoded raw ids`() {
        val merged = mergeSleepSessions(
            listOf(
                sleep(id = "alpha"),
                sleep(id = "beta", start = "2026-05-06T02:10:00Z", end = "2026-05-06T03:00:00Z"),
            ),
            maxGap = Duration.ofHours(2),
        ).single()

        assertNotNull(mergedSleepSessionComponentIds(merged.id))
        assertTrue(
            mergedSleepSessionComponentIds(merged.id).orEmpty().containsAll(listOf("alpha", "beta")),
        )
    }

    private fun sleep(
        id: String,
        source: String = "gadgetbridge",
        start: String = "2026-05-06T01:00:00Z",
        end: String = "2026-05-06T02:00:00Z",
        stages: List<SleepStage> = emptyList(),
    ) = SleepData(
        id = id,
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        durationMs = Duration.between(Instant.parse(start), Instant.parse(end)).toMillis(),
        source = source,
        stages = stages,
    )

    private fun stage(start: String, end: String, type: Int) = SleepStage(
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        stageType = type,
    )
}
