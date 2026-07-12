import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/model/cycle_models.dart';

part 'cycle_display.freezed.dart';

/// The screen-ready derivation of one loaded cycle period: the summary counts
/// the statistics card prints, and the dated observations the entry list
/// renders, in display order.
///
/// Built once per load by [buildCycleDisplay] and stored on the state — the
/// view-model precomputes, the screen only renders (the Kotlin
/// `CycleDisplayState` discipline, restored). The screen used to expand every
/// menstruation period into its days, scan for the latest basal temperature and
/// sort the observation list on every rebuild.
@freezed
abstract class CycleDisplay with _$CycleDisplay {
  const factory CycleDisplay({
    required bool hasData,
    required int periodDays,
    required int ovulationTestCount,
    required int bbtReadingCount,
    required int totalEntryCount,
    double? latestBbtCelsius,
    required List<CycleObservation> observations,
  }) = _CycleDisplay;
}

/// What kind of cycle record an observation row came from. The row's title, its
/// enum-to-label mapping and its temperature formatting are the view's business;
/// which record it was, and when, is this.
enum CycleObservationKind {
  menstruationPeriod,
  menstruationFlow,
  ovulationTest,
  cervicalMucus,
  basalBodyTemperature,
  intermenstrualBleeding,
  sexualActivity,
}

/// One dated cycle observation, carrying the raw Health Connect codes rather
/// than their labels — the labels are l10n/presentation, and stay in the view.
@freezed
abstract class CycleObservation with _$CycleObservation {
  const factory CycleObservation({
    required CycleObservationKind kind,
    required DateTime time,
    required String source,

    /// Menstruation period: its length in whole days (at least one).
    int? days,
    int? flow,
    int? ovulationResult,
    int? mucusAppearance,
    int? mucusSensation,
    double? temperatureCelsius,
    int? measurementLocation,
    int? protectionUsed,
  }) = _CycleObservation;
}

/// Pure derivation from the loaded cycle data to its display model. No clock, no
/// formatter, no l10n — unit-testable with a fixture [CycleData].
CycleDisplay buildCycleDisplay(CycleData data) {
  final periodDates = <LocalDate>{};
  for (final period in data.menstruationPeriods) {
    final start = instantToLocalDate(period.startTime);
    final end = instantToLocalDate(
      period.endTime.subtract(const Duration(milliseconds: 1)),
    );
    var date = start;
    while (!date.isAfter(end)) {
      periodDates.add(date);
      date = date.plusDays(1);
    }
  }
  BasalBodyTemperatureEntry? latest;
  for (final entry in data.basalBodyTemperature) {
    if (latest == null || entry.time.isAfter(latest.time)) {
      latest = entry;
    }
  }
  final total = data.menstruationFlows.length +
      data.menstruationPeriods.length +
      data.ovulationTests.length +
      data.cervicalMucus.length +
      data.basalBodyTemperature.length +
      data.intermenstrualBleeding.length +
      data.sexualActivity.length;

  return CycleDisplay(
    hasData: data.hasData,
    periodDays: periodDates.length,
    ovulationTestCount: data.ovulationTests.length,
    bbtReadingCount: data.basalBodyTemperature.length,
    totalEntryCount: total,
    latestBbtCelsius: latest?.temperatureCelsius,
    observations: _observations(data),
  );
}

/// Every cycle record as a dated observation, newest first.
List<CycleObservation> _observations(CycleData data) {
  final observations = <CycleObservation>[];
  for (final period in data.menstruationPeriods) {
    final start = instantToLocalDate(period.startTime);
    final end = instantToLocalDate(
      period.endTime.subtract(const Duration(milliseconds: 1)),
    );
    final days = (end.epochDay - start.epochDay + 1).clamp(1, 1 << 30);
    observations.add(CycleObservation(
      kind: CycleObservationKind.menstruationPeriod,
      time: period.startTime,
      source: period.source,
      days: days,
    ));
  }
  for (final flow in data.menstruationFlows) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.menstruationFlow,
      time: flow.time,
      source: flow.source,
      flow: flow.flow,
    ));
  }
  for (final test in data.ovulationTests) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.ovulationTest,
      time: test.time,
      source: test.source,
      ovulationResult: test.result,
    ));
  }
  for (final mucus in data.cervicalMucus) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.cervicalMucus,
      time: mucus.time,
      source: mucus.source,
      mucusAppearance: mucus.appearance,
      mucusSensation: mucus.sensation,
    ));
  }
  for (final temperature in data.basalBodyTemperature) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.basalBodyTemperature,
      time: temperature.time,
      source: temperature.source,
      temperatureCelsius: temperature.temperatureCelsius,
      measurementLocation: temperature.measurementLocation,
    ));
  }
  for (final bleeding in data.intermenstrualBleeding) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.intermenstrualBleeding,
      time: bleeding.time,
      source: bleeding.source,
    ));
  }
  for (final activity in data.sexualActivity) {
    observations.add(CycleObservation(
      kind: CycleObservationKind.sexualActivity,
      time: activity.time,
      source: activity.source,
      protectionUsed: activity.protectionUsed,
    ));
  }
  observations.sort((a, b) => b.time.compareTo(a.time));
  return observations;
}
