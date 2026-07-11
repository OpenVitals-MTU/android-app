import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/bodyenergy/body_energy_details_screen.dart';
import 'package:openvitals/features/bodyenergy/body_energy_timeline_chart.dart';
import 'package:openvitals/features/settings/cards/body_energy_calibration_card.dart';
import 'package:openvitals/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';

class _FakeBodyEnergyRepository implements BodyEnergyRepository {
  _FakeBodyEnergyRepository(this.timeline);

  final BodyEnergyTimeline? timeline;

  @override
  Future<BodyEnergyTimelineResult> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async =>
      BodyEnergyTimelineResult(
        query: query,
        days: timeline == null ? const [] : [timeline!],
      );
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

    // "Use automatic estimates" completes setup (setupCompleted = true).
    await tester.tap(find.text(l10n.bodyEnergyCalibrationUseAuto));
    await tester.pumpAndSettle();

    expect(find.byType(BodyEnergyCalibrationCard), findsNothing);
    expect(find.byType(BodyEnergyTimelineChart), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text(l10n.bodyEnergyCalculationTitle),
      300,
    );
    expect(find.text(l10n.bodyEnergyCalculationTitle), findsOneWidget);
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
