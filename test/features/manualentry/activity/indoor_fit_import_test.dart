import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_edit_mapper.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_view_model.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

/// Importing an activity that has NO GPS: an indoor run, a trainer ride, a
/// strength session.
///
/// Both fixtures are REAL files a user could not import. They parsed fine — the
/// FIT parser has always had a routeless branch — and then died one layer later,
/// twice over:
///
///  * CALORIES. A FIT session records `total_calories` and has no active-calorie
///    field, so active came back null and the form ESTIMATED it from METs and
///    distance. The estimate landed beside the file's own measured total and
///    exceeded it (226 estimated active against a measured 208 total), so the
///    write was refused for "total cannot be lower than active" — an invented
///    number contradicting a measured one. This hit every routeless FIT file.
///
///  * SPORT. Type inference joined the sport, the name and the FILE NAME into one
///    string and substring-matched it, testing `run` before `cycling`. So
///    `…Indoor_CyclingiSmoothRun.fit` — a 27 km trainer ride — imported as a RUN,
///    because the exporter's name is in the file name. The FIT sport said
///    cycling, and knew, and was outvoted.
///
/// The GPX guard ("at least 2 timestamped location points") is NOT what these
/// files hit, and it must stay: a GPX carries no session summary, so a routeless
/// one has no start, no duration and no distance, and accepting it would import
/// every corrupt XML as an empty activity. FIT can go routeless precisely because
/// its `session` message says how far and how long.
Future<RouteFileImport> _parse(String fixture) async {
  final bytes = await File('test/fixtures/fit/$fixture').readAsBytes();
  return RouteFileParser.parseFile(bytes, fileName: fixture);
}

/// The chain the Settings importer and the entry form both run.
({ActivityEntryType type, ActivityWriteRequest? request}) _import(
  RouteFileImport parsed,
) {
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
  );
}

void main() {
  test('an indoor run imports, keeping the calories it measured', () async {
    // RunGap, 2.33 km on a treadmill. 208 kcal in the file.
    final parsed = await _parse('indoor_running_rungap.fit');
    expect(parsed.points, isEmpty, reason: 'no GPS: that is the point');
    expect(parsed.totalCaloriesKcal, 208.0);
    expect(parsed.activeCaloriesKcal, isNull);

    final imported = _import(parsed);

    // It used to be null here — refused, because an estimated 226 active sat
    // above the measured 208 total.
    expect(imported.request, isNotNull);
    expect(imported.request!.totalCaloriesKcal, 208.0);
    expect(
      imported.request!.activeCaloriesKcal,
      isNull,
      reason: 'the file did not measure it, so it is not invented',
    );
    expect(imported.request!.distanceMeters, 2334.0);
    expect(imported.request!.routePoints, isEmpty);
  });

  test('an indoor ride imports as a STATIONARY BIKE, not as a run', () async {
    // iSmoothRun, 27.46 km on a trainer. The file name contains "Run".
    final parsed = await _parse('indoor_cycling_ismoothrun.fit');
    expect(parsed.points, isEmpty);

    final imported = _import(parsed);

    expect(
      imported.type.exerciseType,
      ExerciseSessionType.bikingStationary,
      reason: 'the FIT sport says cycling; the file NAME says run, and loses',
    );
    expect(imported.type.supportsGpsRoute, isFalse);
    expect(imported.request, isNotNull);
    expect(imported.request!.exerciseType, ExerciseSessionType.bikingStationary);
    expect(imported.request!.distanceMeters, 27460.0);
    expect(imported.request!.totalCaloriesKcal, 945.0);
    // The heart rate the trainer recorded rides along in the sample buffer — an
    // indoor ride has no route to carry it, but it is not without data.
    expect(imported.request!.bleSamples.heartRateSamples, hasLength(8));
  });

  test('a file that measured NO calories still gets both estimated', () async {
    // The mirror of the calorie fix, and the reason it is "estimate both or
    // estimate neither" rather than "never estimate": a GPX carries no calories
    // at all, and its import must still arrive with a usable pair.
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final clock = ActivityEntryClock.system();
    final state = activityStateWithRouteImport(
      initialActivityEntryState(clock, const {'write-exercise'}),
      RouteFileImport(
        fileName: 'morning_run.gpx',
        points: const [],
        distanceMeters: 5000.0,
        elevationGainedMeters: 0.0,
        startTime: start,
        endTime: start.add(const Duration(minutes: 30)),
        type: 'running',
      ),
      UnitSystem.metric,
      clock,
    );

    expect(state.activeCaloriesText, isNotEmpty);
    expect(state.totalCaloriesText, isNotEmpty);
  });

  test('a generic FIT sport still yields to the name', () async {
    // FIT's `training` and `fitness equipment` are its "I do not know" answers.
    // The sport wins over the file name only when it NAMES something —
    // otherwise `Functional Strength Training.fit` would import as a generic
    // workout, losing what the only informative word in the file was telling us.
    final strength = inferActivityType(
      RouteFileImport(
        fileName: 'Functional Strength Training.fit',
        points: const [],
        distanceMeters: 0,
        elevationGainedMeters: 0,
        startTime: DateTime.utc(2026, 5, 26, 8),
        endTime: DateTime.utc(2026, 5, 26, 9),
        type: 'training',
      ),
      defaultActivityEntryTypes.first,
    );

    expect(strength.exerciseType, ExerciseSessionType.strengthTraining);
  });
}
