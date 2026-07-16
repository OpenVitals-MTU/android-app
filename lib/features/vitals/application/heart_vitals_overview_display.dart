import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/stats/stats.dart';
import '../../../core/time/local_date.dart';
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
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.bloodPressure]..sort((a, b) => a.time.compareTo(b.time)))
      : _bpFromDaily(result.bloodPressureDaily);
  final latest = vitals.latestBloodPressure;
  if (entries.isEmpty || latest == null) return null;
  return BloodPressureOverview(
    entries: entries,
    latest: latest,
    readings: isDay
        ? entries.length
        : result.bloodPressureDaily.fold(0, (sum, p) => sum + p.count),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

SpO2Overview? _spO2(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.spO2]..sort((a, b) => a.time.compareTo(b.time)))
      : _spO2FromDaily(result.spO2Daily);
  final latest = vitals.latestSpO2;
  if (entries.isEmpty || latest == null) return null;
  return SpO2Overview(
    entries: entries,
    latest: latest,
    averagePercent: isDay
        ? averageOrZero(entries.map((e) => e.percent))
        : _weightedMean(result.spO2Daily),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

Vo2MaxOverview? _vo2Max(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.vo2Max]..sort((a, b) => a.time.compareTo(b.time)))
      : _vo2MaxFromDaily(result.vo2MaxDaily);
  final latest = vitals.latestVo2Max;
  if (entries.isEmpty || latest == null) return null;
  return Vo2MaxOverview(
    entries: entries,
    latest: latest,
    readings: isDay ? entries.length : _readingsOf(result.vo2MaxDaily),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

BloodGlucoseOverview? _bloodGlucose(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.bloodGlucose]..sort((a, b) => a.time.compareTo(b.time)))
      : _bloodGlucoseFromDaily(result.bloodGlucoseDaily);
  final latest = vitals.latestBloodGlucose;
  if (entries.isEmpty || latest == null) return null;
  return BloodGlucoseOverview(
    entries: entries,
    latest: latest,
    averageMmolPerLiter: isDay
        ? averageOrZero(entries.map((e) => e.millimolesPerLiter))
        : _weightedMean(result.bloodGlucoseDaily),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

RespiratoryRateOverview? _respiratoryRate(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.respiratoryRate]..sort((a, b) => a.time.compareTo(b.time)))
      : _respiratoryFromDaily(result.respiratoryRateDaily);
  if (entries.isEmpty) return null;
  // Respiratory rate prints the mean of the daily means (each day weighed
  // equally), not the flat mean of every reading — so average the per-day values
  // unweighted, matching respiratoryRateDaySummaries on the day path.
  final periodAverage = isDay
      ? averageOrZero(respiratoryRateDaySummaries(entries).map((s) => s.average))
      : averageOrZero(result.respiratoryRateDaily.map((p) => p.value));
  return RespiratoryRateOverview(
    entries: entries,
    cardBreathsPerMinute:
        isDay ? entries.last.breathsPerMinute : periodAverage,
    // The card names the latest reading's source. (Non-day synthetic entries
    // carry none, as a merged aggregate has no single writer, so this is the
    // only source the card can truthfully print for a long range.)
    cardSource: vitals.latestRespiratoryRate?.source,
    periodAverage: periodAverage,
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

BodyTemperatureOverview? _bodyTemperature(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final entries = isDay
      ? ([...result.bodyTemperature]..sort((a, b) => a.time.compareTo(b.time)))
      : _bodyTempFromDaily(result.bodyTemperatureDaily);
  final latest = vitals.latestBodyTemperature;
  if (entries.isEmpty || latest == null) return null;
  return BodyTemperatureOverview(
    entries: entries,
    latest: latest,
    readings: isDay ? entries.length : _readingsOf(result.bodyTemperatureDaily),
    hasChart: _hasRenderableChartData(entries, range, (e) => e.time),
  );
}

SkinTemperatureOverview? _skinTemperature(
  HeartPeriodLoadResult result,
  TimeRange range,
  HeartVitalsSummary vitals,
) {
  final isDay = range == TimeRange.day;
  final chartEntries = isDay
      ? ([
          for (final e in result.skinTemperature)
            if (e.averageDeltaCelsius != null) e,
        ]..sort((a, b) => a.time.compareTo(b.time)))
      : _skinTempFromDaily(result.skinTemperatureDaily);
  final latest = vitals.latestSkinTemperature;
  if (latest == null) return null;
  // The card reads the newest entry that actually CARRIES a delta — the same
  // population the chart draws. It used to read the newest entry of the
  // unfiltered list, so a reading that arrived without a delta blanked the card
  // while the chart underneath it went on plotting the readings that had one.
  final latestWithDelta = chartEntries.isEmpty ? null : chartEntries.last;
  return SkinTemperatureOverview(
    // On non-day the true latest reading (with its real source) names the card;
    // its delta may be null, which only blanks the delta value, not the source.
    chartEntries: chartEntries,
    latest: isDay ? (latestWithDelta ?? latest) : latest,
    cardDeltaCelsius: latestWithDelta?.averageDeltaCelsius,
    averageDeltaCelsius: isDay
        ? averageOrZero(chartEntries.map((e) => e.averageDeltaCelsius!))
        : _weightedMean(result.skinTemperatureDaily),
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

// ── Long-range (non-day) aggregates ─────────────────────────────────────────
// On week/month/year the overview plots one point per day from the native daily
// aggregates instead of the raw record list. The chart widgets consume the raw
// entry types, so each day becomes a synthetic entry (its aggregated value,
// timestamped at local midnight, no source — a merged aggregate has no single
// writer). Card scalars come straight from the daily points so counts and means
// stay exact: [readings] is the true total, and averages are count-weighted.

/// Local midnight — the representative timestamp for a day's aggregated value.
DateTime _dayStart(LocalDate date) => DateTime(date.year, date.month, date.day);

/// Count-weighted mean, so a period average equals the mean of every underlying
/// reading rather than the unweighted mean of the daily means.
double _weightedMean(List<DailyVitalPoint> points) {
  var weighted = 0.0;
  var readings = 0;
  for (final p in points) {
    weighted += p.value * p.count;
    readings += p.count;
  }
  return readings == 0 ? 0.0 : weighted / readings;
}

int _readingsOf(List<DailyVitalPoint> points) =>
    points.fold(0, (sum, p) => sum + p.count);

List<BloodPressureEntry> _bpFromDaily(List<DailyBloodPressurePoint> points) => [
      for (final p in points)
        BloodPressureEntry(
          time: _dayStart(p.date),
          systolicMmHg: p.systolic.round(),
          diastolicMmHg: p.diastolic.round(),
          source: '',
        ),
    ];

List<SpO2Entry> _spO2FromDaily(List<DailyVitalPoint> points) => [
      for (final p in points)
        SpO2Entry(time: _dayStart(p.date), percent: p.value, source: ''),
    ];

List<RespiratoryRateEntry> _respiratoryFromDaily(List<DailyVitalPoint> points) =>
    [
      for (final p in points)
        RespiratoryRateEntry(
          time: _dayStart(p.date),
          breathsPerMinute: p.value,
          source: '',
        ),
    ];

List<BodyTempEntry> _bodyTempFromDaily(List<DailyVitalPoint> points) => [
      for (final p in points)
        BodyTempEntry(
          time: _dayStart(p.date),
          temperatureCelsius: p.value,
          source: '',
        ),
    ];

List<Vo2MaxEntry> _vo2MaxFromDaily(List<DailyVitalPoint> points) => [
      for (final p in points)
        Vo2MaxEntry(
          time: _dayStart(p.date),
          vo2MaxMlPerKgPerMin: p.value,
          source: '',
        ),
    ];

List<BloodGlucoseEntry> _bloodGlucoseFromDaily(List<DailyVitalPoint> points) => [
      for (final p in points)
        BloodGlucoseEntry(
          time: _dayStart(p.date),
          millimolesPerLiter: p.value,
          specimenSource: 0,
          mealType: 0,
          relationToMeal: 0,
          source: '',
        ),
    ];

List<SkinTemperatureEntry> _skinTempFromDaily(List<DailyVitalPoint> points) => [
      for (final p in points)
        SkinTemperatureEntry(
          startTime: _dayStart(p.date),
          endTime: _dayStart(p.date),
          baselineCelsius: null,
          averageDeltaCelsius: p.value,
          minDeltaCelsius: null,
          maxDeltaCelsius: null,
          measurementLocation: 0,
          source: '',
        ),
    ];
