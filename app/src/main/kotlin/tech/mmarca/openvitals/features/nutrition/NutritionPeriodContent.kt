package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup
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
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.NutritionColor
import java.time.LocalDate
import java.time.ZoneId

internal fun LazyListScope.nutritionContent(
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

    if (!display.hasData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.screen_nutrition),
                icon = Icons.Outlined.Restaurant,
                accentColor = NutritionColor,
                message = stringResource(R.string.message_no_nutrition_period),
                modifier = metricModifier(),
            )
        }
        return
    }

    if (state.dailyMacros.isNotEmpty()) {
        nutritionOverviewStatistics(primaryMetricsData)
        nutritionAdditionalTotals(additionalMetricsData)
        if (trackedMetricsData.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_nutrition_trends)) }
            trackedMetricsData.forEach { metricData ->
                nutritionMetricTrend(
                    metricData = metricData,
                    state = state,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            selectedDateNutritionEntries(
                state = state,
                selectedDate = selectedDate,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
        nutritionOverviewDataConfidence(
            state = state,
            display = display,
            period = period,
            accentColor = NutritionColor,
        )
        macroSplitContext(
            split = display.macroSplit,
            unitFormatter = unitFormatter,
            accentColor = NutritionColor,
        )
    }

    if (state.entries.isNotEmpty()) {
        item {
            PaginatedEntryList(
                title = stringResource(R.string.section_meals),
                entries = state.entries.sortedByDescending { it.time },
            ) { entry, rowModifier ->
                NutritionEntryRow(
                    entry = entry,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = rowModifier,
                )
            }
        }
    }
}

internal fun LazyListScope.nutritionMetricContent(
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
    if (!display.hasData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(metricData.titleRes),
                icon = Icons.Outlined.Restaurant,
                accentColor = metricData.color,
                message = stringResource(R.string.message_no_nutrition_period),
                modifier = metricModifier(),
            )
        }
        return
    }

    if (state.dailyMacros.isNotEmpty()) {
        item {
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
        item {
            MetricBarChart(
                title = stringResource(metricData.titleRes),
                values = metricData.values,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = metricData.color,
                summaryValue = metricData.total.text,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
                valueFormatter = { metricData.valueDisplayFormatter(it).text },
            )
        }
        chartDaySelection.selectedDate?.let { selectedDate ->
            item {
                val zone = ZoneId.systemDefault()
                PaginatedEntryList(
                    title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                    entries = state.entries
                        .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                        .sortedByDescending { it.time },
                ) { entry, rowModifier ->
                    NutritionEntryRow(
                        entry = entry,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = rowModifier,
                    )
                }
            }
        }
        nutritionMetricDataConfidence(
            state = state,
            metricData = metricData,
            period = period,
        )
        nutritionGoal(
            state = state,
            metricData = metricData,
            unitFormatter = unitFormatter,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
        )
        nutritionStatistics(
            metricData = metricData,
            display = display.metric,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
            includeHeader = false,
        )
        macroSplitContext(
            split = display.macroSplit,
            unitFormatter = unitFormatter,
            accentColor = metricData.color,
        )
    }

    if (state.entries.isNotEmpty()) {
        item {
            PaginatedEntryList(
                title = stringResource(R.string.section_meals),
                entries = state.entries.sortedByDescending { it.time },
            ) { entry, rowModifier ->
                NutritionEntryRow(
                    entry = entry,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = rowModifier,
                )
            }
        }
    }
}

private fun LazyListScope.nutritionAdditionalTotals(
    metricsData: List<NutritionSeriesUiModel>,
) {
    NutritionNutrientGroup.entries
        .filter { it != NutritionNutrientGroup.OVERVIEW }
        .forEach { group ->
            val groupMetrics = metricsData.filter { it.nutrient.group == group }
            if (groupMetrics.isNotEmpty()) {
                item { SectionHeader(stringResource(group.titleRes())) }
                item {
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
}

private fun LazyListScope.nutritionOverviewStatistics(
    metricsData: List<NutritionSeriesUiModel>,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
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
}

private fun LazyListScope.nutritionMetricTrend(
    metricData: NutritionSeriesUiModel,
    state: NutritionUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    item {
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

private fun LazyListScope.selectedDateNutritionEntries(
    state: NutritionUiState,
    selectedDate: LocalDate,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val zone = ZoneId.systemDefault()
    item {
        PaginatedEntryList(
            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
            entries = state.entries
                .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                .sortedByDescending { it.time },
        ) { entry, rowModifier ->
            NutritionEntryRow(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = rowModifier,
            )
        }
    }
}

private fun LazyListScope.nutritionOverviewDataConfidence(
    state: NutritionUiState,
    display: NutritionDisplayState,
    period: DatePeriod,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
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
}

private fun LazyListScope.nutritionMetricDataConfidence(
    state: NutritionUiState,
    metricData: NutritionSeriesUiModel,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    val trackedValues = metricData.values.filter { it.value > 0.0 }
    item {
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
}

private fun LazyListScope.macroSplitContext(
    split: tech.mmarca.openvitals.domain.insights.MacroSplitInterpretation?,
    unitFormatter: UnitFormatter,
    accentColor: Color,
) {
    if (split == null) return

    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
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
}

private fun LazyListScope.nutritionGoal(
    state: NutritionUiState,
    metricData: NutritionSeriesUiModel,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val progress = state.display.metric.goalProgress ?: return
    item {
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
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
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

private fun LazyListScope.nutritionStatistics(
    metricData: NutritionSeriesUiModel,
    display: NutritionMetricDisplay,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
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
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
