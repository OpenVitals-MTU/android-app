// Verifies the generated [AppLocalizations] is wired to the ARB catalogs:
// every declared locale loads, translations actually differ between languages
// (not just the English fallback), and placeholder messages interpolate.

import 'dart:convert';
import 'dart:io';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/app.dart';
import 'package:openvitals/domain/preferences/app_language.dart';
import 'package:openvitals/l10n/app_localizations.dart';

void main() {
  test('all five app languages are supported', () {
    final codes = AppLocalizations.supportedLocales
        .map((locale) => locale.languageCode)
        .toSet();
    expect(codes, containsAll(<String>['en', 'de', 'es', 'it', 'et']));
  });

  // `AppLocalizations.supportedLocales` is whatever ARB files gen-l10n FOUND;
  // `OpenVitalsApp.supportedLocales` is what the app OFFERS. They are not the
  // same thing, because `lib/l10n` also hosts in-progress catalogs (see
  // docs/engineering/translations.md).
  group('the app offers only SHIPPED locales', () {
    test('every shipped language, and nothing that has no AppLanguage constant',
        () {
      final tags =
          OpenVitalsApp.supportedLocales.map((locale) => locale.toLanguageTag());

      expect(tags, unorderedEquals(<String>['en', 'es', 'de', 'it', 'et']));
      // `system` follows the platform and carries no tag of its own.
      expect(AppLanguage.shippedLanguageTags, isNot(contains('system')));
      expect(
        AppLanguage.shippedLanguageTags.length,
        AppLanguage.values.length - 1,
      );
    });

    test('an in-progress locale is NOT offered, even once gen-l10n knows it',
        () {
      // Weblate will land `lib/l10n/app_gl.arb` at ~5%, which puts `gl` into
      // `AppLocalizations.supportedLocales`. Had `MaterialApp` been given THAT
      // list, a Galician device would resolve to a 5%-Galician / 95%-English UI
      // with no way out, since the picker only lists `AppLanguage`. So: the
      // offered list is derived from `AppLanguage`, and an ARB alone earns a
      // locale nothing.
      final generated = <Locale>[
        ...AppLocalizations.supportedLocales,
        const Locale('gl'), // as if the in-progress catalog were merged
      ];
      expect(
        generated.map((locale) => locale.languageCode),
        contains('gl'),
        reason: 'premise: gen-l10n would happily list it',
      );

      expect(
        OpenVitalsApp.supportedLocales.map((locale) => locale.languageCode),
        isNot(contains('gl')),
      );
      // Stronger: NOTHING reaches the app that lacks an AppLanguage constant.
      expect(
        OpenVitalsApp.supportedLocales.map((locale) => locale.toLanguageTag()),
        everyElement(isIn(AppLanguage.shippedLanguageTags)),
      );
    });
  });

  test('English and German load with distinct translations', () async {
    final en = await AppLocalizations.delegate.load(const Locale('en'));
    final de = await AppLocalizations.delegate.load(const Locale('de'));

    expect(en.settingsDisplayGroupTitle, 'Display');
    expect(de.settingsDisplayGroupTitle, 'Anzeige');
    // The whole point of wiring l10n: the German value is a real translation,
    // not the English fallback.
    expect(de.settingsDisplayGroupTitle, isNot(en.settingsDisplayGroupTitle));
  });

  test('Spanish, Italian and Estonian also carry real translations', () async {
    final es = await AppLocalizations.delegate.load(const Locale('es'));
    final it = await AppLocalizations.delegate.load(const Locale('it'));
    final et = await AppLocalizations.delegate.load(const Locale('et'));

    expect(es.screenSettings, 'Ajustes');
    expect(it.screenSettings, 'Impostazioni');
    expect(et.screenSettings, 'Seaded');
  });

  test('placeholder messages interpolate their arguments', () async {
    final en = await AppLocalizations.delegate.load(const Locale('en'));

    // Single String arg (Android `%1$s`).
    expect(en.cdExpandDrinkCategory('Coffee'), 'Expand Coffee');
    // Two int args (Android `%1$d ... %2$d`).
    expect(en.dashboardSensorStatusActiveConnected(2, 3), '2 active • 3 connected');
  });

  // PINS gen-l10n's template-fallback semantics, which the whole ARB-native
  // cutover rests on: the locale ARBs are genuinely PARTIAL, and a key a locale
  // omits must fall back to the English template message — NOT to a blank
  // string, and NOT to a build error.
  //
  // The sentinel must be a key a locale genuinely OMITS. Translators can translate
  // it at any moment and silently invalidate the premise -- and they have, twice.
  //
  // So the test does NOT demand the key be missing everywhere; a locale that has
  // translated it simply proves nothing and is skipped. It only demands that at
  // least ONE locale still omits it, because otherwise there is no untranslated
  // string left to demonstrate the fallback with, and the test would be silently
  // vacuous rather than failing.
  //
  // (Spanish reached 100% and this test broke by requiring the key to be absent
  // from all four locales. That was too strict: completing a translation is not a
  // regression. Then de/es/et/it ALL reached 100% and it broke again, because the
  // locale list was hard-coded to exactly those four and gl -- the one locale
  // still partial -- was never consulted. So the list is no longer hard-coded:
  // every app_*.arb on disk is a candidate, and a new partial locale keeps the
  // fallback provable without anyone having to remember to add it here.)
  //
  // If a future Flutter changes `_generateBaseClassFile` so a missing message no
  // longer falls back to the template, this fails loudly instead of the app
  // silently shipping empty strings to every non-English user.
  const sentinelKey = 'settingsAppleHealthImportProgressWithScanPercent';
  String sentinel(AppLocalizations l) =>
      l.settingsAppleHealthImportProgressWithScanPercent(1, 'p', 2, 3, 4, 5, 6);

  test('a key a locale omits falls back to the English template message',
      () async {
    final en = await AppLocalizations.delegate.load(const Locale('en'));
    final proving = <String>[];

    final codes = Directory('lib/l10n')
        .listSync()
        .whereType<File>()
        .map((f) => f.uri.pathSegments.last)
        .where((n) => n.startsWith('app_') && n.endsWith('.arb'))
        .map((n) => n.substring('app_'.length, n.length - '.arb'.length))
        .where((c) => c != 'en')
        .toList()
      ..sort();

    for (final code in codes) {
      final arb = jsonDecode(
        await File('lib/l10n/app_$code.arb').readAsString(),
      ) as Map<String, dynamic>;

      // Translated here: nothing to prove with this locale. Not a failure --
      // finishing a translation is the goal, not a regression.
      if (arb.containsKey(sentinelKey)) continue;
      proving.add(code);

      final l10n = await AppLocalizations.delegate.load(Locale(code));
      expect(
        sentinel(l10n),
        isNotEmpty,
        reason: '$code fell back to a BLANK string, not the template',
      );
      expect(
        sentinel(l10n),
        sentinel(en),
        reason: '$code did not fall back to the English template message; '
            'gen-l10n fallback semantics have changed',
      );
    }

    expect(
      proving,
      isNotEmpty,
      reason: 'Every locale in $codes now translates "$sentinelKey", so it can '
          'no longer demonstrate the template fallback. Point this test at a key '
          'that is still untranslated somewhere -- see the "N untranslated '
          'message(s)" lines that `flutter gen-l10n` prints.',
    );
  });
}
