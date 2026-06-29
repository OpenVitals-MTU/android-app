package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import java.time.LocalDate

@Composable
fun MetricBarChart(
    title: String,
    values: List<PeriodChartValue>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryValue: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    accentAlpha: Float = 0.85f,
    yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    PeriodHistoryChart(
        title = title,
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor.copy(alpha = accentAlpha),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · $summaryValue",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        yearAggregation = yearAggregation,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}

@Composable
fun <T> MetricBarChart(
    title: String,
    data: List<T>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryValue: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    date: (T) -> LocalDate,
    value: (T) -> Double,
    modifier: Modifier = Modifier,
    accentAlpha: Float = 0.85f,
    yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    MetricBarChart(
        title = title,
        values = data.map { item -> PeriodChartValue(date = date(item), value = value(item)) },
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor,
        summaryValue = summaryValue,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        accentAlpha = accentAlpha,
        yearAggregation = yearAggregation,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}
