import '../ble_uuids.dart';

/// Pure port of the Kotlin `parsers/BleParsers.kt`. All functions operate on raw
/// GATT payload bytes (`List<int>`, little-endian, each element 0..255) so this
/// file has NO `flutter_blue_plus` dependency — the coordinator feeds it the
/// bytes delivered by characteristic notifications.

class BleCrankData {
  const BleCrankData({
    required this.crankRevolutionsCount,
    required this.crankRevolutionsTime,
  });

  final int crankRevolutionsCount;
  final int crankRevolutionsTime;
}

class BleWheelData {
  const BleWheelData({
    required this.wheelRevolutionsCount,
    required this.wheelRevolutionsTime,
  });

  final int wheelRevolutionsCount;
  final int wheelRevolutionsTime;
}

class BleCyclingPowerData {
  const BleCyclingPowerData({
    required this.powerWatts,
    required this.crank,
  });

  final int powerWatts;
  final BleCrankData? crank;
}

class BleRunningSpeedCadenceData {
  const BleRunningSpeedCadenceData({
    required this.speedMetersPerSecond,
    required this.cadenceRpm,
  });

  final double? speedMetersPerSecond;
  final int? cadenceRpm;
}

/// Heart Rate Measurement (0x2A37) parser.
class BleHeartRateParser {
  const BleHeartRateParser._();

  /// Returns the heart rate (bpm) or `null` when the payload carries no beats
  /// (empty, zero, or unparsable).
  static int? parseBytes(List<int> raw) {
    if (raw.isEmpty) return null;
    final formatUint16 = (raw[0] & 0x1) == 1;
    int? parsed;
    if (formatUint16 && raw.length >= 3) {
      parsed = ((raw[2] & 0xFF) << 8) | (raw[1] & 0xFF);
    } else if (raw.length >= 2) {
      parsed = raw[1] & 0xFF;
    } else if (raw.length == 1) {
      parsed = raw[0] & 0xFF;
    } else {
      parsed = null;
    }
    if (parsed == null) return null;
    return parsed > 0 ? parsed : null;
  }

  /// A `uint8`-format payload whose measurement byte is 0 — a "connected but no
  /// contact" signal from the strap.
  static bool isZeroSignal(List<int> raw) =>
      raw.length >= 2 && (raw[0] & 0x1) == 0 && (raw[1] & 0xFF) == 0;

  static bool supports(BleServiceMeasurementUuid serviceMeasurement) =>
      serviceMeasurement.measurementUuid == BleUuids.heartRate.measurementUuid;
}

/// Cycling Power Measurement (0x2A63) parser.
class BleCyclingPowerParser {
  const BleCyclingPowerParser._();

  static BleCyclingPowerData? parsePayload(List<int> raw) {
    if (raw.isEmpty) return null;
    var index = 0;
    final flags1 = raw[index++] & 0xFF;
    index++; // second flags byte, unused
    final hasPedalPowerBalance = (flags1 & 0x01) > 0;
    final hasAccumulatedTorque = (flags1 & 0x04) > 0;
    final hasWheel = (flags1 & 16) > 0;
    final hasCrank = (flags1 & 32) > 0;
    final power = _readInt16(raw, index);
    if (power == null) return null;
    index += 2;
    if (hasPedalPowerBalance) index += 1;
    if (hasAccumulatedTorque) index += 2;
    if (hasWheel) index += 4;
    BleCrankData? crank;
    if (hasCrank && raw.length - index >= 4) {
      final crankCount = _readUint16(raw, index);
      if (crankCount == null) return null;
      index += 2;
      final crankTime = _readUint16(raw, index);
      if (crankTime == null) return null;
      crank = BleCrankData(
        crankRevolutionsCount: crankCount,
        crankRevolutionsTime: crankTime,
      );
    }
    return BleCyclingPowerData(powerWatts: power, crank: crank);
  }
}

/// CSC Measurement (0x2A5B) parser. Returns a `(wheel, crank)` record, either of
/// which may be `null` depending on the flags byte.
class BleCyclingSpeedCadenceParser {
  const BleCyclingSpeedCadenceParser._();

  static (BleWheelData?, BleCrankData?)? parsePayload(List<int> raw) {
    if (raw.isEmpty) return null;
    final flags = raw[0] & 0xFF;
    final hasWheel = (flags & 0x01) > 0;
    final hasCrank = (flags & 0x02) > 0;
    var index = 1;
    BleWheelData? wheel;
    if (hasWheel && raw.length - index >= 6) {
      final wheelCount = _readUint32(raw, index);
      if (wheelCount == null) return null;
      index += 4;
      final wheelTime = _readUint16(raw, index);
      if (wheelTime == null) return null;
      index += 2;
      wheel = BleWheelData(
        wheelRevolutionsCount: wheelCount,
        wheelRevolutionsTime: wheelTime,
      );
    }
    BleCrankData? crank;
    if (hasCrank && raw.length - index >= 4) {
      final crankCount = _readUint16(raw, index);
      if (crankCount == null) return null;
      index += 2;
      final crankTime = _readUint16(raw, index);
      if (crankTime == null) return null;
      crank = BleCrankData(
        crankRevolutionsCount: crankCount,
        crankRevolutionsTime: crankTime,
      );
    }
    return (wheel, crank);
  }
}

/// RSC Measurement (0x2A53) parser. [sensorName] is used to special-case the
/// Wahoo TICKR X, which reports double the true cadence.
class BleRunningSpeedCadenceParser {
  const BleRunningSpeedCadenceParser._();

  static BleRunningSpeedCadenceData? parsePayload(
    List<int> raw,
    String? sensorName,
  ) {
    if (raw.isEmpty) return null;
    var index = 1;
    double? speed;
    if (raw.length - index >= 2) {
      final value = _readUint16(raw, index);
      speed = value == null ? null : value / 256.0;
    }
    index = 3;
    int? cadence;
    if (raw.length - index >= 1) {
      cadence = raw[index] & 0xFF;
    }
    if (sensorName != null &&
        sensorName.startsWith('TICKR X') &&
        cadence != null) {
      cadence = cadence ~/ 2;
    }
    return BleRunningSpeedCadenceData(
      speedMetersPerSecond: speed,
      cadenceRpm: cadence,
    );
  }
}

/// Signed 16-bit little-endian read (used for power, which can be negative).
int? _readInt16(List<int> raw, int index) {
  if (index + 1 >= raw.length) return null;
  final value = ((raw[index + 1] & 0xFF) << 8) | (raw[index] & 0xFF);
  return value >= 0x8000 ? value - 0x10000 : value;
}

/// Unsigned 16-bit little-endian read.
int? _readUint16(List<int> raw, int index) {
  if (index + 1 >= raw.length) return null;
  return ((raw[index + 1] & 0xFF) << 8) | (raw[index] & 0xFF);
}

/// Unsigned 32-bit little-endian read.
int? _readUint32(List<int> raw, int index) {
  if (index + 3 >= raw.length) return null;
  return ((raw[index + 3] & 0xFF) << 24) |
      ((raw[index + 2] & 0xFF) << 16) |
      ((raw[index + 1] & 0xFF) << 8) |
      (raw[index] & 0xFF);
}
