package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.DailyGoalValue
import tech.mmarca.openvitals.core.insights.dailyGoalProgress
import tech.mmarca.openvitals.core.insights.macroSplitInterpretation
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.NutritionColor
import kotlin.math.roundToInt

enum class NutritionMetric {
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
}

private val proteinMetricColor = Color(0xFF7E57C2)
private val carbsMetricColor = Color(0xFF26A69A)
private val fatMetricColor = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesInScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CALORIES_IN,
    )
}

@Composable
fun ProteinScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.PROTEIN,
    )
}

@Composable
fun CarbsScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.CARBS,
    )
}

@Composable
fun FatScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    NutritionMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = NutritionMetric.FAT,
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
    val state by viewModel.uiState.collectAsState()

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
    ) { period ->
        nutritionMetricContent(
            metric = metric,
            state = state,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onDecreaseGoal = viewModel::decreaseDailyGoal,
            onIncreaseGoal = viewModel::increaseDailyGoal,
        )
    }
}

private fun LazyListScope.nutritionMetricContent(
    metric: NutritionMetric,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
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
                subtitle = if (metric == NutritionMetric.CALORIES_IN && state.entries.isNotEmpty()) {
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
                valueFormatter = { metricData.valueDisplayFormatter(it).text },
            )
        }
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

    if (metric == NutritionMetric.CALORIES_IN && state.entries.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_meals)) }
        items(state.entries) { entry ->
            NutritionEntryRow(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

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
): NutritionMetricData =
    when (metric) {
        NutritionMetric.CALORIES_IN -> NutritionMetricData(
            titleRes = R.string.metric_calories_in,
            total = unitFormatter.energy(data.sumOf { it.energyKcal }),
            values = data.map { PeriodChartValue(date = it.date, value = it.energyKcal) },
            color = NutritionColor,
            valueDisplayFormatter = { unitFormatter.energy(it) },
        )
        NutritionMetric.PROTEIN -> NutritionMetricData(
            titleRes = R.string.metric_protein,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.proteinGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.proteinGrams) },
            color = proteinMetricColor,
            valueDisplayFormatter = { DisplayValue(unitFormatter.count(it.roundToInt()), GramsUnit) },
        )
        NutritionMetric.CARBS -> NutritionMetricData(
            titleRes = R.string.metric_carbs,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.carbsGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.carbsGrams) },
            color = carbsMetricColor,
            valueDisplayFormatter = { DisplayValue(unitFormatter.count(it.roundToInt()), GramsUnit) },
        )
        NutritionMetric.FAT -> NutritionMetricData(
            titleRes = R.string.metric_fat,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.fatGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.fatGrams) },
            color = fatMetricColor,
            valueDisplayFormatter = { DisplayValue(unitFormatter.count(it.roundToInt()), GramsUnit) },
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

private const val GramsUnit = "g"

private data class NutritionMetricData(
    val titleRes: Int,
    val total: DisplayValue,
    val values: List<PeriodChartValue>,
    val color: Color,
    val valueDisplayFormatter: (Double) -> DisplayValue,
)

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
