import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/heart_rate_aggregated_samples.dart';

void main() {
  test('shouldUseAggregatedHeartRateSamples is true for day ranges', () {
    expect(
      shouldUseAggregatedHeartRateSamples(const Duration(hours: 24)),
      isTrue,
    );
  });

  test('shouldUseAggregatedHeartRateSamples is false for workout-length ranges',
      () {
    expect(
      shouldUseAggregatedHeartRateSamples(const Duration(hours: 2)),
      isFalse,
    );
    expect(
      shouldUseAggregatedHeartRateSamples(heartRateRawSampleMaxRange),
      isFalse,
    );
  });

  test('heartRateSampleFromAggregateBucket maps bucket start and average bpm',
      () {
    final start = DateTime.parse('2026-06-01T08:00:00Z');

    final sample =
        heartRateSampleFromAggregateBucket(startTime: start, avgBpm: 72);

    expect(sample.time == start, isTrue);
    expect(sample.beatsPerMinute == 72, isTrue);
  });
}
