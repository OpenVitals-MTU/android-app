package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.roundToInt

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

/**
 * The horizontal geometry of one bar chart: how wide a slot is, and how much of that slot
 * the bar itself takes.
 *
 * Pulled out of the painter because it is arithmetic, not drawing, and because the axis
 * strip underneath has to agree with it exactly.
 */
@Immutable
data class PeriodBarGeometry(
    val visibleSlots: Float,
    val slotWidth: Float,
    val gap: Float,
    val barWidth: Float,
)

/** The gap, in dp, between bars on a plot showing [visibleSlots] of them. */
fun periodBarGapDp(visibleSlots: Int): Float = when {
    visibleSlots <= 7 -> 8f
    visibleSlots <= 12 -> 6f
    visibleSlots <= 31 -> 3f
    else -> 1f
}

/**
 * The slot/bar widths for a plot [plotWidth] px across showing [viewportSpan] of
 * [bucketCount] buckets, at [pxPerDp] pixels to the dp.
 *
 * Sized by how many slots FIT on screen, not by how many exist: the gap is about how
 * crowded the plot LOOKS, and a zoomed-in chart showing three bars is not crowded just
 * because the week has seven.
 */
fun periodBarGeometry(
    plotWidth: Float,
    bucketCount: Int,
    viewportSpan: Float,
    pxPerDp: Float,
): PeriodBarGeometry {
    val visibleSlots = (bucketCount * viewportSpan).coerceIn(1f, 1e9f)
    val slotWidth = plotWidth / visibleSlots
    val gap = (periodBarGapDp(visibleSlots.roundToInt()) * pxPerDp)
        .coerceIn(0f, (slotWidth * 0.6f).coerceAtLeast(0f))
    val barWidth = (slotWidth - gap).coerceAtLeast(pxPerDp)
    return PeriodBarGeometry(
        visibleSlots = visibleSlots,
        slotWidth = slotWidth,
        gap = gap,
        barWidth = barWidth,
    )
}

/**
 * Where slot [index] of [bucketCount] starts, as a fraction of the plot. A bucket is a
 * SLOT on the same `0..1` axis every other chart uses — the nth of n — so zooming it is
 * the same arithmetic, and the slots simply get wider as fewer of them are on screen.
 */
fun periodSlotLeftFraction(index: Int, bucketCount: Int, viewport: ChartViewport): Float =
    viewport.visibleFraction(index.toFloat() / bucketCount.coerceAtLeast(1))

/**
 * Whether a slot starting at [slotLeft] and [slotWidth] wide overlaps a plot [plotWidth]
 * across. Wholly off one side or the other there is nothing to draw, and laying out its
 * label would be work done for a bar nobody can see.
 */
fun isPeriodSlotVisible(slotLeft: Float, slotWidth: Float, plotWidth: Float): Boolean =
    slotLeft <= plotWidth && slotLeft + slotWidth >= 0f

