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

/// Damps a quantized staircase before it is splined.
///
/// The body-energy score is an integer 0–100 sampled per bucket, so the raw
/// series is a flight of stairs, and a curve through it traces the steps and
/// reads as jagged. A small centred moving average — window widening with the
/// point count — turns the staircase back into the smooth thing it is a
/// measurement of.
///
/// A DATA decision, not a curve one, which is why it is its own function and not
/// folded into [smoothPath]. Smoothing the geometry is a lie about how the line
/// gets from A to B; smoothing the SAMPLES is a claim about the signal underneath
/// them, and the two want to be argued about separately.
List<Offset> movingAverageY(List<Offset> points) {
  if (points.length < 3) return points;
  final radius = (points.length ~/ 16).clamp(1, 4);
  final last = points.length - 1;
  return [
    for (var index = 0; index < points.length; index++)
      () {
        final from = (index - radius).clamp(0, last);
        final to = (index + radius).clamp(0, last);
        var sum = 0.0;
        for (var i = from; i <= to; i++) {
          sum += points[i].dy;
        }
        return Offset(points[index].dx, sum / (to - from + 1));
      }(),
  ];
}
