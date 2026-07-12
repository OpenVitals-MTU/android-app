import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/stats/stats.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/usecase/load_heart_period_use_case.dart';
import '../../heart/presentation/heart_metric_cards.dart';

part 'heart_vitals_overview_display.freezed.dart';

/// The screen-ready derivation of one loaded heart + vitals period: every
/// overview card's value, every chart's sorted series, and the scalars their
/// summary lines print.
///
/// Built once per load by [buildHeartVitalsOverviewDisplay] and stored on the
/// state — the view-model precomputes, the widgets only render (the Kotlin
/// `HeartDisplayState` discipline, as restored for the heart metric screen in
/// `heart_display.dart`). The overview used to sort its ten sample lists,
/// average them and scan them for their extremes on every rebuild, at twenty-odd
/// separate sites inside three `build` methods.
///
/// **A null sub-model means "no data for this metric in this period"** — which is
/// exactly what the screen's `isEmpty`/`== null` guards used to compute inline,
/// and what makes a card fall back to its [MetricCardPlaceholder].
///
/// Everything here is a raw number, a sorted list or a domain entry: the widgets
/// still own the formatting (a [UnitFormatter] is a presentation concern, and it
/// depends on a unit-system preference this layer must not read).
@freezed
abstract class HeartVitalsOverviewDisplay with _$HeartVitalsOverviewDisplay {
  const factory HeartVitalsOverviewDisplay({
    HeartRateOverview? heartRate,
    RestingHeartRateOverview? restingHeartRate,
    HrvOverview? hrv,
    BloodPressureOverview? bloodPressure,
    SpO2Overview? spO2,
    Vo2MaxOverview? vo2Max,
    BloodGlucoseOverview? bloodGlucose,
    RespiratoryRateOverview? respiratoryRate,
    BodyTemperatureOverview? bodyTemperature,
    SkinTemperatureOverview? skinTemperature,
  }) = _HeartVitalsOverviewDisplay;
}

/// The heart-rate card, plus whichever of the two charts the range calls for:
/// the intraday timeline within a day, the daily-summary trend beyond it. Never
/// both, and neither when the day holds a single sample.
@freezed
abstract class HeartRateOverview with _$HeartRateOverview {
  const factory HeartRateOverview({
    required int averageBpm,
    String? source,
    HeartRateDayTimeline? dayTimeline,
    HeartRatePeriodChart? periodChart,
  }) = _HeartRateOverview;
}

/// A day of raw heart-rate samples, oldest first. [chartMinValue] /
/// [chartMaxValue] are the padded axis bounds the timeline draws between
/// (Kotlin's min-5/max+5, floored at a plausible resting rate).
@freezed
abstract class HeartRateDayTimeline with _$HeartRateDayTimeline {
  const factory HeartRateDayTimeline({
    required List<HeartRateSample> samples,
    required int averageBpm,
    required int minBpm,
    required int maxBpm,
    required double chartMinValue,
    required double chartMaxValue,
  }) = _HeartRateDayTimeline;
}

/// A period of daily heart-rate summaries, oldest first.
@freezed
abstract class HeartRatePeriodChart with _$HeartRatePeriodChart {
  const factory HeartRatePeriodChart({
    required List<HeartRateSummary> summaries,
    required int averageBpm,
    required int lowestBpm,
    required int highestBpm,
  }) = _HeartRatePeriodChart;
}

/// The resting heart-rate card: the day's aggregate, or the period's mean.
@freezed
abstract class RestingHeartRateOverview with _$RestingHeartRateOverview {
  const factory RestingHeartRateOverview({
    required int bpm,
    RestingHeartRatePeriodChart? periodChart,
  }) = _RestingHeartRateOverview;
}

@freezed
abstract class RestingHeartRatePeriodChart with _$RestingHeartRatePeriodChart {
  const factory RestingHeartRatePeriodChart({
    required List<DailyRestingHR> entries,
    required int averageBpm,
    required int lowBpm,
    required int highBpm,
  }) = _RestingHeartRatePeriodChart;
}

