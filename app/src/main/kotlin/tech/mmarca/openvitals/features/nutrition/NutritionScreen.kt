package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
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
fun NutritionScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: NutritionMetric = NutritionMetric.CALORIES_IN,
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
        )
    }
}

private fun LazyListScope.nutritionMetricContent(
    metric: NutritionMetric,
    state: NutritionUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val metricData = nutritionMetricData(metric, state.dailyMacros, unitFormatter)
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
                subtitle = stringResource(R.string.summary_entries, unitFormatter.count(state.entries.size)),
                modifier = metricModifier(),
            )
        }
        item {
            PeriodBarChart(
                title = stringResource(metricData.titleRes),
                values = metricData.values,
                selectedRange = state.selectedRange,
                period = period,
                accentColor = metricData.color.copy(alpha = 0.85f),
                summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · ${metricData.total.text}",
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                valueFormatter = metricData.valueFormatter,
            )
        }
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
            valueFormatter = { unitFormatter.energy(it).text },
        )
        NutritionMetric.PROTEIN -> NutritionMetricData(
            titleRes = R.string.metric_protein,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.proteinGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.proteinGrams) },
            color = proteinMetricColor,
            valueFormatter = { "${unitFormatter.count(it.roundToInt())} $GramsUnit" },
        )
        NutritionMetric.CARBS -> NutritionMetricData(
            titleRes = R.string.metric_carbs,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.carbsGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.carbsGrams) },
            color = carbsMetricColor,
            valueFormatter = { "${unitFormatter.count(it.roundToInt())} $GramsUnit" },
        )
        NutritionMetric.FAT -> NutritionMetricData(
            titleRes = R.string.metric_fat,
            total = DisplayValue(unitFormatter.count(data.sumOf { it.fatGrams }.roundToInt()), GramsUnit),
            values = data.map { PeriodChartValue(date = it.date, value = it.fatGrams) },
            color = fatMetricColor,
            valueFormatter = { "${unitFormatter.count(it.roundToInt())} $GramsUnit" },
        )
    }

private const val GramsUnit = "g"

private data class NutritionMetricData(
    val titleRes: Int,
    val total: DisplayValue,
    val values: List<PeriodChartValue>,
    val color: Color,
    val valueFormatter: (Double) -> String,
)

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
