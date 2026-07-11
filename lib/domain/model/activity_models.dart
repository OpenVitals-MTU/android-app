import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import 'ble_sensor_models.dart';
import 'nutrition_models.dart';

part 'activity_models.freezed.dart';

@freezed
abstract class ExerciseData with _$ExerciseData {
  const ExerciseData._();

  const factory ExerciseData.build({
    required String id,
    required String? title,
    required int exerciseType,
    required DateTime startTime,
    required DateTime endTime,
    required int durationMs,
    required String source,
    required double? totalDistanceMeters,
    required double? totalCaloriesKcal,
    required double? activeCaloriesKcal,
    required int? steps,
    required int? wheelchairPushes,
    required double? averageSpeedMetersPerSecond,
    required double? averagePowerWatts,
    required double? averageStepsCadenceRate,
    required double? averageCyclingCadenceRpm,
    required int? averageHeartRateBpm,
    required int? floorsClimbed,
    required double? elevationGainedMeters,
    required String? notes,
    required Duration? startZoneOffset,
    required Duration? endZoneOffset,
    required DateTime? lastModifiedTime,
    required String? clientRecordId,
    required int? clientRecordVersion,
    required int? recordingMethod,
    required ExerciseDeviceData? device,
    required String? plannedExerciseSessionId,
    required List<ExerciseSegmentData> segments,
    required List<ExerciseLapData> laps,
    required ExerciseRouteData route,
    required bool isOpenVitalsEntry,
    required CaloriesBurnedSource totalCaloriesSource,
  }) = _ExerciseData;

  factory ExerciseData({
    required String id,
    required String? title,
    required int exerciseType,
    required DateTime startTime,
    required DateTime endTime,
    required int durationMs,
    required String source,
    double? totalDistanceMeters,
    double? totalCaloriesKcal,
    double? activeCaloriesKcal,
    int? steps,
    int? wheelchairPushes,
    double? averageSpeedMetersPerSecond,
    double? averagePowerWatts,
    double? averageStepsCadenceRate,
    double? averageCyclingCadenceRpm,
    int? averageHeartRateBpm,
    int? floorsClimbed,
    double? elevationGainedMeters,
    String? notes,
    Duration? startZoneOffset,
    Duration? endZoneOffset,
    DateTime? lastModifiedTime,
    String? clientRecordId,
    int? clientRecordVersion,
    int? recordingMethod,
    ExerciseDeviceData? device,
    String? plannedExerciseSessionId,
    List<ExerciseSegmentData> segments = const <ExerciseSegmentData>[],
    List<ExerciseLapData> laps = const <ExerciseLapData>[],
    ExerciseRouteData route = const ExerciseRouteData(),
    bool isOpenVitalsEntry = false,
    CaloriesBurnedSource? totalCaloriesSource,
  }) =>
      ExerciseData.build(
        id: id,
        title: title,
        exerciseType: exerciseType,
        startTime: startTime,
        endTime: endTime,
        durationMs: durationMs,
        source: source,
        totalDistanceMeters: totalDistanceMeters,
        totalCaloriesKcal: totalCaloriesKcal,
        activeCaloriesKcal: activeCaloriesKcal,
        steps: steps,
        wheelchairPushes: wheelchairPushes,
        averageSpeedMetersPerSecond: averageSpeedMetersPerSecond,
        averagePowerWatts: averagePowerWatts,
        averageStepsCadenceRate: averageStepsCadenceRate,
        averageCyclingCadenceRpm: averageCyclingCadenceRpm,
        averageHeartRateBpm: averageHeartRateBpm,
        floorsClimbed: floorsClimbed,
        elevationGainedMeters: elevationGainedMeters,
        notes: notes,
        startZoneOffset: startZoneOffset,
        endZoneOffset: endZoneOffset,
        lastModifiedTime: lastModifiedTime,
        clientRecordId: clientRecordId,
        clientRecordVersion: clientRecordVersion,
        recordingMethod: recordingMethod,
        device: device,
        plannedExerciseSessionId: plannedExerciseSessionId,
        segments: segments,
        laps: laps,
        route: route,
        isOpenVitalsEntry: isOpenVitalsEntry,
        totalCaloriesSource: totalCaloriesSource ??
            (totalCaloriesKcal != null
                ? CaloriesBurnedSource.recordedTotal
                : CaloriesBurnedSource.noData),
      );