@freezed
abstract class HrvOverview with _$HrvOverview {
  const factory HrvOverview({
    required double ms,
    HrvPeriodChart? periodChart,
  }) = _HrvOverview;
}

@freezed
abstract class HrvPeriodChart with _$HrvPeriodChart {
  const factory HrvPeriodChart({
    required List<DailyHrv> entries,
    required double averageMs,
    required double lowMs,
    required double highMs,
  }) = _HrvPeriodChart;
}

/// [hasChart] is Kotlin's `hasRenderableChartData`: within a day a single
/// timestamp draws no line, so the card shows and the chart does not.
@freezed
abstract class BloodPressureOverview with _$BloodPressureOverview {
  const factory BloodPressureOverview({
    required List<BloodPressureEntry> entries,
    required BloodPressureEntry latest,
    required int readings,
    required bool hasChart,
  }) = _BloodPressureOverview;
}

@freezed
abstract class SpO2Overview with _$SpO2Overview {
  const factory SpO2Overview({
    required List<SpO2Entry> entries,
    required SpO2Entry latest,
    required double averagePercent,
    required bool hasChart,
  }) = _SpO2Overview;
}

@freezed
abstract class Vo2MaxOverview with _$Vo2MaxOverview {
  const factory Vo2MaxOverview({
    required List<Vo2MaxEntry> entries,
    required Vo2MaxEntry latest,
    required int readings,
    required bool hasChart,
  }) = _Vo2MaxOverview;
}

@freezed
abstract class BloodGlucoseOverview with _$BloodGlucoseOverview {
  const factory BloodGlucoseOverview({
    required List<BloodGlucoseEntry> entries,
    required BloodGlucoseEntry latest,
    required double averageMmolPerLiter,
    required bool hasChart,
  }) = _BloodGlucoseOverview;
}

/// Respiratory rate keeps two numbers that only coincide on evenly sampled days:
/// the card prints the latest reading within a day and the mean of the per-day
/// means beyond it, while the chart summary always prints [periodAverage] — the
/// mean of the per-day means. Both are what the screen printed before.
@freezed
abstract class RespiratoryRateOverview with _$RespiratoryRateOverview {
  const factory RespiratoryRateOverview({
    required List<RespiratoryRateEntry> entries,
    required double cardBreathsPerMinute,
    String? cardSource,
    required double periodAverage,
    required bool hasChart,
  }) = _RespiratoryRateOverview;
}

@freezed
abstract class BodyTemperatureOverview with _$BodyTemperatureOverview {
  const factory BodyTemperatureOverview({
    required List<BodyTempEntry> entries,
    required BodyTempEntry latest,
    required int readings,
    required bool hasChart,
  }) = _BodyTemperatureOverview;
}

/// Skin temperature is the odd one: an entry can carry no delta. Those are
/// excluded from [chartEntries] and from [averageDeltaCelsius], but [latest] is
/// still the latest entry of the *unfiltered* list — so a delta-less newest entry
/// blanks the card while the chart keeps drawing.
@freezed
abstract class SkinTemperatureOverview with _$SkinTemperatureOverview {
  const factory SkinTemperatureOverview({
    required List<SkinTemperatureEntry> chartEntries,
    required SkinTemperatureEntry latest,
    double? cardDeltaCelsius,
    required double averageDeltaCelsius,
    required bool hasChart,
  }) = _SkinTemperatureOverview;
}

