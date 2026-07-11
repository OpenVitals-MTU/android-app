import 'package:freezed_annotation/freezed_annotation.dart';

part 'body_models.freezed.dart';

enum BodyMeasurementType {
  weight('WEIGHT'),
  height('HEIGHT'),
  bodyFat('BODY_FAT');

  const BodyMeasurementType(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static BodyMeasurementType? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class BodyMeasurementWriteRequest with _$BodyMeasurementWriteRequest {
  const factory BodyMeasurementWriteRequest({
    required BodyMeasurementType type,
    required DateTime time,
    required double value,
  }) = _BodyMeasurementWriteRequest;
}

@freezed
abstract class WeightEntry with _$WeightEntry {
  const factory WeightEntry({
    required DateTime time,
    required double weightKg,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _WeightEntry;
}

@freezed
abstract class HeightEntry with _$HeightEntry {
  const factory HeightEntry({
    required DateTime time,
    required double heightCm,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _HeightEntry;
}

@freezed
abstract class BodyFatEntry with _$BodyFatEntry {
  const factory BodyFatEntry({
    required DateTime time,
    required double percent,
    required String source,
    @Default('') String id,
    @Default(false) bool isOpenVitalsEntry,
  }) = _BodyFatEntry;
}

@freezed
abstract class LeanBodyMassEntry with _$LeanBodyMassEntry {
  const factory LeanBodyMassEntry({
    required DateTime time,
    required double massKg,
    required String source,
  }) = _LeanBodyMassEntry;
}

@freezed
abstract class BmrEntry with _$BmrEntry {
  const factory BmrEntry({
    required DateTime time,
    required double kcalPerDay,
    required String source,
  }) = _BmrEntry;
}

@freezed
abstract class BoneMassEntry with _$BoneMassEntry {
  const factory BoneMassEntry({
    required DateTime time,
    required double massKg,
    required String source,
  }) = _BoneMassEntry;
}

@freezed
abstract class BodyWaterMassEntry with _$BodyWaterMassEntry {
  const factory BodyWaterMassEntry({
    required DateTime time,
    required double massKg,
    required String source,
  }) = _BodyWaterMassEntry;
}

@freezed
abstract class BodyMeasurementEntry with _$BodyMeasurementEntry {
  const factory BodyMeasurementEntry({
    required String id,
    required BodyMeasurementType type,
    required DateTime time,
    required double value,
    required String source,
    required bool isOpenVitalsEntry,
  }) = _BodyMeasurementEntry;
}
