package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.floor

/**
 * Shape-preserving downsampling for dense polylines: Largest-Triangle-
 * Three-Buckets, which keeps the extremes a stride would flatten. A
 * rendering optimisation only; zooming in makes it a no-op. [offsets] must
 * be sorted by x; returned unchanged when already at most [target] points.
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

        // The point forming the largest triangle with `a` and the next bucket's average.
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
