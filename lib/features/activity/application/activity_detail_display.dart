import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/activity_metrics.dart';
import '../../../domain/insights/activity_splits.dart';
import '../../../domain/model/activity_models.dart';
import '../maps/route_geometry.dart';

part 'activity_detail_display.freezed.dart';

/// One point of the session's elevation profile.
typedef ActivityElevationSample = ({DateTime time, double meters});

/// One point of a speed trace derived from the splits.
typedef ActivitySpeedTraceSample = ({DateTime time, double metersPerSecond});

/// Speed over the session for a device that recorded no speed — reconstructed
/// from the splits, which know how far each segment went and how long it took.
///
/// A split's speed holds ACROSS the split (it is an average over a window, not
/// a reading at an instant), so the trace is a STEP: flat for the split's
/// window, and jumping at its boundary. It is meant to look blocky. A smooth
/// curve here would claim a resolution these numbers do not have.
@freezed
abstract class ActivitySplitSpeedTrace with _$ActivitySplitSpeedTrace {
  const factory ActivitySplitSpeedTrace({
    /// Two points per split — its start and its end, at the same speed.
    required List<ActivitySpeedTraceSample> samples,

    /// How many splits are behind the trace. The card counts splits, not
    /// samples: there is no such thing as a sample here.
    required int splitCount,

    /// Total distance over total elapsed, across the splits that are drawn.
    ///
    /// Stated rather than left to the chart, which would take the mean of the
    /// plotted points: with equal-distance splits that is their arithmetic
    /// mean, and average speed over equal distances is the HARMONIC mean — so
    /// the chart would quietly report a slightly faster session than happened,
    /// and disagree with the average speed in the header of the same screen.
    required double averageMetersPerSecond,
  }) = _ActivitySplitSpeedTrace;
}

/// The metric pace bars are scaled per KILOMETRE, whatever the user's units.
/// The scale is a ratio between the activity's own splits, and a ratio does not
/// care which unit its terms are in — but mixing the two would.
const double _paceScaleUnitMeters = 1000.0;

/// The screen-ready derivation of one loaded activity: the figures a workout
/// does not store (paused/moving time), which cadence traces actually recorded
/// anything, the pace scale the splits bars are drawn against, and the route's
/// total distance.
///
/// Built once per load by [buildActivityDetailDisplay] — the detail cards fold
/// nothing.
@freezed
abstract class ActivityDetailDisplay with _$ActivityDetailDisplay {
  const factory ActivityDetailDisplay({
    required int pausedDurationMs,
    required int movingDurationMs,

    /// The cadence kinds the session recorded, in enum order — one card each.
    required List<ActivityCadenceKind> cadenceKinds,

    /// The slowest and fastest split, in seconds per kilometre. Null when no
    /// split has a pace (which is what leaves the bars unpainted).
    required double? slowestSplitPaceSeconds,
    required double? fastestSplitPaceSeconds,

    /// The GPS route's length, in metres. Zero when there is no route.
    required double routeDistanceMeters,

    /// The height profile of the session, oldest first.
    ///
    /// It comes from the ROUTE, not from a record of its own: Health Connect
    /// has no elevation series. `ElevationGainedRecord` is a single total for
    /// the session — it says you climbed 240 m, never where. The altitude on
    /// each route point is the only thing in Health Connect that knows the
    /// shape of a climb, and we already read it.
    ///
    /// Empty when the route has no altitude, or has only one point that does:
    /// a single height is not a profile.
    required List<ActivityElevationSample> elevationSamples,

    /// Speed rebuilt from the splits, for a session that recorded none.
    ///
    /// Null whenever it must not be drawn — see [_splitSpeedTrace] for the two
    /// cases (a real trace exists, or the splits are the estimated kind and are
    /// flat by construction).
    required ActivitySplitSpeedTrace? splitSpeedTrace,
  }) = _ActivityDetailDisplay;
}

