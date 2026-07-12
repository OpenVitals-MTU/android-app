import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/stats/stats.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/period_comparison.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/usecase/load_heart_period_use_case.dart';
import '../presentation/heart_metric_cards.dart';

part 'heart_display.freezed.dart';

/// The screen-ready derivation of one loaded heart/vitals period: every metric
/// section's sorted series, its min/max/average, and the statistics grid values
/// the [HeartNumericStatisticsContent] grids render.
///
/// Built once per load by [buildHeartDisplay] and stored on the state — the
/// view-model precomputes, the widgets only render (the Kotlin
/// `HeartDisplayState` discipline, restored). The screen used to sort the sample
/// lists and `reduce` their extremes on every rebuild, at twenty separate sites.
///
/// One display carries every metric's section because one [HeartPeriodLoadResult]
/// does: a metric's load only populates its own lists, so the others fall out as
/// null for free.
@freezed
abstract class HeartDisplay with _$HeartDisplay {
  const factory HeartDisplay({
    required HeartRateThresholdCheck highHeartRateCheck,
    required HeartRateThresholdCheck lowHeartRateCheck,
    HeartRateDayDisplay? heartRateDay,
    HeartRatePeriodDisplay? heartRatePeriod,
    RestingHeartRateDayDisplay? restingHeartRateDay,
    RestingHeartRatePeriodDisplay? restingHeartRatePeriod,
    HrvDayDisplay? hrvDay,
    HrvPeriodDisplay? hrvPeriod,
    BloodPressureDisplay? bloodPressure,
    SpO2Display? spO2,
    Vo2MaxDisplay? vo2Max,
    RespiratoryRateDisplay? respiratoryRate,
    BodyTemperatureDisplay? bodyTemperature,
    BloodGlucoseDisplay? bloodGlucose,
    SkinTemperatureDisplay? skinTemperature,
  }) = _HeartDisplay;
}

/// The values behind one [HeartNumericStatisticsContent] grid: the average, the
/// extremes, the reading count, the previous-period comparison and the personal
/// baseline series. Formatting stays in the view; the arithmetic lives here.
@freezed
abstract class HeartStats with _$HeartStats {
  const factory HeartStats({
    required double average,
    required double low,
    required double high,
    required int readings,
    PeriodComparison? comparison,
    required double baselineCurrentValue,
    required List<BaselineValue> baselineValues,
  }) = _HeartStats;
}

/// A day of raw heart-rate samples, oldest first. [chartMinValue] /
/// [chartMaxValue] are the padded axis bounds the intraday timeline draws
/// between (Kotlin's min-5/max+5, floored at a plausible resting rate).
@freezed
abstract class HeartRateDayDisplay with _$HeartRateDayDisplay {
  const factory HeartRateDayDisplay({
    required List<HeartRateSample> samples,
    required int averageBpm,
    required int minBpm,
    required int maxBpm,
    required double chartMinValue,
    required double chartMaxValue,
    required HeartStats stats,
  }) = _HeartRateDayDisplay;
}

/// A period of daily heart-rate summaries: oldest first for the chart, newest
/// first for the daily breakdown.
@freezed
abstract class HeartRatePeriodDisplay with _$HeartRatePeriodDisplay {
  const factory HeartRatePeriodDisplay({
    required List<HeartRateSummary> summaries,
    required List<HeartRateSummary> summariesNewestFirst,
    required int averageBpm,
    required int lowestBpm,
    required int highestBpm,
    required HeartStats stats,
  }) = _HeartRatePeriodDisplay;
}

/// A day's resting heart rate: the aggregate the provider reported, or the mean
/// of the day's samples when it reported none.
@freezed
abstract class RestingHeartRateDayDisplay with _$RestingHeartRateDayDisplay {
  const factory RestingHeartRateDayDisplay({
    required List<RestingHeartRateSample> samples,
    required int restingBpm,
    required int lowBpm,
    required int highBpm,
    required double chartMinValue,
    required double chartMaxValue,
    required HeartStats stats,
  }) = _RestingHeartRateDayDisplay;
}

@freezed
abstract class RestingHeartRatePeriodDisplay
    with _$RestingHeartRatePeriodDisplay {
  const factory RestingHeartRatePeriodDisplay({
    required List<DailyRestingHR> entries,
    required int averageBpm,
    required int lowBpm,
    required int highBpm,
    required HeartStats stats,
  }) = _RestingHeartRatePeriodDisplay;
}

