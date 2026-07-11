// Translation gate for the ARB catalogs.
//
//   dart run tool/verify_l10n.dart [--arb-dir DIR] [--template FILE]
//                                  [--min-coverage 0.70]
//
// Exit codes:
//   0  every catalog passes (a per-locale coverage summary goes to stdout)
//   1  validation failed (the findings go to stderr)
//   2  usage or I/O error
//
// The ARBs are the source of truth — Weblate edits them directly and there is no
// generator to regenerate them from. `flutter gen-l10n` will happily accept a
// stale key, a widened placeholder set, or a locale that has rotted to 12%
// coverage, so this is the only thing standing between Weblate and `main`.
//
// The coverage floor applies to SHIPPED locales (those with an `AppLanguage`
// constant). A locale with an ARB but no constant is IN PROGRESS: hosted in
// Weblate, structurally validated here, coverage merely reported. See
// `tool/src/l10n_checks.dart` and `docs/engineering/translations.md`.
// The checks themselves live in `tool/src/l10n_checks.dart` so that
// `test/l10n/verify_l10n_test.dart` runs them too: `flutter test` is a
// translation gate, not just CI.
//
// Kept `print`-free (stdout/stderr only) so `flutter analyze` stays clean.

import 'dart:io';

import 'src/l10n_checks.dart';

const String _usage = '''
Usage: dart run tool/verify_l10n.dart [options]

  --arb-dir DIR         Directory holding the ARB catalogs (default: lib/l10n)
  --template FILE       Template ARB filename (default: app_en.arb)
  --min-coverage RATIO  Minimum share of template keys a locale must translate;
                        the bound is EXCLUSIVE, so exactly this value fails
                        (default: $kDefaultMinCoverage)
  -h, --help            Show this message
''';

/// The language picker is hand-maintained (Dart ships no ICU display-name data,
/// so the autonyms cannot be generated); check 10 verifies it against the ARBs.
const String _appLanguagePath = 'lib/domain/preferences/app_language.dart';
const String _appLanguageDropdownPath =
    'lib/ui/components/app_language_dropdown.dart';

