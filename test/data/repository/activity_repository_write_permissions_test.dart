import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/model/activity_models.dart';

/// The permissions a save actually needs.
///
/// A recorded activity does not go to Health Connect as one record. The session and
/// every sensor series it carries go in ONE atomic `insertRecords` call, so a series the
/// app never asked permission for does not silently go missing — it takes the whole save
/// down with it. The gate must therefore ask for exactly what the writer will write.

final DateTime _start = DateTime.utc(2026, 7, 14, 18);

class _StubDataSource extends HealthDataSource {}

ActivityWriteRequest _request({
  BleRecordingSampleBuffer bleSamples = const BleRecordingSampleBuffer(),
}) =>
    ActivityWriteRequest(
      exerciseType: 8,
      startTime: _start,
      endTime: _start.add(const Duration(minutes: 30)),
      bleSamples: bleSamples,
    );

void main() {
  final repository = ActivityRepositoryImpl(_StubDataSource());

  test('a bare session asks only for exercise', () {
    final permissions = repository.activityWritePermissionsForRequest(_request());

    expect(permissions, {HcPermissions.writeExercise});
  });

  test('a recording with heart rate asks to write heart rate', () {
    // The bug: it did not. A user who granted WRITE_EXERCISE but not WRITE_HEART_RATE
    // was told the save was permitted, and then the whole insert was thrown.
    final permissions = repository.activityWritePermissionsForRequest(
      _request(
        bleSamples: BleRecordingSampleBuffer(
          heartRateSamples: [
            BleHeartRateSample(time: _start, beatsPerMinute: 150),
          ],
        ),
      ),
    );

    expect(permissions, contains(HcPermissions.writeHeartRate));
    expect(permissions, contains(HcPermissions.writeExercise));
  });

  test('each series is asked for only when it has samples', () {
    final permissions = repository.activityWritePermissionsForRequest(
      _request(
        bleSamples: BleRecordingSampleBuffer(
          heartRateSamples: [
            BleHeartRateSample(time: _start, beatsPerMinute: 150),
          ],
          powerSamples: [BlePowerSample(time: _start, watts: 220)],
        ),
      ),
    );

    // Asked for, because they were recorded.
    expect(permissions, contains(HcPermissions.writeHeartRate));
    expect(permissions, contains(HcPermissions.writePower));
    // NOT asked for: the native writer skips an empty series rather than writing an
    // empty record, so asking would be demanding a permission we will never use.
    expect(permissions, isNot(contains(HcPermissions.writeSpeed)));
    expect(permissions, isNot(contains(HcPermissions.writeCyclingCadence)));
    expect(permissions, isNot(contains(HcPermissions.writeStepsCadence)));
  });
}
