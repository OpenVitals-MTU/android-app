import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/vitals_models.dart';

part 'vitals_period_data.freezed.dart';

@freezed
abstract class VitalsPeriodData with _$VitalsPeriodData {
  const factory VitalsPeriodData({
    @Default(<String>{}) Set<String> missingVitalsPermissions,
    @Default(<BloodPressureEntry>[]) List<BloodPressureEntry> bloodPressure,
    @Default(<BloodPressureEntry>[])
    List<BloodPressureEntry> previousBloodPressure,
    @Default(<BloodPressureEntry>[])
    List<BloodPressureEntry> baselineBloodPressure,
    @Default(<SpO2Entry>[]) List<SpO2Entry> spO2,
    @Default(<SpO2Entry>[]) List<SpO2Entry> previousSpO2,
    @Default(<SpO2Entry>[]) List<SpO2Entry> baselineSpO2,
    @Default(<RespiratoryRateEntry>[])
    List<RespiratoryRateEntry> respiratoryRate,
    @Default(<RespiratoryRateEntry>[])
    List<RespiratoryRateEntry> previousRespiratoryRate,
    @Default(<RespiratoryRateEntry>[])
    List<RespiratoryRateEntry> baselineRespiratoryRate,
    @Default(<BodyTempEntry>[]) List<BodyTempEntry> bodyTemperature,
    @Default(<BodyTempEntry>[]) List<BodyTempEntry> previousBodyTemperature,
    @Default(<BodyTempEntry>[]) List<BodyTempEntry> baselineBodyTemperature,
    @Default(<Vo2MaxEntry>[]) List<Vo2MaxEntry> vo2Max,
    @Default(<Vo2MaxEntry>[]) List<Vo2MaxEntry> previousVo2Max,
    @Default(<Vo2MaxEntry>[]) List<Vo2MaxEntry> baselineVo2Max,
    @Default(<BloodGlucoseEntry>[]) List<BloodGlucoseEntry> bloodGlucose,
    @Default(<BloodGlucoseEntry>[]) List<BloodGlucoseEntry> previousBloodGlucose,
    @Default(<BloodGlucoseEntry>[]) List<BloodGlucoseEntry> baselineBloodGlucose,
    @Default(<SkinTemperatureEntry>[])
    List<SkinTemperatureEntry> skinTemperature,
    @Default(<SkinTemperatureEntry>[])
    List<SkinTemperatureEntry> previousSkinTemperature,
    @Default(<SkinTemperatureEntry>[])
    List<SkinTemperatureEntry> baselineSkinTemperature,
  }) = _VitalsPeriodData;
}
