import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/activity_session_deduplication.dart';

ExerciseData _exercise({
  required String id,
  required String source,
  required String start,
  required String end,
  double? distanceMeters,
  int? heartRateBpm,
  int routePoints = 0,
}) {
  final startTime = DateTime.parse(start);
  final endTime = DateTime.parse(end);
  return ExerciseData(
    id: id,
    title: null,
    exerciseType: 1,
    startTime: startTime,
    endTime: endTime,
    durationMs: endTime.difference(startTime).inMilliseconds,
    source: source,
    totalDistanceMeters: distanceMeters,
    averageHeartRateBpm: heartRateBpm,
    route: ExerciseRouteData(
      status: routePoints > 0
          ? ExerciseRouteStatus.data
          : ExerciseRouteStatus.noData,
      points: List.generate(
        routePoints,
        (index) => ExerciseRoutePoint(
          time: startTime.add(Duration(seconds: index)),
          latitude: 59.0,
          longitude: 24.0,
          altitudeMeters: null,
          horizontalAccuracyMeters: null,
          verticalAccuracyMeters: null,
        ),
      ),
    ),
  );
}

void main() {
  test('deduplicateExerciseSessions keeps richer overlapping same type session',
      () {
    final phone = _exercise(
      id: 'phone',
      source: 'google-fit',
      start: '2026-05-06T06:00:00Z',
      end: '2026-05-06T07:00:00Z',
    );
    final watch = _exercise(
      id: 'watch',
      source: 'garmin',
      start: '2026-05-06T06:01:00Z',
      end: '2026-05-06T07:01:00Z',
      distanceMeters: 10000.0,
      heartRateBpm: 154,
      routePoints: 3,
    );

    final result = deduplicateExerciseSessions([phone, watch]);

    expect(result.map((session) => session.id).toList(), ['watch']);
  });

  test('deduplicateExerciseSessions keeps separate non overlapping sessions',
      () {
    final morning = _exercise(
      id: 'morning',
      source: 'google-fit',
      start: '2026-05-06T06:00:00Z',
      end: '2026-05-06T07:00:00Z',
    );
    final evening = _exercise(
      id: 'evening',
      source: 'garmin',
      start: '2026-05-06T18:00:00Z',
      end: '2026-05-06T19:00:00Z',
    );

    final result = deduplicateExerciseSessions([morning, evening]);

    expect(result.map((session) => session.id).toList(), ['evening', 'morning']);
  });
}