/// Pure derivation from the loaded period to its display model. No clock, no ref,
/// no I/O, no [BuildContext] — unit-testable with a fixture result, which is the
/// whole point of the seam.
///
/// [selectedRange] is the one thing besides the payload that changes the answer:
/// within a day the cards read the raw samples and the latest readings, over a
/// longer range they read the daily aggregates.
HeartVitalsOverviewDisplay buildHeartVitalsOverviewDisplay(
  HeartPeriodLoadResult result, {
  required TimeRange selectedRange,
}) {
  final vitals = result.vitalsSummary();
  return HeartVitalsOverviewDisplay(
    heartRate: _heartRate(result, selectedRange),
    restingHeartRate: _restingHeartRate(result, selectedRange),
    hrv: _hrv(result, selectedRange),
    bloodPressure: _bloodPressure(result, selectedRange, vitals),
    spO2: _spO2(result, selectedRange, vitals),
    vo2Max: _vo2Max(result, selectedRange, vitals),
    bloodGlucose: _bloodGlucose(result, selectedRange, vitals),
    respiratoryRate: _respiratoryRate(result, selectedRange, vitals),
    bodyTemperature: _bodyTemperature(result, selectedRange, vitals),
    skinTemperature: _skinTemperature(result, selectedRange, vitals),
  );
}

HeartRateOverview? _heartRate(HeartPeriodLoadResult result, TimeRange range) {
  final isDay = range == TimeRange.day;
  final samples = [...result.daySamples]
    ..sort((a, b) => a.time.compareTo(b.time));
  final summaries = [...result.dailySummaries]
    ..sort((a, b) => a.date.compareTo(b.date));

  if (isDay) {
    if (samples.isEmpty) return null;
    final bpm = samples.map((s) => s.beatsPerMinute.toDouble()).toList();
    return HeartRateOverview(
      averageBpm: averageOrZero(bpm).round(),
      source: _singleSource(samples.map((s) => s.source)),
      dayTimeline: samples.length > 1 ? _dayTimeline(samples, bpm) : null,
    );
  }

  if (summaries.isEmpty) return null;
  final average = averageOrZero(summaries.map((s) => s.avgBpm.toDouble()));
  return HeartRateOverview(
    averageBpm: average.round(),
    periodChart: HeartRatePeriodChart(
      summaries: summaries,
      averageBpm: average.round(),
      lowestBpm: summaries.map((s) => s.minBpm).reduce(math.min),
      highestBpm: summaries.map((s) => s.maxBpm).reduce(math.max),
    ),
  );
}

HeartRateDayTimeline _dayTimeline(
  List<HeartRateSample> samples,
  List<double> bpm,
) {
  final minBpm = minOf(bpm)!.round();
  final maxBpm = maxOf(bpm)!.round();
  return HeartRateDayTimeline(
    samples: samples,
    averageBpm: averageOrZero(bpm).round(),
    minBpm: minBpm,
    maxBpm: maxBpm,
    chartMinValue: math.max(30, minBpm - 5).toDouble(),
    chartMaxValue: (maxBpm + 5).toDouble(),
  );
}

RestingHeartRateOverview? _restingHeartRate(
  HeartPeriodLoadResult result,
  TimeRange range,
) {
  if (range == TimeRange.day) {
    final bpm = result.dayRestingBpm;
    return bpm == null ? null : RestingHeartRateOverview(bpm: bpm);
  }
  final entries = [...result.dailyRestingHR]
    ..sort((a, b) => a.date.compareTo(b.date));
  if (entries.isEmpty) return null;
  final bpm = entries.map((e) => e.bpm.toDouble()).toList();
  final average = averageOrZero(bpm).round();
  return RestingHeartRateOverview(
    bpm: average,
    periodChart: RestingHeartRatePeriodChart(
      entries: entries,
      averageBpm: average,
      lowBpm: minOf(bpm)!.round(),
      highBpm: maxOf(bpm)!.round(),
    ),
  );
}

