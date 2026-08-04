package tech.mmarca.openvitals.features.homewidgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.ColorUtils

/**
 * Draws the Body Energy day curve into a bitmap.
 *
 * WHY A BITMAP: Glance has no canvas. Its composables are layout primitives —
 * boxes, rows, text — so a curve cannot be expressed in them at all. A row of
 * proportional boxes would draw a bar chart, but Body Energy is a LEVEL through
 * the day rather than a quantity per hour, and bars state the wrong thing about
 * it. So the curve is rasterised here and shown as an image.
 *
 * The widgets are deliberately un-themed ([WidgetBackground] and friends) — one
 * flat dark background, no light variant — which is what makes this practical:
 * the colours are known ahead of time, so a bitmap cannot end up drawn for the
 * wrong theme.
 */
internal object BodyEnergyPlot {

    /** The app's own dark-theme chart accent (`AppColors.blue80`). */
    private const val LineColor = 0xFF82D2F2.toInt()

    /** A score is defined as 0..100. Nothing is ever drawn outside this. */
    private const val FloorScore = 0f
    private const val CeilingScore = 100f

    /**
     * Headroom kept above and below the day, as a share of its range.
     *
     * The scale follows the DAY rather than the full 0..100, unlike the chart
     * inside the app. A real day moves through thirty-odd points, which pinned
     * to 0..100 draws as a nearly flat line across the middle of a widget an
     * inch tall — technically true and useless to glance at. Here the shape
     * matters and the absolute height does not: the number is printed next to it.
     */
    private const val RangePadding = 0.18f

    /** Smallest span the scale will show, so a quiet day is not amplified into drama. */
    private const val MinSpan = 24f

    private const val StrokeDp = 2f

    /**
     * The edge labels share the tile's muted foreground ([WidgetMutedText]) and
     * sit close to the 11sp of the text rows beside the plot, so they read as
     * annotation rather than as a second data series.
     */
    private const val LabelTextSizeDp = 10f
    private const val LabelGapDp = 3f

