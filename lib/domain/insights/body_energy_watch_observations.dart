import 'body_energy_calibration_fit.dart';
import 'body_energy_timeline.dart';

/// A raw watch body-energy sample, before it is paired with the model.
class WatchBodyEnergySample {
  const WatchBodyEnergySample({required this.time, required this.score});
  final DateTime time;

  /// The watch's own 0–100 score.
  final int score;
}

/// Turns raw watch samples into calibration observations against a computed
/// timeline.
///
/// Two jobs, both pure:
///
///  * **Downsample.** The watch emits a sample a minute. Feeding every one in
///    would let a single day outvote months of the user's own check-ins, so at
///    most one per [bucket] is kept. Combined with the small watch learning
///    rate, a day contributes a nudge rather than a shove.
///  * **Pair.** Each kept sample is matched to the nearest timeline point, which
///    already carries both what this app predicted at that moment and the
///    influence that was driving it — the gain a mismatch is attributed to.
///    Reusing the timeline's own `primaryInfluence` rather than re-deriving one
///    keeps a watch correction pointed at exactly the gain a feel-check at that
///    moment would have moved, with the zone and workout context a
///    reconstruction from the point's components alone would lose.
///
/// Samples with no point within [maxPairingGap] are dropped: attributing an
/// error to a gain the model was not exercising at that time would teach it the
/// wrong lesson. Points the model itself could not measure are skipped for the
/// same reason.
List<BodyEnergyWatchReading> buildWatchObservations({
  required List<WatchBodyEnergySample> samples,
  required BodyEnergyTimeline timeline,
  Duration bucket = const Duration(hours: 1),
  Duration maxPairingGap = const Duration(minutes: 30),
}) {
  if (samples.isEmpty || timeline.points.isEmpty) return const [];

  final sorted = [...samples]..sort((a, b) => a.time.compareTo(b.time));

  // One sample per bucket — the first in each, so the choice is deterministic
  // and does not drift with how often the user happens to sync.
  final kept = <WatchBodyEnergySample>[];
  int? currentBucket;
  for (final sample in sorted) {
    final index =
        sample.time.toUtc().millisecondsSinceEpoch ~/ bucket.inMilliseconds;
    if (index == currentBucket) continue;
    currentBucket = index;
    kept.add(sample);
  }

  final readings = <BodyEnergyWatchReading>[];
  for (final sample in kept) {
    final point = _nearestPoint(timeline.points, sample.time, maxPairingGap);
    if (point == null) continue;
    if (point.state == BodyEnergyBucketState.unmeasurable) continue;
    readings.add(BodyEnergyWatchReading(
      time: sample.time,
      observedScore: sample.score.clamp(0, 100),
      predictedScore: point.score,
      dominantInfluence: point.primaryInfluence,
    ));
  }
  return readings;
}

BodyEnergyTimelinePoint? _nearestPoint(
  List<BodyEnergyTimelinePoint> points,
  DateTime time,
  Duration maxGap,
) {
  BodyEnergyTimelinePoint? best;
  var bestGap = maxGap.inMilliseconds + 1;
  for (final point in points) {
    final gap = (point.time.difference(time).inMilliseconds).abs();
    if (gap < bestGap) {
      bestGap = gap;
      best = point;
    }
  }
  return best;
}
