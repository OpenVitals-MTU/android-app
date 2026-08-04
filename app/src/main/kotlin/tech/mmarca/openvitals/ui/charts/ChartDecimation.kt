package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.floor

/**
 * Shape-preserving downsampling for dense chart polylines.
 *
 * This reduces the number of *vertices* used to draw an already-known curve while
 * keeping its visual shape — including peaks — intact. It is a rendering optimisation,
 * not a claim about the data: nothing is hidden, because the chart culls to the visible
 * window first and zooming in shrinks that window until every raw point is on screen and
 * this becomes a no-op.
 *
 * The algorithm is Largest-Triangle-Three-Buckets (LTTB), the standard for time-series
 * line downsampling: it keeps the first and last point and, per bucket, the point
 * forming the largest triangle with the previous kept point and the next bucket's
 * average — which is what preserves the extremes a plain stride or bucket-mean would
 * flatten.
 *
 * [offsets] must be sorted ascending by x. Returns [offsets] unchanged when it already
 * has at most [target] points (no allocation for the sparse case).
 */
fun decimateOffsets(offsets: List<Offset>, target: Int): List<Offset> {
    val n = offsets.size
    if (target < 3 || n <= target) return offsets

    val sampled = ArrayList<Offset>(target)
    sampled.add(offsets.first())

    // Bucket size, leaving room for the mandatory first and last points.
    val every = (n - 2).toDouble() / (target - 2)

    var a = 0 // index of the previously selected point
    for (i in 0 until target - 2) {
        // Average point of the next bucket (the triangle's far vertex).
        var avgX = 0.0
        var avgY = 0.0
        var avgStart = floor((i + 1) * every).toInt() + 1
        var avgEnd = floor((i + 2) * every).toInt() + 1
        if (avgEnd > n) avgEnd = n
        if (avgStart >= avgEnd) avgStart = avgEnd - 1
        val avgCount = avgEnd - avgStart
        for (j in avgStart until avgEnd) {
            avgX += offsets[j].x
            avgY += offsets[j].y
        }
        avgX /= avgCount
        avgY /= avgCount

        // Point of the current bucket that forms the largest triangle with `a` and the
        // next bucket's average.
        val rangeStart = floor(i * every).toInt() + 1
        val rangeEnd = floor((i + 1) * every).toInt() + 1
        val pointA = offsets[a]

        var maxArea = -1.0
        var next = rangeStart
        var j = rangeStart
        while (j < rangeEnd && j < n) {
            val area = abs(
                (pointA.x - avgX) * (offsets[j].y - pointA.y) -
                    (pointA.x - offsets[j].x) * (avgY - pointA.y),
            ) * 0.5
            if (area > maxArea) {
                maxArea = area
                next = j
            }
            j++
        }

        sampled.add(offsets[next])
        a = next
    }

    sampled.add(offsets[n - 1])
    return sampled
}
