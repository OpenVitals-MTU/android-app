// Pure validation logic for the ARB catalogs. A Dart port of the Kotlin repo's
// `scripts/verify-translations.py`, adapted to ARB/ICU instead of `strings.xml`.
//
// This library is deliberately free of `dart:io` and of any printing so it can
// be driven both by `tool/verify_l10n.dart` (the CI gate) and by
// `test/l10n/verify_l10n_test.dart` (so `flutter test` is itself a translation
// gate). Callers supply raw file *contents*; this file never touches the disk.
//
// The ARBs are the source of truth and Weblate edits them directly, so these
// checks exist to catch what `flutter gen-l10n` will NOT tell you:
//   * gen-l10n silently ignores a locale key that no longer exists in the
//     template, so a stale string would live in Weblate forever;
//   * gen-l10n INFERS placeholders from every locale, so a translator who adds
//     `{arg7}` silently widens the generated method signature and breaks every
//     call site with an unrelatable error, while a translator who drops a
//     placeholder silently renders a missing value;
//   * a stray `{` becomes gen-l10n's context-free "Found syntax errors."
//
// A locale ARB is expected to be PARTIAL: a key it omits falls back to the
// template message at generation time. That is real coverage, and check 3 is
// what stops it from silently rotting.
//
// SHIPPED vs IN PROGRESS. Weblate hosts a language from 0% and bundles every
// changed locale into ONE pull request, so gating *hosting* on coverage would
// let one 5%-translated newcomer block every other translation fix in that PR.
// Hosting and shipping are therefore separate:
//   * SHIPPED — the locale has an `AppLanguage` constant (and hence an autonym
//     in the picker's exhaustive switch). It is offered to users, so coverage
//     MUST be greater than the threshold (check 3).
//   * IN PROGRESS — an ARB exists but no constant does. Translators can work on
//     it; users never see it (`OpenVitalsApp.supportedLocales` is derived from
//     `AppLanguage`, not from the ARBs present). Its coverage is REPORTED, not
//     gated — but every STRUCTURAL check below still applies to it, because a
//     broken in-progress ARB still breaks `flutter gen-l10n` for everyone.

import 'dart:convert';

/// Minimum share of template keys a locale must translate. Deliberately
/// exclusive, matching `verify-translations.py`: exactly 70.0% FAILS.
const double kDefaultMinCoverage = 0.70;

/// ICU plural categories, plus the explicit `=N` form handled separately.
const Set<String> kPluralCategories = <String>{
  'zero',
  'one',
  'two',
  'few',
  'many',
  'other',
};

/// Per-locale translation coverage: `|L ∩ T| / |T|`.
class LocaleCoverage {
  const LocaleCoverage({
    required this.locale,
    required this.translated,
    required this.templateTotal,
    required this.isTemplate,
    this.isInProgress = false,
  });

  final String locale;
  final int translated;
  final int templateTotal;
  final bool isTemplate;

  /// True when an ARB exists for this locale but no `AppLanguage` constant
  /// offers it: hosted for translators, not shipped to users, so its coverage
  /// is reported rather than gated. Always false for the template.
  final bool isInProgress;

  double get ratio => templateTotal == 0 ? 1 : translated / templateTotal;
}

/// The outcome of a full catalog check.
class L10nCheckResult {
  const L10nCheckResult({required this.errors, required this.coverage});

  final List<String> errors;
  final List<LocaleCoverage> coverage;

  bool get isValid => errors.isEmpty;
}

/// One ARB file, as raw text plus its locale code (taken from the filename).
class ArbSource {
  const ArbSource({
    required this.fileName,
    required this.locale,
    required this.contents,
  });

  final String fileName;
  final String locale;
  final String contents;
}