Future<void> main(List<String> args) async {
  String arbDir = 'lib/l10n';
  String templateFile = 'app_en.arb';
  double minCoverage = kDefaultMinCoverage;

  for (int i = 0; i < args.length; i++) {
    final String arg = args[i];
    String? valueFor(String name) {
      if (arg == name) {
        if (i + 1 >= args.length) return null;
        return args[++i];
      }
      if (arg.startsWith('$name=')) return arg.substring(name.length + 1);
      return null;
    }

    if (arg == '-h' || arg == '--help') {
      stdout.write(_usage);
      return;
    }
    if (arg == '--arb-dir' || arg.startsWith('--arb-dir=')) {
      final String? value = valueFor('--arb-dir');
      if (value == null) _fail('--arb-dir needs a value');
      arbDir = value;
      continue;
    }
    if (arg == '--template' || arg.startsWith('--template=')) {
      final String? value = valueFor('--template');
      if (value == null) _fail('--template needs a value');
      templateFile = value;
      continue;
    }
    if (arg == '--min-coverage' || arg.startsWith('--min-coverage=')) {
      final String? value = valueFor('--min-coverage');
      if (value == null) _fail('--min-coverage needs a value');
      final double? parsed = double.tryParse(value);
      if (parsed == null || parsed < 0 || parsed > 1) {
        _fail('--min-coverage must be a ratio between 0 and 1, got "$value"');
      }
      minCoverage = parsed!;
      continue;
    }
    _fail('unknown argument "$arg"');
  }

  final Directory dir = Directory(arbDir);
  if (!dir.existsSync()) {
    _fail('ARB directory "$arbDir" does not exist');
  }

  final RegExp arbName = RegExp(r'^app_([A-Za-z]{2,3}(?:[_-][A-Za-z0-9]+)?)\.arb$');
  final List<ArbSource> sources = <ArbSource>[];
  final List<File> files = dir
      .listSync()
      .whereType<File>()
      .where((File f) => f.path.endsWith('.arb'))
      .toList()
    ..sort((File a, File b) => a.path.compareTo(b.path));

  for (final File file in files) {
    final String name = file.uri.pathSegments.last;
    final RegExpMatch? match = arbName.firstMatch(name);
    if (match == null) {
      _fail('"$name" is not a recognised ARB filename (expected app_<locale>.arb)');
    }
    sources.add(ArbSource(
      fileName: name,
      locale: match!.group(1)!,
      contents: file.readAsStringSync(),
    ));
  }

  if (sources.isEmpty) {
    _fail('no ARB files found in "$arbDir"');
  }

  final RegExpMatch? templateMatch = arbName.firstMatch(templateFile);
  if (templateMatch == null) {
    _fail('"$templateFile" is not a recognised ARB filename');
  }
  final String templateLocale = templateMatch!.group(1)!;

  final String? appLanguageSource = _readIfPresent(_appLanguagePath);
  final String? appLanguageDropdownSource =
      _readIfPresent(_appLanguageDropdownPath);
  if (appLanguageSource == null || appLanguageDropdownSource == null) {
    stderr.writeln(
      'warning: language-picker sources not found; skipping the picker check. '
      'Run this from the repository root.',
    );
  }

  final L10nCheckResult result = checkCatalogs(
    sources: sources,
    templateLocale: templateLocale,
    minCoverage: minCoverage,
    appLanguageSource: appLanguageSource,
    appLanguageDropdownSource: appLanguageDropdownSource,
  );

  if (!result.isValid) {
    stderr.writeln('Translation validation FAILED:');
    for (final String error in result.errors) {
      stderr.writeln('- $error');
    }
    stderr.writeln('');
    stderr.writeln('${result.errors.length} problem(s) found.');
    exitCode = 1;
    return;
  }

  final List<LocaleCoverage> coverage = result.coverage.toList()
    ..sort((LocaleCoverage a, LocaleCoverage b) {
      if (a.isTemplate != b.isTemplate) return a.isTemplate ? -1 : 1;
      if (a.isInProgress != b.isInProgress) return a.isInProgress ? 1 : -1;
      return b.ratio.compareTo(a.ratio);
    });

  final String threshold = '${(minCoverage * 100).toStringAsFixed(0)}%';

  stdout.writeln('Translation coverage (template: $templateFile):');
  for (final LocaleCoverage c in coverage) {
    final String pct = '${(c.ratio * 100).toStringAsFixed(1)}%';
    // An in-progress locale is hosted for translators and not offered to users,
    // so its coverage is information, not a gate. Say which it is on every row,
    // and say what "done" looks like — CI is where you find out it has crossed.
    final String suffix;
    if (c.isTemplate) {
      suffix = '  (template)';
    } else if (c.isInProgress && c.ratio > minCoverage) {
      suffix = '  (in progress — READY TO SHIP: it is above $threshold, so add '
          'an AppLanguage constant + autonym to offer it in the picker)';
    } else if (c.isInProgress) {
      suffix = '  (in progress — not shipped; add an AppLanguage constant + '
          'autonym when it passes $threshold)';
    } else {
      suffix = '';
    }
    stdout.writeln(
      '  ${c.locale.padRight(6)} ${pct.padLeft(6)}  '
      '${c.translated}/${c.templateTotal}$suffix',
    );
  }

  final int shipped =
      coverage.where((LocaleCoverage c) => !c.isInProgress).length;
  final int inProgress = coverage.length - shipped;

  stdout.writeln('');
  stdout.writeln(
    'Translation validation passed for $shipped shipped locale(s) with '
    'coverage greater than $threshold'
    '${inProgress == 0 ? '' : ', plus $inProgress in-progress locale(s) '
        '(structurally checked, not coverage-gated)'}.',
  );
}

String? _readIfPresent(String path) {
  final File file = File(path);
  return file.existsSync() ? file.readAsStringSync() : null;
}

Never _fail(String message) {
  stderr.writeln('error: $message');
  stderr.write(_usage);
  exit(2);
}
