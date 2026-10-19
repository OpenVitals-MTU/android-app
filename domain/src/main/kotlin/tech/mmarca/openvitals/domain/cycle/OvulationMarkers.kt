package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.ZoneId
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues

/** A recorded sign that ovulation has passed. Each is read back from the record, never predicted. */
enum class OvulationMarker {
    /** The first positive LH test of the cycle. */
    LH_TEST,

    /** The first high day of a confirmed six-low, three-high temperature shift. */
    TEMPERATURE,

    /** The last day of best-quality cervical mucus, once three lesser days follow it. */
    MUCUS,
}

/**
 * The day-level observations the markers are read from, one mapping for the
 * estimate and the thermal chart alike.
 *
 * @param mucusDays every day with a mucus observation; true when it was of the best quality.
 */
data class CycleObservations(
    val lhPositiveDays: Set<LocalDate> = emptySet(),
    val bbtReadings: List<BbtReading> = emptyList(),
    val mucusDays: Map<LocalDate, Boolean> = emptyMap(),
) {
    companion object {
        /**
         * The latest temperature of a day counts; a journal disturbance flags it.
         * Best mucus is egg-white appearance or a wet or slippery sensation. Watery
         * mucus is left out: no source places it at the peak.
         */
        fun from(data: CycleData, journal: List<CycleJournalEntry>, zone: ZoneId): CycleObservations {
            val journalByDate = journal.associateBy { it.date }
            val bbt = data.basalBodyTemperature
                .groupBy { it.time.atZone(zone).toLocalDate() }
                .map { (date, entries) ->
                    BbtReading(
                        date = date,
                        celsius = entries.maxBy { it.time }.temperatureCelsius,
                        disturbed = journalByDate[date]?.bbtDisturbances.orEmpty().isNotEmpty(),
                    )
                }
                .sortedBy { it.date }
            val mucus = mutableMapOf<LocalDate, Boolean>()
            data.cervicalMucus
                .filter { it.appearance != CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN }
                .forEach { entry ->
                    val date = entry.time.atZone(zone).toLocalDate()
                    val best = entry.appearance == CycleRecordValues.MUCUS_APPEARANCE_EGG_WHITE
                    mucus[date] = (mucus[date] ?: false) || best
                }
            journal.forEach { entry ->
                val sensation = entry.cervicalSensation ?: return@forEach
                val best = sensation == CervicalSensation.WET || sensation == CervicalSensation.SLIPPERY
                mucus[entry.date] = (mucus[entry.date] ?: false) || best
            }
            return CycleObservations(
                lhPositiveDays = data.ovulationTests
                    .filter { it.result == CycleRecordValues.OVULATION_POSITIVE }
                    .mapTo(mutableSetOf()) { it.time.atZone(zone).toLocalDate() },
                bbtReadings = bbt,
                mucusDays = mucus,
            )
        }
    }
}

/** Finds the markers recorded inside one cycle, up to [today]. */
object OvulationMarkerDetector {

    /** Lesser-quality days that must follow the best day before it counts as the peak. */
    private const val MUCUS_DAYS_AFTER_PEAK = 3

    fun detect(cycle: RecordedCycle, observations: CycleObservations, today: LocalDate): Map<OvulationMarker, LocalDate> {
        val inCycle = { date: LocalDate -> cycle.contains(date) && !date.isAfter(today) }
        val markers = mutableMapOf<OvulationMarker, LocalDate>()
        observations.lhPositiveDays.filter(inCycle).minOrNull()?.let { markers[OvulationMarker.LH_TEST] = it }
        val shift = ThermalShiftCalculator.evaluateCycle(cycle.startDate, observations.bbtReadings.filter { inCycle(it.date) })
        (shift as? ThermalShiftResult.Confirmed)?.let { markers[OvulationMarker.TEMPERATURE] = it.firstHighDay }
        mucusPeak(observations.mucusDays.filterKeys(inCycle))?.let { markers[OvulationMarker.MUCUS] = it }
        return markers
    }

    /** The first best day followed by three observed lesser days. A missing day leaves the peak unknown. */
    private fun mucusPeak(days: Map<LocalDate, Boolean>): LocalDate? =
        days.filterValues { it }.keys.sorted().firstOrNull { best ->
            (1..MUCUS_DAYS_AFTER_PEAK).all { offset -> days[best.plusDays(offset.toLong())] == false }
        }
}
