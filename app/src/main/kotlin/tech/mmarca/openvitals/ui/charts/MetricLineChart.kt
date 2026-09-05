package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.stats.timeBucketedAverageOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

data class MetricLinePoint(
    val date: LocalDate,
    val value: Double,
    val time: Instant? = null,
)

data class MetricLineSeries(
    val points: List<MetricLinePoint>,
    val color: Color,
    val label: String? = null,
)

data class MetricLinePlotPoint(
    val xFraction: Float,
    val value: Double,
    /** A scaffold point (midnight anchor, trailing hold), not a sample. Never gets a dot. */
    val synthetic: Boolean = false,
)

/** A reference line the data is measured against: a threshold, a goal. Drawn dashed. */
data class ChartGuideLine(val value: Double, val color: Color)

/** A tick along the bottom edge: something happened at this moment. */
data class ChartMarker(val xFraction: Float, val color: Color)

/** One span of a min/max band, drawn as a ribbon behind the average line. */
data class ChartBandSpan(val xFraction: Float, val low: Double, val high: Double)

/** Above this many visible points, per-sample dots are suppressed. */
private const val MaxDotPoints = 120

/**
 * A line on a normalized x axis, for unevenly spaced series. [viewport] is
 * the slice on show. [scrubLabel] turns a sample into the two tooltip lines;
 * null leaves the chart inert.
 */
@Composable
fun MetricLinePlot(
    points: List<MetricLinePlotPoint>,
    minValue: Double,
    maxValue: Double,
    accentColor: Color,
    chartHeight: Dp,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    canvasModifier: Modifier = Modifier,
    lineStrokeWidth: Dp = 2.dp,
    pointRadius: Dp = 3.5.dp,
    drawPoints: Boolean = true,
    guides: List<ChartGuideLine> = emptyList(),
    markers: List<ChartMarker> = emptyList(),
    band: List<ChartBandSpan> = emptyList(),
    viewport: ChartViewport = ChartViewport.Full,
    multiTouch: Boolean = false,
    scrubLabel: ((MetricLinePlotPoint) -> Pair<String, String?>)? = null,
) {
    // A flat series would divide by zero when normalizing.
    val span = maxValue - minValue
    val safeMax = if (abs(span) < 1e-9) minValue + 1.0 else maxValue
    val safeSpan = safeMax - minValue

    // Cached geometry, keyed on everything but reveal progress, so the reveal
    // builds the path once.
    val cache = remember { PlotGeometryCache() }
    val fill = remember(accentColor) { ChartTokens.areaFill(accentColor) }

    // Snapping targets: the samples on show, in plot space.
    val targets = if (scrubLabel == null || points.size < 2) {
        emptyList()
    } else {
        points.mapNotNull { point ->
            val visible = viewport.visibleFraction(point.xFraction)
            if (visible < 0f || visible > 1f) return@mapNotNull null
            val (primary, secondary) = scrubLabel(point)
            ScrubTarget(
                xFraction = visible,
                yFraction = ((point.value - minValue) / safeSpan).toFloat().coerceIn(0f, 1f),
                primary = primary,
                secondary = secondary,
            )
        }
    }

    YAxisChartSlot(
        // chartYAxisLabels raises precision when the compact formatter would
        // print the same label twice.
        labels = chartYAxisLabels(
            minValue = minValue,
            maxValue = safeMax,
            valueFormatter = valueFormatter,
        ),
        chartHeight = chartHeight,
        modifier = modifier,
    ) {
        ChartScrubber(
            targets = targets,
            accentColor = accentColor,
            multiTouch = multiTouch,
        ) {
            ChartReveal { progress ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(canvasModifier),
                ) {
                    drawMetricLinePlot(
                        cache = cache,
                        points = points,
                        minValue = minValue,
                        maxValue = safeMax,
                        accentColor = accentColor,
                        fill = fill,
                        guides = guides,
                        markers = markers,
                        band = band,
                        strokeWidth = lineStrokeWidth.toPx(),
                        pointRadius = if (drawPoints) pointRadius.toPx() else 0f,
                        progress = progress,
                        viewport = viewport,
                    )
                }
            }
        }
    }
}

