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
  DateTime? lastModifiedTime,
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
    lastModifiedTime: lastModifiedTime,
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
  _lastModifiedTieBreakTests();

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

/// The last tie-break between two duplicates that are equally rich and equally
/// long: keep whichever was edited most recently.
///
/// It never fired. `lastModifiedTime` was declared on the model, read here, and
/// populated NOWHERE — the Pigeon message the record crosses on did not carry
/// it, so every session's was null, both sides of the comparison collapsed to
/// the epoch, and the tie resolved to whatever happened to come first in the
/// list.
void _lastModifiedTieBreakTests() {
  test('the most recently edited of two identical duplicates wins', () {
    final older = _exercise(
      id: 'older',
      source: 'com.watch',
      start: '2026-07-12T09:00:00Z',
      end: '2026-07-12T09:30:00Z',
      lastModifiedTime: DateTime.utc(2026, 7, 12, 10),
    );
    final newer = _exercise(
      id: 'newer',
      source: 'com.watch',
      start: '2026-07-12T09:00:00Z',
      end: '2026-07-12T09:30:00Z',
      lastModifiedTime: DateTime.utc(2026, 7, 12, 11),
    );

    // Same richness, same duration -- only lastModifiedTime can separate them,
    // and it must, whichever order they arrive in.
    expect(
      deduplicateExerciseSessions([older, newer]).single.id,
      'newer',
    );
    expect(
      deduplicateExerciseSessions([newer, older]).single.id,
      'newer',
    );
  });
}