/// Runs every check over [sources]. [templateLocale] must be present.
///
/// [appLanguageSource] is `lib/domain/preferences/app_language.dart` and
/// [appLanguageDropdownSource] is `lib/ui/components/app_language_dropdown.dart`;
/// pass null to skip the picker/ARB agreement check (check 10).
L10nCheckResult checkCatalogs({
  required List<ArbSource> sources,
  required String templateLocale,
  double minCoverage = kDefaultMinCoverage,
  String? appLanguageSource,
  String? appLanguageDropdownSource,
}) {
  final List<String> errors = <String>[];
  final List<LocaleCoverage> coverage = <LocaleCoverage>[];

  final Map<String, _Catalog> parsed = <String, _Catalog>{};
  for (final ArbSource source in sources) {
    final _Catalog? catalog = _parseCatalog(source, errors, templateLocale);
    if (catalog != null) parsed[source.locale] = catalog;
  }

  // A broken/unparseable file makes every downstream check meaningless.
  if (errors.isNotEmpty) {
    return L10nCheckResult(errors: errors, coverage: coverage);
  }

  final _Catalog? template = parsed[templateLocale];
  if (template == null) {
    errors.add('template locale "$templateLocale" not found in the ARB dir');
    return L10nCheckResult(errors: errors, coverage: coverage);
  }

  _checkTemplateSelfConsistency(template, errors);

  final Set<String> templateKeys = template.messages.keys.toSet();

  // Which locales are SHIPPED. Derived from the `AppLanguage` constants, since
  // those are what the picker — and `OpenVitalsApp.supportedLocales` — offer.
  // Null when the picker source was not supplied (or could not be parsed, which
  // check 10 reports separately): with no way to tell shipped from in-progress,
  // fall back to the strict pre-in-progress behaviour and gate EVERY locale.
  final Map<String, String> pickerTags = appLanguageSource == null
      ? const <String, String>{}
      : parseAppLanguageTags(appLanguageSource);
  final Set<String>? shippedLocales =
      pickerTags.isEmpty ? null : pickerTags.values.toSet();

  for (final String locale in parsed.keys.toList()..sort()) {
    final _Catalog catalog = parsed[locale]!;
    final bool isTemplate = locale == templateLocale;
    final Set<String> localeKeys = catalog.messages.keys.toSet();
    final Set<String> shared = localeKeys.intersection(templateKeys);

    // An ARB with no `AppLanguage` constant is hosted, not shipped.
    final bool isInProgress = !isTemplate &&
        shippedLocales != null &&
        !shippedLocales.contains(locale);

    coverage.add(LocaleCoverage(
      locale: locale,
      translated: shared.length,
      templateTotal: templateKeys.length,
      isTemplate: isTemplate,
      isInProgress: isInProgress,
    ));

    if (isTemplate) continue;

    // 3. Coverage. `<=` is deliberate: exactly the threshold FAILS.
    //
    // SHIPPED locales only. An in-progress locale is by definition allowed to
    // sit at 5%: gating it would let one unfinished language block the single
    // pull request Weblate opens for ALL locales. Everything below this line
    // still applies to it — coverage is the only thing being lifted.
    final double ratio =
        templateKeys.isEmpty ? 1 : shared.length / templateKeys.length;
    if (!isInProgress && ratio <= minCoverage) {
      errors.add(
        '${catalog.fileName}: translation coverage is '
        '${_percent(ratio)} (${shared.length}/${templateKeys.length}); '
        'must be greater than ${_percent(minCoverage)}',
      );
    }

    // 4. Stale/extra keys. gen-l10n silently ignores these, so without this
    // check Weblate accretes dead strings forever.
    for (final String key in (localeKeys.difference(templateKeys)).toList()
      ..sort()) {
      errors.add(
        '${catalog.fileName}: stale key "$key" is not in the template; '
        'remove it (it was deleted from app_$templateLocale.arb)',
      );
    }

    for (final String key in shared.toList()..sort()) {
      final _Message localeMessage = catalog.messages[key]!;
      final _Message templateMessage = template.messages[key]!;

      // 5. Placeholder-set equality. The highest-value check: gen-l10n infers
      // placeholders from EVERY locale, so an added arg widens the generated
      // signature and a dropped one renders an empty value.
      if (!_setEquals(localeMessage.args, templateMessage.args)) {
        errors.add(
          '${catalog.fileName}: $key placeholder mismatch: template uses '
          '${_fmtSet(templateMessage.args)}, translation uses '
          '${_fmtSet(localeMessage.args)}',
        );
      }

      // 7. Plural kind match. Looser than verify-translations.py on purpose:
      // the translation need NOT carry the same plural categories as English
      // (a Slavic locale legitimately needs `few`/`many` that English lacks);
      // it only has to BE a plural where the template is, and define `other`.
      final Set<String> templatePluralArgs = templateMessage.plurals.keys.toSet();
      final Set<String> localePluralArgs = localeMessage.plurals.keys.toSet();
      if (!_setEquals(templatePluralArgs, localePluralArgs)) {
        errors.add(
          '${catalog.fileName}: $key plural mismatch: template pluralises '
          '${_fmtSet(templatePluralArgs)}, translation pluralises '
          '${_fmtSet(localePluralArgs)}',
        );
      }
    }
  }

  // 10. Picker -> ARB agreement.
  if (appLanguageSource != null && appLanguageDropdownSource != null) {
    _checkLanguagePicker(
      appLanguageSource: appLanguageSource,
      appLanguageDropdownSource: appLanguageDropdownSource,
      arbLocales: parsed.keys.toSet(),
      coverage: coverage,
      minCoverage: minCoverage,
      errors: errors,
    );
  }

  return L10nCheckResult(errors: errors, coverage: coverage);
}

