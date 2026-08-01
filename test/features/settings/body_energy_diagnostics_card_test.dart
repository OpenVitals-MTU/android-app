import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/health_source_totals.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/features/settings/application/body_energy_diagnostics.dart';
import 'package:openvitals/features/settings/presentation/cards/body_energy_diagnostics_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

void main() {
  const date = LocalDate(2026, 7, 25);

  SourceDayTotal source(String package, double total) => SourceDayTotal(
        metric: HealthRecordSourceMetric.activeCalories,
        package: package,
        date: date,
        total: total,
        recordCount: 100,
        manualEntryCount: 0,
        coveredMinutes: 1400,
        firstStart: DateTime(2026, 7, 25),
        lastEnd: DateTime(2026, 7, 25, 23),
      );

  BodyEnergyDiagnosticsReport report({List<SourceDayTotal> sources = const []}) =>
      buildBodyEnergyDiagnostics(
        days: const [],
        watchSamplesByEpochDay: const {},
        modelInputByEpochDay: const {},
        sourceTotals: sources,
        calibration: const BodyEnergyCalibration(),
        watchSampleCount: 500,
      );

  Future<void> pump(WidgetTester tester, BodyEnergyDiagnosticsReport value) =>
      tester.pumpWidget(
        ProviderScope(
          overrides: [
            bodyEnergyDiagnosticsProvider.overrideWith((ref) async => value),
          ],
          child: const MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            home: Scaffold(
              body: SingleChildScrollView(child: BodyEnergyDiagnosticsCard()),
            ),
          ),
        ),
      );

  testWidgets('does not read anything until it is asked to', (tester) async {
    // A cold run is on the order of sixty Health Connect calls; scrolling past
    // the card must not spend them.
    await pump(tester, report());
    await tester.pumpAndSettle();

    expect(find.text('Run diagnostic'), findsOneWidget);
    expect(find.text('Copy report'), findsNothing);
    expect(find.textContaining('Body Energy calibration report'), findsNothing);
  });

  testWidgets('renders the report and offers a copy once run', (tester) async {
    await pump(tester, report());
    await tester.tap(find.text('Run diagnostic'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Body Energy calibration report'), findsOneWidget);
    expect(find.text('Copy report'), findsOneWidget);
    expect(find.text('Run again'), findsOneWidget);
  });

  testWidgets('warns when more than one app wrote active calories',
      (tester) async {
    await pump(
      tester,
      report(sources: [
        source('tech.mmarca.openvitals', 1100),
        source('com.garmin.android.apps.connectmobile', 1130),
      ]),
    );
    await tester.tap(find.text('Run diagnostic'));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('More than one app wrote active calories'),
      findsOneWidget,
    );
  });

  testWidgets('says so when no watch samples are stored', (tester) async {
    // Otherwise "the watch disagrees" and "the watch never synced" look alike.
    await pump(
      tester,
      buildBodyEnergyDiagnostics(
        days: const [],
        watchSamplesByEpochDay: const {},
        modelInputByEpochDay: const {},
        sourceTotals: const [],
        calibration: const BodyEnergyCalibration(),
      ),
    );
    await tester.tap(find.text('Run diagnostic'));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('No watch Body Battery samples stored'),
      findsOneWidget,
    );
  });
}
