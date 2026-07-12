import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

import '../../../../domain/preferences/activity_recording_preferences.dart';
import '../../../../domain/model/activity_entry_types.dart';
import 'activity_recording.dart';
import 'activity_recording_native_sensors.dart';

/// The device capabilities the recording setup screen has to ask about, behind
/// a seam so widget tests never touch a platform channel. Port of the Kotlin
/// permission/sensor helpers in `ActivityRecordingSetupScreen.kt` and
/// `ActivityRecordingLocationSupport.kt`.

/// Kotlin `ActivityGpsFixQuality`.
class ActivityGpsFixQuality {
  const ActivityGpsFixQuality({
    required this.isPrecise,
    required this.accuracyMeters,
    required this.locationTime,
  });

  final bool isPrecise;
  final double? accuracyMeters;
  final DateTime? locationTime;
}

/// A location old enough that it no longer describes where you are standing.
/// Kotlin `MaxLocationAgeMillis`.
const int kMaxLocationAgeMillis = 10000;

/// Kotlin `MaxLocationFutureSkewSeconds`: tolerate a clock slightly ahead.
const int kMaxLocationFutureSkewSeconds = 5;

/// Kotlin `PreRecordingGpsIntervalMillis`.
const Duration kPreRecordingGpsInterval = Duration(seconds: 1);

/// Kotlin `ActivityRecordingService.GpsLostTimeoutMillis`: without any fix for
/// this long, the recording's GPS status flips to LOST.
const int kGpsLostTimeoutMillis = 30000;

/// Kotlin `ActivityRecordingService.LocationSamplingIntervalMillis`.
const Duration kLocationSamplingInterval = Duration(seconds: 1);

/// The location request both the pre-start GPS indicator and the recording
/// stream use, matching the Kotlin service's
/// `requestLocationUpdates(GPS_PROVIDER, 1_000L, 0f, …)`.
///
/// `forceLocationManager` pins the raw GPS provider. Without it geolocator
/// prefers Google's fused provider, which fuses in network/wifi fixes — those
/// pass the accuracy gate but are not the satellite fixes Kotlin filters on,
/// and Dart cannot inspect a `Position`'s provider to reject them. On a device
/// with no Play Services (GrapheneOS, F-Droid builds) geolocator already falls
/// back to `LocationManager`; this makes the choice explicit everywhere.
///
/// The distance filter stays 0: sample thinning happens in the controller via
/// `minimumSampleDistanceMeters`, exactly as it does in Kotlin.
LocationSettings activityRecordingLocationSettings() {
  if (defaultTargetPlatform != TargetPlatform.android) {
    return const LocationSettings(
      accuracy: LocationAccuracy.best,
      distanceFilter: 0,
    );
  }
  return AndroidSettings(
    accuracy: LocationAccuracy.best,
    distanceFilter: 0,
    forceLocationManager: true,
    intervalDuration: kLocationSamplingInterval,
  );
}

/// Kotlin `ActivityRecordingPreferences.DefaultRequiredGpsAccuracyMeters`, as a
/// double so it can be a default argument.
const double kDefaultRequiredGpsAccuracyMeters =
    ActivityRecordingPreferences.defaultRequiredGpsAccuracyMeters * 1.0;

/// Kotlin `Location.activityGpsFixQuality()`.
ActivityGpsFixQuality activityGpsFixQuality(
  Position position, {
  DateTime? startTime,
  required DateTime now,
  double requiredAccuracyMeters = kDefaultRequiredGpsAccuracyMeters,
}) {
  final accuracy = position.accuracy > 0 ? position.accuracy : null;
  final locationTime = position.timestamp.toUtc();
  final ageMillis = now.difference(locationTime).inMilliseconds;
  final isPrecise = accuracy != null &&
      accuracy <= requiredAccuracyMeters &&
      ageMillis <= kMaxLocationAgeMillis &&
      (startTime == null || locationTime.isAfter(startTime)) &&
      !locationTime
          .isAfter(now.add(const Duration(seconds: kMaxLocationFutureSkewSeconds)));
  return ActivityGpsFixQuality(
    isPrecise: isPrecise,
    accuracyMeters: accuracy,
    locationTime: locationTime,
  );
}

