package tech.mmarca.openvitals.features.heart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartBucket
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.periodBarBuckets
import java.time.LocalDate
import java.time.ZoneId

internal fun respiratoryRateSummaryMetric(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
): SummaryMetric? {
    if (entries.isEmpty()) return null

    if (selectedRange == TimeRange.DAY) {
        val latest = entries.maxByOrNull { it.time } ?: return null
        val value = unitFormatter.respiratoryRate(latest.breathsPerMinute)
        return SummaryMetric("Respiratory rate", value.value, value.unit, Icons.Outlined.Air, respiratoryColor, latest.source)
    }

    val average = respiratoryRateAverage(respiratoryRateBuckets(entries, selectedRange, period))
    val value = unitFormatter.respiratoryRate(average)
    val source = entries.map { it.source }.distinct().singleOrNull() ?: "${entries.size} readings"
    return SummaryMetric("Avg respiratory rate", value.value, value.unit, Icons.Outlined.Air, respiratoryColor, source)
}

internal fun respiratoryRateBuckets(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
) = periodBarBuckets(
    values = entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(
                date = date,
                value = dayEntries.map { it.breathsPerMinute }.average(),
            )
        },
    selectedRange = selectedRange,
    period = period,
    yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
)

internal fun respiratoryRateAverage(buckets: List<PeriodChartBucket>): Double =
    buckets
        .map { it.value }
        .filter { it > 0.0 }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: 0.0

internal data class RespiratoryRateDaySummary(
    val date: LocalDate,
    val average: Double,
    val min: Double,
    val max: Double,
    val readings: Int,
)

internal fun respiratoryRateDaySummaries(entries: List<RespiratoryRateEntry>): List<RespiratoryRateDaySummary> =
    entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            val values = dayEntries.map { it.breathsPerMinute }
            RespiratoryRateDaySummary(
                date = date,
                average = values.average(),
                min = values.minOrNull() ?: 0.0,
                max = values.maxOrNull() ?: 0.0,
                readings = values.size,
            )
        }
