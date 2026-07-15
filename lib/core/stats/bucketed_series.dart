/// A time series aggregated into fixed-width time buckets.
///
/// Generalises the private per-bucket averaging the body-energy timeline does
/// (`_bucketedAverages` in `lib/domain/insights/body_energy_timeline.dart`) into
/// one reusable reducer that also carries each bucket's min and max — everything
/// the aggregated ("Google-Health-style") chart view needs to draw an average
/// line with a min/max band.
library;

/// One bucket: its representative instant (the bucket centre) and the average,
/// minimum and maximum of the samples that fell in it.
typedef BucketPoint = ({
  DateTime time,
  double average,
  double min,
  double max,
  int count,
});

class _BucketAcc {
  double sum = 0;
  int count = 0;
  double min = double.infinity;
  double max = double.negativeInfinity;

  void add(double value) {
    sum += value;
    count += 1;
    if (value < min) min = value;
    if (value > max) max = value;
  }
}

/// Buckets [samples] into [bucketMinutes]-wide windows measured from [dayStart],
/// returning one [BucketPoint] per non-empty bucket in ascending time order.
///
/// Samples before [dayStart] and non-finite values are skipped. Buckets are
/// aligned to [dayStart] so their boundaries are stable across refreshes. Returns
/// an empty list when [bucketMinutes] <= 0 or there is nothing to aggregate.
List<BucketPoint> bucketedSeries<T>(
  Iterable<T> samples, {
  required int bucketMinutes,
  required DateTime dayStart,
  required DateTime Function(T) time,
  required double Function(T) value,
}) {
  if (bucketMinutes <= 0) return const [];

  final buckets = <int, _BucketAcc>{};
  for (final sample in samples) {
    final minutesFromStart = time(sample).difference(dayStart).inMinutes;
    if (minutesFromStart < 0) continue;
    final v = value(sample);
    if (!v.isFinite) continue;
    final index = minutesFromStart ~/ bucketMinutes;
    (buckets[index] ??= _BucketAcc()).add(v);
  }
  if (buckets.isEmpty) return const [];

  final indices = buckets.keys.toList()..sort();
  return [
    for (final index in indices)
      () {
        final acc = buckets[index]!;
        // Centre of the bucket, so the point sits in the middle of the window it
        // summarises rather than at its leading edge.
        final centre = dayStart.add(
          Duration(minutes: index * bucketMinutes, seconds: bucketMinutes * 30),
        );
        return (
          time: centre,
          average: acc.sum / acc.count,
          min: acc.min,
          max: acc.max,
          count: acc.count,
        );
      }(),
  ];
}