/// Pure derivation from a loaded workout (and the splits already cut against the
/// user's split-distance preference) to its display model.
ActivityDetailDisplay buildActivityDetailDisplay({
  required ExerciseData workout,
  required List<ActivityCadenceSample> cadenceSamples,
  required List<SpeedSample> speedSamples,
  required ActivitySplits splits,
}) {
  // The bar scale: the slowest split fills the row. Pace, not distance —
  // a partial split is short but need not be slow.
  final paces = <double>[
    for (final split in splits.splits)
      ?split.paceSecondsPerUnit(_paceScaleUnitMeters),
  ];
  return ActivityDetailDisplay(
    pausedDurationMs: pausedDurationMs(workout),
    movingDurationMs: movingDurationMs(workout),
    cadenceKinds: [
      for (final kind in ActivityCadenceKind.values)
        if (cadenceSamples.any((sample) => sample.kind == kind)) kind,
    ],
    slowestSplitPaceSeconds:
        paces.isEmpty ? null : paces.reduce((a, b) => a > b ? a : b),
    fastestSplitPaceSeconds:
        paces.isEmpty ? null : paces.reduce((a, b) => a < b ? a : b),
    routeDistanceMeters: workout.route.status == ExerciseRouteStatus.data
        ? routeTotalDistanceMeters(workout.route.points)
        : 0.0,
    elevationSamples: _elevationProfile(workout.route),
    splitSpeedTrace: _splitSpeedTrace(
      recordedSpeed: speedSamples,
      splits: splits,
    ),
  );
}

/// Speed rebuilt from the splits — the trace for a session whose device wrote
/// no `SpeedRecord` but whose route (or laps) says how far each segment went
/// and how long it took.
///
/// It refuses in two cases, and the refusals are the point:
///
///  * A RECORDED trace exists. A measurement beats a reconstruction, and two
///    speed cards on one screen disagreeing by a hair would be worse than
///    either alone.
///  * The splits are [SplitSource.estimated] — total distance spread evenly
///    over the duration. Every split then carries the identical average pace by
///    construction, so the "trace" is a horizontal line asserting a metronomic
///    pace nobody measured. That is the same reason the splits card paints no
///    bars for this source, and a line is a stronger claim than a bar.
///
/// One split is also refused: a single average is the number the header already
/// states, and drawing it as a chart adds nothing but the suggestion that it
/// was measured over time.
ActivitySplitSpeedTrace? _splitSpeedTrace({
  required List<SpeedSample> recordedSpeed,
  required ActivitySplits splits,
}) {
  if (recordedSpeed.isNotEmpty) return null;
  if (splits.source == SplitSource.estimated) return null;

  final samples = <ActivitySpeedTraceSample>[];
  var meters = 0.0;
  var seconds = 0.0;
  for (final split in splits.splits) {
    final pace = split.paceSecondsPerMeter;
    // Null for a split that covered no distance or took no time — a paused lap,
    // a lap the device wrote no length for. It has no speed, so it is not drawn.
    if (pace == null || !pace.isFinite || pace <= 0) continue;
    final metersPerSecond = 1.0 / pace;
    if (!metersPerSecond.isFinite) continue;

    // The step: this speed held from here to there, and we do not know when
    // within the split it was faster or slower.
    samples
      ..add((time: split.startTime, metersPerSecond: metersPerSecond))
      ..add((time: split.endTime, metersPerSecond: metersPerSecond));
    meters += split.distanceMeters;
    seconds += split.elapsed.inMicroseconds / Duration.microsecondsPerSecond;
  }

  final splitCount = samples.length ~/ 2;
  if (splitCount < 2 || meters <= 0 || seconds <= 0) return null;
  return ActivitySplitSpeedTrace(
    samples: samples,
    splitCount: splitCount,
    averageMetersPerSecond: meters / seconds,
  );
}

/// The session's height over time, from whichever route points carry an
/// altitude.
///
/// A route may carry altitude on some points and not others — a fix taken
/// indoors, a device that drops it under a poor sky — so the ones without are
/// skipped rather than being read as sea level, which would draw a cliff.
List<ActivityElevationSample> _elevationProfile(ExerciseRouteData route) {
  if (route.status != ExerciseRouteStatus.data) {
    return const <ActivityElevationSample>[];
  }
  final samples = <ActivityElevationSample>[
    for (final point in route.points)
      if (point.altitudeMeters case final meters?) (time: point.time, meters: meters),
  ]..sort((a, b) => a.time.compareTo(b.time));
  // One height is a fact, not a profile. Two is a line.
  return samples.length > 1 ? samples : const <ActivityElevationSample>[];
}
