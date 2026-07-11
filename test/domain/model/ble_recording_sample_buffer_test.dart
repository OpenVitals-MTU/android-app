import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

void main() {
  test('trimmed keeps latest samples per series', () {
    final start = DateTime.parse('2024-01-01T12:00:00Z');
    var buffer = const BleRecordingSampleBuffer();
    for (var index = 0; index < 5; index++) {
      buffer = buffer.withHeartRateSample(
        start.add(Duration(seconds: index)),
        100 + index,
      );
    }
    final trimmed = buffer.trimmed(maxSamplesPerSeries: 3);
    expect(trimmed.heartRateSamples.length, 3);
    expect(trimmed.heartRateSamples.last.beatsPerMinute, 104);
  });

  test('isEmpty when no samples', () {
    expect(const BleRecordingSampleBuffer().isEmpty(), isTrue);
  });
}