@freezed
abstract class HrvDayDisplay with _$HrvDayDisplay {
  const factory HrvDayDisplay({
    required List<HrvSample> samples,
    required double hrvMs,
    required double lowMs,
    required double highMs,
    required double chartMinValue,
    required double chartMaxValue,
    required HeartStats stats,
  }) = _HrvDayDisplay;
}

@freezed
abstract class HrvPeriodDisplay with _$HrvPeriodDisplay {
  const factory HrvPeriodDisplay({
    required List<DailyHrv> entries,
    required double averageMs,
    required double lowMs,
    required double highMs,
    required HeartStats stats,
  }) = _HrvPeriodDisplay;
}

/// The blood-pressure statistics grid: systolic and diastolic move together, so
/// its "average" and "highest" are entries, not scalars.
@freezed
abstract class BloodPressureStats with _$BloodPressureStats {
  const factory BloodPressureStats({
    BloodPressureEntry? latest,
    BloodPressureEntry? highest,
    required double averageSystolic,
    required double averageDiastolic,
    PeriodComparison? comparison,
    required int readings,
    required List<BaselineValue> baselineValues,
  }) = _BloodPressureStats;
}

@freezed
abstract class BloodPressureDisplay with _$BloodPressureDisplay {
  const factory BloodPressureDisplay({
    required List<BloodPressureEntry> entries,
    required BloodPressureEntry latest,
    required BloodPressureStats stats,
  }) = _BloodPressureDisplay;
}

@freezed
abstract class SpO2Display with _$SpO2Display {
  const factory SpO2Display({
    required List<SpO2Entry> entries,
    required SpO2Entry latest,
    required double averagePercent,
    required HeartStats stats,
  }) = _SpO2Display;
}

@freezed
abstract class Vo2MaxDisplay with _$Vo2MaxDisplay {
  const factory Vo2MaxDisplay({
    required List<Vo2MaxEntry> entries,
    required Vo2MaxEntry latest,
    required HeartStats stats,
  }) = _Vo2MaxDisplay;
}

/// Respiratory rate keeps two averages: the mean of the per-day means (the chart
/// summary) and the mean of every reading (the context card). They differ when
/// the days are unevenly sampled, and both are what the screen printed before.
@freezed
abstract class RespiratoryRateDisplay with _$RespiratoryRateDisplay {
  const factory RespiratoryRateDisplay({
    required List<RespiratoryRateEntry> entries,
    required List<RespiratoryRateDaySummary> daySummariesNewestFirst,
    required double periodAverage,
    required double entriesAverage,
    required HeartStats stats,
  }) = _RespiratoryRateDisplay;
}

@freezed
abstract class BodyTemperatureDisplay with _$BodyTemperatureDisplay {
  const factory BodyTemperatureDisplay({
    required List<BodyTempEntry> entries,
    required BodyTempEntry latest,
    required HeartStats stats,
  }) = _BodyTemperatureDisplay;
}

@freezed
abstract class BloodGlucoseDisplay with _$BloodGlucoseDisplay {
  const factory BloodGlucoseDisplay({
    required List<BloodGlucoseEntry> entries,
    required double averageMmolPerLiter,
    required HeartStats stats,
  }) = _BloodGlucoseDisplay;
}

/// Skin temperature is the odd one: an entry can carry no delta, and those are
/// excluded from the chart and the arithmetic while still counting as readings.
/// With no delta anywhere there is nothing to average — hence the null [stats].
@freezed
abstract class SkinTemperatureDisplay with _$SkinTemperatureDisplay {
  const factory SkinTemperatureDisplay({
    required List<SkinTemperatureEntry> entries,
    required List<SkinTemperatureEntry> chartEntries,
    double? averageDeltaCelsius,
    HeartStats? stats,
  }) = _SkinTemperatureDisplay;
}

