package tech.mmarca.openvitals.features.cycle

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.CurrentCyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CyclePhase

/** Where today sits against the estimate. Pure, so the widget and the tile agree. */
@Immutable
sealed interface CycleEstimateSummary {
    data object NeedsHistory : CycleEstimateSummary
    data object OutOfRange : CycleEstimateSummary
    data class Range(val earliest: LocalDate, val latest: LocalDate) : CycleEstimateSummary
    data object InProgress : CycleEstimateSummary
    data class PastWindow(val days: Int) : CycleEstimateSummary
}

fun cycleEstimateSummary(estimate: CycleEstimateResult, today: LocalDate): CycleEstimateSummary = when (estimate) {
    CycleEstimateResult.NeedsMoreHistory -> CycleEstimateSummary.NeedsHistory
    CycleEstimateResult.IntervalsOutOfRange -> CycleEstimateSummary.OutOfRange
    is CycleEstimateResult.Available -> {
        val window = estimate.estimate
        when {
            today.isAfter(window.latestDate) ->
                CycleEstimateSummary.PastWindow(ChronoUnit.DAYS.between(window.latestDate, today).toInt())
            !today.isBefore(window.centralDate) -> CycleEstimateSummary.InProgress
            else -> CycleEstimateSummary.Range(window.earliestDate, window.latestDate)
        }
    }
}

/** The second line under a recorded cycle day: the phase when it is known, else the estimate. */
@Immutable
sealed interface CycleSecondaryLine {
    data class Phase(val phase: CyclePhase) : CycleSecondaryLine
    data class Estimate(val summary: CycleEstimateSummary) : CycleSecondaryLine
}

fun cycleSecondaryLine(phase: CurrentCyclePhase, estimate: CycleEstimateResult, today: LocalDate): CycleSecondaryLine =
    when (phase) {
        is CurrentCyclePhase.Available -> CycleSecondaryLine.Phase(phase.phase)
        is CurrentCyclePhase.Indeterminate -> CycleSecondaryLine.Estimate(cycleEstimateSummary(estimate, today))
    }

/** The one-line estimate a widget shows under the cycle day. */
fun cycleEstimateLine(context: Context, estimate: CycleEstimateResult, today: LocalDate, formatter: DateTimeFormatter): String =
    when (val summary = cycleEstimateSummary(estimate, today)) {
        CycleEstimateSummary.NeedsHistory -> context.getString(R.string.widget_cycle_estimate_needs_history)
        CycleEstimateSummary.OutOfRange -> context.getString(R.string.widget_cycle_estimate_out_of_range)
        CycleEstimateSummary.InProgress -> context.getString(R.string.widget_cycle_estimate_in_progress)
        is CycleEstimateSummary.PastWindow ->
            context.resources.getQuantityString(R.plurals.cycle_estimate_past_window, summary.days, summary.days)
        is CycleEstimateSummary.Range -> context.getString(
            R.string.widget_cycle_estimate_range,
            formatter.format(summary.earliest),
            formatter.format(summary.latest),
        )
    }

/** The same second line, for a widget that has a context and no composition. */
fun cycleSummarySecondaryLine(
    context: Context,
    phase: CurrentCyclePhase,
    estimate: CycleEstimateResult,
    today: LocalDate,
    formatter: DateTimeFormatter,
): String = when (phase) {
    is CurrentCyclePhase.Available -> context.getString(cyclePhaseLabelRes(phase.phase))
    is CurrentCyclePhase.Indeterminate -> cycleEstimateLine(context, estimate, today, formatter)
}

@Composable
fun cycleEstimateSummaryText(summary: CycleEstimateSummary, formatter: DateTimeFormatter): String = when (summary) {
    CycleEstimateSummary.NeedsHistory -> stringResource(R.string.widget_cycle_estimate_needs_history)
    CycleEstimateSummary.OutOfRange -> stringResource(R.string.widget_cycle_estimate_out_of_range)
    CycleEstimateSummary.InProgress -> stringResource(R.string.widget_cycle_estimate_in_progress)
    is CycleEstimateSummary.PastWindow -> pluralStringResource(R.plurals.cycle_estimate_past_window, summary.days, summary.days)
    is CycleEstimateSummary.Range -> stringResource(
        R.string.widget_cycle_estimate_range,
        formatter.format(summary.earliest),
        formatter.format(summary.latest),
    )
}

@Composable
fun cycleSecondaryLineText(line: CycleSecondaryLine, formatter: DateTimeFormatter): String = when (line) {
    is CycleSecondaryLine.Phase -> stringResource(cyclePhaseLabelRes(line.phase))
    is CycleSecondaryLine.Estimate -> cycleEstimateSummaryText(line.summary, formatter)
}
