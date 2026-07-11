import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/domain/query/heart_period_data.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';
import 'package:openvitals/features/heart/heart_metric_cards.dart';
import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/features/vitals/heart_vitals_overview_screen.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/line_chart.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository({this.data = const HeartPeriodData()});

  final HeartPeriodData data;
  final List<PeriodLoadQuery> queries = [];

  @override
  Future<HeartPeriodData> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    queries.add(query);
    return data;
  }

  @override
  Future<List<DailyHrv>> loadDailyHRV(LocalDate start, LocalDate end) async =>
      const <DailyHrv>[];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeVitalsRepository implements VitalsRepository {
  _FakeVitalsRepository({this.data = const VitalsPeriodData()});

  final VitalsPeriodData data;

  @override
  Set<String> get phase3Permissions => const <String>{};

  @override
  Future<VitalsPeriodData> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      data;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

SkinTemperatureEntry _skin(DateTime time, double delta, {String source = 'ring'}) =>
    SkinTemperatureEntry(
      startTime: time,
      endTime: time,
      baselineCelsius: 33,
      averageDeltaCelsius: delta,
      minDeltaCelsius: delta,
      maxDeltaCelsius: delta,
      measurementLocation: 0,
      source: source,
    );

HeartPeriodData _heartData() {
  final today = LocalDate.now();
  return HeartPeriodData(
    dailySummaries: [
      HeartRateSummary(
          date: today.plusDays(-2), avgBpm: 70, minBpm: 55, maxBpm: 120),
      HeartRateSummary(
          date: today.plusDays(-1), avgBpm: 72, minBpm: 56, maxBpm: 118),
      HeartRateSummary(date: today, avgBpm: 68, minBpm: 54, maxBpm: 110),
    ],
    dailyRestingHR: [
      DailyRestingHR(date: today.plusDays(-1), bpm: 60),
      DailyRestingHR(date: today, bpm: 58),
    ],
    dailyHrv: [
      DailyHrv(date: today.plusDays(-1), rmssdMs: 42),
      DailyHrv(date: today, rmssdMs: 45),
    ],
  );
}

VitalsPeriodData _vitalsData(DateTime now) => VitalsPeriodData(
      skinTemperature: [
        _skin(now.subtract(const Duration(days: 1)), -0.3),
        _skin(now, 0.4),
      ],
    );

Future<Widget> _bootstrap({
  required ProviderContainer container,
}) async {
  final router = GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => const HeartVitalsOverviewScreen(),
      ),
      GoRoute(
        path: '/metric/:metricId',
        builder: (context, state) => Scaffold(
          body: Center(
            child: Text('metric: ${state.pathParameters['metricId']}'),
          ),
        ),
      ),
    ],
  );
  return UncontrolledProviderScope(
    container: container,
    child: MaterialApp.router(
      routerConfig: router,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
    ),
  );
}

Future<ProviderContainer> _container({
  required HeartPeriodData heart,
  required VitalsPeriodData vitals,
  Set<String>? granted,
  SharedPreferences? sharedPreferences,
  _FakeHeartRepository? heartRepository,
}) async {
  final grantedPermissions = granted ??
      {
        HcPermissions.readHeartRate,
        HcPermissions.readRestingHeartRate,
        HcPermissions.readHrv,
      };
  if (sharedPreferences == null) {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
  }
  final prefs = sharedPreferences ?? await SharedPreferences.getInstance();
  return ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      heartRepositoryProvider.overrideWithValue(
          heartRepository ?? _FakeHeartRepository(data: heart)),
      vitalsRepositoryProvider
          .overrideWithValue(_FakeVitalsRepository(data: vitals)),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider
          .overrideWith((ref) async => grantedPermissions),
    ],
  );
}

