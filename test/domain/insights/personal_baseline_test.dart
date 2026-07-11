import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/personal_baseline.dart';

final LocalDate _referenceDate = LocalDate(2026, 5, 23);

void main() {
  test('calculatesTrailingWindowAverages', () {
    final values = <BaselineValue>[
      for (var offset = 0; offset < 90; offset++)
        BaselineValue(date: _referenceDate.minusDays(offset), value: 10.0),
      BaselineValue(date: _referenceDate.minusDays(91), value: 1000.0),
    ];

    final insight = personalBaselineInsight(12.0, values, _referenceDate);

    expect(insight, isNotNull);
    final summaries = {
      for (final summary in insight!.summaries) summary.windowDays: summary,
    };
    expect(summaries[30]!.average, closeTo(10.0, 0.0001));
    expect(summaries[60]!.average, closeTo(10.0, 0.0001));
    expect(summaries[90]!.average, closeTo(10.0, 0.0001));
    expect(insight.primarySummary.sampleCount, 90);
  });

  test('marksValuesInsideStandardDeviationAsUsual', () {
    final values = [8.0, 10.0, 12.0]
        .asMap()
        .entries
        .map((entry) => BaselineValue(
              date: _referenceDate.minusDays(entry.key),
              value: entry.value,
            ))
        .toList();

    final insight = personalBaselineInsight(
      11.0,
      values,
      _referenceDate,
      windows: const [30],
    );

    expect(insight!.status, BaselineStatus.usual);
  });

  test('marksValuesOutsideUsualRangeButBelowAnomalyThreshold', () {
    final values = [8.0, 10.0, 12.0]
        .asMap()
        .entries
        .map((entry) => BaselineValue(
              date: _referenceDate.minusDays(entry.key),
              value: entry.value,
            ))
        .toList();

    final insight = personalBaselineInsight(
      13.0,
      values,
      _referenceDate,
      windows: const [30],
    );

    expect(insight!.status, BaselineStatus.above);
  });

  test('marksTwoStandardDeviationsAsAnomaly', () {
    final values = [8.0, 10.0, 12.0]
        .asMap()
        .entries
        .map((entry) => BaselineValue(
              date: _referenceDate.minusDays(entry.key),
              value: entry.value,
            ))
        .toList();

    final insight = personalBaselineInsight(
      14.0,
      values,
      _referenceDate,
      windows: const [30],
    );

    expect(insight!.status, BaselineStatus.unusualHigh);
  });

  test('returnsNullWhenThereAreNotEnoughSamples', () {
    final values = [
      BaselineValue(date: _referenceDate, value: 10.0),
      BaselineValue(date: _referenceDate.minusDays(1), value: 12.0),
    ];

    final insight = personalBaselineInsight(
      11.0,
      values,
      _referenceDate,
      windows: const [30],
    );

    expect(insight, isNull);
  });
}
