package tech.mmarca.openvitals.features.vitals

import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.model.toBodyTempEntries
import tech.mmarca.openvitals.domain.model.toVo2MaxEntries
import tech.mmarca.openvitals.domain.model.totalReadings
import tech.mmarca.openvitals.domain.model.weightedMeanOrNull
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.features.heart.bloodPressureStats
import tech.mmarca.openvitals.features.heart.heartRateTimelineStats

/**
 * Port of test/features/vitals/heart_vitals_overview_display_test.dart. Kotlin
 * derives these card values in the overview layer rather than in one display
 * object, so the test calls the same pure helpers the screen calls.
 */
class HeartVitalsOverviewCardsTest {

    private val formatter = UnitFormatter(
        unitSystemProvider = { UnitSystem.METRIC },
        localeProvider = { Locale.US },
    )

    private val monday: LocalDate = LocalDate.of(2026, 3, 2)
    private val tuesday: LocalDate = LocalDate.of(2026, 3, 3)

    private fun at(hour: Int) = Instant.parse("2026-03-02T%02d:00:00Z".format(hour))

    private fun sample(hour: Int, bpm: Long, source: String = "Watch") =
        HeartRateSample(time = at(hour), beatsPerMinute = bpm, source = source)

    private fun spO2(hour: Int, percent: Double) =
        SpO2Entry(time = at(hour), percent = percent, source = "Ring")

    private fun bp(hour: Int, systolic: Int, diastolic: Int) = BloodPressureEntry(
        time = at(hour),
        systolicMmHg = systolic,
        diastolicMmHg = diastolic,
        source = "Cuff",
    )

    private fun skin(hour: Int, delta: Double?) = SkinTemperatureEntry(
        startTime = at(hour),
        endTime = at(hour),
        baselineCelsius = 33.0,
        averageDeltaCelsius = delta,
        minDeltaCelsius = delta,
        maxDeltaCelsius = delta,
        measurementLocation = 0,
        source = "Ring",
    )

    private fun point(date: LocalDate, value: Double, count: Int) =
        DailyVitalPoint(date = date, value = value, count = count)

    // ─── heart rate ───────────────────────────────────────────────────────────

    @Test fun `a day of samples sorts oldest first and averages them`() {
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            daySamples = listOf(sample(14, 90L), sample(6, 52L), sample(9, 71L)),
        )

