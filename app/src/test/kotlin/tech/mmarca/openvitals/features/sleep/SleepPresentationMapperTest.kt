package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `build produces duration points for each day in week period`() {
        val sessions = listOf(sleepSession(anchorDate))

        val display = SleepPresentationMapper.build(
            query = weekQuery,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(7, display.durationPoints.size)
        assertTrue(display.durationPoints.any { it.hours > 0.0 })
    }

    @Test fun `build populates daily summary for day query`() {
        val sessions = listOf(sleepSession(anchorDate))
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertNotNull(display.dailySummary)
        assertFalse(display.dailySessions.isEmpty())
    }

    @Test fun `overview summary aggregates scored nights`() {
        val sessions = listOf(sleepSession(anchorDate))
        val display = SleepPresentationMapper.build(
            query = weekQuery,
            sleepRangeMode = SleepRangeMode.EVENING_18H,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(display.overviewDays.size, display.overviewSummary.dates.size)
        assertNotNull(display.overviewSummary.sleepScore)
    }

    private fun sleepSession(endDate: LocalDate): SleepData = SleepData(
        id = "session-${endDate}",
        startTime = Instant.parse("${endDate.minusDays(1)}T22:00:00Z"),
        endTime = Instant.parse("${endDate}T06:00:00Z"),
        durationMs = 8 * 3_600_000L,
        source = "test",
        stages = listOf(
            SleepStage(
                startTime = Instant.parse("${endDate.minusDays(1)}T22:00:00Z"),
                endTime = Instant.parse("${endDate}T06:00:00Z"),
                stageType = SleepStage.STAGE_LIGHT,
            ),
        ),
    )
}
