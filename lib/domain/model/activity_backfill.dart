import '../../core/geo/geo_distance.dart';
import '../insights/activity_splits.dart';
import 'activity_models.dart';
import 'exercise_session_metrics.dart';
import 'heart_models.dart';
import '../../core/stats/stats.dart';

const double _minBackfillDistanceMeters = 1.0;
const double _minBackfillElevationMeters = 1.0;
const double _minBackfillElevationDeltaMeters = 1.0;

extension ActivityBackfill on ExerciseData {
  ExerciseData withRouteBackfilledMetrics() {
    final points = route.status == ExerciseRouteStatus.data
        ? route.points
        : const <ExerciseRoutePoint>[];
    final metrics = _routeBackfillMetrics(points);

    return copyWith(
      totalDistanceMeters: _backfilledByDouble(
        totalDistanceMeters,
        metrics.distanceMeters >= _minBackfillDistanceMeters
            ? metrics.distanceMeters
            : null,
      ),
      elevationGainedMeters: _backfilledByDouble(
        elevationGainedMeters,
        (metrics.hasAltitudeData &&
                metrics.elevationGainMeters >= _minBackfillElevationMeters)
            ? metrics.elevationGainMeters
            : null,
      ),
    );
  }

  /// Fill in the totals a session record does not carry, read from the sibling
  /// records covering its window (see [ExerciseSessionMetrics]).
  ///
  /// Health Connect is the authority here, so this runs BEFORE the sample-derived
  /// backfill: a real DistanceRecord beats a distance integrated from speed. Only
  /// missing fields are touched -- a figure the session already stated is never
  /// overwritten.
  ExerciseData withSessionMetricsBackfilled(ExerciseSessionMetrics metrics) =>
      copyWith(
        totalDistanceMeters: _backfilledByDouble(
          totalDistanceMeters,
          metrics.totalDistanceMeters,
        ),
        averageSpeedMetersPerSecond: _backfilledByDouble(
          averageSpeedMetersPerSecond,
          metrics.averageSpeedMetersPerSecond,
        ),
        steps: _backfilledByInt(steps, metrics.steps),
        totalCaloriesKcal: _backfilledByDouble(
          totalCaloriesKcal,
          metrics.totalCaloriesKcal,
        ),
        activeCaloriesKcal: _backfilledByDouble(
          activeCaloriesKcal,
          metrics.activeCaloriesKcal,
        ),
        elevationGainedMeters: _backfilledByDouble(
          elevationGainedMeters,
          metrics.elevationGainedMeters,
        ),
        floorsClimbed: _backfilledByInt(floorsClimbed, metrics.floorsClimbed),
        wheelchairPushes: _backfilledByInt(
          wheelchairPushes,
          metrics.wheelchairPushes,
        ),
      );

  ExerciseData withSampleBackfilledMetrics({
    required List<HeartRateSample> heartRateSamples,
    required List<SpeedSample> speedSamples,
    required List<ActivityCadenceSample> cadenceSamples,
  }) {
    final heartRateAverage = average(
      heartRateSamples
          .map((sample) => sample.beatsPerMinute)
          .where((bpm) => bpm > 0)
          .toList(),
    );
    return copyWith(
      // A watch that records speed but writes no DistanceRecord leaves the session
      // with no distance at all -- while the splits card, integrating these very
      // samples, cheerfully reports "every 1 km". Integrate them here too, so the
      // two can never disagree.
      totalDistanceMeters: _backfilledByDouble(
        totalDistanceMeters,
        distanceFromSpeedSamples(speedSamples),
      ),
      averageHeartRateBpm: _backfilledByInt(
        averageHeartRateBpm,
        heartRateAverage?.round(),
      ),
      averageSpeedMetersPerSecond: _backfilledByDouble(
        averageSpeedMetersPerSecond,
        average(
          speedSamples
              .map((sample) => sample.metersPerSecond)
              .where((speed) => speed > 0.0 && speed.isFinite)
              .toList(),
        ),
      ),
      averageStepsCadenceRate: _backfilledByDouble(
        averageStepsCadenceRate,
        average(
          cadenceSamples
              .where((sample) => sample.kind == ActivityCadenceKind.steps)
              .map((sample) => sample.rate)
              .where((rate) => rate > 0.0 && rate.isFinite)
              .toList(),
        ),
      ),
      averageCyclingCadenceRpm: _backfilledByDouble(
        averageCyclingCadenceRpm,
        average(
          cadenceSamples
              .where((sample) => sample.kind == ActivityCadenceKind.cycling)
              .map((sample) => sample.rate)
              .where((rate) => rate > 0.0 && rate.isFinite)
              .toList(),
        ),
      ),
    );
  }
}

class _RouteBackfillMetrics {
  const _RouteBackfillMetrics({
    this.distanceMeters = 0.0,
    this.elevationGainMeters = 0.0,
    this.altitudePairCount = 0,
  });

  final double distanceMeters;
  final double elevationGainMeters;
  final int altitudePairCount;

  bool get hasAltitudeData => altitudePairCount > 0;
}

_RouteBackfillMetrics _routeBackfillMetrics(List<ExerciseRoutePoint> points) {
  if (points.length < 2) return const _RouteBackfillMetrics();

  var distanceMeters = 0.0;
  var elevationGainMeters = 0.0;
  var altitudePairCount = 0;

  final sorted = [...points]..sort((a, b) => a.time.compareTo(b.time));
  for (var index = 0; index < sorted.length - 1; index++) {
    final start = sorted[index];
    final end = sorted[index + 1];
    distanceMeters += _distanceMetersTo(start, end);

    final startAltitude = start.altitudeMeters;
    final endAltitude = end.altitudeMeters;
    if (startAltitude != null && endAltitude != null) {
      altitudePairCount += 1;
      final gain = endAltitude - startAltitude;
      if (gain >= _minBackfillElevationDeltaMeters) {
        elevationGainMeters += gain;
      }
    }
  }

  return _RouteBackfillMetrics(
    distanceMeters: distanceMeters.isFinite ? distanceMeters : 0.0,
    elevationGainMeters: elevationGainMeters.isFinite ? elevationGainMeters : 0.0,
    altitudePairCount: altitudePairCount,
  );
}

double _distanceMetersTo(ExerciseRoutePoint point, ExerciseRoutePoint other) =>
    haversineMeters(
      point.latitude,
      point.longitude,
      other.latitude,
      other.longitude,
    );

double? _backfilledByDouble(double? current, double? value) {
  final isMissing = current == null || current <= 0.0 || !current.isFinite;
  if (isMissing && value != null && value > 0.0 && value.isFinite) {
    return value;
  }
  return current;
}

int? _backfilledByInt(int? current, int? value) {
  if ((current == null || current <= 0) && value != null && value > 0) {
    return value;
  }
  return current;
}

