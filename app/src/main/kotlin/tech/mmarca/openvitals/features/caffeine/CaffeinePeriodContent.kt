package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.CaffeineEntryInsight
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.ChartSkeleton
import tech.mmarca.openvitals.ui.components.ChartSkeletonShape
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The caffeine sections, in the user's section order. A day shows its curve
 * and its night; a longer period shows daily totals and every night in it.
 */
internal fun LazyListScope.caffeinePeriodContent(
    sectionContext: MetricDetailSectionContext,
    state: CaffeineUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onCompleteSetup: (CaffeinePreferences) -> Unit,
    onSkipSetup: () -> Unit,
    onSelectEntry: (String) -> Unit,
    onOpenDrink: (String, LocalDate) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    val display = state.display
    val isDay = state.selectedRange == TimeRange.DAY
    val drinks = display.entryInsights
    val hasDrinks = drinks.isNotEmpty()

    if (state.showSetup) {
        item {
            CaffeineSetupCard(
                preferences = state.preferences,
                bodyProfile = state.bodyProfile,
                onSave = onCompleteSetup,
                onSkip = onSkipSetup,
                modifier = CardModifier,
            )
        }
    }

    // Every load draws a curve, so no curve means the first load is still running.
    if (state.isLoading && display.curvePoints.isEmpty()) {
        item {
            ChartSkeleton(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                shape = ChartSkeletonShape.BARS,
                height = ChartTokens.heightPeriodBar,
            )
        }
        return
    }

    // A day always has a curve and a night, even with nothing drunk that day.
    if (!isDay && !hasDrinks) {
        renderOrderedMetricDetailSections(sectionContext) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_caffeine),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = MaterialTheme.colorScheme.primary,
                    message = stringResource(R.string.caffeine_empty),
                    modifier = CardModifier,
                )
            }
        }
        caffeineScienceContent()
        return
    }

    val zone = ZoneId.systemDefault()
    val chartSelectedDate = chartDaySelection.selectedDate
    val chartSelectedDrinks = chartSelectedDate?.let { date ->
        drinks.filter { it.entry.startTime.atZone(zone).toLocalDate() == date }
    }.orEmpty()

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
            when {
                isDay && state.selectedDate == LocalDate.now() -> CaffeineHomeOverviewCard(
                    insights = display,
                    unitFormatter = unitFormatter,
                    modifier = CardModifier,
                )
                isDay -> CaffeinePastDayOverviewCard(
                    insights = display,
                    unitFormatter = unitFormatter,
                    modifier = CardModifier,
                )
                else -> CaffeineAnalyticsSummaryCard(
                    insights = display,
                    periodTitle = localizedPeriodTitle(state.selectedRange, period),
                    unitFormatter = unitFormatter,
                    modifier = CardModifier,
                )
            }
        }
        section(MetricDetailSectionId.INTRADAY_CHART, isDay) {
            CaffeineCurveCard(
                insights = display,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onSelectEntry = onSelectEntry,
                modifier = CardModifier,
            )
        }
        section(MetricDetailSectionId.PERIOD_CHART, !isDay) {
            MetricBarChart(
                title = stringResource(R.string.metric_caffeine),
                data = display.dailyStats,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = MaterialTheme.colorScheme.primary,
                summaryValue = formatMg(display.periodTotalMg, unitFormatter),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                date = { it.date },
                value = { it.totalMg },
                modifier = CardModifier,
                selectedDate = chartSelectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { formatMg(it, unitFormatter) },
            )
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, chartSelectedDrinks.isNotEmpty()) {
            CaffeineEntriesContent(
                insights = chartSelectedDrinks,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenDrink = onOpenDrink,
                onDeleteEntry = onDeleteEntry,
                titleDate = chartSelectedDate,
            )
        }
        section(MetricDetailSectionId.DAILY_GOAL) {
            SectionHeader(stringResource(R.string.caffeine_section_sleep))
            if (isDay) {
                CaffeineSleepImpactCard(
                    insights = display,
                    unitFormatter = unitFormatter,
                    modifier = CardModifier,
                )
            } else {
                CaffeineDailyImpactCard(
                    stats = display.dailyStats,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    rangeLabel = localizedPeriodTitle(state.selectedRange, period),
                    modifier = CardModifier,
                )
            }
        }
        section(MetricDetailSectionId.STATISTICS, hasDrinks) {
            SectionHeader(stringResource(R.string.section_statistics))
            CaffeineStatistics(
                insights = display,
                comparison = state.periodComparison,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
                modifier = CardModifier,
            )
        }
        section(MetricDetailSectionId.METRIC_CONTEXT, hasDrinks) {
            CaffeineDistributionCard(
                title = stringResource(R.string.caffeine_sources),
                slices = display.sourceTotals,
                unitFormatter = unitFormatter,
                modifier = CardModifier,
            )
            CaffeineDistributionCard(
                title = stringResource(R.string.caffeine_items),
                slices = display.itemTotals,
                unitFormatter = unitFormatter,
                modifier = CardModifier,
            )
            CaffeineDistributionCard(
                title = stringResource(R.string.caffeine_inferred_categories),
                slices = display.categoryTotals,
                unitFormatter = unitFormatter,
                modifier = CardModifier,
            )
            CaffeineTimeBucketsCard(
                buckets = display.timeBuckets,
                unitFormatter = unitFormatter,
                modifier = CardModifier,
            )
        }
        section(MetricDetailSectionId.DATA_CONFIDENCE, !isDay) {
            DataConfidenceCard(
                confidence = dataConfidence(
                    period = period,
                    trackedDates = display.dailyStats.filter { it.totalMg > 0.0 }.map { it.date },
                    sampleCount = drinks.size,
                    valueKind = DataValueKind.AGGREGATED,
                ),
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = CardModifier,
            )
        }
        section(MetricDetailSectionId.ENTRIES) {
            if (hasDrinks) {
                CaffeineEntriesContent(
                    insights = drinks,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenDrink = onOpenDrink,
                    onDeleteEntry = onDeleteEntry,
                )
            } else {
                SectionHeader(stringResource(R.string.section_entries))
                EmptyCaffeineCard(modifier = CardModifier)
            }
        }
    }
    caffeineScienceContent()
}

