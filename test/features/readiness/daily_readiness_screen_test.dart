import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/readiness/presentation/daily_readiness_screen.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/period_navigator.dart';

class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase(this._build) : super(DashboardDataLoader(HealthDataSource()));

  final DashboardData Function(DashboardQuery query) _build;

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async => Ok(_build(query));
}

DashboardData _sampleData(DashboardQuery query) => DashboardData(
      date: query.date,
      avgHeartRateBpm: 72,
      restingHeartRateBpm: 55,
      restingHeartRateBaselineBpm: 54,
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
      home: const DailyReadinessScreen(),
    ),
  );
}

void main() {
  testWidgets('Daily readiness renders the panel + day navigator once loaded',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        granted: {HcPermissions.readHeartRate, HcPermissions.readSleep},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DayNavigator), findsOneWidget);
    expect(find.text('Daily readiness'), findsWidgets);
    expect(find.text('Score'), findsOneWidget);
    // The data-source education link closes the readiness panel.
    expect(find.byType(DataSourceEducationItem), findsOneWidget);
  });

  testWidgets('Daily readiness shows the access gate when permission missing',
      (tester) async {
    await tester.pumpWidget(await _bootstrap(granted: const <String>{}));
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(DayNavigator), findsNothing);
  });
}
