package tech.mmarca.openvitals.features.cycle

import java.time.LocalDate
import java.time.ZoneId
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.cycle.BbtReading
import tech.mmarca.openvitals.domain.cycle.CurrentCyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleFacts
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleStats
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleStatsCalculator
import tech.mmarca.openvitals.domain.cycle.PhaseIndeterminateReason
import tech.mmarca.openvitals.domain.cycle.PhaseTips
import tech.mmarca.openvitals.domain.cycle.SymptomDay
import tech.mmarca.openvitals.domain.cycle.SymptomPatternCalculator
import tech.mmarca.openvitals.domain.cycle.ThermalShiftCalculator
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.DayBleedingChoice

object CyclePresentationMapper {

    private const val RECENT_SYMPTOM_DAYS = 3L

    fun build(
        query: PeriodLoadQuery,
        data: CycleData,
        statistics: CycleStatistics? = null,
        journalEntries: List<CycleJournalEntry> = emptyList(),
        allJournalEntries: List<CycleJournalEntry> = journalEntries,
        currentCycleTemperatures: List<BasalBodyTemperatureEntry> = emptyList(),
        profile: CycleTrackingProfile = CycleTrackingProfile(),
        today: LocalDate = LocalDate.now(),
        pill: PillTodayDisplay? = null,
    ): CycleDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val zone = ZoneId.systemDefault()
        val journalByDate = journalEntries.associateBy { it.date }
        val calendarDays = cycleDays(
            period = selectedPeriod,
            data = data,
            zone = zone,
            predictedWindows = statistics?.predictedWindows.orEmpty(),
            journalByDate = journalByDate,
            cycleStarts = statistics?.cycleStarts.orEmpty().toSet(),
            today = today,
        )
        val periodDays = calendarDays.count { day ->
            day.inSelectedPeriod && (day.periodActive || day.flows.isNotEmpty())
        }
        val trackedDates = data.trackedDates(zone) + journalEntries.map { it.date }
        val allByDate = allJournalEntries.associateBy { it.date }

