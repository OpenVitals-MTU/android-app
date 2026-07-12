// Builds the committed test fixture from a real Health Connect export.
//
//   dart run tool/health_fixture/build.dart \
//     --db "test_objects/Health Connect/health_connect_export.db" \
//     --out test/fixtures/health_connect/golden.json
//
// The export is a 106 MB dump of a real person's health data — heart rate, sleep,
// weight, blood pressure, menstruation, sexual activity. It is gitignored, and
// this repository is PUBLIC. So the fixture is DERIVED, never copied:
//
//   KEPT, because it is the only thing that catches bugs:
//     record boundaries, record→sample parentage, which app wrote what,
//     recordingMethod, clientRecordId/version, lastModifiedTime, zone offsets,
//     and WHICH FIELDS EACH WRITER LEAVES NULL.
//
//   REPLACED:
//     every value (bpm, speed, counts), every timestamp (one global shift),
//     every writer package name, every id, every title and note.
//
//   DROPPED ENTIRELY:
//     menstruation, sexual activity, body fat — not scrubbed, not present.
//
// The structure is what reproduces the bugs. A 17.48-hour HeartRateRecord that
// swallows a workout does so because of its BOUNDARIES, not because of its bpm
// values — so the boundaries survive verbatim and the values do not.
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:sqlite3/sqlite3.dart';

/// Every timestamp moves by this much. ONE shift for the whole fixture — never
/// per-record, which would decouple a workout from the heart rate inside it.
///
/// A whole number of weeks, so weekday alignment survives (a Monday stays a
/// Monday, and the weekly views still see a real week).
const int _shiftDays = 364; // 52 weeks

/// Real writer package -> the name it goes into the public repo under.
///
/// `tech.mmarca.openvitals*` is NOT aliased. It is load-bearing: `isOpenVitalsEntry`,
/// record ownership and the manual-entry count all key off it, so renaming it would
/// quietly disable the very logic the fixture exists to test.
///
/// The aliases name a ROLE, never the product. `com.example.healthsync` would still
/// have told the world which apps this person runs, which is most of what the scrub
/// is for — an alias that echoes the vendor is barely an alias.
const Map<String, String> _writerAliases = {
  'nodomain.freeyourgadget.gadgetbridge': 'com.example.openwatch',
  'com.garmin.android.apps.connectmobile': 'com.example.watchvendor',
  'com.google.android.apps.fitness': 'com.example.fitplatform',
  'com.google.android.gms': 'com.example.fitplatform.gms',
  'de.dennisguse.opentracks.debug': 'com.example.tracker',
  'de.tadris.fitness.debug': 'com.example.tracker2',
  'com.hevy': 'com.example.strengthapp',
  'com.technogym.tgapp': 'com.example.gymequip',
  'com.cemcakmak.hydrotracker': 'com.example.hydration',
  'nl.appyhapps.healthsync': 'com.example.syncbridge',
  'io.homeassistant.companion.android.minimal': 'com.example.homeautomation',
  'dev.easonhuang.heartwood': 'com.example.heartapp',
  'com.monkopedia.healthdisconnect': 'com.example.privacytool',
  'dev.manu.hcdashboard': 'com.example.dashboard',
  'dev.manu.openvitals': 'com.example.dashboard2',
  'com.example.ot2hc': 'com.example.importer',
  'android': 'android',
  'com.android.shell': 'com.android.shell',
};

/// The slice. One contiguous week, chosen because it already contains almost
/// every scenario — see the manifest it writes.
final DateTime _from = DateTime.utc(2026, 6, 18);
final DateTime _to = DateTime.utc(2026, 6, 26);

