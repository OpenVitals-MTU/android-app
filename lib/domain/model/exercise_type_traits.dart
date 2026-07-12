import 'activity_entry_types.dart';

/// What a given kind of exercise can meaningfully be measured in.
///
/// Health Connect exercise types are a flat list of integers, and nothing in the
/// data says a bench press does not cover ground. That has to be knowledge the
/// app carries — and it has to live in the DOMAIN, because both the presentation
/// layer (which metric rows are worth showing) and the domain itself (whether an
/// activity can be cut into distance splits) need to ask the same question and
/// must not answer it differently.

/// Health Connect `EXERCISE_TYPE_*` constants not offered as manual-entry
/// choices, so [ExerciseSessionType] does not carry them. Values are verbatim
/// Health Connect.
const int _bikingStationary = 9;
const int _elliptical = 25;
const int _rowingMachine = 54;
const int _stairClimbing = 68;
const int _stairClimbingMachine = 69;
const int _swimmingPool = 74;
const int _iceSkating = 39;

/// Activities measured in strides: steps and step cadence mean something.
const Set<int> stepBasedExercises = {
  ExerciseSessionType.running,
  ExerciseSessionType.runningTreadmill,
  ExerciseSessionType.walking,
  ExerciseSessionType.hiking,
  ExerciseSessionType.snowshoeing,
  _stairClimbing,
  _stairClimbingMachine,
};

/// Activities with a crank: pedalling cadence means something.
const Set<int> cyclingExercises = {
  ExerciseSessionType.biking,
  _bikingStationary,
};

/// Activities that cover ground, or simulate covering it, so a distance and a
/// speed exist even on a machine.
const Set<int> distanceBasedExercises = {
  ...stepBasedExercises,
  ...cyclingExercises,
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

/// Whether this kind of exercise travels — the question that decides whether a
/// distance means anything for it.
///
/// A strength session does NOT, and that matters beyond tidiness: a phone left on
/// a bench picks up a couple of hundred metres of GPS drift, Health Connect
/// faithfully records it, and the activity screen then cut a lifting session into
/// "1.0 km" and "181 m" splits at a 30:29 min/km pace. The distance was real data;
/// the splits were nonsense. Splits are only meaningful for an activity that
/// actually goes somewhere.
bool isDistanceBasedExercise(int exerciseType) =>
    distanceBasedExercises.contains(exerciseType);
