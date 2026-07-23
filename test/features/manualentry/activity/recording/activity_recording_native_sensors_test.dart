import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_native_sensors.dart';

/// The `recording_sensors` channel bridge. Its contract is best-effort: on a
/// host without the native side (tests, iOS) every method resolves to its safe
/// default instead of throwing, so callers need no platform guards — a broken
/// promise here crashes recording setup on non-Android.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('tech.mmarca.openvitals/recording_sensors');
  const sensors = ActivityRecordingNativeSensors();

  final calls = <MethodCall>[];

  void answerWith(Object? Function(MethodCall call) handler) {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return handler(call);
    });
  }

  setUp(calls.clear);

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('with no native side, every query resolves to its safe default',
      () async {
    // No handler registered — the MissingPluginException path.
    expect(await sensors.hasSensor(ActivityRecordingSensor.proximity), isFalse);
    expect(await sensors.hasActivityRecognitionPermission(), isFalse);
    expect(await sensors.requestActivityRecognitionPermission(), isFalse);
    expect(
      await sensors.convertToMsl(
          latitude: 60.17, longitude: 24.94, altitudeMeters: 25.0),
      isNull,
    );
  });

  test('hasSensor asks the platform by sensor type name', () async {
    answerWith((_) => true);

    expect(await sensors.hasSensor(ActivityRecordingSensor.proximity), isTrue);
    expect(
        await sensors.hasSensor(ActivityRecordingSensor.stepDetector), isTrue);

    expect(calls.map((c) => c.arguments['type']),
        ['proximity', 'stepDetector']);
  });

  test('sensor kinds with no Android sensor type are false without asking',
      () async {
    answerWith((_) => true);

    expect(await sensors.hasSensor(ActivityRecordingSensor.gps), isFalse);
    expect(await sensors.hasSensor(ActivityRecordingSensor.ble), isFalse);
    expect(await sensors.hasSensor(ActivityRecordingSensor.none), isFalse);

    expect(calls, isEmpty,
        reason: 'GPS/BLE presence is not a SensorManager question — asking '
            'would get a meaningless answer');
  });

  test('convertToMsl hands the fix through and returns the platform answer',
      () async {
    answerWith((call) => 5.5);

    final msl = await sensors.convertToMsl(
        latitude: 60.17, longitude: 24.94, altitudeMeters: 25.0);

    expect(msl, 5.5);
    expect(calls.single.method, 'convertToMsl');
    expect(calls.single.arguments['latitude'], 60.17);
    expect(calls.single.arguments['altitudeMeters'], 25.0);
  });

  test('a platform error degrades to null, never a crash', () async {
    // API < 34 has no AltitudeConverter; the native side answers with an error.
    answerWith((_) => throw PlatformException(code: 'unsupported'));

    expect(
      await sensors.convertToMsl(
          latitude: 60.17, longitude: 24.94, altitudeMeters: 25.0),
      isNull,
    );
  });
}
