import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/ui/charts/chart_curve.dart';

/// The curve is decoration. The data still has to be true.
///
/// A plain spline (Catmull-Rom, natural cubic) overshoots: to stay smooth through a
/// corner it swings past the points either side. On a cumulative chart that reads as
/// the running total DIPPING between two drinks — a line saying you un-drank water.
/// These tests pin that it cannot happen.
void main() {
  /// Samples the rendered path densely and returns the y at each step.
  ///
  /// Reading the path back is the only honest check: asserting on control points
  /// would test my arithmetic, not the curve the user sees.
  List<double> renderedYs(Path path, {int steps = 400}) {
    final metrics = path.computeMetrics().toList();
    final ys = <double>[];
    for (final metric in metrics) {
      for (var i = 0; i <= steps; i++) {
        final tangent = metric.getTangentForOffset(metric.length * i / steps);
        if (tangent != null) ys.add(tangent.position.dy);
      }
    }
    return ys;
  }

  test('a rising series never dips on the way up', () {
    // Screen coords: y grows DOWNWARD, so a rising total is a falling y. The three
    // clustered points then the long flat run are exactly the shape that makes a
    // naive spline overshoot.
    final path = smoothPath(const [
      Offset(0, 200),
      Offset(20, 190),
      Offset(40, 100),
      Offset(60, 40),
      Offset(300, 40),
    ]);

    final ys = renderedYs(path);
    for (var i = 1; i < ys.length; i++) {
      expect(
        ys[i],
        lessThanOrEqualTo(ys[i - 1] + 0.01),
        reason: 'the curve went back DOWN — the running total dipped',
      );
    }
  });

  test('a flat run stays flat', () {
    // "You drank nothing between nine and six" must read as a flat line, not as a
    // gentle bulge that implies you were sipping the whole time.
    final path = smoothPath(const [
      Offset(0, 200),
      Offset(50, 100),
      Offset(150, 100),
      Offset(200, 20),
    ]);

    final metrics = path.computeMetrics().first;
    // Sample the middle of the flat stretch by walking the path and keeping the
    // points whose x lands inside it.
    for (var i = 0; i <= 400; i++) {
      final tangent = metrics.getTangentForOffset(metrics.length * i / 400);
      if (tangent == null) continue;
      final position = tangent.position;
      if (position.dx > 60 && position.dx < 140) {
        expect(position.dy, closeTo(100, 0.5),
            reason: 'the flat stretch bulged');
      }
    }
  });

  test('never overshoots below the lowest sample', () {
    // A heart rate curve must not swing under the slowest beat actually recorded.
    final path = smoothPath(const [
      Offset(0, 100),
      Offset(50, 100),
      Offset(100, 20),
      Offset(150, 100),
    ]);

    final ys = renderedYs(path);
    expect(ys.reduce((a, b) => a < b ? a : b), greaterThanOrEqualTo(20 - 0.01));
    expect(ys.reduce((a, b) => a > b ? a : b), lessThanOrEqualTo(100 + 0.01));
  });

  test('draws a vertical riser straight rather than looping through it', () {
    // Two readings at the same instant. No function curves through that, and a
    // spline that tries will loop the path back on itself.
    final path = smoothPath(const [
      Offset(0, 100),
      Offset(50, 100),
      Offset(50, 40),
      Offset(100, 40),
    ]);

    final xs = [
      for (final metric in path.computeMetrics())
        for (var i = 0; i <= 200; i++)
          metric.getTangentForOffset(metric.length * i / 200)?.position.dx,
    ].whereType<double>().toList();

    for (var i = 1; i < xs.length; i++) {
      expect(xs[i], greaterThanOrEqualTo(xs[i - 1] - 0.01),
          reason: 'the path doubled back in x');
    }
  });

  test('degenerate inputs do not throw', () {
    expect(smoothPath(const []).computeMetrics().isEmpty, isTrue);
    expect(smoothPath(const [Offset(5, 5)]), isNotNull);
    expect(smoothPath(const [Offset(0, 0), Offset(10, 10)]), isNotNull);
  });
}
