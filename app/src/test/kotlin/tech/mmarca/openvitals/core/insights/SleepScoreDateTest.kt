package tech.mmarca.openvitals.core.insights

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage

class SleepScoreDateTest {

    @Test
    fun `date score uses previous daily sleep summaries as regularity baseline`() {
        val today = LocalDate.of(2026, 6, 8)
        val estimate = calculateSleepScoreForDate(
            selectedDate = today,
            sessions = (0L..3L).map { offset -> sleepSession(today.minusDays(offset)) },
            sleepRangeMode = SleepRangeMode.EVENING_18H,
        )

        assertEquals(SleepScoreConfidence.HIGH, estimate.confidence)
        assertEquals(0.0, estimate.regularityDifferenceMinutes ?: -1.0, 0.001)
    }

    private fun sleepSession(date: LocalDate): SleepData {
        val zone = ZoneId.systemDefault()
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
