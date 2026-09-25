package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.query.HeartPeriodData

interface HeartRepository {
    suspend fun loadHeartPeriod(
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
    ): HeartPeriodData

    /** The day's average as the Today tile shows it: raw samples, minute-bucketed. */
    suspend fun loadAvgHeartRate(date: LocalDate): Long?

    suspend fun loadRawHeartRateSamplesForDayGraph(date: LocalDate): List<HeartRateSample>

    suspend fun loadHeartRateSamples(start: LocalDate, end: LocalDate): List<HeartRateSample>

    suspend fun loadHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample>

    /** The highest bpm in the window, without loading its samples. Any window length is safe. */
    suspend fun loadMaxHeartRate(start: Instant, end: Instant): Long?

    suspend fun loadDailyHeartRateSummaries(start: LocalDate, end: LocalDate): List<HeartRateSummary>

    suspend fun loadRestingHeartRate(date: LocalDate): Long?

    suspend fun loadDailyRestingHR(start: LocalDate, end: LocalDate): List<DailyRestingHR>

    /** The day's HRV as the Day view shows it: raw samples, minute-bucketed. */
    suspend fun loadHrvRmssd(date: LocalDate): Double?

    suspend fun loadHrvSamples(start: Instant, end: Instant): List<HrvSample>

    suspend fun loadDailyHRV(start: LocalDate, end: LocalDate): List<DailyHrv>
}
