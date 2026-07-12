import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// Asserts the fixture still has the SHAPES the tests depend on.
///
/// Not that the JSON parses — that a `>12 h` heart-rate record still exists and
/// still swallows a workout; that more than one app still wrote sleep on the same
/// night; that the GPS route still has enough points to compute a split from.
///
/// Without this, the fixture can be regenerated, re-sliced, or hand-edited into
/// something that still loads, still passes every other test, and no longer
/// contains a single one of the bugs the suite exists to catch. Every test above it
/// would stay green while testing nothing at all. That is a worse position than
/// having no fixture, because it looks like coverage.
void main() {
  const fixturePath = 'test/fixtures/health_connect/golden.json';
  late final Map<String, Object?> fixture;

  List<Map<String, Object?>> records(String key) =>
      (fixture[key]! as List).cast<Map<String, Object?>>();

  setUpAll(() {
    fixture = jsonDecode(File(fixturePath).readAsStringSync())
        as Map<String, Object?>;
  });

  test('a heart-rate record long enough to swallow a workout still exists', () {
    // The whole reason this fixture exists. On the reporter's phone one
    // HeartRateRecord ran 17.48 hours and held 891 samples; the 36-minute workout
    // inside it read as having no heart rate at all, because Health Connect filters
    // a series record by the boundary of the RECORD, not by the times of its samples.
    final longest = records('heartRate').map((r) {
      final hours = ((r['end']! as int) - (r['start']! as int)) / 3600000.0;
      return (record: r, hours: hours);
    }).reduce((a, b) => a.hours > b.hours ? a : b);

    expect(longest.hours, greaterThan(12.0),
        reason: 'No heart-rate record is long enough to swallow a workout. The '
            'fixture no longer contains the bug it was built for, and every test '
            'that depends on it is now green for the wrong reason.');

    // And it must actually CONTAIN a workout, or it swallows nothing.
    final start = longest.record['start']! as int;
    final end = longest.record['end']! as int;
    final swallowed = records('exercise').where((e) =>
        (e['start']! as int) >= start && (e['end']! as int) <= end);

    expect(swallowed, isNotEmpty,
        reason: 'The long heart-rate record contains no exercise session, so it '
            'swallows nothing and proves nothing.');
  });

  test('the swallowed workout is invisible to a naive windowed read', () {
    // The bug, restated as a property of the DATA. Health Connect returns records
    // by their own boundary, so a workout inside a longer record is found by NO
    // record that starts within the workout's window. If that ever stops being
    // true of this fixture, the Kotlin test proving the fix is vacuous.
    final workout = records('exercise').first;
    final wStart = workout['start']! as int;
    final wEnd = workout['end']! as int;

    final startingInside = records('heartRate').where(
        (r) => (r['start']! as int) >= wStart && (r['start']! as int) < wEnd);

    expect(startingInside, isEmpty,
        reason: 'A heart-rate record now STARTS inside the workout window, so a '
            'naive read would find it and the swallowing bug is no longer '
            'reproduced.');
  });

  test('more than one app wrote sleep on the same night', () {
    // Multi-writer sleep is what exercises the merge; multi-writer anything is what
    // exercises dedup. It cannot be invented by hand — this is the shape real data
    // has and synthetic data never does.
    final byNight = <String, Set<String>>{};
    for (final s in records('sleep')) {
      final night = DateTime.fromMillisecondsSinceEpoch(s['start']! as int,
              isUtc: true)
          .toIso8601String()
          .substring(0, 10);
      byNight.putIfAbsent(night, () => {}).add(s['writer']! as String);
    }

    expect(byNight.values.where((w) => w.length > 1), isNotEmpty,
        reason: 'No night has sleep from two different writers, so the merge path '
            'is never exercised.');
  });

  test('a GPS route with enough points to compute splits from', () {
    final withRoute = records('exercise')
        .where((e) => (e['route']! as List).length > 500);

    expect(withRoute, isNotEmpty,
        reason: 'No exercise session has a substantial GPS route, so distance, '
            'pace and the 1 km splits are never computed from real geometry.');
  });

  test('the sibling records that a session does NOT carry are present', () {
    // The walking-activity bug. A Health Connect ExerciseSessionRecord carries
    // almost nothing — a watch writes the walk as a session with a duration, and
    // puts its steps, distance and calories in SEPARATE records over the same
    // window. Reading the session alone reported "Not available" for numbers the
    // watch had recorded, directly above a chart of that same activity's step
    // cadence. Without these siblings in the fixture, the fix is untestable.
    for (final key in ['steps', 'distance', 'activeCalories']) {
      expect(records(key), isNotEmpty, reason: 'No $key sibling records.');
    }
    // And the calorie chain's second branch (active + BMR pro-rated) is
    // unreachable without a BMR record to pro-rate.
    expect(records('basalMetabolicRate'), isNotEmpty);
  });

  test('speed is a SERIES record, so splits hit the same bug as heart rate', () {
    // Same shape, same trap: Health Connect filters SpeedRecord by the record's own
    // boundary too, which is why the 1 km splits silently fell back to "estimated"
    // on exactly the activities whose heart rate had vanished. A speed record with
    // no samples proves nothing.
    final speed = records('speed');
    expect(speed, isNotEmpty);
    expect((speed.first['dt']! as List).length, greaterThan(10),
        reason: 'The speed record has almost no samples, so no split can be '
            'computed from it.');
  });

  test('the synthetic records are exactly the two we could not derive', () {
    // The export contains ZERO PowerRecords and ZERO CyclingPedalingCadenceRecords —
    // this person has no power meter. Those two are hand-authored so the power fix
    // (e7dfba37) has something to be tested against.
    //
    // Everything else must inherit its shape from real data. If a `synthetic` flag
    // ever appears on a third record type, someone has quietly started inventing
    // the thing the fixture exists to preserve.
    final synthetic = <String>[];
    for (final entry in fixture.entries) {
      if (entry.key == 'manifest' || entry.value is! List) continue;
      for (final r in (entry.value! as List).cast<Map<String, Object?>>()) {
        if (r['synthetic'] == true) synthetic.add(entry.key);
      }
    }

    expect(synthetic.toSet(), {'power', 'cyclingCadence'},
        reason: 'The set of INVENTED record types has changed. Every other record '
            'here derives its shape from real data — that is the whole point.');
    expect(records('power'), isNotEmpty,
        reason: 'No power record, so the power read (e7dfba37) has nothing to '
            'prove itself against.');
  });

  test('records carry the provenance the port kept losing', () {
    // recordingMethod and lastModifiedTime were dropped from two Pigeon messages and
    // read null on every record for months. If the fixture does not carry them, the
    // tests that pin them are asserting against nulls on both sides.
    final all = [...records('heartRate'), ...records('exercise'), ...records('sleep')];

    expect(all.where((r) => r['recordingMethod'] != null), isNotEmpty,
        reason: 'No record carries a recordingMethod.');
    expect(all.where((r) => r['lastModified'] != null), isNotEmpty,
        reason: 'No record carries a lastModifiedTime — the dedup tie-break cannot '
            'be tested.');
    expect(all.where((r) => r['startZoneOffsetSeconds'] != null), isNotEmpty,
        reason: 'No record carries a zone offset.');
  });
}