HrvOverview? _hrv(HeartPeriodLoadResult result, TimeRange range) {
  if (range == TimeRange.day) {
    final ms = result.dayHrvMs;
    return ms == null ? null : HrvOverview(ms: ms);
  }
  final entries = [...result.dailyHrv]
    ..sort((a, b) => a.date.compareTo(b.date));
  if (entries.isEmpty) return null;
  final ms = entries.map((e) => e.rmssdMs).toList();
  final average = averageOrZero(ms);
  return HrvOverview(
    ms: average,
    periodChart: HrvPeriodChart(
      entries: entries,
      averageMs: average,
      lowMs: minOf(ms)!,
      highMs: maxOf(ms)!,
    ),
  );
}

BloodPressureOverview? _bloodPressure(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.bloodPressure]
    ..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestBloodPressure;
  if (entries.isEmpty || latest == null) return null;
  return BloodPressureOverview(
    entries: entries,
    latest: latest,
    readings: entries.length,
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

SpO2Overview? _spO2(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.spO2]..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestSpO2;
  if (entries.isEmpty || latest == null) return null;
  return SpO2Overview(
    entries: entries,
    latest: latest,
    averagePercent: averageOrZero(entries.map((e) => e.percent)),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

Vo2MaxOverview? _vo2Max(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.vo2Max]..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestVo2Max;
  if (entries.isEmpty || latest == null) return null;
  return Vo2MaxOverview(
    entries: entries,
    latest: latest,
    readings: entries.length,
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

BloodGlucoseOverview? _bloodGlucose(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.bloodGlucose]
    ..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestBloodGlucose;
  if (entries.isEmpty || latest == null) return null;
  return BloodGlucoseOverview(
    entries: entries,
    latest: latest,
    averageMmolPerLiter:
        averageOrZero(entries.map((e) => e.millimolesPerLiter)),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

RespiratoryRateOverview? _respiratoryRate(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.respiratoryRate]
    ..sort((a, b) => a.time.compareTo(b.time));
  if (entries.isEmpty) return null;
  final isDay = range == TimeRange.day;
  final daySummaries = respiratoryRateDaySummaries(entries);
  final periodAverage = averageOrZero(daySummaries.map((s) => s.average));
  return RespiratoryRateOverview(
    entries: entries,
    cardBreathsPerMinute:
        isDay ? entries.last.breathsPerMinute : periodAverage,
    cardSource: isDay
        ? vitals.latestRespiratoryRate?.source
        : _singleSource(entries.map((e) => e.source)),
    periodAverage: periodAverage,
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

BodyTemperatureOverview? _bodyTemperature(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final entries = [...result.bodyTemperature]
    ..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestBodyTemperature;
  if (entries.isEmpty || latest == null) return null;
  return BodyTemperatureOverview(
    entries: entries,
    latest: latest,
    readings: entries.length,
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

SkinTemperatureOverview? _skinTemperature(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final chartEntries = [
    for (final e in result.skinTemperature)
      if (e.averageDeltaCelsius != null) e,
  ]..sort((a, b) => a.time.compareTo(b.time));
  final latest = vitals.latestSkinTemperature;
  if (latest == null) return null;
  return SkinTemperatureOverview(
    chartEntries: chartEntries,
    latest: latest,
    cardDeltaCelsius: latest.averageDeltaCelsius,
    averageDeltaCelsius:
        averageOrZero(chartEntries.map((e) => e.averageDeltaCelsius!)),
    hasChart: _hasRenderableChartData(chartEntries, range, (e) => e.time),
  );
}

/// Kotlin `hasRenderableChartData`: within a day, needs more than one distinct
/// timestamp; otherwise any reading renders.
bool _hasRenderableChartData<T>(
  List<T> entries,
  TimeRange range,
  DateTime Function(T) time,
) {
  if (range == TimeRange.day) {
    return entries.map((e) => time(e).millisecondsSinceEpoch).toSet().length > 1;
  }
  return entries.isNotEmpty;
}

/// The source line a card prints only when every reading agrees on it — two
/// devices disagreeing is not something one line can say.
String? _singleSource(Iterable<String> sources) {
  final distinct = sources.toSet();
  return distinct.length == 1 ? distinct.first : null;
}
