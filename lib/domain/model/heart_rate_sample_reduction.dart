import 'dart:math' as math;

import 'heart_models.dart';

/// Upper bound for day-view heart rate charts and in-memory use.
const int maxHeartRateChartSamples = 2500;

extension HeartRateSampleReduction on List<HeartRateSample> {
  /// Reduces high-frequency heart rate samples to a chart-friendly count while
  /// preserving the overall day shape via bucket averaging.
  List<HeartRateSample> reducedForChart({
    int maxSamples = maxHeartRateChartSamples,
  }) {
    if (length <= maxSamples) return this;
    final sorted = [...this]..sort((a, b) => a.time.compareTo(b.time));
    final bucketSize = (sorted.length + maxSamples - 1) ~/ maxSamples;
    final result = <HeartRateSample>[];
    for (var start = 0; start < sorted.length; start += bucketSize) {
      final end = math.min(start + bucketSize, sorted.length);
      final bucket = sorted.sublist(start, end);
      if (bucket.isEmpty) continue;
      final midIndex = bucket.length ~/ 2;
      final representative = bucket[midIndex];
      final average =
          bucket.map((sample) => sample.beatsPerMinute).reduce((a, b) => a + b) /
              bucket.length;
      result.add(
        HeartRateSample(
          time: representative.time,
          beatsPerMinute: average.round(),
          source: representative.source,
        ),
      );
    }
    return result;
  }
}
