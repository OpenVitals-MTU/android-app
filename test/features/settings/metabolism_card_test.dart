import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/presentation/cards/metabolism_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Future<(Widget, SharedPreferences)> _bootstrap({
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
          body: SingleChildScrollView(child: MetabolismCard()),
        ),
      ),
    ),
    prefs,
  );
}

void main() {
  testWidgets('a physiological flag persists through the caffeine store',
      (tester) async {
    // The storage deliberately did not move with the UI: these are still
    // CaffeinePreferences under their original keys, which is what makes the
    // move safe. This test is the proof of that.
    final (widget, prefs) = await _bootstrap();
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

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

    expect(PreferencesRepository(prefs).caffeinePreferences().smoker, isTrue);
  });

  testWidgets('reads values written under the original caffeine keys',
      (tester) async {
    // The migration-safety case: a user who set these before the move must see
    // them afterwards, because nothing was renamed.
    final (widget, _) = await _bootstrap(
      initialValues: const {
        'caffeine_liver_impairment': true,
        'caffeine_hormonal_status': 'pregnant',
      },
    );
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    final liverRow = find.ancestor(
      of: find.text('Liver impairment'),
      matching: find.byType(Row),
    );
    final liverSwitch = tester.widget<Switch>(
      find.descendant(of: liverRow, matching: find.byType(Switch)),
    );
    expect(liverSwitch.value, isTrue);
    expect(find.text('Pregnant'), findsOneWidget);
  });

  testWidgets('surfaces pregnancy, which used to be buried under caffeine',
      (tester) async {
    final (widget, _) = await _bootstrap();
    await tester.pumpWidget(widget);
    await tester.pumpAndSettle();

    expect(find.text('Hormonal status'), findsOneWidget);
    expect(find.text('CYP1A2 genotype'), findsOneWidget);
  });
}
