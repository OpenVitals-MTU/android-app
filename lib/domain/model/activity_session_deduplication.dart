import 'dart:math' as math;

import 'activity_models.dart';

const double _duplicateOverlapRatio = 0.85;
const int _duplicateBoundaryToleranceMs = 15 * 60 * 1000;

final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

List<ExerciseData> deduplicateExerciseSessions(List<ExerciseData> sessions) {
  if (sessions.length < 2) {
    return [...sessions]..sort((a, b) => b.endTime.compareTo(a.endTime));
  }

  final kept = <ExerciseData>[];
  final sorted = [...sessions]..sort((a, b) {
      final byStart = a.startTime.compareTo(b.startTime);
      if (byStart != 0) return byStart;
      return a.endTime.compareTo(b.endTime);
    });
  for (final session in sorted) {
    final duplicateIndex =
        kept.indexWhere((existing) => _isDuplicateOf(existing, session));
    if (duplicateIndex == -1) {
      kept.add(session);
    } else {
      kept[duplicateIndex] =
          _richerExerciseSession(kept[duplicateIndex], session);
    }
  }

  return kept..sort((a, b) => b.endTime.compareTo(a.endTime));
}

bool _isDuplicateOf(ExerciseData session, ExerciseData other) {
  if (session.exerciseType != other.exerciseType) return false;

  final shorterDuration =
      math.min(math.max(session.durationMs, 0), math.max(other.durationMs, 0));
  if (shorterDuration <= 0) return false;

  final overlapMs = math.min(
        session.endTime.millisecondsSinceEpoch,
        other.endTime.millisecondsSinceEpoch,
      ) -
      math.max(
        session.startTime.millisecondsSinceEpoch,
        other.startTime.millisecondsSinceEpoch,
      );
  if (overlapMs <= 0) return false;

  final startDiff = (session.startTime.millisecondsSinceEpoch -
          other.startTime.millisecondsSinceEpoch)
      .abs();
  final endDiff =
      (session.endTime.millisecondsSinceEpoch - other.endTime.millisecondsSinceEpoch)
          .abs();
  return overlapMs / shorterDuration >= _duplicateOverlapRatio &&
      startDiff <= _duplicateBoundaryToleranceMs &&
      endDiff <= _duplicateBoundaryToleranceMs;
}

ExerciseData _richerExerciseSession(ExerciseData first, ExerciseData second) =>
    _compareRichness(first, second) >= 0 ? first : second;

int _compareRichness(ExerciseData a, ExerciseData b) {
  final byScore = _richnessScore(a).compareTo(_richnessScore(b));
  if (byScore != 0) return byScore;
  final byDuration = a.durationMs.compareTo(b.durationMs);
  if (byDuration != 0) return byDuration;
  return (a.lastModifiedTime ?? _epoch).compareTo(b.lastModifiedTime ?? _epoch);
}

int _richnessScore(ExerciseData session) {
  final metrics = <Object?>[
    session.totalDistanceMeters,
    session.totalCaloriesKcal,
    session.activeCaloriesKcal,
    session.steps,
    session.wheelchairPushes,
    session.averageSpeedMetersPerSecond,
    session.averagePowerWatts,
    session.averageStepsCadenceRate,
    session.averageCyclingCadenceRpm,
    session.averageHeartRateBpm,
    session.floorsClimbed,
    session.elevationGainedMeters,
  ].where((value) => value != null).length;

  return (session.isOpenVitalsEntry ? 1000 : 0) +
      (session.route.status == ExerciseRouteStatus.data ? 200 : 0) +
      math.min<int>(session.route.points.length, 500) +
      metrics * 20 +
      math.min<int>(session.segments.length, 20) * 5 +
      math.min<int>(session.laps.length, 20) * 5 +
      (session.device != null ? 10 : 0) +
      (_isNotBlank(session.title) ? 5 : 0) +
      (_isNotBlank(session.notes) ? 5 : 0);
}

bool _isNotBlank(String? value) => value != null && value.trim().isNotEmpty;
