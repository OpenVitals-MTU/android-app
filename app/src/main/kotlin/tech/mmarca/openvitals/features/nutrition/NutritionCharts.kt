package tech.mmarca.openvitals.features.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodTitle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyMacros
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.theme.NutritionColor

@Composable
internal fun EnergyBarChart(
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
