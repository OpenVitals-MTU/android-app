import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/devices/core/ble/aggregators/ble_aggregators.dart';
import 'package:openvitals/devices/core/ble/parsers/ble_parsers.dart';

/// Byte-exact port of the Kotlin `BleAggregatorsTest`.
void main() {
  test('heartRateAggregator returns latest value', () {
    final aggregator = BleHeartRateAggregator();
    final now = DateTime.parse('2024-01-01T12:00:00Z');
    aggregator.add(now, 120);
    expect(aggregator.current(now), 120);
  });

  test('cyclingCadenceAggregator computes rpm from crank delta', () {
    final aggregator = BleCyclingCadenceAggregator();
    final t0 = DateTime.parse('2024-01-01T12:00:00Z');
    final t1 = DateTime.parse('2024-01-01T12:00:01Z');
    aggregator.add(
      t0,
      const BleCrankData(crankRevolutionsCount: 10, crankRevolutionsTime: 0),
    );
    aggregator.add(
      t1,
      const BleCrankData(crankRevolutionsCount: 11, crankRevolutionsTime: 1024),
    );
    expect(aggregator.current(t1), 60);
  });

  test('cyclingCadenceAggregator returns zero when crank stops', () {
    final aggregator = BleCyclingCadenceAggregator();
    final t0 = DateTime.parse('2024-01-01T12:00:00Z');
    final t1 = DateTime.parse('2024-01-01T12:00:01Z');
    final t2 = DateTime.parse('2024-01-01T12:00:02Z');
    aggregator.add(
      t0,
      const BleCrankData(crankRevolutionsCount: 10, crankRevolutionsTime: 0),
    );
    aggregator.add(
      t1,
      const BleCrankData(crankRevolutionsCount: 11, crankRevolutionsTime: 1024),
    );
    aggregator.add(
      t2,
      const BleCrankData(crankRevolutionsCount: 11, crankRevolutionsTime: 1024),
    );
    expect(aggregator.current(t2), 0);
  });

  test('cyclingSpeedAggregator computes meters per second', () {
    final aggregator =
        BleCyclingSpeedAggregator(wheelCircumferenceMeters: 2.1);
    final t0 = DateTime.parse('2024-01-01T12:00:00Z');
    final t1 = DateTime.parse('2024-01-01T12:00:01Z');
    aggregator.add(
      t0,
      const BleWheelData(wheelRevolutionsCount: 100, wheelRevolutionsTime: 0),
    );
    aggregator.add(
      t1,
      const BleWheelData(wheelRevolutionsCount: 102, wheelRevolutionsTime: 1024),
    );
    expect(aggregator.current(t1), closeTo(4.2, 0.01));
  });

  test('cyclingSpeedAggregator returns zero when wheel stops', () {
    final aggregator =
        BleCyclingSpeedAggregator(wheelCircumferenceMeters: 2.1);
    final t0 = DateTime.parse('2024-01-01T12:00:00Z');
    final t1 = DateTime.parse('2024-01-01T12:00:01Z');
    final t2 = DateTime.parse('2024-01-01T12:00:02Z');
    aggregator.add(
      t0,
      const BleWheelData(wheelRevolutionsCount: 100, wheelRevolutionsTime: 0),
    );
    aggregator.add(
      t1,
      const BleWheelData(wheelRevolutionsCount: 102, wheelRevolutionsTime: 1024),
    );
    aggregator.add(
      t2,
      const BleWheelData(wheelRevolutionsCount: 102, wheelRevolutionsTime: 1024),
    );
    expect(aggregator.current(t2)!, closeTo(0.0, 0.01));
  });

  test('powerAggregator returns instantaneous power', () {
    final aggregator = BlePowerAggregator();
    final now = DateTime.parse('2024-01-01T12:00:00Z');
    aggregator.add(now, const BleCyclingPowerData(powerWatts: 250, crank: null));
    expect(aggregator.current(now), 250.0);
  });

  test('runningAggregator returns latest speed and cadence', () {
    final aggregator = BleRunningSpeedCadenceAggregator();
    final now = DateTime.parse('2024-01-01T12:00:00Z');
    aggregator.add(
      now,
      const BleRunningSpeedCadenceData(
        speedMetersPerSecond: 3.5,
        cadenceRpm: 90,
      ),
    );
    final current = aggregator.current(now);
    expect(current?.$1, 3.5);
    expect(current?.$2, 90);
  });

  test('aggregator clears stale values', () {
    final aggregator = BleHeartRateAggregator();
    final now = DateTime.parse('2024-01-01T12:00:00Z');
    aggregator.add(now, 120);
    expect(aggregator.current(now.add(const Duration(seconds: 6))), isNull);
  });

  test('speed and cadence aggregators return zero when stale', () {
    final now = DateTime.parse('2024-01-01T12:00:00Z');

    final cadenceAggregator = BleCyclingCadenceAggregator();
    cadenceAggregator.add(
      now,
      const BleCrankData(crankRevolutionsCount: 10, crankRevolutionsTime: 0),
    );
    expect(
      cadenceAggregator.current(now.add(const Duration(seconds: 6))),
      0,
    );

    final speedAggregator =
        BleCyclingSpeedAggregator(wheelCircumferenceMeters: 2.1);
    speedAggregator.add(
      now,
      const BleWheelData(wheelRevolutionsCount: 100, wheelRevolutionsTime: 0),
    );
    expect(
      speedAggregator.current(now.add(const Duration(seconds: 6)))!,
      closeTo(0.0, 0.01),
    );

    final runningAggregator = BleRunningSpeedCadenceAggregator();
    runningAggregator.add(
      now,
      const BleRunningSpeedCadenceData(
        speedMetersPerSecond: 3.5,
        cadenceRpm: 90,
      ),
    );
    final running = runningAggregator.current(now.add(const Duration(seconds: 6)));
    expect(running?.$1, 0.0);
    expect(running?.$2, 0);
  });
}
