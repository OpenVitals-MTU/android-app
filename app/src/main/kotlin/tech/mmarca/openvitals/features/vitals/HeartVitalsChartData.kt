package tech.mmarca.openvitals.features.vitals

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.core.stats.averageOrNull
import tech.mmarca.openvitals.core.stats.averageOrZero
import tech.mmarca.openvitals.core.stats.timeBucketedAverageOrNull
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.MetricLineSeries
import tech.mmarca.openvitals.ui.components.dailyAverageLinePoints
import tech.mmarca.openvitals.ui.components.mapLinePoints
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class LongRangeSummary(
    val average: Long,
    val min: Long,
    val max: Long,
)

data class DoubleRangeSummary(
    val average: Double,
    val min: Double,
    val max: Double,
)

internal data class VitalsDailyRange(
    val average: List<MetricLinePoint>,
    val min: List<MetricLinePoint>,
    val max: List<MetricLinePoint>,
) {
    val hasRange: Boolean =
        min.zip(max).any { (low, high) -> low.value != high.value }
}

internal fun respiratoryRateSeries(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    metricLabel: String,
    averageLabel: String,
    lowestLabel: String,
    highestLabel: String,
): List<MetricLineSeries> {
    val dailyValues = dailyRangeVitalsPoints(
        entries = entries,
        time = { it.time },
        value = { it.breathsPerMinute },
    )
    val rawPoints = entries.mapLinePoints(
        time = { it.time },
        value = { it.breathsPerMinute },
    )
    return if (selectedRange == TimeRange.DAY) {
        listOf(MetricLineSeries(rawPoints, respiratoryColor, metricLabel))
    } else {
        buildList {
            add(MetricLineSeries(dailyValues.average, respiratoryColor, averageLabel))
            if (dailyValues.hasRange) {
                add(MetricLineSeries(dailyValues.min, respiratoryColor.copy(alpha = 0.55f), lowestLabel))
                add(MetricLineSeries(dailyValues.max, VitalsColor.copy(alpha = 0.75f), highestLabel))
            }
        }
    }
}

internal fun bloodPressureSeries(
    entries: List<BloodPressureEntry>,
    selectedRange: TimeRange,
    systolicLabel: String,
    diastolicLabel: String,
): List<MetricLineSeries> {
    val sorted = entries.sortedBy { it.time }
    val systolic = sorted.mapLinePoints(
        time = { it.time },
        value = { it.systolicMmHg.toDouble() },
    )
    val diastolic = sorted.mapLinePoints(
        time = { it.time },
        value = { it.diastolicMmHg.toDouble() },
    )
    return if (selectedRange == TimeRange.DAY) {
        listOf(
            MetricLineSeries(systolic, VitalsColor, systolicLabel),
            MetricLineSeries(diastolic, HeartColor, diastolicLabel),
        )
    } else {
        listOf(
            MetricLineSeries(dailyAverageLinePoints(systolic), VitalsColor, systolicLabel),
            MetricLineSeries(dailyAverageLinePoints(diastolic), HeartColor, diastolicLabel),
        )
    }
}

internal fun heartRateSeries(
    summaries: List<HeartRateSummary>,
    averageLabel: String,
    lowestLabel: String,
    highestLabel: String,
): List<MetricLineSeries> {
    val sorted = summaries.sortedBy { it.date }
    val avgPoints = sorted.map { MetricLinePoint(date = it.date, value = it.avgBpm.toDouble()) }
    val minPoints = sorted.map { MetricLinePoint(date = it.date, value = it.minBpm.toDouble()) }
    val maxPoints = sorted.map { MetricLinePoint(date = it.date, value = it.maxBpm.toDouble()) }
    val hasRange = sorted.any { it.minBpm != it.maxBpm }

    return buildList {
        add(MetricLineSeries(avgPoints, HeartColor, averageLabel))
        if (hasRange) {
            add(MetricLineSeries(minPoints, HeartColor.copy(alpha = 0.55f), lowestLabel))
            add(MetricLineSeries(maxPoints, HeartColor.copy(alpha = 0.9f), highestLabel))
        }
    }
}

internal fun heartRateRangeSummary(summaries: List<HeartRateSummary>): LongRangeSummary? {
    if (summaries.isEmpty()) return null
    return LongRangeSummary(
        average = summaries.map { it.avgBpm }.averageOrZero().roundToInt().toLong(),
        min = summaries.minOf { it.minBpm },
        max = summaries.maxOf { it.maxBpm },
    )
}

// Null rather than an invented range: the old 40/80 bpm fallback was never measured.
internal fun restingHeartRateRangeSummary(entries: List<DailyRestingHR>): LongRangeSummary? {
    val average = entries.map { it.bpm }.averageOrNull() ?: return null
    return LongRangeSummary(
        average = average.roundToInt().toLong(),
        min = entries.minOf { it.bpm },
        max = entries.maxOf { it.bpm },
    )
}

internal fun hrvRangeSummary(entries: List<DailyHrv>): DoubleRangeSummary? {
    val average = entries.map { it.rmssdMs }.averageOrNull() ?: return null
    return DoubleRangeSummary(
        average = average,
        min = entries.minOf { it.rmssdMs },
        max = entries.maxOf { it.rmssdMs },
    )
}

internal fun <T> dailyRangeVitalsPoints(
    entries: List<T>,
    time: (T) -> Instant,
    value: (T) -> Double,
): VitalsDailyRange {
    val dayRanges = entries
        .groupBy { time(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        // groupBy never yields an empty group. Minute-bucketed like every per-day mean.
        .map { (date, dayEntries) ->
            val values = dayEntries.map(value)
            val average = dayEntries.timeBucketedAverageOrNull(time = time, value = value) ?: 0.0
            MetricLinePoint(date = date, value = average) to
                (MetricLinePoint(date = date, value = values.min()) to
                    MetricLinePoint(date = date, value = values.max()))
        }
        .sortedBy { it.first.date }

    return VitalsDailyRange(
        average = dayRanges.map { it.first },
        min = dayRanges.map { it.second.first },
        max = dayRanges.map { it.second.second },
    )
}
