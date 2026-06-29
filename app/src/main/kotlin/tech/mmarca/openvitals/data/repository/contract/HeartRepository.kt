package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.HeartPeriodData

interface HeartRepository {
    suspend fun loadHeartPeriod(
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): HeartPeriodData

    suspend fun loadHeartRateSamples(date: LocalDate): List<HeartRateSample>

    suspend fun loadHeartRateSamples(start: LocalDate, end: LocalDate): List<HeartRateSample>

    suspend fun loadHeartRateSamples(start: Instant, end: Instant): List<HeartRateSample>

    suspend fun loadDailyHeartRateSummaries(start: LocalDate, end: LocalDate): List<HeartRateSummary>

    suspend fun loadRestingHeartRate(date: LocalDate): Long?

    suspend fun loadDailyRestingHR(start: LocalDate, end: LocalDate): List<DailyRestingHR>

    suspend fun loadHrvRmssd(date: LocalDate): Double?

    suspend fun loadDailyHRV(start: LocalDate, end: LocalDate): List<DailyHrv>
}
