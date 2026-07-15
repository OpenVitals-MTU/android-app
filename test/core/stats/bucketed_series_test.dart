import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/stats/bucketed_series.dart';

typedef _S = ({DateTime time, double value});

void main() {
  final dayStart = DateTime(2026, 1, 1);
  _S s(int minute, double value) =>
      (time: dayStart.add(Duration(minutes: minute)), value: value);

  List<BucketPoint> run(List<_S> samples, int bucketMinutes) => bucketedSeries<_S>(
        samples,
        bucketMinutes: bucketMinutes,
        dayStart: dayStart,
        time: (x) => x.time,
        value: (x) => x.value,
      );

  test('empty input yields no buckets', () {
    expect(run(const [], 5), isEmpty);
  });

  test('non-positive bucket width yields no buckets', () {
    expect(run([s(0, 10)], 0), isEmpty);
    expect(run([s(0, 10)], -5), isEmpty);
  });

  test('computes average, min and max per bucket', () {
    // 0–5 min bucket: 60, 80, 100 → avg 80, min 60, max 100.
    final result = run([s(0, 60), s(2, 80), s(4, 100)], 5);
    expect(result.length, 1);
    expect(result.single.average, closeTo(80, 1e-9));
    expect(result.single.min, 60);
    expect(result.single.max, 100);
    expect(result.single.count, 3);
  });

  test('splits samples into separate buckets and orders them by time', () {
    // one in [0,5), one in [10,15) — the [5,10) bucket is empty and omitted.
    final result = run([s(1, 50), s(12, 70)], 5);
    expect(result.length, 2);
    expect(result[0].time.isBefore(result[1].time), isTrue);
    expect(result[0].average, 50);
    expect(result[1].average, 70);
  });

  test('bucket centre sits in the middle of the window', () {
    final result = run([s(1, 50)], 10);
    expect(result.single.time, dayStart.add(const Duration(minutes: 5)));
  });

  test('skips samples before day start and non-finite values', () {
    final before = (time: dayStart.subtract(const Duration(minutes: 1)), value: 99.0);
    final nan = s(1, double.nan);
    final good = s(2, 40);
    expect(run([before, nan, good], 5).single.average, 40);
  });
}
