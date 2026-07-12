import 'dart:math' as math;
import 'dart:ui';

/// A smooth line through [points], for every chart in the app.
///
/// **Monotone cubic** (Fritsch–Carlson), not a plain spline, and the difference is
/// not cosmetic.
///
/// An ordinary Catmull-Rom or natural cubic overshoots: to stay smooth through a
/// sharp corner it swings past the points on either side. On a cumulative chart
/// that overshoot is a lie you can read off the axis — the running total visibly
/// DIPS between two drinks, drawing a line that says you un-drank water. Same for
/// steps, calories and distance. It would also swing a heart rate below the
/// lowest sample actually recorded.
///
/// Monotone cubic is smooth but structurally cannot do that: where the data rises,
/// the curve rises; where the data is flat, the curve is flat. It never introduces
/// a peak, a trough or a reversal that is not in the samples. The curve is
/// decoration; the data still has to be true.
///
/// [points] must be sorted by x. Vertical segments (two samples at the same
/// instant) are drawn straight, since no function can curve through them.
Path smoothPath(List<Offset> points) {
  final path = Path();
  if (points.isEmpty) return path;

  path.moveTo(points.first.dx, points.first.dy);
  if (points.length == 1) return path;

  final n = points.length;

  // Secant slope of each segment.
  final slopes = <double>[];
  for (var i = 0; i < n - 1; i++) {
    final dx = points[i + 1].dx - points[i].dx;
    slopes.add(dx.abs() < 1e-9 ? 0.0 : (points[i + 1].dy - points[i].dy) / dx);
  }

  // Tangent at each point: the average of the slopes either side of it.
  final tangents = <double>[slopes.first];
  for (var i = 1; i < n - 1; i++) {
    tangents.add((slopes[i - 1] + slopes[i]) / 2);
  }
  tangents.add(slopes.last);

  // Fritsch–Carlson: clamp the tangents so no segment can overshoot. A flat
  // segment forces both its tangents to zero — that is what keeps "you drank
  // nothing between nine and six" flat instead of bulging upward.
  for (var i = 0; i < n - 1; i++) {
    final slope = slopes[i];
    if (slope.abs() < 1e-9) {
      tangents[i] = 0;
      tangents[i + 1] = 0;
      continue;
    }
    final alpha = tangents[i] / slope;
    final beta = tangents[i + 1] / slope;
    final magnitude = alpha * alpha + beta * beta;
    if (magnitude > 9) {
      final tau = 3 / math.sqrt(magnitude);
      tangents[i] = tau * alpha * slope;
      tangents[i + 1] = tau * beta * slope;
    }
  }

  for (var i = 0; i < n - 1; i++) {
    final start = points[i];
    final end = points[i + 1];
    final dx = end.dx - start.dx;

    // A vertical riser: two readings at the same instant. There is no curve
    // through it, and pretending otherwise would loop the path back on itself.
    if (dx.abs() < 1e-9) {
      path.lineTo(end.dx, end.dy);
      continue;
    }

    // Hermite tangents as cubic Bézier control points.
    path.cubicTo(
      start.dx + dx / 3,
      start.dy + tangents[i] * dx / 3,
      end.dx - dx / 3,
      end.dy - tangents[i + 1] * dx / 3,
      end.dx,
      end.dy,
    );
  }

  return path;
}
