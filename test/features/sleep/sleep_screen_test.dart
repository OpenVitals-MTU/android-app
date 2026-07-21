import 'package:openvitals/domain/preferences/activity_week_mode.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/sleep/presentation/sleep_cards.dart';
import 'package:openvitals/features/sleep/presentation/sleep_metric_sections.dart';
import 'package:openvitals/features/sleep/presentation/sleep_schedule_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/cross_metric_insight_card.dart';
import 'package:openvitals/ui/components/daily_goal_components.dart';
import 'package:openvitals/ui/components/data_confidence_card.dart';
import 'package:openvitals/ui/components/metric_interpretation_card.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/query/sleep_period_data.dart';
import 'package:openvitals/features/sleep/presentation/sleep_screen.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

/// A fake [SleepRepository] returning canned period data.
class _FakeSleepRepository implements SleepRepository {
  _FakeSleepRepository({this.sessions = const <SleepData>[]});

  final List<SleepData> sessions;


  @override
  Future<Result<SleepPeriodData>> loadSleepPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(SleepPeriodData(sessions: sessions));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// A fake [HeartRepository]; only the cross-metric HRV load the sleep use case
/// issues is exercised.
class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository({this.hrv = const <DailyHrv>[]});

  final List<DailyHrv> hrv;

  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(
          LocalDate start, LocalDate end) async =>
      Ok(hrv);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

SleepData _session({int daysAgo = 0, int hours = 8}) {
  final now = DateTime.now().subtract(Duration(days: daysAgo));
  final end = DateTime(now.year, now.month, now.day, 7).toUtc();
  final start = end.subtract(Duration(hours: hours));
  return SleepData(
    id: 's$daysAgo',
    startTime: start,
    endTime: end,
    durationMs: const Duration(hours: 8).inMilliseconds,
    source: 'test',
    stages: [
      SleepStage(
        startTime: start,
        endTime: start.add(const Duration(hours: 5)),
        stageType: SleepStage.stageLight,
      ),
      SleepStage(
        startTime: start.add(const Duration(hours: 5)),
        endTime: start.add(const Duration(hours: 7)),
        stageType: SleepStage.stageDeep,
      ),
      SleepStage(
        startTime: start.add(const Duration(hours: 7)),
        endTime: end,
        stageType: SleepStage.stageRem,
      ),
    ],
  );
}

Future<Widget> _bootstrap({
  required _FakeSleepRepository sleepRepository,
  required Set<String> granted,
  List<DailyHrv> hrv = const <DailyHrv>[],
  Map<String, Object> prefsValues = const <String, Object>{},
  ActivityWeekMode weekMode = ActivityWeekMode.mondayToSunday,
}) async {
  SharedPreferences.setMockInitialValues(prefsValues);
  final prefs = await SharedPreferences.getInstance();
  PreferencesRepository(prefs).activityWeekMode = weekMode;
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      sleepRepositoryProvider.overrideWithValue(sleepRepository),
      heartRepositoryProvider.overrideWithValue(_FakeHeartRepository(hrv: hrv)),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const SleepScreen(),
    ),
  );
}

