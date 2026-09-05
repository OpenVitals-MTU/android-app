package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup
import tech.mmarca.openvitals.domain.model.valueFor
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.ChartEmptyState
import tech.mmarca.openvitals.ui.components.ChartSkeleton
import tech.mmarca.openvitals.ui.components.ChartSkeletonShape
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.DayAxisLabels
import tech.mmarca.openvitals.ui.components.axisFractionOf
import tech.mmarca.openvitals.ui.components.cumulativeDayPlotPoints
import tech.mmarca.openvitals.ui.components.dayEndFraction
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.NutritionColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

internal fun LazyListScope.nutritionContent(
    sectionContext: MetricDetailSectionContext,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onDeleteEntry: (String) -> Unit = {},
) {
    val display = state.display
    val metricsData = display.overviewNutrients.map { it.toUiModel(unitFormatter) }
    val primaryMetricsData = primaryNutritionOverviewNutrients.mapNotNull { nutrient ->
        metricsData.find { it.nutrient == nutrient }
    }
    val trackedMetricsData = metricsData.filter { it.hasTrackedValues }
    val additionalMetricsData = trackedMetricsData.filterNot { it.nutrient in primaryNutritionOverviewNutrients }
    val selectedDate = chartDaySelection.selectedDate
    val selectedEntries = selectedDate?.let { date ->
        state.entries.nutritionEntriesOnDay(date, ZoneId.systemDefault())
    }.orEmpty()

    if (!display.hasData && state.isLoading) {
        // Still loading: a skeleton where the chart will be. Existing content is kept.
        item {
            ChartSkeleton(
                modifier = metricModifier(),
                shape = ChartSkeletonShape.BARS,
                height = ChartTokens.heightPeriodBar,
            )
        }
        return
    }

    renderOrderedMetricDetailSections(sectionContext) {
        if (!display.hasData && !state.isLoading) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                MetricCardPlaceholder(
                    title = stringResource(R.string.screen_nutrition),
                    icon = Icons.Outlined.Restaurant,
                    accentColor = NutritionColor,
                    message = stringResource(R.string.message_no_nutrition_period),
                    modifier = metricModifier(),
                )
            }
        }

        if (state.dailyMacros.isNotEmpty()) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY, primaryMetricsData.isNotEmpty() || additionalMetricsData.isNotEmpty()) {
                Column {
                    // A single day has no average worth showing.
                    val showAverages = period.start != period.end
                    NutritionOverviewStatisticsContent(primaryMetricsData, showAverages)
                    NutritionAdditionalTotalsContent(additionalMetricsData, showAverages)
                }
            }
            section(MetricDetailSectionId.PERIOD_CHART, trackedMetricsData.isNotEmpty()) {
                NutritionTrendChartsContent(
                    trackedMetricsData = trackedMetricsData,
                    state = state,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null && selectedEntries.isNotEmpty()) {
                selectedDate?.let { date ->
                    NutritionEntriesContent(
                        title = entryListTitle(date, dateTimeFormatterProvider),
                        entries = selectedEntries,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onDeleteEntry = onDeleteEntry,
                    )
                }
            }
            section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
                NutritionOverviewDataConfidenceContent(
                    state = state,
                    display = display,
                    period = period,
                    accentColor = NutritionColor,
                )
            }
            section(MetricDetailSectionId.METRIC_CONTEXT, display.macroSplit != null) {
                MacroSplitContextContent(
                    split = display.macroSplit,
                    unitFormatter = unitFormatter,
                    accentColor = NutritionColor,
                )
            }
        }

        section(MetricDetailSectionId.ENTRIES, state.entries.isNotEmpty()) {
            NutritionEntriesContent(
                title = stringResource(R.string.section_meals),
                entries = state.entries.nutritionEntriesNewestFirst(),
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onDeleteEntry = onDeleteEntry,
            )
        }
    }
}

