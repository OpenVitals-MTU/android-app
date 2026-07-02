package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
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

internal fun LazyListScope.nutritionContent(
    sectionContext: MetricDetailSectionContext,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
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
        val zone = ZoneId.systemDefault()
        state.entries
            .filter { it.time.atZone(zone).toLocalDate() == date }
            .sortedByDescending { it.time }
    }.orEmpty()

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
                    NutritionOverviewStatisticsContent(primaryMetricsData)
                    NutritionAdditionalTotalsContent(additionalMetricsData)
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
                entries = state.entries.sortedByDescending { it.time },
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
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
) {
    val display = state.display
    val metricData = display.metric.toUiModel(unitFormatter)
    val selectedDate = chartDaySelection.selectedDate
    val selectedEntries = selectedDate?.let { date ->
        val zone = ZoneId.systemDefault()
        state.entries
            .filter { it.time.atZone(zone).toLocalDate() == date }
            .sortedByDescending { it.time }
    }.orEmpty()

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
                entries = state.entries.sortedByDescending { it.time },
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
    }
}

@Composable
private fun NutritionAdditionalTotalsContent(
    metricsData: List<NutritionSeriesUiModel>,
) {
    NutritionNutrientGroup.entries
        .filter { it != NutritionNutrientGroup.OVERVIEW }
        .forEach { group ->
            val groupMetrics = metricsData.filter { it.nutrient.group == group }
            if (groupMetrics.isNotEmpty()) {
                SectionHeader(stringResource(group.titleRes()))
                InsightStatGrid(
                    stats = groupMetrics.map { metricData ->
                        InsightStat(
                            title = stringResource(metricData.titleRes),
                            value = metricData.total.value,
                            unit = metricData.total.unit,
                            icon = Icons.Outlined.Restaurant,
                            accentColor = metricData.color,
                        )
                    },
                    modifier = metricModifier(),
                )
            }
        }
}

@Composable
private fun NutritionOverviewStatisticsContent(
    metricsData: List<NutritionSeriesUiModel>,
) {
    if (metricsData.isEmpty()) return

    SectionHeader(stringResource(R.string.section_statistics))
    InsightStatGrid(
        stats = metricsData.map { metricData ->
            InsightStat(
                title = stringResource(metricData.titleRes),
                value = metricData.total.value,
                unit = metricData.total.unit,
                icon = Icons.Outlined.Restaurant,
                accentColor = metricData.color,
            )
        },
        modifier = metricModifier(),
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
    val isToday = selectedDate == LocalDate.now()
    val chartEnd = if (isToday) Instant.now() else selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val elapsedToday = Duration.between(dayStart, chartEnd).toMillis().coerceAtLeast(1L)
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
                val chartPoints = buildList {
                    add(MetricLinePlotPoint(xFraction = 0f, value = 0.0))
                    points.forEach { point ->
                        val elapsed = Duration.between(dayStart, point.first)
                            .toMillis()
                            .coerceIn(0L, elapsedToday)
                        add(
                            MetricLinePlotPoint(
                                xFraction = elapsed.toFloat() / elapsedToday.toFloat(),
                                value = point.second,
                            )
                        )
                    }
                    add(MetricLinePlotPoint(xFraction = 1f, value = total))
                }

                MetricLinePlot(
                    points = chartPoints,
                    minValue = 0.0,
                    maxValue = maxValue,
                    accentColor = metricData.color,
                    chartHeight = 180.dp,
                    valueFormatter = { metricData.valueDisplayFormatter(it).text },
                    lineStrokeWidth = 3.dp,
                )
                Spacer(Modifier.height(8.dp))
                ChartXAxisWithYAxis {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf(
                            "00:00",
                            "06:00",
                            "12:00",
                            "18:00",
                            if (isToday) stringResource(R.string.summary_now) else "24:00",
                        ).forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                Text(
                    text = if (isToday) {
                        stringResource(R.string.summary_empty_today, stringResource(R.string.screen_nutrition))
                    } else {
                        stringResource(R.string.summary_empty_day, stringResource(R.string.screen_nutrition))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun List<NutritionEntry>.cumulativeNutritionPoints(
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
    entries: List<tech.mmarca.openvitals.domain.model.NutritionEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    PaginatedEntryList(
        title = title,
        entries = entries,
    ) { entry, rowModifier ->
        NutritionEntryRow(
            entry = entry,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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

    InsightStatGrid(
        stats = listOf(
            InsightStat(
                title = stringResource(R.string.stat_total),
                value = metricData.total.value,
                unit = metricData.total.unit,
                icon = Icons.Outlined.Restaurant,
                accentColor = metricData.color,
            ),
            InsightStat(
                title = stringResource(R.string.stat_daily_average),
                value = average.value,
                unit = average.unit,
                icon = Icons.Outlined.Star,
                accentColor = metricData.color,
            ),
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