void main(List<String> args) {
  final dbPath = _arg(args, '--db') ??
      'test_objects/Health Connect/health_connect_export.db';
  final outPath = _arg(args, '--out') ?? 'test/fixtures/health_connect/golden.json';

  if (!File(dbPath).existsSync()) {
    stderr.writeln('No export at $dbPath.\n'
        'It is gitignored on purpose. The FIXTURE is the committed artifact — you '
        'only need the export to regenerate it.');
    exit(1);
  }

  final db = sqlite3.open(dbPath, mode: OpenMode.readOnly);
  final writers = _writers(db);
  final exercise = _exercise(db, writers);
  final fixture = <String, Object?>{
    'heartRate': _heartRate(db, writers),
    'exercise': exercise,
    'sleep': _sleep(db, writers),

    // SERIES records. The same shape as heart rate, and the same bug: Health
    // Connect filters them by the RECORD's boundary, so a workout buried inside a
    // longer record reads as having no speed either — which is why the 1 km splits
    // silently fell back to "estimated" on exactly the activities whose heart rate
    // had vanished.
    'speed': _series(db, writers, 'SpeedRecordTable', 'speed_record_table', 'speed',
        (v, i) => 1.6 + (i % 9) * 0.15),
    'stepsCadence': _series(db, writers, 'StepsCadenceRecordTable',
        'steps_cadence_record_table', 'rate', (v, i) => 78.0 + (i % 20)),

    // SIBLING records — the ones a watch writes BESIDE a session rather than in it.
    // Reading the session alone is why a recorded walk showed "Steps: Not
    // available" above a chart of its own step cadence.
    'steps': _steps(db, writers),
    'distance': _interval(db, writers, 'distance_record_table', 'distance',
        (durationMs) => durationMs / 1000 * 1.4),
    'activeCalories': _interval(db, writers, 'active_calories_burned_record_table',
        'energy', (durationMs) => durationMs / 60000 * 5.0 * 4184),
    'totalCalories': _interval(db, writers, 'total_calories_burned_record_table',
        'energy', (durationMs) => durationMs / 60000 * 7.0 * 4184),
    'elevationGained': _interval(db, writers, 'elevation_gained_record_table',
        'elevation', (durationMs) => durationMs / 60000 * 0.8),
    // The calorie fallback chain: recorded total wins, else active + BMR pro-rated
    // over the window. Without BMR records the chain's second branch is unreachable.
    // BMR is an INSTANT record (a rate at a moment), not an interval. The calorie
    // chain pro-rates it across the window it needs.
    'basalMetabolicRate': _instant(db, writers,
        'basal_metabolic_rate_record_table', 'basal_metabolic_rate',
        (_) => 1650.0),

    'hrv': _instant(db, writers, 'heart_rate_variability_rmssd_record_table',
        'heart_rate_variability_millis', (i) => 42.0 + (i % 17)),
    'restingHeartRate': _instant(db, writers, 'resting_heart_rate_record_table',
        'beats_per_minute', (i) => 52.0 + (i % 8)),
    'hydration': _interval(db, writers, 'hydration_record_table', 'volume',
        (_) => 0.25),

    // NOT IN THE SOURCE DATA. The export has ZERO PowerRecords and ZERO
    // CyclingPedalingCadenceRecords — this person has no power meter. So these are
    // hand-authored onto a real session, and flagged as such: they carry no
    // provenance from anyone's real data, and they are the only records here that
    // do not.
    //
    // They exist because the app WRITES PowerRecord from a BLE sensor, asks Health
    // Connect for READ_POWER, and until e7dfba37 never read it back — so the fix
    // has nothing to be tested against unless we make some.
    'power': _syntheticSeries(exercise, 'power', (i) => 180.0 + (i % 40)),
    'cyclingCadence':
        _syntheticSeries(exercise, 'cyclingCadence', (i) => 82.0 + (i % 12)),
  };
  fixture['manifest'] = _manifest(fixture, writers);
  db.close();

  final file = File(outPath)..parent.createSync(recursive: true);
  file.writeAsStringSync(const JsonEncoder.withIndent('  ').convert(fixture));

  final kb = (file.lengthSync() / 1024).round();
  stdout.writeln('Wrote $outPath (${kb}kB)');
  final counts =
      ((fixture['manifest']! as Map)['counts']! as Map).cast<String, int>();
  counts.forEach((k, v) => stdout.writeln('  $k: $v'));
}

// ── the shift, the aliases, the ids ─────────────────────────────────────────

int _shift(int epochMs) =>
    epochMs - const Duration(days: _shiftDays).inMilliseconds;

/// Rehashed, not randomised: the same input always gives the same output, so the
/// EQUALITY RELATION survives. Records that shared a clientRecordId still share
/// one, which is what dedup-by-client-id and the hydration↔nutrition pairing key
/// off.
String _id(String? real, String salt) {
  if (real == null) return '';
  return sha256.convert(utf8.encode('$salt::$real')).toString().substring(0, 16);
}

Map<int, String> _writers(Database db) {
  final rows = db.select('SELECT row_id, package_name FROM application_info_table');
  return {
    for (final r in rows) r['row_id'] as int: _alias(r['package_name'] as String),
  };
}

/// The app's OWN package is kept verbatim. `isOpenVitalsEntry`, record ownership
/// and the manual-entry count all key off it — aliasing it would quietly disable
/// the very logic the fixture exists to test. (An earlier version of this file
/// aliased it to `com.example.unknownN` via the fallback below, and the leak check
/// is what caught it.)
String _alias(String package) {
  if (package.startsWith('tech.mmarca.openvitals')) return package;
  final alias = _writerAliases[package];
  if (alias != null) return alias;
  // Unknown writer: alias it rather than let a real package name through. Failing
  // closed matters more here than being helpful — this file is committed publicly.
  return 'com.example.writer${sha256.convert(utf8.encode(package)).toString().substring(0, 6)}';
}

