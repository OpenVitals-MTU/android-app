package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.usecase.HeartPeriodLoadResult
import tech.mmarca.openvitals.domain.usecase.vitalsSummary
import java.time.LocalDate
import kotlin.math.roundToInt

object HeartPresentationMapper {

    fun applyLoadResult(
        current: HeartUiState,
        query: PeriodLoadQuery,
        metric: HeartMetric?,
        result: HeartPeriodLoadResult,
    ): HeartUiState {
        val vitalsSummary = result.vitalsSummary()
        val highThreshold = current.highHeartRateCheck.thresholdBpm
        val lowThreshold = current.lowHeartRateCheck.thresholdBpm
        return current.copy(
            isLoading = false,
            selectedDate = query.selectedDate,
            daySamples = result.daySamples,
            previousDaySamples = result.previousDaySamples,
            dailySummaries = result.dailySummaries,
            previousDailySummaries = result.previousDailySummaries,
            baselineDailySummaries = result.baselineDailySummaries,
            dayRestingSamples = result.dayRestingSamples,
            dayRestingBpm = result.dayRestingBpm,
            previousDayRestingBpm = result.previousDayRestingBpm,
            dayHrvSamples = result.dayHrvSamples,
            dayHrvMs = result.dayHrvMs,
            previousDayHrvMs = result.previousDayHrvMs,
            dailyRestingHR = result.dailyRestingHR,
            previousDailyRestingHR = result.previousDailyRestingHR,
            baselineDailyRestingHR = result.baselineDailyRestingHR,
            dailyHrv = result.dailyHrv,
            previousDailyHrv = result.previousDailyHrv,
            baselineDailyHrv = result.baselineDailyHrv,
            missingVitalsPermissions = result.missingVitalsPermissions,
            bloodPressure = result.bloodPressure,
            previousBloodPressure = result.previousBloodPressure,
            baselineBloodPressure = result.baselineBloodPressure,
            spO2 = result.spO2,
            previousSpO2 = result.previousSpO2,
            baselineSpO2 = result.baselineSpO2,
            respiratoryRate = result.respiratoryRate,
            previousRespiratoryRate = result.previousRespiratoryRate,
            baselineRespiratoryRate = result.baselineRespiratoryRate,
            bodyTemperature = result.bodyTemperature,
            previousBodyTemperature = result.previousBodyTemperature,
            baselineBodyTemperature = result.baselineBodyTemperature,
            vo2Max = result.vo2Max,
            previousVo2Max = result.previousVo2Max,
            baselineVo2Max = result.baselineVo2Max,
            bloodGlucose = result.bloodGlucose,
            previousBloodGlucose = result.previousBloodGlucose,
            baselineBloodGlucose = result.baselineBloodGlucose,
            skinTemperature = result.skinTemperature,
            previousSkinTemperature = result.previousSkinTemperature,
            baselineSkinTemperature = result.baselineSkinTemperature,
            hasVitalsData = vitalsSummary.hasVitalsData,
            latestBloodPressure = vitalsSummary.latestBloodPressure,
            latestSpO2 = vitalsSummary.latestSpO2,
            latestRespiratoryRate = vitalsSummary.latestRespiratoryRate,
            latestBodyTemperature = vitalsSummary.latestBodyTemperature,
            latestVo2Max = vitalsSummary.latestVo2Max,
            latestBloodGlucose = vitalsSummary.latestBloodGlucose,
            latestSkinTemperature = vitalsSummary.latestSkinTemperature,
            highHeartRateCheck = result.heartRateThresholdCheck(
                selectedRange = query.range,
                type = HeartRateThresholdCheckType.HIGH,
                thresholdBpm = highThreshold,
            ),
            lowHeartRateCheck = result.heartRateThresholdCheck(
                selectedRange = query.range,
                type = HeartRateThresholdCheckType.LOW,
                thresholdBpm = lowThreshold,
            ),
            display = build(query = query, metric = metric, result = result),
        )
    }

