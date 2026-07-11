import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/cycle_models.dart';

part 'cycle_period_data.freezed.dart';

@freezed
abstract class CyclePeriodData with _$CyclePeriodData {
  const factory CyclePeriodData({
    required CycleData data,
    required Set<String> missingPermissions,
  }) = _CyclePeriodData;
}
