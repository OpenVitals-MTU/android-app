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
 * The widgets are deliberately un-themed (see [HomeWidgetTokens]) — one flat
 * dark background, no light variant — which is what makes this practical: the
 * colours are known ahead of time, so a bitmap cannot end up drawn for the wrong
 * theme.
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
     * Renders [series] as a smoothed line with a gradient beneath it.
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
        return bitmap
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
