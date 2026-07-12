import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/exercise_session_metrics.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/presentation/activity_detail_screen.dart';
import 'package:openvitals/features/activity/presentation/activity_splits_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// The splits card on the activity detail screen. The point of these tests is
/// the HEADER: a lap read off a watch and a pace guessed from a session average
/// must never look like the same claim.

final DateTime _start = DateTime.utc(2026, 7, 10, 8);

DateTime _at(int seconds) => _start.add(Duration(seconds: seconds));

/// Same equator-line trick as the domain test: at latitude 0, an east offset in
/// degrees times this constant is the haversine distance in meters.
const double _metersPerDegreeAtEquator = 6371000.0 * 3.141592653589793 / 180.0;

ExerciseRoutePoint _point(int atSeconds, double eastMeters) =>
    ExerciseRoutePoint(
      time: _at(atSeconds),
      latitude: 0.0,
      longitude: eastMeters / _metersPerDegreeAtEquator,
      altitudeMeters: null,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );

ExerciseData _workout({
  double? totalDistanceMeters,
  List<ExerciseRoutePoint> routePoints = const <ExerciseRoutePoint>[],
  List<ExerciseLapData> laps = const <ExerciseLapData>[],
  int durationSeconds = 600,
}) =>
    ExerciseData(
      id: 'w1',
      title: 'Morning run',
      exerciseType: 56,
      startTime: _start,
      endTime: _at(durationSeconds),
      durationMs: durationSeconds * 1000,
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

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    required this.workout,
    this.speedSamples = const <SpeedSample>[],
    this.speedThrows = false,
  });

  final ExerciseData? workout;
  final List<SpeedSample> speedSamples;
  final bool speedThrows;

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => Ok(workout);

  @override
  Future<Result<List<SpeedSample>>> loadSpeedSamples(
      DateTime start, DateTime end) async {
    // A conforming repository never throws — a failed read is a failure Result.
    if (speedThrows) {
      return Err(UnexpectedFailure(
        'SPEED permission denied',
        cause: StateError('SPEED permission denied'),
      ));
    }
    return Ok(speedSamples);
  }

  // The sibling reads these tests never stub fail as a Result (they used to
  // fail as a noSuchMethod throw the use-case caught); the detail load must
  // degrade them to empty either way.
  @override
  Future<Result<ExerciseSessionMetrics>> loadWorkoutMetrics(
          DateTime start, DateTime end) async =>
      const Err(UnexpectedFailure('not stubbed'));

  @override
  Future<Result<List<ActivityCadenceSample>>> loadActivityCadenceSamples(
          DateTime start, DateTime end) async =>
      const Err(UnexpectedFailure('not stubbed'));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository({this.samples = const <HeartRateSample>[]});

  final List<HeartRateSample> samples;

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async =>
      Ok(samples);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<void> _pump(
  WidgetTester tester, {
  required ExerciseData? workout,
  List<SpeedSample> speedSamples = const <SpeedSample>[],
  bool speedThrows = false,
  List<HeartRateSample> heartRateSamples = const <HeartRateSample>[],
  Map<String, Object> prefs = const <String, Object>{},
}) async {
  SharedPreferences.setMockInitialValues({
    // The unit system defaults to the HOST locale, so a test that asserts on a
    // "1 km" header and does not pin this fails on a US machine and passes in
    // Europe. Pin it; the imperial test overrides it explicitly.
    'unit_system': UnitSystem.metric.name,
    ...prefs,
  });
  final resolved = await SharedPreferences.getInstance();

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(resolved),
        activityRepositoryProvider.overrideWithValue(
          _FakeActivityRepository(
            workout: workout,
            speedSamples: speedSamples,
            speedThrows: speedThrows,
          ),
        ),
        heartRepositoryProvider.overrideWithValue(
          _FakeHeartRepository(samples: heartRateSamples),
        ),
      ],
      child: const MaterialApp(
        // Without the delegates a screen using AppLocalizations dies on a null
        // check that points at the screen, not at the harness.
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: ActivityDetailScreen(activityId: 'w1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('device laps render as "Laps", credited to the recording device',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(
        totalDistanceMeters: 1600,
        durationSeconds: 480,
        laps: [
          for (var lap = 0; lap < 4; lap++)
            ExerciseLapData(
              startTime: _at(lap * 120),
              endTime: _at((lap + 1) * 120),
              lengthMeters: 400,
            ),
        ],
      ),
    );

    expect(find.byType(ActivitySplitsCard), findsOneWidget);
    expect(find.text('Laps'), findsOneWidget);
    expect(
      find.text('Recorded by the device or app that saved this activity.'),
      findsOneWidget,
    );
    expect(find.text('Splits · estimated'), findsNothing);
    // Four laps, so four numbered rows.
    for (final index in ['1', '2', '3', '4']) {
      expect(find.text(index), findsOneWidget);
    }
  });

  testWidgets('a GPS route renders as auto-derived splits, with the split '
      'distance in the header', (tester) async {
    await _pump(
      tester,
      workout: _workout(
        totalDistanceMeters: 2000,
        routePoints: [_point(0, 0), _point(600, 2000)],
      ),
      heartRateSamples: [
        HeartRateSample(time: _at(100), beatsPerMinute: 150, source: 'test'),
      ],
    );

    expect(find.byType(ActivitySplitsCard), findsOneWidget);
    expect(find.text('Splits · every 1 km'), findsOneWidget);
    expect(find.text('Laps'), findsNothing);
    // The heart rate that falls inside split 1 is shown on its row. Scope the
    // finder to the splits card: the heart-rate CHART below it now reports the
    // same 150 bpm as its average, which is correct and not what this asserts.
    expect(
      find.descendant(
        of: find.byType(ActivitySplitsCard),
        matching: find.textContaining('150 bpm'),
      ),
      findsOneWidget,
    );
  });

  testWidgets('the split-distance preference drives the header and the cuts',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(
        totalDistanceMeters: 2000,
        routePoints: [_point(0, 0), _point(600, 2000)],
      ),
      prefs: const {'activity_split_distance_meters': 500.0},
    );

    expect(find.text('Splits · every 0.5 km'), findsOneWidget);
    // 2 km at 500 m per split -> four rows.
    expect(find.text('4'), findsOneWidget);
    expect(find.text('5'), findsNothing);
  });

  testWidgets('speed samples (no route) still derive real splits',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(totalDistanceMeters: 2400),
      speedSamples: [
        for (var i = 0; i <= 6; i++)
          SpeedSample(
            time: _at(i * 100),
            metersPerSecond: 4.0,
            source: 'treadmill',
          ),
      ],
    );

    expect(find.byType(ActivitySplitsCard), findsOneWidget);
    expect(find.text('Splits · every 1 km'), findsOneWidget);
    expect(find.text('Splits · estimated'), findsNothing);
  });

  testWidgets('with no route and no speed samples the card says ESTIMATED and '
      'explains why', (tester) async {
    await _pump(tester, workout: _workout(totalDistanceMeters: 3000));

    expect(find.text('Splits · estimated'), findsOneWidget);
    expect(
      find.text(
        "This activity has no per-time distance data, so every split shows the "
        "activity's average pace.",
      ),
      findsOneWidget,
    );
    expect(find.text('Splits · every 1 km'), findsNothing);
  });

  testWidgets('a failing speed read degrades to estimated splits instead of '
      'blowing up the screen', (tester) async {
    await _pump(
      tester,
      workout: _workout(totalDistanceMeters: 3000),
      speedThrows: true,
    );

    expect(find.byType(ActivitySplitsCard), findsOneWidget);
    expect(find.text('Splits · estimated'), findsOneWidget);
    expect(find.textContaining('Unable to load activity'), findsNothing);
  });

  testWidgets('an activity with no distance hides the card entirely',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(totalDistanceMeters: null, durationSeconds: 3600),
    );

    expect(find.byType(ActivitySplitsCard), findsNothing);
    expect(find.text('Laps'), findsNothing);
    expect(find.text('Splits · estimated'), findsNothing);
    // The rest of the screen still renders.
    expect(find.text('Metrics'), findsOneWidget);
  });

  testWidgets('imperial units re-express the derived header in miles',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(
        totalDistanceMeters: 2000,
        routePoints: [_point(0, 0), _point(600, 2000)],
      ),
      prefs: {
        'unit_system': UnitSystem.imperial.name,
        // The stored value stays METRIC (1609.344 m == 1 mi).
        'activity_split_distance_meters': 1609.344,
      },
    );

    expect(find.text('Splits · every 1 mi'), findsOneWidget);
  });
}
