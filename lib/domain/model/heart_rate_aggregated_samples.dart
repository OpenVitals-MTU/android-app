import 'heart_models.dart';

/// Buckets used when Health Connect aggregates heart rate for day/long-range
/// charts.
const Duration heartRateChartBucketDuration = Duration(minutes: 15);

/// Workout-length ranges keep raw samples for finer chart resolution.
const Duration heartRateRawSampleMaxRange = Duration(hours: 4);

bool shouldUseAggregatedHeartRateSamples(Duration range) =>
    range > heartRateRawSampleMaxRange;

HeartRateSample heartRateSampleFromAggregateBucket({
  required DateTime startTime,
  required int avgBpm,
}) =>
    HeartRateSample(
      time: startTime,
      beatsPerMinute: avgBpm,
      source: '',
    );
