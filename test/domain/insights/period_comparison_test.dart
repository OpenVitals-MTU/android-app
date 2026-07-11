import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/period_comparison.dart';

void main() {
  test('comparison reports upward percent change', () {
    final comparison = periodComparison(120.0, 100.0);

    expect(comparison.change, closeTo(20.0, 0.01));
    expect(comparison.percentChange ?? 0.0, closeTo(20.0, 0.01));
    expect(comparison.direction, PeriodComparisonDirection.up);
  });

  test('comparison reports downward percent change', () {
    final comparison = periodComparison(75.0, 100.0);

    expect(comparison.change, closeTo(-25.0, 0.01));
    expect(comparison.percentChange ?? 0.0, closeTo(-25.0, 0.01));
    expect(comparison.direction, PeriodComparisonDirection.down);
  });

  test('comparison omits percent when previous value is zero', () {
    final comparison = periodComparison(10.0, 0.0);

    expect(comparison.absoluteChange, closeTo(10.0, 0.01));
    expect(comparison.percentChange, isNull);
    expect(comparison.direction, PeriodComparisonDirection.up);
  });
}
