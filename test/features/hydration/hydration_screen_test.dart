import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/hydration_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/hydration_period_data.dart';
import 'package:openvitals/features/hydration/hydration_screen.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

/// A fake [HydrationRepository] returning canned period data.
class _FakeHydrationRepository implements HydrationRepository {
  _FakeHydrationRepository({
    this.dailyHydration = const <DailyHydration>[],
    this.entries = const <HydrationEntry>[],
  });

  final List<DailyHydration> dailyHydration;
  final List<HydrationEntry> entries;

  @override
  double hydrationDailyGoalLiters() => 2.0;

  @override
  Future<HydrationPeriodData> loadHydrationPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      HydrationPeriodData(
        dailyHydration: dailyHydration,
        hydrationEntries: entries,
      );

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

HydrationEntry _drink(DateTime start, double liters, String source) =>
    HydrationEntry(
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: source,
    );

Future<Widget> _bootstrap({
  required _FakeHydrationRepository repository,
  required Set<String> granted,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      hydrationRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: HydrationScreen(),
    ),
  );
}

void main() {
  final today = LocalDate.now();

  testWidgets('Hydration screen renders summary + bar chart once loaded',
      (tester) async {
    final now = DateTime.now();
    final repo = _FakeHydrationRepository(
      dailyHydration: [
        DailyHydration(date: today, liters: 1.8),
        DailyHydration(date: today.minusDays(1), liters: 2.2),
      ],
      entries: [
        _drink(now, 0.5, 'Water bottle'),
        _drink(now.subtract(const Duration(hours: 2)), 0.3, 'Water bottle'),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readHydration}),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.byType(MetricBarChart), findsOneWidget);
    expect(find.text('Total hydration'), findsOneWidget);
  });

  testWidgets('Hydration screen shows the empty placeholder with no data',
      (tester) async {
    final repo = _FakeHydrationRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readHydration}),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
  });

  testWidgets('Hydration screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeHydrationRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: const <String>{}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
  });
}
