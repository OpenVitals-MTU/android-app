import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/heart_rate_sample_reduction.dart';

void main() {
  test('reducedForChart keeps small lists unchanged', () {
    final samples = [
      HeartRateSample(
        time: DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
        beatsPerMinute: 60,
        source: 'test',
      ),
      HeartRateSample(
        time: DateTime.fromMillisecondsSinceEpoch(60000, isUtc: true),
        beatsPerMinute: 70,
        source: 'test',
      ),
    ];

    expect(samples.reducedForChart(), samples);
  });

  test('reducedForChart caps large lists', () {
    final samples = List.generate(
      10000,
      (index) => HeartRateSample(
        time: DateTime.fromMillisecondsSinceEpoch(index * 1000, isUtc: true),
        beatsPerMinute: 60 + index % 40,
        source: 'test',
      ),
    );

    final reduced = samples.reducedForChart(maxSamples: 100);

    expect(reduced.length, 100);
    expect(reduced.first.time.compareTo(reduced.last.time) <= 0, isTrue);
  });
}