/**
 * A raw day series as a zoomable, scrubbable plot with its hour row. When
 * the user chose a bucket width the line becomes a per-bucket average with a
 * min/max band. Only raw series aggregate; a running total is not averaged.
 */
@Composable
fun <T> DayTimelineLinePlot(
    samples: List<T>,
    dayStart: Instant,
    dayEnd: Instant,
    minValue: Double,
    maxValue: Double,
    accentColor: Color,
    valueFormatter: (Double) -> String,
    timeLabel: (Instant) -> String,
    time: (T) -> Instant,
    value: (T) -> Double,
    modifier: Modifier = Modifier,
    chartHeight: Dp = ChartTokens.heightDay,
    lineStrokeWidth: Dp = 2.dp,
    pointRadius: Dp = 3.5.dp,
    drawPoints: Boolean = true,
    zoomKey: Any? = null,
) {
    val aggregationMode = LocalChartAggregationMode.current
    val bucketMinutes = aggregationMode.bucketMinutes

    // Built outside the zoom content: the points do not depend on the viewport,
    // and a fresh list per pinch frame would defeat the geometry cache.
    val (points, band) = remember(samples, dayStart, dayEnd, aggregationMode) {
        if (bucketMinutes != null) {
            val buckets = bucketedSeries(
                samples = samples,
                bucketMinutes = bucketMinutes,
                dayStart = dayStart,
                time = time,
                value = value,
            )
            buckets.map { bucket ->
                MetricLinePlotPoint(axisFractionOf(dayStart, dayEnd, bucket.time), bucket.average)
            } to buckets.map { bucket ->
                ChartBandSpan(axisFractionOf(dayStart, dayEnd, bucket.time), bucket.min, bucket.max)
            }
        } else {
            rawDayPlotPoints(
                samples = samples,
                dayStart = dayStart,
                dayEnd = dayEnd,
                time = time,
                value = value,
            ) to emptyList()
        }
    }
    val dayMillis = Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)

    // Plot and hour row share the one viewport.
    ChartZoom(zoomKey, samples, aggregationMode, modifier = modifier) { zoom ->
        Column {
            MetricLinePlot(
                points = points,
                minValue = minValue,
                maxValue = maxValue,
                accentColor = accentColor,
                chartHeight = chartHeight,
                valueFormatter = valueFormatter,
                lineStrokeWidth = lineStrokeWidth,
                pointRadius = pointRadius,
                // Dots on averaged points read as false precision.
                drawPoints = if (bucketMinutes != null) false else drawPoints,
                band = band,
                viewport = zoom.viewport,
                multiTouch = zoom.multiTouch,
                scrubLabel = { point ->
                    val at = dayStart.plusMillis(
                        (point.xFraction.coerceIn(0f, 1f) * dayMillis).toLong(),
                    )
                    valueFormatter(point.value) to timeLabel(at)
                },
            )
            Spacer(Modifier.height(8.dp))
            ChartXAxisWithYAxis {
                DayAxisLabels(viewport = zoom.viewport)
            }
        }
    }
}

@Composable
fun MetricLineChart(
    title: String,
    points: List<MetricLinePoint>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summaryText: String,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    seriesLabel: String? = title,
    averagePeriodPoints: Boolean = true,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val chartPoints = if (averagePeriodPoints && selectedRange != TimeRange.DAY) {
        dailyAverageLinePoints(points)
    } else {
        points
    }

    MetricLineChart(
        title = title,
        series = listOf(MetricLineSeries(chartPoints, accentColor, seriesLabel)),
        selectedRange = selectedRange,
        period = period,
        accentColor = accentColor,
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = valueFormatter,
    )
}

@Composable
fun <T> MetricLineChart(
    title: String,
    entries: List<T>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summaryText: String,
    time: (T) -> Instant,
    value: (T) -> Double,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    seriesLabel: String? = title,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    MetricLineChart(
        title = title,
        points = entries.mapLinePoints(time = time, value = value),
        selectedRange = selectedRange,
        period = period,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        accentColor = accentColor,
        summaryText = summaryText,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        seriesLabel = seriesLabel,
        valueFormatter = valueFormatter,
    )
}