/// Pure derivation from the loaded period to its display model. No clock, no ref,
/// no I/O — unit-testable with a fixture result, which is the whole point of the
/// seam.
///
/// The heart-rate threshold checks are folded in here too: they read the same
/// loaded samples, and the steppers rebuild the display rather than making the
/// view count.
HeartDisplay buildHeartDisplay(
  HeartPeriodLoadResult result, {
  required TimeRange selectedRange,
  required int highHeartRateThresholdBpm,
  required int lowHeartRateThresholdBpm,
}) =>
    HeartDisplay(
      highHeartRateCheck: heartRateThresholdCheck(
        selectedRange: selectedRange,
        type: HeartRateThresholdCheckType.high,
        thresholdBpm: highHeartRateThresholdBpm,
        daySamples: result.daySamples,
        dailySummaries: result.dailySummaries,
      ),
      lowHeartRateCheck: heartRateThresholdCheck(
        selectedRange: selectedRange,
        type: HeartRateThresholdCheckType.low,
        thresholdBpm: lowHeartRateThresholdBpm,
        daySamples: result.daySamples,
        dailySummaries: result.dailySummaries,
      ),
      heartRateDay: _heartRateDay(result),
      heartRatePeriod: _heartRatePeriod(result),
      restingHeartRateDay: _restingHeartRateDay(result),
      restingHeartRatePeriod: _restingHeartRatePeriod(result),
      hrvDay: _hrvDay(result),
      hrvPeriod: _hrvPeriod(result),
      bloodPressure: _bloodPressure(result),
      spO2: _spO2(result),
      vo2Max: _vo2Max(result),
      respiratoryRate: _respiratoryRate(result),
      bodyTemperature: _bodyTemperature(result),
      bloodGlucose: _bloodGlucose(result),
      skinTemperature: _skinTemperature(result),
    );

HeartRateDayDisplay? _heartRateDay(HeartPeriodLoadResult result) {
  final samples = [...result.daySamples]
    ..sort((a, b) => a.time.compareTo(b.time));
  if (samples.isEmpty) return null;
  final bpm = samples.map((s) => s.beatsPerMinute.toDouble()).toList();
  final previousBpm =
      result.previousDaySamples.map((s) => s.beatsPerMinute.toDouble()).toList();
  final average = averageOrZero(bpm);
  final minBpm = minOf(bpm)!.round();
  final maxBpm = maxOf(bpm)!.round();
  return HeartRateDayDisplay(
    samples: samples,
    averageBpm: average.round(),
    minBpm: minBpm,
    maxBpm: maxBpm,
    chartMinValue: math.max(30, minBpm - 5).toDouble(),
    chartMaxValue: (maxBpm + 5).toDouble(),
    stats: HeartStats(
      average: average,
      low: minOf(bpm)!,
      high: maxOf(bpm)!,
      readings: samples.length,
      comparison: previousBpm.isEmpty
          ? null
          : periodComparison(average, averageOrZero(previousBpm)),
      baselineCurrentValue: average,
      baselineValues: [
        for (final summary in result.baselineDailySummaries)
          BaselineValue(date: summary.date, value: summary.avgBpm.toDouble()),
      ],
    ),
  );
}

HeartRatePeriodDisplay? _heartRatePeriod(HeartPeriodLoadResult result) {
  final summaries = [...result.dailySummaries]
    ..sort((a, b) => a.date.compareTo(b.date));
  if (summaries.isEmpty) return null;
  final average = averageOrZero(summaries.map((s) => s.avgBpm.toDouble()));
  final lowest = summaries.map((s) => s.minBpm).reduce((a, b) => a < b ? a : b);
  final highest = summaries.map((s) => s.maxBpm).reduce((a, b) => a > b ? a : b);
  return HeartRatePeriodDisplay(
    summaries: summaries,
    summariesNewestFirst: [...summaries]
      ..sort((a, b) => b.date.compareTo(a.date)),
    averageBpm: average.round(),
    lowestBpm: lowest,
    highestBpm: highest,
    stats: HeartStats(
      average: average,
      low: lowest.toDouble(),
      high: highest.toDouble(),
      readings: summaries.length,
      comparison: result.previousDailySummaries.isEmpty
          ? null
          : periodComparison(
              average,
              averageOrZero(
                result.previousDailySummaries.map((s) => s.avgBpm.toDouble()),
              ),
            ),
      baselineCurrentValue: average,
      baselineValues: [
        for (final summary in result.baselineDailySummaries)
          BaselineValue(date: summary.date, value: summary.avgBpm.toDouble()),
      ],
    ),
  );
}

