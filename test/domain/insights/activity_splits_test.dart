import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/geo/geo_distance.dart';
import 'package:openvitals/domain/insights/activity_splits.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';

/// `computeActivitySplits` is arithmetic, so it is tested like arithmetic:
/// known geometry in, exact numbers out.

final DateTime _start = DateTime.utc(2026, 7, 10, 8);

DateTime _at(num seconds) =>
    _start.add(Duration(microseconds: (seconds * 1e6).round()));

/// A latitude line: at the equator, moving EAST by `meters` keeps latitude 0 and
/// makes the haversine distance exactly `meters`, so a route's geometry can be
/// stated in meters directly.
///
/// This must use the SAME earth radius as `haversineMeters` (6 371 000 m, a
/// sphere) — the WGS84 equatorial radius would put the fixture 0.11% out and
/// quietly turn every "1000 m" below into 998.9 m.
const double _metersPerDegreeAtEquator = 6371000.0 * 3.141592653589793 / 180.0;

ExerciseRoutePoint _point(
  num atSeconds,
  double eastMeters, {
  double? altitudeMeters,
}) =>
    ExerciseRoutePoint(
      time: _at(atSeconds),
      latitude: 0.0,
      longitude: eastMeters / _metersPerDegreeAtEquator,
      altitudeMeters: altitudeMeters,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );

ExerciseData _workout({
  double? totalDistanceMeters,
  int? durationMs,
  List<ExerciseRoutePoint> routePoints = const <ExerciseRoutePoint>[],
  List<ExerciseLapData> laps = const <ExerciseLapData>[],
  DateTime? endTime,
  int exerciseType = 56, // running
}) {
  final end = endTime ?? _at(600);
  return ExerciseData(
    id: 'w1',
    title: 'Run',
    exerciseType: exerciseType,
    startTime: _start,
    endTime: end,
    durationMs: durationMs ?? end.difference(_start).inMilliseconds,
    source: 'test',
    totalDistanceMeters: totalDistanceMeters,
    laps: laps,
    route: routePoints.isEmpty
        ? const ExerciseRouteData()
        : ExerciseRouteData(
            status: ExerciseRouteStatus.data,
            points: routePoints,
          ),
  );
}

HeartRateSample _hr(num atSeconds, int bpm) =>
    HeartRateSample(time: _at(atSeconds), beatsPerMinute: bpm, source: 'test');

SpeedSample _speed(num atSeconds, double metersPerSecond) => SpeedSample(
      time: _at(atSeconds),
      metersPerSecond: metersPerSecond,
      source: 'test',
    );

ActivitySplits _compute(
  ExerciseData workout, {
  List<HeartRateSample> heartRates = const <HeartRateSample>[],
  List<SpeedSample> speeds = const <SpeedSample>[],
  double splitDistanceMeters = 1000.0,
}) =>
    computeActivitySplits(
      workout: workout,
      heartRateSamples: heartRates,
      speedSamples: speeds,
      splitDistanceMeters: splitDistanceMeters,
    );

double _elapsedSeconds(ActivitySplit split) =>
    split.elapsed.inMicroseconds / Duration.microsecondsPerSecond;

/// Seconds since the workout start. Interpolated crossings land on
/// sub-microsecond boundaries (the haversine of a "1000 m" fixture segment is
/// 1000 m to about eleven decimal places, not exactly), so split times are
/// asserted with a tolerance rather than by DateTime equality.
double _secondsFromStart(DateTime time) =>
    time.difference(_start).inMicroseconds / Duration.microsecondsPerSecond;