void main() {
  testWidgets('Sleep screen renders the Kotlin ordered sections', (tester) async {
    tester.view.physicalSize = const Size(1200, 5000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(sessions: [_session()]),
        granted: {HcPermissions.readSleep},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);

    // A week whose nights know their bedtimes gets the schedule chart, not the
    // duration bars — Kotlin's `useScheduleChart` rule.
    expect(find.byType(SleepScheduleStageChart), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
    expect(find.text('Sleep score'), findsOneWidget);

    // ...plus the sections Kotlin adds and the old screen lacked entirely.
    expect(find.byType(DailyGoalCard), findsOneWidget);
    expect(find.text('Daily goal'), findsOneWidget);
    expect(find.byType(DailyGoalStatistics), findsOneWidget);
    expect(find.text('Nights logged'), findsOneWidget);
    expect(find.text('Longest sleep'), findsOneWidget);
    expect(find.byType(DataConfidenceCard), findsOneWidget);
    expect(find.text('Sleep sessions'), findsOneWidget);
    expect(find.byType(SleepSessionItem), findsOneWidget);

    // The sleep-target reading, from the configured 8 h goal.
    expect(find.byType(MetricInterpretationCard), findsOneWidget);
    expect(find.text('Sleep target'), findsOneWidget);
  });

  testWidgets('the goal steppers move and persist the sleep target',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 5000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(sessions: [_session()]),
        granted: {HcPermissions.readSleep},
      ),
    );
    await tester.pumpAndSettle();

    // The sleep goal defaults to 8 h and steps by a quarter hour.
    expect(find.text('8h 00m'), findsWidgets);

    await tester.tap(find.byTooltip('Increase daily goal'));
    await tester.pumpAndSettle();
    expect(find.text('8h 15m'), findsWidgets);

    await tester.tap(find.byTooltip('Decrease daily goal'));
    await tester.tap(find.byTooltip('Decrease daily goal'));
    await tester.pumpAndSettle();
    expect(find.text('7h 45m'), findsWidgets);
  });

  testWidgets('the sleep-vs-HRV card needs enough paired nights',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 5000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    // One night with one HRV reading cannot correlate with anything.
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(sessions: [_session()]),
        granted: {HcPermissions.readSleep},
        hrv: [DailyHrv(date: LocalDate.now(), rmssdMs: 45)],
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(CrossMetricInsightCard), findsNothing);
  });

  testWidgets('a week of nights paired with HRV shows the correlation card',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final today = LocalDate.now();
    await tester.pumpWidget(
      await _bootstrap(
        // A rolling seven-day week, so that "a week of nights" is seven days
        // whatever day this runs on. Under Monday-to-Sunday a MONDAY holds one
        // elapsed day: four of these five nights fall in the previous week, the
        // pairs drop below the correlation threshold, and the card the test is
        // looking for is correctly not drawn. That is how it failed on the
        // first Monday after it was written.
        weekMode: ActivityWeekMode.last7Days,
        sleepRepository: _FakeSleepRepository(sessions: [
          for (var i = 0; i < 5; i++) _session(daysAgo: i, hours: 6 + i),
        ]),
        granted: {HcPermissions.readSleep},
        hrv: [
          for (var i = 0; i < 5; i++)
            DailyHrv(date: today.minusDays(i), rmssdMs: 40.0 + i * 3),
        ],
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(CrossMetricInsightCard), findsOneWidget);
    expect(find.text('Sleep vs HRV'), findsOneWidget);
  });

  testWidgets('the day view closes with the data-source education link',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    // Seed the persisted range so the screen opens on the DAY view, where
    // Kotlin's `sleepDayContent` appends the education item.
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(sessions: [_session()]),
        granted: {HcPermissions.readSleep},
        prefsValues: const {'detail_range_sleep': 'day'},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DataSourceEducationItem), findsOneWidget);
  });

  testWidgets(
      'a period view renders when the selected day has no sleep but the period does',
      (tester) async {
    // Open the app after midnight, before you have slept: the DAY view has nothing
    // for today, and says so. Switching to week/month/year then rendered a blank
    // page — the intraday section force-unwrapped `dailySummary`, which is derived
    // from the SELECTED DAY and so is null on any period whose selected day has no
    // sleep. `visible:` did not save it: the section's child is a plain Widget
    // argument, so it is built whether or not the section is shown.
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        // Nights before today, nothing for today itself.
        sleepRepository: _FakeSleepRepository(sessions: [
          _session(daysAgo: 1),
          _session(daysAgo: 2),
          _session(daysAgo: 3),
        ]),
        granted: {HcPermissions.readSleep},
        prefsValues: const {'detail_range_sleep': 'month'},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // The period has nights, so it must show them — not the empty message, and
    // certainly not a blank page.
    expect(find.text('No sleep recorded for this period.'), findsNothing);
    expect(find.byType(SleepOverviewCard), findsOneWidget);
  });

  testWidgets('the day view still says so when the selected day has no sleep',
      (tester) async {
    // The other half of the same bug: this one always worked, and must keep working.
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository:
            _FakeSleepRepository(sessions: [_session(daysAgo: 1)]),
        granted: {HcPermissions.readSleep},
        prefsValues: const {'detail_range_sleep': 'day'},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('No sleep recorded for the selected day.'), findsOneWidget);
  });

  testWidgets('Sleep screen shows the access gate when permission missing',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        sleepRepository: _FakeSleepRepository(sessions: [_session()]),
        granted: const <String>{},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
  });
}
