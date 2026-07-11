part of 'apple_health_import_converter.dart';

const int _minWorkoutRoutePoints = 2;

/// Workout → exercise-session + optional distance/energy records with a
/// synthesized GPS route, ported from the Kotlin
/// `AppleHealthImportWorkoutConversions.kt`.
extension AppleHealthImportWorkoutConversions on AppleHealthImportConverter {
  List<ConvertedAppleRecord> convertWorkouts(
    List<AppleWorkout> workouts,
    List<AppleWorkoutOverlapCandidate> overlapCandidates,
    bool overlapCandidateLimitReached,
  ) {
    final result = <ConvertedAppleRecord>[];
    for (final workout in workouts) {
      final start = workout.startDate;
      if (start == null) {
        invalid(workout.workoutActivityType, 'Workout is missing startDate.', null);
        continue;
      }
      final end = workout.endDate;
      if (end == null) {
        invalid(
          workout.workoutActivityType,
          'Workout is missing endDate.',
          start.instant.toIso8601String(),
        );
        continue;
      }
      final iv = interval(start, end);
      final unavailableRoutePaths = workout.unavailableRoutePaths;
      if (reportUnavailableWorkoutRoutes && unavailableRoutePaths.isNotEmpty) {
        skipped(
          workout.workoutActivityType,
          'workout_route_unavailable',
          'Workout route file(s) unavailable: ${unavailableRoutePaths.join(', ')}. '
              'Source: ${workout.sourceName ?? 'unknown'}. '
              'Import or recreate this activity manually if route geometry is needed.',
          AppleImportTimeRange(iv.start.instant, iv.end.instant).toString(),
        );
      }
      final fingerprint =
          buildStableClientRecordId('workout', workout.stableParts());
      final exerciseRoute = _synthesizeExerciseRoute(workout, iv);
      if (workout.routes.isNotEmpty && exerciseRoute == null) {
        skipped(
          workout.workoutActivityType,
          'route_skipped',
          'Workout route file did not contain enough valid geometry points.',
          AppleImportTimeRange(iv.start.instant, iv.end.instant).toString(),
        );
      }
      final title = () {
        final stripped =
            workout.workoutActivityType.replaceFirst('HKWorkoutActivityType', '');
        return stripped.isEmpty ? 'Apple Health workout' : stripped;
      }();
      result.add(ConvertedAppleRecord(
        appleType: workout.workoutActivityType,
        targetType: 'ExerciseSessionRecord',
        fingerprint: fingerprint,
        record: ExerciseSessionImportRecord(
          clientRecordId: fingerprint,
          startTime: iv.start.instant,
          startZoneOffset: iv.start.offset,
          endTime: iv.end.instant,
          endZoneOffset: iv.end.offset,
          exerciseType:
              mapWorkoutActivityTypeToExerciseType(workout.workoutActivityType),
          title: title,
          route: exerciseRoute,
        ),
        sourceTimeRange: AppleImportTimeRange(iv.start.instant, iv.end.instant),
        unit: workout.durationUnit,
        value: workout.duration?.toString(),
      ));
      markConverted(workout.workoutActivityType);

      final totalDistance = workout.totalDistance;
      if (!overlapCandidateLimitReached &&
          !hasOverlapping(overlapCandidates, workout, appleDistanceTypes) &&
          totalDistance != null) {
        final meters = toMeters(totalDistance, workout.totalDistanceUnit);
        if (meters != null && meters > 0.0) {
          final distanceFingerprint = buildStableClientRecordId(
            'workout_distance',
            '${workout.stableParts()}|distance',
          );
          result.add(ConvertedAppleRecord(
            appleType: workout.workoutActivityType,
            targetType: 'DistanceRecord',
            fingerprint: distanceFingerprint,
            record: DistanceImportRecord(
              clientRecordId: distanceFingerprint,
              startTime: iv.start.instant,
              startZoneOffset: iv.start.offset,
              endTime: iv.end.instant,
              endZoneOffset: iv.end.offset,
              meters: meters,
            ),
            sourceTimeRange:
                AppleImportTimeRange(iv.start.instant, iv.end.instant),
            unit: workout.totalDistanceUnit,
            value: totalDistance.toString(),
          ));
          markConverted(workout.workoutActivityType);
        }
      }

      final totalEnergyBurned = workout.totalEnergyBurned;
      if (!overlapCandidateLimitReached &&
          !hasOverlapping(overlapCandidates, workout, {appleActiveEnergyBurned}) &&
          totalEnergyBurned != null) {
        final kcal =
            toKilocalories(totalEnergyBurned, workout.totalEnergyBurnedUnit);
        if (kcal != null && kcal > 0.0) {
          final energyFingerprint = buildStableClientRecordId(
            'workout_active_calories',
            '${workout.stableParts()}|energy',
          );
          result.add(ConvertedAppleRecord(
            appleType: workout.workoutActivityType,
            targetType: 'ActiveCaloriesBurnedRecord',
            fingerprint: energyFingerprint,
            record: ActiveCaloriesBurnedImportRecord(
              clientRecordId: energyFingerprint,
              startTime: iv.start.instant,
              startZoneOffset: iv.start.offset,
              endTime: iv.end.instant,
              endZoneOffset: iv.end.offset,
              kilocalories: kcal,
            ),
            sourceTimeRange:
                AppleImportTimeRange(iv.start.instant, iv.end.instant),
            unit: workout.totalEnergyBurnedUnit,
            value: totalEnergyBurned.toString(),
          ));
          markConverted(workout.workoutActivityType);
        }
      }
    }
    return result;
  }

