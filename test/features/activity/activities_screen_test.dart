import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/features/activity/application/activities_notifier.dart';
import 'package:openvitals/features/activity/presentation/activities_screen.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/charts/sparkline_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/daily_goal_components.dart';
import 'package:openvitals/ui/components/data_confidence_card.dart';

/// A fake [ActivityRepository]; only the methods the aggregate screen calls are
/// overridden. Everything else routes through [noSuchMethod].
class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    this.workouts = const <ExerciseData>[],
    this.dailySteps = const <DailySteps>[],
    this.nutrition = const <DailyNutrition>[],
  });

  final List<ExerciseData> workouts;
  final List<PlannedExerciseData> planned = const <PlannedExerciseData>[];
  final List<DailySteps> dailySteps;
  final List<DailyNutrition> nutrition;

  final List<String> deleted = <String>[];

  /// The windows `loadWorkoutsWithMetrics` (not the plain read) was used for —
  /// the current window pays for the per-session distance/speed aggregates,
  /// previous/baseline must not.
  final List<({LocalDate start, LocalDate end})> withMetricsWindows = [];
  final List<({LocalDate start, LocalDate end})> plainWindows = [];

  @override
  Future<List<ExerciseData>> loadWorkouts(LocalDate start, LocalDate end) async {
    plainWindows.add((start: start, end: end));
    return workouts;
  }

  @override
  Future<List<ExerciseData>> loadWorkoutsWithMetrics(
    LocalDate start,
    LocalDate end,
  ) async {
    withMetricsWindows.add((start: start, end: end));
    return workouts;
  }

  @override
  Future<List<PlannedExerciseData>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  ) async =>
      planned;

  @override
  Future<List<DailySteps>> loadDailySteps(LocalDate start, LocalDate end) async =>
      dailySteps;

  @override
  Future<List<DailyNutrition>> loadDailyNutrition(
    LocalDate start,
    LocalDate end,
  ) async =>
      nutrition;

  @override
  Future<void> deleteActivityEntry(String id) async => deleted.add(id);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeHeartRepository implements HeartRepository {
  @override
  Future<List<HeartRateSample>> loadHeartRateSamples(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <HeartRateSample>[];

  @override
  Future<List<DailyRestingHR>> loadDailyRestingHR(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <DailyRestingHR>[];

  @override
  Future<List<DailyHrv>> loadDailyHRV(LocalDate start, LocalDate end) async =>
      const <DailyHrv>[];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

DailySteps _steps(LocalDate date, int steps) => DailySteps(
      date: date,
      steps: steps,
      distanceMeters: steps.toDouble() * 0.7,
      activeCaloriesKcal: steps.toDouble() * 0.04,
    );

DailyNutrition _nutrition(LocalDate date) => DailyNutrition(
      date: date,
      hydrationLiters: 0,
      caloriesBurnedKcal: 2200,
    );

ExerciseData _workout({
  required String id,
  required String title,
  required int type,
  Duration duration = const Duration(minutes: 40),
  double? distanceMeters = 6000,
  double? averageSpeedMetersPerSecond,
  List<ExerciseSegmentData> segments = const <ExerciseSegmentData>[],
  bool openVitals = false,
}) {
  final start = DateTime.now().toUtc().subtract(const Duration(hours: 2));
  return ExerciseData(
    id: id,
    title: title,
    exerciseType: type,
    startTime: start,
    endTime: start.add(duration),
    durationMs: duration.inMilliseconds,
    source: 'test',
    totalDistanceMeters: distanceMeters,
    averageSpeedMetersPerSecond: averageSpeedMetersPerSecond,
    segments: segments,
    isOpenVitalsEntry: openVitals,
  );
}

Future<Widget> _bootstrap({
  required _FakeActivityRepository repository,
  HeartRepository? heartRepository,
  SharedPreferences? prefs,
}) async {
  final sharedPrefs = prefs ?? await SharedPreferences.getInstance();
  final router = GoRouter(
    initialLocation: '/activity',
    routes: [
      GoRoute(path: '/activity', builder: (c, s) => const ActivitiesScreen()),
      GoRoute(
        path: '/metric/:id',
        builder: (c, s) => Text('metric:${s.pathParameters['id']}'),
      ),
      GoRoute(path: '/calories', builder: (c, s) => const Text('route:calories')),
      GoRoute(
        path: '/activity/cardio_load',
        builder: (c, s) => const Text('route:cardio'),
      ),
      GoRoute(
        path: '/activity_detail/:id',
        builder: (c, s) => const Text('route:detail'),
      ),
      GoRoute(
        path: '/manual_entry/activity',
        builder: (c, s) =>
            Text('route:plan:${s.uri.queryParameters['planId'] ?? ''}'),
      ),
    ],
  );
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(sharedPrefs),
      activityRepositoryProvider.overrideWithValue(repository),
      heartRepositoryProvider
          .overrideWithValue(heartRepository ?? _FakeHeartRepository()),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider
          .overrideWith((ref) async => {HcPermissions.readExercise}),
    ],
    child: MaterialApp.router(
      routerConfig: router,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
    ),
  );
}

void _tallScreen(WidgetTester tester) {
  tester.view.physicalSize = const Size(1200, 6000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  final today = LocalDate.now();

  setUp(() => SharedPreferences.setMockInitialValues(const <String, Object>{}));

  testWidgets('renders the Kotlin activities sections from a period',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000), _steps(today.minusDays(1), 7000)],
      nutrition: [_nutrition(today), _nutrition(today.minusDays(1))],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // ACTIVITY_SUMMARY: the period workout row.
    expect(find.text('Morning run'), findsOneWidget);
    // ACTIVITY_KEY_METRICS: the five metric cards with sparklines.
    expect(find.text('Key metrics'), findsOneWidget);
    expect(find.byType(SparklineChart), findsNWidgets(5));
    // PERIOD_CHART, DAILY_GOAL, STATISTICS, DATA_CONFIDENCE.
    expect(find.byType(MetricBarChart), findsOneWidget);
    expect(find.byType(DailyGoalCard), findsOneWidget);
    expect(find.byType(DailyGoalStatistics), findsOneWidget);
    expect(find.byType(DataConfidenceCard), findsOneWidget);
  });

  testWidgets('the activity-type filter narrows the workout list',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [
        _workout(id: 'w1', title: 'Morning run', type: 56),
        _workout(id: 'w2', title: 'Evening ride', type: 8),
      ],
      dailySteps: [_steps(today, 9000)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(find.text('Morning run'), findsOneWidget);
    expect(find.text('Evening ride'), findsOneWidget);

    // Open the "Activity type" dropdown and pick Cycling.
    await tester.tap(find.byWidgetPredicate(
      (w) => w is DropdownButtonFormField<int?>,
    ));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cycling').last);
    await tester.pumpAndSettle();

    expect(find.text('Morning run'), findsNothing);
    expect(find.text('Evening ride'), findsOneWidget);
  });

  testWidgets('key-metric cards navigate to their metric targets',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000), _steps(today.minusDays(1), 7000)],
      nutrition: [_nutrition(today)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Steps'));
    await tester.pumpAndSettle();
    expect(find.text('metric:STEPS'), findsOneWidget);
  });

  testWidgets('the cardio-load card opens the cardio-load detail',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000), _steps(today.minusDays(1), 7000)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Cardio load'));
    await tester.pumpAndSettle();
    expect(find.text('route:cardio'), findsOneWidget);
  });

  testWidgets('the goal steppers move and persist the workout goal',
      (tester) async {
    _tallScreen(tester);
    final prefs = await SharedPreferences.getInstance();
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo, prefs: prefs));
    await tester.pumpAndSettle();

    // The workout goal defaults to 30 minutes and steps by 5.
    expect(find.text('30'), findsWidgets);
    await tester.tap(find.byTooltip('Increase daily goal'));
    await tester.pumpAndSettle();
    expect(find.text('35'), findsWidgets);

    // Remount with the same preferences: the stepped goal survives.
    await tester.pumpWidget(await _bootstrap(repository: repo, prefs: prefs));
    await tester.pumpAndSettle();
    expect(find.text('35'), findsWidgets);
  });

  test('a pause segment shortens moving duration and speeds moving pace',
      () async {
    // Two 40-minute, 6 km workouts; one contains a 10-minute pause segment.
    final start = DateTime.now().toUtc().subtract(const Duration(hours: 2));
    final paused = _workout(
      id: 'paused',
      title: 'Paused run',
      type: 56,
      distanceMeters: 6000,
      segments: [
        ExerciseSegmentData(
          startTime: start.add(const Duration(minutes: 5)),
          endTime: start.add(const Duration(minutes: 15)),
          segmentType: ExerciseSegmentType.pause,
          repetitions: 0,
        ),
      ],
    );
    final continuous = _workout(
      id: 'continuous',
      title: 'Continuous run',
      type: 56,
      distanceMeters: 6000,
    );

    final withPause = activityTypeAggregatesOf([paused]).single;
    final withoutPause = activityTypeAggregatesOf([continuous]).single;

    // Moving duration drops the 10-minute pause; total duration is unchanged.
    expect(withPause.totalDurationMs, const Duration(minutes: 40).inMilliseconds);
    expect(withPause.totalMovingDurationMs,
        const Duration(minutes: 30).inMilliseconds);
    expect(withPause.totalMovingDurationMs, lessThan(withPause.totalDurationMs));

    // Same distance over less moving time ⇒ a faster average moving speed.
    expect(withoutPause.totalMovingDurationMs,
        const Duration(minutes: 40).inMilliseconds);
    expect(
      withPause.averageMovingSpeedMetersPerSecond,
      greaterThan(withoutPause.averageMovingSpeedMetersPerSecond!),
    );
  });

  test('best speed takes the max of avg speed and distance/moving duration',
      () async {
    // 6 km in 40 min = 2.5 m/s derived; the provider's recorded average is
    // slower (it counts the standing-still stretches the moving duration drops).
    final derivedIsFaster = _workout(
      id: 'derived',
      title: 'Derived faster',
      type: 56,
      averageSpeedMetersPerSecond: 1.5,
    );
    expect(
      activityTypeAggregatesOf([derivedIsFaster]).single.bestSpeedMetersPerSecond,
      closeTo(2.5, 0.001),
    );

    // The other direction: a recorded average above the whole-session derived
    // speed (e.g. a GPS-sampled peak-heavy average) wins.
    final recordedIsFaster = _workout(
      id: 'recorded',
      title: 'Recorded faster',
      type: 56,
      averageSpeedMetersPerSecond: 4.0,
    );
    expect(
      activityTypeAggregatesOf([recordedIsFaster]).single.bestSpeedMetersPerSecond,
      closeTo(4.0, 0.001),
    );

    // Either input alone is enough.
    final speedOnly = _workout(
      id: 'speed-only',
      title: 'Speed only',
      type: 56,
      distanceMeters: null,
      averageSpeedMetersPerSecond: 3.0,
    );
    expect(
      activityTypeAggregatesOf([speedOnly]).single.bestSpeedMetersPerSecond,
      closeTo(3.0, 0.001),
    );

    // Neither ⇒ no fastest pace at all (rather than a bogus zero).
    final neither = _workout(
      id: 'neither',
      title: 'Neither',
      type: 56,
      distanceMeters: null,
    );
    expect(
      activityTypeAggregatesOf([neither]).single.bestSpeedMetersPerSecond,
      isNull,
    );

    // Across a group, the fastest workout wins.
    final group = activityTypeAggregatesOf([derivedIsFaster, recordedIsFaster]);
    expect(group.single.count, 2);
    expect(group.single.bestSpeedMetersPerSecond, closeTo(4.0, 0.001));
  });

  testWidgets(
      'only the current window is loaded with the per-session route metrics',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    // Kotlin's ActivitiesViewModel switched exactly one call site: the current
    // window. Previous/baseline stay on the cheap read (no per-session
    // aggregates), otherwise a year view would fire hundreds of them.
    expect(repo.withMetricsWindows, hasLength(1));
    expect(repo.plainWindows, hasLength(2));
    expect(repo.withMetricsWindows.single.end, today);
  });

  testWidgets('the key-metric sparkline renders weekday label rows for a week',
      (tester) async {
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000), _steps(today.minusDays(1), 7000)],
      nutrition: [_nutrition(today)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    // The default range is a week ⇒ each of the seven buckets is a single day,
    // labelled with its weekday initial. Wednesday's "W" appears once per card —
    // and once more under the week strip, which labels the same seven buckets.
    expect(find.byType(SparklineChart), findsNWidgets(5));
    expect(find.text('W'), findsNWidgets(6));
  });

  test('the section order persists across notifier instances', () async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();

    final first = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    final before = first.read(metricDetailSectionOrderProvider);
    final MetricDetailSectionId moved = before.first;
    first
        .read(metricDetailSectionOrderProvider.notifier)
        .moveSection(moved, 1);
    final after = first.read(metricDetailSectionOrderProvider);
    first.dispose();
    expect(after, isNot(before));

    final second = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    expect(second.read(metricDetailSectionOrderProvider), after);
    second.dispose();
  });
  testWidgets('the week strip marks the day you trained and rings the rest',
      (tester) async {
    // Kotlin opened the week card with a strip of seven markers: a filled circle
    // carrying the workout's own icon on the days you trained, an empty ring on
    // the days you did not. The Flutter port used the buckets for its sparklines
    // and dropped the strip entirely -- so the week view lost the one thing that
    // showed WHICH days were active.
    _tallScreen(tester);
    final repo = _FakeActivityRepository(
      // A single workout, today. Six other days of the week have none.
      workouts: [_workout(id: 'w1', title: 'Morning run', type: 56)],
      dailySteps: [_steps(today, 9000)],
      nutrition: [_nutrition(today)],
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    final active = find.byKey(ValueKey('activity-day-marker-$today-active'));
    expect(active, findsOneWidget, reason: 'today had a workout');

    // The marker is filled AND carries the exercise icon -- an empty ring would
    // have no child, which is exactly what the six rest days render.
    expect(find.descendant(of: active, matching: find.byType(Icon)),
        findsOneWidget);

    // Every other day of the week is a rest ring: seven markers, one active.
    final rest = find.byWidgetPredicate((w) =>
        w.key is ValueKey<String> &&
        (w.key! as ValueKey<String>).value.startsWith('activity-day-marker-') &&
        (w.key! as ValueKey<String>).value.endsWith('-rest'));
    expect(rest, findsNWidgets(6));
  });

}
