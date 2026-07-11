import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/usecase/load_heart_period_use_case.dart';

void main() {
  final now = DateTime.utc(2026, 1, 2, 9);

  test('merge combines heart and vitals result halves', () {
    final heart = HeartPeriodLoadResult(
      daySamples: [
        HeartRateSample(time: now, beatsPerMinute: 70, source: 'a'),
      ],
      dayRestingBpm: 55,
    );
    final vitals = HeartPeriodLoadResult(
      missingVitalsPermissions: {'x'},
      spO2: [SpO2Entry(time: now, percent: 98, source: 'b')],
    );

    final merged = heart.merge(vitals);

    expect(merged.daySamples, hasLength(1));
    expect(merged.dayRestingBpm, 55);
    expect(merged.spO2, hasLength(1));
    expect(merged.missingVitalsPermissions, {'x'});
  });

  test('merge prefers the left-hand scalar when both are present', () {
    final left = const HeartPeriodLoadResult(dayHrvMs: 40);
    final right = const HeartPeriodLoadResult(dayHrvMs: 99);
    expect(left.merge(right).dayHrvMs, 40);

    final onlyRight = const HeartPeriodLoadResult().merge(
      const HeartPeriodLoadResult(dayHrvMs: 99),
    );
    expect(onlyRight.dayHrvMs, 99);
  });

  test('vitalsSummary picks the latest entry per series', () {
    final earlier = DateTime.utc(2026, 1, 2, 8);
    final later = DateTime.utc(2026, 1, 2, 20);
    final result = HeartPeriodLoadResult(
      spO2: [
        SpO2Entry(time: earlier, percent: 95, source: 's'),
        SpO2Entry(time: later, percent: 99, source: 's'),
      ],
    );

    final summary = result.vitalsSummary();
    expect(summary.hasVitalsData, isTrue);
    expect(summary.latestSpO2?.percent, 99);
    expect(summary.latestBloodPressure, isNull);
  });
}
