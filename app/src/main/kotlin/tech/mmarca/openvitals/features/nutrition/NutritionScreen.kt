package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.MealType
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.data.model.NutritionEntry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import java.time.ZoneId
import kotlin.math.roundToInt

private val proteinColor = Color(0xFF7E57C2)
private val carbsColor = Color(0xFF26A69A)
private val fatColor = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
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
        if (state.dailyMacros.isEmpty() && state.entries.isEmpty() && !state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = "Nutrition",
                    icon = Icons.Outlined.Restaurant,
                    accentColor = NutritionColor,
                    message = "No nutrition entries were recorded for this period.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.dailyMacros.isNotEmpty()) {
            item {
                NutritionSummary(
                    state = state,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                MacroSummaryCard(
                    proteinGrams = state.totalProteinGrams,
                    carbsGrams = state.totalCarbsGrams,
                    fatGrams = state.totalFatGrams,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                EnergyBarChart(
                    data = state.dailyMacros,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.entries.isNotEmpty()) {
            item { SectionHeader("Meals") }
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
}

@Composable
private fun NutritionSummary(
    state: NutritionUiState,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val energy = unitFormatter.energy(state.totalEnergyKcal)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = "Calories in",
            value = energy.value,
            unit = energy.unit,
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            subtitle = "${unitFormatter.count(state.entries.size)} entries",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = "Protein",
            value = unitFormatter.count(state.totalProteinGrams.roundToInt()),
            unit = "g",
            icon = Icons.Outlined.Restaurant,
            accentColor = proteinColor,
            subtitle = "Across selected period",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MacroSummaryCard(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = proteinGrams + carbsGrams + fatGrams

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Macros",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            ) {
                MacroSegment(if (total > 0.0) proteinGrams / total else 1.0 / 3.0, proteinColor)
                MacroSegment(if (total > 0.0) carbsGrams / total else 1.0 / 3.0, carbsColor)
                MacroSegment(if (total > 0.0) fatGrams / total else 1.0 / 3.0, fatColor)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MacroLegend("Protein", proteinGrams, proteinColor, unitFormatter)
                MacroLegend("Carbs", carbsGrams, carbsColor, unitFormatter)
                MacroLegend("Fat", fatGrams, fatColor, unitFormatter)
            }
        }
    }
}

@Composable
private fun RowScope.MacroSegment(fraction: Double, color: Color) {
    Box(
        modifier = Modifier
            .weight(fraction.toFloat().coerceAtLeast(0.01f))
            .height(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            drawRoundRect(
                color = color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MacroLegend(
    label: String,
    grams: Double,
    color: Color,
    unitFormatter: UnitFormatter,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${unitFormatter.count(grams.roundToInt())} g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EnergyBarChart(
    data: List<DailyMacros>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    PeriodBarChart(
        title = "Calories in",
        values = data.map { PeriodChartValue(date = it.date, value = it.energyKcal) },
        selectedRange = selectedRange,
        period = period,
        accentColor = NutritionColor.copy(alpha = 0.85f),
        summaryText = "${periodTitle(selectedRange, period)} · ${unitFormatter.energy(data.sumOf { it.energyKcal }).text}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}

@Composable
private fun NutritionEntryRow(
    entry: NutritionEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val start = entry.time.atZone(ZoneId.systemDefault())
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = NutritionColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: mealTypeLabel(entry.mealType),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = macroLine(entry, unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.energyKcal?.let { unitFormatter.energy(it).text } ?: "No kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = CaloriesColor,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = entry.source)
            }
        }
    }
}

private fun macroLine(entry: NutritionEntry, unitFormatter: UnitFormatter): String {
    val parts = buildList {
        entry.proteinGrams?.let { add("P ${unitFormatter.count(it.roundToInt())}g") }
        entry.carbsGrams?.let { add("C ${unitFormatter.count(it.roundToInt())}g") }
        entry.fatGrams?.let { add("F ${unitFormatter.count(it.roundToInt())}g") }
        entry.fiberGrams?.let { add("fiber ${unitFormatter.count(it.roundToInt())}g") }
        entry.sugarGrams?.let { add("sugar ${unitFormatter.count(it.roundToInt())}g") }
    }
    return parts.ifEmpty { listOf(mealTypeLabel(entry.mealType)) }.joinToString(" · ")
}

private fun mealTypeLabel(mealType: Int): String = when (mealType) {
    MealType.MEAL_TYPE_BREAKFAST -> "Breakfast"
    MealType.MEAL_TYPE_LUNCH -> "Lunch"
    MealType.MEAL_TYPE_DINNER -> "Dinner"
    MealType.MEAL_TYPE_SNACK -> "Snack"
    else -> "Meal"
}
