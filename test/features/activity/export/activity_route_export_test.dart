import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/activity/export/activity_route_export.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';

/// Port of the Kotlin `ActivityRouteExportTest`, round-tripping the export
/// bytes through the route-import parser exactly as the Kotlin test used
/// `RouteFileParser.parseFile`.
void main() {
  test('kmz export writes parseable route with metadata', () {
    final points = [
      _routePoint('2026-05-26T08:30:00Z', latitude: 59.0000, longitude: 24.0000, altitude: 10.0),
      _routePoint('2026-05-26T08:31:00Z', latitude: 59.0010, longitude: 24.0020, altitude: 18.0),
    ];
    final workout = _workout(
      title: 'Morning run',
      notes: 'Easy commute',
      points: points,
    );

    final bytes = buildActivityRouteKmz(workout, points);

    final parsed = RouteFileParser.parseFile(bytes, fileName: 'morning-run.kmz');
    expect(parsed.name, 'Morning run');
    expect(parsed.description, 'Easy commute');
    expect(parsed.points, hasLength(points.length));
    expect(parsed.startTime, points.first.time);
    expect(parsed.endTime, points.last.time);
    expect(parsed.elevationGainedMeters, closeTo(8.0, 0.001));
  });

  test('gpx export writes parseable route with metadata', () {
    final points = [
      _routePoint('2026-05-26T08:30:00Z', latitude: 59.0000, longitude: 24.0000, altitude: 10.0),
      _routePoint('2026-05-26T08:31:30Z', latitude: 59.0010, longitude: 24.0020, altitude: 18.0),
    ];
    final workout = _workout(
      title: 'Morning run',
      notes: 'Easy commute',
      points: points,
    );

    final gpx = buildActivityRouteGpx(workout, points);

    expect(gpx, contains('creator="OpenVitals"'));
    // Kotlin Instant.toString drops the zero fraction — no `.000Z` anywhere.
    expect(gpx, isNot(contains('.000Z')));
    expect(gpx, contains('<time>2026-05-26T08:30:00Z</time>'));
    final parsed = RouteFileParser.parse(gpx, fileName: 'morning-run.gpx');
    expect(parsed.name, 'Morning run');
    expect(parsed.description, 'Easy commute');
    expect(parsed.points, hasLength(points.length));
    expect(parsed.startTime, points.first.time);
    expect(parsed.endTime, points.last.time);
  });

  test('kmz escapes markup in title and notes', () {
    final points = [
      _routePoint('2026-05-26T08:30:00Z', latitude: 59.0, longitude: 24.0, altitude: 1.0),
      _routePoint('2026-05-26T08:31:00Z', latitude: 59.001, longitude: 24.002, altitude: 2.0),
    ];
    final workout = _workout(
      title: 'Run <with> "friends" & family',
      notes: "it's <fine>",
      points: points,
    );

    final parsed = RouteFileParser.parseFile(
      buildActivityRouteKmz(workout, points),
      fileName: 'run.kmz',
    );
    expect(parsed.name, 'Run <with> "friends" & family');
    expect(parsed.description, "it's <fine>");
  });

  test('route export file names use selected format extension', () {
    final workout = _workout(title: 'Morning Run!', notes: null, points: const []);

    final gpxName =
        activityRouteExportFileName(workout, ActivityRouteExportFormat.gpx);
    final kmzName =
        activityRouteExportFileName(workout, ActivityRouteExportFormat.kmz);

    expect(gpxName, startsWith('morning-run-'));
    expect(gpxName, endsWith('.gpx'));
    expect(kmzName, startsWith('morning-run-'));
    expect(kmzName, endsWith('.kmz'));
  });

  test('blank title falls back to activity-route', () {
    final workout = _workout(title: '  !!! ', notes: null, points: const []);
    expect(
      activityRouteExportFileName(workout, ActivityRouteExportFormat.gpx),
      startsWith('activity-route-'),
    );
  });

  test('sorted points require a non-empty route', () {
    final workout = _workout(title: null, notes: null, points: const []);
    expect(() => sortedRoutePointsForExport(workout), throwsStateError);
  });

  test('sorted points order by time', () {
    final late = _routePoint('2026-05-26T08:31:00Z', latitude: 1, longitude: 1, altitude: null);
    final early = _routePoint('2026-05-26T08:30:00Z', latitude: 0, longitude: 0, altitude: null);
    final workout = _workout(title: null, notes: null, points: [late, early]);
    expect(sortedRoutePointsForExport(workout), [early, late]);
  });
}

ExerciseData _workout({
  required String? title,
  required String? notes,
  required List<ExerciseRoutePoint> points,
}) =>
    ExerciseData(
      id: 'activity-1',
      title: title,
      exerciseType: 56,
      startTime: DateTime.parse('2026-05-26T08:30:00Z'),
      endTime: DateTime.parse('2026-05-26T09:30:00Z'),
      durationMs: 3600000,
      source: 'test',
      notes: notes,
      route: ExerciseRouteData(
        status: ExerciseRouteStatus.data,
        points: points,
      ),
    );

ExerciseRoutePoint _routePoint(
  String time, {
  required double latitude,
  required double longitude,
  required double? altitude,
}) =>
    ExerciseRoutePoint(
      time: DateTime.parse(time),
      latitude: latitude,
      longitude: longitude,
      altitudeMeters: altitude,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );
