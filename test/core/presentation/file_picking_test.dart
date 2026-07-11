// Guards the fix for a crash that is INVISIBLE until someone picks a big file.
//
// `file_selector_android` reads the whole picked file into a byte[] and ships it
// across the Pigeon channel:
//
//   final byte[] bytes = new byte[size];      // FileSelectorApiImpl.java:352
//   dataInputStream.readFully(bytes);
//
// A 205 MB offline-map pack therefore threw OutOfMemoryError against a 256 MB heap,
// and an Apple Health export.zip -- routinely gigabytes -- never stood a chance.
//
// Nothing about `openFile()` LOOKS dangerous, and it works perfectly on the small
// files any test or dev would try. So a normal unit test cannot catch a regression
// here: it would have to allocate hundreds of MB to fail. This is a source-level
// guard instead, and it is the only thing that will stop the next person
// reintroducing it.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('no lib/ code picks an INPUT file through file_selector', () {
    final offenders = <String>[];

    for (final entity in Directory('lib').listSync(recursive: true)) {
      if (entity is! File || !entity.path.endsWith('.dart')) continue;
      final lines = entity.readAsLinesSync();
      for (var i = 0; i < lines.length; i++) {
        final line = lines[i];
        // `getSaveLocation` is fine and stays: saving reads nothing.
        if (RegExp(r'\bopenFiles?\s*\(').hasMatch(line)) {
          offenders.add('${entity.path}:${i + 1}: ${line.trim()}');
        }
      }
    }

    expect(
      offenders,
      isEmpty,
      reason: 'file_selector\'s openFile()/openFiles() load the ENTIRE file into '
          'memory on Android and OOM on anything large (a 205 MB map pack already '
          'did). Pick input files with pickInputFile()/pickInputFiles() from '
          'lib/core/presentation/file_picking.dart, which returns a path.\n'
          'Offenders:\n  ${offenders.join('\n  ')}',
    );
  });

  test('the pick helpers never ask the platform for the file contents', () {
    // `withData: true` would reintroduce the bug through the new plugin instead of
    // the old one -- the crash is about loading bytes, not about which package does it.
    final source =
        File('lib/core/presentation/file_picking.dart').readAsStringSync();

    // Match the ARGUMENT (trailing comma), not the prose in the doc comment above.
    expect(
      RegExp(r'withData:\s*true\s*,').hasMatch(source),
      isFalse,
      reason: 'withData: true loads the whole file into memory, which is the bug '
          'this file exists to avoid',
    );
    expect(
      RegExp(r'withReadStream:\s*true\s*,').hasMatch(source),
      isFalse,
      reason: 'the callers want a path; a read stream is not what they consume',
    );
    expect(
      RegExp(r'withData:\s*false\s*,').allMatches(source).length,
      2,
      reason: 'both pickInputFile and pickInputFiles must opt out EXPLICITLY, '
          'rather than trusting the plugin default to stay false',
    );
  });
}
