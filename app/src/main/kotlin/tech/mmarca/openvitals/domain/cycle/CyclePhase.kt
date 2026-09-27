package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A phase label. On its own it carries no certainty; see [CurrentCyclePhase]. */
enum class CyclePhase {
    MENSTRUAL,
    FOLLICULAR,
    OVULATORY,
    LUTEAL,
}

/** Whether a phase comes from a recorded observation or a calculation. */
enum class PhaseCertainty {
    RECORDED,
    ESTIMATED,
}

/** Why the app declines to name a phase today. */
enum class PhaseIndeterminateReason {
    NO_CURRENT_CYCLE,
    EARLY_CYCLE_WITHOUT_BLEEDING_DETAIL,
    NEEDS_MORE_HISTORY,
    INTERVALS_OUT_OF_RANGE,
    PHASE_TRANSITION,
    NEXT_PERIOD_WINDOW,
    ESTIMATE_EXPIRED,
}

sealed interface CurrentCyclePhase {
    data class Available(val phase: CyclePhase, val certainty: PhaseCertainty) : CurrentCyclePhase

    data class Indeterminate(val reason: PhaseIndeterminateReason) : CurrentCyclePhase
}

/** What was recorded about bleeding on one day. Null means nothing was recorded. */
enum class DayBleeding {
    /** The user said there was no bleeding. */
    NONE,
    SPOTTING,

    /** Light, medium or heavy period flow. */
    FLOW,
}

/**
 * A conservative phase for today.
 *
 * Menstrual is recorded, never inferred from an average duration. The other
 * phases are estimates hung on the next-period window. Calendar data cannot
 * confirm ovulation, so the ovulatory label is limited to the central day and
 * to a stable history.
 *
 * Sources: Mihm et al. 2011 (phase physiology), Fehring et al. 2006 (phase
 * variability), NHS Periods (period length).
 */
object CurrentCyclePhaseCalculator {
    /** Periods generally last 2 to 7 days. Missing detail in this span stays unknown. */
    private const val EARLY_CYCLE_DAYS = 7

    /** The luteal phase is about 12 to 14 days; the ovulation anchor sits 13 before the central date. */
    private const val LUTEAL_ANCHOR_DAYS = 13L
    private const val OVULATION_EXTRA_RADIUS_DAYS = 2L

    /** The estimated luteal phase ends this many days before the central date. */
    private const val PRE_PERIOD_LEAD_DAYS = 2L

    private const val STABLE_HISTORY_INTERVALS = 6
    private const val STABLE_HISTORY_VARIABILITY_DAYS = 7

    fun evaluate(
        today: LocalDate,
        currentCycle: RecordedCycle?,
        todayBleeding: DayBleeding?,
        estimateResult: CycleEstimateResult,
    ): CurrentCyclePhase {
        val cycle = currentCycle?.takeIf { !today.isBefore(it.startDate) }
            ?: return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NO_CURRENT_CYCLE)

        val dayIndex = ChronoUnit.DAYS.between(cycle.startDate, today).toInt()
        val bleeding = if (today in cycle.flowDays) DayBleeding.FLOW else todayBleeding
        if (dayIndex == 0 || bleeding == DayBleeding.FLOW) {
            return CurrentCyclePhase.Available(CyclePhase.MENSTRUAL, PhaseCertainty.RECORDED)
        }
        if (dayIndex < EARLY_CYCLE_DAYS && (bleeding == null || bleeding == DayBleeding.SPOTTING)) {
            return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.EARLY_CYCLE_WITHOUT_BLEEDING_DETAIL)
        }

        val estimate = when (estimateResult) {
            CycleEstimateResult.NeedsMoreHistory ->
                return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NEEDS_MORE_HISTORY)
            CycleEstimateResult.IntervalsOutOfRange ->
                return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.INTERVALS_OUT_OF_RANGE)
            is CycleEstimateResult.Available -> estimateResult.estimate
        }
        if (today.isAfter(estimate.latestDate)) {
            return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.ESTIMATE_EXPIRED)
        }
        if (!today.isBefore(estimate.centralDate.minusDays(PRE_PERIOD_LEAD_DAYS))) {
            return CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.NEXT_PERIOD_WINDOW)
        }

        val follicularOpensOn = cycle.startDate.plusDays(EARLY_CYCLE_DAYS.toLong())
        val ovulationCentral = estimate.centralDate.minusDays(LUTEAL_ANCHOR_DAYS)
        val ovulationEarliest = maxOf(ovulationCentral.minusDays(OVULATION_EXTRA_RADIUS_DAYS), follicularOpensOn)
        val ovulationLatest = maxOf(ovulationCentral.plusDays(OVULATION_EXTRA_RADIUS_DAYS), ovulationEarliest)
        return when {
            today.isBefore(ovulationEarliest) ->
                CurrentCyclePhase.Available(CyclePhase.FOLLICULAR, PhaseCertainty.ESTIMATED)
            today.isAfter(ovulationLatest) ->
                CurrentCyclePhase.Available(CyclePhase.LUTEAL, PhaseCertainty.ESTIMATED)
            today == ovulationCentral && estimate.hasStableHistory() ->
                CurrentCyclePhase.Available(CyclePhase.OVULATORY, PhaseCertainty.ESTIMATED)
            else -> CurrentCyclePhase.Indeterminate(PhaseIndeterminateReason.PHASE_TRANSITION)
        }
    }

    private fun CycleEstimate.hasStableHistory(): Boolean =
        cycleCount >= STABLE_HISTORY_INTERVALS && variabilityDays <= STABLE_HISTORY_VARIABILITY_DAYS
}
