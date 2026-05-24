package tech.mmarca.openvitals.features.body

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun BodyFatLineChart(
    entries: List<BodyFatEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.time }
    val maxPct = sorted.maxOfOrNull { it.percent } ?: 30.0
    val minPct = sorted.minOfOrNull { it.percent } ?: 0.0

    PeriodHistoryChart(
        title = stringResource(R.string.metric_body_fat),
        values = bodyFatHistoryValues(sorted),
        selectedRange = selectedRange,
        period = period,
        accentColor = BodyFatColor.copy(alpha = 0.85f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.percent(minPct).text} - ${
            unitFormatter.percent(maxPct).text
        } · ${stringResource(R.string.summary_entries, unitFormatter.count(sorted.size))}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.percent(it).text },
    )
}

@Composable
internal fun WeightLineChart(
    entries: List<WeightEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val sorted = entries.sortedBy { it.time }
    val maxKg = sorted.maxOfOrNull { it.weightKg } ?: 100.0
    val minKg = sorted.minOfOrNull { it.weightKg } ?: 50.0

    PeriodHistoryChart(
        title = stringResource(R.string.metric_weight),
        values = weightHistoryValues(sorted),
        selectedRange = selectedRange,
        period = period,
        accentColor = WeightColor.copy(alpha = 0.85f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.weight(minKg).text} - ${
            unitFormatter.weight(maxKg).text
        } · ${stringResource(R.string.summary_entries, unitFormatter.count(sorted.size))}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.weight(it).text },
    )
}

private fun weightHistoryValues(entries: List<WeightEntry>): List<PeriodChartValue> =
    entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(
                date = date,
                value = dayEntries.maxByOrNull { it.time }?.weightKg ?: 0.0,
            )
        }

private fun bodyFatHistoryValues(entries: List<BodyFatEntry>): List<PeriodChartValue> =
    entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(
                date = date,
                value = dayEntries.maxByOrNull { it.time }?.percent ?: 0.0,
            )
        }
