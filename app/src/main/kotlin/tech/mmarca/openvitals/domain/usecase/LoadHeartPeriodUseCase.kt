package tech.mmarca.openvitals.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.DailyBloodPressurePoint
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.model.toBloodGlucoseEntries
import tech.mmarca.openvitals.domain.model.toBloodPressureEntries
import tech.mmarca.openvitals.domain.model.toBodyTempEntries
import tech.mmarca.openvitals.domain.model.toRespiratoryRateEntries
import tech.mmarca.openvitals.domain.model.toSkinTemperatureEntries
import tech.mmarca.openvitals.domain.model.toSpO2Entries
import tech.mmarca.openvitals.domain.model.toVo2MaxEntries
import tech.mmarca.openvitals.domain.query.HeartPeriodData
import tech.mmarca.openvitals.domain.query.VitalsPeriodData
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository

sealed interface HeartPeriodLoadRequest {
    data object Combined : HeartPeriodLoadRequest

    data class HeartOnly(val metric: HeartPeriodMetric) : HeartPeriodLoadRequest

    data class VitalsOnly(val metric: VitalsPeriodMetric) : HeartPeriodLoadRequest
}

data class HeartPeriodLoadResult(
    val daySamples: List<HeartRateSample> = emptyList(),
    val previousDaySamples: List<HeartRateSample> = emptyList(),
    val dailySummaries: List<HeartRateSummary> = emptyList(),
    val previousDailySummaries: List<HeartRateSummary> = emptyList(),
    val baselineDailySummaries: List<HeartRateSummary> = emptyList(),
    val dayRestingSamples: List<RestingHeartRateSample> = emptyList(),
    val dayRestingBpm: Long? = null,
    val previousDayRestingBpm: Long? = null,
    val dayHrvSamples: List<HrvSample> = emptyList(),
    val dayHrvMs: Double? = null,
    val previousDayHrvMs: Double? = null,
    val dailyRestingHR: List<DailyRestingHR> = emptyList(),
    val previousDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val baselineDailyRestingHR: List<DailyRestingHR> = emptyList(),
    val dailyHrv: List<DailyHrv> = emptyList(),
    val previousDailyHrv: List<DailyHrv> = emptyList(),
    val baselineDailyHrv: List<DailyHrv> = emptyList(),
    val missingVitalsPermissions: Set<String> = emptySet(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val previousBloodPressure: List<BloodPressureEntry> = emptyList(),
    val baselineBloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val previousSpO2: List<SpO2Entry> = emptyList(),
    val baselineSpO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val previousRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val baselineRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val previousBodyTemperature: List<BodyTempEntry> = emptyList(),
    val baselineBodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val previousVo2Max: List<Vo2MaxEntry> = emptyList(),
    val baselineVo2Max: List<Vo2MaxEntry> = emptyList(),
    val bloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val previousBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val baselineBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val skinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val previousSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val baselineSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
    // Non-day loads: one point per day, the window's latest reading, and budget-blown metrics.
    val bloodPressureDaily: List<DailyBloodPressurePoint> = emptyList(),
    val spO2Daily: List<DailyVitalPoint> = emptyList(),
    val respiratoryRateDaily: List<DailyVitalPoint> = emptyList(),
    val bodyTemperatureDaily: List<DailyVitalPoint> = emptyList(),
    val vo2MaxDaily: List<DailyVitalPoint> = emptyList(),
    val bloodGlucoseDaily: List<DailyVitalPoint> = emptyList(),
    val skinTemperatureDaily: List<DailyVitalPoint> = emptyList(),
    val latestBloodPressure: BloodPressureEntry? = null,
    val latestSpO2: SpO2Entry? = null,
    val latestRespiratoryRate: RespiratoryRateEntry? = null,
    val latestBodyTemperature: BodyTempEntry? = null,
    val latestVo2Max: Vo2MaxEntry? = null,
    val latestBloodGlucose: BloodGlucoseEntry? = null,
    val latestSkinTemperature: SkinTemperatureEntry? = null,
    val timedOutVitals: Set<VitalsPeriodMetric> = emptySet(),
)

data class HeartVitalsSummary(
    val hasVitalsData: Boolean,
    val latestBloodPressure: BloodPressureEntry?,
    val latestSpO2: SpO2Entry?,
    val latestRespiratoryRate: RespiratoryRateEntry?,
    val latestBodyTemperature: BodyTempEntry?,
    val latestVo2Max: Vo2MaxEntry?,
    val latestBloodGlucose: BloodGlucoseEntry?,
    val latestSkinTemperature: SkinTemperatureEntry?,
)

fun HeartPeriodLoadResult.vitalsSummary(): HeartVitalsSummary =
    HeartVitalsSummary(
        hasVitalsData = bloodPressure.isNotEmpty() ||
            spO2.isNotEmpty() ||
            respiratoryRate.isNotEmpty() ||
            bodyTemperature.isNotEmpty() ||
            vo2Max.isNotEmpty() ||
            bloodGlucose.isNotEmpty() ||
            skinTemperature.isNotEmpty() ||
            // A timed-out daily read leaves its list empty; the latest reading still proves data.
            latestBloodPressure != null ||
            latestSpO2 != null ||
            latestRespiratoryRate != null ||
            latestBodyTemperature != null ||
            latestVo2Max != null ||
            latestBloodGlucose != null ||
            latestSkinTemperature != null,
        // Prefer the window-latest reads: non-day entries are synthesised aggregates.
        latestBloodPressure = latestBloodPressure ?: bloodPressure.maxByOrNull { it.time },
        latestSpO2 = latestSpO2 ?: spO2.maxByOrNull { it.time },
        latestRespiratoryRate = latestRespiratoryRate ?: respiratoryRate.maxByOrNull { it.time },
        latestBodyTemperature = latestBodyTemperature ?: bodyTemperature.maxByOrNull { it.time },
        latestVo2Max = latestVo2Max ?: vo2Max.maxByOrNull { it.time },
        latestBloodGlucose = latestBloodGlucose ?: bloodGlucose.maxByOrNull { it.time },
        latestSkinTemperature = latestSkinTemperature ?: skinTemperature.maxByOrNull { it.time },
    )

class LoadHeartPeriodUseCase @Inject constructor(
    private val heartRepository: HeartRepository,
    private val vitalsRepository: VitalsRepository,
) {
    suspend operator fun invoke(
        query: PeriodLoadQuery,
        request: HeartPeriodLoadRequest,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): HeartPeriodLoadResult =
        when (request) {
            HeartPeriodLoadRequest.Combined -> coroutineScope {
                val heart = async {
                    loadHeartPeriod(query, HeartPeriodMetric.ALL, refreshMode).toLoadResult()
                }
                val vitals = async {
                    loadVitalsPeriod(query, VitalsPeriodMetric.ALL, refreshMode).toLoadResult()
                }
                heart.await().merge(vitals.await())
            }
            is HeartPeriodLoadRequest.HeartOnly -> loadHeartPeriod(
                query,
                request.metric,
                refreshMode,
            ).toLoadResult()
            is HeartPeriodLoadRequest.VitalsOnly -> loadVitalsPeriod(
                query,
                request.metric,
                refreshMode,
            ).toLoadResult()
        }

    private suspend fun loadHeartPeriod(
        query: PeriodLoadQuery,
        metric: HeartPeriodMetric,
        refreshMode: RefreshMode,
    ): HeartPeriodData =
        if (refreshMode == RefreshMode.NORMAL) {
            heartRepository.loadHeartPeriod(query, metric)
        } else {
            heartRepository.loadHeartPeriod(query, metric, refreshMode)
        }

    private suspend fun loadVitalsPeriod(
        query: PeriodLoadQuery,
        metric: VitalsPeriodMetric,
        refreshMode: RefreshMode,
    ): VitalsPeriodData =
        if (refreshMode == RefreshMode.NORMAL) {
            vitalsRepository.loadVitalsPeriod(query, metric)
        } else {
            vitalsRepository.loadVitalsPeriod(query, metric, refreshMode)
        }
}

private fun HeartPeriodData.toLoadResult(): HeartPeriodLoadResult =
    HeartPeriodLoadResult(
        daySamples = daySamples,
        previousDaySamples = previousDaySamples,
        dailySummaries = dailySummaries,
        previousDailySummaries = previousDailySummaries,
        baselineDailySummaries = baselineDailySummaries,
        dayRestingSamples = dayRestingSamples,
        dayRestingBpm = dayRestingBpm,
        previousDayRestingBpm = previousDayRestingBpm,
        dayHrvSamples = dayHrvSamples,
        dayHrvMs = dayHrvMs,
        previousDayHrvMs = previousDayHrvMs,
        dailyRestingHR = dailyRestingHR,
        previousDailyRestingHR = previousDailyRestingHR,
        baselineDailyRestingHR = baselineDailyRestingHR,
        dailyHrv = dailyHrv,
        previousDailyHrv = previousDailyHrv,
        baselineDailyHrv = baselineDailyHrv,
    )

private fun VitalsPeriodData.toLoadResult(): HeartPeriodLoadResult =
    HeartPeriodLoadResult(
        missingVitalsPermissions = missingVitalsPermissions,
        // On non-day loads the daily points stand in for the raw lists.
        bloodPressure = bloodPressure.ifEmpty { bloodPressureDaily.toBloodPressureEntries() },
        previousBloodPressure = previousBloodPressure,
        baselineBloodPressure = baselineBloodPressure,
        spO2 = spO2.ifEmpty { spO2Daily.toSpO2Entries() },
        previousSpO2 = previousSpO2,
        baselineSpO2 = baselineSpO2,
        respiratoryRate = respiratoryRate.ifEmpty { respiratoryRateDaily.toRespiratoryRateEntries() },
        previousRespiratoryRate = previousRespiratoryRate,
        baselineRespiratoryRate = baselineRespiratoryRate,
        bodyTemperature = bodyTemperature.ifEmpty { bodyTemperatureDaily.toBodyTempEntries() },
        previousBodyTemperature = previousBodyTemperature,
        baselineBodyTemperature = baselineBodyTemperature,
        vo2Max = vo2Max.ifEmpty { vo2MaxDaily.toVo2MaxEntries() },
        previousVo2Max = previousVo2Max,
        baselineVo2Max = baselineVo2Max,
        bloodGlucose = bloodGlucose.ifEmpty { bloodGlucoseDaily.toBloodGlucoseEntries() },
        previousBloodGlucose = previousBloodGlucose,
        baselineBloodGlucose = baselineBloodGlucose,
        skinTemperature = skinTemperature.ifEmpty { skinTemperatureDaily.toSkinTemperatureEntries() },
        previousSkinTemperature = previousSkinTemperature,
        baselineSkinTemperature = baselineSkinTemperature,
        bloodPressureDaily = bloodPressureDaily,
        spO2Daily = spO2Daily,
        respiratoryRateDaily = respiratoryRateDaily,
        bodyTemperatureDaily = bodyTemperatureDaily,
        vo2MaxDaily = vo2MaxDaily,
        bloodGlucoseDaily = bloodGlucoseDaily,
        skinTemperatureDaily = skinTemperatureDaily,
        latestBloodPressure = latestBloodPressure,
        latestSpO2 = latestSpO2,
        latestRespiratoryRate = latestRespiratoryRate,
        latestBodyTemperature = latestBodyTemperature,
        latestVo2Max = latestVo2Max,
        latestBloodGlucose = latestBloodGlucose,
        latestSkinTemperature = latestSkinTemperature,
        timedOutVitals = timedOutMetrics,
    )

internal fun HeartPeriodLoadResult.merge(other: HeartPeriodLoadResult): HeartPeriodLoadResult =
    HeartPeriodLoadResult(
        daySamples = daySamples + other.daySamples,
        previousDaySamples = previousDaySamples + other.previousDaySamples,
        dailySummaries = dailySummaries + other.dailySummaries,
        previousDailySummaries = previousDailySummaries + other.previousDailySummaries,
        baselineDailySummaries = baselineDailySummaries + other.baselineDailySummaries,
        dayRestingSamples = dayRestingSamples + other.dayRestingSamples,
        dayRestingBpm = dayRestingBpm ?: other.dayRestingBpm,
        previousDayRestingBpm = previousDayRestingBpm ?: other.previousDayRestingBpm,
        dayHrvSamples = dayHrvSamples + other.dayHrvSamples,
        dayHrvMs = dayHrvMs ?: other.dayHrvMs,
        previousDayHrvMs = previousDayHrvMs ?: other.previousDayHrvMs,
        dailyRestingHR = dailyRestingHR + other.dailyRestingHR,
        previousDailyRestingHR = previousDailyRestingHR + other.previousDailyRestingHR,
        baselineDailyRestingHR = baselineDailyRestingHR + other.baselineDailyRestingHR,
        dailyHrv = dailyHrv + other.dailyHrv,
        previousDailyHrv = previousDailyHrv + other.previousDailyHrv,
        baselineDailyHrv = baselineDailyHrv + other.baselineDailyHrv,
        missingVitalsPermissions = missingVitalsPermissions + other.missingVitalsPermissions,
        bloodPressure = bloodPressure + other.bloodPressure,
        previousBloodPressure = previousBloodPressure + other.previousBloodPressure,
        baselineBloodPressure = baselineBloodPressure + other.baselineBloodPressure,
        spO2 = spO2 + other.spO2,
        previousSpO2 = previousSpO2 + other.previousSpO2,
        baselineSpO2 = baselineSpO2 + other.baselineSpO2,
        respiratoryRate = respiratoryRate + other.respiratoryRate,
        previousRespiratoryRate = previousRespiratoryRate + other.previousRespiratoryRate,
        baselineRespiratoryRate = baselineRespiratoryRate + other.baselineRespiratoryRate,
        bodyTemperature = bodyTemperature + other.bodyTemperature,
        previousBodyTemperature = previousBodyTemperature + other.previousBodyTemperature,
        baselineBodyTemperature = baselineBodyTemperature + other.baselineBodyTemperature,
        vo2Max = vo2Max + other.vo2Max,
        previousVo2Max = previousVo2Max + other.previousVo2Max,
        baselineVo2Max = baselineVo2Max + other.baselineVo2Max,
        bloodGlucose = bloodGlucose + other.bloodGlucose,
        previousBloodGlucose = previousBloodGlucose + other.previousBloodGlucose,
        baselineBloodGlucose = baselineBloodGlucose + other.baselineBloodGlucose,
        skinTemperature = skinTemperature + other.skinTemperature,
        previousSkinTemperature = previousSkinTemperature + other.previousSkinTemperature,
        baselineSkinTemperature = baselineSkinTemperature + other.baselineSkinTemperature,
        bloodPressureDaily = bloodPressureDaily + other.bloodPressureDaily,
        spO2Daily = spO2Daily + other.spO2Daily,
        respiratoryRateDaily = respiratoryRateDaily + other.respiratoryRateDaily,
        bodyTemperatureDaily = bodyTemperatureDaily + other.bodyTemperatureDaily,
        vo2MaxDaily = vo2MaxDaily + other.vo2MaxDaily,
        bloodGlucoseDaily = bloodGlucoseDaily + other.bloodGlucoseDaily,
        skinTemperatureDaily = skinTemperatureDaily + other.skinTemperatureDaily,
        latestBloodPressure = latestBloodPressure ?: other.latestBloodPressure,
        latestSpO2 = latestSpO2 ?: other.latestSpO2,
        latestRespiratoryRate = latestRespiratoryRate ?: other.latestRespiratoryRate,
        latestBodyTemperature = latestBodyTemperature ?: other.latestBodyTemperature,
        latestVo2Max = latestVo2Max ?: other.latestVo2Max,
        latestBloodGlucose = latestBloodGlucose ?: other.latestBloodGlucose,
        latestSkinTemperature = latestSkinTemperature ?: other.latestSkinTemperature,
        timedOutVitals = timedOutVitals + other.timedOutVitals,
    )
