import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_view_model.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

/// A GPX with no locations in it — and an activity all the same.
///
/// Both fixtures are REAL HealthFit exports the app refused with "GPX route must
/// contain at least 2 timestamped location points". The reasoning behind that
/// refusal was that a GPX is a list of PLACES, so an indoor session cannot be
/// written as one. The files say otherwise: every `<trkpt>` carries a `<time>`
/// and NO `lat`/`lon` whatsoever — 1931 of them for a strength session, 1422 for
/// an indoor run — and the strength file hangs a heart rate off each. The GPX
/// schema does demand those attributes; exporters omit them anyway, and the
/// result is not a corrupt file. It is a timestamped series with the positions
/// left out, which is precisely what an indoor activity is. Other apps import
/// these. So do we, now.
///
/// What a routeless GPX genuinely lacks is DISTANCE and CALORIES. Those are not
/// invented: distance stays zero, and the calories are estimated by the entry
/// form from the duration — which is safe here exactly because the file measured
/// none to contradict.
Future<RouteFileImport> _parse(String fixture) async {
  final bytes = await File('test/fixtures/gpx/$fixture').readAsBytes();
  return RouteFileParser.parseFile(bytes, fileName: fixture);
}

({ActivityEntryType type, ActivityWriteRequest? request, String activeCalories})
    _import(RouteFileImport parsed) {
  final clock = ActivityEntryClock.system();
  final state = activityStateWithRouteImport(
    initialActivityEntryState(clock, const {'write-exercise'}),
    parsed,
    UnitSystem.metric,
    clock,
  );
  return (
    type: state.selectedActivityType,
    request: buildWriteRequest(state, UnitSystem.metric),
    activeCalories: state.activeCaloriesText,
  );
}

void main() {
  test('a strength session: 1931 heartbeats and not one location', () async {
    final parsed = await _parse('strength_training.gpx');

    expect(parsed.points, isEmpty);
    // The session is in the timestamps: 05:50:28 to 06:23:23.
    expect(parsed.startTime, DateTime.utc(2026, 7, 8, 5, 50, 28));
    expect(parsed.endTime, DateTime.utc(2026, 7, 8, 6, 23, 23));
    expect(parsed.durationSeconds, 1975);
    // And the heart rate was there all along, in the extensions.
    expect(parsed.bleSamples.heartRateSamples, hasLength(1931));
    expect(parsed.bleSamples.heartRateSamples.first.beatsPerMinute, 101);

    final imported = _import(parsed);

    expect(imported.request, isNotNull, reason: 'this used to throw');
    expect(imported.type.exerciseType, ExerciseSessionType.strengthTraining);
    expect(imported.request!.routePoints, isEmpty);
    expect(imported.request!.bleSamples.heartRateSamples, hasLength(1931));
    expect(imported.request!.distanceMeters ?? 0.0, 0.0);
  });

  test('an indoor run: the times, and the sport the file names', () async {
    final parsed = await _parse('indoor_running.gpx');

    expect(parsed.points, isEmpty);
    expect(parsed.durationSeconds, 1421);
    // `<trk><type>running</type>` — the file says what it is, and is believed.
    expect(parsed.type, 'running');

    final imported = _import(parsed);

    expect(imported.request, isNotNull);
    expect(imported.request!.exerciseType, ExerciseSessionType.running);
    // Nothing was measured, so the estimate is free to fill both fields — the
    // rule that keeps a guess from ever standing beside a measurement.
    expect(imported.activeCalories, isNotEmpty);
  });

  test('a GPX with neither places nor times is still refused', () {
    // The guard that survives: an empty (or corrupt, or HTML) file must not
    // arrive as a blank activity. What changed is that "no LOCATIONS" no longer
    // means "no activity" — "no timestamps either" does.
    const empty = '<?xml version="1.0"?><gpx version="1.1"><trk><trkseg>'
        '</trkseg></trk></gpx>';

    expect(
      () => RouteFileParser.parse(empty, fileName: 'empty.gpx'),
      throwsA(isA<RouteImportException>()),
    );
  });

  test('a routed GPX keeps its route AND gains its heart rate', () {
    // The same collector runs on files that do have a track, so a GPX whose
    // trackpoints carry `gpxtpx:hr` no longer throws that heart rate away at the
    // parser — it used to import as a bare line on a map.
    const gpx = '''
<?xml version="1.0"?>
<gpx version="1.1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
  <trk><type>running</type><trkseg>
    <trkpt lat="52.5" lon="13.4"><ele>34</ele><time>2026-07-08T05:50:28Z</time>
      <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>120</gpxtpx:hr><gpxtpx:cad>84</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
    </trkpt>
    <trkpt lat="52.51" lon="13.41"><ele>40</ele><time>2026-07-08T05:55:28Z</time>
      <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>148</gpxtpx:hr><gpxtpx:cad>86</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
    </trkpt>
  </trkseg></trk>
</gpx>''';

    final parsed = RouteFileParser.parse(gpx, fileName: 'run.gpx');

    expect(parsed.points, hasLength(2));
    expect(
      [for (final s in parsed.bleSamples.heartRateSamples) s.beatsPerMinute],
      [120, 148],
    );
    // Per foot, as everywhere else: 84 is 168 steps a minute.
    expect(
      [for (final s in parsed.bleSamples.stepsCadenceSamples) s.stepsPerMinute],
      [168, 172],
    );
  });
}
