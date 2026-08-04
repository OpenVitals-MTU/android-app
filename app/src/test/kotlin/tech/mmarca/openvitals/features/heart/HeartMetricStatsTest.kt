package tech.mmarca.openvitals.features.heart

import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.stats.averageOrNull
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.vitals.restingHeartRateValue
import tech.mmarca.openvitals.features.vitals.skinTemperatureChartEntries

/**
 * Port of test/features/heart/heart_display_test.dart's inline-stats cases: the
 * arithmetic the composables used to hide is hoisted into pure functions
 * (heartRateTimelineStats, bloodPressureStats, spO2Stats, skinTemperatureStats,
 * skinTemperatureChartEntries) so a unit test can call it with a fixture.
 */
class HeartMetricStatsTest {

    private val formatter = UnitFormatter(
        unitSystemProvider = { UnitSystem.METRIC },
        localeProvider = { Locale.US },
    )

    private fun sample(hour: Int, bpm: Long) = HeartRateSample(
        time = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        beatsPerMinute = bpm,
        source = "Test",
    )

    private fun restingSample(hour: Int, bpm: Long) = RestingHeartRateSample(
        time = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        beatsPerMinute = bpm,
        source = "Test",
    )

    private fun bp(hour: Int, systolic: Int, diastolic: Int) = BloodPressureEntry(
        time = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        systolicMmHg = systolic,
        diastolicMmHg = diastolic,
        source = "Test",
    )

    private fun spO2(hour: Int, percent: Double) = SpO2Entry(
        time = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        percent = percent,
        source = "Test",
    )

    private fun skin(hour: Int, delta: Double?) = SkinTemperatureEntry(
        startTime = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        endTime = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        baselineCelsius = 33.0,
        averageDeltaCelsius = delta,
        minDeltaCelsius = delta,
        maxDeltaCelsius = delta,
        measurementLocation = 0,
        source = "Test",
    )

    // ─── heart rate ───────────────────────────────────────────────────────────

    @Test fun `a day of samples sorts oldest first and takes its extremes`() {
        val stats = heartRateTimelineStats(
            listOf(sample(14, 90L), sample(6, 52L), sample(9, 71L)),
        )

        assertEquals(listOf(52L, 71L, 90L), stats.sorted.map { it.beatsPerMinute })
        assertEquals(52L, stats.minBpm)
        assertEquals(90L, stats.maxBpm)
        assertEquals(71, stats.avgBpm)
        assertEquals(3, stats.sorted.size)
        // The intraday axis is floored at 30 and padded by 5 either side.
        assertEquals(47L, stats.paddedMin)
        assertEquals(95L, stats.paddedMax)
    }

    @Test fun `the intraday axis floors at 30 never at min minus five below it`() {
        val low = heartRateTimelineStats(listOf(sample(6, 33L), sample(7, 40L)))
        // 33 - 5 = 28, below the plausible-resting floor: clamped to 30.
        assertEquals(30L, low.paddedMin)
        assertEquals(45L, low.paddedMax)

        val normal = heartRateTimelineStats(listOf(sample(6, 60L), sample(7, 90L)))
        assertEquals(55L, normal.paddedMin)
        assertEquals(95L, normal.paddedMax)
    }

    // ─── vitals ───────────────────────────────────────────────────────────────

    @Test fun `blood pressure keeps the latest reading and the highest one`() {
        val stats = bloodPressureStats(
            listOf(bp(9, 140, 90), bp(18, 120, 80), bp(7, 118, 76)),
        )!!

        assertEquals(120, stats.latest.systolicMmHg)
        assertEquals(140, stats.highest.systolicMmHg)
        assertEquals(126.0, stats.averageSystolic, 0.5)
        assertEquals(82.0, stats.averageDiastolic, 0.5)
        assertEquals(3, stats.readings)
    }

    @Test fun `blood pressure highest tie breaks on the diastolic`() {
        val stats = bloodPressureStats(listOf(bp(8, 140, 90), bp(9, 140, 95)))!!
        assertEquals(95, stats.highest.diastolicMmHg)
    }

    @Test fun `blood pressure with no readings has no statistics`() {
        assertNull(bloodPressureStats(emptyList()))
    }

    @Test fun `spO2 averages every reading and keeps its extremes`() {
        val entries = listOf(spO2(20, 95.0), spO2(2, 99.0))
        val stats = spO2Stats(entries)!!

        assertEquals(97.0, stats.average, 1e-9)
        assertEquals(95.0, stats.low, 1e-9)
        assertEquals(99.0, stats.high, 1e-9)
        assertEquals(2, stats.readings)
        // The previous-period comparison compares average against average, the
        // way SpO2StatisticsContent builds it.
        val previousAverage = listOf(spO2(2, 97.0)).map { it.percent }.averageOrNull()!!
        val comparison = periodComparison(stats.average, previousAverage)
        assertEquals(97.0, comparison.currentValue, 1e-9)
        assertEquals(97.0, comparison.previousValue, 1e-9)
    }

    @Test fun `skin temperature excludes deltaless entries from the arithmetic`() {
        val entries = listOf(skin(8, 0.4), skin(20, null), skin(2, -0.2))

        val chartEntries = skinTemperatureChartEntries(entries)
        assertEquals(2, chartEntries.size)
        assertEquals(listOf(-0.2, 0.4), chartEntries.map { it.averageDeltaCelsius })

        val stats = skinTemperatureStats(entries)!!
        assertEquals(0.1, stats.average, 1e-4)
        assertEquals(-0.2, stats.low, 1e-9)
        assertEquals(0.4, stats.high, 1e-9)
        // …but delta-less entries still count as readings, which is what the
        // screen prints.
        assertEquals(3, stats.readings)
    }

    @Test fun `skin temperature with no delta anywhere has no statistics at all`() {
        val entries = listOf(skin(8, null))

        assertTrue(skinTemperatureChartEntries(entries).isEmpty())
        assertNull(skinTemperatureStats(entries))
    }

    // ─── a day average never sits outside its own range ──────────────────────

    @Test fun `resting heart rate averages the samples it also ranges`() {
        // The provider aggregate disagrees with its own samples. It loses: the
        // overview card averages the samples, the timeline card ranges them —
        // one population, printed side by side.
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            dayRestingSamples = listOf(restingSample(8, 50L), restingSample(9, 60L)),
            dayRestingBpm = 70L,
        )

        assertEquals("55", state.restingHeartRateValue(formatter)?.value)

        val stats = heartRateTimelineStats(
            state.dayRestingSamples.map {
                HeartRateSample(it.time, it.beatsPerMinute, it.source)
            },
        )
        assertEquals(55, stats.avgBpm)
        assertEquals(50L, stats.minBpm)
        assertEquals(60L, stats.maxBpm)
        assertTrue(stats.avgBpm.toLong() in stats.minBpm..stats.maxBpm)
    }

    @Test fun `with no samples the provider aggregate is all there is`() {
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            dayRestingBpm = 70L,
        )

        assertEquals("70", state.restingHeartRateValue(formatter)?.value)
    }
}