RestingHeartRateDayDisplay? _restingHeartRateDay(HeartPeriodLoadResult result) {
  final samples = [...result.dayRestingSamples]
    ..sort((a, b) => a.time.compareTo(b.time));
  if (samples.isEmpty && result.dayRestingBpm == null) return null;
  // The average comes from the SAME samples as the low and the high. It used to
  // come from the provider's own day aggregate while the range came from the
  // samples we read — two different populations printed as one row, so the
  // average could sit outside the range it was printed next to. The aggregate
  // is still what we show when there are no samples to average.
  final restingBpm = samples.isEmpty
      ? result.dayRestingBpm!
      : averageOrZero(samples.map((s) => s.beatsPerMinute.toDouble())).round();
  final low = samples.isEmpty
      ? restingBpm
      : samples.map((s) => s.beatsPerMinute).reduce((a, b) => a < b ? a : b);
  final high = samples.isEmpty
      ? restingBpm
      : samples.map((s) => s.beatsPerMinute).reduce((a, b) => a > b ? a : b);
  return RestingHeartRateDayDisplay(
    samples: samples,
    restingBpm: restingBpm,
    lowBpm: low,
    highBpm: high,
    chartMinValue: math.max(30, low - 5).toDouble(),
    chartMaxValue: (high + 5).toDouble(),
    stats: HeartStats(
      average: restingBpm.toDouble(),
      low: low.toDouble(),
      high: high.toDouble(),
      readings: math.max(samples.length, 1),
      comparison:
          result.dayRestingBpm != null && result.previousDayRestingBpm != null
              ? periodComparison(
                  result.dayRestingBpm!.toDouble(),
                  result.previousDayRestingBpm!.toDouble(),
                )
              : null,
      baselineCurrentValue: restingBpm.toDouble(),
      baselineValues: [
        for (final entry in result.baselineDailyRestingHR)
          BaselineValue(date: entry.date, value: entry.bpm.toDouble()),
      ],
    ),
  );
}

RestingHeartRatePeriodDisplay? _restingHeartRatePeriod(
  HeartPeriodLoadResult result,
) {
  final entries = [...result.dailyRestingHR]
    ..sort((a, b) => a.date.compareTo(b.date));
  if (entries.isEmpty) return null;
  final bpm = entries.map((e) => e.bpm.toDouble()).toList();
  final average = averageOrZero(bpm);
  return RestingHeartRatePeriodDisplay(
    entries: entries,
    averageBpm: average.round(),
    lowBpm: minOf(bpm)!.round(),
    highBpm: maxOf(bpm)!.round(),
    stats: HeartStats(
      average: average,
      low: minOf(bpm)!,
      high: maxOf(bpm)!,
      readings: entries.length,
      comparison: result.previousDailyRestingHR.isEmpty
          ? null
          : periodComparison(
              average,
              averageOrZero(
                result.previousDailyRestingHR.map((e) => e.bpm.toDouble()),
              ),
            ),
      baselineCurrentValue: average,
      baselineValues: [
        for (final entry in result.baselineDailyRestingHR)
          BaselineValue(date: entry.date, value: entry.bpm.toDouble()),
      ],
    ),
  );
}

HrvDayDisplay? _hrvDay(HeartPeriodLoadResult result) {
  final samples = [...result.dayHrvSamples]
    ..sort((a, b) => a.time.compareTo(b.time));
  if (samples.isEmpty && result.dayHrvMs == null) return null;
  // Same population for the average as for the range — see _restingHeartRateDay.
  final hrvMs = samples.isEmpty
      ? result.dayHrvMs!
      : averageOrZero(samples.map((s) => s.rmssdMs));
  final ms = samples.map((s) => s.rmssdMs).toList();
  final low = samples.isEmpty ? hrvMs : minOf(ms)!;
  final high = samples.isEmpty ? hrvMs : maxOf(ms)!;
  return HrvDayDisplay(
    samples: samples,
    hrvMs: hrvMs,
    lowMs: low,
    highMs: high,
    chartMinValue: math.max(0, low - 5),
    chartMaxValue: high + 5,
    stats: HeartStats(
      average: hrvMs,
      low: low,
      high: high,
      readings: math.max(samples.length, 1),
      comparison: result.dayHrvMs != null && result.previousDayHrvMs != null
          ? periodComparison(result.dayHrvMs!, result.previousDayHrvMs!)
          : null,
      baselineCurrentValue: hrvMs,
      baselineValues: [
        for (final entry in result.baselineDailyHrv)
          BaselineValue(date: entry.date, value: entry.rmssdMs),
      ],
    ),
  );
}

