/// Port of the Kotlin `ActivityEntryTypes.kt` (activity type catalogue) plus the
/// Health Connect `ExerciseSessionRecord` / `ExerciseSegment` /
/// `PlannedExerciseStep` constants the manual-entry + recording feature depends
/// on.
///
/// `exerciseType` is the verbatim Health Connect
/// `ExerciseSessionRecord.EXERCISE_TYPE_*` integer constant — the same value the
/// native bridge round-trips through the record JSON (see
/// `health_record_json.dart` and `exercise_labels.dart`), matching the Kotlin
/// source. Exercise-segment types and planned-step phases likewise use verbatim
/// Health Connect integer constants.
class ExerciseSessionType {
  const ExerciseSessionType._();

  static const int running = 56;
  static const int biking = 8;
  static const int bikingStationary = 9;
  static const int walking = 79;
  static const int hiking = 37;
  static const int wheelchair = 82;
  static const int rowing = 53;
  static const int paddling = 46;
  static const int skiing = 61;
  static const int snowboarding = 62;
  static const int snowshoeing = 63;
  static const int skating = 60;
  static const int sailing = 58;
  static const int surfing = 72;
  static const int swimmingOpenWater = 73;
  static const int golf = 32;
  static const int strengthTraining = 70;
  static const int runningTreadmill = 57;
  static const int calisthenics = 13;
  static const int gymnastics = 34;
  static const int otherWorkout = 0;
}

/// Verbatim Health Connect `ExerciseSegment.EXERCISE_SEGMENT_TYPE_*` constants.
class ExerciseSegmentType {
  const ExerciseSegmentType._();

  static const int unknown = 0;
  static const int jumpRope = 28;
  static const int otherWorkout = 38;
  static const int pause = 39;
  static const int pullUp = 42;
  static const int rest = 44;
  static const int runningTreadmill = 47;
}

/// Verbatim Health Connect `PlannedExerciseStep.EXERCISE_PHASE_*` constants.
class PlannedExerciseStepPhase {
  const PlannedExerciseStepPhase._();

  static const int unknown = 0;
  static const int warmup = 1;
  static const int rest = 2;
  static const int active = 3;
}

/// Which live-recording sensor an activity type records from.
enum ActivityRecordingSensor {
  none,
  gps,
  ble,
  proximity,
  accelerometer,
  stepDetector,
}

/// The unit a repetition-like activity counts in.
enum ActivityRepetitionUnit {
  repetitions,
  steps,
}

/// Port of the Kotlin `ActivityEntryType` data class.
class ActivityEntryType {
  ActivityEntryType({
    required this.exerciseType,
    required this.label,
    String? id,
    this.supportsGpsRoute = true,
    this.supportsDistance = true,
    this.supportsElevation = false,
    ActivityRecordingSensor? recordingSensor,
    this.segmentType,
    this.defaultTitle,
    this.repetitionUnit,
  })  : id = id ?? exerciseType.toString(),
        recordingSensor = recordingSensor ??
            (supportsGpsRoute
                ? ActivityRecordingSensor.gps
                : ActivityRecordingSensor.none);

  final int exerciseType;
  final String id;
  final String label;
  final bool supportsGpsRoute;
  final bool supportsDistance;
  final bool supportsElevation;
  final ActivityRecordingSensor recordingSensor;
  final int? segmentType;
  final String? defaultTitle;
  final ActivityRepetitionUnit? repetitionUnit;

  bool get supportsLiveRecording =>
      recordingSensor != ActivityRecordingSensor.none;

  bool get supportsSetRepetitions =>
      repetitionUnit == ActivityRepetitionUnit.repetitions;

  bool get isRepetitionLike => repetitionUnit != null;

  bool get supportsStepCounting =>
      repetitionUnit == ActivityRepetitionUnit.steps;

  @override
  bool operator ==(Object other) =>
      other is ActivityEntryType && other.id == id;

  @override
  int get hashCode => id.hashCode;
}

