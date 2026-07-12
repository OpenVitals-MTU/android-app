import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/activity_metrics.dart';
import '../../../domain/insights/activity_splits.dart';
import '../../../domain/model/activity_models.dart';
import '../maps/route_geometry.dart';

part 'activity_detail_display.freezed.dart';

/// One point of the session's elevation profile.
typedef ActivityElevationSample = ({DateTime time, double meters});

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
  }) = _ActivityDetailDisplay;
}

/// Pure derivation from a loaded workout (and the splits already cut against the
/// user's split-distance preference) to its display model.
ActivityDetailDisplay buildActivityDetailDisplay({
  required ExerciseData workout,
  required List<ActivityCadenceSample> cadenceSamples,
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