    fun build(
        query: PeriodLoadQuery,
        metric: HeartMetric?,
        result: HeartPeriodLoadResult,
    ): HeartDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val metricDisplay = when (metric) {
            HeartMetric.AVERAGE_HEART_RATE,
            null,
            -> averageHeartRateDisplay(query, result)
            HeartMetric.RESTING_HEART_RATE -> restingHeartRateDisplay(query, result)
            HeartMetric.HRV -> hrvDisplay(query, result)
            HeartMetric.BLOOD_PRESSURE -> vitalsEntriesDisplay(result.bloodPressure.isNotEmpty(), result.bloodPressure.size)
            HeartMetric.SPO2 -> vitalsEntriesDisplay(result.spO2.isNotEmpty(), result.spO2.size)
            HeartMetric.VO2_MAX -> vitalsEntriesDisplay(result.vo2Max.isNotEmpty(), result.vo2Max.size)
            HeartMetric.RESPIRATORY_RATE -> vitalsEntriesDisplay(result.respiratoryRate.isNotEmpty(), result.respiratoryRate.size)
            HeartMetric.BODY_TEMPERATURE -> vitalsEntriesDisplay(result.bodyTemperature.isNotEmpty(), result.bodyTemperature.size)
            HeartMetric.BLOOD_GLUCOSE -> vitalsEntriesDisplay(result.bloodGlucose.isNotEmpty(), result.bloodGlucose.size)
            HeartMetric.SKIN_TEMPERATURE -> vitalsEntriesDisplay(result.skinTemperature.isNotEmpty(), result.skinTemperature.size)
        }
        return HeartDisplayState(
            selectedPeriod = selectedPeriod,
            metric = metricDisplay,
        )
    }

    fun build(
        query: PeriodLoadQuery,
        metric: HeartMetric?,
        state: HeartUiState,
    ): HeartDisplayState =
        build(
            query = query,
            metric = metric,
            result = state.toLoadResult(),
        )
}

private fun HeartUiState.toLoadResult(): HeartPeriodLoadResult =
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

private fun averageHeartRateDisplay(
    query: PeriodLoadQuery,
    result: HeartPeriodLoadResult,
): HeartMetricDisplay {
    val hasDaySamples = query.range == TimeRange.DAY && result.daySamples.isNotEmpty()
    val sortedSummaries = result.dailySummaries.sortedBy { it.date }
    val hasPeriodSummaries = query.range != TimeRange.DAY && sortedSummaries.isNotEmpty()
    return HeartMetricDisplay(
        hasData = hasDaySamples || hasPeriodSummaries,
        hasDayHeartRateSamples = hasDaySamples,
        hasPeriodHeartRateSummaries = hasPeriodSummaries,
        showDayHeartRateTimeline = hasDaySamples && result.daySamples.size > 1,
        sortedDailySummaries = sortedSummaries,
        heartRateRangeSummary = heartRateRangeSummary(sortedSummaries),
        heartRateTrackedDates = sortedSummaries.map { it.date },
        heartRateSampleCount = if (query.range == TimeRange.DAY) {
            result.daySamples.size
        } else {
            sortedSummaries.size
        },
    )
}

private fun restingHeartRateDisplay(
    query: PeriodLoadQuery,
    result: HeartPeriodLoadResult,
): HeartMetricDisplay {
    val hasDayRestingSamples = query.range == TimeRange.DAY && result.dayRestingSamples.isNotEmpty()
    val hasDayResting = query.range == TimeRange.DAY && (hasDayRestingSamples || result.dayRestingBpm != null)
    val sorted = result.dailyRestingHR.sortedBy { it.date }
    val hasPeriodResting = query.range != TimeRange.DAY && sorted.isNotEmpty()
    return HeartMetricDisplay(
        hasData = hasDayResting || hasPeriodResting,
        hasDayRestingRate = hasDayResting,
        hasPeriodRestingRate = hasPeriodResting,
        restingRangeSummary = sorted.takeIf { it.isNotEmpty() }?.let { restingHeartRateRangeSummary(it) },
        restingDayComparison = result.dayRestingBpm?.let { current ->
            result.previousDayRestingBpm?.let { previous ->
                periodComparison(current.toDouble(), previous.toDouble())
            }
        },
        restingPeriodAverageBpm = sorted
            .takeIf { it.isNotEmpty() }
            ?.map { it.bpm }
            ?.average()
            ?.roundToInt()
            ?.toLong(),
        restingBaselineValues = result.baselineDailyRestingHR.map { BaselineValue(it.date, it.bpm.toDouble()) },
        vitalsTrackedDates = if (hasDayResting) listOf(query.selectedDate) else sorted.map { it.date },
        vitalsSampleCount = if (hasDayResting) result.dayRestingSamples.size.coerceAtLeast(1) else sorted.size,
    )
}