/// The provenance block every record carries. This is the half the port kept
/// losing, so it is the half the fixture is most careful with.
Map<String, Object?> _provenance(Row r, Map<int, String> writers) => {
      'id': _id(r['row_id'].toString(), 'rec'),
      'writer': writers[r['app_info_id']] ?? 'com.example.unknown',
      'start': _shift(r['start_time'] as int),
      'end': _shift(r['end_time'] as int),
      'startZoneOffsetSeconds': r['start_zone_offset'],
      'endZoneOffsetSeconds': r['end_zone_offset'],
      'recordingMethod': r['recording_method'],
      'lastModified': r['last_modified_time'] == null
          ? null
          : _shift(r['last_modified_time'] as int),
      'clientRecordId': r['client_record_id'] == null
          ? null
          : _id(r['client_record_id'] as String, 'client'),
      // Health Connect stores client_record_version as TEXT, so it arrives as a
      // String. The Pigeon field is an int, and the cast throws — which the data
      // source's `_catch` then swallows into an empty list. A whole screen goes
      // blank and nothing anywhere reports an error. Parse it here.
      'clientRecordVersion': _asInt(r['client_record_version']),
    };

int? _asInt(Object? value) => switch (value) {
      null => null,
      final int v => v,
      final String v => int.tryParse(v),
      final num v => v.toInt(),
      _ => null,
    };

// ── record types ────────────────────────────────────────────────────────────

/// Heart rate, delta-encoded. The 891-sample swallowing record is the point of
/// the whole fixture, so it is kept at FULL fidelity — every sample, exact
/// sample-to-sample spacing. The irregular cadence is real shape: a writer that
/// samples every 5 s during exercise and every 10 min at rest is what produces
/// records like this one.
///
/// The bpm VALUES are synthesized. They are the one thing that cannot cause a
/// bug: nothing in the app branches on 62 vs 64.
List<Map<String, Object?>> _heartRate(Database db, Map<int, String> writers) {
  final out = <Map<String, Object?>>[];
  final records = db.select(
    'SELECT * FROM heart_rate_record_table '
    'WHERE start_time >= ? AND start_time < ? ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  for (final r in records) {
    final samples = db.select(
      'SELECT epoch_millis FROM heart_rate_record_series_table '
      'WHERE parent_key = ? ORDER BY epoch_millis',
      [r['row_id']],
    );
    if (samples.isEmpty) continue;

    final times = [for (final s in samples) _shift(s['epoch_millis'] as int)];
    final deltas = <int>[];
    for (var i = 1; i < times.length; i++) {
      deltas.add(times[i] - times[i - 1]);
    }
    out.add({
      ..._provenance(r, writers),
      't0': times.first,
      'dt': deltas,
      'bpm': [
        for (var i = 0; i < times.length; i++) _syntheticBpm(times[i], i),
      ],
    });
  }
  return out;
}

/// Plausible, and COHERENT: a resting baseline with a slow diurnal drift. Not
/// noise — a chart of it should look like a heart rate, because a human will look
/// at these tests' output when one fails.
int _syntheticBpm(int epochMs, int index) {
  final hour = DateTime.fromMillisecondsSinceEpoch(epochMs, isUtc: true).hour;
  final base = hour < 6 ? 54 : 66;
  return base + (index * 7) % 11;
}

List<Map<String, Object?>> _exercise(Database db, Map<int, String> writers) {
  final records = db.select(
    'SELECT * FROM exercise_session_record_table '
    'WHERE start_time >= ? AND start_time < ? ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  return [
    for (final r in records)
      {
        ..._provenance(r, writers),
        'exerciseType': r['exercise_type'],
        // Never carry a real free-text note or title across.
        'title': r['title'] == null ? null : 'Session',
        'notes': r['notes'] == null ? null : 'Recorded by a device.',
        'route': _route(db, r['row_id'] as int),
      },
  ];
}

