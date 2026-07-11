// Makes `flutter test` itself a translation gate, not just CI.
//
// The first group runs the REAL checker over the REAL catalogs, so a bad Weblate
// merge fails the test suite locally, before anyone pushes. The rest are
// negative cases: each check gets a hand-built catalog that should trip it,
// because a validator that cannot fail is a validator that is not running.
//
// The checks themselves live in `tool/src/l10n_checks.dart` (no `dart:io`, no
// printing) and are shared verbatim with `tool/verify_l10n.dart`.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import '../../tool/src/l10n_checks.dart';

/// The template's own locale; everything else is measured against it.
const String _templateLocale = 'en';

List<ArbSource> _realCatalogs() {
  final Directory dir = Directory('lib/l10n');
  final List<ArbSource> sources = <ArbSource>[];
  for (final FileSystemEntity entity in dir.listSync()) {
    if (entity is! File || !entity.path.endsWith('.arb')) continue;
    final String name = entity.uri.pathSegments.last;
    final RegExpMatch? match = RegExp(r'^app_(\w+)\.arb$').firstMatch(name);
    if (match == null) continue;
    sources.add(ArbSource(
      fileName: name,
      locale: match.group(1)!,
      contents: entity.readAsStringSync(),
    ));
  }
  return sources;
}

/// Builds a catalog from `locale -> {key: value}` maps, JSON-encoding each.
List<ArbSource> _catalogs(Map<String, String> byLocale) {
  return <ArbSource>[
    for (final MapEntry<String, String> entry in byLocale.entries)
      ArbSource(
        fileName: 'app_${entry.key}.arb',
        locale: entry.key,
        contents: entry.value,
      ),
  ];
}

L10nCheckResult _check(
  Map<String, String> byLocale, {
  double minCoverage = kDefaultMinCoverage,
  String? appLanguageSource,
  String? appLanguageDropdownSource,
}) {
  return checkCatalogs(
    sources: _catalogs(byLocale),
    templateLocale: _templateLocale,
    minCoverage: minCoverage,
    appLanguageSource: appLanguageSource,
    appLanguageDropdownSource: appLanguageDropdownSource,
  );
}

/// Matches an error list containing at least one entry mentioning [needle].
Matcher _hasError(String needle) => contains(contains(needle));

