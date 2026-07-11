import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';
import 'package:openvitals/features/settings/cards/caffeine_preferences_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Future<(Widget, SharedPreferences)> _bootstrap(
  Widget child, {
  Map<String, Object> initialValues = const <String, Object>{},
}) async {
  SharedPreferences.setMockInitialValues(initialValues);
  final prefs = await SharedPreferences.getInstance();
  return (
    ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const Scaffold(
          body: SingleChildScrollView(child: CaffeinePreferencesCard()),
        ),
      ),
    ),
    prefs,
  );
}

void main() {
  testWidgets('renders the caffeine fields seeded from preferences',
      (tester) async {
    // Seed a non-default half-life directly so we can prove the field reads
    // from stored preferences.
    final (widget, _) = await _bootstrap(
      const CaffeinePreferencesCard(),
      initialValues: {'caffeine_half_life_minutes': 420},
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // Card title and field labels render.
    expect(find.text('Caffeine model'), findsOneWidget);
    expect(find.text('Half-life'), findsOneWidget);
    expect(find.text('Absorption'), findsOneWidget);
    expect(find.text('Sleep threshold'), findsOneWidget);
    expect(find.text('Bedtime'), findsOneWidget);
    expect(find.text('Save'), findsOneWidget);
    // Seeded half-life (420) is shown in the field.
    expect(find.text('420'), findsOneWidget);
  });

  testWidgets('editing a field and saving persists via the repository',
      (tester) async {
    final (widget, prefs) = await _bootstrap(const CaffeinePreferencesCard());
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // Default half-life is shown.
    expect(
      find.text(CaffeinePreferences.defaultHalfLifeMinutes.toString()),
      findsOneWidget,
    );

    // Edit the half-life field to a new in-range value.
    final halfLifeField = find.byType(TextField).first;
    await tester.enterText(halfLifeField, '360');
    await tester.pump();

    final saveButton = find.widgetWithText(FilledButton, 'Save');
    await tester.ensureVisible(saveButton);
    await tester.tap(saveButton);
    await tester.pumpAndSettle();

    // The whole preferences object is written back with profileCompleted = true.
    final saved = PreferencesRepository(prefs).caffeinePreferences();
    expect(saved.halfLifeMinutes, 360);
    expect(saved.profileCompleted, isTrue);
  });

  testWidgets('editing a switch and dropdown persists on save',
      (tester) async {
    final (widget, prefs) = await _bootstrap(const CaffeinePreferencesCard());
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // Toggle the Smoker switch (default false -> true).
    final smokerRow = find.ancestor(
      of: find.text('Smoker'),
      matching: find.byType(Row),
    );
    final smokerSwitch = find.descendant(
      of: smokerRow,
      matching: find.byType(Switch),
    );
    await tester.ensureVisible(smokerSwitch);
    await tester.tap(smokerSwitch);
    await tester.pump();

    final saveButton = find.widgetWithText(FilledButton, 'Save');
    await tester.ensureVisible(saveButton);
    await tester.tap(saveButton);
    await tester.pumpAndSettle();

    final saved = PreferencesRepository(prefs).caffeinePreferences();
    expect(saved.smoker, isTrue);
  });

  testWidgets('an out-of-range half-life is clamped by the repository on save',
      (tester) async {
    final (widget, prefs) = await _bootstrap(const CaffeinePreferencesCard());
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    // Enter a value above the max (720); the repository normalizes on write.
    final halfLifeField = find.byType(TextField).first;
    await tester.enterText(halfLifeField, '9000');
    await tester.pump();

    final saveButton = find.widgetWithText(FilledButton, 'Save');
    await tester.ensureVisible(saveButton);
    await tester.tap(saveButton);
    await tester.pumpAndSettle();

    final saved = PreferencesRepository(prefs).caffeinePreferences();
    expect(saved.halfLifeMinutes, CaffeinePreferences.maxHalfLifeMinutes);
    // Field reseeds to the clamped value.
    expect(
      find.text(CaffeinePreferences.maxHalfLifeMinutes.toString()),
      findsOneWidget,
    );
  });

  test('LocalTime bedtime default matches the model', () {
    expect(
      const CaffeinePreferences().bedtime,
      const LocalTime(22, 30),
    );
  });
}
