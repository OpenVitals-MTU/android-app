package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.SleepWindow

class SleepScoreDateTest {

    @Test
    fun `date score uses previous daily sleep summaries as regularity baseline`() {
        val today = LocalDate.of(2026, 6, 8)
        val estimate = calculateSleepScoreForDate(
            selectedDate = today,
            sessions = (0L..3L).map { offset -> sleepSession(today.minusDays(offset)) },
            sleepWindow = SleepWindow.Default,
        )

        assertEquals(SleepScoreConfidence.MEDIUM, estimate.confidence)
        assertEquals(0.0, estimate.regularityDifferenceMinutes ?: -1.0, 0.001)
        assertFalse(estimate.usesOvernightHrv)
    }

    @Test
    fun `adult duration target scores full credit inside seven to nine hours`() {
        assertEquals(40.0, durationPoints(8.0, sleepDurationTargetForAge(30)), 0.001)
        assertTrue(durationPoints(6.0, sleepDurationTargetForAge(30)) < 40.0)
        assertTrue(durationPoints(10.0, sleepDurationTargetForAge(30)) < 40.0)
    }

    @Test
    fun `older adult duration target peaks at seven to eight hours`() {
        val target = sleepDurationTargetForAge(70)
        assertEquals(7.0, target.idealMinHours, 0.001)
        assertEquals(8.0, target.idealMaxHours, 0.001)
        assertEquals(40.0, durationPoints(7.5, target), 0.001)
        assertTrue(durationPoints(9.0, target) < 40.0)
    }

    @Test
    fun `efficiency and continuity follow NSF quality thresholds`() {
        assertEquals(15.0, efficiencyPoints(85.0), 0.001)
        assertEquals(0.0, efficiencyPoints(65.0), 0.001)
        assertEquals(15.0, continuityPoints(20.0), 0.001)
        assertEquals(0.0, continuityPoints(90.0), 0.001)
    }

    @Test
    fun `overnight recovery is full when RMSSD meets baseline`() {
        assertEquals(20.0, recoveryPoints(50.0, 50.0), 0.001)
        assertTrue(recoveryPoints(35.0, 50.0) < 20.0)
        assertTrue(recoveryPoints(20.0, 50.0) < recoveryPoints(35.0, 50.0))
    }

    @Test
    fun `overnight HRV inputs require a personal baseline night`() {
        val today = LocalDate.of(2026, 6, 8)
        val zone = ZoneId.of("UTC")
        val sessions = listOf(
            sleepSession(today.minusDays(1), zone),
            sleepSession(today, zone),
        )
        val samples = listOf(
            HrvSample(time = sessions[0].startTime.plusSeconds(3600), rmssdMs = 40.0, source = "test"),
            HrvSample(time = sessions[1].startTime.plusSeconds(3600), rmssdMs = 48.0, source = "test"),
        )

        val inputs = overnightHrvInputsByDate(
            sessions = sessions,
            hrvSamples = samples,
            start = today.minusDays(1),
            end = today,
            zone = zone,
        )

        assertFalse(inputs.containsKey(today.minusDays(1)))
        assertEquals(48.0, inputs.getValue(today).rmssdMs, 0.001)
        assertEquals(40.0, inputs.getValue(today).baselineRmssdMs, 0.001)
    }

    @Test
    fun `high confidence requires overnight HRV with staged sleep and regularity`() {
        val today = LocalDate.of(2026, 6, 8)
        val sessions = (0L..3L).map { offset -> sleepSession(today.minusDays(offset)) }
        val estimate = calculateSleepScore(
            session = sessions.first(),
            previousSessions = sessions.drop(1),
            overnightHrv = OvernightHrvInput(rmssdMs = 50.0, baselineRmssdMs = 48.0),
        )

        assertEquals(SleepScoreConfidence.HIGH, estimate.confidence)
        assertTrue(estimate.usesOvernightHrv)
        assertEquals(20.0, estimate.recoveryPoints, 0.001)
    }

    private fun sleepSession(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): SleepData {
        val end = date.atTime(7, 0).atZone(zone).toInstant()
        val start = end.minus(Duration.ofHours(8))
        val deepEnd = start.plus(Duration.ofHours(2))
        val remEnd = deepEnd.plus(Duration.ofMinutes(90))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(30))
        val lightEnd = awakeEnd.plus(Duration.ofHours(4))
        return SleepData(
            id = "sleep-$date",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, lightEnd, SleepStage.STAGE_LIGHT),
            ),
        )
    }
}
