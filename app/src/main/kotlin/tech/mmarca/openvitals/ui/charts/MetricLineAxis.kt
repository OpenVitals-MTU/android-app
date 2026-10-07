package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

/** How a line chart fits its y axis to the data. */
sealed interface LineAxisRange {
    /** The axis bounds for data spanning [minValue]..[maxValue]. */
    fun resolve(minValue: Double, maxValue: Double): Pair<Double, Double>

    /** The data's own span, padded a little either side. The default. */
    data object Padded : LineAxisRange {
        override fun resolve(minValue: Double, maxValue: Double): Pair<Double, Double> {
            // The line charts' own padding rule, kept so every existing axis stays put.
            val range = maxValue - minValue
            val padding = if (range == 0.0) max(abs(maxValue) * 0.05, 1.0) else range * AxisPadding
            return (minValue - padding) to (maxValue + padding)
        }
    }

    /**
     * Symmetric about zero, for a signed variation: above the middle is
     * more, below it less, and the middle label is exactly 0. A flat series
     * at zero still gets a visible axis.
     */
    data object ZeroCentred : LineAxisRange {
        override fun resolve(minValue: Double, maxValue: Double): Pair<Double, Double> {
            val half = max(abs(minValue), abs(maxValue)).coerceAtLeast(MinimumHalfSpan) * (1.0 + AxisPadding)
            return -half to half
        }
    }
}

private const val AxisPadding = 0.08
private const val MinimumHalfSpan = 0.1

/** A point's place in the plot: x and y in 0..1, y measured from the top. */
internal fun metricLinePointFraction(
    point: MetricLinePoint,
    selectedRange: TimeRange,
    period: DatePeriod,
    dayStart: Instant,
    dayDurationMillis: Long,
    periodDayCount: Int,
    minValue: Double,
    maxValue: Double,
): Offset {
    // Guarded against a flat axis only. A floor of one unit squashed every
    // series narrower than that (a tenth of a degree, a mmol/L) under its labels.
    val range = (maxValue - minValue).coerceAtLeast(1e-9)
    val xFraction = if (selectedRange == TimeRange.DAY) {
        val pointTime = point.time ?: point.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val elapsed = Duration.between(dayStart, pointTime).toMillis().coerceIn(0L, dayDurationMillis)
        elapsed.toFloat() / dayDurationMillis
    } else {
        val daysFromStart = ChronoUnit.DAYS.between(period.start, point.date)
            .coerceIn(0L, (periodDayCount - 1).toLong())
        (daysFromStart + 0.5f) / periodDayCount
    }
    return Offset(
        x = xFraction,
        y = 1f - ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f),
    )
}

/** Dashed reference lines under the data: a threshold, a goal, zero. [yFor] maps a value to its y pixel. */
internal fun DrawScope.drawChartGuides(guides: List<ChartGuideLine>, yFor: (Double) -> Float) {
    if (guides.isEmpty()) return
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
    for (guide in guides) {
        val y = yFor(guide.value)
        drawLine(
            color = guide.color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = dash,
        )
    }
}

internal fun DrawScope.drawLineSelectedDateHighlight(
    selectedRange: TimeRange,
    selectedDate: LocalDate?,
    period: DatePeriod,
    axisDates: List<LocalDate>,
    color: Color,
    viewport: ChartViewport,
) {
    if (!selectedRange.supportsChartDaySelection() || selectedDate == null || selectedDate !in period.start..period.end) {
        return
    }
    val index = axisDates.indexOf(selectedDate)
    if (index < 0 || axisDates.isEmpty()) return

    // Through the viewport, so the highlight stays on its day when pinched.
    val left = size.width * viewport.visibleFraction(index.toFloat() / axisDates.size)
    val slotWidth = size.width / (axisDates.size * viewport.span)
    drawRect(
        color = color,
        topLeft = Offset(left, 0f),
        size = Size(slotWidth, size.height),
    )
}

internal fun datesInPeriod(period: DatePeriod): List<LocalDate> =
    generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.toList()