/** How the estimate works, and its sources. Always last: it explains every section above. */
private fun LazyListScope.caffeineScienceContent() {
    item { SectionHeader(stringResource(R.string.caffeine_section_science)) }
    item { CaffeineScienceCard(modifier = CardModifier) }
    item { CaffeineReferencesCard(modifier = CardModifier) }
}

/** The measured card rhythm: 16 at the sides, 8 between cards. */
private val CardModifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)

@Composable
private fun CaffeineEntriesContent(
    insights: List<CaffeineEntryInsight>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenDrink: (String, LocalDate) -> Unit,
    onDeleteEntry: (String) -> Unit,
    titleDate: LocalDate? = null,
) {
    val zone = ZoneId.systemDefault()
    PaginatedEntryList(
        title = entryListTitle(titleDate, dateTimeFormatterProvider),
        entries = insights,
    ) { insight, rowModifier ->
        val entry = insight.entry
        CaffeineEntryRow(
            insight = insight,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            // The drink screen loads the drink's own day.
            onClick = { onOpenDrink(entry.id, entry.startTime.atZone(zone).toLocalDate()) },
            // Only a record this app wrote, with an id, can be deleted here.
            onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                { onDeleteEntry(entry.id) }
            } else {
                null
            },
            modifier = rowModifier,
        )
    }
}

@Composable
private fun CaffeineStatistics(
    insights: CaffeineInsights,
    comparison: PeriodComparison,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val total = InsightStat(
        title = stringResource(R.string.stat_total_intake),
        value = unitFormatter.count(insights.periodTotalMg.roundToInt()),
        unit = MilligramsUnit,
        icon = Icons.Outlined.LocalDrink,
        accentColor = accent,
    )
    val comparisonStat = previousPeriodInsightStat(
        comparison = comparison,
        selectedRange = selectedRange,
        unitFormatter = unitFormatter,
        valueFormatter = { DisplayValue(unitFormatter.count(it.roundToInt()), MilligramsUnit) },
        accentColor = accent,
    )
    val stats = if (selectedRange == TimeRange.DAY) {
        listOf(
            total,
            InsightStat(
                title = stringResource(R.string.caffeine_peak),
                value = unitFormatter.count(caffeineCurvePeakMg(insights.curvePoints).roundToInt()),
                unit = MilligramsUnit,
                icon = Icons.Outlined.QueryStats,
                accentColor = accent,
            ),
            comparisonStat,
        )
    } else {
        listOf(
            total,
            InsightStat(
                title = stringResource(R.string.stat_daily_average),
                value = unitFormatter.count(insights.periodAverageMg.roundToInt()),
                unit = MilligramsUnit,
                icon = Icons.Outlined.Star,
                accentColor = accent,
            ),
            InsightStat(
                title = stringResource(R.string.stat_highest),
                value = unitFormatter.count((insights.peakDay?.totalMg ?: 0.0).roundToInt()),
                unit = MilligramsUnit,
                icon = Icons.Outlined.CalendarMonth,
                accentColor = accent,
            ),
            InsightStat(
                title = stringResource(R.string.metric_logged_days),
                value = unitFormatter.count(insights.loggedDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = accent,
            ),
            InsightStat(
                title = stringResource(R.string.caffeine_safe_nights),
                value = "${insights.safeNights}/${insights.totalNights}",
                unit = "",
                icon = Icons.Outlined.NightsStay,
                accentColor = accent,
            ),
            InsightStat(
                title = stringResource(R.string.caffeine_safe_streak),
                value = unitFormatter.count(insights.safeSleepStreak),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = accent,
            ),
            comparisonStat,
        )
    }
    InsightStatGrid(stats = stats, modifier = modifier)
}

/** The day's highest level. A day without caffeine peaks at zero. */
internal fun caffeineCurvePeakMg(points: List<CaffeinePoint>): Double =
    points.maxOfOrNull { it.valueMg } ?: 0.0

private const val MilligramsUnit = "mg"