    /**
     * Renders [series] as a smoothed line with a gradient beneath it, the first
     * and last values printed beside their ends of the curve.
     *
     * [widthPx] and [heightPx] are the pixel size to draw at — the caller knows
     * the widget's real size, so nothing is scaled after the fact and the line
     * stays crisp at any of the sizes the widget can be resized to.
     *
     * Returns null when there is nothing to draw. Callers fall back to the text
     * layout rather than showing an empty frame that looks like a failure.
     */
    fun render(series: List<Int>, widthPx: Int, heightPx: Int, density: Float): Bitmap? {
        if (series.size < 2 || widthPx <= 0 || heightPx <= 0) return null

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = StrokeDp * density
        // Inset by half the stroke so the line's own width cannot be clipped at
        // the top or bottom of the bitmap.
        val top = stroke / 2f
        val bottom = heightPx - stroke / 2f
        val usableHeight = bottom - top
        if (usableHeight <= 0f) return null

        fun xAt(index: Int): Float =
            widthPx * index.toFloat() / (series.size - 1).toFloat()

        val (lo, hi) = scaleFor(series)
        fun yAt(score: Int): Float {
            val clamped = score.toFloat().coerceIn(lo, hi)
            val fraction = (clamped - lo) / (hi - lo)
            // A canvas grows downward; a score grows upward.
            return bottom - fraction * usableHeight
        }

        val line = smoothPath(series, ::xAt, ::yAt)

        // The fill is the same path closed down to the baseline. Drawn first so
        // the line sits on top of its own gradient rather than under it.
        val fill = Path(line).apply {
            lineTo(xAt(series.size - 1), heightPx.toFloat())
            lineTo(xAt(0), heightPx.toFloat())
            close()
        }
        canvas.drawPath(
            fill,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f,
                    top,
                    0f,
                    heightPx.toFloat(),
                    ColorUtils.setAlphaComponent(LineColor, 0x66),
                    ColorUtils.setAlphaComponent(LineColor, 0x00),
                    Shader.TileMode.CLAMP,
                )
            },
        )
        canvas.drawPath(
            line,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = LineColor
            },
        )
        drawEdgeLabels(canvas, series, ::xAt, ::yAt, widthPx, heightPx, density)
        return bitmap
    }

    /**
     * Prints the curve's first and last value beside their ends of the line —
     * the day's start on the left, the current score on the right.
     *
     * Each label sits above the stretch of curve it spans, flipping below it
     * when the curve is already at the top, and is clamped inside the bitmap
     * so nothing clips at the edges. On a plot too small to hold both without
     * covering the line, neither is drawn ([bodyEnergyEdgeLabelsFit]) — the
     * text rows beside the plot still carry the numbers.
     */
    private fun drawEdgeLabels(
        canvas: Canvas,
        series: List<Int>,
        xAt: (Int) -> Float,
        yAt: (Int) -> Float,
        widthPx: Int,
        heightPx: Int,
        density: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WidgetMutedTextArgb
            textSize = LabelTextSizeDp * density
        }
        val startText = series.first().toString()
        val endText = series.last().toString()
        val startWidth = paint.measureText(startText)
        val endWidth = paint.measureText(endText)
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        if (!bodyEnergyEdgeLabelsFit(
                plotWidth = widthPx.toFloat(),
                plotHeight = heightPx.toFloat(),
                startTextWidth = startWidth,
                endTextWidth = endWidth,
                textHeight = textHeight,
            )
        ) {
            return
        }
        val gap = LabelGapDp * density

        fun draw(text: String, textWidth: Float, alignEnd: Boolean) {
            val x = bodyEnergyEdgeLabelX(
                textWidth = textWidth,
                plotWidth = widthPx.toFloat(),
                alignEnd = alignEnd,
            )
            // The curve's extremes across the label's horizontal span, so the
            // label clears the whole stretch of line it sits over, not just the
            // endpoint.
            var curveTopY = Float.MAX_VALUE
            var curveBottomY = -Float.MAX_VALUE
            series.forEachIndexed { index, score ->
                val pointX = xAt(index)
                if (pointX >= x && pointX <= x + textWidth) {
                    val pointY = yAt(score)
                    if (pointY < curveTopY) curveTopY = pointY
                    if (pointY > curveBottomY) curveBottomY = pointY
                }
            }
            val baseline = bodyEnergyEdgeLabelBaseline(
                curveTopY = curveTopY,
                curveBottomY = curveBottomY,
                ascent = metrics.ascent,
                descent = metrics.descent,
                plotHeight = heightPx.toFloat(),
                gap = gap,
            )
            canvas.drawText(text, x, baseline, paint)
        }

        draw(startText, startWidth, alignEnd = false)
        draw(endText, endWidth, alignEnd = true)
    }

    /**
     * The vertical range to draw [series] against: the day, plus headroom.
     *
     * Widened to at least [MinSpan] so a day that barely moved stays visibly
     * flat instead of being stretched into peaks it never had — the failure
     * mode in the other direction, and the more misleading of the two.
     */
    private fun scaleFor(series: List<Int>): Pair<Float, Float> {
        val min = (series.minOrNull() ?: 0).toFloat()
        val max = (series.maxOrNull() ?: 100).toFloat()
        val pad = ((max - min) * RangePadding).coerceAtLeast(2f)
        var lo = min - pad
        var hi = max + pad
        if (hi - lo < MinSpan) {
            val middle = (hi + lo) / 2f
            lo = middle - MinSpan / 2f
            hi = middle + MinSpan / 2f
        }
        // Never past the ends of the scale a score is defined on.
        lo = lo.coerceAtLeast(FloorScore)
        hi = hi.coerceAtMost(CeilingScore)
        // Clamping both ends can collapse the span on a day spent near a limit.
        if (hi - lo < 1f) return FloorScore to CeilingScore
        return lo to hi
    }

    /**
     * A curve through every point, as cubic segments.
     *
     * Control points come from the neighbours on each side (a Catmull-Rom spline
     * written as beziers), which is what makes the line round through a sample
     * instead of turning a corner at it. The tangents are deliberately damped to
     * a sixth of the neighbour distance rather than the textbook half: at widget
     * size a fuller curve overshoots a sharp drop and draws the score going
     * somewhere it never went.
     */
    private fun smoothPath(
        series: List<Int>,
        xAt: (Int) -> Float,
        yAt: (Int) -> Float,
    ): Path {
        val path = Path()
        path.moveTo(xAt(0), yAt(series[0]))
        for (i in 0 until series.size - 1) {
            val x0 = xAt(i)
            val y0 = yAt(series[i])
            val x1 = xAt(i + 1)
            val y1 = yAt(series[i + 1])
            val prevY = yAt(series[(i - 1).coerceAtLeast(0)])
            val nextY = yAt(series[(i + 2).coerceAtMost(series.size - 1)])
            val dx = (x1 - x0) / 6f
            path.cubicTo(
                x0 + dx,
                y0 + (y1 - prevY) / 6f,
                x1 - dx,
                y1 - (nextY - y0) / 6f,
                x1,
                y1,
            )
        }
        return path
    }
}