/// Parses one ARB file: JSON validity (1), `@@locale` agreement (1),
/// duplicate keys (2), and the ICU body of every message (8).
_Catalog? _parseCatalog(
  ArbSource source,
  List<String> errors,
  String templateLocale,
) {
  // 2. Duplicates first: jsonDecode silently keeps the LAST of a repeated key,
  // so by the time we have a Map the evidence is gone.
  for (final String key in findDuplicateTopLevelKeys(source.contents)) {
    errors.add('${source.fileName}: duplicate key "$key"');
  }

  final Object? decoded;
  try {
    decoded = jsonDecode(source.contents);
  } on FormatException catch (e) {
    errors.add('${source.fileName}: invalid JSON: ${e.message}');
    return null;
  }
  if (decoded is! Map<String, Object?>) {
    errors.add('${source.fileName}: top level must be a JSON object');
    return null;
  }

  // 1. `@@locale`, when present, must agree with the filename.
  final Object? declared = decoded['@@locale'];
  if (declared != null && declared != source.locale) {
    errors.add(
      '${source.fileName}: @@locale is "$declared" but the filename says '
      '"${source.locale}"',
    );
  }

  // 1b. The TEMPLATE must not declare `@@locale` at all.
  //
  // This is not stylistic. `app_en.arb` is Weblate's `new_base`: when a
  // translator starts a new language, Weblate COPIES that file and then fills in
  // the units. It treats `@@locale` as an inert `@@`-key and never rewrites it,
  // so every new locale would inherit `"@@locale": "en"` and gen-l10n would fail
  // with "the locale specified in @@locale and the arb filename do not match".
  // It bit Galician exactly this way. gen-l10n resolves the template's locale
  // from the filename, so the key buys nothing and costs every future language.
  //
  // (Weblate DOES preserve a correct `@@locale` on an existing file, which is why
  // the locale ARBs may keep theirs.)
  if (source.locale == templateLocale && declared != null) {
    errors.add(
      '${source.fileName}: the template must NOT declare @@locale. Weblate copies '
      'this file to seed every new language and does not rewrite the key, so each '
      'one would inherit "$declared" and break gen-l10n. The locale comes from the '
      'filename.',
    );
  }

  final Map<String, _Message> messages = <String, _Message>{};
  final Map<String, Object?> metadata = <String, Object?>{};

  for (final MapEntry<String, Object?> entry in decoded.entries) {
    final String key = entry.key;

    // 9. `@`-prefixed keys are metadata, not messages. Weblate writes
    // `@@locale`/`@@last_modified` into translation files and may copy `@key`
    // blocks across; gen-l10n discards all of them from a non-template bundle.
    // They must never count towards coverage nor trip the stale-key check, or
    // the first Weblate PR reds the build.
    if (key.startsWith('@@')) continue;
    if (key.startsWith('@')) {
      metadata[key.substring(1)] = entry.value;
      continue;
    }

    final Object? value = entry.value;
    if (value is! String) {
      errors.add('${source.fileName}: $key must be a string');
      continue;
    }

    // 8. ICU syntax: a translator's stray `{` gets a file-and-key error here
    // instead of gen-l10n's context-free "Found syntax errors."
    final _IcuParse parse = parseIcu(value);
    for (final String problem in parse.errors) {
      errors.add('${source.fileName}: $key: $problem');
    }
    for (final MapEntry<String, Set<String>> plural in parse.plurals.entries) {
      // Every plural must define `other`: ICU falls back to it for any
      // category the locale's rules produce but the message omits.
      if (!plural.value.contains('other')) {
        errors.add(
          '${source.fileName}: $key: plural "{${plural.key}}" has no "other" '
          'branch (required: ICU falls back to it)',
        );
      }
      for (final String category in plural.value) {
        if (kPluralCategories.contains(category)) continue;
        if (RegExp(r'^=\d+$').hasMatch(category)) continue;
        errors.add(
          '${source.fileName}: $key: invalid plural category "$category" '
          '(expected one of ${kPluralCategories.join(", ")} or "=N")',
        );
      }
    }

    messages[key] = _Message(
      value: value,
      args: parse.args,
      plurals: parse.plurals,
    );
  }

  return _Catalog(
    fileName: source.fileName,
    locale: source.locale,
    messages: messages,
    metadata: metadata,
  );
}

