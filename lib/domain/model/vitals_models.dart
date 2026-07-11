import 'package:freezed_annotation/freezed_annotation.dart';

part 'vitals_models.freezed.dart';

@freezed
abstract class BloodPressureEntry with _$BloodPressureEntry {
  const factory BloodPressureEntry({
    required DateTime time,
    required int systolicMmHg,
    required int diastolicMmHg,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _BloodPressureEntry;
}

@freezed
abstract class SpO2Entry with _$SpO2Entry {
  const factory SpO2Entry({
    required DateTime time,
    required double percent,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _SpO2Entry;
}

@freezed
abstract class RespiratoryRateEntry with _$RespiratoryRateEntry {
  const factory RespiratoryRateEntry({
    required DateTime time,
    required double breathsPerMinute,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _RespiratoryRateEntry;
}

@freezed
abstract class BodyTempEntry with _$BodyTempEntry {
  const factory BodyTempEntry({
    required DateTime time,
    required double temperatureCelsius,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _BodyTempEntry;
}

@freezed
abstract class BloodGlucoseEntry with _$BloodGlucoseEntry {
  const factory BloodGlucoseEntry({
    required DateTime time,
    required double millimolesPerLiter,
    required int specimenSource,
    required int mealType,
    required int relationToMeal,
    required String source,
  }) = _BloodGlucoseEntry;
}

@freezed
abstract class SkinTemperatureEntry with _$SkinTemperatureEntry {
  const SkinTemperatureEntry._();

  const factory SkinTemperatureEntry({
    required DateTime startTime,
    required DateTime endTime,
    required double? baselineCelsius,
    required double? averageDeltaCelsius,
    required double? minDeltaCelsius,
    required double? maxDeltaCelsius,
    required int measurementLocation,
    required String source,
  }) = _SkinTemperatureEntry;

  DateTime get time => endTime;
}

@freezed
abstract class Vo2MaxEntry with _$Vo2MaxEntry {
  const factory Vo2MaxEntry({
    required DateTime time,
    required double vo2MaxMlPerKgPerMin,
    required String source,
  }) = _Vo2MaxEntry;
}

enum VitalsMeasurementType {
  bloodPressure('BLOOD_PRESSURE'),
  spo2('SPO2'),
  respiratoryRate('RESPIRATORY_RATE'),
  bodyTemperature('BODY_TEMPERATURE');

  const VitalsMeasurementType(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static VitalsMeasurementType? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class VitalsMeasurementWriteRequest
    with _$VitalsMeasurementWriteRequest {
  const factory VitalsMeasurementWriteRequest({
    required VitalsMeasurementType type,
    required DateTime time,
    required double value,
    double? secondaryValue,
  }) = _VitalsMeasurementWriteRequest;
}

@freezed
abstract class VitalsMeasurementEntry with _$VitalsMeasurementEntry {
  const factory VitalsMeasurementEntry({
    required String id,
    required VitalsMeasurementType type,
    required DateTime time,
    required double value,
    double? secondaryValue,
    required String source,
    required bool isOpenVitalsEntry,
  }) = _VitalsMeasurementEntry;
}