  int get durationMinutes => durationMs ~/ 60000;
}

@freezed
abstract class ExerciseDeviceData with _$ExerciseDeviceData {
  const factory ExerciseDeviceData({
    required int type,
    required String? manufacturer,
    required String? model,
  }) = _ExerciseDeviceData;
}

@freezed
abstract class ExerciseSegmentData with _$ExerciseSegmentData {
  const ExerciseSegmentData._();

  const factory ExerciseSegmentData({
    required DateTime startTime,
    required DateTime endTime,
    required int segmentType,
    required int repetitions,
    int? setIndex,
  }) = _ExerciseSegmentData;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;
}

@freezed
abstract class ExerciseLapData with _$ExerciseLapData {
  const ExerciseLapData._();

  const factory ExerciseLapData({
    required DateTime startTime,
    required DateTime endTime,
    required double? lengthMeters,
  }) = _ExerciseLapData;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;
}

@freezed
abstract class ActivityRecordingLap with _$ActivityRecordingLap {
  const ActivityRecordingLap._();

  const factory ActivityRecordingLap({
    required DateTime startTime,
    required DateTime endTime,
    required double? distanceMeters,
  }) = _ActivityRecordingLap;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;
}

@freezed
abstract class ActivityRecordingMarker with _$ActivityRecordingMarker {
  const factory ActivityRecordingMarker({
    required String id,
    required DateTime time,
    required double latitude,
    required double longitude,
    required double? altitudeMeters,
    required String name,
    @Default('') String note,
    // Equals ActivityRecordingMarkerType.generic.value.
    @Default('generic') String type,
  }) = _ActivityRecordingMarker;
}

enum ActivityRecordingMarkerType {
  generic('generic');

  const ActivityRecordingMarkerType(this.value);

  final String value;
}

@freezed
abstract class ExerciseRouteData with _$ExerciseRouteData {
  const factory ExerciseRouteData({
    @Default(ExerciseRouteStatus.noData) ExerciseRouteStatus status,
    @Default(<ExerciseRoutePoint>[]) List<ExerciseRoutePoint> points,
  }) = _ExerciseRouteData;
}

enum ExerciseRouteStatus {
  data('DATA'),
  consentRequired('CONSENT_REQUIRED'),
  noData('NO_DATA');

