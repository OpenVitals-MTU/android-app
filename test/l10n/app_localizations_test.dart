// Verifies the generated [AppLocalizations] is wired to the ARB catalogs:
// every declared locale loads, translations actually differ between languages
// (not just the English fallback), and placeholder messages interpolate.

import 'dart:convert';
import 'dart:io';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/l10n/app_localizations.dart';

void main() {
  test('all five app languages are supported', () {
    final codes = AppLocalizations.supportedLocales
        .map((locale) => locale.languageCode)
        .toSet();
    expect(codes, containsAll(<String>['en', 'de', 'es', 'it', 'et']));
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
  // The sentinel below must be a key that is genuinely ABSENT from the locale ARBs.
  // Translators can translate it at any time and silently invalidate this test's
  // premise -- which is exactly what happened once already -- so the test verifies
  // its own premise first and says so, instead of failing as a confusing
  // "fallback broke".
  //
  // If a future Flutter changes `_generateBaseClassFile` so a missing message no
  // longer falls back to the template, this fails loudly instead of the app
  // silently shipping empty strings to every non-English user.
  const sentinelKey = 'settingsAppleHealthImportProgressWithScanPercent';
  String sentinel(AppLocalizations l) =>
      l.settingsAppleHealthImportProgressWithScanPercent(1, 'p', 2, 3, 4, 5, 6);

  test('an untranslated key falls back to the English template message',
      () async {
    final en = await AppLocalizations.delegate.load(const Locale('en'));

    for (final code in <String>['de', 'es', 'it', 'et']) {
      final arb = jsonDecode(
        await File('lib/l10n/app_$code.arb').readAsString(),
      ) as Map<String, dynamic>;

      expect(
        arb.containsKey(sentinelKey),
        isFalse,
        reason: '"$sentinelKey" has now been translated into $code, so it can no '
            'longer prove the fallback. Point this test at a key that is still '
            'untranslated (see the "N untranslated message(s)" lines from '
            '`flutter gen-l10n`).',
      );

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
  });
}