/// 6. Template self-consistency: the placeholders a message USES must be exactly
/// those its `@key.placeholders` DECLARES, and no `@key` may dangle.
void _checkTemplateSelfConsistency(_Catalog template, List<String> errors) {
  for (final MapEntry<String, Object?> entry in template.metadata.entries) {
    final String key = entry.key;
    if (!template.messages.containsKey(key)) {
      errors.add(
        '${template.fileName}: "@$key" has no matching message "$key"',
      );
      continue;
    }

    final Object? meta = entry.value;
    final Set<String> declared = <String>{};
    if (meta is Map<String, Object?>) {
      final Object? placeholders = meta['placeholders'];
      if (placeholders is Map<String, Object?>) {
        declared.addAll(placeholders.keys);
      }
    }

    final Set<String> used = template.messages[key]!.args;
    if (!_setEquals(declared, used)) {
      errors.add(
        '${template.fileName}: $key declares placeholders ${_fmtSet(declared)} '
        'but uses ${_fmtSet(used)}',
      );
    }
  }

  // The inverse: a message that uses placeholders but declares none leaves
  // gen-l10n to guess the Dart type (it defaults to Object).
  for (final MapEntry<String, _Message> entry in template.messages.entries) {
    if (entry.value.args.isEmpty) continue;
    if (template.metadata.containsKey(entry.key)) continue;
    errors.add(
      '${template.fileName}: ${entry.key} uses placeholders '
      '${_fmtSet(entry.value.args)} but has no "@${entry.key}.placeholders" '
      'metadata to type them',
    );
  }
}