        return CycleDisplayState(
            selectedPeriod = selectedPeriod,
            hasData = data.hasData || journalEntries.any { it.hasObservations },
            summary = CyclePeriodSummary(
                periodDays = periodDays,
                ovulationTestCount = data.ovulationTests.size,
                bbtReadingCount = data.basalBodyTemperature.size,
                totalEntryCount = data.entryCount() + journalEntries.count { it.hasObservations },
            ),
            calendarDays = calendarDays,
            trackedDates = trackedDates,
            sampleCount = data.entryCount() + journalEntries.count { it.hasObservations },
            sources = data.allSources(),
            today = todayDisplay(statistics, data, allByDate, profile, today, zone, pill),
            history = statistics?.let { LongitudinalCycleStatsCalculator.calculate(it.cycles, today) }
                ?: LongitudinalCycleStats.Empty,
            thermal = thermalDisplay(statistics, currentCycleTemperatures, allByDate, zone),
            patterns = statistics?.let { stats ->
                SymptomPatternCalculator.calculate(
                    cycles = stats.cycles,
                    days = allJournalEntries.map { SymptomDay(it.date, it.symptoms) },
                    bleedingDays = stats.cycles.flatMap { it.flowDays.keys + it.spottingDays }.toSet(),
                )
            }.orEmpty(),
        )
    }

    private fun todayDisplay(
        statistics: CycleStatistics?,
        data: CycleData,
        journalByDate: Map<LocalDate, CycleJournalEntry>,
        profile: CycleTrackingProfile,
        today: LocalDate,
        zone: ZoneId,
        pill: PillTodayDisplay?,
    ): CycleTodayDisplay {
        val journal = journalByDate[today]
        val phase = statistics?.currentPhase
            ?: CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE)
        val recentSymptoms = (0..RECENT_SYMPTOM_DAYS)
            .mapNotNull { journalByDate[today.minusDays(it)] }
            .flatMapTo(mutableSetOf()) { it.symptoms }
        val todayFlow = data.menstruationFlows
            .filter { it.time.atZone(zone).toLocalDate() == today }
            .maxOfOrNull { it.flow }
        val bleeding = when {
            todayFlow != null -> DayBleedingChoice.Flow(todayFlow)
            statistics?.currentCycle?.flowDays?.containsKey(today) == true ->
                DayBleedingChoice.Flow(statistics.currentCycle!!.flowDays.getValue(today))
            statistics?.currentCycle?.spottingDays?.contains(today) == true -> DayBleedingChoice.Spotting
            journal?.bleedingNone == true -> DayBleedingChoice.None
            else -> null
        }
        return CycleTodayDisplay(
            today = today,
            currentCycle = statistics?.currentCycle,
            cycleDay = statistics?.currentCycleDay,
            phase = phase,
            estimate = statistics?.estimate ?: CycleEstimateResult.NeedsMoreHistory,
            tip = (phase as? CurrentCyclePhase.Available)?.let {
                PhaseTips.forDate(it.phase, today, profile.contexts, recentSymptoms)
            },
            fact = if (phase is CurrentCyclePhase.Indeterminate) CycleFacts.forDate(today) else null,
            journal = journal,
            bleeding = bleeding,
            recentIntervalLengths = statistics?.recentIntervalLengths.orEmpty(),
            totalCycles = statistics?.cycles?.size ?: 0,
            hasProfile = profile.contexts.isNotEmpty() || profile.ageBand != null,
            pill = pill,
        )
    }

    private fun thermalDisplay(
        statistics: CycleStatistics?,
        temperatures: List<BasalBodyTemperatureEntry>,
        journalByDate: Map<LocalDate, CycleJournalEntry>,
        zone: ZoneId,
    ): CycleThermalDisplay? {
        val cycle = statistics?.currentCycle ?: return null
        val readings = temperatures
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .filterKeys { !it.isBefore(cycle.startDate) }
            .map { (date, entries) ->
                val latest = entries.maxBy { it.time }
                BbtReading(
                    date = date,
                    celsius = latest.temperatureCelsius,
                    disturbed = journalByDate[date]?.bbtDisturbances.orEmpty().isNotEmpty(),
                )
            }
            .sortedBy { it.date }
        if (readings.isEmpty()) return null
        return CycleThermalDisplay(
            cycleStart = cycle.startDate,
            readings = readings,
            shift = ThermalShiftCalculator.evaluateCycle(cycle.startDate, readings),
        )
    }
}

private fun CycleData.trackedDates(zone: ZoneId): List<LocalDate> =
    menstruationFlows.map { it.time.atZone(zone).toLocalDate() } +
        menstruationPeriods.map { it.startTime.atZone(zone).toLocalDate() } +
        ovulationTests.map { it.time.atZone(zone).toLocalDate() } +
        cervicalMucus.map { it.time.atZone(zone).toLocalDate() } +
        basalBodyTemperature.map { it.time.atZone(zone).toLocalDate() } +
        intermenstrualBleeding.map { it.time.atZone(zone).toLocalDate() } +
        sexualActivity.map { it.time.atZone(zone).toLocalDate() }

private fun CycleData.entryCount(): Int =
    menstruationFlows.size +
        menstruationPeriods.size +
        ovulationTests.size +
        cervicalMucus.size +
        basalBodyTemperature.size +
        intermenstrualBleeding.size +
        sexualActivity.size

private fun CycleData.allSources(): List<String> =
    menstruationFlows.map { it.source } +
        menstruationPeriods.map { it.source } +
        ovulationTests.map { it.source } +
        cervicalMucus.map { it.source } +
        basalBodyTemperature.map { it.source } +
        intermenstrualBleeding.map { it.source } +
        sexualActivity.map { it.source }
