package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleItem
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.DayBleedingChoice
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** The callbacks of the cycle screen's sections. Defaults keep tests and previews short. */
internal data class CycleContentActions(
    val onOpenDayLog: (LocalDate) -> Unit = {},
    val onStartPeriod: () -> Unit = {},
    val onAddPastPeriod: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onManageExclusion: (LongitudinalCycleItem) -> Unit = {},
    /** A swipe only asks; the screen confirms and deletes. */
    val onRequestDelete: (CycleObservation) -> Unit = {},
)

internal fun LazyListScope.cyclePeriodContent(
    state: CycleUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    observations: List<CycleObservation>,
    actions: CycleContentActions = CycleContentActions(),
) {
    val display = state.display
    val sectionModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs)

    cycleTodaySections(display.today, dateTimeFormatterProvider, actions, sectionModifier)

    if (display.hasData) {
        cycleDataConfidence(display = display, period = period)
        cycleStatistics(display = display, unitFormatter = unitFormatter)
        item(key = "cycle-calendar-header") { SectionHeader(stringResource(R.string.section_cycle_calendar)) }
        item(key = "cycle-calendar") {
            CycleCalendarCard(
                days = display.calendarDays,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = sectionModifier,
                onSelectDay = actions.onOpenDayLog,
            )
        }
    } else if (!state.isLoading) {
        item(key = "cycle-empty") {
            MetricCardPlaceholder(
                title = stringResource(R.string.metric_cycle_tracking),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                message = stringResource(R.string.message_no_cycle_period),
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
            )
        }
    }

    if (display.today.hasCycleHistory) {
        item(key = "cycle-history-header") { SectionHeader(stringResource(R.string.section_cycle_history)) }
        item(key = "cycle-history") {
            CycleHistoryCard(
                stats = display.history,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onManageExclusion = actions.onManageExclusion,
                modifier = sectionModifier,
            )
        }
        item(key = "cycle-thermal-header") { SectionHeader(stringResource(R.string.section_cycle_thermal)) }
        item(key = "cycle-thermal") {
            CycleThermalCard(thermal = display.thermal, unitFormatter = unitFormatter, modifier = sectionModifier)
        }
        item(key = "cycle-patterns-header") { SectionHeader(stringResource(R.string.section_cycle_patterns)) }
        item(key = "cycle-patterns") {
            CyclePatternsCard(patterns = display.patterns, modifier = sectionModifier)
        }
    }

    if (observations.isNotEmpty()) {
        item(key = "cycle-entries") {
            PaginatedEntryList(
                title = stringResource(R.string.section_entries),
                entries = observations,
            ) { observation, rowModifier ->
                val dayLogDate = observation.dayLogDate
                CycleObservationRow(
                    observation = observation,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = rowModifier,
                    // Every own observation is edited through its day's log.
                    onEdit = dayLogDate?.let { { actions.onOpenDayLog(it) } },
                    onDelete = dayLogDate?.let { { actions.onRequestDelete(observation) } },
                    asksBeforeDeleting = true,
                )
            }
        }
    }
}

/** Today first: the recorded day, the phase, what was logged, the estimate, and a sourced card. */
private fun LazyListScope.cycleTodaySections(
    today: CycleTodayDisplay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    actions: CycleContentActions,
    sectionModifier: Modifier,
) {
    item(key = "cycle-hero") {
        CycleHeroCard(today = today, dateTimeFormatterProvider = dateTimeFormatterProvider, modifier = sectionModifier)
    }
    item(key = "cycle-today-observations") {
        CycleTodayObservationsCard(today = today, onOpenDayLog = { actions.onOpenDayLog(today.today) }, modifier = sectionModifier)
    }
    if (today.bleeding !is DayBleedingChoice.Flow) {
        item(key = "cycle-start-period") {
            CycleStartPeriodButton(onClick = actions.onStartPeriod, modifier = sectionModifier)
        }
    }
    item(key = "cycle-daily-card") {
        CycleDailyCard(today = today, modifier = sectionModifier)
    }
    item(key = "cycle-estimate") {
        CycleEstimateCard(
            today = today,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onAddPastPeriod = actions.onAddPastPeriod,
            modifier = sectionModifier,
        )
    }
    if (today.recentIntervalLengths.isNotEmpty()) {
        item(key = "cycle-stats") { CycleStatsCard(today = today, modifier = sectionModifier) }
    }
    if (!today.hasProfile) {
        item(key = "cycle-setup") { CycleSetupCard(onOpenSettings = actions.onOpenSettings, modifier = sectionModifier) }
    }
}

private fun LazyListScope.cycleDataConfidence(
    display: CycleDisplayState,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    item(key = "cycle-confidence") {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = display.trackedDates,
                sampleCount = display.sampleCount,
                sources = display.sources,
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = CycleColor,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
        )
    }
}

/** The browsed period's counts. The cycle day and the mean length live in the today cards. */
private fun LazyListScope.cycleStatistics(
    display: CycleDisplayState,
    unitFormatter: UnitFormatter,
) {
    item(key = "cycle-statistics-header") { SectionHeader(stringResource(R.string.section_statistics)) }
    item(key = "cycle-statistics") {
        val summary = display.summary
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_period_days),
                    value = unitFormatter.count(summary.periodDays),
                    unit = stringResource(R.string.unit_days),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.metric_ovulation_tests),
                    value = unitFormatter.count(summary.ovulationTestCount),
                    unit = stringResource(R.string.unit_tests),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_bbt_readings),
                    value = unitFormatter.count(summary.bbtReadingCount),
                    unit = "",
                    icon = Icons.Outlined.DeviceThermostat,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_entries),
                    value = unitFormatter.count(summary.totalEntryCount),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = CycleColor,
                ),
            ),
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
        )
    }
}
