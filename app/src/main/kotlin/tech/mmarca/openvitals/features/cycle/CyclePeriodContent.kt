package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleItem
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** The callbacks of the cycle screen's sections. Defaults keep tests and previews short. */
internal data class CycleContentActions(
    val onOpenDayLog: (LocalDate) -> Unit = {},
    /** What to log today: the chooser. */
    val onChooseLog: () -> Unit = {},
    /** Marks or unmarks today's pill. */
    val onTogglePillTaken: (Boolean) -> Unit = {},
    val onAddPastPeriod: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onManageExclusion: (LongitudinalCycleItem) -> Unit = {},
    /** A swipe only asks; the screen confirms and deletes. */
    val onRequestDelete: (CycleObservation) -> Unit = {},
)

/**
 * The cycle screen's sections, in the order the user arranged. The default
 * puts today first: the cycle day, the calendar, what was logged today, the
 * pill, then the entries, the estimate, and the rest.
 */
internal fun LazyListScope.cyclePeriodContent(
    state: CycleUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    observations: List<CycleObservation>,
    sectionContext: MetricDetailSectionContext,
    actions: CycleContentActions = CycleContentActions(),
) {
    val display = state.display
    val today = display.today
    val sectionModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.xs)

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.CYCLE_TODAY) {
            CycleHeroCard(today = today, dateTimeFormatterProvider = dateTimeFormatterProvider, modifier = sectionModifier)
        }
        section(MetricDetailSectionId.CYCLE_CALENDAR, display.hasData || !state.isLoading) {
            if (display.hasData) {
                SectionHeader(stringResource(R.string.section_cycle_calendar))
                CycleCalendarCard(
                    days = display.calendarDays,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = sectionModifier,
                    onSelectDay = actions.onOpenDayLog,
                )
            } else {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_cycle_tracking),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                    message = stringResource(R.string.message_no_cycle_period),
                    modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.sm),
                )
            }
        }
        // Only a day with entries; an empty day is logged from the Log action.
        section(MetricDetailSectionId.CYCLE_OBSERVATIONS, today.hasLoggedSomething) {
            CycleTodayObservationsCard(
                today = today,
                onOpenDayLog = { actions.onOpenDayLog(today.today) },
                modifier = sectionModifier,
            )
        }
        today.pill?.let { pill ->
            section(MetricDetailSectionId.CYCLE_PILL) {
                CyclePillCard(
                    pill = pill,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onToggleTaken = actions.onTogglePillTaken,
                    modifier = sectionModifier,
                )
            }
        }
        section(MetricDetailSectionId.CYCLE_ENTRIES, observations.isNotEmpty()) {
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
        section(MetricDetailSectionId.CYCLE_ESTIMATE) {
            CycleEstimateCard(
                today = today,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onAddPastPeriod = actions.onAddPastPeriod,
                modifier = sectionModifier,
            )
        }
        section(MetricDetailSectionId.CYCLE_TIP, today.tip != null || today.fact != null) {
            CycleDailyCard(today = today, modifier = sectionModifier)
        }
        section(MetricDetailSectionId.CYCLE_STATISTICS, display.hasData || today.recentIntervalLengths.isNotEmpty()) {
            if (today.recentIntervalLengths.isNotEmpty()) {
                CycleStatsCard(today = today, modifier = sectionModifier)
            }
            if (display.hasData) {
                CycleDataConfidenceCard(display = display, period = period)
                CycleStatisticsGrid(display = display, unitFormatter = unitFormatter)
            }
        }
        section(MetricDetailSectionId.CYCLE_SETUP, !today.hasProfile) {
            CycleSetupCard(onOpenSettings = actions.onOpenSettings, modifier = sectionModifier)
        }
        section(MetricDetailSectionId.CYCLE_HISTORY, today.hasCycleHistory) {
            SectionHeader(stringResource(R.string.section_cycle_history))
            CycleHistoryCard(
                stats = display.history,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onManageExclusion = actions.onManageExclusion,
                modifier = sectionModifier,
            )
        }
        section(MetricDetailSectionId.CYCLE_THERMAL, today.hasCycleHistory) {
            SectionHeader(stringResource(R.string.section_cycle_thermal))
            CycleThermalCard(thermal = display.thermal, unitFormatter = unitFormatter, modifier = sectionModifier)
        }
        section(MetricDetailSectionId.CYCLE_PATTERNS, today.hasCycleHistory) {
            SectionHeader(stringResource(R.string.section_cycle_patterns))
            CyclePatternsCard(patterns = display.patterns, modifier = sectionModifier)
        }
    }
}

@Composable
private fun CycleDataConfidenceCard(display: CycleDisplayState, period: DatePeriod) {
    if (period.start == period.end) return
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

/** The browsed period's counts. The cycle day and the mean length live in the today cards. */
@Composable
private fun CycleStatisticsGrid(display: CycleDisplayState, unitFormatter: UnitFormatter) {
    SectionHeader(stringResource(R.string.section_statistics))
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
