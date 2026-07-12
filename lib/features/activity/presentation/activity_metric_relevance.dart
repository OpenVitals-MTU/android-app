import '../../../domain/model/activity_entry_types.dart';

/// Which metric rows are worth showing on the activity detail screen.
///
/// The Kotlin app rendered a fixed row list for every activity and fell back to
/// "Not available" whenever a field was null, so a bike ride advertised Steps,
/// Floors climbed and Wheelchair pushes -- all of them absent, none of them ever
/// going to be present. This picks the rows instead, on ONE rule:
///
///   Show a row if it has a value, OR if the absence is worth reporting for this
///   kind of activity.
///
/// The two halves matter equally. "Has a value" comes first, so recorded data is
/// NEVER hidden -- if a device somehow reports steps for a bike ride, the row
/// appears, and no relevance table can suppress it. Relevance only decides which
/// ABSENCES are informative: "Distance: Not available" tells a cyclist something
/// real (the GPS did not record), while "Wheelchair pushes: Not available" tells
/// them nothing at all.
///
/// An exercise type this table has never seen -- a new Health Connect constant, an
/// import from another app -- needs no special case. It matches none of the sets
/// below, so it reports only the absences that are universal (duration, heart
/// rate, energy) and still shows every metric it actually recorded. The card can
/// never come out empty, because a session always has a duration.
enum ActivityMetric {
  duration,
  movingTime,
  steps,
  stepsCadence,
  distance,
  averagePace,
  averageSpeed,
  recordedSpeed,
  cyclingCadence,
  averagePower,
  averageHeartRate,
  caloriesBurned,
  activeCalories,
  wheelchairPushes,
  floorsClimbed,
  elevationGained,
}

/// Health Connect `EXERCISE_TYPE_*` constants this table needs that the
/// manual-entry catalogue ([ExerciseSessionType]) does not carry, because they
/// are not offered as manual-entry choices. Values are verbatim Health Connect.
const int _bikingStationary = 9;
const int _elliptical = 25;
const int _rowingMachine = 54;
const int _stairClimbing = 68;
const int _stairClimbingMachine = 69;
const int _swimmingPool = 74;
const int _iceSkating = 39;

/// Activities measured in strides: steps and step cadence mean something.
const Set<int> _stepBased = {
  ExerciseSessionType.running,
  ExerciseSessionType.runningTreadmill,
  ExerciseSessionType.walking,
  ExerciseSessionType.hiking,
  ExerciseSessionType.snowshoeing,
  _stairClimbing,
  _stairClimbingMachine,
};

/// Activities with a crank: pedalling cadence means something.
const Set<int> _cycling = {ExerciseSessionType.biking, _bikingStationary};

/// Activities that cover ground (or simulate covering it), so a distance and a
/// speed exist even on a machine.
const Set<int> _distanceBased = {
  ..._stepBased,
  ..._cycling,
  ExerciseSessionType.rowing,
  _rowingMachine,
  _elliptical,
  ExerciseSessionType.swimmingOpenWater,
  _swimmingPool,
  ExerciseSessionType.skating,
  _iceSkating,
  ExerciseSessionType.skiing,
  ExerciseSessionType.snowboarding,
  ExerciseSessionType.paddling,
  ExerciseSessionType.surfing,
  ExerciseSessionType.sailing,
  ExerciseSessionType.wheelchair,
};

/// Pace reads better than speed for these; speed reads better for the rest.
/// Mirrors `_prefersPace` in `activities_ordered_sections.dart`.
const Set<int> _prefersPace = {
  ExerciseSessionType.running,
  ExerciseSessionType.runningTreadmill,
  ExerciseSessionType.walking,
  ExerciseSessionType.hiking,
  ExerciseSessionType.snowshoeing,
};

/// Indoor and machine-bound activities: the ground never rises, so a missing
/// elevation gain is not news.
const Set<int> _indoor = {
  ExerciseSessionType.runningTreadmill,
  _bikingStationary,
  _rowingMachine,
  _stairClimbingMachine,
  _elliptical,
  _swimmingPool,
};

/// Whether the ABSENCE of [metric] is worth reporting for [exerciseType].
///
/// Callers must still show the row whenever it has a value -- see the rule in
/// the library doc above. This answers only the second half of it.
bool isMetricRelevant(
  ActivityMetric metric,
  int exerciseType,
) => switch (metric) {
  // Every session has a duration, a heart rate worth looking for, and burns
  // energy -- there is no activity for which these are meaningless.
  ActivityMetric.duration ||
  ActivityMetric.movingTime ||
  ActivityMetric.averageHeartRate ||
  ActivityMetric.caloriesBurned ||
  ActivityMetric.activeCalories => true,
  ActivityMetric.steps => _stepBased.contains(exerciseType),
  ActivityMetric.cyclingCadence => _cycling.contains(exerciseType),
  ActivityMetric.distance => _distanceBased.contains(exerciseType),
  ActivityMetric.averagePace => _prefersPace.contains(exerciseType),
  ActivityMetric.averageSpeed => _distanceBased.contains(exerciseType),
  ActivityMetric.wheelchairPushes =>
    exerciseType == ExerciseSessionType.wheelchair,
  ActivityMetric.floorsClimbed =>
    exerciseType == _stairClimbing || exerciseType == _stairClimbingMachine,
  ActivityMetric.elevationGained =>
    _distanceBased.contains(exerciseType) && !_indoor.contains(exerciseType),

  // Everything below needs hardware most people do not own -- a power meter, a
  // footpod, a bike computer reporting its own average. Their absence is the
  // normal case and says nothing about the activity, so they earn a row only
  // by actually being recorded. Announcing "Average power: Not available" on
  // every ride would just be the old noise wearing a better label.
  ActivityMetric.averagePower ||
  ActivityMetric.stepsCadence ||
  ActivityMetric.recordedSpeed => false,
};
