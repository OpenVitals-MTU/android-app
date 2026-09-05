package tech.mmarca.openvitals.features.homewidgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.ColorUtils

/**
 * Draws the Body Energy day curve into a bitmap. Glance has no canvas, and
 * bars would state the wrong thing about a level. The widgets are un-themed,
 * so the colours are known ahead of time.
 */
internal object BodyEnergyPlot {

    /** The app's own dark-theme chart accent (`AppColors.blue80`). */
    private const val LineColor = 0xFF82D2F2.toInt()

    /** A score is defined as 0..100. Nothing is ever drawn outside this. */
    private const val FloorScore = 0f
    private const val CeilingScore = 100f

    /**
     * Headroom above and below the day, as a share of its range. The scale
     * follows the day, not 0..100: the shape matters, the number is printed.
     */
    private const val RangePadding = 0.18f

    /** Smallest span the scale will show, so a quiet day is not amplified into drama. */
    private const val MinSpan = 24f

    private const val StrokeDp = 2f

    /** Edge labels use the muted foreground, close to the 11sp text beside the plot. */
    private const val LabelTextSizeDp = 10f
    private const val LabelGapDp = 3f

    /**
     * Renders [series] as a smoothed line with a gradient beneath it, first
     * and last values printed beside the ends. Drawn at the widget's real
     * size. Null when there is nothing to draw.
     */
    fun render(series: List<Int>, widthPx: Int, heightPx: Int, density: Float): Bitmap? {
        if (series.size < 2 || widthPx <= 0 || heightPx <= 0) return null

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = StrokeDp * density
        // Inset by half the stroke so the line is not clipped.
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

        // The fill is the same path closed to the baseline, drawn first.
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
     * Prints the first and last value beside the ends of the line, above the
     * curve or below it when there is no headroom, clamped inside the bitmap.
     * Neither is drawn when they would cover the line ([bodyEnergyEdgeLabelsFit]).
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
            // The curve's extremes across the label's span, so the label clears it all.
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

    /** The vertical range for [series]: the day plus headroom, at least [MinSpan] wide. */
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
        // Clamping both ends can collapse the span.
        if (hi - lo < 1f) return FloorScore to CeilingScore
        return lo to hi
    }

    /**
     * A Catmull-Rom spline as cubic beziers. Tangents are damped to a sixth
     * of the neighbour distance: the textbook half overshoots sharp drops.
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

/** Whether the plot has room for both edge labels: half the width, a third of the height. */
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

/** The x of an edge label's left edge: flush with its end of the curve, never past the bitmap. */
internal fun bodyEnergyEdgeLabelX(
    textWidth: Float,
    plotWidth: Float,
    alignEnd: Boolean,
): Float =
    if (alignEnd) (plotWidth - textWidth).coerceAtLeast(0f) else 0f

/**
 * The baseline y for an edge label over a stretch of curve from [curveTopY]
 * to [curveBottomY] (canvas coordinates). Above the curve by [gap], below it
 * when the text would not fit, clamped inside `0..plotHeight`.
 */
internal fun bodyEnergyEdgeLabelBaseline(
    curveTopY: Float,
    curveBottomY: Float,
    ascent: Float,
    descent: Float,
    plotHeight: Float,
    gap: Float,
): Float {
    // Above the curve: the descent must clear the curve's top.
    var baseline = curveTopY - gap - descent
    if (baseline + ascent < 0f) {
        // No headroom: below the curve.
        baseline = curveBottomY + gap - ascent
    }
    return baseline.coerceIn(-ascent, (plotHeight - descent).coerceAtLeast(-ascent))
}
