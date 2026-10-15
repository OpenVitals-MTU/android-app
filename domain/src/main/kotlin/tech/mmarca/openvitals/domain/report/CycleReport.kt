package tech.mmarca.openvitals.domain.report

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.ReportCycleDetail
import tech.mmarca.openvitals.domain.model.ReportCycleNote
import tech.mmarca.openvitals.domain.model.ReportCycleRow
import tech.mmarca.openvitals.domain.model.ReportDailyValue
import tech.mmarca.openvitals.domain.model.ReportSymptomFrequency

/** Pain is logged 1 to 5, where 1 is none. */
private const val PainDayLevel = 2
private const val SeverePainLevel = 4

/** Spotting this many days or more after a start is intermenstrual, not the period's tail. */
private const val IntermenstrualOffsetDays = 7L

/** The cycle day per calendar day in the range: the chart's sawtooth, one tooth per cycle. */
fun cycleDailySeries(cycles: List<RecordedCycle>, start: LocalDate, end: LocalDate): List<ReportDailyValue> =
    cycles.flatMap { cycle ->
        val first = maxOf(cycle.startDate, start)
        val last = minOf(cycle.endDate ?: end, end)
        generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .map { date -> ReportDailyValue(date, (ChronoUnit.DAYS.between(cycle.startDate, date) + 1).toDouble()) }
            .toList()
    }.sortedBy { it.date }

/**
 * The cycle section: one row per cycle touching the range, length statistics
 * over the closed ones, bleeding and pain counted over the range's days, and
 * the symptoms and notes the journal holds. Null with nothing to report.
 */
fun cycleDetail(
    cycles: List<RecordedCycle>,
    journal: List<CycleJournalEntry>,
    start: LocalDate,
    end: LocalDate,
): ReportCycleDetail? {
    val inRange = cycles
        .filter { !it.startDate.isAfter(end) && (it.endDate == null || !it.endDate.isBefore(start)) }
        .sortedBy { it.startDate }
    val entries = journal.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }.sortedBy { it.date }
    if (inRange.isEmpty() && entries.isEmpty()) return null

    val flowDays = inRange.flatMap { it.flowDays.keys }.filter { it in start..end }.toSet()
    val spottingDays = inRange.flatMap { it.spottingDays }.filter { it in start..end }.toSet()
    val bleedingDays = flowDays + spottingDays
    val painByDate = entries.mapNotNull { entry -> entry.painLevel?.let { entry.date to it } }.toMap()

    val rows = inRange.map { cycle ->
        ReportCycleRow(
            start = cycle.startDate,
            end = cycle.endDate,
            lengthDays = cycle.endDate?.let { cycle.lengthDays(it) },
            bleedingDays = cycle.bleedingDayCount,
            peakFlow = cycle.peakFlow,
            painDays = painByDate.count { (date, level) -> cycle.contains(date) && level >= PainDayLevel },
            excluded = cycle.isExcludedFromEstimates,
            exclusionReason = cycle.exclusionReason,
        )
    }
    val lengths = rows.mapNotNull { it.lengthDays }.sorted()
    val pain = painByDate.values
    val painOn = painByDate.count { (date, level) -> level >= PainDayLevel && date in bleedingDays }
    val painOff = painByDate.count { (date, level) -> level >= PainDayLevel && date !in bleedingDays }

    return ReportCycleDetail(
        cycles = rows,
        completedCycles = lengths.size,
        meanLengthDays = lengths.takeIf { it.isNotEmpty() }?.average(),
        medianLengthDays = median(lengths),
        sdLengthDays = sampleStdDev(lengths),
        minLengthDays = lengths.firstOrNull(),
        maxLengthDays = lengths.lastOrNull(),
        meanBleedingDays = rows.takeIf { it.isNotEmpty() }?.map { it.bleedingDays }?.average(),
        bleedingDays = flowDays.size,
        spottingDays = spottingDays.size,
        intermenstrualDays = spottingDays.count { day ->
            val cycle = inRange.firstOrNull { it.contains(day) }
            cycle != null && ChronoUnit.DAYS.between(cycle.startDate, day) >= IntermenstrualOffsetDays
        },
        painDaysOnBleeding = painOn,
        painDaysOffBleeding = painOff,
        severePainDays = pain.count { it >= SeverePainLevel },
        meanPain = pain.takeIf { it.isNotEmpty() }?.average(),
        symptomFrequency = symptomFrequency(entries, bleedingDays),
        notes = entries.filter { it.notes.isNotBlank() }.map { ReportCycleNote(it.date, it.notes) },
    )
}

private fun symptomFrequency(entries: List<CycleJournalEntry>, bleedingDays: Set<LocalDate>): List<ReportSymptomFrequency> =
    CycleSymptom.entries.mapNotNull { symptom ->
        val on = entries.count { symptom in it.symptoms && it.date in bleedingDays }
        val off = entries.count { symptom in it.symptoms && it.date !in bleedingDays }
        if (on + off == 0) null else ReportSymptomFrequency(symptom, on, off)
    }.sortedWith(compareByDescending<ReportSymptomFrequency> { it.onBleeding + it.offBleeding }.thenBy { it.symptom.ordinal })

private fun median(sorted: List<Int>): Double? {
    if (sorted.isEmpty()) return null
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle].toDouble() else (sorted[middle - 1] + sorted[middle]) / 2.0
}

private fun sampleStdDev(values: List<Int>): Double? {
    if (values.size < 2) return null
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1))
}