        // (90 + 52 + 71) / 3 = 71.
        assertEquals("71", state.averageHeartRateValue(formatter)?.value)
        val timeline = heartRateTimelineStats(state.daySamples)
        assertEquals(listOf(52L, 71L, 90L), timeline.sorted.map { it.beatsPerMinute })
        assertEquals(52L, timeline.minBpm)
        assertEquals(90L, timeline.maxBpm)
    }

    @Test fun `the day card names its source only when the samples agree`() {
        val agreed = listOf(sample(6, 60L), sample(7, 70L))
        assertEquals("Watch", agreed.sourceForDay(TimeRange.DAY))

        val disagreed = listOf(sample(6, 60L), sample(7, 70L, source = "Ring"))
        assertNull(disagreed.sourceForDay(TimeRange.DAY))

        // A period never names a source, however uniform its samples.
        assertNull(agreed.sourceForDay(TimeRange.WEEK))
        assertNull(emptyList<HeartRateSample>().sourceForDay(TimeRange.DAY))
    }

    @Test fun `a period takes its extremes across every daily summary`() {
        val state = HeartUiState(
            selectedRange = TimeRange.WEEK,
            dailySummaries = listOf(
                HeartRateSummary(tuesday, avgBpm = 72L, minBpm = 56L, maxBpm = 118L),
                HeartRateSummary(monday, avgBpm = 70L, minBpm = 55L, maxBpm = 120L),
            ),
        )

        val summary = heartRateRangeSummary(state.dailySummaries)!!
        assertEquals(71L, summary.average) // (70 + 72) / 2.
        assertEquals(55L, summary.min)
        assertEquals(120L, summary.max)
        // The card prints the same mean; a period draws no intraday timeline and
        // names no source.
        assertEquals("71", state.averageHeartRateValue(formatter)?.value)
        assertNull(state.daySamples.sourceForDay(TimeRange.WEEK))
        assertNull(heartRateRangeSummary(emptyList()))
    }

    // ─── resting heart rate and HRV ───────────────────────────────────────────

    @Test fun `a day reads the provider aggregate not the daily series`() {
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            dayRestingBpm = 58L,
            dayHrvMs = 44.5,
            dailyRestingHR = listOf(DailyRestingHR(monday, 99L)),
            dailyHrv = listOf(DailyHrv(monday, 99.0)),
        )

        assertEquals("58", state.restingHeartRateValue(formatter)?.value)
        // The daily series belongs to the longer ranges; the day ignores it.
        assertEquals(
            "99",
            state.copy(selectedRange = TimeRange.WEEK).restingHeartRateValue(formatter)?.value,
        )
    }

    @Test fun `a day with no resting aggregate has no resting card`() {
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            dailyRestingHR = listOf(DailyRestingHR(monday, 61L)),
        )

        assertNull(state.restingHeartRateValue(formatter))
    }

    // ─── cardiovascular ───────────────────────────────────────────────────────

    @Test fun `blood pressure sorts counts and takes the latest reading`() {
        val entries = listOf(bp(18, 128, 82), bp(8, 118, 76))

        assertEquals(
            listOf(118, 128),
            entries.sortedBy { it.time }.map { it.systolicMmHg },
        )
        val stats = bloodPressureStats(entries)!!
        assertEquals(128, stats.latest.systolicMmHg)
        assertEquals(2, stats.readings)
        assertTrue(entries.hasRenderableChartData(TimeRange.DAY) { it.time })
    }

    @Test fun `within a day one timestamp draws no chart and two do`() {
        val single = listOf(spO2(9, 97.0))
        // The card still shows the reading; the chart has no line to draw.
        assertEquals(97.0, single.maxByOrNull { it.time }!!.percent, 1e-9)
        assertFalse(single.hasRenderableChartData(TimeRange.DAY) { it.time })

        val pair = listOf(spO2(9, 97.0), spO2(11, 95.0))
        assertTrue(pair.hasRenderableChartData(TimeRange.DAY) { it.time })

        // Two readings at the SAME instant are still one timestamp.
        val sameInstant = listOf(spO2(9, 97.0), spO2(9, 95.0))
        assertFalse(sameInstant.hasRenderableChartData(TimeRange.DAY) { it.time })

        // Longer ranges chart anything non-empty.
        assertTrue(single.hasRenderableChartData(TimeRange.WEEK) { it.time })
        assertFalse(emptyList<SpO2Entry>().hasRenderableChartData(TimeRange.WEEK) { it.time })
    }

    @Test fun `a long-range overview reads native daily aggregates`() {
        val state = HeartUiState(
            selectedRange = TimeRange.WEEK,
            spO2Daily = listOf(point(monday, 95.0, 1), point(tuesday, 98.0, 3)),
        )

        // "Average every reading" → count-weighted: (95*1 + 98*3) / 4 = 97.25.
        assertEquals(97.25, state.spO2Daily.weightedMeanOrNull()!!, 1e-9)
        assertEquals(4, state.spO2Daily.totalReadings())
        assertNull(emptyList<DailyVitalPoint>().weightedMeanOrNull())
        assertEquals(0, emptyList<DailyVitalPoint>().totalReadings())
    }

    // ─── vo2 max ──────────────────────────────────────────────────────────────

    @Test fun `vo2 max sorts counts and takes the latest reading`() {
        val entries = listOf(
            Vo2MaxEntry(Instant.parse("2026-03-03T00:00:00Z"), 44.0, "Watch"),
            Vo2MaxEntry(Instant.parse("2026-03-02T00:00:00Z"), 42.0, "Watch"),
        )

        assertEquals(
            listOf(42.0, 44.0),
            entries.sortedBy { it.time }.map { it.vo2MaxMlPerKgPerMin },
        )
        assertEquals(44.0, entries.maxByOrNull { it.time }!!.vo2MaxMlPerKgPerMin, 1e-9)
        assertEquals(2, entries.size)
        assertTrue(entries.hasRenderableChartData(TimeRange.DAY) { it.time })
    }

    @Test fun `vo2 max over a long range totals its daily reading counts`() {
        val daily = listOf(point(monday, 42.0, 1), point(tuesday, 44.0, 2))
        val entries = daily.toVo2MaxEntries()

        // One synthetic point per day, in date order; the card's readings total
        // the raw counts, not the number of synthetic points.
        assertEquals(listOf(42.0, 44.0), entries.map { it.vo2MaxMlPerKgPerMin })
        assertEquals(3, daily.totalReadings())
        assertTrue(entries.hasRenderableChartData(TimeRange.WEEK) { it.time })
    }

    // ─── body temperature ─────────────────────────────────────────────────────

    @Test fun `body temperature counts its readings and takes the latest`() {
        val entries = listOf(
            BodyTempEntry(at(20), 36.9, "Thermometer"),
            BodyTempEntry(at(8), 36.4, "Thermometer"),
        )

        assertEquals(2, entries.size)
        assertEquals(36.9, entries.maxByOrNull { it.time }!!.temperatureCelsius, 1e-9)
        assertEquals(
            listOf(36.4, 36.9),
            entries.sortedBy { it.time }.map { it.temperatureCelsius },
        )
    }

    @Test fun `body temperature over a long range totals its daily reading counts`() {
        val daily = listOf(point(monday, 36.4, 2), point(tuesday, 36.9, 4))
        val entries = daily.toBodyTempEntries()

        assertEquals(6, daily.totalReadings())
        assertEquals(listOf(36.4, 36.9), entries.map { it.temperatureCelsius })
    }

    // ─── skin temperature ─────────────────────────────────────────────────────

    @Test fun `skin temperature charts a daily delta point per day with a weighted mean`() {
        val daily = listOf(point(monday, -0.2, 2), point(tuesday, 0.4, 1))
        val chartEntries = skinTemperatureChartEntries(
            daily.map { skinFromPoint(it) },
        )

        assertEquals(2, chartEntries.size)
        assertEquals(listOf(-0.2, 0.4), chartEntries.map { it.averageDeltaCelsius })
        // Count-weighted over the days: (-0.2*2 + 0.4*1) / 3 = 0.0.
        assertEquals(0.0, daily.weightedMeanOrNull()!!, 1e-9)
    }

    @Test fun `day view charts only the raw entries that carry a delta`() {
        val entries = listOf(skin(20, 0.4), skin(8, null), skin(12, -0.2))

        val chartEntries = skinTemperatureChartEntries(entries)
        assertEquals(2, chartEntries.size)
        assertEquals(listOf(-0.2, 0.4), chartEntries.map { it.averageDeltaCelsius })
        // (-0.2 + 0.4) / 2, over the delta-bearing entries only.
        assertEquals(
            0.1,
            chartEntries.mapNotNull { it.averageDeltaCelsius }.average(),
            1e-9,
        )
        assertEquals(0.4, entries.maxByOrNull { it.startTime }!!.averageDeltaCelsius!!, 1e-9)
    }

    @Test fun `a delta-less newest entry does not blank the card (day)`() {
        // The card used to read the newest entry of the UNFILTERED list, so one
        // reading arriving without a delta emptied it while its own chart still
        // plotted the readings that had one. It now reads the newest entry that
        // actually carries a delta: the same population the chart draws.
        val entries = listOf(skin(8, 0.4), skin(20, null))
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            skinTemperature = entries,
            latestSkinTemperature = entries.maxByOrNull { it.startTime },
        )

        assertEquals(0.4, state.skinTemperatureCardDeltaCelsius()!!, 1e-9)
        assertEquals(
            0.4,
            skinTemperatureChartEntries(entries).single().averageDeltaCelsius!!,
            1e-9,
        )
    }

    @Test fun `a day with no delta anywhere shows nothing card or chart`() {
        val entries = listOf(skin(8, null), skin(20, null))
        val state = HeartUiState(
            selectedRange = TimeRange.DAY,
            skinTemperature = entries,
            latestSkinTemperature = entries.maxByOrNull { it.startTime },
        )

        // No entry carries a delta, so there is nothing to print.
        assertNull(state.skinTemperatureCardDeltaCelsius())
        assertTrue(skinTemperatureChartEntries(entries).isEmpty())
        assertFalse(
            skinTemperatureChartEntries(entries)
                .hasRenderableChartData(TimeRange.DAY) { it.startTime },
        )
    }

    // ─── an empty period ──────────────────────────────────────────────────────

    @Test fun `an empty period derives an empty display section by section`() {
        val state = HeartUiState(selectedRange = TimeRange.WEEK, isLoading = false)

        assertNull(state.averageHeartRateValue(formatter))
        assertNull(state.restingHeartRateValue(formatter))
        assertNull(heartRateRangeSummary(state.dailySummaries))
        assertNull(restingHeartRateRangeSummary(state.dailyRestingHR))
        assertNull(hrvRangeSummary(state.dailyHrv))
        assertNull(bloodPressureStats(state.bloodPressure))
        assertNull(state.latestSpO2)
        assertNull(state.latestVo2Max)
        assertNull(state.latestBloodGlucose)
        assertNull(state.latestRespiratoryRate)
        assertNull(state.latestBodyTemperature)
        assertNull(state.latestSkinTemperature)
        assertTrue(skinTemperatureChartEntries(state.skinTemperature).isEmpty())
        assertFalse(state.hasVitalsData)
    }

    private fun skinFromPoint(pointValue: DailyVitalPoint) = SkinTemperatureEntry(
        startTime = pointValue.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
        endTime = pointValue.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
        baselineCelsius = null,
        averageDeltaCelsius = pointValue.value,
        minDeltaCelsius = null,
        maxDeltaCelsius = null,
        measurementLocation = 0,
        source = "",
    )
}