/// 10. Picker -> ARB agreement.
///
/// One direction only, and deliberately so:
///   * an `AppLanguage` constant with no ARB (or an under-translated one) is an
///     ERROR — it is a picker entry that silently does nothing, or that ships a
///     half-English UI to whoever chooses it;
///   * an ARB with no constant is NOT an error — that is an IN-PROGRESS locale,
///     hosted in Weblate and invisible to users. It used to be an error, which
///     meant a new language could not exist in the repo below 70% and so could
///     not be translated in Weblate at all without redding the build.
/// Crossing the threshold is surfaced in the summary (`verify_l10n.dart`), not
/// as a build failure: an in-progress locale that reaches 71% must not suddenly
/// break the very pull request that got it there.
///
/// This is a VERIFIER, not a generator, on purpose. Kotlin could afford to
/// codegen the language list because the JVM hands it `Locale.getDisplayName`;
/// Dart ships no ICU display-name data, so a generated list would render a
/// picker entry labelled `eo`. Verifying keeps the exhaustive switch, which
/// forces a human to supply the autonym.
void _checkLanguagePicker({
  required String appLanguageSource,
  required String appLanguageDropdownSource,
  required Set<String> arbLocales,
  required List<LocaleCoverage> coverage,
  required double minCoverage,
  required List<String> errors,
}) {
  final Map<String, String> pickerTags = parseAppLanguageTags(appLanguageSource);
  final Set<String> labelled = parseAppLanguageLabels(appLanguageDropdownSource);

  if (pickerTags.isEmpty) {
    errors.add(
      'lib/domain/preferences/app_language.dart: could not parse any '
      'AppLanguage constant; the picker check cannot run',
    );
    return;
  }

  // Every enum constant needs an autonym. The switch is exhaustive, so the
  // compiler normally catches this — unless someone adds a `default` case.
  for (final String constant in pickerTags.keys) {
    if (labelled.contains(constant)) continue;
    errors.add(
      'lib/ui/components/app_language_dropdown.dart: AppLanguage.$constant has '
      'no autonym in appLanguageLabel(); add a case returning the language in '
      'its OWN name (e.g. "Deutsch", not "German")',
    );
  }

  final Map<String, LocaleCoverage> byLocale = <String, LocaleCoverage>{
    for (final LocaleCoverage c in coverage) c.locale: c,
  };

  // Picker -> ARB. A tag with no (or an under-translated) ARB is a silent no-op
  // today: the user picks the language and nothing changes.
  for (final MapEntry<String, String> entry in pickerTags.entries) {
    final String tag = entry.value;
    if (!arbLocales.contains(tag)) {
      errors.add(
        'lib/domain/preferences/app_language.dart: AppLanguage.${entry.key} '
        'offers language tag "$tag" but lib/l10n/app_$tag.arb does not exist; '
        'selecting it in the picker silently does nothing. Add the ARB or '
        'remove the constant.',
      );
      continue;
    }
    final LocaleCoverage? c = byLocale[tag];
    if (c != null && !c.isTemplate && c.ratio <= minCoverage) {
      errors.add(
        'lib/domain/preferences/app_language.dart: AppLanguage.${entry.key} '
        'offers "$tag" but app_$tag.arb is only ${_percent(c.ratio)} '
        'translated (must be greater than ${_percent(minCoverage)}); either '
        'finish the translation or remove the constant.',
      );
    }
  }
}

/// Parses `enum AppLanguage { english('en'), ... }` into constant -> tag.
/// Constants with a `null` tag (i.e. `system`) map to no tag and are skipped.
Map<String, String> parseAppLanguageTags(String source) {
  final Map<String, String> out = <String, String>{};
  final RegExp pattern = RegExp(r"(\w+)\(\s*'([a-zA-Z-]+)'\s*\)");
  final int enumStart = source.indexOf('enum AppLanguage');
  if (enumStart < 0) return out;
  for (final RegExpMatch match in pattern.allMatches(source, enumStart)) {
    out[match.group(1)!] = match.group(2)!;
  }
  return out;
}

/// Parses the exhaustive `switch` in `appLanguageLabel` into the set of
/// `AppLanguage` constants that have an autonym.
Set<String> parseAppLanguageLabels(String source) {
  final Set<String> out = <String>{};
  final RegExp pattern = RegExp(r'AppLanguage\.(\w+)\s*=>');
  for (final RegExpMatch match in pattern.allMatches(source)) {
    out.add(match.group(1)!);
  }
  return out;
}

