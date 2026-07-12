import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// The safety control, and the most important test in this repository.
///
/// `test/fixtures/health_connect/golden.json` is DERIVED from a real person's
/// Health Connect export — heart rate, sleep, workouts, the lot — and this repo is
/// PUBLIC. Git history is append-only: a leak committed here cannot be taken back
/// by a later commit. So the fixture is checked on every run, not just on the day
/// it was generated.
///
/// It is not hypothetical. The first generated fixture leaked twice, and both were
/// caught here:
///
///  * the manifest embedded the writer ALIAS TABLE, whose *keys* are the real
///    package names — the scrubber leaked its own key;
///  * `com.example.healthsync` as an alias still told the world which apps this
///    person runs, which is most of what the scrub is for.
void main() {
  const fixturePath = 'test/fixtures/health_connect/golden.json';

  late final String raw;
  late final Map<String, Object?> fixture;

  setUpAll(() {
    raw = File(fixturePath).readAsStringSync();
    fixture = jsonDecode(raw) as Map<String, Object?>;
  });

  test('no real writer, vendor or person appears anywhere in the fixture', () {
    // Substrings, not whole package names: an alias that merely *contains* the
    // vendor ("com.example.healthsync") is not an alias.
    const denied = [
      'gadgetbridge', 'freeyourgadget', 'nodomain',
      'garmin', 'hevy', 'technogym', 'opentracks', 'dennisguse',
      'tadris', 'hydrotracker', 'cemcakmak', 'healthsync', 'appyhapps',
      'homeassistant', 'heartwood', 'easonhuang', 'monkopedia', 'ot2hc',
      // The maintainer's own name appears in real package names (dev.manu.*).
      'manu',
    ];

    final found = denied
        .where((needle) => raw.toLowerCase().contains(needle))
        .toList();

    expect(
      found,
      isEmpty,
      reason: 'The fixture leaks real identifying strings into a PUBLIC repo, and '
          'git history cannot un-commit them:\n  ${found.join('\n  ')}\n'
          'Fix tool/health_fixture/build.dart and regenerate. Do NOT hand-edit the '
          'fixture — the next regeneration would put it straight back.',
    );
  });

  test('every writer is either an example alias or OpenVitals itself', () {
    // Fail CLOSED. A writer the alias table has never heard of must not be able to
    // reach the file just because nobody thought about it.
    final writers =
        ((fixture['manifest']! as Map)['writers']! as List).cast<String>();

    for (final writer in writers) {
      final allowed = writer.startsWith('com.example.') ||
          // Load-bearing: isOpenVitalsEntry, ownership, the manual-entry count.
          writer.startsWith('tech.mmarca.openvitals') ||
          // Health Connect's own platform writers. Not a person.
          writer == 'android' ||
          writer == 'com.android.shell';

      expect(allowed, isTrue,
          reason: '"$writer" is neither an alias nor a platform writer. The '
              'aliasing in build.dart failed open.');
    }
  });

  test('no coordinate is anywhere near the real route', () {
    // The track is rotated and re-anchored to a synthetic origin in the North Sea.
    // Shape, length and speed profile survive — so distance, pace and splits are
    // genuinely exercised — but the location does not.
    for (final session in (fixture['exercise']! as List).cast<Map<String, Object?>>()) {
      for (final point in (session['route']! as List).cast<Map<String, Object?>>()) {
        expect(point['lat']! as double, inInclusiveRange(55.0, 57.0),
            reason: 'A route point is outside the synthetic bbox — the GPS '
                're-anchoring did not happen, and this is a real place someone was.');
        expect(point['lon']! as double, inInclusiveRange(2.0, 4.0));
      }
    }
  });

  test('no free text survived', () {
    // Titles and notes are the one field a human types into. Never carry one across.
    for (final key in ['exercise', 'sleep']) {
      for (final r in (fixture[key]! as List).cast<Map<String, Object?>>()) {
        for (final field in ['title', 'notes']) {
          final value = r[field] as String?;
          if (value == null) continue;
          expect(
            const {'Session', 'Sleep', 'Recorded by a device.',
                   'Sleep data from a device.'},
            contains(value),
            reason: '$key.$field is "$value" — that is not one of the canned '
                'replacements, so a real note or title reached the fixture.',
          );
        }
      }
    }
  });
}
