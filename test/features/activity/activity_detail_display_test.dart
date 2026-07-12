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

/// Consecutive splits, each starting where the last one ended — a real cut of a
/// session, unlike [_split], which stacks them all at the session start.
List<ActivitySplit> _consecutive(List<(double, Duration)> legs) {
  final splits = <ActivitySplit>[];
  var start = _start;
  for (final (index, (meters, elapsed)) in legs.indexed) {
    splits.add(ActivitySplit(
      index: index + 1,
      distanceMeters: meters,
      elapsed: elapsed,
      startTime: start,
      endTime: start.add(elapsed),
      isPartial: false,
    ));
    start = start.add(elapsed);
  }
  return splits;
}

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
      speedSamples: const <SpeedSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.pausedDurationMs, const Duration(minutes: 10).inMilliseconds);
    expect(display.movingDurationMs, const Duration(minutes: 30).inMilliseconds);
  });

  test('an unpaused session is all moving time', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      speedSamples: const <SpeedSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.pausedDurationMs, 0);
    expect(display.movingDurationMs, const Duration(minutes: 40).inMilliseconds);
  });

  test('only the cadence kinds that recorded something get a card', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: [_cadence(ActivityCadenceKind.cycling)],
      speedSamples: const <SpeedSample>[],
      splits: const ActivitySplits.none(),
    );

    expect(display.cadenceKinds, [ActivityCadenceKind.cycling]);

    final none = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      speedSamples: const <SpeedSample>[],
      splits: const ActivitySplits.none(),
    );
    expect(none.cadenceKinds, isEmpty);
  });

  test('the pace scale is the slowest and fastest split, per kilometre', () {
    final display = buildActivityDetailDisplay(
      workout: _workout(),
      cadenceSamples: const <ActivityCadenceSample>[],
      speedSamples: const <SpeedSample>[],
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
      speedSamples: const <SpeedSample>[],
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
      speedSamples: const <SpeedSample>[],
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
        speedSamples: const <SpeedSample>[],
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
        speedSamples: const <SpeedSample>[],
        splits: const ActivitySplits.none(),
      );

      expect([for (final s in display.elevationSamples) s.meters], [120.0, 132.0]);
    });

    test('one height is not a profile', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(route: _route([120.0, null, null])),
        cadenceSamples: const [],
        speedSamples: const <SpeedSample>[],
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
        speedSamples: const <SpeedSample>[],
        splits: const ActivitySplits.none(),
      );

      expect(display.elevationSamples, isEmpty);
    });

    test('an activity with no route has no profile', () {
      final display = buildActivityDetailDisplay(
        workout: _workout(),
        cadenceSamples: const [],
        speedSamples: const <SpeedSample>[],
        splits: const ActivitySplits.none(),
      );

      expect(display.elevationSamples, isEmpty);
    });
  });

  group('speed rebuilt from the splits', () {
    // Most watches write a route and a distance but no SpeedRecord, so the
    // speed card never appeared for them — while the splits card, sitting right
    // above it, knew each segment's distance and duration all along.
    ActivityDetailDisplay displayOf({
      required ActivitySplits splits,
      List<SpeedSample> recorded = const <SpeedSample>[],
    }) =>
        buildActivityDetailDisplay(
          workout: _workout(),
          cadenceSamples: const <ActivityCadenceSample>[],
          speedSamples: recorded,
          splits: splits,
        );

    // 1 km in 5:00 = 3.33 m/s, then 1 km in 6:00 = 2.78 m/s.
    final twoSplits = ActivitySplits(
      source: SplitSource.route,
      splits: _consecutive([
        (1000.0, const Duration(minutes: 5)),
        (1000.0, const Duration(minutes: 6)),
      ]),
    );

    test('a split holds its speed across its window: the trace is a step', () {
      final trace = displayOf(splits: twoSplits).splitSpeedTrace!;

      // Two points per split, at the same height: flat from here to there. A
      // split's speed is an average over a window, not a reading at an instant,
      // and a smooth curve would claim a resolution it does not have.
      expect(trace.samples, hasLength(4));
      expect(trace.splitCount, 2);
      expect(trace.samples[0].metersPerSecond, closeTo(1000 / 300, 0.001));
      expect(trace.samples[1].metersPerSecond, closeTo(1000 / 300, 0.001));
      expect(trace.samples[2].metersPerSecond, closeTo(1000 / 360, 0.001));
      expect(trace.samples[3].metersPerSecond, closeTo(1000 / 360, 0.001));
      // And it steps at the boundary: the first split's end is the second's
      // start, one instant carrying both speeds.
      expect(trace.samples[1].time, trace.samples[2].time);
      expect(
        trace.samples.first.time.isBefore(trace.samples.last.time),
        isTrue,
      );
    });

    test('the average is distance over time — NOT the mean of the plotted '
        'points', () {
      final trace = displayOf(splits: twoSplits).splitSpeedTrace!;

      // 2 km in 11:00. The chart would otherwise average its own samples, and
      // the mean of two equal-DISTANCE splits' speeds is their arithmetic mean
      // (3.06 m/s) where the truth is the harmonic one (3.03 m/s) — a session
      // reported very slightly faster than it was run, contradicting the
      // average speed in the header of the same screen.
      expect(trace.averageMetersPerSecond, closeTo(2000 / 660, 0.0001));
      final arithmetic = (1000 / 300 + 1000 / 360) / 2;
      expect(trace.averageMetersPerSecond, lessThan(arithmetic));
    });

    test('a recorded trace wins — a measurement beats a reconstruction', () {
      final display = displayOf(
        splits: twoSplits,
        recorded: [
          SpeedSample(
            time: _start,
            metersPerSecond: 3.1,
            source: 'test',
          ),
        ],
      );

      // Two speed cards on one screen, disagreeing by a hair, would be worse
      // than either alone.
      expect(display.splitSpeedTrace, isNull);
    });

    test('estimated splits draw NOTHING: they are flat by construction', () {
      // The estimated source spreads the total distance evenly over the
      // duration, so every split necessarily carries the activity's average
      // pace. A line through them would assert a metronomic pace nobody
      // measured — the same reason the splits card paints no bars for it, and a
      // line is a stronger claim than a bar.
      final display = displayOf(
        splits: ActivitySplits(
          source: SplitSource.estimated,
          splits: _consecutive([
            (1000.0, const Duration(minutes: 5)),
            (1000.0, const Duration(minutes: 5)),
          ]),
        ),
      );

      expect(display.splitSpeedTrace, isNull);
    });

    test('one split is an average, not a trace', () {
      final display = displayOf(
        splits: ActivitySplits(
          source: SplitSource.route,
          splits: _consecutive([(1000.0, const Duration(minutes: 5))]),
        ),
      );

      // It is the number the header already states. Drawing it as a chart adds
      // nothing but the suggestion that it was measured over time.
      expect(display.splitSpeedTrace, isNull);
    });

    test('device laps get a trace too', () {
      final display = displayOf(
        splits: ActivitySplits(
          source: SplitSource.deviceLaps,
          splits: _consecutive([
            (400.0, const Duration(seconds: 90)),
            (400.0, const Duration(seconds: 95)),
          ]),
        ),
      );

      expect(display.splitSpeedTrace!.splitCount, 2);
    });

    test('a lap with no distance or no time is skipped, not drawn at zero', () {
      // A paused lap, or a lap the device wrote no length for, has no speed —
      // and no speed is not zero speed, which would draw a plunge to the floor.
      final display = displayOf(
        splits: ActivitySplits(
          source: SplitSource.deviceLaps,
          splits: _consecutive([
            (400.0, const Duration(seconds: 90)),
            (0.0, const Duration(seconds: 30)),
            (400.0, const Duration(seconds: 95)),
          ]),
        ),
      );

      final trace = display.splitSpeedTrace!;
      expect(trace.splitCount, 2);
      expect(trace.samples, hasLength(4));
      // The skipped lap is out of the average as well: 800 m over 185 s, not
      // over the 215 s the stopped clock ran for.
      expect(trace.averageMetersPerSecond, closeTo(800 / 185, 0.0001));
    });
  });
}