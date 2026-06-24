package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSummary
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

internal data class LongRangeSummary(
    val average: Long,
    val min: Long,
    val max: Long,
)

internal data class DoubleRangeSummary(
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
        average = summaries.map { it.avgBpm }.average().roundToInt().toLong(),
        min = summaries.minOf { it.minBpm },
        max = summaries.maxOf { it.maxBpm },
    )
}

internal fun restingHeartRateRangeSummary(entries: List<DailyRestingHR>): LongRangeSummary {
    return LongRangeSummary(
        average = entries.map { it.bpm }.average().takeUnless { it.isNaN() }?.roundToInt()?.toLong() ?: 0L,
        min = entries.minOfOrNull { it.bpm } ?: 40L,
        max = entries.maxOfOrNull { it.bpm } ?: 80L,
    )
}

internal fun hrvRangeSummary(entries: List<DailyHrv>): DoubleRangeSummary {
    return DoubleRangeSummary(
        average = entries.map { it.rmssdMs }.average().takeUnless { it.isNaN() } ?: 0.0,
        min = entries.minOfOrNull { it.rmssdMs } ?: 0.0,
        max = entries.maxOfOrNull { it.rmssdMs } ?: 100.0,
    )
}

internal fun <T> dailyRangeVitalsPoints(
    entries: List<T>,
    time: (T) -> Instant,
    value: (T) -> Double,
): VitalsDailyRange {
    val dayRanges = entries
        .groupBy { time(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            val values = dayEntries.map(value)
            MetricLinePoint(date = date, value = values.average()) to
                (MetricLinePoint(date = date, value = values.minOrNull() ?: 0.0) to
                    MetricLinePoint(date = date, value = values.maxOrNull() ?: 0.0))
        }
        .sortedBy { it.first.date }

    return VitalsDailyRange(
        average = dayRanges.map { it.first },
        min = dayRanges.map { it.second.first },
        max = dayRanges.map { it.second.second },
    )
}
