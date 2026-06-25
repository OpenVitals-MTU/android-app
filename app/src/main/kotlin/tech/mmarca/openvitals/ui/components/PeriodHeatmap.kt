package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class PeriodHeatmapCell(
    val date: LocalDate?,
    val value: Double,
    val isWithinLoadedPeriod: Boolean,
)

fun periodMonthHeatmapCells(
    values: List<PeriodChartValue>,
    period: DatePeriod,
): List<PeriodHeatmapCell> {
    val firstDay = period.start.withDayOfMonth(1)
    val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
    val valuesByDate = values
        .groupBy { it.date }
        .mapValues { (_, dayValues) -> dayValues.sumOf { it.value } }

    val leadingEmptyCells = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val dayCells = generateSequence(firstDay) { date ->
        date.plusDays(1).takeUnless { it.isAfter(lastDay) }
    }.map { date ->
        PeriodHeatmapCell(
            date = date,
            value = valuesByDate[date] ?: 0.0,
            isWithinLoadedPeriod = !date.isAfter(period.end),
        )
    }.toList()

    val totalCellsBeforeTrailing = leadingEmptyCells + dayCells.size
    val trailingEmptyCells = (7 - totalCellsBeforeTrailing % 7).takeUnless { it == 7 } ?: 0

    return List(leadingEmptyCells) { emptyHeatmapCell() } +
        dayCells +
        List(trailingEmptyCells) { emptyHeatmapCell() }
}

fun periodYearHeatmapCells(
    values: List<PeriodChartValue>,
    period: DatePeriod,
): List<PeriodHeatmapCell> {
    val firstDay = period.start.withDayOfYear(1)
    val lastDay = firstDay.withDayOfYear(firstDay.lengthOfYear())
    val valuesByDate = values
        .groupBy { it.date }
        .mapValues { (_, dayValues) -> dayValues.sumOf { it.value } }

    return generateSequence(firstDay) { date ->
        date.plusDays(1).takeUnless { it.isAfter(lastDay) }
    }.map { date ->
        PeriodHeatmapCell(
            date = date,
            value = valuesByDate[date] ?: 0.0,
            isWithinLoadedPeriod = !date.isAfter(period.end),
        )
    }.toList()
}

@Composable
fun PeriodMonthHeatmap(
    title: String,
    values: List<PeriodChartValue>,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val cells = remember(values, period) { periodMonthHeatmapCells(values, period) }
    val minPositiveValue = cells.map { it.value }.filter { it > 0.0 }.minOrNull() ?: 0.0
    val maxValue = cells.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    val dayFormatter = dateTimeFormatterProvider.chartDayOfMonth()
    val monthStart = period.start.withDayOfMonth(1)
    val weekdays = remember(monthStart) {
        (0..6).map { offset ->
            monthStart
                .with(DayOfWeek.MONDAY)
                .plusDays(offset.toLong())
                .dayOfWeek
                .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        }
    }

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PeriodHeatmapHeader(title, summaryText)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekdays.forEach { weekday ->
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            cells.chunked(7).forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCells.forEach { cell ->
                        val date = cell.date
                        val cellColor = heatmapCellColor(
                            value = cell.value,
                            minPositiveValue = minPositiveValue,
                            maxValue = maxValue,
                            isWithinLoadedPeriod = cell.isWithinLoadedPeriod,
                            accentColor = accentColor,
                        )
                        val isSelected = date != null && date == selectedDate
                        val cellModifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(cellColor, MaterialTheme.shapes.small)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, accentColor, MaterialTheme.shapes.small)
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (date != null && cell.isWithinLoadedPeriod && onDateSelected != null) {
                                    Modifier.clickable { onDateSelected(date) }
                                } else {
                                    Modifier
                                },
                            )
                        Box(
                            modifier = cellModifier,
                            contentAlignment = Alignment.Center,
                        ) {
                            date?.let {
                                Text(
                                    text = dayFormatter.format(it),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            HeatmapLegend(accentColor = accentColor, minPositiveValue = minPositiveValue, maxValue = maxValue)
        }
    }
}

@Composable
fun PeriodYearHeatmap(
    title: String,
    values: List<PeriodChartValue>,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    modifier: Modifier = Modifier,
) {
    val cells = remember(values, period) { periodYearHeatmapCells(values, period) }
    val minPositiveValue = cells.map { it.value }.filter { it > 0.0 }.minOrNull() ?: 0.0
    val maxValue = cells.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PeriodHeatmapHeader(title, summaryText)
            Spacer(Modifier.height(16.dp))
            cells.chunked(20).forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowCells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = heatmapCellColor(
                                        value = cell.value,
                                        minPositiveValue = minPositiveValue,
                                        maxValue = maxValue,
                                        isWithinLoadedPeriod = cell.isWithinLoadedPeriod,
                                        accentColor = accentColor,
                                    ),
                                    shape = CircleShape,
                                ),
                        )
                    }
                    if (rowCells.size < 20) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            HeatmapLegend(accentColor = accentColor, minPositiveValue = minPositiveValue, maxValue = maxValue)
        }
    }
}

@Composable
fun PeriodHistoryChart(
    title: String,
    values: List<PeriodChartValue>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    when (selectedRange) {
        TimeRange.MONTH -> PeriodMonthHeatmap(
            title = title,
            values = values,
            period = period,
            accentColor = accentColor,
            summaryText = summaryText,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = modifier,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
        )
        TimeRange.YEAR -> PeriodYearHeatmap(
            title = title,
            values = values,
            period = period,
            accentColor = accentColor,
            summaryText = summaryText,
            modifier = modifier,
        )
        TimeRange.DAY,
        TimeRange.WEEK -> PeriodBarChart(
            title = title,
            values = values,
            selectedRange = selectedRange,
            period = period,
            accentColor = accentColor,
            summaryText = summaryText,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = modifier,
            yearAggregation = yearAggregation,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = valueFormatter,
        )
    }
}

@Composable
private fun PeriodHeatmapHeader(
    title: String,
    summaryText: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = summaryText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HeatmapLegend(
    accentColor: Color,
    minPositiveValue: Double,
    maxValue: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.legend_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        repeat(5) { index ->
            val value = if (maxValue <= minPositiveValue) {
                maxValue
            } else {
                minPositiveValue + (maxValue - minPositiveValue) * index / 4.0
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = heatmapCellColor(
                            value = value,
                            minPositiveValue = minPositiveValue,
                            maxValue = maxValue,
                            isWithinLoadedPeriod = true,
                            accentColor = accentColor,
                        ),
                        shape = CircleShape,
                    ),
            )
            if (index < 4) {
                Spacer(Modifier.size(6.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.legend_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun heatmapCellColor(
    value: Double,
    minPositiveValue: Double,
    maxValue: Double,
    isWithinLoadedPeriod: Boolean,
    accentColor: Color,
): Color {
    if (!isWithinLoadedPeriod) {
        return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    }
    if (value <= 0.0) {
        return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    }
    val fraction = if (maxValue <= minPositiveValue) {
        1f
    } else {
        ((value - minPositiveValue) / (maxValue - minPositiveValue)).toFloat().coerceIn(0f, 1f)
    }
    return accentColor.copy(alpha = 0.25f + 0.75f * fraction)
}

private fun emptyHeatmapCell(): PeriodHeatmapCell =
    PeriodHeatmapCell(
        date = null,
        value = 0.0,
        isWithinLoadedPeriod = false,
    )