void main() {
  group('the shipped catalogs', () {
    test('pass every check', () {
      final result = checkCatalogs(
        sources: _realCatalogs(),
        templateLocale: _templateLocale,
        appLanguageSource:
            File('lib/domain/preferences/app_language.dart').readAsStringSync(),
        appLanguageDropdownSource:
            File('lib/ui/components/app_language_dropdown.dart')
                .readAsStringSync(),
      );

      expect(
        result.errors,
        isEmpty,
        reason: 'ARB validation failed:\n- ${result.errors.join("\n- ")}',
      );
    });

    test('every locale is above the coverage floor', () {
      final result = checkCatalogs(
        sources: _realCatalogs(),
        templateLocale: _templateLocale,
      );

      expect(result.coverage, isNotEmpty);
      for (final LocaleCoverage c in result.coverage) {
        expect(
          c.ratio,
          greaterThan(kDefaultMinCoverage),
          reason: '${c.locale} is only ${(c.ratio * 100).toStringAsFixed(1)}% '
              'translated (${c.translated}/${c.templateTotal})',
        );
      }
    });

    test('the locale ARBs are genuinely partial, not English back-fills', () {
      // The whole point of the ARB-native cutover: a key a locale has not
      // translated is ABSENT, and gen-l10n falls back to the template. If this
      // ever reads 100% again, something has started back-filling English.
      final result = checkCatalogs(
        sources: _realCatalogs(),
        templateLocale: _templateLocale,
      );
      final template =
          result.coverage.firstWhere((LocaleCoverage c) => c.isTemplate);
      expect(template.ratio, 1.0);
    });
  });

  group('check 1: JSON and @@locale', () {
    test('rejects invalid JSON', () {
      final result = _check(<String, String>{
        'en': '{"hello": "Hello",}',
      });
      expect(result.errors, _hasError('invalid JSON'));
    });

    test('rejects an @@locale that disagrees with the filename', () {
      final result = _check(<String, String>{
        'en': '{"@@locale": "en", "hello": "Hello"}',
        'de': '{"@@locale": "fr", "hello": "Hallo"}',
      });
      expect(result.errors, _hasError('@@locale is "fr"'));
    });
  });

  group('check 2: duplicate keys', () {
    test('catches a duplicate that jsonDecode would silently swallow', () {
      // jsonDecode keeps the LAST value, so this is invisible after parsing.
      final result = _check(<String, String>{
        'en': '{"hello": "Hello", "hello": "Hello again"}',
      });
      expect(result.errors, _hasError('duplicate key "hello"'));
    });

    test('is not fooled by a colon inside a string value', () {
      expect(
        findDuplicateTopLevelKeys('{"a": "x: y", "b": "p: q"}'),
        isEmpty,
      );
    });

    test('is not fooled by a repeated key nested inside @-metadata', () {
      // "type" appears twice, but at depth 2 — not a top-level key.
      expect(
        findDuplicateTopLevelKeys(
          '{"a": "{n}", "@a": {"placeholders": {"n": {"type": "int"}}}, '
          '"b": "{n}", "@b": {"placeholders": {"n": {"type": "int"}}}}',
        ),
        isEmpty,
      );
    });
  });

  group('check 3: coverage', () {
    test('fails a locale below the floor', () {
      final result = _check(
        <String, String>{
          'en': '{"a": "A", "b": "B", "c": "C", "d": "D"}',
          'de': '{"a": "A"}',
        },
      );
      expect(result.errors, _hasError('translation coverage is 25.0%'));
    });

    test('fails at EXACTLY the threshold (the bound is exclusive)', () {
      // 7/10 == 0.70 must FAIL, matching verify-translations.py's `<=`.
      final Map<String, String> byLocale = <String, String>{
        'en': '{${List<String>.generate(10, (int i) => '"k$i": "v$i"').join(",")}}',
        'de': '{${List<String>.generate(7, (int i) => '"k$i": "v$i"').join(",")}}',
      };
      final result = _check(byLocale);
      expect(result.errors, _hasError('translation coverage is 70.0%'));
    });

    test('passes just above the threshold', () {
      final Map<String, String> byLocale = <String, String>{
        'en': '{${List<String>.generate(10, (int i) => '"k$i": "v$i"').join(",")}}',
        'de': '{${List<String>.generate(8, (int i) => '"k$i": "v$i"').join(",")}}',
      };
      expect(_check(byLocale).errors, isEmpty);
    });
  });

  group('check 4: stale keys', () {
    test('flags a locale key the template no longer has', () {
      final result = _check(<String, String>{
        'en': '{"a": "A", "b": "B", "c": "C"}',
        'de': '{"a": "A", "b": "B", "c": "C", "removedLastYear": "Alt"}',
      });
      expect(result.errors, _hasError('stale key "removedLastYear"'));
    });
  });

  group('check 5: placeholder-set equality', () {
    test('flags an ADDED placeholder (it widens the generated signature)', () {
      final result = _check(<String, String>{
        'en': '{"greet": "Hi {name}", "@greet": {"placeholders": {"name": {"type": "String"}}}}',
        'de': '{"greet": "Hallo {name} {surprise}"}',
      });
      expect(result.errors, _hasError('greet placeholder mismatch'));
      expect(result.errors, _hasError('{name, surprise}'));
    });

    test('flags a DROPPED placeholder (it renders a missing value)', () {
      final result = _check(<String, String>{
        'en': '{"greet": "Hi {name}", "@greet": {"placeholders": {"name": {"type": "String"}}}}',
        'de': '{"greet": "Hallo"}',
      });
      expect(result.errors, _hasError('greet placeholder mismatch'));
    });

    test('accepts a reordered but identical placeholder set', () {
      final result = _check(<String, String>{
        'en': '{"m": "{a} then {b}", "@m": {"placeholders": {"a": {"type": "String"}, "b": {"type": "String"}}}}',
        'de': '{"m": "{b} dann {a}"}',
      });
      expect(result.errors, isEmpty);
    });
  });

  group('check 6: template self-consistency', () {
    test('flags a used placeholder that is not declared', () {
      final result = _check(<String, String>{
        'en': '{"m": "{a} and {b}", "@m": {"placeholders": {"a": {"type": "String"}}}}',
      });
      expect(result.errors, _hasError('declares placeholders {a} but uses {a, b}'));
    });

    test('flags a declared placeholder that is not used', () {
      final result = _check(<String, String>{
        'en': '{"m": "{a}", "@m": {"placeholders": {"a": {"type": "String"}, "ghost": {"type": "int"}}}}',
      });
      expect(result.errors, _hasError('declares placeholders {a, ghost}'));
    });

    test('flags a placeholder message with no @key metadata at all', () {
      final result = _check(<String, String>{
        'en': '{"m": "{a}"}',
      });
      expect(result.errors, _hasError('has no "@m.placeholders" metadata'));
    });

    test('flags a dangling @key with no message', () {
      final result = _check(<String, String>{
        'en': '{"m": "plain", "@ghost": {"description": "nothing"}}',
      });
      expect(result.errors, _hasError('"@ghost" has no matching message'));
    });
  });

  group('check 7: plurals', () {
    const String enPlural =
        '{"n": "{count, plural, one{1 step} other{# steps}}", '
        '"@n": {"placeholders": {"count": {"type": "int"}}}}';

    test('accepts a locale that adds categories English does not have', () {
      // Linguistically REQUIRED: a Slavic locale needs few/many. The Python
      // original demanded an exact quantity match, which is simply wrong.
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count, plural, one{1 Schritt} few{# Schritte} '
            'many{# Schritte} other{# Schritte}}"}',
      });
      expect(result.errors, isEmpty);
    });

    test('accepts a locale that drops a category English has', () {
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count, plural, other{# Schritte}}"}',
      });
      expect(result.errors, isEmpty);
    });

    test('requires an "other" branch', () {
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count, plural, one{1 Schritt} few{# Schritte}}"}',
      });
      expect(result.errors, _hasError('has no "other" branch'));
    });

    test('rejects an invalid plural category', () {
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count, plural, plenty{# Schritte} other{# Schritte}}"}',
      });
      expect(result.errors, _hasError('invalid plural category "plenty"'));
    });

    test('accepts the explicit =N form', () {
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count, plural, =0{keine} one{1 Schritt} other{# Schritte}}"}',
      });
      expect(result.errors, isEmpty);
    });

    test('flags a plural that the translation turned into a plain string', () {
      final result = _check(<String, String>{
        'en': enPlural,
        'de': '{"n": "{count} Schritte"}',
      });
      expect(result.errors, _hasError('plural mismatch'));
    });
  });

  group('check 8: ICU syntax', () {
    test('reports a stray opening brace with the file and key', () {
      final result = _check(<String, String>{
        'en': '{"a": "A", "b": "B", "c": "C", "oops": "50{ percent"}',
        'de': '{"a": "A", "b": "B", "c": "C", "oops": "50{ Prozent"}',
      });
      // gen-l10n would only ever say "Found syntax errors." — no file, no key.
      expect(result.errors, _hasError('app_de.arb: oops'));
      expect(result.errors, _hasError('never closed'));
    });

    test('reports a brace followed by something that is not an identifier', () {
      final result = _check(<String, String>{
        'en': '{"m": "risk: {50%} of users"}',
      });
      expect(result.errors, _hasError('app_en.arb: m'));
      expect(result.errors, _hasError('invalid placeholder'));
    });

    test('reports an unclosed placeholder', () {
      final result = _check(<String, String>{
        'en': '{"m": "hello {name", "@m": {"placeholders": {"name": {"type": "String"}}}}',
      });
      expect(result.errors, _hasError('never closed'));
    });

    test('reports an unbalanced closing brace', () {
      final result = _check(<String, String>{
        'en': '{"m": "hello}"}',
      });
      expect(result.errors, _hasError('unbalanced "}"'));
    });

    test('accepts a formatted argument like {n, number}', () {
      final result = _check(<String, String>{
        'en': '{"m": "{n, number} items", "@m": {"placeholders": {"n": {"type": "int"}}}}',
      });
      expect(result.errors, isEmpty);
    });

    test('treats an apostrophe as a literal, matching use-escaping: false', () {
      // `l10n.yaml` does not set `use-escaping`, and gen-l10n defaults it to
      // false: its lexer then uses `normalString = [^{}]+`, so `'` is an
      // ordinary character and braces have NO escape form. This parser must
      // agree, or every "Couldn't" in the catalog would be a false positive.
      // If someone turns `use-escaping` on, this test is the tripwire.
      final result = _check(<String, String>{
        'en': '{"m": "Couldn\'t open the link"}',
      });
      expect(result.errors, isEmpty);
    });
  });

  group('check 9: @-prefixed keys in LOCALE files are inert', () {
    test('Weblate metadata does not count as a stale key or hurt coverage', () {
      // Weblate writes @@locale/@@last_modified and may copy @key blocks into a
      // translation file. gen-l10n discards them. If check 4 flagged them, the
      // first Weblate PR would red the build on day one.
      final result = _check(<String, String>{
        'en': '{"a": "A", "@a": {"description": "letter"}, "b": "B"}',
        'de': '{"@@locale": "de", "@@last_modified": "2026-07-11T00:00:00Z", '
            '"a": "A", "@a": {"description": "Buchstabe"}, "b": "B"}',
      });
      expect(result.errors, isEmpty);

      final LocaleCoverage de =
          result.coverage.firstWhere((LocaleCoverage c) => c.locale == 'de');
      expect(de.ratio, 1.0, reason: '@-keys must not be counted as messages');
      expect(de.templateTotal, 2);
    });
  });

  group('check 10: picker <-> ARB agreement', () {
    const String dropdown = '''
String appLanguageLabel(AppLanguage value) => switch (value) {
      AppLanguage.system => 'System default',
      AppLanguage.english => 'English',
      AppLanguage.german => 'Deutsch',
    };
''';

    test('flags an ARB with no AppLanguage constant (an unselectable language)',
        () {
      const String appLanguage = '''
enum AppLanguage {
  system(null),
  english('en');
  const AppLanguage(this.languageTag);
  final String? languageTag;
}
''';
      final result = _check(
        <String, String>{'en': '{"a": "A"}', 'de': '{"a": "A"}'},
        appLanguageSource: appLanguage,
        appLanguageDropdownSource: dropdown,
      );
      expect(result.errors, _hasError('no AppLanguage constant offers "de"'));
      expect(result.errors, _hasError('Add a constant'));
    });

    test('flags an AppLanguage constant with no ARB (a silent no-op picker)',
        () {
      const String appLanguage = '''
enum AppLanguage {
  system(null),
  english('en'),
  german('de'),
  esperanto('eo');
  const AppLanguage(this.languageTag);
  final String? languageTag;
}
''';
      final result = _check(
        <String, String>{'en': '{"a": "A"}', 'de': '{"a": "A"}'},
        appLanguageSource: appLanguage,
        appLanguageDropdownSource: dropdown,
      );
      expect(result.errors, _hasError('app_eo.arb does not exist'));
      expect(result.errors, _hasError('silently does nothing'));
    });

    test('flags an AppLanguage constant with no autonym in the dropdown', () {
      const String appLanguage = '''
enum AppLanguage {
  system(null),
  english('en'),
  german('de');
  const AppLanguage(this.languageTag);
  final String? languageTag;
}
''';
      const String lazyDropdown = '''
String appLanguageLabel(AppLanguage value) => switch (value) {
      AppLanguage.system => 'System default',
      AppLanguage.english => 'English',
    };
''';
      final result = _check(
        <String, String>{'en': '{"a": "A"}', 'de': '{"a": "A"}'},
        appLanguageSource: appLanguage,
        appLanguageDropdownSource: lazyDropdown,
      );
      expect(result.errors, _hasError('AppLanguage.german has no autonym'));
      expect(result.errors, _hasError('its OWN name'));
    });

    test('the real picker and the real ARBs agree', () {
      final Map<String, String> tags = parseAppLanguageTags(
        File('lib/domain/preferences/app_language.dart').readAsStringSync(),
      );
      final Set<String> labelled = parseAppLanguageLabels(
        File('lib/ui/components/app_language_dropdown.dart').readAsStringSync(),
      );

      expect(tags.values.toSet(), <String>{'en', 'es', 'de', 'it', 'et'});
      // `system` has a null tag but still needs an autonym.
      expect(labelled, containsAll(<String>[...tags.keys, 'system']));
    });
  });
}
