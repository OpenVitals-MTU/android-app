package tech.mmarca.openvitals.domain.report

import java.time.ZoneId
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues
import tech.mmarca.openvitals.domain.model.ReportGlucoseContextAverage
import tech.mmarca.openvitals.domain.model.ReportGlucoseDetail
import tech.mmarca.openvitals.domain.model.ReportGlucoseReading
import tech.mmarca.openvitals.domain.model.ReportMetricSummary

/** The clinical ordering of meal contexts: fasting first. */
private val ContextOrder = listOf(
    GlucoseRecordValues.RELATION_TO_MEAL_FASTING,
    GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL,
    GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL,
    GlucoseRecordValues.RELATION_TO_MEAL_GENERAL,
    GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN,
)

/** [dedupeReadings] keyed on value and meal context — see that helper for the rule. */
fun distinctBloodGlucoseReadings(entries: List<BloodGlucoseEntry>): List<BloodGlucoseEntry> =
    dedupeReadings(entries, time = { it.time }, key = { it.millimolesPerLiter to it.relationToMeal })

/** The glucose section: reading list, per-context averages, overall summary. Null when empty. */
fun bloodGlucoseDetail(
    entries: List<BloodGlucoseEntry>,
    zone: ZoneId,
): ReportGlucoseDetail? {
    if (entries.isEmpty()) return null

    val readings = distinctBloodGlucoseReadings(entries)
        .map { ReportGlucoseReading(it.time, it.millimolesPerLiter, it.relationToMeal) }

    val byContext = readings.groupBy { normalizedContext(it.relationToMeal) }
    val contextAverages = buildList {
        ContextOrder.forEach { context ->
            byContext[context]?.let { add(contextAverage(context, it)) }
        }
        add(contextAverage(relationToMeal = null, readings))
    }

    val values = readings.map { it.millimolesPerLiter }
    return ReportGlucoseDetail(
        readings = readings,
        contextAverages = contextAverages,
        summary = ReportMetricSummary(
            average = values.average(),
            min = values.min(),
            max = values.max(),
            total = null,
            daysWithData = readings.map { it.time.atZone(zone).toLocalDate() }.distinct().size,
        ),
    )
}

/** A relation value outside the known constants reads as UNKNOWN, not a crash. */
private fun normalizedContext(relationToMeal: Int): Int =
    if (relationToMeal in ContextOrder) relationToMeal else GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN

private fun contextAverage(relationToMeal: Int?, readings: List<ReportGlucoseReading>) =
    ReportGlucoseContextAverage(
        relationToMeal = relationToMeal,
        average = readings.map { it.millimolesPerLiter }.average(),
        min = readings.minOf { it.millimolesPerLiter },
        max = readings.maxOf { it.millimolesPerLiter },
        readings = readings.size,
    )