/** Which bucket a tap [xFraction] of the way across the PLOT landed on. */
fun periodBarIndexAt(xFraction: Float, bucketCount: Int, viewport: ChartViewport): Int {
    if (bucketCount <= 0) return 0
    val dataFraction = viewport.dataFraction(xFraction)
    return floor(dataFraction * bucketCount).toInt().coerceIn(0, bucketCount - 1)
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
    val textMeasurer = rememberTextMeasurer()
    val barLabelStyle = MaterialTheme.typography.labelSmall.copy(
        color = if (accentColor.luminance() > 0.25f) {
            Color.Black.copy(alpha = 0.78f)
        } else {
            Color.White
        },
        fontWeight = FontWeight.Bold,
    )
    val canSelect = selectedRange == TimeRange.WEEK &&
        onDateSelected != null &&
        buckets.isNotEmpty()

    // A Canvas publishes no semantics, so without this the chart is absent to a
    // screen reader rather than merely hard to read.
    OpenVitalsCard(
        modifier = modifier.chartSemantics(
            chartSemanticSummary(title = title, summaryText = summaryText),
        ),

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            // The bars and the dates under them are BOTH inside the zoom and share the
            // one viewport. A bar chart whose dates had drifted off the bars they name
            // would be worse than one that never zoomed at all.
            //
            // No scrubber here, deliberately: a scrub layer on the period charts broke
            // the pinch (its drag detector claimed the first finger of the pinch) and was
            // reverted in the Flutter app. The tap-to-select-day interaction is untouched
            // — the zoom claims nothing single-finger.
            ChartZoom(selectedRange, period.start, period.end) { zoom ->
                val viewport = zoom.viewport
                val currentViewport by rememberUpdatedState(viewport)
                val chartTapModifier = if (canSelect) {
                    Modifier.pointerInput(buckets, onDateSelected) {
                        detectTapGestures { offset ->
                            // The finger lands on the PLOT; the bucket it is over depends
                            // on what is on show. Tapping the third bar of a zoomed chart
                            // has to select the third bar you can SEE.
                            val xFraction =
                                (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val index =
                                periodBarIndexAt(xFraction, buckets.size, currentViewport)
                            onDateSelected?.invoke(buckets[index].date)
                        }
                    }
                } else {
                    Modifier
                }

                Column {
                    ChartReveal { progress ->
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight)
                                .then(chartTapModifier),
                        ) {
                            drawPeriodBars(
                                buckets = buckets,
                                maxValue = maxValue,
                                accentColor = accentColor,
                                selectedDate = selectedDate,
                                selectedRange = selectedRange,
                                viewport = viewport,
                                progress = progress,
                                textMeasurer = textMeasurer,
                                barLabelStyle = barLabelStyle,
                                valueFormatter = valueFormatter,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PeriodChartXAxis(
                        dates = buckets.map { it.date },
                        selectedRange = selectedRange,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        viewport = viewport,
                    )
                }
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

private fun DrawScope.drawPeriodBars(
    buckets: List<PeriodChartBucket>,
    maxValue: Double,
    accentColor: Color,
    selectedDate: LocalDate?,
    selectedRange: TimeRange,
    viewport: ChartViewport,
    progress: Float,
    textMeasurer: TextMeasurer,
    barLabelStyle: TextStyle,
    valueFormatter: (Double) -> String,
) {
    if (buckets.isEmpty()) return

    val geometry = periodBarGeometry(
        plotWidth = size.width,
        bucketCount = buckets.size,
        viewportSpan = viewport.span,
        pxPerDp = 1.dp.toPx(),
    )
    val slotWidth = geometry.slotWidth
    val barWidth = geometry.barWidth
    val minVisibleHeight = 4.dp.toPx()

    // Zoomed, the outermost bars run past the plot edges; clip so they end at the plot
    // rather than spilling across the card.
    val drawBars: DrawScope.() -> Unit = {
        buckets.forEachIndexed { index, bucket ->
            val slotLeft = periodSlotLeftFraction(index, buckets.size, viewport) * size.width
            if (!isPeriodSlotVisible(slotLeft, slotWidth, size.width)) return@forEachIndexed

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
            val labelLayout = measureBarValueLabel(
                textMeasurer = textMeasurer,
                text = valueFormatter(value),
                style = barLabelStyle,
                width = barWidth,
            )
            val minLabelHeight = labelLayout
                ?.let { it.height + 4.dp.toPx() }
                ?: minVisibleHeight
            val barHeight = (size.height * fraction)
                .coerceAtLeast(maxOf(minVisibleHeight, minLabelHeight))
                .coerceAtMost(size.height)
            val left = slotLeft + (slotWidth - barWidth) / 2f
            // Grown by `progress`. The LABEL is not: it is laid out against the bar's
            // FINAL height (that is what reserves room for it) and drawn only once the
            // bar has arrived — a number sliding up inside a growing rectangle is a
            // number nobody can read, and a label that overflows a half-grown bar is
            // worse than one that waits.
            val drawnHeight = barHeight * progress.coerceIn(0f, 1f)
            val top = size.height - drawnHeight
            val radius = (barWidth / 2f).coerceAtMost(8.dp.toPx())

            drawRoundRect(
                color = accentColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, drawnHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
            if (progress >= 1f) {
                labelLayout?.let { layout ->
                    drawBarValueLabel(
                        textLayout = layout,
                        left = left,
                        top = top,
                        width = barWidth,
                        height = barHeight,
                    )
                }
            }
        }
    }
    if (viewport.isZoomed) clipRect { drawBars() } else drawBars()
}

private data class BarValueLabelLayout(
    val lines: List<TextLayoutResult>,
    val width: Int,
    val height: Int,
)

private fun DrawScope.measureBarValueLabel(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    width: Float,
): BarValueLabelLayout? {
    val lines = barLabelLines(text) ?: return null
    val horizontalPadding = 2.dp.toPx()
    val measured = measureBarLabelLines(
        lines = lines,
        maxWidth = width - horizontalPadding * 2f,
        lineGap = 1.dp.toPx().toInt(),
        measure = { line ->
            textMeasurer.measure(
                text = line,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        },
        lineWidth = { it.size.width },
        lineHeight = { it.size.height },
    ) ?: return null
    return BarValueLabelLayout(
        lines = measured.lines,
        width = measured.width,
        height = measured.height,
    )
}

/**
 * The lines a bar's value label is drawn as: the number with its unit split onto a
 * second line, or the whole (trimmed) text as one line. Null when there is nothing
 * worth drawing at all.
 */
internal fun barLabelLines(text: String): List<String>? {
    if (text.isBlank()) return null
    return splitBarValueLabel(text) ?: listOf(text.trim())
}

/**
 * What a set of measured label lines amounts to: the widest line, and the lines
 * stacked with a gap between them.
 */
internal data class BarLabelMeasurement<T>(
    val lines: List<T>,
    val width: Int,
    val height: Int,
)

/**
 * The pure half of the bar-label decision, parameterized by [measure] so a JVM
 * test can stand in a deterministic measurer for Compose text measurement: measure
 * every line, DROP the label when any line overruns [maxWidth], otherwise report
 * the widest line as the width and the stacked line heights (plus [lineGap]
 * between lines) as the height.
 */
internal fun <T> measureBarLabelLines(
    lines: List<String>,
    maxWidth: Float,
    lineGap: Int,
    measure: (String) -> T,
    lineWidth: (T) -> Int,
    lineHeight: (T) -> Int,
): BarLabelMeasurement<T>? {
    val layouts = lines.map(measure)
    if (layouts.any { lineWidth(it) > maxWidth }) return null

    return BarLabelMeasurement(
        lines = layouts,
        width = layouts.maxOfOrNull(lineWidth) ?: 0,
        height = layouts.sumOf(lineHeight) + lineGap * (layouts.size - 1).coerceAtLeast(0),
    )
}

internal fun splitBarValueLabel(text: String): List<String>? {
    val trimmed = text.trim()
    val splitIndex = trimmed.lastIndexOf(' ')
    if (splitIndex <= 0 || splitIndex >= trimmed.lastIndex) return null
    return listOf(
        trimmed.substring(0, splitIndex).withoutIntegerGroupingSeparators(),
        trimmed.substring(splitIndex + 1),
    )
}

private fun String.withoutIntegerGroupingSeparators(): String {
    val signedValue = trim()
    val sign = signedValue.firstOrNull()?.takeIf { it == '-' || it == '+' }?.toString().orEmpty()
    val unsignedValue = if (sign.isNotEmpty()) signedValue.drop(1) else signedValue
    if (unsignedValue.isEmpty()) return signedValue
    if (unsignedValue.any { !it.isDigit() && !it.isIntegerGroupingSeparator() }) return signedValue

    val groups = unsignedValue.split(*IntegerGroupingSeparators)
    if (groups.size <= 1 || groups.any { it.isEmpty() }) return signedValue
    if (groups.drop(1).any { it.length != 3 }) return signedValue

    return sign + groups.joinToString(separator = "")
}

private fun Char.isIntegerGroupingSeparator(): Boolean =
    this == ',' || this == '.' || this == '\'' || isWhitespace() || this == '\u00A0'

private val IntegerGroupingSeparators = charArrayOf(',', '.', '\'', ' ', '\u00A0')

private fun DrawScope.drawBarValueLabel(
    textLayout: BarValueLabelLayout,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    val verticalPadding = 2.dp.toPx()
    if (height < textLayout.height + verticalPadding * 2f) return

    var lineTop = top + (height - textLayout.height) / 2f
    textLayout.lines.forEach { line ->
        drawText(
            textLayoutResult = line,
            topLeft = Offset(
                x = left + (width - line.size.width) / 2f,
                y = lineTop,
            ),
        )
        lineTop += line.size.height + 1.dp.toPx()
    }
}

@Composable
fun PeriodChartXAxis(
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    /** The slice of the period on show, when the chart above has been pinched. */
    viewport: ChartViewport = ChartViewport.Full,
) {
    val label: @Composable (Int) -> Unit = { index ->
        if (isPeriodChartLabelVisible(index, dates.lastIndex, selectedRange)) {
            Text(
                text = periodChartLabel(
                    date = dates[index],
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

    if (!viewport.isZoomed) {
        // Unzoomed, the slots ARE the row: even weights, exactly as before.
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            dates.indices.forEach { index ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    label(index)
                }
            }
        }
        return
    }

    // Zoomed, a date has to sit over ITS OWN bar. Evenly spacing whichever labels survive
    // would drift them off the bars they name — and a bar chart whose dates belong to the
    // wrong bars is worse than one that does not zoom.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(PeriodAxisRowHeight)
            .clipToBounds(),
    ) {
        val rowWidth = maxWidth
        val slotWidth = rowWidth / (dates.size * viewport.span)
        dates.indices.forEach { index ->
            val left = rowWidth * periodSlotLeftFraction(index, dates.size, viewport)
            if (!isPeriodSlotVisible(left.value, slotWidth.value, rowWidth.value)) return@forEach
            Box(
                modifier = Modifier
                    .offset(x = left)
                    .width(slotWidth),
                contentAlignment = Alignment.TopCenter,
            ) {
                label(index)
            }
        }
    }
}

private val PeriodAxisRowHeight = 16.dp

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
    TimeRange.DAY -> dateTimeFormatterProvider.chartDay().format(date)
    TimeRange.WEEK,
    TimeRange.MONTH -> dateTimeFormatterProvider.chartDayOfMonth().format(date)
    TimeRange.YEAR -> dateTimeFormatterProvider.chartMonth().format(date)
}

private fun List<Double>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else average()
