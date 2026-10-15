package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [sleepSessionHasReliableStages]: a partly staged night gets a note instead of a near-empty hypnogram. */
class SleepStageCoverageTest {

    private val start: Instant =
        LocalDateTime.of(2026, 7, 18, 0, 20).toInstant(ZoneOffset.UTC)
    private val end: Instant =
        LocalDateTime.of(2026, 7, 18, 8, 14).toInstant(ZoneOffset.UTC) // a 7h54m night

    private fun session(stages: List<SleepStage>): SleepData = SleepData(
        id = "s",
        startTime = start,
        endTime = end,
        durationMs = Duration.between(start, end).toMillis(),
        source = "test",
        stages = stages,
    )

    private fun stage(type: Int, fromMin: Long, toMin: Long): SleepStage = SleepStage(
        startTime = start.plus(Duration.ofMinutes(fromMin)),
        endTime = start.plus(Duration.ofMinutes(toMin)),
        stageType = type,
    )

    @Test fun `a fully-staged night is reliable`() {
        // Stages wall-to-wall across the 474-minute span.
        val full = session(
            listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 120),
                stage(SleepStage.STAGE_DEEP, 120, 240),
                stage(SleepStage.STAGE_REM, 240, 360),
                stage(SleepStage.STAGE_LIGHT, 360, 474),
            ),
        )
        assertTrue(sleepSessionHasReliableStages(full))
    }

    @Test fun `a tail-only session is not reliable`() {
        // The real symptom: an 8h span with stages only in the last ~90 minutes.
        val partial = session(listOf(stage(SleepStage.STAGE_LIGHT, 384, 466))) // 06:44-08:06
        assertFalse(sleepSessionHasReliableStages(partial))
    }

    @Test fun `a session with no stages is not reliable`() {
        assertFalse(sleepSessionHasReliableStages(session(emptyList())))
    }

    @Test fun `coverage is measured against the span, not the stages own extent`() {
        // Stages that together cover just over half the night pass; just under, fail.
        val justOver = session(listOf(stage(SleepStage.STAGE_LIGHT, 0, 240))) // ~51%
        val justUnder = session(listOf(stage(SleepStage.STAGE_LIGHT, 0, 230))) // ~49%
        assertTrue(sleepSessionHasReliableStages(justOver))
        assertFalse(sleepSessionHasReliableStages(justUnder))
    }

    @Test fun `a zero-length session never divides by zero`() {
        val instant = SleepData(
            id = "s",
            startTime = start,
            endTime = start,
            durationMs = 0L,
            source = "test",
            stages = listOf(stage(SleepStage.STAGE_LIGHT, 0, 0)),
        )
        assertFalse(sleepSessionHasReliableStages(instant))
    }
}