  ExerciseRoute? _synthesizeExerciseRoute(
    AppleWorkout workout,
    AppleInterval iv,
  ) {
    // Routes are already deduplicated by path at parse time.
    final points = workout.routes
        .expand((it) => it.points)
        .where((it) =>
            it.latitude >= -90.0 &&
            it.latitude <= 90.0 &&
            it.longitude >= -180.0 &&
            it.longitude <= 180.0)
        .toList();
    if (points.length < _minWorkoutRoutePoints) return null;

    final cumulativeDistances = _runningRouteDistances(points);
    final totalDistanceMeters =
        cumulativeDistances.isEmpty ? 0.0 : cumulativeDistances.last;
    final rawDurationMillis =
        iv.end.instant.difference(iv.start.instant).inMilliseconds;
    final durationMillis =
        math.max(rawDurationMillis, points.length + 1);
    final lastIndex = points.length - 1;
    final lastOffsetMillis = math.max(durationMillis - 1, lastIndex);
    var previousOffsetMillis = -1;

    final locations = <ExerciseRouteLocation>[];
    for (var index = 0; index < points.length; index++) {
      final point = points[index];
      final progress = totalDistanceMeters > 0.0
          ? cumulativeDistances[index] / totalDistanceMeters
          : index / lastIndex;
      final requestedOffset = (lastOffsetMillis * progress).round();
      final minOffset = previousOffsetMillis + 1;
      final maxOffset = lastOffsetMillis - (lastIndex - index);
      final offset = requestedOffset.clamp(minOffset, maxOffset).toInt();
      previousOffsetMillis = offset;
      locations.add(ExerciseRouteLocation(
        time: iv.start.instant.add(Duration(milliseconds: offset)),
        latitude: point.latitude,
        longitude: point.longitude,
        horizontalAccuracyMeters: point.horizontalAccuracyMeters,
        verticalAccuracyMeters: point.verticalAccuracyMeters,
        altitudeMeters: point.altitudeMeters,
      ));
    }
    return ExerciseRoute(locations);
  }

  List<double> _runningRouteDistances(List<AppleWorkoutRoutePoint> points) {
    var distance = 0.0;
    final result = <double>[];
    for (var index = 0; index < points.length; index++) {
      if (index > 0) {
        final previous = points[index - 1];
        final point = points[index];
        distance += haversineMeters(
          previous.latitude,
          previous.longitude,
          point.latitude,
          point.longitude,
        );
      }
      result.add(distance);
    }
    return result;
  }
}
