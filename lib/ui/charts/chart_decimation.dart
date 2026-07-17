import 'dart:ui';

/// Shape-preserving downsampling for dense chart polylines.
///
/// This reduces the number of *vertices* used to draw an already-known curve
/// while keeping its visual shape — including peaks — intact. It is a rendering
/// optimisation, not a claim about the data: nothing is hidden, because the chart
/// culls to the visible window first and zooming in shrinks that window until
/// every raw point is on screen and this becomes a no-op.
///
/// The algorithm is Largest-Triangle-Three-Buckets (LTTB), the standard for
/// time-series line downsampling: it keeps the first and last point and, per
/// bucket, the point forming the largest triangle with the previous kept point
/// and the next bucket's average — which is what preserves the extremes a plain
/// stride or bucket-mean would flatten.
///
/// [offsets] must be sorted ascending by dx. Returns [offsets] unchanged when it
/// already has at most [target] points (no allocation for the sparse case).
List<Offset> decimateOffsets(List<Offset> offsets, int target) {
  final n = offsets.length;
  if (target < 3 || n <= target) return offsets;

  final sampled = <Offset>[offsets.first];

  // Bucket size, leaving room for the mandatory first and last points.
  final every = (n - 2) / (target - 2);

  var a = 0; // index of the previously selected point
  for (var i = 0; i < target - 2; i++) {
    // Average point of the next bucket (the triangle's far vertex).
    var avgX = 0.0;
    var avgY = 0.0;
    var avgStart = (((i + 1) * every).floor()) + 1;
    var avgEnd = (((i + 2) * every).floor()) + 1;
    if (avgEnd > n) avgEnd = n;
    if (avgStart >= avgEnd) avgStart = avgEnd - 1;
    final avgCount = avgEnd - avgStart;
    for (var j = avgStart; j < avgEnd; j++) {
      avgX += offsets[j].dx;
      avgY += offsets[j].dy;
    }
    avgX /= avgCount;
    avgY /= avgCount;

    // Point of the current bucket that forms the largest triangle with `a` and
    // the next bucket's average.
    final rangeStart = ((i * every).floor()) + 1;
    final rangeEnd = (((i + 1) * every).floor()) + 1;
    final pointA = offsets[a];

    var maxArea = -1.0;
    var next = rangeStart;
    for (var j = rangeStart; j < rangeEnd && j < n; j++) {
      final area = ((pointA.dx - avgX) * (offsets[j].dy - pointA.dy) -
                  (pointA.dx - offsets[j].dx) * (avgY - pointA.dy))
              .abs() *
          0.5;
      if (area > maxArea) {
        maxArea = area;
        next = j;
      }
    }

    sampled.add(offsets[next]);
    a = next;
  }

  sampled.add(offsets[n - 1]);
  return sampled;
}
