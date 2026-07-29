// Guards the single export surface, against a regression that is INVISIBLE on a
// desktop test machine.
//
// Every export that leaves the app -- a route as GPX/KMZ, the sanitized
// diagnostics log, the CSV / Apple Health / device-sync reports -- goes through
// `lib/core/export/`: staged into the app cache, then either handed to the share
// sheet or written through the platform save picker.
//
// Before that, three features carried their own copy of the staging + ShareParams
// dance, and two different SAVE mechanisms had grown side by side:
//
//   file_picker's saveFile()   -- raises SAF CREATE_DOCUMENT, writes the bytes.
//   file_selector's            -- has NO Android implementation. It THROWS there,
//   getSaveLocation()             so every caller wrapped it in a catch that wrote
//                                 to the app documents directory instead: a
//                                 "Saved." toast for a file no file manager reaches.
//
// The second one works perfectly on a Linux or macOS dev machine, which is why it
// survived in three places. A behavioural test cannot catch its return: it fails
// only on a real phone. This is a source-level guard instead.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// Every `.dart` file under `lib/`, as (path, lines).
Iterable<(String, List<String>)> _libSources() sync* {
  for (final entity in Directory('lib').listSync(recursive: true)) {
    if (entity is! File || !entity.path.endsWith('.dart')) continue;
    yield (entity.path, entity.readAsLinesSync());
  }
}

void main() {
  test('no lib/ code saves an export through file_selector', () {
    final offenders = <String>[];

    for (final (path, lines) in _libSources()) {
      for (var i = 0; i < lines.length; i++) {
        // The argument, not the prose: `core/export/export_saving.dart` explains
        // at length why this call is banned, and must not indict itself.
        if (RegExp(r'\bgetSaveLocation\s*\(').hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}: ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'file_selector has no Android save picker: getSaveLocation() throws '
          'there, and the documents-directory fallback every caller then writes '
          'is unreachable for the user. Save through saveExportBytes() / '
          'saveExportText() in lib/core/export/export_saving.dart, which uses '
          "file_picker's saveFile() and raises the real SAF picker.\n"
          'Offenders:\n  ${offenders.join('\n  ')}',
    );
  });

  test('ShareParams is built in exactly one place', () {
    final offenders = <String>[];

    for (final (path, lines) in _libSources()) {
      if (path.endsWith('core/export/export_sharing.dart')) continue;
      for (var i = 0; i < lines.length; i++) {
        if (RegExp(r'\bShareParams\s*\(').hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}: ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'the share sheet is raised by shareStagedFile() in '
          'lib/core/export/export_sharing.dart. Three features each built their '
          'own ShareParams before, and they drifted -- one forgot the email '
          'subject, one forgot to prune what it staged.\n'
          'Offenders:\n  ${offenders.join('\n  ')}',
    );
  });

  test('exports are staged through the shared cache, not hand-rolled', () {
    final offenders = <String>[];

    for (final (path, lines) in _libSources()) {
      if (path.endsWith('core/export/export_staging.dart')) continue;
      for (var i = 0; i < lines.length; i++) {
        if (RegExp(r'\bgetTemporaryDirectory\b').hasMatch(lines[i])) {
          offenders.add('$path:${i + 1}: ${lines[i].trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'stage exports with ExportStagingCache in '
          'lib/core/export/export_staging.dart. It prunes on every write; a '
          'hand-rolled cache directory is how the diagnostics log came to leave '
          'a copy of itself behind on every single share.\n'
          'Offenders:\n  ${offenders.join('\n  ')}',
    );
  });
}
