import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/activity/activity_daily_entries.dart';
import 'package:openvitals/features/activity/activity_intraday_chart_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/daily_goal_components.dart';
import 'package:openvitals/ui/components/data_confidence_card.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/activity_period_data.dart';
import 'package:openvitals/features/activity/activity_metric.dart';
import 'package:openvitals/features/activity/activity_metric_screen.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

/// A fake [ActivityRepository] returning canned period data. Only the handful of
/// methods the activity screens call are overridden; everything else routes
/// through [noSuchMethod] (and throws if unexpectedly reached).
class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    this.dailySteps = const <DailySteps>[],
    this.nutrition = const <DailyNutrition>[],
    this.activityProgress = const <ActivityProgressPoint>[],
  });

  final List<DailySteps> dailySteps;
  final List<DailyNutrition> nutrition;
  final List<ActivityProgressPoint> activityProgress;

  @override
  Future<ActivityPeriodData> loadActivityPeriod(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    bool includeWheelchairPushes = false,
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      ActivityPeriodData(
        dailySteps: dailySteps,
        nutrition: nutrition,
        activityProgress: activityProgress,
      );

  @override
  Future<List<PlannedExerciseData>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <PlannedExerciseData>[];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

DailySteps _steps(LocalDate date, int steps) => DailySteps(
      date: date,
      steps: steps,
      distanceMeters: steps.toDouble() * 0.7,
      activeCaloriesKcal: steps.toDouble() * 0.04,
      floorsClimbed: 3,
      elevationGainedMeters: 12,
      wheelchairPushes: 1200,
    );

DailyNutrition _nutrition(LocalDate date) => DailyNutrition(
      date: date,
      hydrationLiters: 0,
      caloriesBurnedKcal: 2200,
    );

/// Kotlin renders one shared ordered-section layout for every movement metric.
const _metricScreens = <ActivityMetric, Widget>{
  ActivityMetric.steps: StepsScreen(),
  ActivityMetric.distance: DistanceScreen(),
  ActivityMetric.caloriesOut: CaloriesOutScreen(),
  ActivityMetric.activeCalories: ActiveCaloriesScreen(),
  ActivityMetric.floors: FloorsScreen(),
  ActivityMetric.elevation: ElevationScreen(),
  ActivityMetric.wheelchair: WheelchairScreen(),
};

Future<Widget> _bootstrap({
  required Widget screen,
  required _FakeActivityRepository repository,
  required Set<String> granted,
  TimeRange? storedRange,
}) async {
  SharedPreferences.setMockInitialValues(
    storedRange == null
        ? const <String, Object>{}
        : {PeriodRangePreferenceKey.steps.storageKey: storedRange.name},
  );
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      activityRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: screen,
    ),
  );
}

void main() {
  final today = LocalDate.now();

  testWidgets('Steps screen renders the Kotlin sections once loaded',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _FakeActivityRepository(
      dailySteps: [
        _steps(today, 9000),
        _steps(today.minusDays(1), 7000),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        screen: const StepsScreen(),
        repository: repo,
        granted: {HcPermissions.readSteps},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // A week range charts the period, not the intraday curve.
    expect(find.byType(MetricBarChart), findsOneWidget);
    expect(find.byType(IntradayActivityChartCard), findsNothing);

    // The goal card, its statistics, the period statistics, confidence, entries.
    expect(find.byType(DailyGoalCard), findsOneWidget);
    expect(find.text('Daily goal'), findsOneWidget);
    expect(find.byType(DailyGoalStatistics), findsOneWidget);
    expect(find.text('Goals met'), findsOneWidget);
    expect(find.text('Total'), findsOneWidget);
    expect(find.text('Active days'), findsOneWidget);
    expect(find.byType(DataConfidenceCard), findsOneWidget);
    expect(find.byType(ActivityDailyEntriesContent), findsWidgets);

    // Kotlin's steps content has no hero MetricCard.
    expect(find.byType(MetricCard), findsNothing);
  });

  testWidgets('the goal steppers move and persist the daily goal',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _FakeActivityRepository(dailySteps: [_steps(today, 9000)]);
    await tester.pumpWidget(
      await _bootstrap(
        screen: const StepsScreen(),
        repository: repo,
        granted: {HcPermissions.readSteps},
      ),
    );
    await tester.pumpAndSettle();

    // The steps goal defaults to 8000 and steps by 500.
    expect(find.text('8,000'), findsOneWidget);

    await tester.tap(find.byTooltip('Increase daily goal'));
    await tester.pumpAndSettle();
    expect(find.text('8,500'), findsOneWidget);

    await tester.tap(find.byTooltip('Decrease daily goal'));
    await tester.tap(find.byTooltip('Decrease daily goal'));
    await tester.pumpAndSettle();
    expect(find.text('7,500'), findsOneWidget);
  });

  for (final entry in _metricScreens.entries) {
    testWidgets('${entry.key.name} screen renders the shared sections',
        (tester) async {
      tester.view.physicalSize = const Size(1200, 4000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final repo = _FakeActivityRepository(
        dailySteps: [_steps(today, 9000), _steps(today.minusDays(1), 7000)],
        nutrition: [_nutrition(today), _nutrition(today.minusDays(1))],
      );
      await tester.pumpWidget(
        await _bootstrap(
          screen: entry.value,
          repository: repo,
          granted: {entry.key.readPermission},
        ),
      );
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.text(entry.key.title), findsWidgets);
      expect(find.byType(MetricBarChart), findsOneWidget);
      expect(find.byType(DailyGoalCard), findsOneWidget);
      expect(find.byType(DailyGoalStatistics), findsOneWidget);
      expect(find.byType(DataConfidenceCard), findsOneWidget);
      expect(find.byType(ActivityDailyEntriesContent), findsWidgets);
    });
  }

  testWidgets('the day range shows the intraday chart, not the period chart',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _FakeActivityRepository(
      dailySteps: [_steps(today, 9000)],
      activityProgress: [
        for (var hour = 8; hour <= 12; hour++)
          ActivityProgressPoint(
            time: DateTime.now().copyWith(hour: hour, minute: 0),
            totalSteps: hour * 800,
            totalDistanceMeters: null,
            totalCaloriesBurnedKcal: null,
          ),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        screen: const StepsScreen(),
        repository: repo,
        granted: {HcPermissions.readSteps},
        storedRange: TimeRange.day,
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(IntradayActivityChartCard), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
    // The day's cumulative total heads the card.
    expect(find.textContaining('9,000'), findsWidgets);
  });

  testWidgets('Steps screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeActivityRepository(dailySteps: [_steps(today, 9000)]);
    await tester.pumpWidget(
      await _bootstrap(
        screen: const StepsScreen(),
        repository: repo,
        granted: const <String>{},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricCard), findsNothing);
  });

  testWidgets('Steps screen shows the empty placeholder with no data',
      (tester) async {
    final repo = _FakeActivityRepository(dailySteps: const <DailySteps>[]);
    await tester.pumpWidget(
      await _bootstrap(
        screen: const ActivityMetricScreen(metric: ActivityMetric.steps),
        repository: repo,
        granted: {HcPermissions.readSteps},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
  });
}
