import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../../domain/model/activity_models.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../../domain/preferences/activity_recording_preferences.dart';
import '../../../../domain/model/activity_entry_types.dart';
import 'activity_recording_splits.dart';

part 'activity_recording.freezed.dart';

/// Port of the pure parts of the Kotlin `ActivityRecording.kt` — the recording
/// state model (enums, [ActivityRecordingState], [ActivityRecordedRepetitionSet],
/// [ActivityRecordingSnapshot]) plus the pure state/location extensions from
/// `ActivityRecordingStateExtensions.kt` and `ActivityRecordingLocationSupport.kt`.
///
/// The device-bound service (GPS/sensors/foreground service) lives in
/// `activity_recording_service.dart`; here only the [ActivityRecordingController]
/// interface it implements is declared, so the notifier can depend on it without
/// pulling in any plugin.

enum ActivityRecordingStatus { idle, recording, resting, paused }

enum ActivityRecordingKind { gpsRoute, repetition, timed }

enum ActivityGpsStatus { waitingForFix, fix, poorAccuracy, lost, disabled }

@freezed
abstract class ActivityRecordedRepetitionSet with _$ActivityRecordedRepetitionSet {
  const factory ActivityRecordedRepetitionSet({
    required int repetitions,
    required int restSeconds,
    required int activeMillis,
  }) = _ActivityRecordedRepetitionSet;
}

@freezed
abstract class ActivityRecordingState with _$ActivityRecordingState {
  const ActivityRecordingState._();

  const factory ActivityRecordingState({
    @Default(ActivityRecordingStatus.idle) ActivityRecordingStatus status,
    @Default(ActivityRecordingKind.gpsRoute) ActivityRecordingKind recordingKind,
    String? activityTypeId,
    int? exerciseType,
    DateTime? startTime,
    DateTime? endTime,
    DateTime? pausedStartedAt,
    @Default(0) int totalPausedMillis,
    @Default(<ActivityPauseInterval>[]) List<ActivityPauseInterval> pauseIntervals,
    @Default(<ExerciseRoutePoint>[]) List<ExerciseRoutePoint> points,
    @Default(<int>[]) List<int> routeBreakIndexes,
    @Default(<ActivityRecordingLap>[]) List<ActivityRecordingLap> manualLaps,
    @Default(<ActivityRecordingMarker>[]) List<ActivityRecordingMarker> markers,
    ExerciseRoutePoint? latestUiPoint,
    @Default(0.0) double distanceMeters,
    @Default(0.0) double elevationGainedMeters,
    @Default(0.0) double elevationLostMeters,
    @Default(0.0) double barometerElevationGainedMeters,
    @Default(0.0) double barometerElevationLostMeters,
    @Default(false) bool hasBarometerElevation,
    double? lastBarometerAltitudeMeters,
    @Default(0.0) double currentSpeedMetersPerSecond,
    @Default(0.0) double maxSpeedMetersPerSecond,
    @Default(ActivityGpsStatus.waitingForFix) ActivityGpsStatus gpsStatus,
    @Default(ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording)
    bool keepScreenOnDuringRecording,
    @Default(ActivityRecordingPreferences.defaultAutoIdleEnabled)
    bool autoIdleEnabled,
    @Default(ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds * 1000)
    int autoIdleTimeoutMillis,
    DateTime? lastMovementAt,
    @Default(0) int totalIdleMillis,
    @Default(0) int repetitionCount,
    @Default(0) int currentSetRepetitionCount,
    @Default(<ActivityRecordedRepetitionSet>[])
    List<ActivityRecordedRepetitionSet> repetitionSets,
    @Default(0) int repetitionRestSeconds,
    DateTime? currentSetStartedAt,
    DateTime? restStartedAt,
    @Default(0) int accumulatedRestMillis,
    double? lastAccuracyMeters,
    DateTime? lastLocationTime,
    @Default(0) int droppedPointCount,
    String? errorMessage,
    int? currentHeartRateBpm,
    int? currentCyclingCadenceRpm,
    double? currentPowerWatts,
    double? currentSensorSpeedMetersPerSecond,
    int? currentRunningCadenceRpm,
    @Default(false) bool bleHeartRateNoSignal,
    @Default(<BleDeviceConnectionStatus>[])
    List<BleDeviceConnectionStatus> bleDeviceStatuses,
    @Default(ActivityRecordingDashboardLayout())
    ActivityRecordingDashboardLayout dashboardLayout,
  }) = _ActivityRecordingState;

