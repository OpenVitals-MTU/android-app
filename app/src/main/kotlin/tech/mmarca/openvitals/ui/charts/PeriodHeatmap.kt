package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.usesRollingDates
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

/**
 * The month grid cells with fillers. A calendar month draws the whole month
 * of [DatePeriod.start]; a [rolling] window draws `[period.start, period.end]`.
 */
fun periodMonthHeatmapCells(
    values: List<PeriodChartValue>,
    period: DatePeriod,
    rolling: Boolean = false,
): List<PeriodHeatmapCell> {
    val firstDay = if (rolling) period.start else period.start.withDayOfMonth(1)
    val lastDay = if (rolling) period.end else firstDay.withDayOfMonth(firstDay.lengthOfMonth())
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
            isWithinLoadedPeriod = !date.isBefore(period.start) && !date.isAfter(period.end),
        )
    }.toList()

    val totalCellsBeforeTrailing = leadingEmptyCells + dayCells.size
    val trailingEmptyCells = (7 - totalCellsBeforeTrailing % 7).takeUnless { it == 7 } ?: 0

    return List(leadingEmptyCells) { emptyHeatmapCell() } +
        dayCells +
        List(trailingEmptyCells) { emptyHeatmapCell() }
}

/**
 * The year grid cells, Monday-aligned into week columns. A calendar year
 * draws the whole year; a [rolling] window draws `[period.start, period.end]`.
 */
fun periodYearHeatmapCells(
    values: List<PeriodChartValue>,
    period: DatePeriod,
    rolling: Boolean = false,
): List<PeriodHeatmapCell> {
    val firstDay = if (rolling) period.start else period.start.withDayOfYear(1)
    val lastDay = if (rolling) period.end else firstDay.withDayOfYear(firstDay.lengthOfYear())
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
            isWithinLoadedPeriod = !date.isBefore(period.start) && !date.isAfter(period.end),
        )
    }.toList()

    val totalCellsBeforeTrailing = leadingEmptyCells + dayCells.size
    val trailingEmptyCells = (7 - totalCellsBeforeTrailing % 7).takeUnless { it == 7 } ?: 0

    return List(leadingEmptyCells) { emptyHeatmapCell() } +
        dayCells +
        List(trailingEmptyCells) { emptyHeatmapCell() }
}

