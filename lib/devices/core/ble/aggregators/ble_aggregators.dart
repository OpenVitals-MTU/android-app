import '../ble_uint_utils.dart';
import '../parsers/ble_parsers.dart';

/// Pure port of the Kotlin `aggregators/BleAggregators.kt`. Converts the raw
/// per-notification parser outputs (revolution counts + event times, or
/// instantaneous values) into displayable metrics, handling `uint` rollover and
/// staleness. No `flutter_blue_plus` dependency.

/// The running-speed/cadence aggregator emits a `(speed, cadence)` record,
/// mirroring the Kotlin `Pair<Double?, Long?>`.
typedef BleRunningOutput = (double?, int?);

class BleAggregatedSample<T> {
  const BleAggregatedSample({required this.value, required this.receivedAt});

  final T value;
  final DateTime receivedAt;
}

/// Base sliding-window aggregator. Subclasses implement [computeValue] to set
/// [output] from the current (and, via [previousValue]/[previousTime], previous)
/// sample. [current] decays to [_staleOutput] once samples stop arriving.
abstract class BleSampleAggregator<Input, Output> {
  BleSampleAggregator([this._staleOutput]);

  final Output? _staleOutput;

  static const Duration _maxAge = Duration(seconds: 5);

  (DateTime, Input)? _previous;
  DateTime? _lastReceivedAt;

  /// The last computed output. Subclasses assign to it from [computeValue].
  Output? output;

  void computeValue(DateTime now, Input current);

  void add(DateTime now, Input current) {
    computeValue(now, current);
    _previous = (now, current);
    _lastReceivedAt = now;
  }

  Output? current([DateTime? now]) {
    final receivedAt = _lastReceivedAt;
    if (receivedAt == null) return null;
    final effectiveNow = now ?? DateTime.now();
    if (effectiveNow.difference(receivedAt) > _maxAge) {
      output = _staleOutput;
    }
    return output;
  }

  void reset() {
    _previous = null;
    output = null;
    _lastReceivedAt = null;
  }

  Input? previousValue() => _previous?.$2;

  DateTime? previousTime() => _previous?.$1;
}

class BleHeartRateAggregator extends BleSampleAggregator<int, int> {
  @override
  void computeValue(DateTime now, int current) {
    output = current;
  }
}

class BlePowerAggregator extends BleSampleAggregator<BleCyclingPowerData, double> {
  @override
  void computeValue(DateTime now, BleCyclingPowerData current) {
    output = current.powerWatts.toDouble();
  }
}

class BleCyclingCadenceAggregator extends BleSampleAggregator<BleCrankData, int> {
  BleCyclingCadenceAggregator() : super(0);

  @override
  void computeValue(DateTime now, BleCrankData current) {
    final previous = previousValue();
    if (previous == null) return;
    final timeDiffMs = BleUintUtils.diff(
          current.crankRevolutionsTime,
          previous.crankRevolutionsTime,
          BleUintUtils.uint16Max,
        ) /
        1024.0 *
        1000.0;
    if (timeDiffMs <= 0.0) {
      output = 0;
      return;
    }
    if (current.crankRevolutionsCount < previous.crankRevolutionsCount) return;
    final crankDiff = BleUintUtils.diff(
      current.crankRevolutionsCount,
      previous.crankRevolutionsCount,
      BleUintUtils.uint32Max,
    );
    final value = (crankDiff / (timeDiffMs / 60000.0)).toInt();
    output = value < 0 ? 0 : value;
  }
}

class BleCyclingSpeedAggregator extends BleSampleAggregator<BleWheelData, double> {
  BleCyclingSpeedAggregator({required this.wheelCircumferenceMeters})
      : super(0.0);

  double wheelCircumferenceMeters;

  void setWheelCircumferenceMeters(double value) {
    wheelCircumferenceMeters = value;
  }

  @override
  void computeValue(DateTime now, BleWheelData current) {
    final previous = previousValue();
    if (previous == null) return;
    final timeDiffMs = BleUintUtils.diff(
          current.wheelRevolutionsTime,
          previous.wheelRevolutionsTime,
          BleUintUtils.uint16Max,
        ) /
        1024.0 *
        1000.0;
    if (timeDiffMs <= 0.0) {
      output = 0.0;
      return;
    }
    if (current.wheelRevolutionsCount < previous.wheelRevolutionsCount) return;
    final wheelDiff = BleUintUtils.diff(
      current.wheelRevolutionsCount,
      previous.wheelRevolutionsCount,
      BleUintUtils.uint32Max,
    );
    output = wheelCircumferenceMeters * wheelDiff / (timeDiffMs / 1000.0);
  }
}

class BleRunningSpeedCadenceAggregator
    extends BleSampleAggregator<BleRunningSpeedCadenceData, BleRunningOutput> {
  BleRunningSpeedCadenceAggregator() : super((0.0, 0));

  @override
  void computeValue(DateTime now, BleRunningSpeedCadenceData current) {
    output = (current.speedMetersPerSecond, current.cadenceRpm);
  }
}
