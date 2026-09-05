package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.SleepTargetStatus
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.sleepTargetInterpretation
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.RecordingMethod
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            sleepWindow = SleepWindow.Default,
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
            sleepWindow = SleepWindow.Default,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertNotNull(display.dailySummary)
        assertFalse(display.dailySessions.isEmpty())
    }

    @Test fun `build uses merged night instead of aggregate duration for summary and points`() {
        val sessions = listOf(
            sleepSession(anchorDate),
            sleepSession(anchorDate).copy(id = "duplicate", source = "google-fit"),
        )
        val mergedNightMs = 8 * 60 * 60 * 1000L
        val aggregateDurationMs = 5 * 60 * 60 * 1000L
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepWindow = SleepWindow.Default,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            dailyDurations = listOf(DailySleepDuration(anchorDate, aggregateDurationMs)),
            crossDailyHrv = emptyList(),
        )

        assertEquals(mergedNightMs, display.dailySummary!!.durationMs)
        assertEquals(8.0, display.durationPoints.single().hours, 0.01)
        assertEquals(mergedNightMs, display.overviewSummary.sleepDurationMs)
    }

    @Test fun `duration points are asleep hours with awake stages excluded`() {
        val awakeStart = Instant.parse("${anchorDate}T05:00:00Z")
        val session = sleepSession(anchorDate).copy(
            stages = listOf(
                SleepStage(
                    startTime = Instant.parse("${anchorDate.minusDays(1)}T22:00:00Z"),
                    endTime = awakeStart,
                    stageType = SleepStage.STAGE_LIGHT,
                ),
                SleepStage(
                    startTime = awakeStart,
                    endTime = Instant.parse("${anchorDate}T06:00:00Z"),
                    stageType = SleepStage.STAGE_AWAKE,
                ),
            ),
        )
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepWindow = SleepWindow.Default,
            sessions = listOf(session),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(7.0, display.durationPoints.single().hours, 0.01)
        assertEquals(8 * 3_600_000L, display.overviewSummary.timeInBedMs)
        assertEquals(3_600_000L, display.overviewSummary.awakeDurationMs)
    }

    @Test fun `duration points fall back to time in bed when the night has no stage data`() {
        // Nothing to subtract awake from, so the whole session counts.
        val session = localNight(anchorDate, hours = 8.5).copy(stages = emptyList())
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepWindow = SleepWindow.Default,
            sessions = listOf(session),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(8.5, display.durationPoints.single().hours, 1e-9)
    }

    @Test fun `asleep hours are zero when there is no night for the date`() {
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepWindow = SleepWindow.Default,
            sessions = emptyList(),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(0.0, display.durationPoints.single().hours, 0.0)
        assertNull(display.dailySummary)
    }

    @Test fun `an empty period derives zeroes not nulls`() {
        val display = SleepPresentationMapper.build(
            query = weekQuery,
            sleepWindow = SleepWindow.Default,
            sessions = emptyList(),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        // Every night of the week is still a point — a zero-hour one.
        assertEquals(7, display.durationPoints.size)
        assertTrue(display.durationPoints.all { it.hours == 0.0 })
        assertEquals(7, display.overviewDays.size)
        assertTrue(display.overviewDays.all { it.sessions.isEmpty() })
        assertTrue(display.dailySessions.isEmpty())
        assertNull(display.dailySummary)
        assertTrue(display.dayNaps.isEmpty())
        // No night, no reading: the score and the schedule self-hide.
        assertNull(display.overviewSummary.sleepScore)
        assertNull(display.overviewSummary.schedule)
        assertNull(display.overviewSummary.sleepEfficiencyPercent)
        assertEquals(0L, display.overviewSummary.sleepDurationMs)
        assertEquals(0L, display.overviewSummary.timeInBedMs)
        assertTrue(sleepStageShares(sleepStageDurationsOf(display.overviewSummary)).isEmpty())
        // A week with no bedtimes has nothing to put on a clock axis.
        val scheduleDays = display.overviewDays.toSleepScheduleDays()
        assertNull(SleepScheduleAxis.range(scheduleDays, ZoneId.systemDefault(), 22 * 60))
        assertFalse(useSleepScheduleChart(TimeRange.WEEK, scheduleDays, null))
    }

    @Test fun `only the nights that recorded sleep count as nights`() {
        val display = SleepPresentationMapper.build(
            query = weekQuery,
            sleepWindow = SleepWindow.Default,
            sessions = listOf(
                localNight(anchorDate, hours = 8.0),
                localNight(anchorDate.minusDays(1), hours = 6.0),
            ),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        val totals = sleepPeriodTotals(display.durationPoints)
        assertEquals(7, display.durationPoints.size)
        assertEquals(2, totals.nights)
        assertEquals(14.0, totals.totalHours, 1e-6)
        assertEquals(7.0, totals.averageHours, 1e-6)
        assertEquals(8.0, totals.longestHours, 1e-6)
    }

    @Test fun `build exposes the day's naps separately from the night`() {
        val nap = sleepSession(anchorDate).copy(
            id = "nap",
            startTime = Instant.parse("${anchorDate}T14:00:00Z"),
            endTime = Instant.parse("${anchorDate}T15:00:00Z"),
            durationMs = 3_600_000L,
            stages = emptyList(),
        )
        val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

        val display = SleepPresentationMapper.build(
            query = dayQuery,
            sleepWindow = SleepWindow.Default,
            sessions = listOf(sleepSession(anchorDate), nap),
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(listOf("nap"), display.dayNaps.map { it.id })
        assertEquals(8.0, display.durationPoints.single().hours, 0.01)
    }

    @Test fun `overview summary aggregates scored nights`() {
        val sessions = listOf(sleepSession(anchorDate))
        val display = SleepPresentationMapper.build(
            query = weekQuery,
            sleepWindow = SleepWindow.Default,
            sessions = sessions,
            previousSessions = emptyList(),
            baselineSessions = emptyList(),
            crossDailyHrv = emptyList(),
        )

        assertEquals(display.overviewDays.size, display.overviewSummary.dates.size)
        assertNotNull(display.overviewSummary.sleepScore)
    }

    /** One night ending at 07:00 LOCAL on [date], so the day bucketing is zone-proof. */
    private fun localNight(date: LocalDate, hours: Double): SleepData {
        val zone = ZoneId.systemDefault()
        val end = date.atTime(7, 0).atZone(zone).toInstant()
        val durationMs = (hours * 3_600_000).toLong()
        val start = end.minusMillis(durationMs)
        return SleepData(
            id = "night-$date",
            startTime = start,
            endTime = end,
            durationMs = durationMs,
            source = "test",
            stages = listOf(SleepStage(start, end, SleepStage.STAGE_LIGHT)),
        )
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
