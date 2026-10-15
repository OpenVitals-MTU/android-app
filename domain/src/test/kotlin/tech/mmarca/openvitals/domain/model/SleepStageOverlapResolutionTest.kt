package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A writer that records one stretch under two stage records made asleep time exceed the
 * session window. Every instant counts once, and the deeper stage wins.
 */
class SleepStageOverlapResolutionTest {

    // The reported night: 23:57 → 09:30, a 9h33m window.
    private val sessionStart: Instant =
        LocalDateTime.of(2026, 7, 30, 23, 57).toInstant(ZoneOffset.UTC)
    private val sessionEnd: Instant = sessionStart.plus(Duration.ofMinutes(573))

    private fun stage(type: Int, fromMin: Long, toMin: Long): SleepStage = SleepStage(
        startTime = sessionStart.plus(Duration.ofMinutes(fromMin)),
        endTime = sessionStart.plus(Duration.ofMinutes(toMin)),
        stageType = type,
    )

    private fun session(stages: List<SleepStage>): SleepData = SleepData(
        id = "s",
        startTime = sessionStart,
        endTime = sessionEnd,
        durationMs = Duration.between(sessionStart, sessionEnd).toMillis(),
        source = "coredevices.coreapp",
        stages = stages,
    )

    @Test fun `overlapping light and deep never sum past the session window`() {
        // Stage records sum to 10h in a 9h33m window because 08:00 to 09:00 is both light and deep.
        val raw = listOf(
            stage(SleepStage.STAGE_LIGHT, 0, 120),
            stage(SleepStage.STAGE_DEEP, 120, 180),
            stage(SleepStage.STAGE_REM, 180, 240),
            stage(SleepStage.STAGE_LIGHT, 240, 540),
            stage(SleepStage.STAGE_DEEP, 480, 540),
            stage(SleepStage.STAGE_AWAKE, 540, 573),
        )
        val windowMs = Duration.between(sessionStart, sessionEnd).toMillis()
        assertTrue(raw.totalStageMs() > windowMs) // the bug's shape, pre-resolution

        val resolved = resolveSleepStages(raw, sessionStart, sessionEnd)
        val asleepMs = session(resolved).asleepDurationMs()

        assertEquals(Duration.ofHours(9).toMillis(), asleepMs) // union of 0–540min
        assertTrue(asleepMs <= windowMs)
        // The disputed hour belongs to deep alone.
        assertEquals(
            Duration.ofMinutes(120).toMillis(),
            resolved.durationMsForTypes(setOf(SleepStage.STAGE_DEEP)),
        )
        assertEquals(
            Duration.ofMinutes(360).toMillis(),
            resolved.durationMsForTypes(setOf(SleepStage.STAGE_LIGHT)),
        )
        assertEquals(
            listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 120),
                stage(SleepStage.STAGE_DEEP, 120, 180),
                stage(SleepStage.STAGE_REM, 180, 240),
                stage(SleepStage.STAGE_LIGHT, 240, 480),
                stage(SleepStage.STAGE_DEEP, 480, 540),
                stage(SleepStage.STAGE_AWAKE, 540, 573),
            ),
            resolved,
        )
    }

    @Test fun `a deep interval inside a light one splits the light stage around it`() {
        val resolved = resolveSleepStages(
            listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 100),
                stage(SleepStage.STAGE_DEEP, 30, 50),
            ),
            sessionStart,
            sessionEnd,
        )
        assertEquals(
            listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 30),
                stage(SleepStage.STAGE_DEEP, 30, 50),
                stage(SleepStage.STAGE_LIGHT, 50, 100),
            ),
            resolved,
        )
    }

    @Test fun `identical duplicated records count once`() {
        val duplicated = listOf(
            stage(SleepStage.STAGE_LIGHT, 0, 60),
            stage(SleepStage.STAGE_LIGHT, 0, 60),
        )
        assertEquals(
            listOf(stage(SleepStage.STAGE_LIGHT, 0, 60)),
            resolveSleepStages(duplicated, sessionStart, sessionEnd),
        )
        // The duration helpers are union-based too, so a duplicate can never double a total.
        assertEquals(
            Duration.ofMinutes(60).toMillis(),
            duplicated.durationMsForTypes(setOf(SleepStage.STAGE_LIGHT)),
        )
        assertEquals(Duration.ofMinutes(60).toMillis(), session(duplicated).asleepDurationMs())
    }

    @Test fun `stages are clipped to the session window`() {
        val resolved = resolveSleepStages(
            listOf(
                stage(SleepStage.STAGE_LIGHT, -30, 60), // started before the session
                stage(SleepStage.STAGE_DEEP, 60, 600), // runs past the 573-minute end
            ),
            sessionStart,
            sessionEnd,
        )
        assertEquals(
            listOf(
                stage(SleepStage.STAGE_LIGHT, 0, 60),
                stage(SleepStage.STAGE_DEEP, 60, 573),
            ),
            resolved,
        )
    }

    @Test fun `a clean non-overlapping night passes through untouched`() {
        val clean = listOf(
            stage(SleepStage.STAGE_LIGHT, 0, 120),
            stage(SleepStage.STAGE_DEEP, 120, 240),
            stage(SleepStage.STAGE_AWAKE, 240, 250), // a gap 250–300 stays a gap
            stage(SleepStage.STAGE_REM, 300, 540),
        )
        assertSame(clean, resolveSleepStages(clean, sessionStart, sessionEnd))
    }
}