HrvPeriodDisplay? _hrvPeriod(HeartPeriodLoadResult result) {
  final entries = [...result.dailyHrv]
    ..sort((a, b) => a.date.compareTo(b.date));
  if (entries.isEmpty) return null;
  final ms = entries.map((e) => e.rmssdMs).toList();
  final average = averageOrZero(ms);
  return HrvPeriodDisplay(
    entries: entries,
    averageMs: average,
    lowMs: minOf(ms)!,
    highMs: maxOf(ms)!,
    stats: HeartStats(
      average: average,
      low: minOf(ms)!,
      high: maxOf(ms)!,
      readings: entries.length,
      comparison: result.previousDailyHrv.isEmpty
          ? null
          : periodComparison(
              average,
              averageOrZero(result.previousDailyHrv.map((e) => e.rmssdMs)),
            ),
      baselineCurrentValue: average,
      baselineValues: [
        for (final entry in result.baselineDailyHrv)
          BaselineValue(date: entry.date, value: entry.rmssdMs),
      ],
    ),
  );
}

BloodPressureDisplay? _bloodPressure(HeartPeriodLoadResult result) {
  final entries = result.bloodPressure;
  if (entries.isEmpty) return null;
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  final averageSystolic =
      averageOrZero(entries.map((e) => e.systolicMmHg.toDouble()));
  final averageDiastolic =
      averageOrZero(entries.map((e) => e.diastolicMmHg.toDouble()));
  BloodPressureEntry? highest;
  for (final entry in entries) {
    if (highest == null ||
        entry.systolicMmHg > highest.systolicMmHg ||
        (entry.systolicMmHg == highest.systolicMmHg &&
            entry.diastolicMmHg > highest.diastolicMmHg)) {
      highest = entry;
    }
  }
  final previousAverageSystolic = result.previousBloodPressure.isEmpty
      ? null
      : averageOrZero(
          result.previousBloodPressure.map((e) => e.systolicMmHg.toDouble()),
        );
  return BloodPressureDisplay(
    entries: sorted,
    latest: sorted.last,
    stats: BloodPressureStats(
      latest: entries.reduce((a, b) => a.time.isAfter(b.time) ? a : b),
      highest: highest,
      averageSystolic: averageSystolic,
      averageDiastolic: averageDiastolic,
      comparison: previousAverageSystolic == null
          ? null
          : periodComparison(averageSystolic, previousAverageSystolic),
      readings: entries.length,
      baselineValues: [
        for (final entry in result.baselineBloodPressure)
          BaselineValue(
            date: instantToLocalDate(entry.time),
            value: entry.systolicMmHg.toDouble(),
          ),
      ],
    ),
  );
}

SpO2Display? _spO2(HeartPeriodLoadResult result) {
  final entries = result.spO2;
  if (entries.isEmpty) return null;
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  return SpO2Display(
    entries: sorted,
    latest: sorted.last,
    averagePercent: averageOrZero(entries.map((e) => e.percent)),
    stats: _vitalStats<SpO2Entry>(
      entries: entries,
      previousEntries: result.previousSpO2,
      baselineEntries: result.baselineSpO2,
      value: (e) => e.percent,
      time: (e) => e.time,
    ),
  );
}

Vo2MaxDisplay? _vo2Max(HeartPeriodLoadResult result) {
  final entries = result.vo2Max;
  if (entries.isEmpty) return null;
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  return Vo2MaxDisplay(
    entries: sorted,
    latest: sorted.last,
    stats: _vitalStats<Vo2MaxEntry>(
      entries: entries,
      previousEntries: result.previousVo2Max,
      baselineEntries: result.baselineVo2Max,
      value: (e) => e.vo2MaxMlPerKgPerMin,
      time: (e) => e.time,
    ),
  );
}

