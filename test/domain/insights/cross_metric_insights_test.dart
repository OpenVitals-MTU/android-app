import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/cross_metric_insights.dart';

final LocalDate _start = LocalDate(2026, 5, 1);

List<CrossMetricValue> _valuesOf(List<double> values) => [
      for (var index = 0; index < values.length; index++)
        CrossMetricValue(date: _start.plusDays(index), value: values[index]),
    ];

void main() {
  test('calculatesPositiveCorrelationForPairedDays', () {
    final primary = _valuesOf([1.0, 2.0, 3.0, 4.0]);
    final secondary = _valuesOf([2.0, 4.0, 6.0, 8.0]);

    final insight = crossMetricInsight(primary, secondary);

    expect(insight, isNotNull);
    expect(insight!.correlation, closeTo(1.0, 0.0001));
    expect(insight.direction, CrossMetricDirection.positive);
    expect(insight.strength, CrossMetricStrength.strong);
    expect(insight.pairedDays, 4);
  });

  test('calculatesNegativeCorrelationForPairedDays', () {
    final primary = _valuesOf([1.0, 2.0, 3.0, 4.0]);
    final secondary = _valuesOf([8.0, 6.0, 4.0, 2.0]);

    final insight = crossMetricInsight(primary, secondary);

    expect(insight, isNotNull);
    expect(insight!.correlation, closeTo(-1.0, 0.0001));
    expect(insight.direction, CrossMetricDirection.negative);
    expect(insight.strength, CrossMetricStrength.strong);
  });

  test('ignoresUnpairedAndEmptyValues', () {
    final primary = _valuesOf([0.0, 2.0, 3.0, 4.0]);
    final secondary = [
      CrossMetricValue(date: _start.plusDays(1), value: 4.0),
      CrossMetricValue(date: _start.plusDays(2), value: 6.0),
      CrossMetricValue(date: _start.plusDays(3), value: 8.0),
      CrossMetricValue(date: _start.plusDays(10), value: 100.0),
    ];

    final insight = crossMetricInsight(primary, secondary);

    expect(insight, isNotNull);
    expect(insight!.pairedDays, 3);
    expect(insight.correlation, closeTo(1.0, 0.0001));
  });

  test('returnsNullWhenThereAreNotEnoughPairs', () {
    final primary = _valuesOf([1.0, 2.0]);
    final secondary = _valuesOf([2.0, 4.0]);

    final insight = crossMetricInsight(primary, secondary);

    expect(insight, isNull);
  });
}
