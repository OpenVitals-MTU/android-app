package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.macroSplitInterpretation
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionNutrientGroup
import tech.mmarca.openvitals.domain.model.NutritionNutrientUnit
import tech.mmarca.openvitals.domain.model.valueFor
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.NutritionColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class NutritionMetric {
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
}

private val nutritionMetrics = listOf(
    NutritionNutrient.ENERGY,
    NutritionNutrient.PROTEIN,
    NutritionNutrient.TOTAL_CARBOHYDRATE,
    NutritionNutrient.TOTAL_FAT,
)

private val proteinMetricColor = Color(0xFF7E57C2)
private val carbsMetricColor = Color(0xFF26A69A)
private val fatMetricColor = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "nutrition",
    )

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        nutritionContent(
            state = state,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            chartDaySelection = chartDaySelection,
        )
    }
}

@Composable
fun CaloriesInScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun ProteinScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun CarbsScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun FatScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NutritionMetricScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: NutritionMetric,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        nutritionMetricContent(
            metric = metric,
            state = state,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            chartDaySelection = chartDaySelection,
            onDecreaseGoal = viewModel::decreaseDailyGoal,
            onIncreaseGoal = viewModel::increaseDailyGoal,
        )
    }
}

private fun LazyListScope.nutritionContent(
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
) {
    val metricsData = NutritionNutrient.entries.map { nutrient ->
        nutritionMetricData(nutrient, state.dailyMacros, unitFormatter)
    }
    val primaryMetricsData = nutritionMetrics.map { nutrient ->
        nutritionMetricData(nutrient, state.dailyMacros, unitFormatter)
    }
    val trackedMetricsData = metricsData.filter { it.hasTrackedValues }
    val additionalMetricsData = trackedMetricsData.filterNot { it.nutrient in nutritionMetrics }

    if (state.dailyMacros.isEmpty() && state.entries.isEmpty() && !state.isLoading) {
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
        nutritionDataConfidence(
            state = state,
            period = period,
            accentColor = NutritionColor,
        )
        macroSplitContext(
            data = state.dailyMacros,
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

private fun LazyListScope.nutritionAdditionalTotals(
    metricsData: List<NutritionMetricData>,
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
    metricsData: List<NutritionMetricData>,
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
    metricData: NutritionMetricData,
    state: NutritionUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    item {
        PeriodHistoryChart(
            title = stringResource(metricData.titleRes),
            values = metricData.values,
            selectedRange = state.selectedRange,
            period = period,
            accentColor = metricData.color.copy(alpha = 0.85f),
            summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${metricData.total.text}",
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

private fun LazyListScope.nutritionMetricContent(
    metric: NutritionMetric,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val metricData = nutritionMetricData(metric, state.dailyMacros, unitFormatter)
    val previousMetricData = nutritionMetricData(metric, state.previousDailyMacros, unitFormatter)
    val baselineMetricData = nutritionMetricData(metric, state.baselineDailyMacros, unitFormatter)
    if (state.dailyMacros.isEmpty() && !state.isLoading) {
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
            PeriodHistoryChart(
                title = stringResource(metricData.titleRes),
                values = metricData.values,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = metricData.color.copy(alpha = 0.85f),
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${metricData.total.text}",
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
        nutritionDataConfidence(
            state = state,
            period = period,
            metricData = metricData,
        )
        nutritionGoal(
            metric = metric,
            state = state,
            period = period,
            metricData = metricData,
            unitFormatter = unitFormatter,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
        )
        nutritionStatistics(
            metricData = metricData,
            previousMetricData = previousMetricData,
            baselineMetricData = baselineMetricData,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
            includeHeader = false,
        )
        macroSplitContext(
            data = state.dailyMacros,
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

private fun LazyListScope.nutritionDataConfidence(
    state: NutritionUiState,
    period: DatePeriod,
    accentColor: Color,
) {
    if (period.start == period.end) return

    val trackedDates = state.dailyMacros
        .filter { it.hasNutritionData() }
        .map { it.date }
    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = trackedDates,
                sampleCount = state.entries.takeIf { it.isNotEmpty() }?.size ?: trackedDates.size,
                sources = state.entries.map { it.source },
                valueKind = DataValueKind.AGGREGATED,
            ),
            accentColor = accentColor,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.nutritionDataConfidence(
    state: NutritionUiState,
    period: DatePeriod,
    metricData: NutritionMetricData,
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

private fun DailyMacros.hasNutritionData(): Boolean =
    nutrientValues.any { (_, value) -> value > 0.0 } ||
        energyKcal > 0.0 ||
        proteinGrams > 0.0 ||
        carbsGrams > 0.0 ||
        fatGrams > 0.0

private fun LazyListScope.macroSplitContext(
    data: List<DailyMacros>,
    unitFormatter: UnitFormatter,
    accentColor: Color,
) {
    val split = macroSplitInterpretation(
        proteinGrams = data.sumOf { it.proteinGrams },
        carbsGrams = data.sumOf { it.carbsGrams },
        fatGrams = data.sumOf { it.fatGrams },
    ) ?: return

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

private fun nutritionMetricData(
    metric: NutritionMetric,
    data: List<DailyMacros>,
    unitFormatter: UnitFormatter,
): NutritionMetricData = nutritionMetricData(metric.nutrient, data, unitFormatter)

private fun nutritionMetricData(
    nutrient: NutritionNutrient,
    data: List<DailyMacros>,
    unitFormatter: UnitFormatter,
): NutritionMetricData {
    val values = data.map { PeriodChartValue(date = it.date, value = it.valueFor(nutrient)) }
    val totalValue = values.sumOf { it.value }
    return NutritionMetricData(
        nutrient = nutrient,
        titleRes = nutrient.titleRes(),
        total = nutrient.displayValue(totalValue, unitFormatter),
        totalValue = totalValue,
        values = values,
        color = nutrient.color(),
        valueDisplayFormatter = { nutrient.displayValue(it, unitFormatter) },
    )
}

private fun LazyListScope.nutritionGoal(
    metric: NutritionMetric,
    state: NutritionUiState,
    period: DatePeriod,
    metricData: NutritionMetricData,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val progress = dailyGoalProgress(
        values = metricData.values.map { DailyGoalValue(date = it.date, value = it.value) },
        period = period,
        target = state.dailyGoal,
        direction = metric.dailyGoalKey.direction,
    )
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
    metricData: NutritionMetricData,
    previousMetricData: NutritionMetricData,
    baselineMetricData: NutritionMetricData,
    period: DatePeriod,
    selectedRange: tech.mmarca.openvitals.core.period.TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        val values = metricData.values.map { it.value }
        val loggedDays = values.count { it > 0.0 }
        val average = loggedDays.takeIf { it > 0 }
            ?.let { metricData.valueDisplayFormatter(values.sum() / it) }
            ?: metricData.valueDisplayFormatter(0.0)
        val best = metricData.valueDisplayFormatter(values.maxOrNull() ?: 0.0)

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
                    value = unitFormatter.count(loggedDays),
                    unit = stringResource(R.string.unit_days),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = metricData.color,
                ),
                previousPeriodInsightStat(
                    comparison = periodComparison(
                        currentValue = metricData.values.sumOf { it.value },
                        previousValue = previousMetricData.values.sumOf { it.value },
                    ),
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { metricData.valueDisplayFormatter(it) },
                    accentColor = metricData.color,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = loggedDays.takeIf { it > 0 }
                        ?.let { values.sum() / it }
                        ?: 0.0,
                    values = baselineMetricData.values.map { BaselineValue(it.date, it.value) },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { metricData.valueDisplayFormatter(it) },
                accentColor = metricData.color,
            ),
            modifier = metricModifier(),
        )
    }
}

private data class NutritionMetricData(
    val nutrient: NutritionNutrient,
    val titleRes: Int,
    val total: DisplayValue,
    val totalValue: Double,
    val values: List<PeriodChartValue>,
    val color: Color,
    val valueDisplayFormatter: (Double) -> DisplayValue,
) {
    val hasTrackedValues: Boolean = values.any { it.value > 0.0 }
}

private val NutritionMetric.nutrient: NutritionNutrient
    get() = when (this) {
        NutritionMetric.CALORIES_IN -> NutritionNutrient.ENERGY
        NutritionMetric.PROTEIN -> NutritionNutrient.PROTEIN
        NutritionMetric.CARBS -> NutritionNutrient.TOTAL_CARBOHYDRATE
        NutritionMetric.FAT -> NutritionNutrient.TOTAL_FAT
    }

private fun NutritionNutrient.displayValue(
    value: Double,
    unitFormatter: UnitFormatter,
): DisplayValue =
    when (unit) {
        NutritionNutrientUnit.ENERGY_KCAL -> unitFormatter.energy(value)
        NutritionNutrientUnit.MASS_GRAMS -> DisplayValue(unitFormatter.count(value.roundToInt()), "g")
        NutritionNutrientUnit.MASS_ADAPTIVE -> adaptiveMassDisplay(value, unitFormatter)
    }

private fun adaptiveMassDisplay(
    grams: Double,
    unitFormatter: UnitFormatter,
): DisplayValue {
    val milligrams = grams * 1_000.0
    val micrograms = grams * 1_000_000.0
    return when {
        grams >= 1.0 -> DisplayValue(unitFormatter.decimal(grams, if (grams < 10.0) 1 else 0), "g")
        milligrams >= 1.0 -> DisplayValue(unitFormatter.decimal(milligrams, if (milligrams < 10.0) 1 else 0), "mg")
        else -> DisplayValue(unitFormatter.decimal(micrograms, if (micrograms < 10.0) 1 else 0), "mcg")
    }
}

private fun NutritionNutrient.color(): Color =
    when (group) {
        NutritionNutrientGroup.OVERVIEW -> when (this) {
            NutritionNutrient.ENERGY -> NutritionColor
            NutritionNutrient.PROTEIN -> proteinMetricColor
            NutritionNutrient.TOTAL_CARBOHYDRATE -> carbsMetricColor
            NutritionNutrient.TOTAL_FAT -> fatMetricColor
            else -> NutritionColor
        }
        NutritionNutrientGroup.CARBOHYDRATES -> carbsMetricColor
        NutritionNutrientGroup.FATS -> fatMetricColor
        NutritionNutrientGroup.VITAMINS -> Color(0xFF5E7CE2)
        NutritionNutrientGroup.MINERALS -> Color(0xFF8D6E63)
        NutritionNutrientGroup.OTHER -> Color(0xFF00897B)
    }

private fun NutritionNutrientGroup.titleRes(): Int =
    when (this) {
        NutritionNutrientGroup.OVERVIEW -> R.string.screen_nutrition
        NutritionNutrientGroup.CARBOHYDRATES -> R.string.section_carbohydrates
        NutritionNutrientGroup.FATS -> R.string.section_fats
        NutritionNutrientGroup.VITAMINS -> R.string.section_vitamins
        NutritionNutrientGroup.MINERALS -> R.string.section_minerals
        NutritionNutrientGroup.OTHER -> R.string.section_other_nutrients
    }

private fun NutritionNutrient.titleRes(): Int =
    when (this) {
        NutritionNutrient.ENERGY -> R.string.metric_calories_in
        NutritionNutrient.PROTEIN -> R.string.metric_protein
        NutritionNutrient.TOTAL_CARBOHYDRATE -> R.string.metric_carbs
        NutritionNutrient.TOTAL_FAT -> R.string.metric_fat
        NutritionNutrient.DIETARY_FIBER -> R.string.metric_dietary_fiber
        NutritionNutrient.SUGAR -> R.string.metric_sugar
        NutritionNutrient.ENERGY_FROM_FAT -> R.string.metric_energy_from_fat
        NutritionNutrient.MONOUNSATURATED_FAT -> R.string.metric_monounsaturated_fat
        NutritionNutrient.POLYUNSATURATED_FAT -> R.string.metric_polyunsaturated_fat
        NutritionNutrient.SATURATED_FAT -> R.string.metric_saturated_fat
        NutritionNutrient.TRANS_FAT -> R.string.metric_trans_fat
        NutritionNutrient.UNSATURATED_FAT -> R.string.metric_unsaturated_fat
        NutritionNutrient.CHOLESTEROL -> R.string.metric_cholesterol
        NutritionNutrient.BIOTIN -> R.string.metric_biotin
        NutritionNutrient.FOLATE -> R.string.metric_folate
        NutritionNutrient.FOLIC_ACID -> R.string.metric_folic_acid
        NutritionNutrient.NIACIN -> R.string.metric_niacin
        NutritionNutrient.PANTOTHENIC_ACID -> R.string.metric_pantothenic_acid
        NutritionNutrient.RIBOFLAVIN -> R.string.metric_riboflavin
        NutritionNutrient.THIAMIN -> R.string.metric_thiamin
        NutritionNutrient.VITAMIN_A -> R.string.metric_vitamin_a
        NutritionNutrient.VITAMIN_B12 -> R.string.metric_vitamin_b12
        NutritionNutrient.VITAMIN_B6 -> R.string.metric_vitamin_b6
        NutritionNutrient.VITAMIN_C -> R.string.metric_vitamin_c
        NutritionNutrient.VITAMIN_D -> R.string.metric_vitamin_d
        NutritionNutrient.VITAMIN_E -> R.string.metric_vitamin_e
        NutritionNutrient.VITAMIN_K -> R.string.metric_vitamin_k
        NutritionNutrient.CALCIUM -> R.string.metric_calcium
        NutritionNutrient.CHLORIDE -> R.string.metric_chloride
        NutritionNutrient.CHROMIUM -> R.string.metric_chromium
        NutritionNutrient.COPPER -> R.string.metric_copper
        NutritionNutrient.IODINE -> R.string.metric_iodine
        NutritionNutrient.IRON -> R.string.metric_iron
        NutritionNutrient.MAGNESIUM -> R.string.metric_magnesium
        NutritionNutrient.MANGANESE -> R.string.metric_manganese
        NutritionNutrient.MOLYBDENUM -> R.string.metric_molybdenum
        NutritionNutrient.PHOSPHORUS -> R.string.metric_phosphorus
        NutritionNutrient.POTASSIUM -> R.string.metric_potassium
        NutritionNutrient.SELENIUM -> R.string.metric_selenium
        NutritionNutrient.SODIUM -> R.string.metric_sodium
        NutritionNutrient.ZINC -> R.string.metric_zinc
        NutritionNutrient.CAFFEINE -> R.string.metric_caffeine
    }

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