RespiratoryRateDisplay? _respiratoryRate(HeartPeriodLoadResult result) {
  final entries = result.respiratoryRate;
  if (entries.isEmpty) return null;
  final daySummaries = respiratoryRateDaySummaries(entries)
    ..sort((a, b) => b.date.compareTo(a.date));
  return RespiratoryRateDisplay(
    entries: entries,
    daySummariesNewestFirst: daySummaries,
    periodAverage: averageOrZero(daySummaries.map((summary) => summary.average)),
    entriesAverage: averageOrZero(entries.map((e) => e.breathsPerMinute)),
    stats: _vitalStats<RespiratoryRateEntry>(
      entries: entries,
      previousEntries: result.previousRespiratoryRate,
      baselineEntries: result.baselineRespiratoryRate,
      value: (e) => e.breathsPerMinute,
      time: (e) => e.time,
    ),
  );
}

BodyTemperatureDisplay? _bodyTemperature(HeartPeriodLoadResult result) {
  final entries = result.bodyTemperature;
  if (entries.isEmpty) return null;
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  return BodyTemperatureDisplay(
    entries: sorted,
    latest: sorted.last,
    stats: _vitalStats<BodyTempEntry>(
      entries: entries,
      previousEntries: result.previousBodyTemperature,
      baselineEntries: result.baselineBodyTemperature,
      value: (e) => e.temperatureCelsius,
      time: (e) => e.time,
    ),
  );
}

BloodGlucoseDisplay? _bloodGlucose(HeartPeriodLoadResult result) {
  final entries = result.bloodGlucose;
  if (entries.isEmpty) return null;
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  return BloodGlucoseDisplay(
    entries: sorted,
    averageMmolPerLiter:
        averageOrZero(sorted.map((e) => e.millimolesPerLiter)),
    stats: _vitalStats<BloodGlucoseEntry>(
      entries: entries,
      previousEntries: result.previousBloodGlucose,
      baselineEntries: result.baselineBloodGlucose,
      value: (e) => e.millimolesPerLiter,
      time: (e) => e.time,
    ),
  );
}

SkinTemperatureDisplay? _skinTemperature(HeartPeriodLoadResult result) {
  final entries = result.skinTemperature;
  if (entries.isEmpty) return null;
  final chartEntries = [
    for (final e in entries)
      if (e.averageDeltaCelsius != null) e,
  ]..sort((a, b) => a.time.compareTo(b.time));
  final values = [
    for (final entry in entries)
      if (entry.averageDeltaCelsius != null) entry.averageDeltaCelsius!,
  ];
  final previousValues = [
    for (final entry in result.previousSkinTemperature)
      if (entry.averageDeltaCelsius != null) entry.averageDeltaCelsius!,
  ];
  final average = averageOrZero(values);
  return SkinTemperatureDisplay(
    entries: entries,
    chartEntries: chartEntries,
    averageDeltaCelsius: chartEntries.isEmpty
        ? null
        : averageOrZero(chartEntries.map((e) => e.averageDeltaCelsius!)),
    stats: values.isEmpty
        ? null
        : HeartStats(
            average: average,
            low: minOf(values)!,
            high: maxOf(values)!,
            readings: entries.length,
            comparison: previousValues.isEmpty
                ? null
                : periodComparison(average, averageOrZero(previousValues)),
            baselineCurrentValue: average,
            baselineValues: [
              for (final entry in result.baselineSkinTemperature)
                if (entry.averageDeltaCelsius != null)
                  BaselineValue(
                    date: instantToLocalDate(entry.time),
                    value: entry.averageDeltaCelsius!,
                  ),
            ],
          ),
  );
}

/// The shared shape of the single-value vitals statistics (Kotlin
/// `SpO2StatisticsContent`, `Vo2MaxStatisticsContent`, …), now computed once.
HeartStats _vitalStats<T>({
  required List<T> entries,
  required List<T> previousEntries,
  required List<T> baselineEntries,
  required double Function(T) value,
  required DateTime Function(T) time,
}) {
  final values = entries.map(value).toList();
  final previousValues = previousEntries.map(value).toList();
  final average = averageOrZero(values);
  return HeartStats(
    average: average,
    low: values.isEmpty ? 0 : minOf(values)!,
    high: values.isEmpty ? 0 : maxOf(values)!,
    readings: entries.length,
    comparison: previousValues.isEmpty
        ? null
        : periodComparison(average, averageOrZero(previousValues)),
    baselineCurrentValue: average,
    baselineValues: [
      for (final entry in baselineEntries)
        BaselineValue(
          date: instantToLocalDate(time(entry)),
          value: value(entry),
        ),
    ],
  );
}
