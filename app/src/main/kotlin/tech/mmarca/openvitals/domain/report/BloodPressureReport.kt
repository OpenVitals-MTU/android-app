package tech.mmarca.openvitals.domain.report

import java.time.ZoneId
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BpMealContext
import tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail
import tech.mmarca.openvitals.domain.model.ReportBpReading
import tech.mmarca.openvitals.domain.model.ReportBpSlotAverage
import tech.mmarca.openvitals.domain.model.ReportMetricSummary

/** [dedupeReadings] on the value pair. At equal instants the copy with a meal context wins. */
fun distinctBloodPressureReadings(entries: List<BloodPressureEntry>): List<BloodPressureEntry> =
    dedupeReadings(
        entries.sortedWith(compareBy({ it.time }, { it.mealContext == null })),
        time = { it.time },
        key = { it.systolicMmHg to it.diastolicMmHg },
    )

/**
 * The blood-pressure section: reading list, meal-context averages and
 * per-component summaries. Context is the user's choice on OpenVitals
 * records, else estimated from typical meal hours.
 */
fun bloodPressureDetail(
    entries: List<BloodPressureEntry>,
    zone: ZoneId,
): ReportBloodPressureDetail? {
    if (entries.isEmpty()) return null

    val readings = distinctBloodPressureReadings(entries)
        .map { entry ->
            ReportBpReading(
                time = entry.time,
                systolicMmHg = entry.systolicMmHg,
                diastolicMmHg = entry.diastolicMmHg,
                context = entry.mealContext ?: estimatedContext(entry.time.atZone(zone).hour),
                contextEstimated = entry.mealContext == null,
                bodyPosition = entry.bodyPosition,
                measurementLocation = entry.measurementLocation,
            )
        }

    val byContext = readings.groupBy { it.context }
    val slotAverages = buildList {
        BpMealContext.entries.forEach { context ->
            byContext[context]?.let { add(slotAverage(context, it)) }
        }
        add(slotAverage(context = null, readings))
    }

    return ReportBloodPressureDetail(
        readings = readings,
        slotAverages = slotAverages,
        systolic = componentSummary(readings, zone) { it.systolicMmHg },
        diastolic = componentSummary(readings, zone) { it.diastolicMmHg },
    )
}

/** The time-of-day fallback for records that carry no explicit context. */
internal fun estimatedContext(hourOfDay: Int): BpMealContext =
    when (hourOfDay) {
        in 4..7 -> BpMealContext.BEFORE_BREAKFAST
        in 8..10 -> BpMealContext.AFTER_BREAKFAST
        in 11..13 -> BpMealContext.BEFORE_LUNCH
        in 14..16 -> BpMealContext.AFTER_LUNCH
        in 17..19 -> BpMealContext.BEFORE_DINNER
        else -> BpMealContext.AFTER_DINNER
    }

private fun slotAverage(context: BpMealContext?, readings: List<ReportBpReading>) =
    ReportBpSlotAverage(
        context = context,
        systolic = readings.map { it.systolicMmHg }.average(),
        diastolic = readings.map { it.diastolicMmHg }.average(),
        readings = readings.size,
    )

private fun componentSummary(
    readings: List<ReportBpReading>,
    zone: ZoneId,
    component: (ReportBpReading) -> Int,
): ReportMetricSummary {
    val values = readings.map { component(it).toDouble() }
    return ReportMetricSummary(
        average = values.average(),
        min = values.min(),
        max = values.max(),
        total = null,
        daysWithData = readings.map { it.time.atZone(zone).toLocalDate() }.distinct().size,
    )
}
