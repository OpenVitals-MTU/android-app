package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Cross-cycle statistics derived from bleeding days.
 *
 * All fields degrade gracefully with sparse data: [currentCycleDay] is null
 * without a recent cycle start, the length statistics and [predictedWindows]
 * are empty until [CycleCalculations.MinCompletedCyclesForPrediction] cycles
 * have completed.
 */
data class CycleStatistics(
    val cycleStarts: List<LocalDate> = emptyList(),
    val currentCycleDay: Int? = null,
    val averageCycleLengthDays: Double? = null,
    val stdDevCycleLengthDays: Double? = null,
    val predictedWindows: List<ClosedRange<LocalDate>> = emptyList(),
)

/**
 * Pure cycle arithmetic on local calendar days.
 *
 * The rules reimplement the statistical method popularised by drip
 * (bloodyhealth/drip): a cycle starts on a bleeding day preceded by more than
 * [MaxBreakInBleedingDays] bleeding-free days, predictions need
 * [MinCompletedCyclesForPrediction] completed cycles and use the mean cycle
 * length with a spread chosen by the standard deviation. No fertility or
 * ovulation inference is performed here.
 *
 * Callers map instants to [LocalDate] in the current system zone; a timezone
 * change can therefore shift a bleeding day by one, which can move a cycle
 * start accordingly. This matches how the rest of the app buckets days.
 */
object CycleCalculations {

    const val MaxBreakInBleedingDays = 1
    const val MinCompletedCyclesForPrediction = 3
    const val MaxPredictedWindows = 3
    const val MaxDisplayableCycleDay = 99
    const val NarrowWindowStdDevThreshold = 1.5

    /**
     * Groups bleeding days into contiguous segments, tolerating gaps of at
     * most [maxBreakDays] bleeding-free days inside one segment. Each
     * segment's start is a cycle start; the segments are also the desired
     * menstruation period spans.
     */
    fun bleedingSegments(
        bleedingDays: Collection<LocalDate>,
        maxBreakDays: Int = MaxBreakInBleedingDays,
    ): List<ClosedRange<LocalDate>> {
        if (bleedingDays.isEmpty()) return emptyList()
        val sorted = bleedingDays.toSortedSet().toList()
        val segments = mutableListOf<ClosedRange<LocalDate>>()
        var start = sorted.first()
        var end = sorted.first()
        for (day in sorted.drop(1)) {
            val gap = ChronoUnit.DAYS.between(end, day) - 1
            if (gap > maxBreakDays) {
                segments.add(start..end)
                start = day
            }
            end = day
        }
        segments.add(start..end)
        return segments
    }

    fun compute(bleedingDays: Collection<LocalDate>, today: LocalDate): CycleStatistics {
        val starts = bleedingSegments(bleedingDays).map { it.start }
        if (starts.isEmpty()) return CycleStatistics()

        val lastStart = starts.last()
        val daysSinceStart = ChronoUnit.DAYS.between(lastStart, today)
        val currentCycleDay = (daysSinceStart + 1)
            .takeIf { daysSinceStart >= 0 && it <= MaxDisplayableCycleDay }
            ?.toInt()

        val lengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toDouble() }
        if (lengths.size < MinCompletedCyclesForPrediction) {
            return CycleStatistics(cycleStarts = starts, currentCycleDay = currentCycleDay)
        }

        val mean = lengths.average()
        val stdDev = sqrt(lengths.sumOf { (it - mean) * (it - mean) } / lengths.size)
        val periodDistance = mean.roundToLong()
        val variation = if (stdDev < NarrowWindowStdDevThreshold) 1L else 2L

        // Overlapping windows carry no information; suppress prediction when
        // the cycle length varies too much relative to its own spread.
        val windows = if (periodDistance - 5 < variation) {
            emptyList()
        } else {
            (1..MaxPredictedWindows).map { i ->
                val predictedStart = lastStart.plusDays(periodDistance * i)
                predictedStart.minusDays(variation)..predictedStart.plusDays(variation)
            }
        }

        return CycleStatistics(
            cycleStarts = starts,
            currentCycleDay = currentCycleDay,
            averageCycleLengthDays = mean,
            stdDevCycleLengthDays = stdDev,
            predictedWindows = windows,
        )
    }
}
