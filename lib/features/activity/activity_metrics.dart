import 'dart:math' as math;

import '../../domain/model/activity_models.dart';
import '../manualentry/activity/activity_entry_types.dart';

/// Port of the Kotlin `ActivityMetrics.kt` — derived figures a workout does not
/// store, computed the same way everywhere they are shown.
///
/// These lived as private helpers in `activities_notifier.dart` until the detail
/// screen needed them too. One definition, so the "Moving time" a workout reports
/// in the list can never disagree with the one on its own detail page.

/// Summed pause-segment duration of a workout, each coerced >= 0 and the total
/// capped at the workout's own duration (Kotlin `ActivityMetrics.pausedDurationMs`).
int pausedDurationMs(ExerciseData workout) {
  final total = math.max(0, workout.durationMs);
  var paused = 0;
  for (final segment in workout.segments) {
    if (segment.segmentType == ExerciseSegmentType.pause) {
      paused += math.max(0, segment.durationMs);
    }
  }
  return math.min(paused, total);
}

/// Moving (non-paused) duration of a workout in ms
/// (Kotlin `ActivityMetrics.movingDurationMs`).
int movingDurationMs(ExerciseData workout) =>
    math.max(0, math.max(0, workout.durationMs) - pausedDurationMs(workout));
