package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.sqrt

/** One drawing command of a monotone cubic curve. Plain data, so the maths is JVM-testable. */
sealed interface CurveSegment {
    /** A vertical riser: two readings at the same instant. */
    data class Line(val end: Offset) : CurveSegment

    /** Hermite tangents expressed as cubic Bézier control points. */
    data class Cubic(
        val control1: Offset,
        val control2: Offset,
        val end: Offset,
    ) : CurveSegment
}

/**
 * A smooth line through [points], for every chart. Monotone cubic
 * (Fritsch-Carlson): a plain spline overshoots and draws a running total
 * dipping between two drinks. This one never adds a peak, trough or
 * reversal not in the data. [points] must be sorted by x.
 */
fun monotoneCubicSegments(points: List<Offset>): List<CurveSegment> {
    val n = points.size
    if (n < 2) return emptyList()

    // Secant slope of each segment.
    val slopes = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        val dx = points[i + 1].x - points[i].x
        slopes[i] = if (abs(dx) < Epsilon) 0f else (points[i + 1].y - points[i].y) / dx
    }

    // Tangent at each point: the average of the slopes either side of it.
    val tangents = FloatArray(n)
    tangents[0] = slopes.first()
    for (i in 1 until n - 1) {
        tangents[i] = (slopes[i - 1] + slopes[i]) / 2f
    }
    tangents[n - 1] = slopes.last()

    // Fritsch-Carlson: clamp the tangents so no segment overshoots.
    for (i in 0 until n - 1) {
        val slope = slopes[i]
        if (abs(slope) < Epsilon) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
            continue
        }
        val alpha = tangents[i] / slope
        val beta = tangents[i + 1] / slope
        val magnitude = alpha * alpha + beta * beta
        if (magnitude > 9f) {
            val tau = 3f / sqrt(magnitude)
            tangents[i] = tau * alpha * slope
            tangents[i + 1] = tau * beta * slope
        }
    }

    val segments = ArrayList<CurveSegment>(n - 1)
    for (i in 0 until n - 1) {
        val start = points[i]
        val end = points[i + 1]
        val dx = end.x - start.x

        if (abs(dx) < Epsilon) {
            segments.add(CurveSegment.Line(end))
            continue
        }

        // Hermite tangents as cubic Bézier control points.
        segments.add(
            CurveSegment.Cubic(
                control1 = Offset(start.x + dx / 3f, start.y + tangents[i] * dx / 3f),
                control2 = Offset(end.x - dx / 3f, end.y - tangents[i + 1] * dx / 3f),
                end = end,
            ),
        )
    }
    return segments
}

/** The [monotoneCubicSegments] of [points], built into a drawable [Path]. */
fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)
    for (segment in monotoneCubicSegments(points)) {
        when (segment) {
            is CurveSegment.Line -> path.lineTo(segment.end.x, segment.end.y)
            is CurveSegment.Cubic -> path.cubicTo(
                segment.control1.x,
                segment.control1.y,
                segment.control2.x,
                segment.control2.y,
                segment.end.x,
                segment.end.y,
            )
        }
    }
    return path
}

/**
 * Damps a quantized staircase before it is splined: a small centred moving
 * average, window widening with the point count. A data decision, kept
 * apart from [smoothPath].
 */
fun movingAverageY(points: List<Offset>): List<Offset> {
    if (points.size < 3) return points
    val radius = (points.size / 16).coerceIn(1, 4)
    val last = points.size - 1
    return List(points.size) { index ->
        val from = (index - radius).coerceIn(0, last)
        val to = (index + radius).coerceIn(0, last)
        var sum = 0f
        for (i in from..to) {
            sum += points[i].y
        }
        Offset(points[index].x, sum / (to - from + 1))
    }
}

private const val Epsilon = 1e-9f
