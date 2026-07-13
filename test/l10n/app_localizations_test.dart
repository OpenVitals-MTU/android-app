// Verifies the generated [AppLocalizations] is wired to the ARB catalogs:
// every declared locale loads, translations actually differ between languages
// (not just the English fallback), and placeholder messages interpolate.

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

  // gen-l10n's template fallback (a key a locale omits resolves to the English
  // message rather than to a blank) used to be pinned here. It needed a locale
  // that genuinely omits a key to demonstrate it, which tied the test to how far
  // along the translators happened to be — it broke three times as catalogs were
  // completed, never once because the fallback was actually wrong. Every SHIPPED
  // locale is now at 100%, so nothing falls back and the test was proving a path
  // no user reaches. Ship a locale below 100% and the fallback matters again;
  // `tool/verify_l10n.dart` is the gate that would let that happen (70% floor).
}
