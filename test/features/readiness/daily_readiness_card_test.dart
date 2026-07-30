import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/readiness/presentation/daily_readiness_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/data_source_education_item.dart';

/// The Daily Readiness panel, now a card hosted by the Body Energy screen. The
/// screen it used to be is gone; these pin what survived the move.
class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase(this._build) : super(DashboardDataLoader(HealthDataSource()));

  final DashboardData Function(DashboardQuery query) _build;

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async =>
      Ok(_build(query));
}

DashboardData _sampleData(DashboardQuery query) => DashboardData(
      date: query.date,
      avgHeartRateBpm: 72,
      restingHeartRateBpm: 55,
      restingHeartRateBaselineBpm: 54,
      loadedMetrics: query.visibleMetrics,
    );

Future<Widget> _bootstrap({required LocalDate date}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      loadDashboardDayUseCaseProvider
          .overrideWithValue(_FakeUseCase(_sampleData)),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(
        body: SingleChildScrollView(child: DailyReadinessCard(date: date)),
      ),
    ),
  );
}

void main() {
  testWidgets('renders the readiness verdict for the host day', (tester) async {
    await tester.pumpWidget(await _bootstrap(date: LocalDate.now()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Daily readiness'), findsOneWidget);
    expect(find.text('Score'), findsOneWidget);
    expect(find.byType(DataSourceEducationItem), findsOneWidget);
  });

  testWidgets('no self-link: the card offers Training but not Body energy',
      (tester) async {
    // The panel used to link to Body Energy; it now lives INSIDE the Body
    // Energy view, and a card linking to its own screen would be a loop.
    await tester.pumpWidget(await _bootstrap(date: LocalDate.now()));
    await tester.pumpAndSettle();

    expect(find.text('Training'), findsOneWidget);
    expect(find.text('Body energy'), findsNothing);
  });

  testWidgets('a day the provider has not reached yet shows a placeholder',
      (tester) async {
    // The host keeps the provider pointed at its selected day; until the load
    // lands the card must not show ANOTHER day's verdict.
    await tester
        .pumpWidget(await _bootstrap(date: LocalDate.now().minusDays(3)));
    // Not pumpAndSettle: the placeholder is an animated spinner that never
    // settles, which is the point — the card is waiting for its host.
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 200));

    expect(find.text('Score'), findsNothing);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