@Composable
fun MetricLineChart(
    title: String,
    series: List<MetricLineSeries>,
    selectedRange: TimeRange,
    period: DatePeriod,
    accentColor: Color,
    summaryText: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    valueFormatter: (Double) -> String = ::formatCompactAxisValue,
) {
    val visibleSeries = series
        .map { chartSeries ->
            chartSeries.copy(points = chartSeries.points.filter { point ->
                point.value.isFinite() && !point.date.isBefore(period.start) && !point.date.isAfter(period.end)
            })
        }
        .filter { it.points.isNotEmpty() }
    val allPoints = visibleSeries.flatMap { it.points }
    if (allPoints.isEmpty()) return
    if (selectedRange == TimeRange.DAY && allPoints.mapNotNull { it.time }.distinct().size <= 1) return

    val allValues = allPoints.map { it.value }
    val minValue = allValues.minOrNull() ?: return
    val maxValue = allValues.maxOrNull() ?: return
    val (axisMin, axisMax) = paddedLineAxisRange(minValue, maxValue)
    val axisDates = remember(period) { datesInPeriod(period) }
    // A year of days gives 365 slots for twelve month names. Borrow the bar
    // chart's twelve buckets instead.
    val labelDates = remember(axisDates, selectedRange, period) {
        if (selectedRange == TimeRange.YEAR && axisDates.size > MonthsInYear) {
            monthStartsIn(period)
        } else {
            axisDates
        }
    }
    val chartHeight = ChartTokens.heightLine
    val zone = ZoneId.systemDefault()
    val dayStart = remember(period, zone) { period.start.atStartOfDay(zone).toInstant() }
    val dayEnd = remember(period, zone) { period.start.plusDays(1).atStartOfDay(zone).toInstant() }
    val dayDurationMillis = remember(dayStart, dayEnd) {
        Duration.between(dayStart, dayEnd).toMillis().coerceAtLeast(1L)
    }
    val periodDayCount = axisDates.size.coerceAtLeast(1)
    val gridColor = ChartTokens.grid(accentColor)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)

    // The Canvas publishes nothing to a screen reader; this one line does.
    val semanticSummary = chartSemanticSummary(title = title, summaryText = summaryText)

    OpenVitalsCard(
        modifier = modifier.chartSemantics(semanticSummary),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            // Keyed on the data identity so a zoom does not carry over to new data.
            // No scrubber: it broke the pinch. The tap-to-select-day is untouched.
            ChartZoom(selectedRange, period.start, period.end) { zoom ->
                val viewport = zoom.viewport
                val currentViewport by rememberUpdatedState(viewport)
                val chartTapModifier = if (
                    selectedRange.supportsChartDaySelection() &&
                    onDateSelected != null &&
                    axisDates.isNotEmpty()
                ) {
                    Modifier.pointerInput(axisDates, onDateSelected) {
                        detectTapGestures { offset ->
                            // Map the tap through the viewport so a zoomed chart
                            // selects the date under the finger.
                            val visible = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val index = (currentViewport.dataFraction(visible) * axisDates.size)
                                .toInt()
                                .coerceIn(0, axisDates.lastIndex)
                            onDateSelected(axisDates[index])
                        }
                    }
                } else {
                    Modifier
                }

                Column {
                    YAxisChart(
                        labels = chartYAxisLabels(
                            minValue = axisMin,
                            maxValue = axisMax,
                            valueFormatter = valueFormatter,
                        ),
                        chartHeight = chartHeight,
                        canvasModifier = chartTapModifier,
                    ) {
                        drawYAxisGuides(
                            gridColor = gridColor,
                            axisColor = axisColor,
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLineSelectedDateHighlight(
                            selectedRange = selectedRange,
                            selectedDate = selectedDate,
                            period = period,
                            axisDates = axisDates,
                            color = accentColor.copy(alpha = 0.16f),
                            viewport = viewport,
                        )
                        // Zoomed, the line runs past the plot edges: clip, never clamp.
                        val drawSeries: DrawScope.() -> Unit = {
                            visibleSeries.forEach { lineSeries ->
                                drawMetricLineSeries(
                                    points = lineSeries.points,
                                    selectedRange = selectedRange,
                                    period = period,
                                    dayStart = dayStart,
                                    dayDurationMillis = dayDurationMillis,
                                    periodDayCount = periodDayCount,
                                    minValue = axisMin,
                                    maxValue = axisMax,
                                    color = lineSeries.color,
                                    viewport = viewport,
                                )
                            }
                        }
                        if (viewport.isZoomed) clipRect { drawSeries() } else drawSeries()
                    }
                    Spacer(Modifier.height(8.dp))
                    if (selectedRange == TimeRange.DAY) {
                        // Inside the zoom, sharing its viewport.
                        ChartXAxisWithYAxis {
                            DayAxisLabels(viewport = viewport)
                        }
                    } else {
                        // Same viewport as the plot, so dates stay over their slots.
                        ChartXAxisWithYAxis {
                            PeriodChartXAxis(
                                dates = labelDates,
                                selectedRange = selectedRange,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                viewport = viewport,
                            )
                        }
                    }
                }
            }
            if (visibleSeries.size > 1) {
                Spacer(Modifier.height(8.dp))
                MetricLineLegend(visibleSeries)
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

fun <T> List<T>.mapLinePoints(
    time: (T) -> Instant,
    value: (T) -> Double,
): List<MetricLinePoint> =
    map { entry ->
        val entryTime = time(entry)
        MetricLinePoint(
            date = entryTime.atZone(ZoneId.systemDefault()).toLocalDate(),
            time = entryTime,
            value = value(entry),
        )
    }.sortedBy { it.time }

/**
 * One point per date, minute-bucketed so continuous overnight monitoring does
 * not outvote the day's spot checks. A point without a time sits at midnight.
 */
fun dailyAverageLinePoints(points: List<MetricLinePoint>): List<MetricLinePoint> {
    val zone = ZoneId.systemDefault()
    return points
        .groupBy { it.date }
        .mapNotNull { (date, dayPoints) ->
            val dayStart = date.atStartOfDay(zone).toInstant()
            val value = dayPoints.timeBucketedAverageOrNull(
                time = { it.time ?: dayStart },
                value = { it.value },
            ) ?: return@mapNotNull null
            MetricLinePoint(date = date, value = value)
        }
        .sortedBy { it.date }
}

/**
 * The geometry of a plotted line. Depends only on points, size, viewport and
 * range, never on reveal progress, so it is computed once and reused.
 */
internal class PlotGeometry(
    val offsets: List<Offset>,
    val path: Path,
    val pathMeasure: PathMeasure,
    val end: Offset,
)

/**
 * A single-slot memo for [PlotGeometry]. Points are compared by identity:
 * callers keep the list stable, and a fresh list means new data.
 */
internal class PlotGeometryCache {
    private var cached: PlotGeometry? = null
    private var points: List<MetricLinePlotPoint>? = null
    private var size: Size? = null
    private var viewport: ChartViewport? = null
    private var minValue = Double.NaN
    private var maxValue = Double.NaN

    fun build(
        points: List<MetricLinePlotPoint>,
        size: Size,
        viewport: ChartViewport,
        minValue: Double,
        maxValue: Double,
        offsetFor: (MetricLinePlotPoint) -> Offset,
    ): PlotGeometry {
        val hit = cached
        if (hit != null &&
            this.points === points &&
            this.size == size &&
            this.viewport == viewport &&
            this.minValue == minValue &&
            this.maxValue == maxValue
        ) {
            return hit
        }

        // Cull to the visible window, then decimate to about one vertex per pixel.
        val visible = cullPlotPoints(points, viewport)
        val offsets = decimateOffsets(visible.map(offsetFor), ceil(size.width).toInt())
        val path = smoothPath(offsets)
        val pathMeasure = PathMeasure().apply { setPath(path, false) }
        val end = when {
            offsets.isEmpty() -> Offset.Zero
            pathMeasure.length > 0f ->
                pathMeasure.getPosition(pathMeasure.length).takeIf { it.isSpecified } ?: offsets.last()
            else -> offsets.last()
        }

        val geometry = PlotGeometry(offsets, path, pathMeasure, end)
        cached = geometry
        this.points = points
        this.size = size
        this.viewport = viewport
        this.minValue = minValue
        this.maxValue = maxValue
        return geometry
    }
}

/**
 * The points inside the viewport plus one past each edge, sorted by
 * xFraction. A window inside a gap between samples yields nothing.
 */
internal fun cullPlotPoints(
    points: List<MetricLinePlotPoint>,
    viewport: ChartViewport,
): List<MetricLinePlotPoint> {
    if (!viewport.isZoomed) return points
    val n = points.size
    var lo = 0
    while (lo < n && viewport.visibleFraction(points[lo].xFraction) < 0f) lo++
    var hi = n - 1
    while (hi >= 0 && viewport.visibleFraction(points[hi].xFraction) > 1f) hi--
    if (lo > hi) return emptyList()
    if (lo > 0) lo--
    if (hi < n - 1) hi++
    if (lo == 0 && hi == n - 1) return points
    return points.subList(lo, hi + 1)
}

private fun DrawScope.drawMetricLinePlot(
    cache: PlotGeometryCache,
    points: List<MetricLinePlotPoint>,
    minValue: Double,
    maxValue: Double,
    accentColor: Color,
    fill: Brush,
    guides: List<ChartGuideLine>,
    markers: List<ChartMarker>,
    band: List<ChartBandSpan>,
    strokeWidth: Float,
    pointRadius: Float,
    progress: Float,
    viewport: ChartViewport,
) {
    if (points.size < 2 || size.width <= 0f || size.height <= 0f) return

    val range = maxValue - minValue
    fun yFor(value: Double): Float =
        size.height * (1f - ((value - minValue) / range).toFloat().coerceIn(0f, 1f))

    fun offsetFor(point: MetricLinePlotPoint): Offset =
        Offset(viewport.visibleFraction(point.xFraction) * size.width, yFor(point.value))

    val body: DrawScope.() -> Unit = {
        // The baseline, under everything else.
        drawLine(
            color = ChartTokens.baseline(accentColor),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        // Guides first, so the data is drawn on them.
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        guides.forEach { guide ->
            val y = yFor(guide.value)
            drawLine(
                color = guide.color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = dash,
            )
        }

        val geometry = cache.build(
            points = points,
            size = size,
            viewport = viewport,
            minValue = minValue,
            maxValue = maxValue,
            offsetFor = ::offsetFor,
        )
        val offsets = geometry.offsets
        // A deep zoom on a gap has nothing to draw.
        if (offsets.size >= 2 && progress > 0f) {
            // The line draws itself in by length from the cached measure.
            val revealed = progress >= 1f
            val path: Path
            val drawnEnd: Offset
            if (revealed) {
                path = geometry.path
                drawnEnd = geometry.end
            } else {
                val partial = Path()
                val stop = geometry.pathMeasure.length * progress.coerceIn(0f, 1f)
                geometry.pathMeasure.getSegment(0f, stop, partial, true)
                path = partial
                drawnEnd = geometry.pathMeasure.getPosition(stop)
                    .takeIf { it.isSpecified } ?: offsets.last()
            }

            // Aggregated: the ribbon replaces the under-line gradient.
            if (band.size >= 2) {
                drawPlotBand(band, viewport, minValue, maxValue, accentColor)
            } else {
                // Close the fill under the last drawn point, not the plot edge, or
                // today's fill sweeps across hours that have not happened.
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(drawnEnd.x, size.height)
                    lineTo(offsets.first().x, size.height)
                    close()
                }
                // A gradient reads as the line's own space; a solid wash reads as a second object.
                drawPath(fillPath, fill)
            }
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Moments on the baseline.
            markers.forEach { marker ->
                drawCircle(
                    color = marker.color,
                    radius = 3.dp.toPx(),
                    center = Offset(
                        viewport.visibleFraction(marker.xFraction) * size.width,
                        size.height - 3.dp.toPx(),
                    ),
                )
            }

            // A dot per sample, never on a scaffold point, only where the line has
            // reached. Capped at [MaxDotPoints].
            if (pointRadius > 0f && points.size <= MaxDotPoints) {
                points.forEach { point ->
                    if (point.synthetic) return@forEach
                    val offset = offsetFor(point)
                    if (offset.x <= drawnEnd.x + 0.5f) {
                        drawCircle(color = accentColor, radius = pointRadius, center = offset)
                    }
                }
            }
        }
    }

    // Painters clip, never clamp: a clamped point would draw a value nobody recorded.
    if (viewport.isZoomed) clipRect { body() } else body()
}

/** The min/max ribbon: across the top by the maxima, back by the minima. */
private fun DrawScope.drawPlotBand(
    band: List<ChartBandSpan>,
    viewport: ChartViewport,
    minValue: Double,
    maxValue: Double,
    accentColor: Color,
) {
    fun x(fraction: Float): Float = viewport.visibleFraction(fraction) * size.width
    fun y(value: Double): Float =
        size.height * (1f - ((value - minValue) / (maxValue - minValue)).toFloat().coerceIn(0f, 1f))

    val ribbon = Path().apply {
        moveTo(x(band.first().xFraction), y(band.first().high))
        for (i in 1 until band.size) {
            lineTo(x(band[i].xFraction), y(band[i].high))
        }
        for (i in band.indices.reversed()) {
            lineTo(x(band[i].xFraction), y(band[i].low))
        }
        close()
    }
    drawPath(ribbon, accentColor.copy(alpha = 0.16f))
}

@Composable
private fun MetricLineLegend(series: List<MetricLineSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        series.forEach { item ->
            item.label?.let { label ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(8.dp)
                            .background(item.color, shape = CircleShape),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMetricLineSeries(
    points: List<MetricLinePoint>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dayStart: Instant,
    dayDurationMillis: Long,
    periodDayCount: Int,
    minValue: Double,
    maxValue: Double,
    color: Color,
    viewport: ChartViewport,
) {
    val range = (maxValue - minValue).coerceAtLeast(1.0)
    val positioned = points.map { point ->
        val xFraction = if (selectedRange == TimeRange.DAY) {
            val pointTime = point.time ?: point.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val elapsed = Duration.between(dayStart, pointTime).toMillis().coerceIn(0L, dayDurationMillis)
            elapsed.toFloat() / dayDurationMillis
        } else {
            val daysFromStart = ChronoUnit.DAYS.between(period.start, point.date)
                .coerceIn(0L, (periodDayCount - 1).toLong())
            (daysFromStart + 0.5f) / periodDayCount
        }
        Offset(
            // Full viewport is a no-op.
            x = size.width * viewport.visibleFraction(xFraction),
            y = size.height * (1f - ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)),
        )
    }

    // Cull first, then decimate. Culling is what lets a zoom restore detail.
    val drawn = visibleDecimatedOffsets(positioned, size.width)

    drawPath(
        path = smoothPath(drawn),
        color = color,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    if (drawn.size <= MaxDotPoints) {
        drawn.forEach { point ->
            drawCircle(color = color, radius = 3.5.dp.toPx(), center = point)
        }
    }
}

/**
 * The visible slice of [positioned], plus one point past each edge, decimated
 * to about one vertex per pixel. A window between two points keeps the pair.
 */
internal fun visibleDecimatedOffsets(positioned: List<Offset>, width: Float): List<Offset> {
    val n = positioned.size
    if (n < 2) return positioned

    var firstIn = 0
    while (firstIn < n && positioned[firstIn].x < 0f) firstIn++
    var lastIn = n - 1
    while (lastIn >= 0 && positioned[lastIn].x > width) lastIn--

    var lo: Int
    var hi: Int
    if (firstIn > lastIn) {
        lo = lastIn.coerceIn(0, n - 1)
        hi = firstIn.coerceIn(0, n - 1)
        if (lo > hi) {
            val swap = lo
            lo = hi
            hi = swap
        }
    } else {
        lo = if (firstIn > 0) firstIn - 1 else 0
        hi = if (lastIn < n - 1) lastIn + 1 else n - 1
    }

    val visible = if (lo == 0 && hi == n - 1) positioned else positioned.subList(lo, hi + 1)
    return decimateOffsets(visible, ceil(width).toInt())
}

private fun DrawScope.drawLineSelectedDateHighlight(
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

private fun paddedLineAxisRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    // The line charts' own padding rule, kept so every existing axis stays put.
    val range = maxValue - minValue
    val padding = if (range == 0.0) {
        max(abs(maxValue) * 0.05, 1.0)
    } else {
        range * 0.08
    }
    return (minValue - padding) to (maxValue + padding)
}

private fun datesInPeriod(period: DatePeriod): List<LocalDate> =
    generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.toList()