/// Kotlin `RecordingSensorReadiness`.
class RecordingSensorReadiness {
  const RecordingSensorReadiness({
    required this.hasRequiredSensor,
    required this.hasActivityRecognitionPermission,
  });

  final bool hasRequiredSensor;
  final bool hasActivityRecognitionPermission;
}

/// Kotlin `PreRecordingGpsFixState`.
class PreRecordingGpsFixState {
  const PreRecordingGpsFixState({
    this.hasPrecisePermission = false,
    this.gpsProviderEnabled = false,
    this.latestPosition,
    this.fixQuality,
  });

  final bool hasPrecisePermission;
  final bool gpsProviderEnabled;
  final Position? latestPosition;
  final ActivityGpsFixQuality? fixQuality;

  /// The most recent fix, but only when it is actually trustworthy — the value
  /// Kotlin hands to `onStartRecording` as the session's initial fix.
  Position? get latestPreciseFix {
    final position = latestPosition;
    if (position == null) return null;
    if (!hasPrecisePermission || !gpsProviderEnabled) return null;
    return fixQuality?.isPrecise == true ? position : null;
  }

  ActivityRecordingInitialFix? get initialFix {
    final position = latestPreciseFix;
    if (position == null) return null;
    return ActivityRecordingInitialFix(
      latitude: position.latitude,
      longitude: position.longitude,
      accuracyMeters: position.accuracy > 0 ? position.accuracy : null,
      altitudeMeters: position.altitude,
      timeMillis: position.timestamp.millisecondsSinceEpoch,
    );
  }
}

/// Everything the setup screen needs to know about this device.
abstract interface class ActivityRecordingDeviceSupport {
  /// Kotlin `hasActivityRecordingPreciseLocationPermission`.
  Future<bool> hasPreciseLocationPermission();

  /// Kotlin `activityRecordingLocationPermissions()` launcher.
  Future<bool> requestPreciseLocationPermission();

  /// Kotlin `Context.isGpsProviderEnabled()`.
  Future<bool> isLocationServiceEnabled();

  /// Kotlin `hasActivityRecordingNotificationPermission`. The recording runs in
  /// a foreground service, which cannot post its notification without this.
  Future<bool> hasNotificationPermission();

  Future<bool> requestNotificationPermission();

  /// Kotlin `ActivityRecordingController.hasActivityRecognitionPermission`.
  Future<bool> hasActivityRecognitionPermission();

  Future<bool> requestActivityRecognitionPermission();

  /// Whether the sensor this activity type counts with exists and can be read.
  Future<bool> hasSensorFor(ActivityEntryType activityType);

  /// A 1 Hz stream of the best current fix, for the pre-start GPS indicator.
  Stream<Position> watchPosition();

  Future<Position?> lastKnownPosition();
}

