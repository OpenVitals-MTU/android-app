import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_view_model.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

/// TCX — the format Strava and Garmin export an INDOOR activity as, and the one
/// the app could not read.
///
/// A user reporting "GPX route must contain at least 2 timestamped location
/// points" on an indoor activity was usually holding a TCX. It is XML, so it fell
/// through the dispatcher into the GPX parser, which looked for `trkpt`, found
/// none, and blamed the file — while the file was carrying a complete session:
/// duration, distance, calories, heart rate and cadence, with the `Position`
/// element that a GPX cannot live without simply absent.
///
/// A routeless GPX is still (correctly) refused: it has no session summary to
/// fall back on, so accepting one would import every corrupt XML as an empty
/// activity. TCX is precisely the format that HAS one.
Future<RouteFileImport> _parse(String fixture) async {
  final bytes = await File('test/fixtures/tcx/$fixture').readAsBytes();
  return RouteFileParser.parseFile(bytes, fileName: fixture);
}

ActivityWriteRequest? _writeRequest(RouteFileImport parsed) {
  final clock = ActivityEntryClock.system();
  return buildWriteRequest(
    activityStateWithRouteImport(
      initialActivityEntryState(clock, const {'write-exercise'}),
      parsed,
      UnitSystem.metric,
      clock,
    ),
    UnitSystem.metric,
  );
}

void main() {
  group('an indoor ride: no GPS, and a whole activity anyway', () {
    test('keeps the session the file recorded', () async {
      final parsed = await _parse('indoor_ride.tcx');

      expect(parsed.points, isEmpty, reason: 'no Position: that is the point');
      expect(parsed.distanceMeters, 15000.0);
      expect(parsed.durationSeconds, 1800);
      expect(parsed.totalCaloriesKcal, 420.0);
      // TCX has no active-calorie field, so active stays unknown rather than
      // being invented — an estimate placed beside a measured total is what made
      // every routeless FIT file unsavable.
      expect(parsed.activeCaloriesKcal, isNull);
      expect(parsed.startTime, DateTime.utc(2026, 1, 14, 18, 30));
      expect(parsed.endTime, DateTime.utc(2026, 1, 14, 19, 0));
    });

    test('carries the heart rate, cadence and speed beside it', () async {
      final parsed = await _parse('indoor_ride.tcx');

      expect(
        [for (final s in parsed.bleSamples.heartRateSamples) s.beatsPerMinute],
        [110, 142, 138],
      );
      // A bike's cadence is PEDALLING cadence: a different Health Connect record
      // from a runner's steps, and the sport is what decides which.
      expect(
        [for (final s in parsed.bleSamples.cyclingCadenceSamples) s.rpm],
        [85, 92, 88],
      );
      expect(parsed.bleSamples.stepsCadenceSamples, isEmpty);
      expect(
        [for (final s in parsed.bleSamples.speedSamples) s.metersPerSecond],
        [7.5, 8.9, 8.1],
      );
    });

    test('imports — which is the whole bug', () async {
      final request = _writeRequest(await _parse('indoor_ride.tcx'));

      expect(request, isNotNull);
      expect(request!.distanceMeters, 15000.0);
      expect(request.totalCaloriesKcal, 420.0);
      expect(request.activeCaloriesKcal, isNull);
      expect(request.routePoints, isEmpty);
      expect(request.bleSamples.heartRateSamples, hasLength(3));
    });
  });

  group('an outdoor run: the route still works', () {
    test('reads the track, and the samples along it', () async {
      final parsed = await _parse('outdoor_run.tcx');

      expect(parsed.points, hasLength(3));
      expect(parsed.points.first.latitude, closeTo(52.5, 0.0001));
      expect(parsed.points.first.altitudeMeters, 34.0);
      expect(parsed.distanceMeters, 2000.0);
      expect(parsed.totalCaloriesKcal, 150.0);
      expect(parsed.bleSamples.heartRateSamples, hasLength(3));
      // A runner's TCX cadence counts ONE foot: 82 is 164 steps a minute, and
      // every watch that reads it doubles it back.
      expect(
        [for (final s in parsed.bleSamples.stepsCadenceSamples) s.stepsPerMinute],
        [164, 172, 168],
      );
      expect(parsed.bleSamples.cyclingCadenceSamples, isEmpty);
    });

    test('is a run, and it saves', () async {
      final parsed = await _parse('outdoor_run.tcx');
      final request = _writeRequest(parsed);

      expect(request, isNotNull);
      expect(request!.exerciseType, ExerciseSessionType.running);
      expect(request.routePoints, hasLength(3));
    });
  });

  test('a TCX is recognised by its CONTENT, not its extension', () async {
    // The dispatcher sniffs. A .tcx renamed to .gpx used to die in the GPX
    // parser with a message about location points — the very report that started
    // this.
    final bytes = await File('test/fixtures/tcx/indoor_ride.tcx').readAsBytes();

    final parsed = RouteFileParser.parseFile(bytes, fileName: 'mystery.gpx');

    expect(parsed.distanceMeters, 15000.0);
  });

  test('a routeless GPX is still refused, and must be', () {
    // The guard TCX support must NOT be used as an excuse to loosen. A GPX has
    // no session summary: no start, no duration, no distance. Accepting a
    // trackless one would import every corrupt XML as an empty activity.
    const gpx = '<?xml version="1.0"?><gpx version="1.1"><trk><trkseg>'
        '</trkseg></trk></gpx>';

    expect(
      () => RouteFileParser.parse(gpx, fileName: 'empty.gpx'),
      throwsA(isA<RouteImportException>()),
    );
  });
}
