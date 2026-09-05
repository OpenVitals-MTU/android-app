package tech.mmarca.openvitals.features.reports.pdf

import androidx.compose.ui.geometry.Offset
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportValueKind
import tech.mmarca.openvitals.ui.components.ChartRange
import tech.mmarca.openvitals.ui.components.chartYAxisLabels
import tech.mmarca.openvitals.ui.components.decimateOffsets

/**
 * A report chart resolved to PDF coordinates. Pure data. AVERAGE metrics
 * draw as a line with a band; SUM metrics as bars; blood pressure adds a line.
 */
data class PdfChartData(
    val valueKind: ReportValueKind,
    val width: Float,
    val height: Float,
    val plotLeft: Float,
    val plotTop: Float,
    val plotRight: Float,
    val plotBottom: Float,
    val linePoints: List<Offset>,
    val lineMarkers: List<Offset>,
    val bandMaxPoints: List<Offset>,
    val bandMinPoints: List<Offset>,
    val secondaryLinePoints: List<Offset>,
    val secondaryLineMarkers: List<Offset>,
    val bars: List<PdfBar>,
    val yAxisLabels: List<PdfAxisLabel>,
    val xAxisLabels: List<PdfAxisLabel>,
    val gridLineYs: List<Float>,
) {
    /** One bar, already positioned; [top] is the value edge, [bottom] the baseline. */
    data class PdfBar(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** An axis label anchored at [position] (a y for the Y axis, an x center for the X axis). */
    data class PdfAxisLabel(val text: String, val position: Float)
}

private const val YAxisWidth = 42f
private const val XAxisHeight = 16f
private const val MaxLinePoints = 240
private const val MaxXLabels = 6

/** Above this many buckets, markers become a smear; dense charts draw the line alone. */
private const val MaxMarkerPoints = 90

/** Lays [points] out in a box. [approxCharWidth] drops X labels that would overlap. */
fun buildPdfChart(
    points: List<ReportPoint>,
    valueKind: ReportValueKind,
    width: Float,
    height: Float,
    formatAxisValue: (Double) -> String,
    bucketLabel: (ReportPoint) -> String,
    approxCharWidth: Float = 4.5f,
): PdfChartData {
    val plotLeft = YAxisWidth
    val plotTop = 4f
    val plotRight = width - 2f
    val plotBottom = height - XAxisHeight

    val hasBand = valueKind == ReportValueKind.AVERAGE && points.any { it.min != it.value || it.max != it.value }
    val values = buildList {
        points.forEach { point ->
            add(point.value)
            if (hasBand) {
                add(point.min)
                add(point.max)
            }
            point.secondaryValue?.let(::add)
            if (hasBand) {
                point.secondaryMin?.let(::add)
                point.secondaryMax?.let(::add)
            }
        }
    }
    val range = ChartRange.padded(
        values = values,
        floor = 0.0.takeIf { valueKind == ReportValueKind.SUM },
    )

    fun yFor(value: Double): Float {
        val span = (range.max - range.min).takeIf { it > 0 } ?: 1.0
        val fraction = ((value - range.min) / span).coerceIn(0.0, 1.0)
        return (plotBottom - (plotBottom - plotTop) * fraction).toFloat()
    }

    val slotWidth = (plotRight - plotLeft) / points.size.coerceAtLeast(1)
    fun xCenterFor(index: Int): Float = plotLeft + slotWidth * (index + 0.5f)

    val linePoints: List<Offset>
    val lineMarkers: List<Offset>
    val bandMaxPoints: List<Offset>
    val bandMinPoints: List<Offset>
    val secondaryLinePoints: List<Offset>
    val secondaryLineMarkers: List<Offset>
    val bars: List<PdfChartData.PdfBar>

    if (valueKind == ReportValueKind.SUM) {
        val barWidth = (slotWidth * 0.68f).coerceAtLeast(0.75f)
        bars = points.mapIndexed { index, point ->
            val center = xCenterFor(index)
            PdfChartData.PdfBar(
                left = center - barWidth / 2f,
                top = yFor(point.value),
                right = center + barWidth / 2f,
                bottom = yFor(range.min.coerceAtLeast(0.0)),
            )
        }
        linePoints = emptyList()
        lineMarkers = emptyList()
        bandMaxPoints = emptyList()
        bandMinPoints = emptyList()
        secondaryLinePoints = emptyList()
        secondaryLineMarkers = emptyList()
    } else {
        bars = emptyList()
        val rawLinePoints = points.mapIndexed { index, point -> Offset(xCenterFor(index), yFor(point.value)) }
        linePoints = decimateOffsets(rawLinePoints, MaxLinePoints)
        lineMarkers = if (points.size <= MaxMarkerPoints) rawLinePoints else emptyList()
        bandMaxPoints = if (hasBand) {
            decimateOffsets(
                points.mapIndexed { index, point -> Offset(xCenterFor(index), yFor(point.max)) },
                MaxLinePoints,
            )
        } else {
            emptyList()
        }
        bandMinPoints = if (hasBand) {
            decimateOffsets(
                points.mapIndexed { index, point -> Offset(xCenterFor(index), yFor(point.min)) },
                MaxLinePoints,
            )
        } else {
            emptyList()
        }
        val rawSecondaryPoints = points.mapIndexedNotNull { index, point ->
            point.secondaryValue?.let { Offset(xCenterFor(index), yFor(it)) }
        }
        secondaryLinePoints = decimateOffsets(rawSecondaryPoints, MaxLinePoints)
        secondaryLineMarkers = if (points.size <= MaxMarkerPoints) rawSecondaryPoints else emptyList()
    }

    val yLabels = chartYAxisLabels(range.min, range.max, formatAxisValue)
    val yPositions = listOf(plotTop, (plotTop + plotBottom) / 2f, plotBottom)
    val yAxisLabels = yLabels.zip(yPositions) { text, y -> PdfChartData.PdfAxisLabel(text, y) }

    val xAxisLabels = buildXLabels(points, ::xCenterFor, bucketLabel, approxCharWidth)

    return PdfChartData(
        valueKind = valueKind,
        width = width,
        height = height,
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = plotRight,
        plotBottom = plotBottom,
        linePoints = linePoints,
        lineMarkers = lineMarkers,
        bandMaxPoints = bandMaxPoints,
        bandMinPoints = bandMinPoints,
        secondaryLinePoints = secondaryLinePoints,
        secondaryLineMarkers = secondaryLineMarkers,
        bars = bars,
        yAxisLabels = yAxisLabels,
        xAxisLabels = xAxisLabels,
        gridLineYs = yPositions,
    )
}

/** Up to [MaxXLabels] spread labels, minus any whose extent would overlap its predecessor. */
private fun buildXLabels(
    points: List<ReportPoint>,
    xCenterFor: (Int) -> Float,
    bucketLabel: (ReportPoint) -> String,
    approxCharWidth: Float,
): List<PdfChartData.PdfAxisLabel> {
    if (points.isEmpty()) return emptyList()
    val count = minOf(MaxXLabels, points.size)
    val candidates = (0 until count).map { slot ->
        val index = if (count == 1) 0 else (slot * (points.size - 1)) / (count - 1)
        PdfChartData.PdfAxisLabel(bucketLabel(points[index]), xCenterFor(index))
    }
    val kept = mutableListOf<PdfChartData.PdfAxisLabel>()
    var lastEnd = Float.NEGATIVE_INFINITY
    for (label in candidates) {
        val halfWidth = label.text.length * approxCharWidth / 2f
        if (label.position - halfWidth > lastEnd) {
            kept += label
            lastEnd = label.position + halfWidth
        }
    }
    return kept
}
