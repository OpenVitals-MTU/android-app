import 'dart:async';

import 'package:flutter/services.dart';

import '../../../../domain/model/activity_entry_types.dart';

/// Platform-channel bridge to the recording hardware `sensors_plus` does not
/// expose: the proximity sensor (push-up counting), the Android step detector
/// (step-based rep counting), the ACTIVITY_RECOGNITION runtime permission that
/// gates the step detector, and the WGS84→MSL altitude conversion.
///
/// Native counterpart: `MainActivity.kt` in the Android embedding, which ports
/// the Kotlin `ActivityRecordingService` sensor listener and the Kotlin
/// controller's `Location.withMslAltitude`.
///
/// Every call is best-effort: on platforms without the channel (tests, iOS)
/// methods resolve to their safe default (false / null) and streams stay
/// silent, so callers never need their own platform guards.
class ActivityRecordingNativeSensors {
  const ActivityRecordingNativeSensors();

  static const MethodChannel _methods =
      MethodChannel('tech.mmarca.openvitals/recording_sensors');
  static const EventChannel _proximityChannel =
      EventChannel('tech.mmarca.openvitals/recording_sensors/proximity');
  static const EventChannel _stepDetectorChannel =
      EventChannel('tech.mmarca.openvitals/recording_sensors/step_detector');

  /// Proximity sensor readings in centimeters, `SENSOR_DELAY_NORMAL`. Kotlin
  /// `Sensor.TYPE_PROXIMITY` events (the native side also sends the receipt
  /// timestamp, but the recognizer only stores it informationally, so the
  /// subscriber's clock is equivalent).
  Stream<double> proximityEvents() => _proximityChannel
      .receiveBroadcastStream()
      .map((event) => ((event as Map)['value'] as num).toDouble());

  /// One event per detected step, as the receipt time in epoch millis —
  /// Kotlin's `System.currentTimeMillis()` capture in `onSensorChanged` for
  /// `Sensor.TYPE_STEP_DETECTOR`.
  Stream<int> stepDetectorEvents() => _stepDetectorChannel
      .receiveBroadcastStream()
      .map((event) => (event as num).toInt());

  /// Kotlin `sensorManager.getDefaultSensor(type) != null`. Only the sensor
  /// kinds with an Android sensor type can be queried; the rest are false.
  Future<bool> hasSensor(ActivityRecordingSensor sensor) async {
    final type = switch (sensor) {
      ActivityRecordingSensor.proximity => 'proximity',
      ActivityRecordingSensor.stepDetector => 'stepDetector',
      ActivityRecordingSensor.accelerometer => 'accelerometer',
      ActivityRecordingSensor.gps ||
      ActivityRecordingSensor.ble ||
      ActivityRecordingSensor.none =>
        null,
    };
    if (type == null) return false;
    return await _invoke<bool>('hasSensor', {'type': type}) ?? false;
  }

  /// Kotlin `ActivityRecordingController.hasActivityRecognitionPermission`
  /// (always true below Android 10, where the permission does not exist).
  Future<bool> hasActivityRecognitionPermission() async =>
      await _invoke<bool>('hasActivityRecognitionPermission') ?? false;

  Future<bool> requestActivityRecognitionPermission() async =>
      await _invoke<bool>('requestActivityRecognitionPermission') ?? false;

  /// Kotlin `Location.withMslAltitude`: the mean-sea-level altitude for a
  /// WGS84 ellipsoid fix via `AltitudeConverter` (API 34+), or null when the
  /// platform cannot convert. Called per fix, like Kotlin — no caching.
  Future<double?> convertToMsl({
    required double latitude,
    required double longitude,
    required double altitudeMeters,
  }) =>
      _invoke<double>('convertToMsl', {
        'latitude': latitude,
        'longitude': longitude,
        'altitudeMeters': altitudeMeters,
      });

  Future<T?> _invoke<T>(String method, [Object? arguments]) async {
    try {
      return await _methods.invokeMethod<T>(method, arguments);
    } on PlatformException {
      return null;
    } on MissingPluginException {
      return null;
    }
  }
}
