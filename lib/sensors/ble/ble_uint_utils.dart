/// Pure port of the Kotlin `BleUintUtils` — unsigned-integer rollover helpers
/// used by the cadence/speed aggregators. No `flutter_blue_plus` dependency.
class BleUintUtils {
  const BleUintUtils._();

  static const int uint16Max = 0xFFFF;
  static const int uint32Max = 0xFFFFFFFF;

  /// Difference `a - b` on a `uint`-ranged counter, handling wrap-around: when
  /// `a < b` the counter is assumed to have rolled over `uintMax`.
  static int diff(int a, int b, int uintMax) {
    if (a < 0 || b < 0) {
      throw ArgumentError('Values must be non-negative');
    }
    if (a > uintMax || b > uintMax) {
      throw ArgumentError('Values outside uint range');
    }
    if (a >= b) {
      return a - b;
    }
    return (uintMax + 1 - b) + a;
  }
}
