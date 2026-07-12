import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/domain/query/heart_period_data.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';
import 'package:openvitals/features/heart/presentation/heart_metric_cards.dart';
import 'package:openvitals/features/heart/presentation/heart_metric_screen.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/line_chart.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

class _FakeHeartRepository implements HeartRepository {
  _FakeHeartRepository({this.summaries = const <HeartRateSummary>[]});

  final List<HeartRateSummary> summaries;

  @override
  Future<HeartPeriodData> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      HeartPeriodData(dailySummaries: summaries);

  @override
  Future<List<DailyHrv>> loadDailyHRV(LocalDate start, LocalDate end) async =>
      const <DailyHrv>[];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeVitalsRepository implements VitalsRepository {
  @override
  Set<String> get phase3Permissions => const <String>{};

  @override
  Future<VitalsPeriodData> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      const VitalsPeriodData();

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

HeartRateSummary _summary(LocalDate date, int avg) => HeartRateSummary(
      date: date,
      avgBpm: avg,
      minBpm: avg - 10,
      maxBpm: avg + 20,
    );

Future<(Widget, SharedPreferences)> _bootstrap({
  required _FakeHeartRepository heartRepository,
  required Set<String> granted,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final app = ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      heartRepositoryProvider.overrideWithValue(heartRepository),
      vitalsRepositoryProvider.overrideWithValue(_FakeVitalsRepository()),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: HeartRateScreen(),
    ),
  );
  return (app, prefs);
}

void main() {
  final today = LocalDate.now();

  Future<SharedPreferences> pumpLoadedHeartRateScreen(
    WidgetTester tester,
  ) async {
    final repo = _FakeHeartRepository(
      summaries: [
        _summary(today, 68),
        _summary(today.minusDays(1), 72),
      ],
    );
    final (app, prefs) = await _bootstrap(
      heartRepository: repo,
      granted: {HcPermissions.readHeartRate},
    );
    await tester.pumpWidget(app);
    await tester.pumpAndSettle();
    return prefs;
  }

  testWidgets('Heart rate screen renders the ordered period sections',
      (tester) async {
    await pumpLoadedHeartRateScreen(tester);

    expect(tester.takeException(), isNull);
    // PERIOD_CHART
    expect(find.byType(MetricLineChart), findsOneWidget);
    // DAILY_GOAL slot: the threshold checks with their two stepper cards.
    expect(find.text('Heart rate checks'), findsOneWidget);
    expect(find.byType(HeartRateThresholdCheckCard), findsNWidgets(2));
    // STATISTICS with the previous-period/baseline grid host.
    expect(find.text('Statistics'), findsOneWidget);
    expect(find.text('Average'), findsWidgets);
    // ENTRIES: the per-day breakdown rows.
    expect(find.text('Daily breakdown'), findsOneWidget);
    expect(find.byType(HeartRateDayRow), findsWidgets);
    // DATA_CONFIDENCE
    expect(find.text('Data confidence'), findsOneWidget);
  });

  testWidgets(
      'Average heart rate period view renders the data-source education item',
      (tester) async {
    // Kotlin `averageHeartRateContent` appends `dataSourceEducationItem()` after
    // the period sections (HeartMetricContent.kt:219).
    await pumpLoadedHeartRateScreen(tester);

    expect(find.byType(DataSourceEducationItem), findsOneWidget);
    expect(find.text('Manage data sources'), findsOneWidget);
  });

  testWidgets('Heart rate screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeHeartRepository(summaries: [_summary(today, 68)]);
    final (app, _) = await _bootstrap(
      heartRepository: repo,
      granted: const <String>{},
    );
    await tester.pumpWidget(app);
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricLineChart), findsNothing);
  });

  testWidgets('Heart rate screen shows the empty placeholder with no data',
      (tester) async {
    final (app, _) = await _bootstrap(
      heartRepository: _FakeHeartRepository(),
      granted: {HcPermissions.readHeartRate},
    );
    await tester.pumpWidget(app);
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricLineChart), findsNothing);
  });

  testWidgets('Threshold steppers persist to SharedPreferences',
      (tester) async {
    final prefs = await pumpLoadedHeartRateScreen(tester);

    final increaseButtons = find.byTooltip('Increase heart rate threshold');
    await tester.ensureVisible(increaseButtons.first);
    await tester.pumpAndSettle();
    // The first card is the high-heart-rate check (Kotlin order).
    await tester.tap(increaseButtons.first, warnIfMissed: false);
    await tester.pumpAndSettle();
    expect(prefs.getInt('high_heart_rate_threshold_bpm'), 125);

    final decreaseButtons = find.byTooltip('Decrease heart rate threshold');
    await tester.tap(decreaseButtons.last, warnIfMissed: false);
    await tester.pumpAndSettle();
    expect(prefs.getInt('low_heart_rate_threshold_bpm'), 45);
  });

  testWidgets('Section reorder persists the new order', (tester) async {
    final prefs = await pumpLoadedHeartRateScreen(tester);

    final context = tester.element(find.byType(HeartRateScreen));
    final container = ProviderScope.containerOf(context);
    container
        .read(metricDetailSectionOrderProvider.notifier)
        .moveSection(MetricDetailSectionId.statistics, -1);
    await tester.pumpAndSettle();

    final stored = prefs.getString('metric_detail_section_order');
    expect(stored, isNotNull);
    final order = stored!.split(',');
    expect(
      order.indexOf('STATISTICS'),
      lessThan(order.indexOf('DAILY_GOAL')),
    );
  });
}