  const ExerciseRouteStatus(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static ExerciseRouteStatus? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class ExerciseRoutePoint with _$ExerciseRoutePoint {
  const factory ExerciseRoutePoint({
    required DateTime time,
    required double latitude,
    required double longitude,
    required double? altitudeMeters,
    required double? horizontalAccuracyMeters,
    required double? verticalAccuracyMeters,
  }) = _ExerciseRoutePoint;
}

@freezed
abstract class ActivityPauseInterval with _$ActivityPauseInterval {
  const factory ActivityPauseInterval({
    required DateTime startTime,
    required DateTime endTime,
  }) = _ActivityPauseInterval;
}

@freezed
abstract class ActivityExerciseSegmentWrite with _$ActivityExerciseSegmentWrite {
  const factory ActivityExerciseSegmentWrite({
    required DateTime startTime,
    required DateTime endTime,
    required int segmentType,
    @Default(0) int repetitions,
    int? setIndex,
  }) = _ActivityExerciseSegmentWrite;
}

@freezed
abstract class ActivityWriteRequest with _$ActivityWriteRequest {
  const factory ActivityWriteRequest({
    required int exerciseType,
    required DateTime startTime,
    required DateTime endTime,
    String? title,
    String? notes,
    String? plannedExerciseSessionId,
    @Default(<ExerciseRoutePoint>[]) List<ExerciseRoutePoint> routePoints,
    @Default(<ActivityPauseInterval>[])
    List<ActivityPauseInterval> pauseIntervals,
    @Default(<ExerciseLapData>[]) List<ExerciseLapData> laps,
    @Default(<ActivityExerciseSegmentWrite>[])
    List<ActivityExerciseSegmentWrite> exerciseSegments,
    int? stepsCount,
    double? distanceMeters,
    double? elevationGainedMeters,
    double? activeCaloriesKcal,
    double? totalCaloriesKcal,
    @Default(BleRecordingSampleBuffer()) BleRecordingSampleBuffer bleSamples,
  }) = _ActivityWriteRequest;
}

@freezed
abstract class PlannedExerciseData with _$PlannedExerciseData {
  const PlannedExerciseData._();

  const factory PlannedExerciseData({
    required String id,
    required String? title,
    required int exerciseType,
    required DateTime startTime,
    required DateTime endTime,
    required bool hasExplicitTime,
    required String? completedExerciseSessionId,
    required String? notes,
    required int blockCount,
    required String source,
    @Default(<PlannedExerciseBlockData>[])
    List<PlannedExerciseBlockData> blocks,
  }) = _PlannedExerciseData;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;
}

@freezed
abstract class PlannedExerciseBlockData with _$PlannedExerciseBlockData {
  const factory PlannedExerciseBlockData({
    required int repetitions,
    required String? description,
    required List<PlannedExerciseStepData> steps,
  }) = _PlannedExerciseBlockData;
}

@freezed
abstract class PlannedExerciseStepData with _$PlannedExerciseStepData {
  const factory PlannedExerciseStepData({
    required int exerciseType,
    required int exercisePhase,
    required String? description,
    required PlannedExerciseCompletion completion,
  }) = _PlannedExerciseStepData;
}

/// Sealed hierarchy mirroring Kotlin `sealed interface
/// PlannedExerciseCompletion`. Nested Kotlin members are flattened with a
/// `PlannedExerciseCompletion` prefix.
sealed class PlannedExerciseCompletion {
  const PlannedExerciseCompletion();
}

class PlannedExerciseCompletionRepetitions extends PlannedExerciseCompletion {
  const PlannedExerciseCompletionRepetitions(this.repetitions);

  final int repetitions;

  @override
  bool operator ==(Object other) =>
      other is PlannedExerciseCompletionRepetitions &&
      other.repetitions == repetitions;

  @override
  int get hashCode => repetitions.hashCode;
}

class PlannedExerciseCompletionDurationSeconds
    extends PlannedExerciseCompletion {
  const PlannedExerciseCompletionDurationSeconds(this.seconds);

  final int seconds;

  @override
  bool operator ==(Object other) =>
      other is PlannedExerciseCompletionDurationSeconds &&
      other.seconds == seconds;

  @override
  int get hashCode => seconds.hashCode;
}

class PlannedExerciseCompletionManual extends PlannedExerciseCompletion {
  const PlannedExerciseCompletionManual();
}

class PlannedExerciseCompletionUnknown extends PlannedExerciseCompletion {
  const PlannedExerciseCompletionUnknown();
}

@freezed
abstract class PlannedExerciseWriteRequest with _$PlannedExerciseWriteRequest {
  const factory PlannedExerciseWriteRequest({
    String? id,
    required int exerciseType,
    required DateTime startTime,
    required DateTime endTime,
    String? title,
    String? notes,
    required List<PlannedExerciseBlockData> blocks,
  }) = _PlannedExerciseWriteRequest;
}

@freezed
abstract class DailySteps with _$DailySteps {
  const factory DailySteps({
    required LocalDate date,
    required int steps,
    required double distanceMeters,
    int? wheelchairPushes,
    int? floorsClimbed,
    double? activeCaloriesKcal,
    double? elevationGainedMeters,
  }) = _DailySteps;
}

@freezed
abstract class ActivityProgressPoint with _$ActivityProgressPoint {
  const factory ActivityProgressPoint({
    required DateTime time,
    required int totalSteps,
    required double? totalDistanceMeters,
    required double? totalCaloriesBurnedKcal,
    double? totalActiveCaloriesKcal,
    int? totalWheelchairPushes,
    int? totalFloorsClimbed,
    double? totalElevationGainedMeters,
  }) = _ActivityProgressPoint;
}

@freezed
abstract class SpeedSample with _$SpeedSample {
  const factory SpeedSample({
    required DateTime time,
    required double metersPerSecond,
    required String source,
  }) = _SpeedSample;
}

enum ActivityCadenceKind {
  cycling('CYCLING'),
  steps('STEPS');

  const ActivityCadenceKind(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static ActivityCadenceKind? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class ActivityCadenceSample with _$ActivityCadenceSample {
  const factory ActivityCadenceSample({
    required DateTime time,
    required double rate,
    required ActivityCadenceKind kind,
    required String source,
  }) = _ActivityCadenceSample;
}
