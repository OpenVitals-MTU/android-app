import 'package:freezed_annotation/freezed_annotation.dart';

part 'cycle_models.freezed.dart';

@freezed
abstract class CycleData with _$CycleData {
  const CycleData._();

  const factory CycleData({
    @Default(<MenstruationFlowEntry>[]) List<MenstruationFlowEntry> menstruationFlows,
    @Default(<MenstruationPeriodEntry>[])
    List<MenstruationPeriodEntry> menstruationPeriods,
    @Default(<OvulationTestEntry>[]) List<OvulationTestEntry> ovulationTests,
    @Default(<CervicalMucusEntry>[]) List<CervicalMucusEntry> cervicalMucus,
    @Default(<BasalBodyTemperatureEntry>[])
    List<BasalBodyTemperatureEntry> basalBodyTemperature,
    @Default(<IntermenstrualBleedingEntry>[])
    List<IntermenstrualBleedingEntry> intermenstrualBleeding,
    @Default(<SexualActivityEntry>[]) List<SexualActivityEntry> sexualActivity,
  }) = _CycleData;

  bool get hasData =>
      menstruationFlows.isNotEmpty ||
      menstruationPeriods.isNotEmpty ||
      ovulationTests.isNotEmpty ||
      cervicalMucus.isNotEmpty ||
      basalBodyTemperature.isNotEmpty ||
      intermenstrualBleeding.isNotEmpty ||
      sexualActivity.isNotEmpty;
}

@freezed
abstract class MenstruationFlowEntry with _$MenstruationFlowEntry {
  const factory MenstruationFlowEntry({
    required DateTime time,
    required int flow,
    required String source,
  }) = _MenstruationFlowEntry;
}

@freezed
abstract class MenstruationPeriodEntry with _$MenstruationPeriodEntry {
  const MenstruationPeriodEntry._();

  const factory MenstruationPeriodEntry({
    required DateTime startTime,
    required DateTime endTime,
    required String source,
  }) = _MenstruationPeriodEntry;

  int get durationMs =>
      endTime.millisecondsSinceEpoch - startTime.millisecondsSinceEpoch;
}

@freezed
abstract class OvulationTestEntry with _$OvulationTestEntry {
  const factory OvulationTestEntry({
    required DateTime time,
    required int result,
    required String source,
  }) = _OvulationTestEntry;
}

@freezed
abstract class CervicalMucusEntry with _$CervicalMucusEntry {
  const factory CervicalMucusEntry({
    required DateTime time,
    required int appearance,
    required int sensation,
    required String source,
  }) = _CervicalMucusEntry;
}

@freezed
abstract class BasalBodyTemperatureEntry with _$BasalBodyTemperatureEntry {
  const factory BasalBodyTemperatureEntry({
    required DateTime time,
    required double temperatureCelsius,
    required int measurementLocation,
    required String source,
  }) = _BasalBodyTemperatureEntry;
}

@freezed
abstract class IntermenstrualBleedingEntry with _$IntermenstrualBleedingEntry {
  const factory IntermenstrualBleedingEntry({
    required DateTime time,
    required String source,
  }) = _IntermenstrualBleedingEntry;
}

@freezed
abstract class SexualActivityEntry with _$SexualActivityEntry {
  const factory SexualActivityEntry({
    required DateTime time,
    required int protectionUsed,
    required String source,
  }) = _SexualActivityEntry;
}