internal fun LazyListScope.nutritionMetricContent(
    sectionContext: MetricDetailSectionContext,
    metric: NutritionMetric,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onDeleteEntry: (String) -> Unit = {},
) {
    val display = state.display
    val metricData = display.metric.toUiModel(unitFormatter)
    val selectedDate = chartDaySelection.selectedDate
    val selectedEntries = selectedDate?.let { date ->
        state.entries.nutritionEntriesOnDay(date, ZoneId.systemDefault())
    }.orEmpty()

    if (!display.hasData && state.isLoading) {
        // Still loading: a skeleton where the chart will be. Existing content is kept.
        item {
            ChartSkeleton(
                modifier = metricModifier(),
                shape = ChartSkeletonShape.BARS,
                height = ChartTokens.heightPeriodBar,
            )
        }
        return
    }

    renderOrderedMetricDetailSections(sectionContext) {
        if (!display.hasData && !state.isLoading) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                MetricCardPlaceholder(
                    title = stringResource(metricData.titleRes),
                    icon = Icons.Outlined.Restaurant,
                    accentColor = metricData.color,
                    message = stringResource(R.string.message_no_nutrition_period),
                    modifier = metricModifier(),
                )
            }
        }

        if (state.dailyMacros.isNotEmpty()) {
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                NutritionMetricSummaryContent(
                    metricData = metricData,
                    state = state,
                    unitFormatter = unitFormatter,
                )
            }
            section(MetricDetailSectionId.PERIOD_CHART, metricData.values.isNotEmpty()) {
                NutritionMetricTrendContent(
                    metricData = metricData,
                    state = state,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null && selectedEntries.isNotEmpty()) {
                selectedDate?.let { date ->
                    NutritionEntriesContent(
                        title = entryListTitle(date, dateTimeFormatterProvider),
                        entries = selectedEntries,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onDeleteEntry = onDeleteEntry,
                    )
                }
            }
            section(MetricDetailSectionId.DATA_CONFIDENCE, period.start != period.end) {
                NutritionMetricDataConfidenceContent(
                    state = state,
                    metricData = metricData,
                    period = period,
                )
            }
            section(MetricDetailSectionId.DAILY_GOAL, state.display.metric.goalProgress != null) {
                NutritionGoalContent(
                    state = state,
                    metricData = metricData,
                    onDecreaseGoal = onDecreaseGoal,
                    onIncreaseGoal = onIncreaseGoal,
                )
            }
            section(MetricDetailSectionId.STATISTICS) {
                NutritionStatisticsContent(
                    metricData = metricData,
                    display = display.metric,
                    period = period,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    includeGoalProgress = state.display.metric.goalProgress != null,
                )
            }
            section(MetricDetailSectionId.METRIC_CONTEXT, display.macroSplit != null) {
                MacroSplitContextContent(
                    split = display.macroSplit,
                    unitFormatter = unitFormatter,
                    accentColor = metricData.color,
                )
            }
        }

        section(MetricDetailSectionId.ENTRIES, state.entries.isNotEmpty()) {
            NutritionEntriesContent(
                title = stringResource(R.string.section_meals),
                entries = state.entries.nutritionEntriesNewestFirst(),
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onDeleteEntry = onDeleteEntry,
            )
        }
    }
}

@Composable
private fun NutritionAdditionalTotalsContent(
    metricsData: List<NutritionSeriesUiModel>,
    showAverages: Boolean,
) {
    NutritionNutrientGroup.entries
        .filter { it != NutritionNutrientGroup.OVERVIEW }
        .forEach { group ->
            val groupMetrics = metricsData.filter { it.nutrient.group == group }
            if (groupMetrics.isNotEmpty()) {
                SectionHeader(stringResource(group.titleRes()))
                InsightStatGrid(
                    stats = groupMetrics.map { it.nutrientStat(showAverages) },
                    modifier = metricModifier(),
                )
            }
        }
}

@Composable
internal fun NutritionOverviewStatisticsContent(
    metricsData: List<NutritionSeriesUiModel>,
    showAverages: Boolean,
) {
    if (metricsData.isEmpty()) return

    SectionHeader(stringResource(R.string.section_statistics))
    InsightStatGrid(
        stats = metricsData.map { it.nutrientStat(showAverages) },
        modifier = metricModifier(),
    )
}

/**
 * One nutrient's tile: the daily average over a period, with the total
 * underneath (#259). A single day shows its total alone.
 */
@Composable
private fun NutritionSeriesUiModel.nutrientStat(showAverage: Boolean): InsightStat {
    val name = stringResource(titleRes)
    if (!showAverage || !hasAverage) {
        return InsightStat(
            title = name,
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.Restaurant,
            accentColor = color,
        )
    }
    // The title carries the per-day reading; the nutrient's name is what tells tiles apart.
    return InsightStat(
        title = stringResource(R.string.stat_nutrient_per_day, name),
        value = average.value,
        unit = average.unit,
        icon = Icons.Outlined.Restaurant,
        accentColor = color,
        caption = stringResource(R.string.stat_caption_period_total, total.value, total.unit),
    )
}

@Composable
private fun NutritionTrendChartsContent(
    trackedMetricsData: List<NutritionSeriesUiModel>,
    state: NutritionUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_nutrition_trends))
    trackedMetricsData.forEach { metricData ->
        NutritionMetricTrendContent(
            metricData = metricData,
            state = state,
            period = period,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
        )
    }
}

@Composable
private fun NutritionMetricTrendContent(
    metricData: NutritionSeriesUiModel,
    state: NutritionUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY) {
        NutritionIntradayChartCard(
            selectedDate = state.selectedDate,
            metricData = metricData,
            entries = state.entries,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
        )
    } else {
        MetricBarChart(
            title = stringResource(metricData.titleRes),
            values = metricData.values,
            selectedRange = state.selectedRange,
            period = period,
            accentColor = metricData.color,
            summaryValue = metricData.total.text,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { metricData.valueDisplayFormatter(it).text },
        )
    }
}

