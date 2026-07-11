import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_device_support.dart';

void main() {
  tearDown(() => debugDefaultTargetPlatformOverride = null);

  test('android requests the Kotlin service GPS sampling parameters', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    final settings = activityRecordingLocationSettings();

    // Kotlin: requestLocationUpdates(GPS_PROVIDER, 1_000L, 0f, ...).
    expect(settings, isA<AndroidSettings>());
    final android = settings as AndroidSettings;
    expect(android.forceLocationManager, isTrue,
        reason: 'the fused provider would blend in network/wifi fixes, which '
            'Kotlin never accepts and Dart cannot filter out by provider');
    expect(android.intervalDuration, const Duration(seconds: 1));
    expect(android.distanceFilter, 0,
        reason: 'thinning happens in the controller, not the OS');
    expect(android.accuracy, LocationAccuracy.best);
  });

  test('non-android falls back to plain settings', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    final settings = activityRecordingLocationSettings();

    expect(settings, isNot(isA<AndroidSettings>()));
    expect(settings.accuracy, LocationAccuracy.best);
    expect(settings.distanceFilter, 0);
  });
}