private fun hrvDisplay(
    query: PeriodLoadQuery,
    result: HeartPeriodLoadResult,
): HeartMetricDisplay {
    val hasDayHrvSamples = query.range == TimeRange.DAY && result.dayHrvSamples.isNotEmpty()
    val hasDayHrv = query.range == TimeRange.DAY && (hasDayHrvSamples || result.dayHrvMs != null)
    val sorted = result.dailyHrv.sortedBy { it.date }
    val hasPeriodHrv = query.range != TimeRange.DAY && sorted.isNotEmpty()
    return HeartMetricDisplay(
        hasData = hasDayHrv || hasPeriodHrv,
        hasDayHrv = hasDayHrv,
        hasPeriodHrv = hasPeriodHrv,
        hrvRangeSummary = sorted.takeIf { it.isNotEmpty() }?.let { hrvRangeSummary(it) },
        hrvDayComparison = result.dayHrvMs?.let { current ->
            result.previousDayHrvMs?.let { previous -> periodComparison(current, previous) }
        },
        hrvBaselineValues = result.baselineDailyHrv.map { BaselineValue(it.date, it.rmssdMs) },
        vitalsTrackedDates = if (hasDayHrv) listOf(query.selectedDate) else sorted.map { it.date },
        vitalsSampleCount = if (hasDayHrv) result.dayHrvSamples.size.coerceAtLeast(1) else sorted.size,
    )
}

private fun vitalsEntriesDisplay(hasEntries: Boolean, sampleCount: Int): HeartMetricDisplay =
    HeartMetricDisplay(
        hasData = hasEntries,
        hasVitalsEntries = hasEntries,
        vitalsSampleCount = sampleCount,
    )

internal fun HeartUiState.heartRateThresholdCheck(
    type: HeartRateThresholdCheckType,
    thresholdBpm: Int,
): HeartRateThresholdCheck {
    val hasData = if (selectedRange == TimeRange.DAY) {
        daySamples.isNotEmpty()
    } else {
        dailySummaries.isNotEmpty()
    }
    val count = when (type) {
        HeartRateThresholdCheckType.HIGH -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute >= thresholdBpm }
        } else {
            dailySummaries.count { it.maxBpm >= thresholdBpm }
        }
        HeartRateThresholdCheckType.LOW -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute <= thresholdBpm }
        } else {
            dailySummaries.count { it.minBpm <= thresholdBpm }
        }
    }
    return HeartRateThresholdCheck(
        type = type,
        thresholdBpm = thresholdBpm,
        count = count,
        hasData = hasData,
    )
}

private fun HeartPeriodLoadResult.heartRateThresholdCheck(
    selectedRange: TimeRange,
    type: HeartRateThresholdCheckType,
    thresholdBpm: Int,
): HeartRateThresholdCheck {
    val hasData = if (selectedRange == TimeRange.DAY) {
        daySamples.isNotEmpty()
    } else {
        dailySummaries.isNotEmpty()
    }
    val count = when (type) {
        HeartRateThresholdCheckType.HIGH -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute >= thresholdBpm }
        } else {
            dailySummaries.count { it.maxBpm >= thresholdBpm }
        }
        HeartRateThresholdCheckType.LOW -> if (selectedRange == TimeRange.DAY) {
            daySamples.count { it.beatsPerMinute <= thresholdBpm }
        } else {
            dailySummaries.count { it.minBpm <= thresholdBpm }
        }
    }
    return HeartRateThresholdCheck(
        type = type,
        thresholdBpm = thresholdBpm,
        count = count,
        hasData = hasData,
    )
}
