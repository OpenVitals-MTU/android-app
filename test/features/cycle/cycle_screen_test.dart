import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/cycle_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/cycle_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/cycle_period_data.dart';
import 'package:openvitals/features/cycle/cycle_screen.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

class _FakeCycleRepository implements CycleRepository {
  _FakeCycleRepository({this.data = const CycleData()});

  final CycleData data;

  @override
  Set<String> get phase4Permissions => const <String>{};

  @override
  Future<Set<String>> missingPermissions() async => const <String>{};

  @override
  Future<CyclePeriodData> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      CyclePeriodData(data: data, missingPermissions: const <String>{});

  @override
  Future<CycleData> loadCycleData(LocalDate start, LocalDate end) async => data;
}

Future<Widget> _bootstrap({
  required _FakeCycleRepository repository,
  required Set<String> granted,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      cycleRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const CycleScreen(),
    ),
  );
}

void main() {
  testWidgets('renders summary + observations once loaded', (tester) async {
    final now = DateTime.now();
    final data = CycleData(
      menstruationPeriods: [
        MenstruationPeriodEntry(
          startTime: now.subtract(const Duration(days: 3)),
          endTime: now,
          source: 'com.openvitals',
        ),
      ],
      ovulationTests: [
        OvulationTestEntry(time: now, result: 1, source: 'com.openvitals'),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: _FakeCycleRepository(data: data),
        granted: {HcPermissions.readMenstruationPeriod},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.text('Period days'), findsWidgets);
    expect(find.text('Ovulation test'), findsOneWidget);
  });

  testWidgets('gates the screen when the cycle permission is missing',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        repository: _FakeCycleRepository(),
        granted: const <String>{},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricCard), findsNothing);
  });

  testWidgets('shows the empty placeholder with no data', (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        repository: _FakeCycleRepository(),
        granted: {HcPermissions.readMenstruationPeriod},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
  });
}
