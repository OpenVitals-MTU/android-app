/// Which Pigeon message carries which domain model, field by field.
///
/// Hand-written and reviewed ON PURPOSE. Matching by name automatically looks
/// tempting and does not survive contact: `timeEpochMs` vs `time`,
/// `startZoneOffsetSeconds` vs `startZoneOffset`, `lastModifiedEpochMs` vs
/// `lastModifiedTime` — a name-matcher flags all of those as gaps, the list fills
/// with `// ignore`, and within a month it means nothing. This table is a
/// document: an entry in [domainOnly] is a DECISION; an omission is a BUG.
///
/// It exists because we shipped the same bug twice. `SleepData.recordingMethod`
/// and `ExerciseData.recordingMethod` were declared, rendered, and populated
/// NOWHERE — the Pigeon message they cross on never carried them. Every layer
/// compiled. Every test passed. Five rows read "Not available" on every sleep
/// session ever recorded, the activities screen counted zero manual entries
/// however many you had, and duplicate-session resolution silently fell back to
/// list order.
class MsgBinding {
  const MsgBinding({
    required this.msg,
    required this.domain,
    required this.domainFile,
    required this.map,
    this.domainOnly = const {},
    this.msgOnly = const {},
  });

  /// The Pigeon class, in `pigeons/messages.dart`.
  final String msg;

  /// The domain class it maps onto.
  final String domain;
  final String domainFile;

  /// msg field -> domain field.
  final Map<String, String> map;

  /// Domain fields that deliberately do NOT cross the bridge, each with the
  /// reason. This is the half that catches the bug: a domain field that is
  /// neither mapped nor listed here has no way to ever be populated.
  final Map<String, String> domainOnly;

  /// Msg fields the domain deliberately drops, each with the reason.
  final Map<String, String> msgOnly;
}

const String _activity = 'lib/domain/model/activity_models.dart';
const String _sleep = 'lib/domain/model/sleep_models.dart';
const String _heart = 'lib/domain/model/heart_models.dart';
const String _metrics = 'lib/domain/model/exercise_session_metrics.dart';

