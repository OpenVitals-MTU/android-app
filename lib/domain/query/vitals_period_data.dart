import 'package:freezed_annotation/freezed_annotation.dart';

import '../../data/repository/contract/vitals_repository.dart';
import '../model/vitals_models.dart';

part 'vitals_period_data.freezed.dart';

@freezed
abstract class VitalsPeriodData with _$VitalsPeriodData {
  const factory VitalsPeriodData({
    @Default(<String>{}) Set<String> missingVitalsPermissions,
    // Non-day metrics whose daily read exceeded its budget (too large to read
    // raw over this range) — the card shows an "unavailable for this range"
    // state instead of "no readings". See VitalsRepositoryImpl.
    @Default(<VitalsPeriodMetric>{}) Set<VitalsPeriodMetric> timedOutMetrics,
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
    // Long-range (non-day) overview: one aggregated point per day plus the true
    // latest reading, so the year chart and cards never load the raw record
    // list. Empty/null on the day view, which keeps using the raw lists above.
    // See VitalsRepositoryImpl._loadVitalsPeriodRaw (VitalsPeriodMetric.all).
    @Default(<DailyBloodPressurePoint>[])
    List<DailyBloodPressurePoint> bloodPressureDaily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> spO2Daily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> respiratoryRateDaily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> bodyTemperatureDaily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> vo2MaxDaily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> bloodGlucoseDaily,
    @Default(<DailyVitalPoint>[]) List<DailyVitalPoint> skinTemperatureDaily,
    BloodPressureEntry? latestBloodPressure,
    SpO2Entry? latestSpO2,
    Vo2MaxEntry? latestVo2Max,
    RespiratoryRateEntry? latestRespiratoryRate,
    BodyTempEntry? latestBodyTemperature,
    BloodGlucoseEntry? latestBloodGlucose,
    SkinTemperatureEntry? latestSkinTemperature,
  }) = _VitalsPeriodData;
}