/// 2. Duplicate top-level keys. `jsonDecode` keeps the last silently, so this
/// has to be a string-aware scan of the raw text: track quoting and nesting, and
/// only treat a depth-1 string followed by `:` as a key.
List<String> findDuplicateTopLevelKeys(String raw) {
  final Set<String> seen = <String>{};
  final List<String> duplicates = <String>[];
  int depth = 0;
  int i = 0;

  while (i < raw.length) {
    final String c = raw[i];

    if (c == '"') {
      final StringBuffer buf = StringBuffer();
      i++; // opening quote
      while (i < raw.length && raw[i] != '"') {
        if (raw[i] == r'\' && i + 1 < raw.length) {
          buf.write(raw[i]);
          buf.write(raw[i + 1]);
          i += 2;
          continue;
        }
        buf.write(raw[i]);
        i++;
      }
      i++; // closing quote

      if (depth == 1) {
        int j = i;
        while (j < raw.length && _isWhitespace(raw[j])) {
          j++;
        }
        if (j < raw.length && raw[j] == ':') {
          final String key = buf.toString();
          if (!seen.add(key) && !duplicates.contains(key)) {
            duplicates.add(key);
          }
        }
      }
      continue;
    }

    if (c == '{' || c == '[') depth++;
    if (c == '}' || c == ']') depth--;
    i++;
  }

  return duplicates;
}

bool _isWhitespace(String c) =>
    c == ' ' || c == '\t' || c == '\n' || c == '\r';

/// The result of parsing one ICU message body.
class _IcuParse {
  _IcuParse(this.args, this.plurals, this.errors);

  /// Every argument name referenced by the message, at any nesting depth.
  final Set<String> args;

  /// Argument name -> the plural categories its branches define.
  final Map<String, Set<String>> plurals;

  final List<String> errors;
}

/// 8. A small recursive-descent ICU parser. It is intentionally narrow: it only
/// needs to recover the ARGUMENT NAMES and PLURAL CATEGORIES and to notice
/// unbalanced braces / invalid identifiers. Everything else is opaque text.
_IcuParse parseIcu(String message) {
  final _IcuParser parser = _IcuParser(message);
  parser.parseMessage(0);
  return _IcuParse(parser.args, parser.plurals, parser.errors);
}

class _IcuParser {
  _IcuParser(this.src);

  final String src;
  int i = 0;
  final Set<String> args = <String>{};
  final Map<String, Set<String>> plurals = <String, Set<String>>{};
  final List<String> errors = <String>[];

  static final RegExp _identifier = RegExp(r'[a-zA-Z_][a-zA-Z0-9_]*');

  void parseMessage(int depth) {
    while (i < src.length) {
      final String c = src[i];
      if (c == '{') {
        parseArgument();
        continue;
      }
      if (c == '}') {
        if (depth > 0) return;
        errors.add('unbalanced "}" at offset $i');
        i++;
        continue;
      }
      i++;
    }
    if (depth > 0) {
      errors.add('unbalanced "{": the message ends inside a placeholder');
    }
  }

