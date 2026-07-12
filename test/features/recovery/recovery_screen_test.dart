import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/recovery/recovery_screen.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/period_navigator.dart';

class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase(this._build) : super(DashboardDataLoader(HealthDataSource()));

  final DashboardData Function(DashboardQuery query) _build;

  @override
  Future<DashboardData> call(DashboardQuery query) async => _build(query);
}

DashboardData _sampleData(DashboardQuery query) => DashboardData(
      date: query.date,
      avgHeartRateBpm: 90,
      restingHeartRateBpm: 58,
      restingHeartRateBaselineBpm: 52,
      hrvRmssdMs: 40,
      hrvBaselineRmssdMs: 55,
      hrvSampleCount: 6,
      loadedMetrics: query.visibleMetrics,
    );

Future<Widget> _bootstrap({required Set<String> granted}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
      loadDashboardDayUseCaseProvider
          .overrideWithValue(_FakeUseCase(_sampleData)),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: StressDetailsScreen(date: '${LocalDate.now()}'),
    ),
  );
}

void main() {
  testWidgets('Recovery renders the stress score card once loaded',
      (tester) async {
    tester.view.physicalSize = const Size(1200, 6000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      await _bootstrap(
        granted: {HcPermissions.readHeartRate, HcPermissions.readHrv},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DayNavigator), findsOneWidget);
    expect(find.text('Stress tracking'), findsOneWidget);
    // The data-source education link closes the loaded stress content.
    expect(find.byType(DataSourceEducationItem), findsOneWidget);
  });

  testWidgets('Recovery shows the access gate when permission missing',
      (tester) async {
    await tester.pumpWidget(await _bootstrap(granted: const <String>{}));
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(DayNavigator), findsNothing);
  });
}