/// The GPS track, rotated and re-anchored to a synthetic origin. Shape, length and
/// speed profile survive exactly — so distance, splits and pace are genuinely
/// exercised — while the location does not. Altitude keeps its relative profile.
List<Map<String, Object?>> _route(Database db, int parentKey) {
  final points = db.select(
    'SELECT timestamp_millis, latitude, longitude, altitude, horizontal_accuracy '
    'FROM exercise_route_table WHERE parent_key = ? ORDER BY timestamp_millis',
    [parentKey],
  );
  if (points.isEmpty) return const [];

  final lat0 = points.first['latitude'] as double;
  final lon0 = points.first['longitude'] as double;
  final alt0 = points.first['altitude'] as double;
  // A synthetic origin in the North Sea. Nothing is there.
  const anchorLat = 56.0;
  const anchorLon = 3.0;

  return [
    for (final p in points)
      {
        't': _shift(p['timestamp_millis'] as int),
        'lat': anchorLat + ((p['latitude'] as double) - lat0),
        'lon': anchorLon + ((p['longitude'] as double) - lon0),
        'alt': 10.0 + ((p['altitude'] as double) - alt0),
        'acc': p['horizontal_accuracy'],
      },
  ];
}

List<Map<String, Object?>> _sleep(Database db, Map<int, String> writers) {
  final records = db.select(
    'SELECT * FROM sleep_session_record_table '
    'WHERE start_time >= ? AND start_time < ? ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  return [
    for (final r in records)
      {
        ..._provenance(r, writers),
        'title': r['title'] == null ? null : 'Sleep',
        'notes': r['notes'] == null ? null : 'Sleep data from a device.',
        'stages': [
          for (final s in db.select(
            'SELECT stage_start_time, stage_end_time, stage_type '
            'FROM sleep_stages_table WHERE parent_key = ? ORDER BY stage_start_time',
            [r['row_id']],
          ))
            {
              'start': _shift(s['stage_start_time'] as int),
              'end': _shift(s['stage_end_time'] as int),
              'type': s['stage_type'],
            },
        ],
      },
  ];
}

/// Interval records — the sibling records a watch writes BESIDE a session, which
/// is why a recorded walk showed no steps. Counts are synthesized from the
/// duration, so they stay proportionate and the aggregates remain sane.
List<Map<String, Object?>> _steps(Database db, Map<int, String> writers) {
  final records = db.select(
    'SELECT * FROM steps_record_table '
    'WHERE start_time >= ? AND start_time < ? ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  return [
    for (final r in records)
      {
        ..._provenance(r, writers),
        'count': 1 +
            ((r['end_time'] as int) - (r['start_time'] as int)) ~/ 1000 ~/ 2,
      },
  ];
}

/// A series record: a parent carrying the provenance, and its nested samples.
///
/// Exactly the shape that causes the bug, so it is exactly the shape that must
/// survive. Sample TIMES are kept (shifted); sample VALUES are synthesized.
List<Map<String, Object?>> _series(
  Database db,
  Map<int, String> writers,
  String parentTable,
  String sampleTable,
  String valueColumn,
  double Function(double real, int index) synth,
) {
  final out = <Map<String, Object?>>[];
  final records = db.select(
    'SELECT * FROM $parentTable WHERE start_time >= ? AND start_time < ? '
    'ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  for (final r in records) {
    final samples = db.select(
      'SELECT epoch_millis, $valueColumn FROM $sampleTable '
      'WHERE parent_key = ? ORDER BY epoch_millis',
      [r['row_id']],
    );
    if (samples.isEmpty) continue;

    final times = [for (final s in samples) _shift(s['epoch_millis'] as int)];
    out.add({
      ..._provenance(r, writers),
      't0': times.first,
      'dt': [for (var i = 1; i < times.length; i++) times[i] - times[i - 1]],
      'v': [
        for (var i = 0; i < samples.length; i++)
          synth((samples[i][valueColumn] as num).toDouble(), i),
      ],
    });
  }
  return out;
}

/// An interval record — a total over a window. Steps, distance, calories,
/// elevation, BMR, hydration.
///
/// The VALUE is derived from the window's own duration, so totals stay
/// proportionate to the time they cover and the aggregates over them remain sane.
/// A distance of 4 km over 40 minutes has to keep being a plausible pace, or every
/// split assertion downstream is meaningless.
List<Map<String, Object?>> _interval(
  Database db,
  Map<int, String> writers,
  String table,
  String valueColumn,
  double Function(int durationMs) synth,
) {
  final records = db.select(
    'SELECT * FROM $table WHERE start_time >= ? AND start_time < ? '
    'ORDER BY start_time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  return [
    for (final r in records)
      {
        ..._provenance(r, writers),
        'v': synth((r['end_time'] as int) - (r['start_time'] as int)),
      },
  ];
}

