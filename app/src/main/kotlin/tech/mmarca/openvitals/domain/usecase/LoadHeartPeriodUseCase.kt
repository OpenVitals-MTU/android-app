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
            skinTemperature.isNotEmpty(),
        latestBloodPressure = bloodPressure.maxByOrNull { it.time },
        latestSpO2 = spO2.maxByOrNull { it.time },
        latestRespiratoryRate = respiratoryRate.maxByOrNull { it.time },
        latestBodyTemperature = bodyTemperature.maxByOrNull { it.time },
        latestVo2Max = vo2Max.maxByOrNull { it.time },
        latestBloodGlucose = bloodGlucose.maxByOrNull { it.time },
        latestSkinTemperature = skinTemperature.maxByOrNull { it.time },
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
        bloodPressure = bloodPressure,
        previousBloodPressure = previousBloodPressure,
        baselineBloodPressure = baselineBloodPressure,
        spO2 = spO2,
        previousSpO2 = previousSpO2,
        baselineSpO2 = baselineSpO2,
        respiratoryRate = respiratoryRate,
        previousRespiratoryRate = previousRespiratoryRate,
        baselineRespiratoryRate = baselineRespiratoryRate,
        bodyTemperature = bodyTemperature,
        previousBodyTemperature = previousBodyTemperature,
        baselineBodyTemperature = baselineBodyTemperature,
        vo2Max = vo2Max,
        previousVo2Max = previousVo2Max,
        baselineVo2Max = baselineVo2Max,
        bloodGlucose = bloodGlucose,
        previousBloodGlucose = previousBloodGlucose,
        baselineBloodGlucose = baselineBloodGlucose,
        skinTemperature = skinTemperature,
        previousSkinTemperature = previousSkinTemperature,
        baselineSkinTemperature = baselineSkinTemperature,
    )

private fun HeartPeriodLoadResult.merge(other: HeartPeriodLoadResult): HeartPeriodLoadResult =
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
    )