@Composable
private fun NutritionIntradayChartCard(
    selectedDate: LocalDate,
    metricData: NutritionSeriesUiModel,
    entries: List<NutritionEntry>,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val dayMillis = Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)
    val points = entries.cumulativeNutritionPoints(metricData.nutrient)
    val total = points.lastOrNull()?.second ?: 0.0
    val maxValue = total.coerceAtLeast(1.0)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = metricData.valueDisplayFormatter(total).text,
                style = MaterialTheme.typography.headlineMedium,
                color = metricData.color,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.summary_today, stringResource(metricData.titleRes))
                } else {
                    stringResource(
                        R.string.summary_on_date,
                        stringResource(metricData.titleRes),
                        dateFormatter.format(selectedDate),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (points.isNotEmpty()) {
                // Built outside the zoom content, so the geometry cache holds.
                val chartPoints = remember(points, selectedDate, isToday) {
                    cumulativeDayPlotPoints(
                        fractions = points.map { (time, value) ->
                            axisFractionOf(dayStart, dayEnd, time) to value
                        },
                        endFraction = dayEndFraction(dayStart, dayEnd, Instant.now()),
                    )
                }

                // Plot and hour row share the one viewport.
                ChartZoom(selectedDate, points) { zoom ->
                    Column {
                        MetricLinePlot(
                            points = chartPoints,
                            minValue = 0.0,
                            maxValue = maxValue,
                            accentColor = metricData.color,
                            chartHeight = ChartTokens.heightDay,
                            valueFormatter = { metricData.valueDisplayFormatter(it).text },
                            lineStrokeWidth = 3.dp,
                            viewport = zoom.viewport,
                            multiTouch = zoom.multiTouch,
                            scrubLabel = { point ->
                                val at = dayStart.plusMillis(
                                    (point.xFraction.coerceIn(0f, 1f) * dayMillis).roundToLong(),
                                )
                                metricData.valueDisplayFormatter(point.value).text to
                                    timeFormatter.format(at.atZone(zone))
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        ChartXAxisWithYAxis {
                            DayAxisLabels(viewport = zoom.viewport)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.summary_last_update,
                        timeFormatter.format(points.last().first.atZone(zone)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ChartEmptyState(
                    message = if (isToday) {
                        stringResource(R.string.summary_empty_today, stringResource(R.string.screen_nutrition))
                    } else {
                        stringResource(R.string.summary_empty_day, stringResource(R.string.screen_nutrition))
                    },
                )
            }
        }
    }
}

/** Meals as the screen lists them: newest first. */
internal fun List<NutritionEntry>.nutritionEntriesNewestFirst(): List<NutritionEntry> =
    sortedByDescending { it.time }

/** The meals of one calendar day, newest first. */
internal fun List<NutritionEntry>.nutritionEntriesOnDay(
    date: LocalDate,
    zone: ZoneId,
): List<NutritionEntry> =
    filter { it.time.atZone(zone).toLocalDate() == date }.nutritionEntriesNewestFirst()

internal fun List<NutritionEntry>.cumulativeNutritionPoints(
    nutrient: NutritionNutrient,
): List<Pair<Instant, Double>> {
    var cumulative = 0.0
    return sortedBy { it.time }
        .mapNotNull { entry ->
            val value = entry.valueFor(nutrient)?.takeIf { it > 0.0 } ?: return@mapNotNull null
            cumulative += value
            entry.time to cumulative
        }
}

@Composable
private fun NutritionEntriesContent(
    title: String,
    entries: List<NutritionEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onDeleteEntry: (String) -> Unit = {},
) {
    PaginatedEntryList(
        title = title,
        entries = entries,
    ) { entry, rowModifier ->
        NutritionEntryRow(
            entry = entry,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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
private fun NutritionOverviewDataConfidenceContent(
    state: NutritionUiState,
    display: NutritionDisplayState,
    period: DatePeriod,
    accentColor: Color,
) {
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = display.trackedDates,
            sampleCount = display.sampleCount,
            sources = state.entries.map { it.source },
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = accentColor,
        modifier = metricModifier(),
    )
}

@Composable
private fun NutritionMetricDataConfidenceContent(
    state: NutritionUiState,
    metricData: NutritionSeriesUiModel,
    period: DatePeriod,
) {
    val trackedValues = metricData.values.filter { it.value > 0.0 }
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = trackedValues.map { it.date },
            sampleCount = state.entries.takeIf { it.isNotEmpty() }?.size ?: trackedValues.size,
            sources = state.entries.map { it.source },
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = metricData.color,
        modifier = metricModifier(),
    )
}

@Composable
private fun MacroSplitContextContent(
    split: tech.mmarca.openvitals.domain.insights.MacroSplitInterpretation?,
    unitFormatter: UnitFormatter,
    accentColor: Color,
) {
    if (split == null) return

    SectionHeader(stringResource(R.string.section_metric_context))
    MetricInterpretationCard(
        title = stringResource(R.string.interpretation_macro_title),
        status = if (split.isWithinReference) {
            stringResource(R.string.interpretation_macro_within)
        } else {
            stringResource(R.string.interpretation_macro_outside)
        },
        body = stringResource(
            R.string.interpretation_macro_body,
            unitFormatter.percent(split.proteinPercent, decimals = 0).text,
            unitFormatter.percent(split.carbsPercent, decimals = 0).text,
            unitFormatter.percent(split.fatPercent, decimals = 0).text,
        ),
        source = stringResource(R.string.interpretation_macro_source),
        icon = Icons.Outlined.Restaurant,
        accentColor = accentColor,
        severity = split.severity,
        modifier = metricModifier(),
    )
}

@Composable
private fun NutritionMetricSummaryContent(
    metricData: NutritionSeriesUiModel,
    state: NutritionUiState,
    unitFormatter: UnitFormatter,
) {
    MetricCard(
        title = stringResource(metricData.titleRes),
        value = metricData.total.value,
        unit = metricData.total.unit,
        icon = Icons.Outlined.Restaurant,
        accentColor = metricData.color,
        subtitle = if (state.entries.isNotEmpty()) {
            stringResource(R.string.summary_entries, unitFormatter.count(state.entries.size))
        } else {
            stringResource(R.string.summary_across_selected_period)
        },
        modifier = metricModifier(),
    )
}

@Composable
private fun NutritionGoalContent(
    state: NutritionUiState,
    metricData: NutritionSeriesUiModel,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val progress = state.display.metric.goalProgress ?: return
    DailyGoalCard(
        goal = metricData.valueDisplayFormatter(state.dailyGoal),
        progress = progress,
        icon = Icons.Outlined.Restaurant,
        accentColor = metricData.color,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
        modifier = metricModifier(),
    )
}

@Composable
private fun NutritionStatisticsContent(
    metricData: NutritionSeriesUiModel,
    display: NutritionMetricDisplay,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeGoalProgress: Boolean,
) {
    SectionHeader(stringResource(R.string.section_statistics))
    if (includeGoalProgress) {
        val progress = display.goalProgress
        if (progress != null) {
            DailyGoalStatistics(
                progress = progress,
                averageGap = metricData.valueDisplayFormatter(progress.averageGapToGoal),
                unitFormatter = unitFormatter,
                icon = Icons.Outlined.Restaurant,
                accentColor = metricData.color,
                modifier = metricModifier(),
            )
        }
    }
    NutritionStatisticsGrid(
        metricData = metricData,
        display = display,
        period = period,
        selectedRange = selectedRange,
        unitFormatter = unitFormatter,
    )
}

@Composable
private fun NutritionStatisticsGrid(
    metricData: NutritionSeriesUiModel,
    display: NutritionMetricDisplay,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val average = metricData.valueDisplayFormatter(display.averageValue)
    val best = metricData.valueDisplayFormatter(display.bestDayValue)
    // On one day the total, average and best day coincide; only the total earns a tile.
    val isDay = period.start == period.end

    InsightStatGrid(
        stats = listOfNotNull(
            InsightStat(
                title = stringResource(R.string.stat_total),
                value = metricData.total.value,
                unit = metricData.total.unit,
                icon = Icons.Outlined.Restaurant,
                accentColor = metricData.color,
            ),
            if (isDay) {
                null
            } else {
                InsightStat(
                    title = stringResource(R.string.stat_daily_average),
                    value = average.value,
                    unit = average.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = metricData.color,
                )
            },
            InsightStat(
                title = stringResource(R.string.stat_best_day),
                value = best.value,
                unit = best.unit,
                icon = Icons.Outlined.CalendarMonth,
                accentColor = metricData.color,
            ),
            InsightStat(
                title = stringResource(R.string.metric_logged_days),
                value = unitFormatter.count(display.loggedDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = metricData.color,
            ),
            previousPeriodInsightStat(
                comparison = display.periodComparison,
                selectedRange = selectedRange,
                unitFormatter = unitFormatter,
                valueFormatter = { metricData.valueDisplayFormatter(it) },
                accentColor = metricData.color,
            ),
        ) + personalBaselineInsightStats(
            insight = display.baselineInsight,
            unitFormatter = unitFormatter,
            valueFormatter = { metricData.valueDisplayFormatter(it) },
            accentColor = metricData.color,
        ),
        modifier = metricModifier(),
    )
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
