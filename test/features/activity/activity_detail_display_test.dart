import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/activity_splits.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/activity/application/activity_detail_display.dart';

/// The derivations the detail cards used to run in their build paths — the
/// paused/moving split of the session, which cadence traces exist, and the pace
/// scale the split bars are drawn against — as one pure function.
final _start = DateTime(2026, 3, 2, 7);

ExerciseData _workout({
  Duration duration = const Duration(minutes: 40),
  List<ExerciseSegmentData> segments = const <ExerciseSegmentData>[],
  ExerciseRouteData route = const ExerciseRouteData(),
}) =>
    ExerciseData(
      id: 'w1',
      title: null,
      exerciseType: 56,
      startTime: _start,
      endTime: _start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'test',
      segments: segments,
      route: route,
    );

/// A route whose points carry [altitudes] — null for a point the device gave no
/// height for.
ExerciseRouteData _route(List<double?> altitudes) => ExerciseRouteData(
      status: ExerciseRouteStatus.data,
      points: [
        for (final (index, altitude) in altitudes.indexed)
          ExerciseRoutePoint(
            time: _start.add(Duration(minutes: index)),
            latitude: 59.43 + index * 0.001,
            longitude: 24.75,
            altitudeMeters: altitude,
            horizontalAccuracyMeters: null,
            verticalAccuracyMeters: null,
          ),
      ],
    );

ActivitySplit _split(int index, double meters, Duration elapsed) => ActivitySplit(
      index: index,
      distanceMeters: meters,
      elapsed: elapsed,
      startTime: _start,
      endTime: _start.add(elapsed),
      isPartial: false,
    );

ActivityCadenceSample _cadence(ActivityCadenceKind kind) => ActivityCadenceSample(
      time: _start,
      rate: 80,
      kind: kind,
      source: 'test',
    );

void main() {
  test('a pause segment splits the session into paused and moving time', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(segments: [
        ExerciseSegmentData(
          startTime: _start.add(const Duration(minutes: 5)),
          endTime: _start.add(const Duration(minutes: 15)),
          segmentType: ExerciseSegmentType.pause,
          repetitions: 0,
        ),
      ]),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.pausedDurationMs, const Duration(minutes: 10).inMilliseconds);
    expect(display.movingDurationMs, const Duration(minutes: 30).inMilliseconds);
  });

  test('an unpaused session is all moving time', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.pausedDurationMs, 0);
    expect(display.movingDurationMs, const Duration(minutes: 40).inMilliseconds);
  });

  test('only the cadence kinds that recorded something get a card', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: [_cadence(ActivityCadenceKind.cycling)],
      splits: const ActivitySplits.none(),
    );

    expect(display.cadenceKinds, [ActivityCadenceKind.cycling]);

    final none = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: const ActivitySplits.none(),
    );
    expect(none.cadenceKinds, isEmpty);
  });

  test('the pace scale is the slowest and fastest split, per kilometre', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: ActivitySplits(
        source: SplitSource.route,
        splits: [
          // 1 km in 5:00, then 1 km in 6:00.
          _split(1, 1000, const Duration(minutes: 5)),
          _split(2, 1000, const Duration(minutes: 6)),
        ],
      ),
    );

    expect(display.fastestSplitPaceSeconds, 300.0);
    expect(display.slowestSplitPaceSeconds, 360.0);
  });

  test('a split with no distance leaves the scale unset, not zeroed', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: ActivitySplits(
        source: SplitSource.estimated,
        splits: [_split(1, 0, const Duration(minutes: 5))],
      ),
    );

    expect(display.fastestSplitPaceSeconds, isNull);
    expect(display.slowestSplitPaceSeconds, isNull);
  });

  test('a workout with no route has no route distance', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.routeDistanceMeters, 0.0);
  });

  group('the elevation profile', () {
    // Health Connect has no elevation SERIES. ElevationGainedRecord is one
    // total for the session — it says you climbed 240 m, never where. The
    // altitudes on the route are the only thing that knows the shape of a
    // climb, so that is what the profile is drawn from.
    test('comes from the route altitudes, oldest first', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(route: _route([120.0, 145.5, 132.0])),
        cadenceSamples: const [],
        splits: const ActivitySplits.none(),
      );

      expect(display.elevationSamples, hasLength(3));
      expect([for (final s in display.elevationSamples) s.meters],
          [120.0, 145.5, 132.0]);
      expect(
        display.elevationSamples.first.time
            .isBefore(display.elevationSamples.last.time),
        isTrue,
      );
    });

    test('skips the points the device gave no height for', () {
      // A fix taken indoors, or under a poor sky, carries no altitude. Reading
      // that as sea level would draw a cliff that never happened.
      final display = buildActivityDetailDisplay(
        workout: _workout(route: _route([120.0, null, 132.0])),
        cadenceSamples: const [],
        splits: const ActivitySplits.none(),
      );

      expect([for (final s in display.elevationSamples) s.meters], [120.0, 132.0]);
    });

    test('one height is not a profile', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(route: _route([120.0, null, null])),
        cadenceSamples: const [],
        splits: const ActivitySplits.none(),
      );

      // A single point draws no line, and a card with no line is worse than no
      // card. The screen renders nothing.
      expect(display.elevationSamples, isEmpty);
    });

    test('a route with no altitude at all has no profile', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(route: _route([null, null])),
        cadenceSamples: const [],
        splits: const ActivitySplits.none(),
      );

      expect(display.elevationSamples, isEmpty);
    });

    test('an activity with no route has no profile', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(),
        cadenceSamples: const [],
        splits: const ActivitySplits.none(),
      );

      expect(display.elevationSamples, isEmpty);
    });
  });
}