void main() {
  group('an activity that does not travel has no splits', () {
    test('a strength session with GPS drift is not cut into "laps"', () {
      // The bug, from a real session: a phone left on the bench picked up 1.2 km
      // of GPS drift over 36 minutes. Health Connect recorded it faithfully, the
      // old "does it have any distance?" gate said yes, and a lifting session was
      // duly cut into a "1.0 km" split and a "181 m (partial)" one, both at a
      // 30:29 min/km pace. The distance was real data; the splits were nonsense.
      final splits = _compute(
        _workout(
          exerciseType: ExerciseSessionType.strengthTraining,
          totalDistanceMeters: 1200,
          endTime: _at(2160), // 36 minutes
        ),
      );

      expect(splits.splits, isEmpty);
      expect(splits.isEmpty, isTrue);
    });

    test('...and neither is one carrying a route it never meant to record', () {
      final splits = _compute(
        _workout(
          exerciseType: ExerciseSessionType.strengthTraining,
          // ~1.2 km of "movement" — exactly the drift a bench-side phone logs.
          routePoints: [_point(0, 0), _point(2160, 1200)],
        ),
      );
      expect(splits.splits, isEmpty);
    });

    test('a run with the same distance IS cut, so the gate is on the KIND', () {
      final splits = _compute(
        _workout(
          exerciseType: ExerciseSessionType.running,
          totalDistanceMeters: 1200,
          endTime: _at(2160),
        ),
      );
      expect(splits.splits, isNotEmpty);
    });
  });

  group('the equator-line fixture', () {
    test('really does put the requested number of meters between fixes', () {
      // Everything below reads distance off this; if the fixture lies, the
      // whole file lies.
      final a = _point(0, 0);
      final b = _point(10, 1000);
      expect(
        haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude),
        closeTo(1000.0, 0.01),
      );
    });
  });

  group('source priority', () {
    test('device laps win over a route, and are NOT re-cut to the split '
        'distance', () {
      // A track session: 400 m laps, recorded by the watch. A 1 km splitter
      // would produce ~1.6 splits from the same route — the device's laps must
      // survive untouched.
      final workout = _workout(
        totalDistanceMeters: 1600,
        endTime: _at(480),
        routePoints: [
          for (var i = 0; i <= 16; i++) _point(i * 30, i * 100.0),
        ],
        laps: [
          for (var lap = 0; lap < 4; lap++)
            ExerciseLapData(
              startTime: _at(lap * 120),
              endTime: _at((lap + 1) * 120),
              lengthMeters: 400,
            ),
        ],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.deviceLaps);
      expect(result.splits, hasLength(4));
      expect(
        result.splits.map((s) => s.distanceMeters),
        everyElement(closeTo(400.0, 0.001)),
      );
      expect(result.splits.map((s) => s.index), [1, 2, 3, 4]);
      // An uneven lap is not a partial one.
      expect(result.splits.map((s) => s.isPartial), everyElement(isFalse));
    });

    test('uneven device laps keep their own lengths', () {
      final workout = _workout(
        totalDistanceMeters: 1500,
        endTime: _at(600),
        laps: [
          ExerciseLapData(
            startTime: _at(0),
            endTime: _at(200),
            lengthMeters: 800,
          ),
          ExerciseLapData(
            startTime: _at(200),
            endTime: _at(600),
            lengthMeters: 700,
          ),
        ],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.deviceLaps);
      expect(result.splits.map((s) => s.distanceMeters), [800.0, 700.0]);
    });

    test('a route beats speed samples', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [
          for (var i = 0; i <= 20; i++) _point(i * 30, i * 100.0),
        ],
      );

      final result = _compute(
        workout,
        speeds: [for (var i = 0; i <= 10; i++) _speed(i * 60, 3.0)],
      );

      expect(result.source, SplitSource.route);
    });
  });

  group('route splits', () {
    test('cut at exactly the right distance, with the crossing time '
        'INTERPOLATED between fixes', () {
      // A deliberately hostile sample rate: one fix every 200 s. The runner
      // covers 900 m in the first 200 s, then slows hard — the 1 km boundary
      // falls just 100 m into a 1000 m segment that takes 600 s.
      //
      //   fix 0:   t=0 s      0 m
      //   fix 1:   t=200 s    900 m
      //   fix 2:   t=800 s    1900 m
      //
      // The 1 km mark is 10% of the way along the second segment, so it is
      // crossed at t = 200 + 0.10 * 600 = 260 s.
      //
      // SNAPPING to the next fix would call split 1 "1000 m in 800 s" — a
      // 13:20 min/km pace for a kilometre actually run in 4:20. That is the
      // whole reason for interpolating.
      final workout = _workout(
        totalDistanceMeters: 1900,
        endTime: _at(800),
        routePoints: [
          _point(0, 0),
          _point(200, 900),
          _point(800, 1900),
        ],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.route);
      expect(result.splits, hasLength(2));

      final first = result.splits.first;
      expect(first.distanceMeters, closeTo(1000.0, 0.01));
      expect(_elapsedSeconds(first), closeTo(260.0, 0.5));
      expect(_secondsFromStart(first.endTime), closeTo(260, 0.05));
      expect(first.isPartial, isFalse);
      // Not the snapped-to-the-next-fix answer.
      expect(_elapsedSeconds(first), isNot(closeTo(800.0, 1.0)));

      final second = result.splits[1];
      expect(second.distanceMeters, closeTo(900.0, 0.01));
      expect(_secondsFromStart(second.startTime), closeTo(260, 0.05));
      expect(_elapsedSeconds(second), closeTo(540.0, 0.5));
      expect(second.isPartial, isTrue);
    });

    test('several boundaries inside one long segment are each interpolated', () {
      // Two fixes, 3 km apart, 300 s apart: constant 10 m/s. Splits must land
      // at 100 s, 200 s, 300 s.
      final workout = _workout(
        totalDistanceMeters: 3000,
        endTime: _at(300),
        routePoints: [_point(0, 0), _point(300, 3000)],
      );

      final result = _compute(workout);

      expect(result.splits, hasLength(3));
      expect(_secondsFromStart(result.splits[0].endTime), closeTo(100, 0.05));
      expect(_secondsFromStart(result.splits[1].endTime), closeTo(200, 0.05));
      expect(_secondsFromStart(result.splits[2].endTime), closeTo(300, 0.05));
      expect(
        result.splits.map((s) => s.distanceMeters),
        everyElement(closeTo(1000.0, 0.01)),
      );
      expect(result.splits.map((s) => s.isPartial), everyElement(isFalse));
    });

    test('a custom split distance (5 km, the cyclist case) is honoured', () {
      final workout = _workout(
        totalDistanceMeters: 12000,
        endTime: _at(1200),
        routePoints: [_point(0, 0), _point(1200, 12000)],
      );

      final result = _compute(workout, splitDistanceMeters: 5000);

      expect(result.splits.map((s) => s.distanceMeters), [
        closeTo(5000, 0.01),
        closeTo(5000, 0.01),
        closeTo(2000, 0.01),
      ]);
      expect(result.splits.map((s) => s.isPartial), [false, false, true]);
    });

    test('a trailing partial split is kept and flagged', () {
      final workout = _workout(
        totalDistanceMeters: 1400,
        endTime: _at(420),
        routePoints: [_point(0, 0), _point(420, 1400)],
      );

      final result = _compute(workout);

      expect(result.splits, hasLength(2));
      expect(result.splits.last.isPartial, isTrue);
      expect(result.splits.last.distanceMeters, closeTo(400.0, 0.01));
      expect(_elapsedSeconds(result.splits.last), closeTo(120.0, 0.5));
    });

    test('elevation gain and loss come from the route altitudes', () {
      // Up 20 m over the first km, back down 8 m over the second.
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [
          _point(0, 0, altitudeMeters: 100),
          _point(150, 500, altitudeMeters: 110),
          _point(300, 1000, altitudeMeters: 120),
          _point(450, 1500, altitudeMeters: 116),
          _point(600, 2000, altitudeMeters: 112),
        ],
      );

      final result = _compute(workout);

      expect(result.splits[0].elevationGainMeters, closeTo(20.0, 0.01));
      expect(result.splits[0].elevationLossMeters, closeTo(0.0, 0.01));
      expect(result.splits[1].elevationGainMeters, closeTo(0.0, 0.01));
      expect(result.splits[1].elevationLossMeters, closeTo(8.0, 0.01));
    });

    test('altitude at a mid-segment boundary is interpolated, and no interior '
        'fix is lost from the next split', () {
      // Boundary at 1 km falls halfway between the 750 m fix (110 m) and the
      // 1250 m fix (130 m) -> 120 m. Split 1 gains 20 m, split 2 gains 10 m.
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [
          _point(0, 0, altitudeMeters: 100),
          _point(225, 750, altitudeMeters: 110),
          _point(375, 1250, altitudeMeters: 130),
          _point(600, 2000, altitudeMeters: 130),
        ],
      );

      final result = _compute(workout);

      expect(result.splits[0].elevationGainMeters, closeTo(20.0, 0.05));
      // 120 m at the boundary -> 130 m at the 1250 m fix -> flat to the end.
      expect(result.splits[1].elevationGainMeters, closeTo(10.0, 0.05));
      expect(result.splits[1].elevationLossMeters, closeTo(0.0, 0.05));
    });

    test('a route without altitudes reports null elevation, not zero', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [_point(0, 0), _point(600, 2000)],
      );

      final result = _compute(workout);

      expect(result.splits.first.elevationGainMeters, isNull);
      expect(result.splits.first.elevationLossMeters, isNull);
    });
  });

  group('speed-sample splits (the treadmill case)', () {
    test('integrate v.dt to cut at the right times, with no route at all', () {
      // A treadmill at a constant 4 m/s: the 1 km mark falls at 250 s, 2 km at
      // 500 s. Samples every 100 s, so both boundaries are mid-sample and must
      // be interpolated.
      final workout = _workout(
        totalDistanceMeters: 2400,
        endTime: _at(600),
      );

      final result = _compute(
        workout,
        speeds: [for (var i = 0; i <= 6; i++) _speed(i * 100, 4.0)],
      );

      expect(result.source, SplitSource.speedSamples);
      expect(result.splits, hasLength(3));
      expect(_secondsFromStart(result.splits[0].endTime), closeTo(250, 0.05));
      expect(_secondsFromStart(result.splits[1].endTime), closeTo(500, 0.05));
      expect(result.splits[0].distanceMeters, closeTo(1000.0, 0.01));
      expect(result.splits[1].distanceMeters, closeTo(1000.0, 0.01));
      expect(result.splits[2].distanceMeters, closeTo(400.0, 0.01));
      expect(result.splits[2].isPartial, isTrue);
      // No route -> no elevation claim.
      expect(result.splits[0].elevationGainMeters, isNull);
      expect(result.splits[0].elevationLossMeters, isNull);
    });

    test('a changing belt speed integrates trapezoidally', () {
      // 0-200 s at 5 m/s (1000 m) then 200-400 s at 2.5 m/s (500 m).
      // The 1 km boundary lands exactly on the 200 s sample.
      final workout = _workout(
        totalDistanceMeters: 1500,
        endTime: _at(400),
      );

      final result = _compute(
        workout,
        speeds: [
          _speed(0, 5.0),
          _speed(100, 5.0),
          _speed(200, 5.0),
          _speed(300, 2.5),
          _speed(400, 2.5),
        ],
      );

      expect(result.source, SplitSource.speedSamples);
      expect(result.splits, hasLength(2));
      expect(result.splits[0].distanceMeters, closeTo(1000.0, 0.01));
      expect(_secondsFromStart(result.splits[0].endTime), closeTo(200, 0.05));
      // The 200-300 s ramp averages 3.75 m/s = 375 m, plus 250 m to the end.
      expect(result.splits[1].distanceMeters, closeTo(625.0, 0.01));
      expect(result.splits[1].isPartial, isTrue);
    });
  });

  group('the estimated fallback', () {
    test('every split shares the activity average pace, and the source says '
        'so', () {
      // 5 km in 25 min, nothing else: no laps, no route, no speed samples.
      final workout = _workout(
        totalDistanceMeters: 5000,
        endTime: _at(1500),
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.estimated);
      expect(result.splits, hasLength(5));
      for (final split in result.splits) {
        expect(split.distanceMeters, closeTo(1000.0, 0.001));
        expect(_elapsedSeconds(split), closeTo(300.0, 0.001));
        // Identical pace on every row: the honest, useless answer.
        expect(split.paceSecondsPerMeter, closeTo(0.3, 1e-6));
        expect(split.paceDeltaSeconds, closeTo(0.0, 1e-6));
        expect(split.elevationGainMeters, isNull);
      }
      expect(result.splits.last.isPartial, isFalse);
    });

    test('an odd total distance still yields a flagged trailing partial', () {
      final workout = _workout(
        totalDistanceMeters: 2500,
        endTime: _at(1000),
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.estimated);
      expect(result.splits.map((s) => s.distanceMeters), [1000.0, 1000.0, 500.0]);
      expect(result.splits.map((s) => s.isPartial), [false, false, true]);
      // Evenly spread in time: 400 s + 400 s + 200 s.
      expect(_elapsedSeconds(result.splits[0]), closeTo(400.0, 0.001));
      expect(_elapsedSeconds(result.splits[2]), closeTo(200.0, 0.001));
    });

    test('a single speed sample cannot be integrated, so it falls back to '
        'estimated', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
      );

      final result = _compute(workout, speeds: [_speed(10, 3.5)]);

      expect(result.source, SplitSource.estimated);
      expect(result.splits, hasLength(2));
    });
  });

  group('average heart rate', () {
    test('covers only the samples inside the split window', () {
      // Two 1 km splits, each 300 s.
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [_point(0, 0), _point(600, 2000)],
      );

      final result = _compute(
        workout,
        heartRates: [
          _hr(10, 100), // split 1
          _hr(150, 110), // split 1
          _hr(290, 120), // split 1
          _hr(310, 160), // split 2
          _hr(500, 170), // split 2
        ],
      );

      expect(result.splits[0].averageHeartRateBpm, 110); // (100+110+120)/3
      expect(result.splits[1].averageHeartRateBpm, 165); // (160+170)/2
    });

    test('the split window is half-open: a sample exactly on the boundary '
        'belongs to the NEXT split, never to both', () {
      // The estimated source cuts on exact times (300 s / 600 s), so the
      // boundary can be probed to the microsecond — unlike an interpolated GPS
      // crossing, which lands a hair either side of the round number.
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
      );

      final result = _compute(
        workout,
        heartRates: [
          _hr(0, 100), // split 1: the start IS included
          _hr(300, 160), // exactly on the boundary -> split 2
          _hr(400, 170), // split 2
        ],
      );

      expect(result.source, SplitSource.estimated);
      expect(result.splits[0].averageHeartRateBpm, 100);
      expect(result.splits[1].averageHeartRateBpm, 165); // (160+170)/2
    });

    test('is null, not zero, when no sample falls inside the split', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [_point(0, 0), _point(600, 2000)],
      );

      final result = _compute(workout, heartRates: [_hr(10, 130)]);

      expect(result.splits[0].averageHeartRateBpm, 130);
      expect(result.splits[1].averageHeartRateBpm, isNull);
    });

    test('unsorted heart-rate samples are still bucketed correctly', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [_point(0, 0), _point(600, 2000)],
      );

      final result = _compute(
        workout,
        heartRates: [_hr(500, 170), _hr(0, 100), _hr(400, 150), _hr(100, 120)],
      );

      expect(result.splits[0].averageHeartRateBpm, 110); // (100+120)/2
      expect(result.splits[1].averageHeartRateBpm, 160); // (150+170)/2
    });
  });

  group('paceDeltaSeconds', () {
    test('is negative for a faster split and positive for a slower one', () {
      // 2 km in 600 s -> the activity averages 300 s/km. The first km takes
      // 240 s (60 s faster), the second 360 s (60 s slower).
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [
          _point(0, 0),
          _point(240, 1000),
          _point(600, 2000),
        ],
      );

      final result = _compute(workout);

      expect(result.splits[0].paceDeltaSeconds, closeTo(-60.0, 0.5));
      expect(result.splits[1].paceDeltaSeconds, closeTo(60.0, 0.5));
      // Same numbers, re-expressed per mile at the display boundary.
      expect(
        result.splits[0].paceDeltaSecondsPerUnit(1609.344),
        closeTo(-96.56, 0.5),
      );
    });

    test('measures against the ACTIVITY average, not the mean of the split '
        'paces, so a short partial cannot skew the baseline', () {
      // 1 km at 300 s/km, then a 100 m sprint at 200 s/km. The mean of the two
      // split paces is 250 s/km; the activity's real average is
      // (300 + 20) s / 1.1 km = 290.9 s/km.
      final workout = _workout(
        totalDistanceMeters: 1100,
        endTime: _at(320),
        routePoints: [
          _point(0, 0),
          _point(300, 1000),
          _point(320, 1100),
        ],
      );

      final result = _compute(workout);

      expect(result.splits[0].paceDeltaSeconds, closeTo(9.09, 0.5));
      expect(result.splits[1].paceDeltaSeconds, closeTo(-90.9, 0.5));
    });
  });

  group('no distance', () {
    test('a strength session (no distance, no route, no speed) has no '
        'splits', () {
      final workout = _workout(endTime: _at(3600));

      final result = _compute(workout);

      expect(result.splits, isEmpty);
      expect(result.isEmpty, isTrue);
    });

    test('a zero total distance has no splits', () {
      final workout = _workout(totalDistanceMeters: 0, endTime: _at(1800));

      expect(_compute(workout).splits, isEmpty);
    });

    test('heart-rate samples alone do not conjure splits', () {
      final workout = _workout(endTime: _at(1800));

      final result = _compute(
        workout,
        heartRates: [_hr(0, 100), _hr(600, 140), _hr(1200, 150)],
      );

      expect(result.splits, isEmpty);
    });
  });

  group('degenerate input does not crash or divide by zero', () {
    test('a single route point', () {
      final workout = _workout(
        totalDistanceMeters: 1000,
        endTime: _at(300),
        routePoints: [_point(0, 0)],
      );

      final result = _compute(workout);

      // One fix carries no distance: fall through to the estimated source.
      expect(result.source, SplitSource.estimated);
      expect(result.splits, hasLength(1));
    });

    test('duplicated route points (zero-length segments)', () {
      final workout = _workout(
        totalDistanceMeters: 1000,
        endTime: _at(300),
        routePoints: [
          _point(0, 0),
          _point(60, 0),
          _point(120, 0),
          _point(300, 1000),
        ],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.route);
      expect(result.splits, hasLength(1));
      expect(result.splits.first.distanceMeters, closeTo(1000.0, 0.01));
    });

    test('zero duration', () {
      final workout = _workout(
        totalDistanceMeters: 1000,
        durationMs: 0,
        endTime: _start,
      );

      final result = _compute(workout);

      expect(result.splits, hasLength(1));
      final split = result.splits.first;
      expect(split.elapsed, Duration.zero);
      // Pace is undefined, not infinite, and not zero.
      expect(split.paceSecondsPerMeter, isNull);
      expect(split.paceDeltaSeconds, isNull);
    });

    test('a route whose fixes all share one timestamp', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _start,
        durationMs: 0,
        routePoints: [_point(0, 0), _point(0, 1000), _point(0, 2000)],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.route);
      expect(result.splits, hasLength(2));
      expect(result.splits.first.elapsed, Duration.zero);
      expect(result.splits.first.paceDeltaSeconds, isNull);
    });

    test('unsorted route points and speed samples are sorted first', () {
      final unsorted = [
        _point(600, 2000),
        _point(0, 0),
        _point(300, 1000),
      ];
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: unsorted,
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.route);
      expect(result.splits, hasLength(2));
      expect(_secondsFromStart(result.splits[0].endTime), closeTo(300, 0.05));
      expect(_secondsFromStart(result.splits[1].endTime), closeTo(600, 0.05));
    });

    test('a zero or negative split distance falls back to 1 km rather than '
        'looping forever', () {
      final workout = _workout(
        totalDistanceMeters: 3000,
        endTime: _at(900),
        routePoints: [_point(0, 0), _point(900, 3000)],
      );

      expect(_compute(workout, splitDistanceMeters: 0).splits, hasLength(3));
      expect(_compute(workout, splitDistanceMeters: -5).splits, hasLength(3));
      expect(
        _compute(workout, splitDistanceMeters: double.nan).splits,
        hasLength(3),
      );
    });

    test('an absurdly small split distance is capped instead of building a '
        'million rows', () {
      final workout = _workout(
        totalDistanceMeters: 10000,
        endTime: _at(3000),
        routePoints: [_point(0, 0), _point(3000, 10000)],
      );

      final result = _compute(workout, splitDistanceMeters: 0.01);

      expect(result.splits.length, lessThanOrEqualTo(500));
    });

    test('a lap that ends before it starts is discarded', () {
      final workout = _workout(
        totalDistanceMeters: 1000,
        endTime: _at(300),
        laps: [
          ExerciseLapData(
            startTime: _at(200),
            endTime: _at(100),
            lengthMeters: 400,
          ),
        ],
      );

      final result = _compute(workout);

      // The only lap was nonsense -> fall through to the next usable source.
      expect(result.source, SplitSource.estimated);
      expect(result.splits, hasLength(1));
    });

    test('a lap with no recorded length borrows the route distance', () {
      final workout = _workout(
        totalDistanceMeters: 2000,
        endTime: _at(600),
        routePoints: [
          _point(0, 0),
          _point(300, 1200),
          _point(600, 2000),
        ],
        laps: [
          ExerciseLapData(
            startTime: _at(0),
            endTime: _at(300),
            lengthMeters: null,
          ),
          ExerciseLapData(
            startTime: _at(300),
            endTime: _at(600),
            lengthMeters: null,
          ),
        ],
      );

      final result = _compute(workout);

      expect(result.source, SplitSource.deviceLaps);
      expect(result.splits[0].distanceMeters, closeTo(1200.0, 0.5));
      expect(result.splits[1].distanceMeters, closeTo(800.0, 0.5));
    });
  });
}
