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
    /**
     * A point the line is scaffolded with rather than a recorded sample — the
     * midnight anchor and the trailing hold of a cumulative day line. It shapes
     * the line but never gets a dot: a dot says "an entry happened here".
     */
    val synthetic: Boolean = false,
)

/**
 * A horizontal line the data is measured AGAINST, rather than data itself — the
 * caffeine sleep threshold, a goal, a clinical limit. Drawn dashed, because a solid
 * line of the same weight reads as another series.
 */
data class ChartGuideLine(val value: Double, val color: Color)

/**
 * A tick along the bottom edge: something happened at this moment. The caffeine
 * card marks each drink, so the sawtooth in the curve can be read against the act
 * that caused it.
 */
data class ChartMarker(val xFraction: Float, val color: Color)

/**
 * One span of a min/max band: at [xFraction], the data ranged from [low] to
 * [high]. Drawn as a filled ribbon behind the (average) line in the aggregated
 * chart view.
 */
data class ChartBandSpan(val xFraction: Float, val low: Double, val high: Double)

/**
 * Above this many visible points, per-sample dots are suppressed: at that density
 * they overlap into an illegible band and cost one `drawCircle` each, every frame.
 * The line itself still carries every point.
 */
private const val MaxDotPoints = 120

/**
 * A line drawn against a normalized x axis, for series whose points are not evenly
 * spaced in time (the intraday and session charts).
 *
 * [viewport] is the slice of the axis on show — [ChartViewport.Full] unless a
 * [ChartZoom] above has been pinched; thread [ChartZoomState.viewport] and
 * [ChartZoomState.multiTouch] in together. [scrubLabel] turns a sample into the two
 * lines of a scrub tooltip: the VALUE, and what it is a value OF (usually the time
 * it was taken). Null leaves the chart inert — which is what a chart with nothing
 * to say about a single point should be.
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
    // Guard a flat series: a zero span would divide by zero when normalizing.
    val span = maxValue - minValue
    val safeMax = if (abs(span) < 1e-9) minValue + 1.0 else maxValue
    val safeSpan = safeMax - minValue

    // Survives the per-frame recompositions of the entry animation and of a pinch:
    // the cache — keyed by everything the geometry depends on EXCEPT reveal
    // progress — turns the 550ms reveal from dozens of full path rebuilds into one
    // build plus a cheap per-frame path segment extraction.
    val cache = remember { PlotGeometryCache() }
    val fill = remember(accentColor) { ChartTokens.areaFill(accentColor) }

    // Snapping targets: only the samples inside the slice on show, in PLOT space.
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
        // `chartYAxisLabels`, not three hand-rolled calls to the formatter: the
        // compact formatter rounds anything over 10 to a whole number, so a narrow
        // range collides — a weight chart across 74.06–74.64 kg would show "74"
        // twice at different heights. `chartYAxisLabels` notices the collision and
        // steps up to a precision that separates them.
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
 * A raw (non-cumulative) day series as a zoomable, scrubbable plot with its hour
 * row — the shared body of the heart and body intraday cards.
 *
 * This is also where the "Aggregate charts" setting lands: when the user has
 * chosen a bucket width, the line becomes a per-bucket average with a min/max
 * band behind it instead of the raw polyline. Aggregation applies ONLY to raw
 * series — a running total is not something you average — which is why the
 * cumulative day card does not go through here.
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

    // Built once here, not inside the zoom content: the plotted points do not
    // depend on the viewport (the plot applies it), so recomputing them per pinch
    // frame would churn a fresh list and defeat the plot's geometry cache.
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

    // The plot and its hour row are BOTH inside the zoom, sharing the one
    // viewport — a chart whose hours disagreed with its line would be worse than
    // one that did not zoom at all.
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
                // Dots on averaged points read as false precision; the band
                // already shows the spread.
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

    // The chart itself is a Canvas and publishes nothing, so without this a
    // screen reader goes from the screen title straight past the chart as if it
    // were not there. One line carrying what a glance carries.
    val semanticSummary = chartSemanticSummary(title = title, summaryText = summaryText)

    OpenVitalsCard(
        modifier = modifier.chartSemantics(semanticSummary),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            // Keyed on the chart's data identity so a zoom does not carry over when
            // the data underneath changes: switching range or period rebuilds a
            // fresh, unzoomed viewport rather than stretching the old slice onto
            // the new period.
            //
            // No scrubber here, deliberately: a scrub layer on the period charts
            // broke the pinch (the drag detector claimed the first finger of the
            // pinch) and was reverted in the Flutter app. The tap-to-select-day
            // interaction below is untouched — the zoom claims nothing
            // single-finger.
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
                            // Map the tap back through the viewport so a zoomed
                            // chart selects the date actually under the finger,
                            // not the unzoomed slot.
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
                        // Zoomed, the line runs past the plot edges; clip so it
                        // ends at the plot rather than spilling across the card.
                        // Clip, never clamp: a point scrolled off the edge keeps
                        // its real position so the line leaves at its true angle.
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
                        // Inside the zoom, sharing its viewport: an hour row that
                        // disagreed with the line above it would be worse than one
                        // that did not zoom at all.
                        ChartXAxisWithYAxis {
                            DayAxisLabels(viewport = viewport)
                        }
                    } else {
                        // Same viewport as the plot: zoomed, each date is positioned
                        // over its own slot rather than evenly spaced, so the row
                        // never drifts off the days it names.
                        ChartXAxisWithYAxis {
                            PeriodChartXAxis(
                                dates = axisDates,
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

fun dailyAverageLinePoints(points: List<MetricLinePoint>): List<MetricLinePoint> =
    points
        .groupBy { it.date }
        .map { (date, dayPoints) ->
            MetricLinePoint(
                date = date,
                value = dayPoints.map { it.value }.average(),
            )
        }
        .sortedBy { it.date }

/**
 * The geometry of a plotted line: the pixel positions, the smoothed [Path], its
 * measured length (for the reveal's partial extraction) and drawn end. All of it
 * depends only on the points, size, viewport and value range — never on the reveal
 * progress — so it is computed once per those and reused across frames.
 */