/// Port of the Kotlin `activityEntryTypeById`.
ActivityEntryType? activityEntryTypeById(String? id) {
  if (id == null) return null;
  for (final type in defaultActivityEntryTypes) {
    if (type.id == id) return type;
  }
  return null;
}

/// Port of the Kotlin `DefaultActivityEntryTypes`.
final List<ActivityEntryType> defaultActivityEntryTypes = <ActivityEntryType>[
  ActivityEntryType(
    exerciseType: ExerciseSessionType.running,
    label: 'Running',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.biking,
    label: 'Cycling',
    supportsElevation: true,
  ),
  // A trainer ride is not a ride that happens to be indoors: it has a distance
  // and no route, and importing it as outdoor cycling would hang a 27 km ride on
  // a map it never touched. FIT names it (`sub_sport` 5/6) and the app had
  // nowhere to put it.
  ActivityEntryType(
    exerciseType: ExerciseSessionType.bikingStationary,
    id: 'stationary_bike',
    label: 'Stationary bike',
    supportsGpsRoute: false,
    supportsDistance: true,
    recordingSensor: ActivityRecordingSensor.ble,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.walking,
    label: 'Walking',
    supportsElevation: true,
    repetitionUnit: ActivityRepetitionUnit.steps,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.hiking,
    label: 'Hiking',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.wheelchair,
    label: 'Wheelchair',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.rowing,
    label: 'Rowing',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.paddling,
    label: 'Paddling',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.skiing,
    label: 'Skiing',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.snowboarding,
    label: 'Snowboarding',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.snowshoeing,
    label: 'Snowshoeing',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.skating,
    label: 'Skating',
    supportsElevation: true,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.sailing,
    label: 'Sailing',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.surfing,
    label: 'Surfing',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.swimmingOpenWater,
    label: 'Open water swimming',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.golf,
    label: 'Golf',
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.strengthTraining,
    label: 'Strength training',
    supportsGpsRoute: false,
    supportsDistance: false,
    recordingSensor: ActivityRecordingSensor.ble,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.runningTreadmill,
    id: 'treadmill',
    label: 'Treadmill',
    supportsGpsRoute: false,
    supportsDistance: true,
    recordingSensor: ActivityRecordingSensor.stepDetector,
    segmentType: ExerciseSegmentType.runningTreadmill,
    repetitionUnit: ActivityRepetitionUnit.steps,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.calisthenics,
    id: 'push_ups',
    label: 'Push-ups',
    supportsGpsRoute: false,
    supportsDistance: false,
    recordingSensor: ActivityRecordingSensor.proximity,
    segmentType: ExerciseSegmentType.otherWorkout,
    defaultTitle: 'Push-ups',
    repetitionUnit: ActivityRepetitionUnit.repetitions,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.calisthenics,
    id: 'pull_ups',
    label: 'Pull-ups',
    supportsGpsRoute: false,
    supportsDistance: false,
    recordingSensor: ActivityRecordingSensor.accelerometer,
    segmentType: ExerciseSegmentType.pullUp,
    repetitionUnit: ActivityRepetitionUnit.repetitions,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.calisthenics,
    id: 'rope_skipping',
    label: 'Rope skipping',
    supportsGpsRoute: false,
    supportsDistance: false,
    recordingSensor: ActivityRecordingSensor.accelerometer,
    segmentType: ExerciseSegmentType.jumpRope,
    repetitionUnit: ActivityRepetitionUnit.repetitions,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.gymnastics,
    id: 'trampoline_jumping',
    label: 'Trampoline jumping',
    supportsGpsRoute: false,
    supportsDistance: false,
    recordingSensor: ActivityRecordingSensor.accelerometer,
    segmentType: ExerciseSegmentType.otherWorkout,
    defaultTitle: 'Trampoline jumping',
    repetitionUnit: ActivityRepetitionUnit.repetitions,
  ),
  ActivityEntryType(
    exerciseType: ExerciseSessionType.otherWorkout,
    label: 'Other workout',
    supportsGpsRoute: false,
    supportsDistance: false,
  ),
];
