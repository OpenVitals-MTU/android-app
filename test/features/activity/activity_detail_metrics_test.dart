import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/exercise_session_metrics.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/presentation/activity_detail_screen.dart';
import 'package:openvitals/features/activity/presentation/activity_session_metric_chart_cards.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// The activity detail screen's metric rows and session charts.
///
/// Two bugs are guarded here, both reported off a real bike ride:
///
///  1. Speed and cadence were recorded but never shown. The samples were fetched
///     (speed) or not fetched at all (cadence), and no chart existed either way.
///  2. The ride advertised "Steps", "Floors climbed" and "Wheelchair pushes",
///     all "Not available" — absences that were never going to be anything else.

final DateTime _start = DateTime.utc(2026, 7, 10, 8);

DateTime _at(int seconds) => _start.add(Duration(seconds: seconds));

ExerciseData _workout({
  required int exerciseType,
  double? totalDistanceMeters = 20000,
  int durationSeconds = 3600,
  int? steps,
  int? floorsClimbed,
  int? wheelchairPushes,
  double? averageCyclingCadenceRpm,
  double? averageStepsCadenceRate,
  double? averagePowerWatts,
  double? averageSpeedMetersPerSecond,
}) =>
    ExerciseData(
      id: 'w1',
      title: null,
      exerciseType: exerciseType,
      startTime: _start,
      endTime: _at(durationSeconds),
      durationMs: durationSeconds * 1000,
      source: 'test',
      totalDistanceMeters: totalDistanceMeters,
      steps: steps,
      floorsClimbed: floorsClimbed,
      wheelchairPushes: wheelchairPushes,
      averageCyclingCadenceRpm: averageCyclingCadenceRpm,
      averageStepsCadenceRate: averageStepsCadenceRate,
      averagePowerWatts: averagePowerWatts,
      averageSpeedMetersPerSecond: averageSpeedMetersPerSecond,
    );

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    required this.workout,
    this.speedSamples = const <SpeedSample>[],
    this.cadenceSamples = const <ActivityCadenceSample>[],
    this.sessionMetrics = ExerciseSessionMetrics.none,
    this.cadenceThrows = false,
    this.metricsThrow = false,
  });

  final ExerciseData? workout;
  final List<SpeedSample> speedSamples;
  final List<ActivityCadenceSample> cadenceSamples;
  final ExerciseSessionMetrics sessionMetrics;
  final bool cadenceThrows;
  final bool metricsThrow;

  @override
  Future<ExerciseData?> loadWorkout(String id) async => workout;

  @override
  Future<ExerciseSessionMetrics> loadWorkoutMetrics(
    DateTime start,
    DateTime end,
  ) async {
    if (metricsThrow) throw StateError('STEPS permission denied');
    return sessionMetrics;
  }

  @override
  Future<List<SpeedSample>> loadSpeedSamples(DateTime start, DateTime end) async =>
      speedSamples;

  @override
  Future<List<ActivityCadenceSample>> loadActivityCadenceSamples(
    DateTime start,
    DateTime end,
  ) async {
    if (cadenceThrows) throw StateError('CADENCE permission denied');
    return cadenceSamples;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHeartRepository implements HeartRepository {
  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async =>
      const Ok(<HeartRateSample>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<void> _pump(
  WidgetTester tester, {
  required ExerciseData workout,
  List<SpeedSample> speedSamples = const <SpeedSample>[],
  List<ActivityCadenceSample> cadenceSamples = const <ActivityCadenceSample>[],
  ExerciseSessionMetrics sessionMetrics = ExerciseSessionMetrics.none,
  bool cadenceThrows = false,
  bool metricsThrow = false,
}) async {
  // The unit system follows the host locale unless pinned, which would make the
  // km/h assertions below pass in Europe and fail in the US.
  SharedPreferences.setMockInitialValues({'unit_system': UnitSystem.metric.name});
  final resolved = await SharedPreferences.getInstance();

  // The detail screen is a long scroll and the chart cards sit near the bottom of
  // it. On the default 800x600 surface a ListView never builds them, so a finder
  // reports "not rendered" for something that is merely below the fold. Give the
  // test a surface tall enough to hold the whole screen.
  tester.view.physicalSize = const Size(1000, 4000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(resolved),
        activityRepositoryProvider.overrideWithValue(
          _FakeActivityRepository(
            workout: workout,
            speedSamples: speedSamples,
            cadenceSamples: cadenceSamples,
            sessionMetrics: sessionMetrics,
            cadenceThrows: cadenceThrows,
            metricsThrow: metricsThrow,
          ),
        ),
        heartRepositoryProvider.overrideWithValue(_FakeHeartRepository()),
      ],
      child: const MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: ActivityDetailScreen(activityId: 'w1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

List<SpeedSample> _speed() => [
      for (var i = 0; i < 10; i++)
        SpeedSample(
          time: _at(i * 300),
          metersPerSecond: 5.0 + i,
          source: 'bike computer',
        ),
    ];

List<ActivityCadenceSample> _cadence(ActivityCadenceKind kind) => [
      for (var i = 0; i < 10; i++)
        ActivityCadenceSample(
          time: _at(i * 300),
          rate: 80.0 + i,
          kind: kind,
          source: 'bike computer',
        ),
    ];

void main() {
  group('a cycling activity', () {
    testWidgets('renders the recorded speed and cadence it used to drop',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(exerciseType: ExerciseSessionType.biking),
        speedSamples: _speed(),
        cadenceSamples: _cadence(ActivityCadenceKind.cycling),
      );

      expect(find.byType(ActivitySpeedChartCard), findsOneWidget);
      expect(find.byType(ActivityCadenceChartCard), findsOneWidget);
      // Twice over: the chart's title, and the metric row above it.
      expect(find.text('Cycling cadence'), findsNWidgets(2));
      // The session record carries no averages of its own here — only samples —
      // so the row can only read 84.5 rpm if the workout was backfilled from
      // them. Otherwise the screen contradicts itself: a full cadence trace
      // charted underneath "Cycling cadence: Not available".
      // Once on the metric row, again in the chart's own average stat. (Other
      // rows here DO read "Not available" — this fixture records no heart rate
      // and no calories — which is exactly the honest absence the fix keeps.)
      expect(find.text('84.5 rpm'), findsWidgets);
    });

    testWidgets('does not offer steps, floors or wheelchair pushes',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(exerciseType: ExerciseSessionType.biking),
        speedSamples: _speed(),
      );

      expect(find.text('Metrics'), findsOneWidget);
      for (final absent in [
        'Steps',
        'Step cadence',
        'Floors climbed',
        'Wheelchair pushes',
        'Average pace',
      ]) {
        expect(find.text(absent), findsNothing, reason: absent);
      }
      // What a cyclist DOES want is still offered, even unrecorded.
      expect(find.text('Distance'), findsOneWidget);
      expect(find.text('Average speed'), findsOneWidget);
      expect(find.text('Cycling cadence'), findsOneWidget);
      // ...but a power meter is hardware most riders do not own, so an unrecorded
      // "Average power" stays out of the way until something reports one.
      expect(find.text('Average power'), findsNothing);
      // "Recorded speed" DOES appear: the speed samples backfill it. A metric with
      // a value is never hidden — relevance only suppresses absences.
      expect(find.text('Recorded speed'), findsOneWidget);
    });

    testWidgets('surfaces cadence and power averages on the metric rows',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.biking,
          averageCyclingCadenceRpm: 88.0,
          averagePowerWatts: 210.0,
          averageSpeedMetersPerSecond: 7.5,
        ),
      );

      expect(find.text('88.0 rpm'), findsOneWidget);
      expect(find.text('210 W'), findsOneWidget);
      // Recorded speed is the device's own figure, distinct from the average this
      // app derives from distance over duration.
      expect(find.text('Recorded speed'), findsOneWidget);
      expect(find.text('27.0 km/h'), findsOneWidget);
    });

    testWidgets('a failing cadence read costs the card, not the screen',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(exerciseType: ExerciseSessionType.biking),
        speedSamples: _speed(),
        cadenceThrows: true,
      );

      expect(find.byType(ActivityCadenceChartCard), findsNothing);
      expect(find.byType(ActivitySpeedChartCard), findsOneWidget);
      expect(find.text('Metrics'), findsOneWidget);
      expect(find.textContaining('Unable to load activity'), findsNothing);
    });
  });

  group('a running activity', () {
    testWidgets('gets step cadence in steps per minute, not revolutions',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.running,
          averageStepsCadenceRate: 168.0,
        ),
        cadenceSamples: _cadence(ActivityCadenceKind.steps),
      );

      // The chart's title, and the metric row above it.
      expect(find.text('Step cadence'), findsNWidgets(2));
      // Kotlin formatted step cadence through its rpm formatter, labelling a
      // runner's stride rate as revolutions per minute.
      expect(find.text('168.0 spm'), findsOneWidget);
      expect(find.text('168.0 rpm'), findsNothing);
    });

    testWidgets('shows pace and steps, and no crank', (tester) async {
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.running,
          steps: 8000,
        ),
      );

      expect(find.text('Average pace'), findsOneWidget);
      expect(find.text('Steps'), findsOneWidget);
      expect(find.text('Cycling cadence'), findsNothing);
      expect(find.text('Wheelchair pushes'), findsNothing);
    });
  });

  testWidgets('a recorded value is shown even when the type says it is '
      'irrelevant', (tester) async {
    // Relevance may only hide an ABSENCE. If a device really did count steps on
    // a bike ride, hiding them would be destroying data, not decluttering.
    await _pump(
      tester,
      workout: _workout(exerciseType: ExerciseSessionType.biking, steps: 1234),
    );

    expect(find.text('Steps'), findsOneWidget);
    expect(find.text('1,234'), findsOneWidget);
  });

  // The watch bug, end to end. The walk's own record is nearly empty: the steps,
  // distance, calories and elevation were written as SEPARATE records covering
  // the same window, which is how a two-hour walk came to report "Steps: Not
  // available" directly above a chart of its own step cadence.
  group('a walking activity recorded by a watch', () {
    testWidgets('shows the totals its session record never carried',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.walking,
          totalDistanceMeters: null,
        ),
        sessionMetrics: const ExerciseSessionMetrics(
          totalDistanceMeters: 4500.0,
          steps: 6200,
          totalCaloriesKcal: 320.0,
          activeCaloriesKcal: 210.0,
          elevationGainedMeters: 48.0,
        ),
      );

      expect(find.text('6,200'), findsOneWidget);
      expect(find.text('4.5 km'), findsOneWidget);
      expect(find.text('320 kcal'), findsOneWidget);
      expect(find.text('210 kcal'), findsOneWidget);
      expect(find.text('48 m'), findsOneWidget);
    });

    testWidgets('derives a distance from speed when no distance was written',
        (tester) async {
      // A watch that records speed but writes no DistanceRecord left the session
      // with no distance at all — while the splits card, integrating these very
      // samples, cheerfully reported "every 1 km".
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.walking,
          totalDistanceMeters: null,
        ),
        speedSamples: [
          for (var i = 0; i <= 3600; i += 60)
            SpeedSample(
              time: _at(i),
              metersPerSecond: 2.0,
              source: 'watch',
            ),
        ],
      );

      // 2 m/s held for an hour.
      expect(find.text('7.2 km'), findsOneWidget);
    });

    testWidgets('a failing metrics read costs the numbers, not the screen',
        (tester) async {
      await _pump(
        tester,
        workout: _workout(
          exerciseType: ExerciseSessionType.walking,
          totalDistanceMeters: null,
        ),
        metricsThrow: true,
      );

      expect(find.text('Metrics'), findsOneWidget);
      expect(find.textContaining('Unable to load activity'), findsNothing);
    });
  });

  testWidgets('a strength session shows no distance metrics at all',
      (tester) async {
    await _pump(
      tester,
      workout: _workout(
        exerciseType: ExerciseSessionType.strengthTraining,
        totalDistanceMeters: null,
      ),
    );

    expect(find.text('Metrics'), findsOneWidget);
    expect(find.text('Duration'), findsOneWidget);
    for (final absent in [
      'Distance',
      'Average pace',
      'Average speed',
      'Steps',
      'Cycling cadence',
      'Elevation gained',
    ]) {
      expect(find.text(absent), findsNothing, reason: absent);
    }
  });
}
