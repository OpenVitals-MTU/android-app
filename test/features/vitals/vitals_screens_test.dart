import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/query/heart_period_data.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';
import 'package:openvitals/features/vitals/presentation/vitals_screens.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/charts/line_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

class _FakeHeartRepository implements HeartRepository {
  @override
  Future<Result<HeartPeriodData>> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      const Ok(HeartPeriodData());

  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(
          LocalDate start, LocalDate end) async =>
      const Ok(<DailyHrv>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeVitalsRepository implements VitalsRepository {
  _FakeVitalsRepository({this.bloodPressure = const <BloodPressureEntry>[]});

  final List<BloodPressureEntry> bloodPressure;
  final List<(VitalsMeasurementType, String)> deletedEntries = [];

  @override
  Set<String> get phase3Permissions => const <String>{};

  @override
  Future<VitalsPeriodData> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      VitalsPeriodData(bloodPressure: bloodPressure);

  @override
  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async {
    deletedEntries.add((type, id));
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

BloodPressureEntry _bp(
  DateTime time,
  int systolic,
  int diastolic, {
  String id = '',
  bool isOpenVitalsEntry = false,
}) =>
    BloodPressureEntry(
      time: time,
      systolicMmHg: systolic,
      diastolicMmHg: diastolic,
      source: 'test',
      id: id,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

Future<Widget> _bootstrap({
  required _FakeVitalsRepository vitalsRepository,
  required Set<String> granted,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      heartRepositoryProvider.overrideWithValue(_FakeHeartRepository()),
      vitalsRepositoryProvider.overrideWithValue(vitalsRepository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const BloodPressureScreen(),
    ),
  );
}

void main() {
  final now = DateTime.now().toUtc();

  testWidgets('Blood pressure screen renders the ordered sections once loaded',
      (tester) async {
    final repo = _FakeVitalsRepository(
      bloodPressure: [
        _bp(now.subtract(const Duration(hours: 2)), 118, 76),
        _bp(now.subtract(const Duration(days: 1)), 122, 80),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        vitalsRepository: repo,
        granted: {HcPermissions.readBloodPressure},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // PERIOD_CHART (systolic + diastolic series).
    expect(find.byType(MetricLineChart), findsOneWidget);
    // METRIC_CONTEXT: the AHA category card for the latest reading.
    expect(find.text('Blood pressure category'), findsOneWidget);
    // STATISTICS with the Latest systolic/diastolic pair.
    expect(find.text('Statistics'), findsOneWidget);
    expect(find.text('Latest'), findsOneWidget);
    expect(find.text('118/76'), findsWidgets);
    // DATA_CONFIDENCE + ENTRIES.
    expect(find.text('Data confidence'), findsOneWidget);
    expect(find.text('Entries'), findsOneWidget);
  });

  testWidgets('Blood pressure screen shows the access gate when missing',
      (tester) async {
    final repo = _FakeVitalsRepository(bloodPressure: [_bp(now, 118, 76)]);
    await tester.pumpWidget(
      await _bootstrap(
        vitalsRepository: repo,
        granted: const <String>{},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricLineChart), findsNothing);
  });

  testWidgets('Blood pressure screen shows placeholder with no readings',
      (tester) async {
    final repo = _FakeVitalsRepository();
    await tester.pumpWidget(
      await _bootstrap(
        vitalsRepository: repo,
        granted: {HcPermissions.readBloodPressure},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
  });

  testWidgets('Swipe-deleting a manual entry calls the vitals repository',
      (tester) async {
    final repo = _FakeVitalsRepository(
      bloodPressure: [
        _bp(
          now.subtract(const Duration(hours: 2)),
          118,
          76,
          id: 'bp-1',
          isOpenVitalsEntry: true,
        ),
      ],
    );
    await tester.pumpWidget(
      await _bootstrap(
        vitalsRepository: repo,
        granted: {HcPermissions.readBloodPressure},
      ),
    );
    await tester.pumpAndSettle();

    final row = find.text('118/76 mmHg');
    expect(row, findsOneWidget);
    await tester.ensureVisible(row);
    await tester.pumpAndSettle();
    await tester.drag(row, const Offset(-500, 0), warnIfMissed: false);
    await tester.pumpAndSettle();

    expect(
      repo.deletedEntries,
      [(VitalsMeasurementType.bloodPressure, 'bp-1')],
    );
    // The deleted entry is dropped from the list without a reload.
    expect(find.text('118/76 mmHg'), findsNothing);
  });
}