/** The week columns that begin a month, to their first day. These carry the labels. */
internal fun yearHeatmapMonthStartColumns(
    weeks: List<List<PeriodHeatmapCell>>,
): List<Pair<Int, LocalDate>> =
    weeks.mapIndexedNotNull { index, week ->
        week.firstNotNullOfOrNull { cell -> cell.date?.takeIf { it.dayOfMonth == 1 } }
            ?.let { monthStart -> index to monthStart }
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
    val rolling = LocalPeriodWeekMode.current.usesRollingDates()
    // Inside a metric-detail scaffold a tap drills into the Day view.
    val openDay = LocalMetricDayOpener.current
    val onCellTapped = openDay ?: onDateSelected
    val cells = remember(values, period, rolling) { periodMonthHeatmapCells(values, period, rolling) }
    val minPositiveValue = cells.map { it.value }.filter { it > 0.0 }.minOrNull() ?: 0.0
    val maxValue = cells.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    val dayFormatter = dateTimeFormatterProvider.chartDayOfMonth()
    val gridStart = if (rolling) period.start else period.start.withDayOfMonth(1)
    val weekdays = remember(gridStart) {
        (0..6).map { offset ->
            gridStart
                .with(DayOfWeek.MONDAY)
                .plusDays(offset.toLong())
                .dayOfWeek
                .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        }
    }

    // A Canvas publishes no semantics; this line does.
    OpenVitalsCard(
        modifier = modifier.chartSemantics(
            chartSemanticSummary(title = title, summaryText = summaryText),
        ),

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
                                if (date != null && cell.isWithinLoadedPeriod && onCellTapped != null) {
                                    Modifier.clickable { onCellTapped(date) }
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
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val rolling = LocalPeriodWeekMode.current.usesRollingDates()
    val cells = remember(values, period, rolling) { periodYearHeatmapCells(values, period, rolling) }
    val weeks = remember(cells) { cells.chunked(DaysPerWeek) }
    val minPositiveValue = cells.map { it.value }.filter { it > 0.0 }.minOrNull() ?: 0.0
    val maxValue = cells.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0

    // A Canvas publishes no semantics; this line does.
    OpenVitalsCard(
        modifier = modifier.chartSemantics(
            chartSemanticSummary(title = title, summaryText = summaryText),
        ),

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PeriodHeatmapHeader(title, summaryText)
            Spacer(Modifier.height(16.dp))
            YearHeatmapGrid(
                weeks = weeks,
                minPositiveValue = minPositiveValue,
                maxValue = maxValue,
                accentColor = accentColor,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            HeatmapLegend(accentColor = accentColor, minPositiveValue = minPositiveValue, maxValue = maxValue)
        }
    }
}

/** The year as a week-per-column calendar with month names, so April can be found. */
@Composable
private fun YearHeatmapGrid(
    weeks: List<List<PeriodHeatmapCell>>,
    minPositiveValue: Double,
    maxValue: Double,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val emptyDayColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = HeatmapEmptyDayAlpha)
    val outsidePeriodColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = HeatmapOutsidePeriodAlpha)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val textMeasurer = rememberTextMeasurer()
    val monthFormatter = remember(dateTimeFormatterProvider) { dateTimeFormatterProvider.chartMonth() }
    val monthColumns = remember(weeks) { yearHeatmapMonthStartColumns(weeks) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val weekCount = weeks.size.coerceAtLeast(1)
        val cell = (maxWidth - YearHeatmapCellGap * (weekCount - 1)) / weekCount
        val gridHeight = YearHeatmapLabelHeight +
            cell * DaysPerWeek +
            YearHeatmapCellGap * (DaysPerWeek - 1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
        ) {
            val cellPx = cell.toPx()
            val gapPx = YearHeatmapCellGap.toPx()
            val gridTop = YearHeatmapLabelHeight.toPx()
            val cornerRadius = CornerRadius(cellPx * 0.3f, cellPx * 0.3f)

            // Greedy from the left: a label that would overlap is dropped.
            var previousLabelEnd = Float.NEGATIVE_INFINITY
            monthColumns.forEach { (weekIndex, monthStart) ->
                val left = weekIndex * (cellPx + gapPx)
                val layout = textMeasurer.measure(
                    text = monthFormatter.format(monthStart),
                    style = labelStyle,
                    maxLines = 1,
                )
                if (left >= previousLabelEnd && left + layout.size.width <= size.width) {
                    drawText(layout, topLeft = Offset(left, 0f))
                    previousLabelEnd = left + layout.size.width + YearHeatmapLabelGap.toPx()
                }
            }

            weeks.forEachIndexed { weekIndex, week ->
                val left = weekIndex * (cellPx + gapPx)
                week.forEachIndexed { dayIndex, dayCell ->
                    // Fillers keep the columns Monday-aligned; nothing is drawn.
                    if (dayCell.date == null) return@forEachIndexed
                    drawRoundRect(
                        color = heatmapCellColor(
                            value = dayCell.value,
                            minPositiveValue = minPositiveValue,
                            maxValue = maxValue,
                            isWithinLoadedPeriod = dayCell.isWithinLoadedPeriod,
                            accentColor = accentColor,
                            emptyDayColor = emptyDayColor,
                            outsidePeriodColor = outsidePeriodColor,
                        ),
                        topLeft = Offset(left, gridTop + dayIndex * (cellPx + gapPx)),
                        size = Size(cellPx, cellPx),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }
    }
}

private const val DaysPerWeek = 7
private val YearHeatmapCellGap = 1.5.dp
private val YearHeatmapLabelHeight = 18.dp

/** The clear space two neighbouring month labels keep between them. */
private val YearHeatmapLabelGap = 4.dp

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
            dateTimeFormatterProvider = dateTimeFormatterProvider,
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
): Color = heatmapCellColor(
    value = value,
    minPositiveValue = minPositiveValue,
    maxValue = maxValue,
    isWithinLoadedPeriod = isWithinLoadedPeriod,
    accentColor = accentColor,
    emptyDayColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = HeatmapEmptyDayAlpha),
    outsidePeriodColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = HeatmapOutsidePeriodAlpha),
)

private fun heatmapCellColor(
    value: Double,
    minPositiveValue: Double,
    maxValue: Double,
    isWithinLoadedPeriod: Boolean,
    accentColor: Color,
    emptyDayColor: Color,
    outsidePeriodColor: Color,
): Color {
    if (!isWithinLoadedPeriod) {
        return outsidePeriodColor
    }
    if (value <= 0.0) {
        return emptyDayColor
    }
    val fraction = if (maxValue <= minPositiveValue) {
        1f
    } else {
        ((value - minPositiveValue) / (maxValue - minPositiveValue)).toFloat().coerceIn(0f, 1f)
    }
    return accentColor.copy(alpha = HeatmapMinCellAlpha + (1f - HeatmapMinCellAlpha) * fraction)
}

/** The faintest a tracked day may be drawn. At 0.25 it matched the untracked grey. */
private const val HeatmapMinCellAlpha = 0.4f

/** A day inside the period with nothing tracked: visibly a slot, visibly empty. */
private const val HeatmapEmptyDayAlpha = 0.65f

/** A day the grid draws but the loaded period does not cover — future, or before a rolling window. */
private const val HeatmapOutsidePeriodAlpha = 0.35f

private fun emptyHeatmapCell(): PeriodHeatmapCell =
    PeriodHeatmapCell(
        date = null,
        value = 0.0,
        isWithinLoadedPeriod = false,
    )
