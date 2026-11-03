package tech.mmarca.openvitals.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.io.ByteArrayOutputStream

/**
 * Draws a metric tile's day curve, the way the phone's widgets draw theirs
 * (`BodyEnergyPlot`): a line in the metric's accent with a gradient fading
 * beneath it. Tiles have no canvas, so the curve travels as a PNG image; the
 * numbers stay text in the layout. The x axis is the whole day from
 * [TileDay], so the line ends at now, over the hour marks 0, 6, 12, 18, 24.
 */
internal class TileChart(
    private val widthPx: Int,
    private val heightPx: Int,
    private val density: Float,
    private val accent: Int,
) {
    private val labelHeight = LABEL_TEXT_DP * density + LABEL_GAP_DP * density
    private val stroke = STROKE_DP * density
    private val top = stroke
    private val baseline = heightPx - labelHeight

    /** A running total from 0 at midnight, one point per bucket so far: steps or floors. Null when there is no room. */
    fun cumulative(points: List<Double>, day: TileDay): Png? {
        if (!fits() || points.isEmpty()) return null
        val high = points.last().takeIf { it > 0.0 } ?: 1.0
        fun yAt(value: Double): Float = baseline - (value / high).toFloat() * (baseline - top)
        val line = Path().apply {
            moveTo(0f, baseline)
            points.forEachIndexed { bucket, value -> lineTo(xAt(day.bucketEnd(bucket)), yAt(value)) }
        }
        return draw(day) { canvas ->
            val fill = Path(line).apply {
                lineTo(xAt(day.bucketEnd(points.lastIndex)), baseline)
                close()
            }
            canvas.drawPath(fill, fillPaint())
            canvas.drawPath(line, linePaint())
        }
    }

    /**
     * Bucket means joined into lines, broken where a bucket has none: off the
     * wrist or charging. A lone bucket is a dot. The scale follows the day,
     * at least [MIN_BPM_SPAN] wide, so a calm day is not drawn as drama.
     */
    fun heartRate(points: List<Int?>, day: TileDay): Png? {
        val known = points.filterNotNull()
        if (!fits() || known.isEmpty()) return null
        val middle = (known.min() + known.max()) / 2f
        val span = maxOf((known.max() - known.min()).toFloat(), MIN_BPM_SPAN)
        val low = middle - span / 2f
        fun yAt(bpm: Int): Float = baseline - (bpm - low) / span * (baseline - top)
        fun xOf(bucket: Int): Float = xAt(day.bucketMiddle(bucket))
        return draw(day) { canvas ->
            val line = linePaint()
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
            var run = Path()
            var runLength = 0
            fun endRun(lastBucket: Int) {
                if (runLength == 1) canvas.drawCircle(xOf(lastBucket), yAt(points[lastBucket]!!), stroke, dot)
                if (runLength > 1) canvas.drawPath(run, line)
                run = Path()
                runLength = 0
            }
            points.forEachIndexed { bucket, bpm ->
                if (bpm == null) {
                    if (runLength > 0) endRun(bucket - 1)
                } else {
                    if (runLength == 0) run.moveTo(xOf(bucket), yAt(bpm)) else run.lineTo(xOf(bucket), yAt(bpm))
                    runLength++
                }
            }
            if (runLength > 0) endRun(points.lastIndex)
        }
    }

    private fun fits(): Boolean = widthPx > 0 && baseline - top > stroke * 4

    private fun xAt(fraction: Float): Float = widthPx * fraction

    private fun draw(day: TileDay, plot: (Canvas) -> Unit): Png {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawLine(0f, baseline, widthPx.toFloat(), baseline, Paint().apply {
            color = AXIS_COLOR
            strokeWidth = density
        })
        drawHourLabels(canvas, day)
        plot(canvas)
        val png = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, png)
        bitmap.recycle()
        return Png(png.toByteArray(), widthPx, heightPx)
    }

    /** The hour marks where those hours fell, so a 23- or 25-hour day still labels its noon right. */
    private fun drawHourLabels(canvas: Canvas, day: TileDay) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_COLOR
            textSize = LABEL_TEXT_DP * density
        }
        val y = heightPx.toFloat() - paint.descent()
        TileDay.HOUR_MARKS.zip(day.hourMarks).forEach { (hour, fraction) ->
            val label = hour.toString()
            val width = paint.measureText(label)
            val x = (xAt(fraction) - width / 2f).coerceIn(0f, widthPx - width)
            canvas.drawText(label, x, y, paint)
        }
    }

    private fun linePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = accent
    }

    private fun fillPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = LinearGradient(0f, top, 0f, baseline, withAlpha(accent, FILL_ALPHA_TOP), withAlpha(accent, 0), Shader.TileMode.CLAMP)
    }

    /**
     * The curve as PNG bytes, sent as an inline image of unspecified format:
     * every tile renderer decodes that, where raw ARGB pixels need a newer
     * one. It is also a few kilobytes instead of the raw pixels' hundred and more.
     */
    class Png(val bytes: ByteArray, val widthPx: Int, val heightPx: Int)

    private companion object {
        const val STROKE_DP = 2f
        const val LABEL_TEXT_DP = 10f
        const val LABEL_GAP_DP = 3f
        const val MIN_BPM_SPAN = 40f
        const val FILL_ALPHA_TOP = 0x66

        /** PNG ignores the quality; lossless either way. */
        const val PNG_QUALITY = 100

        /** Muted, as the phone's chart labels; the tile is always dark. */
        const val LABEL_COLOR = 0xFFA9ACB1.toInt()
        const val AXIS_COLOR = 0x33FFFFFF

        fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
    }
}