void main() {
  final now = DateTime.now().toUtc();

  // Locks the two distinct respiratory averages Kotlin uses, so a future edit
  // cannot collapse them. Uneven reading counts make them differ: day1 has
  // three readings [12,12,12], day2 has one [20].
  test('respiratory flat mean and daily-bucket mean stay distinct', () {
    final entries = <RespiratoryRateEntry>[
      RespiratoryRateEntry(
          time: DateTime(2026, 1, 1, 8), breathsPerMinute: 12, source: 'ring'),
      RespiratoryRateEntry(
          time: DateTime(2026, 1, 1, 12), breathsPerMinute: 12, source: 'ring'),
      RespiratoryRateEntry(
          time: DateTime(2026, 1, 1, 20), breathsPerMinute: 12, source: 'ring'),
      RespiratoryRateEntry(
          time: DateTime(2026, 1, 2, 12), breathsPerMinute: 20, source: 'ring'),
    ];

    // Flat mean over every reading — Kotlin `RespiratoryRateStatisticsContent`
    // (values.average()) and the context card. (12*3 + 20) / 4 = 14.0.
    final flatMean = entries.map((e) => e.breathsPerMinute).reduce((a, b) => a + b) /
        entries.length;
    expect(flatMean, 14.0);

    // Mean of daily-bucket averages — Kotlin `respiratoryRateAverage(
    // respiratoryRateBuckets(...))` feeding the chart summary + overview value.
    // (12 + 20) / 2 = 16.0, NOT the flat 14.0.
    final summaries = respiratoryRateDaySummaries(entries);
    expect(summaries.length, 2);
    final bucketedMean = summaries.map((s) => s.average).reduce((a, b) => a + b) /
        summaries.length;
    expect(bucketedMean, 16.0);
    expect(flatMean, isNot(bucketedMean));
  });

  testWidgets('renders the three reorderable group sections', (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Heart'), findsOneWidget);
    expect(find.text('Cardiovascular'), findsOneWidget);
    expect(find.text('Respiratory'), findsOneWidget);
    // Kotlin `VitalsOverviewContent` renders 2-per-row summary cards plus the
    // per-group trend charts.
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.byType(MetricLineChart), findsWidgets);
  });

  testWidgets('renders the data-source education item after the sections',
      (tester) async {
    // Kotlin `HeartVitalsOverviewScreen` renders `dataSourceEducationItem()` as
    // a bare trailing item after the grouped sections (line 155).
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    expect(find.byType(DataSourceEducationItem), findsOneWidget);
    expect(find.text('Manage data sources'), findsOneWidget);
  });

  testWidgets('renders the three heart-section MetricLineCharts', (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    // Heart rate + resting HR + HRV charts (Kotlin `HeartOverviewChartsContent`),
    // plus the skin-temperature chart, all render for the week range.
    final charts = find.byType(MetricLineChart);
    expect(charts, findsWidgets);
    expect(tester.widgetList(charts).length, greaterThanOrEqualTo(3));
  });

  testWidgets('lays the summary metrics out two per row', (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    // The heart section's first two cards sit side by side on the same row.
    final firstCard = find.ancestor(
      of: find.text('Average heart rate'),
      matching: find.byType(MetricCard),
    );
    final secondCard = find.ancestor(
      of: find.text('Resting heart rate'),
      matching: find.byType(MetricCard),
    );
    expect(firstCard, findsOneWidget);
    expect(secondCard, findsOneWidget);
    final firstTop = tester.getTopLeft(firstCard);
    final secondTop = tester.getTopLeft(secondCard);
    expect((firstTop.dy - secondTop.dy).abs(), lessThan(1));
    expect(secondTop.dx, greaterThan(firstTop.dx));
  });

  testWidgets('includes the skin temperature card in the respiratory section',
      (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    // The card title, and (with two readings) the trend-chart title, both show.
    expect(
      find.ancestor(
        of: find.text('Skin temperature'),
        matching: find.byType(MetricCard),
      ),
      findsOneWidget,
    );
  });

  testWidgets('changing the range selector reloads the period', (tester) async {
    final heartRepo = _FakeHeartRepository(data: _heartData());
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
      heartRepository: heartRepo,
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    // Defaults to the persisted HEART range (week).
    expect(container.read(heartVitalsOverviewNotifierProvider).selectedRange,
        TimeRange.week);

    await tester.tap(find.text('Month'));
    await tester.pumpAndSettle();

    expect(container.read(heartVitalsOverviewNotifierProvider).selectedRange,
        TimeRange.month);
    expect(heartRepo.queries.map((q) => q.range), contains(TimeRange.month));
  });

  testWidgets('tapping the skin temperature row opens its metric route',
      (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    final row = find.ancestor(
      of: find.text('Skin temperature'),
      matching: find.byType(InkWell),
    );
    await tester.ensureVisible(row);
    await tester.pumpAndSettle();
    await tester.tap(row);
    await tester.pumpAndSettle();

    expect(find.text('metric: SKIN_TEMPERATURE'), findsOneWidget);
  });

  testWidgets('reordering a section persists across a rebuild', (tester) async {
    final container = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(await _bootstrap(container: container));
    await tester.pumpAndSettle();

    // Move the respiratory section ahead of the heart section.
    container.read(metricDetailSectionOrderProvider.notifier).moveSectionToTarget(
          MetricDetailSectionId.vitalsRespiratorySection,
          MetricDetailSectionId.vitalsHeartSection,
        );

    // Persisted to the preferences repository: respiratory now precedes heart.
    final stored =
        container.read(preferencesRepositoryProvider).metricDetailSectionOrder();
    expect(stored, isNotNull);
    expect(
      stored!.indexOf(
              MetricDetailSectionId.vitalsRespiratorySection.storageName) <
          stored.indexOf(MetricDetailSectionId.vitalsHeartSection.storageName),
      isTrue,
    );

    // A fresh container reading the same SharedPreferences honours the order.
    final rebuilt = await _container(
      heart: _heartData(),
      vitals: _vitalsData(now),
      sharedPreferences: container.read(sharedPreferencesProvider),
    );
    addTearDown(rebuilt.dispose);
    final order = rebuilt.read(metricDetailSectionOrderProvider);
    expect(
      order.indexOf(MetricDetailSectionId.vitalsRespiratorySection) <
          order.indexOf(MetricDetailSectionId.vitalsHeartSection),
      isTrue,
    );
  });
}
