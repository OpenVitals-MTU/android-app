package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.usecase.HeartPeriodLoadResult
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartPresentationMapperTest {

    private val anchorDate = LocalDate.of(2026, 5, 10)
    private val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = anchorDate,
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )
    private val dayQuery = weekQuery.copy(range = TimeRange.DAY, anchorDate = anchorDate)

    @Test fun `average heart rate display populates week summaries`() {
        val result = HeartPeriodLoadResult(
            dailySummaries = listOf(
                HeartRateSummary(anchorDate.minusDays(1), avgBpm = 72L, minBpm = 55L, maxBpm = 110L),
                HeartRateSummary(anchorDate, avgBpm = 70L, minBpm = 48L, maxBpm = 100L),
            ),
        )

        val display = HeartPresentationMapper.build(
            query = weekQuery,
            metric = HeartMetric.AVERAGE_HEART_RATE,
            result = result,
        ).metric

        assertTrue(display.hasData)
        assertTrue(display.hasPeriodHeartRateSummaries)
        assertFalse(display.hasDayHeartRateSamples)
        assertEquals(2, display.sortedDailySummaries.size)
        assertEquals(2, display.heartRateSampleCount)
        assertEquals(2, display.heartRateTrackedDates.size)
        assertNotNull(display.heartRateRangeSummary)
    }

    @Test fun `average heart rate display has no data for empty week`() {
        val display = HeartPresentationMapper.build(
            query = weekQuery,
            metric = HeartMetric.AVERAGE_HEART_RATE,
            result = HeartPeriodLoadResult(),
        ).metric

        assertFalse(display.hasData)
        assertFalse(display.hasPeriodHeartRateSummaries)
        assertTrue(display.sortedDailySummaries.isEmpty())
    }

    @Test fun `average heart rate display populates day samples`() {
        val result = HeartPeriodLoadResult(
            daySamples = listOf(
                HeartRateSample(Instant.parse("2026-05-10T08:00:00Z"), 75L, "test"),
                HeartRateSample(Instant.parse("2026-05-10T09:00:00Z"), 80L, "test"),
            ),
        )

        val display = HeartPresentationMapper.build(
            query = dayQuery,
            metric = HeartMetric.AVERAGE_HEART_RATE,
            result = result,
        ).metric

        assertTrue(display.hasData)
        assertTrue(display.hasDayHeartRateSamples)
        assertTrue(display.showDayHeartRateTimeline)
        assertEquals(2, display.heartRateSampleCount)
        assertFalse(display.hasPeriodHeartRateSummaries)
    }

    @Test fun `average heart rate display hides timeline for single day sample`() {
        val result = HeartPeriodLoadResult(
            daySamples = listOf(
                HeartRateSample(Instant.parse("2026-05-10T08:00:00Z"), 75L, "test"),
            ),
        )

        val display = HeartPresentationMapper.build(
            query = dayQuery,
            metric = HeartMetric.AVERAGE_HEART_RATE,
            result = result,
        ).metric

        assertTrue(display.hasDayHeartRateSamples)
        assertFalse(display.showDayHeartRateTimeline)
        assertEquals(1, display.heartRateSampleCount)
    }

    @Test fun `resting heart rate display populates week trend`() {
        val result = HeartPeriodLoadResult(
            dailyRestingHR = listOf(
                DailyRestingHR(anchorDate.minusDays(1), 56L),
                DailyRestingHR(anchorDate, 58L),
            ),
        )

        val display = HeartPresentationMapper.build(
            query = weekQuery,
            metric = HeartMetric.RESTING_HEART_RATE,
            result = result,
        ).metric

        assertTrue(display.hasData)
        assertTrue(display.hasPeriodRestingRate)
        assertFalse(display.hasDayRestingRate)
        assertNotNull(display.restingRangeSummary)
        assertEquals(57L, display.restingPeriodAverageBpm)
        assertEquals(2, display.vitalsSampleCount)
        assertEquals(2, display.vitalsTrackedDates.size)
    }

    @Test fun `resting heart rate display populates day value`() {
        val result = HeartPeriodLoadResult(
            dayRestingBpm = 58L,
            previousDayRestingBpm = 60L,
            baselineDailyRestingHR = listOf(DailyRestingHR(anchorDate.minusDays(7), 57L)),
        )

        val display = HeartPresentationMapper.build(
            query = dayQuery,
            metric = HeartMetric.RESTING_HEART_RATE,
            result = result,
        ).metric

        assertTrue(display.hasData)
        assertTrue(display.hasDayRestingRate)
        assertFalse(display.hasPeriodRestingRate)
        assertNotNull(display.restingDayComparison)
        assertEquals(1, display.vitalsSampleCount)
        assertEquals(listOf(anchorDate), display.vitalsTrackedDates)
        assertEquals(1, display.restingBaselineValues.size)
    }

    @Test fun `hrv display sets hasPeriodHrv when week has readings`() {
        val result = HeartPeriodLoadResult(
            dailyHrv = listOf(
                DailyHrv(anchorDate.minusDays(1), 38.0),
                DailyHrv(anchorDate, 42.5),
            ),
        )

        val display = HeartPresentationMapper.build(
            query = weekQuery,
            metric = HeartMetric.HRV,
            result = result,
        ).metric

        assertTrue(display.hasData)
        assertTrue(display.hasPeriodHrv)
        assertFalse(display.hasDayHrv)
        assertNotNull(display.hrvRangeSummary)
    }
}
