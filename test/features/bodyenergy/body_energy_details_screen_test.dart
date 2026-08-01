import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/readiness/presentation/daily_readiness_card.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/bodyenergy/presentation/body_energy_details_screen.dart';
import 'package:openvitals/features/bodyenergy/presentation/body_energy_timeline_chart.dart';
import 'package:openvitals/features/settings/presentation/cards/body_energy_calibration_card.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

/// The hosted readiness card loads a dashboard day of its own; the fake keeps
/// that read off the platform channels.
class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase() : super(DashboardDataLoader(HealthDataSource()));

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async =>
      Ok(DashboardData(
        date: query.date,
        avgHeartRateBpm: 72,
        restingHeartRateBpm: 55,
        loadedMetrics: query.visibleMetrics,
      ));
}

class _FakeBodyEnergyRepository implements BodyEnergyRepository {
  _FakeBodyEnergyRepository(this.timeline);

  final BodyEnergyTimeline? timeline;

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async =>
      Ok(BodyEnergyTimelineResult(
        query: query,
        days: timeline == null ? const [] : [timeline!],
      ));
}

BodyEnergyTimelinePoint _point(LocalDate date, int hour, int score) =>
    BodyEnergyTimelinePoint(
      time: date.atTimeInstant(hour),
      score: score,
      delta: 1,
      state: BodyEnergyBucketState.rest,
      confidence: BodyEnergyConfidence.high,
    );

BodyEnergyTimeline _timeline(LocalDate date) => BodyEnergyTimeline(
      date: date,
      startScore: 50,
      currentScore: 62,
      charged: 14,
      drained: 2,
      points: [
        _point(date, 7, 54),
        _point(date, 12, 60),
        _point(date, 17, 62),
      ],
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'Heart-rate intensity has strong calibration.',
    );

Future<Widget> _bootstrap({
  required BodyEnergyTimeline? timeline,
  required Set<String> granted,
  bool setupCompleted = true,
}) async {
  SharedPreferences.setMockInitialValues(<String, Object>{
    if (setupCompleted) 'body_energy_setup_completed': true,
    // A completed setup now REQUIRES a birth year or manual zones: automatic
    // zones are Tanaka against age, and an install with neither is reopened at
    // the setup card on purpose. Without this every screen test would be
    // testing that gate instead of what it is named for.
    if (setupCompleted) 'body_profile_birth_year': 1990,
  });
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      bodyEnergyRepositoryProvider
          .overrideWithValue(_FakeBodyEnergyRepository(timeline)),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
      loadDashboardDayUseCaseProvider.overrideWithValue(_FakeUseCase()),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: BodyEnergyDetailsScreen(date: '$today'),
    ),
  );
}

final LocalDate today = LocalDate.now();

void main() {
  testWidgets('Body Energy renders the timeline chart once loaded',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(BodyEnergyTimelineChart), findsOneWidget);
    expect(find.text('Body Energy'), findsWidgets);
  });

  testWidgets('the Daily Readiness card rides along on the same day',
      (tester) async {
    // The Daily Readiness screen merged into this one: the card must be here,
    // showing the verdict for the screen's selected day.
    tester.view.physicalSize = const Size(1000, 4000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(DailyReadinessCard), findsOneWidget);
    expect(find.text('Daily Readiness'), findsOneWidget);
    expect(find.text('Score'), findsOneWidget);
  });

  testWidgets('Body Energy renders the "how it is estimated" card',
      (tester) async {
    // A tall surface so every card lays out in the viewport at once.
    tester.view.physicalSize = const Size(1000, 3000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
      ),
    );
    await tester.pumpAndSettle();

    final l10n = await AppLocalizations.delegate.load(const Locale('en'));
    expect(find.text(l10n.bodyEnergyCalculationTitle), findsOneWidget);
    // Localized section labels are present (no hardcoded English literals).
    expect(find.text(l10n.bodyEnergyWhyTitle), findsOneWidget);
    expect(find.text(l10n.bodyEnergyInputsTitle), findsOneWidget);
  });

  testWidgets('Body Energy shows only the calibration card until setup completes',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
        setupCompleted: false,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(BodyEnergyCalibrationCard), findsOneWidget);
    expect(find.byType(BodyEnergyTimelineChart), findsNothing);
  });

  testWidgets('Body Energy reveals the timeline after calibration is saved',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
        setupCompleted: false,
      ),
    );
    await tester.pumpAndSettle();

    final l10n = await AppLocalizations.delegate.load(const Locale('en'));
    expect(find.byType(BodyEnergyCalibrationCard), findsOneWidget);

    // Save completes setup (setupCompleted = true), but only once there is a
    // birth year: automatic zones are Tanaka against age, so setup refuses to
    // pass without one. It used to be "Use automatic estimates", which was
    // removed along with the manual resting and max heart rate inputs.
    await tester.enterText(
      find.widgetWithText(TextField, l10n.bodyEnergyCalibrationBirthYear),
      '1990',
    );
    await tester.tap(find.text(l10n.actionSave));
    await tester.pumpAndSettle();

    expect(find.byType(BodyEnergyCalibrationCard), findsNothing);
    expect(find.byType(BodyEnergyTimelineChart), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text(l10n.bodyEnergyCalculationTitle),
      300,
    );
    expect(find.text(l10n.bodyEnergyCalculationTitle), findsOneWidget);
  });

  testWidgets('Body Energy setup refuses to complete without a birth year',
      (tester) async {
    // The whole point of requiring it. Automatic zones are derived from Tanaka
    // against age; with no age the model falls back to resting + 70, which is
    // not a rougher ladder but a wrong one -- a resting 60 claims a maximum of
    // 130 and reads ordinary effort as zone 5.
    await tester.pumpWidget(
      await _bootstrap(
        timeline: _timeline(today),
        granted: {HcPermissions.readHeartRate},
        setupCompleted: false,
      ),
    );
    await tester.pumpAndSettle();

    final l10n = await AppLocalizations.delegate.load(const Locale('en'));
    await tester.tap(find.text(l10n.actionSave));
    await tester.pumpAndSettle();

    expect(find.byType(BodyEnergyCalibrationCard), findsOneWidget,
        reason: 'setup must not complete');
    expect(find.text(l10n.bodyEnergyCalibrationBirthYearRequired),
        findsOneWidget);
  });

  testWidgets('Body Energy shows the access gate when permission missing',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(timeline: _timeline(today), granted: const <String>{}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(BodyEnergyTimelineChart), findsNothing);
  });

  testWidgets('Body Energy shows the empty state with no timeline',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(timeline: null, granted: {HcPermissions.readHeartRate}),
    );
    await tester.pumpAndSettle();

    final l10n = await AppLocalizations.delegate.load(const Locale('en'));
    expect(find.byType(BodyEnergyTimelineChart), findsNothing);
    expect(find.text(l10n.bodyEnergyTimelineNoData), findsOneWidget);
  });
}