/** [WidgetMutedText] as an ARGB int, for [Paint] rather than Compose. */
private const val WidgetMutedTextArgb = 0xFFC9D7DD.toInt()

/**
 * Whether the plot has room for both edge labels.
 *
 * Together they may take at most half the plot's width and a third of its
 * height; past that they stop annotating the curve and start covering it, and
 * the numbers are already printed in the text column anyway.
 */
internal fun bodyEnergyEdgeLabelsFit(
    plotWidth: Float,
    plotHeight: Float,
    startTextWidth: Float,
    endTextWidth: Float,
    textHeight: Float,
): Boolean =
    plotWidth > 0f &&
        plotHeight > 0f &&
        textHeight > 0f &&
        startTextWidth + endTextWidth <= plotWidth * 0.5f &&
        textHeight <= plotHeight / 3f

/**
 * The x of an edge label's left edge: flush with its end of the curve — the
 * first point sits at x 0, the last at the plot's right edge — and never past
 * the bitmap, so a label wider than the plot cannot clip.
 */
internal fun bodyEnergyEdgeLabelX(
    textWidth: Float,
    plotWidth: Float,
    alignEnd: Boolean,
): Float =
    if (alignEnd) (plotWidth - textWidth).coerceAtLeast(0f) else 0f

/**
 * The baseline y for an edge label spanning a stretch of curve whose highest
 * point is [curveTopY] and lowest is [curveBottomY] (canvas coordinates, so
 * "highest" is the SMALLER number).
 *
 * Preferred placement is above the curve, [gap] clear of it; when the curve is
 * too close to the top of the bitmap for the text to fit, the label flips to
 * below the curve instead. Either way the result is clamped so every pixel of
 * the text — [ascent] above the baseline (negative, as [Paint.FontMetrics]
 * reports it) to [descent] below — stays inside `0..plotHeight`.
 */
internal fun bodyEnergyEdgeLabelBaseline(
    curveTopY: Float,
    curveBottomY: Float,
    ascent: Float,
    descent: Float,
    plotHeight: Float,
    gap: Float,
): Float {
    // Above the curve: the text's descent must clear the curve's top by gap.
    var baseline = curveTopY - gap - descent
    if (baseline + ascent < 0f) {
        // No headroom — below the curve: the text's ascent clears its bottom.
        baseline = curveBottomY + gap - ascent
    }
    return baseline.coerceIn(-ascent, (plotHeight - descent).coerceAtLeast(-ascent))
}