/// An instantaneous record — one value at one moment. HRV, resting heart rate.
/// Its time column is `time`, not `start_time`, so it cannot share [_interval].
List<Map<String, Object?>> _instant(
  Database db,
  Map<int, String> writers,
  String table,
  String valueColumn,
  double Function(int index) synth,
) {
  final records = db.select(
    'SELECT * FROM $table WHERE time >= ? AND time < ? ORDER BY time',
    [_from.millisecondsSinceEpoch, _to.millisecondsSinceEpoch],
  );

  var i = 0;
  return [
    for (final r in records)
      {
        'id': _id(r['row_id'].toString(), 'rec'),
        'writer': writers[r['app_info_id']] ?? 'com.example.unknown',
        'time': _shift(r['time'] as int),
        'zoneOffsetSeconds': r['zone_offset'],
        'recordingMethod': r['recording_method'],
        'lastModified': r['last_modified_time'] == null
            ? null
            : _shift(r['last_modified_time'] as int),
        'v': synth(i++),
      },
  ];
}

/// Wholly invented, and labelled as such.
///
/// The export contains ZERO PowerRecords and ZERO CyclingPedalingCadenceRecords —
/// this person has no power meter, so there is nothing to derive from. But the app
/// writes PowerRecord from a BLE sensor, asks Health Connect for READ_POWER, and
/// until e7dfba37 never read it back. A fix with nothing to test it against is a
/// fix that will break again.
///
/// So: a sample a minute across a real session's window. Every other record in this
/// fixture inherits its SHAPE from real data; these two do not, and `synthetic:
/// true` says so, so nobody later mistakes them for evidence of how a real power
/// meter behaves.
List<Map<String, Object?>> _syntheticSeries(
  List<Map<String, Object?>> exercise,
  String kind,
  double Function(int index) synth,
) {
  if (exercise.isEmpty) return const [];
  // The session with a GPS route: the long outdoor one. That is where a power
  // meter and a cadence sensor would actually be, and attaching them to a
  // three-minute session would produce a record with one sample in it.
  final session = exercise.reduce((a, b) =>
      (a['route']! as List).length >= (b['route']! as List).length ? a : b);
  final start = session['start']! as int;
  final end = session['end']! as int;

  final times = <int>[];
  for (var t = start; t < end; t += 60000) {
    times.add(t);
  }
  if (times.length < 2) return const [];

  return [
    {
      'synthetic': true,
      'id': _id('$kind-synthetic', 'rec'),
      'writer': session['writer'],
      'start': start,
      'end': end,
      'startZoneOffsetSeconds': session['startZoneOffsetSeconds'],
      'endZoneOffsetSeconds': session['endZoneOffsetSeconds'],
      'recordingMethod': 2, // AUTOMATICALLY_RECORDED
      'lastModified': session['lastModified'],
      'clientRecordId': null,
      'clientRecordVersion': null,
      't0': times.first,
      'dt': [for (var i = 1; i < times.length; i++) times[i] - times[i - 1]],
      'v': [for (var i = 0; i < times.length; i++) synth(i)],
    },
  ];
}

// ── manifest ────────────────────────────────────────────────────────────────

Map<String, Object?> _manifest(
  Map<String, Object?> fixture,
  Map<int, String> writers,
) {
  final hr = fixture['heartRate']! as List<Map<String, Object?>>;
  final longest = hr.reduce((a, b) =>
      (a['end']! as int) - (a['start']! as int) >
              (b['end']! as int) - (b['start']! as int)
          ? a
          : b);

  return {
    'version': 1,
    'note': 'GENERATED by tool/health_fixture/build.dart from a real Health '
        'Connect export. Values, timestamps, writer names and ids are all '
        'synthetic; record BOUNDARIES and provenance are real, because that is '
        'what reproduces the bugs. Do not hand-edit — regenerate.',
    'shiftDays': _shiftDays,
    // The alias table is NOT written here. Its KEYS are the real package names —
    // emitting it would have leaked every one of them into a public repo, in the
    // file whose whole job is to not do that. The leak check caught it.
    'writers': writers.values.toSet().toList()..sort(),
    'counts': {
      for (final e in fixture.entries)
        if (e.value is List) e.key: (e.value! as List).length,
    },
    // Named so tests never hardcode a date.
    'days': {
      'swallowingHr': DateTime.fromMillisecondsSinceEpoch(
        longest['start']! as int,
        isUtc: true,
      ).toIso8601String().substring(0, 10),
    },
    'longestHeartRateRecordHours':
        ((longest['end']! as int) - (longest['start']! as int)) / 3600000.0,
  };
}

String? _arg(List<String> args, String name) {
  final i = args.indexOf(name);
  return i == -1 || i + 1 >= args.length ? null : args[i + 1];
}
