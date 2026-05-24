package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.LocalDate

data class PeriodChartValue(
    val date: LocalDate,
    val value: Double,
)

data class PeriodChartBucket(
    val date: LocalDate,
    val value: Double,
)

enum class PeriodBarAggregation {
    SUM,
    AVERAGE,
    AVERAGE_NON_ZERO,
}

fun periodBarBuckets(
    values: List<PeriodChartValue>,
    selectedRange: TimeRange,
    period: DatePeriod,
    yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
): List<PeriodChartBucket> {
    val dailyBuckets = dailyBuckets(values, period)
    if (selectedRange != TimeRange.YEAR) {
        return dailyBuckets
    }

    val endMonth = period.end.withDayOfMonth(1)
    return generateSequence(period.start.withDayOfMonth(1)) { monthStart ->
        monthStart.plusMonths(1).takeUnless { it.isAfter(endMonth) }
    }.map { monthStart ->
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        val monthValues = dailyBuckets
            .asSequence()
            .filter { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            .map { it.value }
            .toList()
        PeriodChartBucket(
            date = monthStart,
            value = when (yearAggregation) {
                PeriodBarAggregation.SUM -> monthValues.sum()
                PeriodBarAggregation.AVERAGE -> monthValues.averageOrZero()
                PeriodBarAggregation.AVERAGE_NON_ZERO -> monthValues.filter { it > 0.0 }.averageOrZero()
            },
        )
    }.toList()
}

fun isPeriodChartLabelVisible(index: Int, lastIndex: Int, selectedRange: TimeRange): Boolean =
    when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> true
        TimeRange.YEAR -> lastIndex <= 11 || index % 30 == 0 || index == lastIndex
        TimeRange.MONTH -> index % 5 == 0 || index == lastIndex
    }

@Composable
fun PeriodBarChart(
    title: String,
    values: List<PeriodChartValue>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
    chartHeight: Dp = 120.dp,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val buckets = remember(values, selectedRange, period, yearAggregation) {
        periodBarBuckets(
            values = values,
            selectedRange = selectedRange,
            period = period,
            yearAggregation = yearAggregation,
        )
    }
    val maxValue = buckets.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    val gridColor = accentColor.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val chartTapModifier = if (
        selectedRange == TimeRange.WEEK &&
        onDateSelected != null &&
        buckets.isNotEmpty()
    ) {
        Modifier.pointerInput(buckets, onDateSelected) {
            detectTapGestures { offset ->
                val slotWidth = size.width.toFloat() / buckets.size
                val index = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
                onDateSelected(buckets[index].date)
            }
        }
    } else {
        Modifier
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            YAxisChart(
                labels = chartYAxisLabels(0.0, maxValue, valueFormatter),
                chartHeight = chartHeight,
                canvasModifier = chartTapModifier,
            ) {
                drawYAxisGuides(
                    gridColor = gridColor,
                    axisColor = axisColor,
                    strokeWidth = 1.dp.toPx(),
                )
                if (buckets.isEmpty()) return@YAxisChart

                val slotWidth = size.width / buckets.size
                val gap = when {
                    buckets.size <= 7 -> 8.dp.toPx()
                    buckets.size <= 12 -> 6.dp.toPx()
                    buckets.size <= 31 -> 3.dp.toPx()
                    else -> 1.dp.toPx()
                }.coerceAtMost(slotWidth * 0.6f)
                val barWidth = (slotWidth - gap).coerceAtLeast(1.dp.toPx())
                val minVisibleHeight = 4.dp.toPx()

                buckets.forEachIndexed { index, bucket ->
                    val slotLeft = index * slotWidth
                    val isSelected = selectedDate == bucket.date && selectedRange == TimeRange.WEEK
                    if (isSelected) {
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.16f),
                            topLeft = Offset(slotLeft, 0f),
                            size = Size(slotWidth, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                    }
                    val value = bucket.value.coerceAtLeast(0.0)
                    if (value <= 0.0) return@forEachIndexed

                    val fraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
                    val barHeight = (size.height * fraction).coerceAtLeast(minVisibleHeight)
                    val left = slotLeft + (slotWidth - barWidth) / 2f
                    val top = size.height - barHeight
                    val radius = (barWidth / 2f).coerceAtMost(8.dp.toPx())

                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ChartXAxisWithYAxis {
                PeriodChartXAxis(
                    dates = buckets.map { it.date },
                    selectedRange = selectedRange,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PeriodChartXAxis(
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        dates.forEachIndexed { index, date ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (isPeriodChartLabelVisible(index, dates.lastIndex, selectedRange)) {
                    Text(
                        text = periodChartLabel(
                            date = date,
                            selectedRange = selectedRange,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun dailyBuckets(values: List<PeriodChartValue>, period: DatePeriod): List<PeriodChartBucket> {
    val valuesByDate = values
        .groupBy { it.date }
        .mapValues { (_, dayValues) -> dayValues.sumOf { it.value } }

    return generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        PeriodChartBucket(
            date = date,
            value = valuesByDate[date] ?: 0.0,
        )
    }.toList()
}

private fun periodChartLabel(
    date: LocalDate,
    selectedRange: TimeRange,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String = when (selectedRange) {
    TimeRange.DAY,
    TimeRange.WEEK -> dateTimeFormatterProvider.chartDay().format(date)
    TimeRange.MONTH -> dateTimeFormatterProvider.chartDayOfMonth().format(date)
    TimeRange.YEAR -> dateTimeFormatterProvider.chartMonth().format(date)
}

private fun List<Double>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else average()
