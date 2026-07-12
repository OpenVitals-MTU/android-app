import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';

/// Captures the include-flags `loadWorkoutsWithMetrics` forwards, so the tests
/// can assert the per-metric permission gating without a real provider.
class _CaptureDataSource extends HealthDataSource {
  _CaptureDataSource(this._granted);

  final Set<String> _granted;
  int withMetricsCalls = 0;
  bool? capturedIncludeDistance;
  bool? capturedIncludeSpeed;
  DateTime? capturedStart;
  DateTime? capturedEnd;

  @override
  Future<Set<String>> grantedPermissions() async => _granted;

  @override
  Future<List<ExerciseData>> readExerciseSessionsWithMetrics(
    DateTime start,
    DateTime end, {
    bool includeDistance = false,
    bool includeSpeed = false,
  }) async {
    withMetricsCalls++;
    capturedStart = start;
    capturedEnd = end;
    capturedIncludeDistance = includeDistance;
    capturedIncludeSpeed = includeSpeed;
    return [
      ExerciseData(
        id: 'ex-1',
        title: 'Morning run',
        exerciseType: 56,
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'provider',
        totalDistanceMeters: includeDistance ? 5000 : null,
        averageSpeedMetersPerSecond: includeSpeed ? 3.2 : null,
      ),
    ];
  }
}

_CaptureDataSource _source(Set<String> granted) => _CaptureDataSource(granted)
  ..cachedAvailability = HealthConnectAvailability.available;

void main() {
  final start = LocalDate(2026, 7, 1);
  final end = LocalDate(2026, 7, 7);

  test('forwards both metrics when distance and speed are granted', () async {
    final ds = _source({
      HcPermissions.readExercise,
      HcPermissions.readDistance,
      HcPermissions.readSpeed,
    });

    final workouts =
        (await ActivityRepositoryImpl(ds).loadWorkoutsWithMetrics(start, end))
            .orThrow();

    expect(ds.capturedIncludeDistance, isTrue);
    expect(ds.capturedIncludeSpeed, isTrue);
    expect(workouts.single.totalDistanceMeters, 5000);
    expect(workouts.single.averageSpeedMetersPerSecond, 3.2);
  });

  test('degrades to null metrics when distance/speed are not granted', () async {
    final ds = _source({HcPermissions.readExercise});

    final workouts =
        (await ActivityRepositoryImpl(ds).loadWorkoutsWithMetrics(start, end))
            .orThrow();

    // The read still happens (the sessions themselves are readable) — only the
    // aggregate metrics are dropped. It must not throw.
    expect(ds.withMetricsCalls, 1);
    expect(ds.capturedIncludeDistance, isFalse);
    expect(ds.capturedIncludeSpeed, isFalse);
    expect(workouts.single.totalDistanceMeters, isNull);
    expect(workouts.single.averageSpeedMetersPerSecond, isNull);
  });

  test('gates distance and speed independently', () async {
    final ds = _source({HcPermissions.readExercise, HcPermissions.readDistance});

    await ActivityRepositoryImpl(ds).loadWorkoutsWithMetrics(start, end);

    expect(ds.capturedIncludeDistance, isTrue);
    expect(ds.capturedIncludeSpeed, isFalse);
  });

  test('skips the read entirely without the exercise permission', () async {
    final ds = _source({HcPermissions.readDistance, HcPermissions.readSpeed});

    final workouts =
        (await ActivityRepositoryImpl(ds).loadWorkoutsWithMetrics(start, end))
            .orThrow();

    expect(workouts, isEmpty);
    expect(ds.withMetricsCalls, 0);
  });

  test('reads the local-day span of the requested range', () async {
    final ds = _source({HcPermissions.readExercise});

    await ActivityRepositoryImpl(ds).loadWorkoutsWithMetrics(start, end);

    expect(ds.capturedStart, DateTime(2026, 7, 1));
    // Inclusive end day: through the start of the following day.
    expect(ds.capturedEnd, DateTime(2026, 7, 8));
  });
}