  void parseArgument() {
    final int start = i;
    i++; // '{'
    _skipWhitespace();

    final Match? name = _identifier.matchAsPrefix(src, i);
    if (name == null) {
      errors.add(
        'invalid placeholder at offset $start: expected an identifier after '
        '"{" (a literal brace is not allowed in an ARB message)',
      );
      // Skip to the closing brace so one bad placeholder doesn't cascade.
      _recoverToClose();
      return;
    }
    final String argName = name.group(0)!;
    i = name.end;
    args.add(argName);
    _skipWhitespace();

    if (i >= src.length) {
      errors.add('unbalanced "{": placeholder "$argName" is never closed');
      return;
    }
    if (src[i] == '}') {
      i++;
      return;
    }
    if (src[i] != ',') {
      errors.add(
        'invalid placeholder "$argName" at offset $start: expected "}" or "," '
        'after the name',
      );
      _recoverToClose();
      return;
    }

    i++; // ','
    _skipWhitespace();
    final Match? type = _identifier.matchAsPrefix(src, i);
    if (type == null) {
      errors.add('placeholder "$argName": expected a type after ","');
      _recoverToClose();
      return;
    }
    final String argType = type.group(0)!;
    i = type.end;
    _skipWhitespace();

    if (argType == 'plural' || argType == 'select' || argType == 'selectordinal') {
      if (i >= src.length || src[i] != ',') {
        errors.add('placeholder "$argName": "$argType" needs branches');
        _recoverToClose();
        return;
      }
      i++; // ','
      final Set<String> categories = <String>{};
      while (true) {
        _skipWhitespace();
        if (i >= src.length) {
          errors.add('unbalanced "{": "$argName" ($argType) is never closed');
          return;
        }
        if (src[i] == '}') {
          i++;
          break;
        }
        final String? selector = _readSelector();
        if (selector == null) {
          errors.add(
            'placeholder "$argName": invalid $argType branch at offset $i',
          );
          _recoverToClose();
          return;
        }
        _skipWhitespace();
        if (i >= src.length || src[i] != '{') {
          errors.add(
            'placeholder "$argName": $argType branch "$selector" has no body',
          );
          _recoverToClose();
          return;
        }
        i++; // '{'
        parseMessage(1);
        if (i >= src.length || src[i] != '}') {
          errors.add(
            'unbalanced "{": $argType branch "$selector" of "$argName" is '
            'never closed',
          );
          return;
        }
        i++; // '}'
        categories.add(selector);
      }
      if (argType != 'select') {
        plurals[argName] = categories;
      }
      return;
    }

    // A formatted argument: `{arg0, number}`, `{when, date, short}`, ...
    // The name is what matters; skip the rest, honouring nesting.
    _recoverToClose();
  }

  /// A plural selector: an ICU category word, or the explicit `=N` form.
  String? _readSelector() {
    if (i < src.length && src[i] == '=') {
      final Match? digits = RegExp(r'=\d+').matchAsPrefix(src, i);
      if (digits == null) return null;
      i = digits.end;
      return digits.group(0)!;
    }
    final Match? word = _identifier.matchAsPrefix(src, i);
    if (word == null) return null;
    i = word.end;
    return word.group(0)!;
  }

  void _skipWhitespace() {
    while (i < src.length && _isWhitespace(src[i])) {
      i++;
    }
  }

  /// Consumes up to and including the brace that closes the argument we are in.
  void _recoverToClose() {
    int depth = 1;
    while (i < src.length && depth > 0) {
      if (src[i] == '{') depth++;
      if (src[i] == '}') depth--;
      i++;
    }
    if (depth > 0) {
      errors.add('unbalanced "{": the message ends inside a placeholder');
    }
  }
}

class _Catalog {
  const _Catalog({
    required this.fileName,
    required this.locale,
    required this.messages,
    required this.metadata,
  });

  final String fileName;
  final String locale;
  final Map<String, _Message> messages;

  /// `@key` blocks, keyed WITHOUT the leading `@`.
  final Map<String, Object?> metadata;
}

class _Message {
  const _Message({
    required this.value,
    required this.args,
    required this.plurals,
  });

  final String value;
  final Set<String> args;
  final Map<String, Set<String>> plurals;
}

bool _setEquals(Set<String> a, Set<String> b) =>
    a.length == b.length && a.containsAll(b);

String _fmtSet(Set<String> values) {
  if (values.isEmpty) return '{}';
  final List<String> sorted = values.toList()..sort();
  return '{${sorted.join(", ")}}';
}

String _percent(double ratio) => '${(ratio * 100).toStringAsFixed(1)}%';
