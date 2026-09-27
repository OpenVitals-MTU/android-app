package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.cycle.CurrentCyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CyclePhase
import tech.mmarca.openvitals.domain.cycle.PhaseCertainty
import tech.mmarca.openvitals.domain.model.DayBleedingChoice
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.components.ReferenceLinkButton
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val TrackHeight: Dp = 10.dp
private const val StackHeroFontScale = 1.3f

/** A small label that says whether a value was recorded or calculated. Never colour alone. */
@Composable
internal fun CycleStatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    OpenVitalsSurface(
        modifier = modifier,
        containerColor = color.copy(alpha = Emphasis.wash),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

/** The cycle day, recorded, with the phase beneath it. */
@Composable
internal fun CycleHeroCard(
    today: CycleTodayDisplay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val cycle = today.currentCycle
    OpenVitalsCard(modifier = modifier.fillMaxWidth(), accentColor = CycleColor) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (cycle == null || today.cycleDay == null) {
                Text(text = stringResource(R.string.cycle_no_history_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.cycle_no_history_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val day = today.cycleDay
                val stack = LocalDensity.current.fontScale > StackHeroFontScale
                val facts: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(text = stringResource(R.string.cycle_day_caption), style = MaterialTheme.typography.titleMedium)
                        CycleStatusPill(stringResource(R.string.cycle_recorded_label), CycleColor)
                        Text(
                            text = stringResource(
                                R.string.cycle_day_counted_from,
                                dateTimeFormatterProvider.mediumDate().format(cycle.startDate),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val ring: @Composable () -> Unit = {
                    CycleDayRing(dayOfCycle = day, accessibleLabel = stringResource(R.string.cycle_day_of_cycle, day))
                }
                if (stack) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        ring()
                        facts()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ring()
                        Box(modifier = Modifier.weight(1f)) { facts() }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CurrentPhaseSummary(today.phase)
                today.bleeding?.let { bleeding ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = stringResource(R.string.cycle_today_flow, bleedingLabel(bleeding)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentPhaseSummary(phase: CurrentCyclePhase) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = stringResource(R.string.cycle_phase_current), style = MaterialTheme.typography.titleMedium)
        when (phase) {
            is CurrentCyclePhase.Available -> {
                val recorded = phase.certainty == PhaseCertainty.RECORDED
                Text(
                    text = stringResource(cyclePhaseLabelRes(phase.phase)),
                    style = MaterialTheme.typography.titleLarge,
                    color = cyclePhaseColor(phase.phase),
                )
                CycleStatusPill(
                    text = stringResource(if (recorded) R.string.cycle_recorded_label else R.string.cycle_estimated_label),
                    color = if (recorded) CycleColor else MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(
                        when {
                            recorded -> R.string.cycle_phase_support_recorded
                            phase.phase == CyclePhase.OVULATORY -> R.string.cycle_phase_support_ovulatory
                            else -> R.string.cycle_phase_support_estimated
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is CurrentCyclePhase.Indeterminate -> {
                Text(text = stringResource(R.string.cycle_phase_indeterminate), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(phaseIndeterminateReasonRes(phase.reason)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun bleedingLabel(bleeding: DayBleedingChoice): String = when (bleeding) {
    DayBleedingChoice.None -> stringResource(R.string.cycle_bleeding_none)
    DayBleedingChoice.Spotting -> stringResource(R.string.cycle_entry_section_spotting)
    is DayBleedingChoice.Flow -> stringResource(flowLabelRes(bleeding.level))
}

/** What today holds, as chips. Tapping opens the day log. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CycleTodayObservationsCard(
    today: CycleTodayDisplay,
    onOpenDayLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val journal = today.journal
    val hasSomething = today.bleeding != null || journal?.hasObservations == true
    OpenVitalsCard(modifier = modifier.fillMaxWidth(), onClick = onOpenDayLog) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.cycle_today_observations), style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(if (hasSomething) R.string.cycle_edit_today else R.string.cycle_log_today),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!hasSomething) {
                Text(
                    text = stringResource(R.string.cycle_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    today.bleeding?.let { CycleStatusPill(stringResource(R.string.cycle_today_flow, bleedingLabel(it)), CycleColor) }
                    journal?.painLevel?.let { CycleStatusPill(stringResource(R.string.cycle_today_pain, it), MaterialTheme.colorScheme.error) }
                    journal?.moodLevel?.let { CycleStatusPill(stringResource(R.string.cycle_today_mood, it), MaterialTheme.colorScheme.tertiary) }
                    journal?.energyLevel?.let { CycleStatusPill(stringResource(R.string.cycle_today_energy, it), MaterialTheme.colorScheme.primary) }
                    journal?.symptoms?.takeIf { it.isNotEmpty() }?.let {
                        CycleStatusPill(pluralStringResource(R.plurals.cycle_today_symptoms, it.size, it.size), MaterialTheme.colorScheme.secondary)
                    }
                }
                journal?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(
                        text = stringResource(R.string.cycle_today_note, notes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

/** The next period as a range, with the wait drawn to scale. */
@Composable
internal fun CycleEstimateCard(
    today: CycleTodayDisplay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onAddPastPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.cycle_prediction_next_period), style = MaterialTheme.typography.titleMedium)
                CycleStatusPill(stringResource(R.string.cycle_estimated_label), MaterialTheme.colorScheme.tertiary)
            }
            when (val result = today.estimate) {
                CycleEstimateResult.NeedsMoreHistory -> {
                    Text(text = stringResource(R.string.cycle_estimate_needs_history_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.cycle_estimate_needs_history_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onAddPastPeriod) { Text(stringResource(R.string.cycle_add_past_period)) }
                }
                // History exists; the intervals are outside what the calculator models.
                CycleEstimateResult.IntervalsOutOfRange -> {
                    Text(text = stringResource(R.string.cycle_estimate_out_of_range_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.cycle_estimate_out_of_range_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is CycleEstimateResult.Available -> EstimateBody(result.estimate, today, dateTimeFormatterProvider.mediumDate())
            }
        }
    }
}

@Composable
private fun EstimateBody(estimate: CycleEstimate, today: CycleTodayDisplay, formatter: DateTimeFormatter) {
    val daysUntilCentral = ChronoUnit.DAYS.between(today.today, estimate.centralDate).toInt()
    val daysUntilEarliest = ChronoUnit.DAYS.between(today.today, estimate.earliestDate).toInt()
    val daysPastWindow = ChronoUnit.DAYS.between(estimate.latestDate, today.today).toInt()
    val windowDays = ChronoUnit.DAYS.between(estimate.earliestDate, estimate.latestDate).toInt() + 1
    Text(
        text = stringResource(R.string.cycle_estimate_range, formatter.format(estimate.earliestDate), formatter.format(estimate.latestDate)),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = when {
            daysUntilCentral > 0 -> pluralStringResource(R.plurals.cycle_estimate_days_remaining, daysUntilCentral, daysUntilCentral)
            daysPastWindow > 0 -> pluralStringResource(R.plurals.cycle_estimate_past_window, daysPastWindow, daysPastWindow)
            else -> stringResource(R.string.cycle_estimate_in_progress)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (daysUntilEarliest > 0) {
        EstimateWindowTrack(leadDays = daysUntilEarliest, windowDays = windowDays, latestLabel = formatter.format(estimate.latestDate))
    }
    Text(
        text = pluralStringResource(R.plurals.cycle_estimate_basis, estimate.cycleCount, estimate.cycleCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.cycle_estimate_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The wait ahead, to scale, with the window as a band. Its width is the uncertainty. */
@Composable
private fun EstimateWindowTrack(leadDays: Int, windowDays: Int, latestLabel: String) {
    val scheme = MaterialTheme.colorScheme
    val description = stringResource(R.string.cycle_estimate_window_a11y, windowDays)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(MaterialTheme.shapes.extraSmall)
                .semantics { contentDescription = description },
        ) {
            Box(
                modifier = Modifier
                    .weight(leadDays.toFloat())
                    .fillMaxHeight()
                    .background(scheme.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .weight(windowDays.toFloat())
                    .fillMaxHeight()
                    .background(scheme.tertiary),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.period_today),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Text(text = latestLabel, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** A sourced tip for a known phase, or a sourced fact when there is none. */
@Composable
internal fun CycleDailyCard(today: CycleTodayDisplay, modifier: Modifier = Modifier) {
    val phase = today.phase as? CurrentCyclePhase.Available
    val tip = today.tip
    val fact = today.fact
    val text = when {
        phase != null && tip != null -> phaseTipText(tip.id)
        fact != null -> cycleFactText(fact.id)
        else -> null
    } ?: return
    val source = tip?.source?.takeIf { phase != null } ?: fact?.source ?: return
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(if (phase != null) R.string.cycle_tip_title else R.string.cycle_fact_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (phase != null) {
                Text(
                    text = stringResource(
                        if (phase.certainty == PhaseCertainty.RECORDED) R.string.cycle_tip_context_recorded else R.string.cycle_tip_context_estimated,
                        stringResource(cyclePhaseLabelRes(phase.phase)),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = cyclePhaseColor(phase.phase),
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.cycle_source_label, source.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReferenceLinkButton(title = stringResource(R.string.cycle_view_source), url = source.url)
        }
    }
}

/** Average, count and range of the intervals the estimate uses. An average alone hides the spread. */
@Composable
internal fun CycleStatsCard(today: CycleTodayDisplay, modifier: Modifier = Modifier) {
    val lengths = today.recentIntervalLengths
    if (lengths.isEmpty()) return
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(R.string.cycle_stats_title), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.cycle_stats_avg_length, lengths.average().roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.cycle_stats_total, today.totalCycles),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (lengths.size > 1) {
                Text(
                    text = stringResource(R.string.cycle_stats_range, lengths.min(), lengths.max()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shown until a context or an age band is declared. Optional, and says so. */
@Composable
internal fun CycleSetupCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.cycle_setup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.cycle_setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsOutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_setup_action))
            }
        }
    }
}

/** The one-tap "my period started" action. */
@Composable
internal fun CycleStartPeriodButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsOutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Outlined.WaterDrop, contentDescription = null, tint = CycleColor)
        Text(text = stringResource(R.string.cycle_start_period), modifier = Modifier.padding(start = Spacing.sm))
    }
}
