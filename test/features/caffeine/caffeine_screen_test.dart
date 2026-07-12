import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/data/repository/contract/caffeine_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/features/caffeine/caffeine_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

/// A fake [CaffeineRepository] returning canned entries.
class _FakeCaffeineRepository implements CaffeineRepository {
  _FakeCaffeineRepository({this.entries = const <CaffeineEntry>[]});

  final List<CaffeineEntry> entries;

  @override
  Future<CaffeinePeriodData> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      CaffeinePeriodData(entries: entries);

  @override
  Future<CaffeinePeriodData> loadCaffeinePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      CaffeinePeriodData(entries: entries);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

CaffeineEntry _entry(DateTime start, double mg) => CaffeineEntry(
      id: 'entry-${start.millisecondsSinceEpoch}',
      startTime: start,
      endTime: start.add(const Duration(minutes: 10)),
      caffeineMg: mg,
      name: 'Coffee',
      source: 'Test source',
      mealType: 0,
    );

Future<Widget> _bootstrap({
  required _FakeCaffeineRepository repository,
  required Set<String> granted,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      caffeineRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: CaffeineScreen(),
    ),
  );
}

void main() {
  testWidgets('Caffeine screen renders curve + guidance cards once loaded',
      (tester) async {
    final now = DateTime.now().toUtc();
    final repo = _FakeCaffeineRepository(
      entries: [
        _entry(now.subtract(const Duration(hours: 2)), 95),
        _entry(now.subtract(const Duration(hours: 6)), 120),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readNutrition}),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // Top-of-scroll cards (the rest of the long list is lazily built off-screen).
    expect(find.text('Active caffeine now'), findsOneWidget);
    // The card used to hardcode this title in English, while `caffeineCurveTitle`
    // sat unused in the catalogs — translated, and never shown to anyone.
    expect(find.text('Caffeine curve'), findsOneWidget);
    expect(find.text('Caffeine dashboard'), findsOneWidget);
    expect(find.byType(CustomPaint), findsWidgets);
  });

  testWidgets('Caffeine screen shows the access gate when permission missing',
      (tester) async {
    final repo = _FakeCaffeineRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: const <String>{}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricCard), findsNothing);
  });
}