class DefaultActivityRecordingDeviceSupport
    implements ActivityRecordingDeviceSupport {
  const DefaultActivityRecordingDeviceSupport(
      [this._nativeSensors = const ActivityRecordingNativeSensors()]);

  /// Channel calls happen only inside the async methods below, never at
  /// construction, so this class stays constructible in widget tests.
  final ActivityRecordingNativeSensors _nativeSensors;

  @override
  Future<bool> hasPreciseLocationPermission() async {
    final permission = await Geolocator.checkPermission();
    return permission == LocationPermission.always ||
        permission == LocationPermission.whileInUse;
  }

  @override
  Future<bool> requestPreciseLocationPermission() async {
    final permission = await Geolocator.requestPermission();
    return permission == LocationPermission.always ||
        permission == LocationPermission.whileInUse;
  }

  @override
  Future<bool> isLocationServiceEnabled() => Geolocator.isLocationServiceEnabled();

  @override
  Future<bool> hasNotificationPermission() async =>
      await FlutterForegroundTask.checkNotificationPermission() ==
      NotificationPermission.granted;

  @override
  Future<bool> requestNotificationPermission() async =>
      await FlutterForegroundTask.requestNotificationPermission() ==
      NotificationPermission.granted;

  /// Kotlin `ActivityRecordingController.hasActivityRecognitionPermission`,
  /// via the native recording-sensors channel (ACTIVITY_RECOGNITION gates the
  /// step detector).
  @override
  Future<bool> hasActivityRecognitionPermission() =>
      _nativeSensors.hasActivityRecognitionPermission();

  @override
  Future<bool> requestActivityRecognitionPermission() =>
      _nativeSensors.requestActivityRecognitionPermission();

  /// Kotlin `rememberRecordingSensorReadiness`'s sensor query. Proximity and
  /// step-detector availability comes from `SensorManager.getDefaultSensor`
  /// through the native channel (`sensors_plus` exposes neither), and a GPS
  /// type that counts steps (walking) needs the step detector too. The
  /// accelerometer — which Kotlin also queries — is reported present without a
  /// platform call: `sensors_plus` provides its stream everywhere this app
  /// runs, and the static answer keeps accelerometer-only setups (pull-ups,
  /// rope skipping) working in widget tests where no channel exists.
  @override
  Future<bool> hasSensorFor(ActivityEntryType activityType) async =>
      switch (activityType.recordingSensor) {
        ActivityRecordingSensor.proximity ||
        ActivityRecordingSensor.stepDetector =>
          await _nativeSensors.hasSensor(activityType.recordingSensor),
        ActivityRecordingSensor.accelerometer => true,
        ActivityRecordingSensor.ble => true,
        ActivityRecordingSensor.gps ||
        ActivityRecordingSensor.none =>
          !activityType.supportsStepCounting ||
              await _nativeSensors
                  .hasSensor(ActivityRecordingSensor.stepDetector),
      };

  @override
  Stream<Position> watchPosition() => Geolocator.getPositionStream(
        locationSettings: activityRecordingLocationSettings(),
      );

  @override
  Future<Position?> lastKnownPosition() => Geolocator.getLastKnownPosition();
}

final activityRecordingDeviceSupportProvider =
    Provider<ActivityRecordingDeviceSupport>(
  (ref) => const DefaultActivityRecordingDeviceSupport(),
);

/// Kotlin `rememberRecordingSensorReadiness`.
final recordingSensorReadinessProvider = FutureProvider.autoDispose
    .family<RecordingSensorReadiness, ActivityEntryType>((ref, type) async {
  final support = ref.watch(activityRecordingDeviceSupportProvider);
  final results = await Future.wait([
    support.hasSensorFor(type),
    support.hasActivityRecognitionPermission(),
  ]);
  return RecordingSensorReadiness(
    hasRequiredSensor: results[0],
    hasActivityRecognitionPermission: results[1],
  );
});

/// Kotlin `rememberPreRecordingGpsFixState`: polls permission + provider state
/// once a second while [enabled], and streams positions in between.
///
/// Emits [PreRecordingGpsFixState] rather than a raw position so the screen can
/// distinguish "no permission", "GPS off" and "fix not precise enough yet".
final preRecordingGpsFixProvider = StreamProvider.autoDispose
    .family<PreRecordingGpsFixState, bool>((ref, enabled) async* {
  if (!enabled) {
    yield const PreRecordingGpsFixState();
    return;
  }
  final support = ref.watch(activityRecordingDeviceSupportProvider);

  final hasPermission = await support.hasPreciseLocationPermission();
  if (!hasPermission) {
    yield const PreRecordingGpsFixState();
    return;
  }

  var serviceEnabled = await support.isLocationServiceEnabled();
  Position? latest = serviceEnabled ? await support.lastKnownPosition() : null;

  PreRecordingGpsFixState snapshot() {
    final position = latest;
    return PreRecordingGpsFixState(
      hasPrecisePermission: true,
      gpsProviderEnabled: serviceEnabled,
      latestPosition: position,
      fixQuality: position == null
          ? null
          : activityGpsFixQuality(position, now: DateTime.now().toUtc()),
    );
  }

  yield snapshot();
  if (!serviceEnabled) return;

  final subscription = support.watchPosition().listen(
    (position) => latest = position,
    onError: (Object _) => serviceEnabled = false,
  );
  ref.onDispose(subscription.cancel);

  // Re-evaluate once a second even when no position arrives: a fix goes stale
  // on its own, so `isPrecise` has to be able to fall back to false. Kotlin's
  // `LaunchedEffect` ticks on the same interval for the same reason.
  yield* Stream<void>.periodic(kPreRecordingGpsInterval).asyncMap((_) async {
    serviceEnabled = await support.isLocationServiceEnabled();
    return snapshot();
  });
});