const List<MsgBinding> msgDomainBindings = [
  MsgBinding(
    msg: 'SleepDataMsg',
    domain: 'SleepData',
    domainFile: _sleep,
    map: {
      'id': 'id',
      'startEpochMs': 'startTime',
      'endEpochMs': 'endTime',
      'source': 'source',
      'title': 'title',
      'notes': 'notes',
      'clientRecordId': 'clientRecordId',
      'device': 'device',
      'stages': 'stages',
      'startZoneOffsetSeconds': 'startZoneOffset',
      'endZoneOffsetSeconds': 'endZoneOffset',
      'lastModifiedEpochMs': 'lastModifiedTime',
      'clientRecordVersion': 'clientRecordVersion',
      'recordingMethod': 'recordingMethod',
    },
    domainOnly: {
      'durationMs': 'derived by the Dart mapper from the STAGES, not from '
          'end - start: a session that is 8h in bed but 6h asleep must report 6h.',
    },
  ),
  MsgBinding(
    msg: 'SleepStageMsg',
    domain: 'SleepStage',
    domainFile: _sleep,
    map: {
      'startEpochMs': 'startTime',
      'endEpochMs': 'endTime',
      'stageType': 'stageType',
    },
    // No domainOnly: SleepStage.durationMs is a computed GETTER, not a field, so
    // it is not part of the contract at all. (The table's own guard caught this
    // being listed here in error — which is the point of that guard: a bogus
    // entry silences the orphan check for a field that really is missing.)
  ),
  MsgBinding(
    msg: 'SleepDeviceDataMsg',
    domain: 'SleepDeviceData',
    domainFile: _sleep,
    map: {'type': 'type', 'manufacturer': 'manufacturer', 'model': 'model'},
  ),
  MsgBinding(
    msg: 'ExerciseDataMsg',
    domain: 'ExerciseData',
    domainFile: _activity,
    map: {
      'id': 'id',
      'title': 'title',
      'exerciseType': 'exerciseType',
      'startEpochMs': 'startTime',
      'endEpochMs': 'endTime',
      'source': 'source',
      'notes': 'notes',
      'clientRecordId': 'clientRecordId',
      'plannedExerciseSessionId': 'plannedExerciseSessionId',
      'device': 'device',
      'segments': 'segments',
      'laps': 'laps',
      'route': 'route',
      'isOpenVitalsEntry': 'isOpenVitalsEntry',
      'totalDistanceMeters': 'totalDistanceMeters',
      'averageSpeedMetersPerSecond': 'averageSpeedMetersPerSecond',
      'startZoneOffsetSeconds': 'startZoneOffset',
      'endZoneOffsetSeconds': 'endZoneOffset',
      'lastModifiedEpochMs': 'lastModifiedTime',
      'clientRecordVersion': 'clientRecordVersion',
      'recordingMethod': 'recordingMethod',
    },
    domainOnly: {
      'durationMs': 'derived: end - start.',
      // A Health Connect ExerciseSessionRecord carries almost nothing. A watch
      // writes the walk as a session with a duration, and puts its steps,
      // distance, calories and elevation in SEPARATE records over the same
      // window. These are re-attached by aggregating over the session's window
      // (ExerciseSessionMetrics) or from its samples — never carried on the
      // session record itself, so they cannot come across on ExerciseDataMsg.
      'steps': 'backfilled from ExerciseSessionMetrics (sibling StepsRecord).',
      'totalCaloriesKcal': 'backfilled from ExerciseSessionMetrics.',
      'activeCaloriesKcal': 'backfilled from ExerciseSessionMetrics.',
      'floorsClimbed': 'backfilled from ExerciseSessionMetrics.',
      'elevationGainedMeters': 'backfilled from ExerciseSessionMetrics.',
      'wheelchairPushes': 'backfilled from ExerciseSessionMetrics.',
      'averagePowerWatts': 'backfilled from ExerciseSessionMetrics '
          '(PowerRecord.POWER_AVG). Was unreadable until e7dfba37.',
      'averageHeartRateBpm': 'backfilled from the session\'s HR samples.',
      'averageStepsCadenceRate': 'backfilled from the session\'s cadence samples.',
      'averageCyclingCadenceRpm': 'backfilled from the session\'s cadence samples.',
      'totalCaloriesSource': 'set by the calorie fallback chain (recorded total, '
          'else active + BMR): it says WHERE the number came from, which is not a '
          'thing Health Connect stores.',
    },
  ),
  MsgBinding(
    msg: 'ExerciseSessionMetricsMsg',
    domain: 'ExerciseSessionMetrics',
    domainFile: _metrics,
    map: {
      'totalDistanceMeters': 'totalDistanceMeters',
      'averageSpeedMetersPerSecond': 'averageSpeedMetersPerSecond',
      'steps': 'steps',
      'totalCaloriesKcal': 'totalCaloriesKcal',
      'activeCaloriesKcal': 'activeCaloriesKcal',
      'elevationGainedMeters': 'elevationGainedMeters',
      'floorsClimbed': 'floorsClimbed',
      'wheelchairPushes': 'wheelchairPushes',
      'averagePowerWatts': 'averagePowerWatts',
    },
  ),
  MsgBinding(
    msg: 'ExerciseSegmentMsg',
    domain: 'ExerciseSegmentData',
    domainFile: _activity,
    map: {
      'startEpochMs': 'startTime',
      'endEpochMs': 'endTime',
      'segmentType': 'segmentType',
      'repetitions': 'repetitions',
      // Present on BOTH sides — the field is not the problem. The Kotlin reader
      // hard-codes it to null, which no contract test can see. That is what
      // kotlin_msg_population_test.dart is for.
      'setIndex': 'setIndex',
    },
  ),
  MsgBinding(
    msg: 'ExerciseLapMsg',
    domain: 'ExerciseLapData',
    domainFile: _activity,
    map: {
      'startEpochMs': 'startTime',
      'endEpochMs': 'endTime',
      'lengthMeters': 'lengthMeters',
    },
  ),
  MsgBinding(
    msg: 'ExerciseDeviceDataMsg',
    domain: 'ExerciseDeviceData',
    domainFile: _activity,
    map: {'type': 'type', 'manufacturer': 'manufacturer', 'model': 'model'},
  ),
  MsgBinding(
    msg: 'HeartRateSampleMsg',
    domain: 'HeartRateSample',
    domainFile: _heart,
    map: {
      'timeEpochMs': 'time',
      'beatsPerMinute': 'beatsPerMinute',
      'source': 'source',
    },
  ),
];