  bool get isActive =>
      status == ActivityRecordingStatus.recording ||
      status == ActivityRecordingStatus.resting ||
      status == ActivityRecordingStatus.paused;

  // ── Pure state extensions (ActivityRecordingStateExtensions.kt) ────────────

  Duration elapsedDuration([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    final start = startTime;
    if (start == null) return Duration.zero;
    final effectiveEnd = endTime ??
        (status == ActivityRecordingStatus.paused
            ? (pausedStartedAt ?? resolvedNow)
            : resolvedNow);
    return Duration(
      milliseconds:
          _atLeast0(_ms(effectiveEnd) - _ms(start)),
    );
  }

  Duration restDuration([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    return Duration(
      milliseconds:
          _atLeast0(accumulatedRestMillis + openRestMillis(resolvedNow)),
    );
  }

  Duration movingDuration([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    final elapsedMillis = elapsedDuration(resolvedNow).inMilliseconds;
    final openPauseMillis =
        (status == ActivityRecordingStatus.paused && pausedStartedAt != null)
            ? _atLeast0(_ms(resolvedNow) - _ms(pausedStartedAt!))
            : 0;
    final pausedMillis = totalPausedMillis + openPauseMillis;
    final idleMillis = totalIdleMillis + openIdleMillis(resolvedNow);
    final restMillis = recordingKind == ActivityRecordingKind.repetition
        ? restDuration(resolvedNow).inMilliseconds
        : 0;
    return Duration(
      milliseconds:
          _atLeast0(elapsedMillis - pausedMillis - idleMillis - restMillis),
    );
  }

  Duration restRemainingDuration([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    final restEnd = restEndTime();
    if (restEnd == null) return Duration.zero;
    return Duration(milliseconds: _atLeast0(_ms(restEnd) - _ms(resolvedNow)));
  }

  DateTime? restEndTime() {
    final start = restStartedAt;
    if (start == null) return null;
    if (status != ActivityRecordingStatus.resting || repetitionRestSeconds <= 0) {
      return null;
    }
    return start.add(Duration(seconds: repetitionRestSeconds));
  }

  int openRestMillis(DateTime now) {
    final start = restStartedAt;
    if (start == null || status != ActivityRecordingStatus.resting) return 0;
    return _atLeast0(_ms(now) - _ms(start));
  }

  List<ActivityRecordedRepetitionSet> recordedRepetitionSets(DateTime end) {
    final sets = status == ActivityRecordingStatus.resting
        ? withLastRestSeconds(
            repetitionSets, _atLeast0(openRestMillis(end) ~/ 1000))
        : repetitionSets;
    if (status != ActivityRecordingStatus.recording ||
        currentSetRepetitionCount <= 0) {
      return sets;
    }
    final activeMillis = _atLeast(
      _ms(end) - _ms(currentSetStartedAt ?? startTime ?? end),
      1,
    );
    return [
      ...sets,
      ActivityRecordedRepetitionSet(
        repetitions: currentSetRepetitionCount,
        restSeconds: 0,
        activeMillis: activeMillis,
      ),
    ];
  }

  double displayElevationGainedMeters() =>
      hasBarometerElevation ? barometerElevationGainedMeters : elevationGainedMeters;

  List<ActivityRecordingLap> closedManualLaps(DateTime endTime) {
    if (manualLaps.isEmpty) return const [];
    final openStart = manualLaps
            .reduce((a, b) => a.endTime.isAfter(b.endTime) ? a : b)
            .endTime;
    if (!openStart.isBefore(endTime)) return manualLaps;
    final distance = activityRecordingRouteDistanceMeters(
      points: points,
      routeBreakIndexes: routeBreakIndexes,
      startTime: openStart,
      endTime: endTime,
    );
    return [
      ...manualLaps,
      ActivityRecordingLap(
        startTime: openStart,
        endTime: endTime,
        distanceMeters: distance > 0.0 ? distance : null,
      ),
    ];
  }

  double effectiveCurrentSpeedMetersPerSecond([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    if (status != ActivityRecordingStatus.recording ||
        isAutoIdle(resolvedNow) ||
        gpsStatus == ActivityGpsStatus.poorAccuracy ||
        gpsStatus == ActivityGpsStatus.lost ||
        gpsStatus == ActivityGpsStatus.disabled) {
      return 0.0;
    }
    return currentSpeedMetersPerSecond;
  }

  bool isAutoIdle([DateTime? now]) {
    final resolvedNow = now ?? _nowUtc();
    final movementAt = lastMovementAt;
    return status == ActivityRecordingStatus.recording &&
        autoIdleEnabled &&
        movementAt != null &&
        !resolvedNow.isBefore(
          movementAt.add(Duration(milliseconds: autoIdleTimeoutMillis)),
        );
  }

  int openIdleMillis(DateTime now) {
    if (status != ActivityRecordingStatus.recording || !autoIdleEnabled) return 0;
    final movementAt = lastMovementAt;
    if (movementAt == null) return 0;
    final idleStartedAt =
        movementAt.add(Duration(milliseconds: autoIdleTimeoutMillis));
    return now.isAfter(idleStartedAt) ? _atLeast0(_ms(now) - _ms(idleStartedAt)) : 0;
  }

  double minimumSampleDistanceMeters(ActivityRecordingPreferences preferences) {
    final interval = preferences.recordingDistanceIntervalMeters;
    if (interval != null) return interval.toDouble();
    final type = exerciseType;
    if (type == ExerciseSessionType.biking ||
        type == ExerciseSessionType.skiing ||
        type == ExerciseSessionType.snowboarding ||
        type == ExerciseSessionType.sailing) {
      return 10.0;
    }
    if (type == ExerciseSessionType.rowing ||
        type == ExerciseSessionType.paddling ||
        type == ExerciseSessionType.skating ||
        type == ExerciseSessionType.surfing) {
      return 7.0;
    }
    if (type == ExerciseSessionType.swimmingOpenWater) return 4.0;
    return 5.0;
  }
}

/// Port of the Kotlin `List<ActivityRecordedRepetitionSet>.withLastRestSeconds`.
List<ActivityRecordedRepetitionSet> withLastRestSeconds(
  List<ActivityRecordedRepetitionSet> sets,
  int restSeconds,
) {
  if (sets.isEmpty) return sets;
  final updated = [...sets];
  updated[updated.length - 1] =
      updated.last.copyWith(restSeconds: _atLeast0(restSeconds));
  return updated;
}

@freezed
abstract class ActivityRecordingSnapshot with _$ActivityRecordingSnapshot {
  const factory ActivityRecordingSnapshot({
    required int exerciseType,
    @Default(ActivityRecordingKind.gpsRoute) ActivityRecordingKind recordingKind,
    String? activityTypeId,
    required DateTime startTime,
    required DateTime endTime,
    required List<ExerciseRoutePoint> points,
    required List<ActivityPauseInterval> pauseIntervals,
    @Default(<int>[]) List<int> routeBreakIndexes,
    @Default(<ActivityRecordingLap>[]) List<ActivityRecordingLap> manualLaps,
    @Default(<ActivityRecordingMarker>[]) List<ActivityRecordingMarker> markers,
    required double distanceMeters,
    required double elevationGainedMeters,
    @Default(0) int repetitionCount,
    @Default(<ActivityRecordedRepetitionSet>[])
    List<ActivityRecordedRepetitionSet> repetitionSets,
    @Default(BleRecordingSampleBuffer()) BleRecordingSampleBuffer bleSamples,
  }) = _ActivityRecordingSnapshot;
}

/// A minimal, plugin-free GPS fix passed into the controller when starting a GPS
/// recording (the device layer maps a geolocator `Position` to this).
class ActivityRecordingInitialFix {
  const ActivityRecordingInitialFix({
    required this.latitude,
    required this.longitude,
    this.accuracyMeters,
    this.altitudeMeters,
    this.timeMillis,
  });

  final double latitude;
  final double longitude;
  final double? accuracyMeters;
  final double? altitudeMeters;
  final int? timeMillis;
}

/// The recorder as its consumers see it: implemented by the device-bound
/// `ActivityRecordingService` (deferred runtime), consumed by
/// `ActivityRecordingViewModel` and the activity-entry notifier, and faked
/// wholesale in tests. Nothing above this line may reach for the plugins.
abstract interface class ActivityRecordingController {
  ValueListenable<ActivityRecordingState> get state;

  void prepareRecordingDashboard(ActivityEntryType activityType);

  void updateDashboardLayout(ActivityRecordingDashboardLayout layout);

  void clearPreparedRecording();

  Future<bool> startRecording(
    ActivityEntryType activityType,
    ActivityRecordingInitialFix? initialFix, {
    int repetitionRestSeconds = 0,
  });

  void previewBleConnections();

  void stopBlePreview();

  void pauseRecording();

  void resumeRecording();

  void addManualLap();

  void addMarker();

  void updateMarker(ActivityRecordingMarker marker);

  void deleteMarker(String markerId);

  void discardRecording();

  ActivityRecordingSnapshot? finishRecording();

  void adjustRepetitionCount(int delta);

  void endRepetitionSet();

  void startNextRepetitionSet();
}

// ── Pure location-support helpers (ActivityRecordingLocationSupport.kt) ───────

ActivityRecordingState withDroppedLocation(
  ActivityRecordingState state,
  double? accuracyMeters, {
  DateTime? locationTime,
  ActivityGpsStatus? gpsStatus,
}) =>
    state.copyWith(
      gpsStatus: gpsStatus ?? state.gpsStatus,
      lastAccuracyMeters: accuracyMeters ?? state.lastAccuracyMeters,
      lastLocationTime: locationTime ?? state.lastLocationTime,
      droppedPointCount: state.droppedPointCount + 1,
    );

ActivityRecordingState withLocationMetadata(
  ActivityRecordingState state, {
  required double? accuracyMeters,
  required DateTime locationTime,
  ActivityGpsStatus? gpsStatus,
  ActivityRecordingPreferences? recordingPreferences,
}) =>
    state.copyWith(
      gpsStatus: gpsStatus ?? state.gpsStatus,
      keepScreenOnDuringRecording:
          recordingPreferences?.keepScreenOnDuringRecording ??
              state.keepScreenOnDuringRecording,
      autoIdleEnabled:
          recordingPreferences?.autoIdleEnabled ?? state.autoIdleEnabled,
      autoIdleTimeoutMillis: recordingPreferences != null
          ? recordingPreferences.autoIdleTimeoutSeconds * 1000
          : state.autoIdleTimeoutMillis,
      lastAccuracyMeters: accuracyMeters ?? state.lastAccuracyMeters,
      lastLocationTime: locationTime,
      errorMessage: null,
    );

/// The state after a GPS fix that cleared [activityGpsFixQuality] is folded in.
///
/// This is the accumulator behind every number the recording screen shows —
/// distance, elevation gain and loss, current and max speed, the route and its
/// segment breaks, and the auto-idle clock. Keeping it a pure fold of one fix
/// into the running state is what lets it be tested without a GPS receiver; the
/// controller keeps only the device I/O around it (the async ellipsoid→MSL
/// altitude hop, and the generation guards that straddle it).
///
/// A fix that cleared the quality gate can still be turned away here, for
/// reasons about the *route* rather than the fix: it may not advance the clock,
/// it may fall within the minimum sample distance or interval, or it may be an
/// implausible jump. Each of those still yields a state, so the caller never has
/// to know which branch it took.
ActivityRecordingState withAcceptedLocation(
  ActivityRecordingState state, {
  required ExerciseRoutePoint point,
  required double accuracyMeters,
  required ActivityRecordingPreferences preferences,
}) {
  final lastPoint = state.points.isNotEmpty ? state.points.last : null;
  if (lastPoint == null) {
    // The first fix opens the route but closes no leg: no distance, no climb,
    // no speed.
    return _withAppendedPoint(
      state,
      point: point,
      accuracyMeters: accuracyMeters,
      preferences: preferences,
    );
  }

  // A fix that does not advance the clock cannot bound an interval, so every
  // rate below it (speed, idle) would divide by zero or run backwards.
  if (!point.time.isAfter(lastPoint.time)) {
    return withDroppedLocation(state, accuracyMeters,
        locationTime: point.time, gpsStatus: ActivityGpsStatus.fix);
  }

  final elapsedMillis = (point.time.millisecondsSinceEpoch -
          lastPoint.time.millisecondsSinceEpoch)
      .clamp(0, 1 << 62);
  final distanceMeters = recordingDistanceMetersTo(lastPoint, point);

  // Too near or too soon to be worth a route point: keep what the fix says about
  // the receiver, and show it live, but grow neither the route nor the totals.
  if (distanceMeters < state.minimumSampleDistanceMeters(preferences) ||
      elapsedMillis < preferences.recordingTimeIntervalMillis) {
    return withLocationMetadata(
      state,
      accuracyMeters: accuracyMeters,
      locationTime: point.time,
      gpsStatus: ActivityGpsStatus.fix,
      recordingPreferences: preferences,
    ).copyWith(latestUiPoint: point);
  }

  // A gap wider than `routeGapMeters` is a tunnel or a recording carried between
  // places, not a sprint: break the drawn line rather than bank a false distance.
  final routeGap = preferences.routeGapMeters;
  if (routeGap != null && distanceMeters > routeGap) {
    return _withAppendedPoint(
      state,
      point: point,
      accuracyMeters: accuracyMeters,
      preferences: preferences,
      routeBreakIndexes: [...state.routeBreakIndexes, state.points.length],
    );
  }

  if (isImplausibleJump(
      lastPoint, point, distanceMeters, elapsedMillis, accuracyMeters)) {
    return withDroppedLocation(state, accuracyMeters,
        locationTime: point.time, gpsStatus: ActivityGpsStatus.fix);
  }

  return _withAppendedPoint(
    state,
    point: point,
    accuracyMeters: accuracyMeters,
    preferences: preferences,
    distanceIncrement: distanceMeters,
    elevationIncrement: elevationGainMetersTo(lastPoint, point),
    elevationLossIncrement: elevationLossMetersTo(lastPoint, point),
    currentSpeed: distanceMeters / (elapsedMillis / 1000.0),
    movedAt: point.time,
  );
}

/// Appends [point] to the route and folds in whatever this fix added to the
/// totals. The increments default to zero — the first-fix and route-break case,
/// where the point joins the route but opens no leg.
///
/// [movedAt] is null when the fix is not movement (again: first fix, route
/// break), and that is what leaves the auto-idle clock alone.
ActivityRecordingState _withAppendedPoint(
  ActivityRecordingState state, {
  required ExerciseRoutePoint point,
  required double accuracyMeters,
  required ActivityRecordingPreferences preferences,
  List<int>? routeBreakIndexes,
  double distanceIncrement = 0.0,
  double elevationIncrement = 0.0,
  double elevationLossIncrement = 0.0,
  double currentSpeed = 0.0,
  DateTime? movedAt,
}) {
  final lastMovementAt = state.lastMovementAt ?? state.startTime ?? point.time;
  return state.copyWith(
    points: [...state.points, point],
    routeBreakIndexes: routeBreakIndexes ?? state.routeBreakIndexes,
    latestUiPoint: point,
    distanceMeters: state.distanceMeters + distanceIncrement,
    elevationGainedMeters: state.elevationGainedMeters + elevationIncrement,
    elevationLostMeters: state.elevationLostMeters + elevationLossIncrement,
    currentSpeedMetersPerSecond: currentSpeed,
    maxSpeedMetersPerSecond: state.maxSpeedMetersPerSecond > currentSpeed
        ? state.maxSpeedMetersPerSecond
        : currentSpeed,
    gpsStatus: ActivityGpsStatus.fix,
    keepScreenOnDuringRecording: preferences.keepScreenOnDuringRecording,
    autoIdleEnabled: preferences.autoIdleEnabled,
    autoIdleTimeoutMillis: preferences.autoIdleTimeoutSeconds * 1000,
    lastMovementAt: movedAt ?? lastMovementAt,
    totalIdleMillis: movedAt == null
        ? state.totalIdleMillis
        : _accruedIdleMillis(
            state,
            lastMovementAt: lastMovementAt,
            movedAt: movedAt,
          ),
    lastAccuracyMeters: accuracyMeters,
    lastLocationTime: point.time,
    errorMessage: null,
  );
}

/// Idle is only charged for the stretch BEYOND the auto-idle timeout: standing
/// still for `timeout + 30s` accrues 30s of idle, not the whole stop.
///
/// The timeout and the enabled flag are read from the state, not from
/// preferences, so a preference changed mid-recording cannot retroactively
/// re-price idle time already accrued under the old one.
int _accruedIdleMillis(
  ActivityRecordingState state, {
  required DateTime lastMovementAt,
  required DateTime movedAt,
}) {
  if (!state.autoIdleEnabled) return state.totalIdleMillis;
  final idleStartedAt =
      lastMovementAt.add(Duration(milliseconds: state.autoIdleTimeoutMillis));
  if (!movedAt.isAfter(idleStartedAt)) return state.totalIdleMillis;
  return state.totalIdleMillis +
      (movedAt.millisecondsSinceEpoch - idleStartedAt.millisecondsSinceEpoch)
          .clamp(0, 1 << 62);
}

/// Kotlin `recordingDistanceMetersTo` uses `Location.distanceBetween`, which is
/// geodesic on the WGS84 ellipsoid — not the spherical haversine the split
/// helpers use (they are haversine in the Kotlin app too). The live distance
/// accumulator has to match it.
double recordingDistanceMetersTo(ExerciseRoutePoint from, ExerciseRoutePoint to) {
  return geodesicDistanceMeters(
    from.latitude,
    from.longitude,
    to.latitude,
    to.longitude,
  );
}

/// Port of `android.location.Location.computeDistanceAndBearing` (Vincenty
/// inverse formula on WGS84), the algorithm behind `Location.distanceBetween`.
double geodesicDistanceMeters(
    double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg) {
  const a = 6378137.0; // WGS84 major axis
  const b = 6356752.3142; // WGS84 semi-major axis
  const f = (a - b) / a;
  const aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

  final lat1 = lat1Deg * math.pi / 180.0;
  final lat2 = lat2Deg * math.pi / 180.0;
  final lon1 = lon1Deg * math.pi / 180.0;
  final lon2 = lon2Deg * math.pi / 180.0;

  final bigL = lon2 - lon1;
  final u1 = math.atan((1.0 - f) * math.tan(lat1));
  final u2 = math.atan((1.0 - f) * math.tan(lat2));

  final cosU1 = math.cos(u1);
  final cosU2 = math.cos(u2);
  final sinU1 = math.sin(u1);
  final sinU2 = math.sin(u2);
  final cosU1cosU2 = cosU1 * cosU2;
  final sinU1sinU2 = sinU1 * sinU2;

  var sigma = 0.0;
  var deltaSigma = 0.0;
  var bigA = 1.0;

  var lambda = bigL;
  const maxIters = 20;
  for (var iter = 0; iter < maxIters; iter++) {
    final lambdaOrig = lambda;
    final cosLambda = math.cos(lambda);
    final sinLambda = math.sin(lambda);
    final t1 = cosU2 * sinLambda;
    final t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
    final sinSqSigma = t1 * t1 + t2 * t2;
    final sinSigma = math.sqrt(sinSqSigma);
    final cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda;
    sigma = math.atan2(sinSigma, cosSigma);
    final sinAlpha = sinSigma == 0 ? 0.0 : cosU1cosU2 * sinLambda / sinSigma;
    final cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
    final cos2SM =
        cosSqAlpha == 0 ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha;

    final uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
    bigA = 1 +
        (uSquared / 16384.0) *
            (4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)));
    final bigB = (uSquared / 1024.0) *
        (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
    final bigC = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
    final cos2SMSq = cos2SM * cos2SM;
    deltaSigma = bigB *
        sinSigma *
        (cos2SM +
            (bigB / 4.0) *
                (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                    (bigB / 6.0) *
                        cos2SM *
                        (-3.0 + 4.0 * sinSigma * sinSigma) *
                        (-3.0 + 4.0 * cos2SMSq)));

    lambda = bigL +
        (1.0 - bigC) *
            f *
            sinAlpha *
            (sigma +
                bigC *
                    sinSigma *
                    (cos2SM + bigC * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)));

    final delta = (lambda - lambdaOrig) / lambda;
    if (delta.abs() < 1.0e-12) break;
  }

  return b * bigA * (sigma - deltaSigma);
}

double elevationGainMetersTo(ExerciseRoutePoint from, ExerciseRoutePoint to) {
  final startAltitude = from.altitudeMeters;
  final endAltitude = to.altitudeMeters;
  if (startAltitude == null || endAltitude == null) return 0.0;
  final delta = endAltitude - startAltitude;
  return delta >= minElevationGainIncrementMeters ? delta : 0.0;
}

double elevationLossMetersTo(ExerciseRoutePoint from, ExerciseRoutePoint to) {
  final startAltitude = from.altitudeMeters;
  final endAltitude = to.altitudeMeters;
  if (startAltitude == null || endAltitude == null) return 0.0;
  final delta = startAltitude - endAltitude;
  return delta >= minElevationGainIncrementMeters ? delta : 0.0;
}

bool isImplausibleJump(
  ExerciseRoutePoint lastPoint,
  ExerciseRoutePoint point,
  double distanceMeters,
  int elapsedMillis,
  double accuracyMeters,
) {
  if (elapsedMillis <= 0) return true;
  final metersPerSecond = distanceMeters / (elapsedMillis / 1000.0);
  final lastAccuracyMeters = lastPoint.horizontalAccuracyMeters ?? accuracyMeters;
  final combinedAccuracyMeters = lastAccuracyMeters + accuracyMeters;
  return metersPerSecond > maxPlausibleSpeedMetersPerSecond &&
      distanceMeters > combinedAccuracyMeters;
}

/// Port of the Kotlin `Instant.toPauseInterval`.
ActivityPauseInterval? toPauseInterval(DateTime startTime, DateTime endTime) {
  if (!startTime.isBefore(endTime)) return null;
  return ActivityPauseInterval(startTime: startTime, endTime: endTime);
}

DateTime _nowUtc() => DateTime.now().toUtc();
int _ms(DateTime time) => time.millisecondsSinceEpoch;
int _atLeast0(int value) => value < 0 ? 0 : value;
int _atLeast(int value, int min) => value < min ? min : value;

const double maxPlausibleSpeedMetersPerSecond = 55.0;
const double minElevationGainIncrementMeters = 1.0;
const double barometerSmoothingAlpha = 0.3;
const double minBarometerElevationStepMeters = 3.0;
const int minRecordedRoutePoints = 2;