internal class PlotGeometry(
    val offsets: List<Offset>,
    val path: Path,
    val pathMeasure: PathMeasure,
    val end: Offset,
)

/**
 * A single-slot memo for [PlotGeometry]. Rebuilds only when an input the geometry
 * actually depends on changes; a progress-only change (every reveal frame) is a
 * hit. Points are compared by IDENTITY — callers keep the list stable across
 * recompositions, and a fresh list is a legitimate "data changed" signal.
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

        // Cull to the visible window (keeping one point past each edge so the line
        // reaches the border), then decimate to roughly one vertex per pixel — no
        // point drawing more cubics than the chart is wide. Both depend on the
        // viewport and size, which is exactly why they live behind this cache and
        // not in the per-frame draw.
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
 * The points inside the viewport, plus one on each side so the line runs to the
 * edges instead of stopping short. Points are sorted ascending by xFraction. The
 * full (unzoomed) viewport shows everything, so there is nothing to cull; a window
 * that falls entirely in a gap between samples yields nothing to draw.
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
        // The line a chart sits ON — under everything else.
        drawLine(
            color = ChartTokens.baseline(accentColor),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        // Guides first, so the data is drawn ON them and not under them.
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
        // The visible window can fall entirely in a gap between samples (deep zoom
        // on a stretch with no readings) — nothing more to draw then.
        if (offsets.size >= 2 && progress > 0f) {
            // The line draws itself in, left to right — a segment of the real
            // curve extracted by LENGTH, so the leading end is the line's own end
            // and not a cut. Extracted from the CACHED measure: no path is
            // re-measured per frame.
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

            // Aggregated view: the min/max ribbon behind the average line takes
            // the place of the under-line gradient — the spread IS the fill, and
            // drawing both would double up.
            if (band.size >= 2) {
                drawPlotBand(band, viewport, minValue, maxValue, accentColor)
            } else {
                // Fill closed under the LAST DRAWN POINT, not at the plot's right
                // edge: closing at the edge shades a region the line never went to
                // — on `today` the trace stops at the current hour, and the fill
                // would sweep a triangle across the hours that have not happened.
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(drawnEnd.x, size.height)
                    lineTo(offsets.first().x, size.height)
                    close()
                }
                // Gradient, not a flat block: a solid wash under a line reads as a
                // second object; a fade reads as the line and the space it
                // encloses.
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

            // Moments, on the baseline: each one is a thing that HAPPENED, sitting
            // under the consequence it had.
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

            // A dot per SAMPLE when requested — never on a synthetic scaffold
            // point, and only the dots the line has actually reached: a dot
            // ahead of the trace is a sample the chart is claiming to have
            // drawn and has not. Capped at [MaxDotPoints]: past that the dots
            // merge into a solid band, so the loop is pure cost.
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

    // Painters CLIP; they do not clamp. A point scrolled off the left edge keeps
    // its real position, so the line running off the plot carries on to where it
    // actually is — clamping it to the edge would bend it into the corner and draw
    // a value nobody ever recorded. The clip keeps that honest line inside the
    // card.
    if (viewport.isZoomed) clipRect { body() } else body()
}

/**
 * The min/max ribbon: across the top by the maxima, back along the bottom by the
 * minima, closed and filled. Buckets are few, so a straight-segment ribbon behind
 * the smoothed average line is cheap and reads cleanly under a translucent fill.
 */
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
            // Full viewport is a no-op (visibleFraction(f) == f), so an unzoomed
            // chart positions exactly as before.
            x = size.width * viewport.visibleFraction(xFraction),
            y = size.height * (1f - ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)),
        )
    }

    // Cull to the visible window first, THEN decimate to ~one vertex per pixel.
    // Culling is what lets a zoom restore detail: the narrower the pinch, the
    // fewer points the window spans, until the decimation is a no-op and every raw
    // point in view is drawn. A sparse period series (a handful of daily points)
    // stays under target and is untouched.
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
 * The visible slice of [positioned] (screen-space offsets, ascending in x), plus
 * one point past each edge so the line reaches the borders, decimated to ~one
 * vertex per pixel. When the window falls between two points (a gap, or a deep
 * zoom), the straddling pair is kept so the line still crosses the plot.
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

    // Through the viewport, so the highlight stays glued to its day when the chart
    // is pinched: the slot widens by exactly the factor the axis stretched.
    val left = size.width * viewport.visibleFraction(index.toFloat() / axisDates.size)
    val slotWidth = size.width / (axisDates.size * viewport.span)
    drawRect(
        color = color,
        topLeft = Offset(left, 0f),
        size = Size(slotWidth, size.height),
    )
}

private fun paddedLineAxisRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    // The line charts' own padding rule (predates ChartRange.padded and differs on
    // a flat series: 5% of the magnitude with a 1.0 minimum, and no zero floor):
    // kept as-is so every existing axis stays put.
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
