import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/settings/presentation/cards/body_energy_calibration_card.dart';
import 'package:openvitals/features/settings/presentation/cards/body_profile_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';

Future<SharedPreferences> _prefs([
  void Function(PreferencesRepository repo)? seed,
]) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  if (seed != null) seed(PreferencesRepository(prefs));
  return prefs;
}

Widget _host(SharedPreferences prefs, Widget child) {
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      // Widget tests must pin the unit system: the default follows the host
      // locale, which would make the weight field non-deterministic.
      unitSystemProvider.overrideWithValue(UnitSystem.metric),
    ],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: SingleChildScrollView(child: child)),
    ),
  );
}

Finder _fieldFor(String label) =>
    find.ancestor(of: find.text(label), matching: find.byType(TextField));

void main() {
  group('BodyProfileCard', () {
    testWidgets('seeds fields from stored profile', (tester) async {
      final prefs = await _prefs(
        (repo) => repo.setBodyProfile(
          const BodyProfile(
            birthYear: 1990,
            weightKg: 72.0,
          ),
        ),
      );
      await tester.pumpWidget(_host(prefs, const BodyProfileCard()));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.text('1990'), findsOneWidget);
      expect(find.text('72.0'), findsOneWidget); // metric kg
      // Resting and max heart rate are no longer settable anywhere: both are
      // derived from observed heart-rate data.
      expect(find.text('55'), findsNothing);
      expect(find.text('190'), findsNothing);
    });


    testWidgets('shows where weight came from', (tester) async {
      // Without the provenance the merge is invisible and the user cannot tell
      // why editing weight sometimes writes a Health Connect entry.
      final prefs = await _prefs(
        (repo) => repo.setBodyProfile(const BodyProfile(weightKg: 72.0)),
      );
      await tester.pumpWidget(_host(prefs, const BodyProfileCard()));
      await tester.pumpAndSettle();

      expect(find.text('Entered here'), findsWidgets);
    });

    testWidgets('editing a field and saving persists via bodyProfile()',
        (tester) async {
      final prefs = await _prefs(
        (repo) => repo.setBodyProfile(const BodyProfile(birthYear: 1980)),
      );
      await tester.pumpWidget(_host(prefs, const BodyProfileCard()));
      await tester.pumpAndSettle();

      final birthYear = _fieldFor('Birth year');
      await tester.ensureVisible(birthYear);
      await tester.enterText(birthYear, '1995');
      await tester.pumpAndSettle();

      final save = find.widgetWithText(FilledButton, 'Save');
      await tester.ensureVisible(save);
      await tester.tap(save);
      await tester.pumpAndSettle();

      expect(PreferencesRepository(prefs).bodyProfile().birthYear, 1995);
    });
  });

  group('the Body profile screen shows each fact once', () {
    testWidgets('the birth year is not asked for twice', (tester) async {
      // The calibration card carries a birth year for the Body Energy screen,
      // where it stands alone as the setup gate. On this screen the Body card
      // is directly above it with the same field, and shipping both put two
      // boxes for one number on one screen -- disagreeing until you noticed
      // they were the same number.
      final prefs = await _prefs(
        (repo) => repo.setBodyProfile(const BodyProfile(birthYear: 1993)),
      );

      await tester.pumpWidget(_host(
        prefs,
        const Column(children: [
          BodyProfileCard(),
          BodyEnergyCalibrationCard(showBirthYear: false),
        ]),
      ));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(TextField, 'Birth year'), findsOneWidget);
      expect(find.text('1993'), findsOneWidget);
    });

    testWidgets('but standalone it still carries one', (tester) async {
      // The Body Energy screen has no other route to the field, and setup will
      // not complete without it.
      final prefs = await _prefs((_) {});

      await tester
          .pumpWidget(_host(prefs, const BodyEnergyCalibrationCard()));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(TextField, 'Birth year'), findsOneWidget);
    });
  });

  group('BodyEnergyCalibrationCard', () {
    testWidgets('toggling manual zones reveals the five zone fields',
        (tester) async {
      final prefs = await _prefs();
      await tester
          .pumpWidget(_host(prefs, const BodyEnergyCalibrationCard()));
      await tester.pumpAndSettle();

      expect(find.text('Zone 1 lower bpm'), findsNothing);

      await tester.tap(find.byType(Switch));
      await tester.pumpAndSettle();

      expect(find.text('Zone 1 lower bpm'), findsOneWidget);
      expect(find.text('Zone 5 lower bpm'), findsOneWidget);
    });

    testWidgets('editing zones and saving persists and completes setup',
        (tester) async {
      final prefs = await _prefs();
      await tester
          .pumpWidget(_host(prefs, const BodyEnergyCalibrationCard()));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(Switch));
      await tester.pumpAndSettle();

      const labels = [
        'Zone 1 lower bpm',
        'Zone 2 lower bpm',
        'Zone 3 lower bpm',
        'Zone 4 lower bpm',
        'Zone 5 lower bpm',
      ];
      const values = ['90', '110', '130', '150', '170'];
      for (var i = 0; i < labels.length; i++) {
        final field = _fieldFor(labels[i]);
        await tester.ensureVisible(field);
        await tester.enterText(field, values[i]);
        await tester.pumpAndSettle();
      }

      final save = find.widgetWithText(FilledButton, 'Save');
      await tester.ensureVisible(save);
      await tester.tap(save);
      await tester.pumpAndSettle();

      final saved = PreferencesRepository(prefs).bodyEnergyCalibration();
      expect(saved.useManualZones, isTrue);
      expect(saved.setupCompleted, isTrue);
      expect(saved.manualZoneThresholdsBpm?.zone1LowerBpm, 90);
      expect(saved.manualZoneThresholdsBpm?.zone5LowerBpm, 170);
    });

    testWidgets('switching manual zones off keeps the typed ladder',
        (tester) async {
      final prefs = await _prefs(
        (repo) => repo.setBodyEnergyCalibration(
          const BodyEnergyCalibration(
            manualZoneThresholdsBpm: HeartZoneThresholds(
              zone1LowerBpm: 90,
              zone2LowerBpm: 110,
              zone3LowerBpm: 130,
              zone4LowerBpm: 150,
              zone5LowerBpm: 170,
            ),
            useManualZones: true,
            setupCompleted: true,
          ),
        ),
      );
      await tester
          .pumpWidget(_host(prefs, const BodyEnergyCalibrationCard()));
      await tester.pumpAndSettle();

      // The toggle is the only control now. "Use automatic estimates" was
      // removed with the manual resting and max heart rate inputs, and it is
      // the one behavioural difference that came with removing it: switching
      // manual zones OFF stops them applying but no longer ERASES them, so a
      // ladder typed once can be switched back on rather than retyped.
      await tester.ensureVisible(find.byType(Switch));
      await tester.tap(find.byType(Switch));
      await tester.pumpAndSettle();
      // Switching manual zones OFF hands the ladder to the automatic path,
      // which needs a birth year, so setup will not save without one.
      await tester.enterText(
        find.widgetWithText(TextField, 'Birth year'),
        '1990',
      );
      final save = find.widgetWithText(FilledButton, 'Save');
      await tester.ensureVisible(save);
      await tester.tap(save);
      await tester.pumpAndSettle();

      final saved = PreferencesRepository(prefs).bodyEnergyCalibration();
      expect(saved.useManualZones, isFalse);
      expect(saved.manualZoneThresholdsBpm, isNotNull,
          reason: 'switched off, not thrown away');
      expect(saved.setupCompleted, isTrue);
    });
  });
}
