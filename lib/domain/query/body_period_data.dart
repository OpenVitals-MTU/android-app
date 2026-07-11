import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/body_models.dart';

part 'body_period_data.freezed.dart';

@freezed
abstract class BodyPeriodData with _$BodyPeriodData {
  const factory BodyPeriodData({
    @Default(<WeightEntry>[]) List<WeightEntry> weightEntries,
    @Default(<WeightEntry>[]) List<WeightEntry> previousWeightEntries,
    @Default(<WeightEntry>[]) List<WeightEntry> baselineWeightEntries,
    double? latestWeightKg,
    double? heightCm,
    @Default(<HeightEntry>[]) List<HeightEntry> heightEntries,
    @Default(<HeightEntry>[]) List<HeightEntry> previousHeightEntries,
    @Default(<HeightEntry>[]) List<HeightEntry> baselineHeightEntries,
    @Default(<BodyFatEntry>[]) List<BodyFatEntry> bodyFatEntries,
    @Default(<BodyFatEntry>[]) List<BodyFatEntry> previousBodyFatEntries,
    @Default(<BodyFatEntry>[]) List<BodyFatEntry> baselineBodyFatEntries,
    double? latestBodyFatPercent,
    double? leanMassKg,
    @Default(<LeanBodyMassEntry>[]) List<LeanBodyMassEntry> leanMassEntries,
    @Default(<LeanBodyMassEntry>[])
    List<LeanBodyMassEntry> previousLeanMassEntries,
    @Default(<LeanBodyMassEntry>[])
    List<LeanBodyMassEntry> baselineLeanMassEntries,
    double? bmrKcal,
    @Default(<BmrEntry>[]) List<BmrEntry> bmrEntries,
    @Default(<BmrEntry>[]) List<BmrEntry> previousBmrEntries,
    @Default(<BmrEntry>[]) List<BmrEntry> baselineBmrEntries,
    double? boneMassKg,
    @Default(<BoneMassEntry>[]) List<BoneMassEntry> boneMassEntries,
    @Default(<BoneMassEntry>[]) List<BoneMassEntry> previousBoneMassEntries,
    @Default(<BoneMassEntry>[]) List<BoneMassEntry> baselineBoneMassEntries,
    double? bodyWaterMassKg,
    @Default(<BodyWaterMassEntry>[])
    List<BodyWaterMassEntry> bodyWaterMassEntries,
    @Default(<BodyWaterMassEntry>[])
    List<BodyWaterMassEntry> previousBodyWaterMassEntries,
    @Default(<BodyWaterMassEntry>[])
    List<BodyWaterMassEntry> baselineBodyWaterMassEntries,
  }) = _BodyPeriodData;
}
