import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/data_confidence.dart';
import '../../domain/insights/metric_interpretations.dart';
import '../../domain/insights/period_comparison.dart';
import '../../domain/insights/personal_baseline.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/vitals_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/components/data_confidence_card.dart';
import '../../ui/components/insight_cards.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_interpretation_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/paginated_entry_list.dart';
import '../../ui/components/period_comparison_stat.dart';
import '../../ui/components/personal_baseline_stat.dart';
import '../../ui/components/swipe_to_delete_entry_row.dart';
import '../../ui/theme/app_colors.dart';

/// Port of the Kotlin `HeartMetricSharedSections.kt`: the section bodies shared
/// by the ten heart + vitals period-detail screens (data confidence, metric
/// context/interpretation, statistics grids and entry lists).

/// Kotlin `metricModifier()` — the standard horizontal inset every heart
/// section content uses.
Widget heartPadded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

/// Kotlin `entryListTitle(date, ...)`: "Entries" or "Entries · Jan 5, 2026".
String heartEntryListTitle(BuildContext context, LocalDate? titleDate) {
  final l10n = AppLocalizations.of(context);
  if (titleDate == null) return l10n.sectionEntries;
  final locale = Localizations.localeOf(context).toLanguageTag();
  final formatted = DateFormat.yMMMd(locale)
      .format(DateTime(titleDate.year, titleDate.month, titleDate.day));
  return '${l10n.sectionEntries} · $formatted';
}

// ── Data confidence ──────────────────────────────────────────────────────────

/// Kotlin `HeartRawDataConfidenceContent`: confidence over raw measured
/// entries, deriving coverage and sources from the entries themselves.
class HeartRawDataConfidenceContent<T> extends StatelessWidget {
  const HeartRawDataConfidenceContent({
    super.key,
    required this.period,
    required this.entries,
    required this.source,
    required this.time,
    required this.accentColor,
  });

  final DatePeriod period;
  final List<T> entries;
  final String Function(T) source;
  final DateTime Function(T) time;
  final Color accentColor;

  @override
  Widget build(BuildContext context) => heartPadded(DataConfidenceCard(
        confidence: dataConfidence(
          period,
          [for (final entry in entries) instantToLocalDate(time(entry))],
          entries.length,
          sources: [for (final entry in entries) source(entry)],
          valueKind: DataValueKind.measured,
        ),
        accentColor: accentColor,
      ));
}

/// Kotlin `HeartAggregateDataConfidenceContent`: confidence over per-day
/// aggregates.
class HeartAggregateDataConfidenceContent extends StatelessWidget {
  const HeartAggregateDataConfidenceContent({
    super.key,
    required this.period,
    required this.trackedDates,
    required this.sampleCount,
    required this.accentColor,
  });

  final DatePeriod period;
  final Iterable<LocalDate> trackedDates;
  final int sampleCount;
  final Color accentColor;

  @override
  Widget build(BuildContext context) => heartPadded(DataConfidenceCard(
        confidence: dataConfidence(
          period,
          trackedDates,
          sampleCount,
          valueKind: DataValueKind.aggregated,
        ),
        accentColor: accentColor,
      ));
}

// ── Metric context (interpretation) cards ────────────────────────────────────

/// Kotlin `bloodPressureCategoryText`.
String bloodPressureCategoryText(
  BloodPressureCategory category,
  AppLocalizations l10n,
) =>
    switch (category) {
      BloodPressureCategory.normal => l10n.interpretationBpNormal,
      BloodPressureCategory.elevated => l10n.interpretationBpElevated,
      BloodPressureCategory.stage1 => l10n.interpretationBpStage1,
      BloodPressureCategory.stage2 => l10n.interpretationBpStage2,
      BloodPressureCategory.severeReference => l10n.interpretationBpSevere,
    };

/// Kotlin `vitalContextStatusText`.
String vitalContextStatusText(VitalContextStatus status, AppLocalizations l10n) =>
    switch (status) {
      VitalContextStatus.withinReference => l10n.interpretationVitalWithin,
      VitalContextStatus.belowReference => l10n.interpretationVitalBelow,
      VitalContextStatus.aboveReference => l10n.interpretationVitalAbove,
      VitalContextStatus.belowTypicalOxygen =>
        l10n.interpretationVitalOxygenBelowTypical,
      VitalContextStatus.lowOxygenReference =>
        l10n.interpretationVitalOxygenLow,
      VitalContextStatus.veryLowOxygenReference =>
        l10n.interpretationVitalOxygenVeryLow,
    };

/// Kotlin `BloodPressureContextCardContent`: the AHA blood-pressure category
/// card for the latest reading.
class BloodPressureContextCardContent extends StatelessWidget {
  const BloodPressureContextCardContent({super.key, required this.entry});

  final BloodPressureEntry? entry;

  @override
  Widget build(BuildContext context) {
    final latest = entry;
    final interpretation = latest == null
        ? null
        : bloodPressureInterpretation(
            latest.systolicMmHg,
            latest.diastolicMmHg,
          );
    if (interpretation == null) return const SizedBox.shrink();
    final l10n = AppLocalizations.of(context);
    final status = bloodPressureCategoryText(interpretation.category, l10n);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionMetricContext),
        heartPadded(MetricInterpretationCard(
          title: l10n.interpretationBpTitle,
          status: status,
          body: interpretation.category == BloodPressureCategory.severeReference
              ? l10n.interpretationBpSevereBody
              : l10n.interpretationBpBody(status),
          source: l10n.interpretationBpSource,
          icon: Icons.favorite_outline,
          accentColor: AppColors.vitals,
          severity: interpretation.severity,
        )),
      ],
    );
  }
}

/// Kotlin `VitalContextCardContent`: the generic reference-range card.
class VitalContextCardContent extends StatelessWidget {
  const VitalContextCardContent({
    super.key,
    required this.interpretation,
    required this.body,
    required this.source,
    required this.icon,
    required this.accentColor,
  });

  final VitalContextInterpretation? interpretation;
  final String Function(AppLocalizations) body;
  final String Function(AppLocalizations) source;
  final IconData icon;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final resolved = interpretation;
    if (resolved == null) return const SizedBox.shrink();
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionMetricContext),
        heartPadded(MetricInterpretationCard(
          title: l10n.interpretationVitalTitle,
          status: vitalContextStatusText(resolved.status, l10n),
          body: body(l10n),
          source: source(l10n),
          icon: icon,
          accentColor: accentColor,
          severity: resolved.severity,
        )),
      ],
    );
  }
}

/// Kotlin `RestingHeartRateContextCardContent`.
Widget restingHeartRateContextCardContent(int bpm) => VitalContextCardContent(
      interpretation: restingHeartRateContext(bpm),
      body: (l10n) => l10n.interpretationVitalRestingHrBody,
      source: (l10n) => l10n.interpretationVitalSource,
      icon: Icons.favorite_border,
      accentColor: AppColors.heart,
    );

/// Kotlin `OxygenSaturationContextCardContent`.
Widget oxygenSaturationContextCardContent(SpO2Entry? entry, Color accentColor) =>
    VitalContextCardContent(
      interpretation:
          entry == null ? null : oxygenSaturationContext(entry.percent),
      body: (l10n) => l10n.interpretationVitalOxygenBody,
      source: (l10n) => l10n.interpretationOxygenSource,
      icon: Icons.favorite_border,
      accentColor: accentColor,
    );

/// Kotlin `RespiratoryRateContextCardContent`.
Widget respiratoryRateContextCardContent(
  double breathsPerMinute,
  Color accentColor,
) =>
    VitalContextCardContent(
      interpretation: respiratoryRateContext(breathsPerMinute),
      body: (l10n) => l10n.interpretationVitalRespiratoryBody,
      source: (l10n) => l10n.interpretationVitalSource,
      icon: Icons.favorite_outline,
      accentColor: accentColor,
    );

/// Kotlin `BodyTemperatureContextCardContent`.
Widget bodyTemperatureContextCardContent(
  BodyTempEntry? entry,
  Color accentColor,
) =>
    VitalContextCardContent(
      interpretation: entry == null
          ? null
          : bodyTemperatureContext(entry.temperatureCelsius),
      body: (l10n) => l10n.interpretationVitalTemperatureBody,
      source: (l10n) => l10n.interpretationVitalSource,
      icon: Icons.device_thermostat_outlined,
      accentColor: accentColor,
    );

// ── Statistics ────────────────────────────────────────────────────────────────

double _average(Iterable<double> values) {
  final list = values.toList();
  if (list.isEmpty) return 0;
  return list.reduce((a, b) => a + b) / list.length;
}

/// Kotlin `HeartNumericStatisticsContent`: the shared avg/low/high/readings
/// grid, plus the previous-period comparison and the personal-baseline stats.
class HeartNumericStatisticsContent extends StatelessWidget {
  const HeartNumericStatisticsContent({
    super.key,
    required this.unitFormatter,
    required this.average,
    required this.low,
    required this.high,
    required this.readings,
    this.comparison,
    required this.selectedRange,
    required this.comparisonValueFormatter,
    required this.icon,
    required this.accentColor,
    this.countInLoggedDays = false,
    this.period,
    this.baselineCurrentValue,
    this.baselineValues = const [],
  });

  final UnitFormatter unitFormatter;
  final DisplayValue average;
  final DisplayValue low;
  final DisplayValue high;
  final int readings;
  final PeriodComparison? comparison;
  final TimeRange selectedRange;
  final DisplayValue Function(double) comparisonValueFormatter;
  final IconData icon;
  final Color accentColor;

  /// Kotlin `countTitleRes = R.string.metric_logged_days` +
  /// `countUnitRes = R.string.unit_days`.
  final bool countInLoggedDays;
  final DatePeriod? period;
  final double? baselineCurrentValue;
  final List<BaselineValue> baselineValues;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final periodValue = period;
    final baselineValue = baselineCurrentValue;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionStatistics),
        heartPadded(InsightStatGrid(
          stats: [
            InsightStat(
              title: l10n.statAverage,
              value: average.value,
              unit: average.unit,
              icon: icon,
              accentColor: accentColor,
            ),
            InsightStat(
              title: l10n.statLowest,
              value: low.value,
              unit: low.unit,
              icon: Icons.star_outline,
              accentColor: accentColor,
            ),
            InsightStat(
              title: l10n.statHighest,
              value: high.value,
              unit: high.unit,
              icon: Icons.calendar_month_outlined,
              accentColor: accentColor,
            ),
            InsightStat(
              title: countInLoggedDays ? l10n.metricLoggedDays : l10n.statReadings,
              value: unitFormatter.count(readings),
              unit: countInLoggedDays ? l10n.unitDays : '',
              icon: Icons.check_circle_outline,
              accentColor: accentColor,
            ),
            if (comparison != null)
              previousPeriodInsightStat(
                comparison: comparison!,
                selectedRange: selectedRange,
                unitFormatter: unitFormatter,
                valueFormatter: comparisonValueFormatter,
                accentColor: accentColor,
                l10n: l10n,
              ),
            if (periodValue != null && baselineValue != null)
              ...personalBaselineInsightStats(
                insight: personalBaselineInsight(
                  baselineValue,
                  baselineValues,
                  periodValue.start.minusDays(1),
                ),
                unitFormatter: unitFormatter,
                valueFormatter: comparisonValueFormatter,
                accentColor: accentColor,
                l10n: l10n,
              ),
          ],
        )),
      ],
    );
  }
}

/// Kotlin `BloodPressureStatisticsContent`: Latest/Average/Highest as
/// systolic/diastolic pairs + readings, previous-period (systolic average) and
/// systolic personal baseline.
class BloodPressureStatisticsContent extends StatelessWidget {
  const BloodPressureStatisticsContent({
    super.key,
    required this.entries,
    required this.previousEntries,
    required this.baselineEntries,
    required this.period,
    required this.selectedRange,
    required this.unitFormatter,
  });

  final List<BloodPressureEntry> entries;
  final List<BloodPressureEntry> previousEntries;
  final List<BloodPressureEntry> baselineEntries;
  final DatePeriod period;
  final TimeRange selectedRange;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final latest = entries.isEmpty
        ? null
        : entries.reduce((a, b) => a.time.isAfter(b.time) ? a : b);
    final average = unitFormatter.bloodPressure(
      _average(entries.map((e) => e.systolicMmHg.toDouble())).round(),
      _average(entries.map((e) => e.diastolicMmHg.toDouble())).round(),
    );
    BloodPressureEntry? highestEntry;
    for (final entry in entries) {
      if (highestEntry == null ||
          entry.systolicMmHg > highestEntry.systolicMmHg ||
          (entry.systolicMmHg == highestEntry.systolicMmHg &&
              entry.diastolicMmHg > highestEntry.diastolicMmHg)) {
        highestEntry = entry;
      }
    }
    final highest = highestEntry == null
        ? unitFormatter.bloodPressure(0, 0)
        : unitFormatter.bloodPressure(
            highestEntry.systolicMmHg,
            highestEntry.diastolicMmHg,
          );
    final previousAverageSystolic = previousEntries.isEmpty
        ? null
        : _average(previousEntries.map((e) => e.systolicMmHg.toDouble()));
    final latestDisplay = latest == null
        ? null
        : unitFormatter.bloodPressure(latest.systolicMmHg, latest.diastolicMmHg);

    DisplayValue systolicDisplay(double value) =>
        DisplayValue(unitFormatter.count(value.round()), 'mmHg');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionStatistics),
        heartPadded(InsightStatGrid(
          stats: [
            InsightStat(
              title: l10n.metricLatest,
              value: latestDisplay?.value ?? '',
              unit: latestDisplay?.unit ?? '',
              icon: Icons.favorite_outline,
              accentColor: AppColors.vitals,
            ),
            InsightStat(
              title: l10n.statAverage,
              value: average.value,
              unit: average.unit,
              icon: Icons.star_outline,
              accentColor: AppColors.vitals,
            ),
            InsightStat(
              title: l10n.statHighest,
              value: highest.value,
              unit: highest.unit,
              icon: Icons.calendar_month_outlined,
              accentColor: AppColors.vitals,
            ),
            InsightStat(
              title: l10n.statReadings,
              value: unitFormatter.count(entries.length),
              unit: '',
              icon: Icons.check_circle_outline,
              accentColor: AppColors.vitals,
            ),
            if (previousAverageSystolic != null)
              previousPeriodInsightStat(
                comparison: periodComparison(
                  _average(entries.map((e) => e.systolicMmHg.toDouble())),
                  previousAverageSystolic,
                ),
                selectedRange: selectedRange,
                unitFormatter: unitFormatter,
                valueFormatter: systolicDisplay,
                accentColor: AppColors.vitals,
                l10n: l10n,
              ),
            ...personalBaselineInsightStats(
              insight: personalBaselineInsight(
                _average(entries.map((e) => e.systolicMmHg.toDouble())),
                [
                  for (final entry in baselineEntries)
                    BaselineValue(
                      date: instantToLocalDate(entry.time),
                      value: entry.systolicMmHg.toDouble(),
                    ),
                ],
                period.start.minusDays(1),
              ),
              unitFormatter: unitFormatter,
              valueFormatter: systolicDisplay,
              accentColor: AppColors.vitals,
              l10n: l10n,
            ),
          ],
        )),
      ],
    );
  }
}

/// Kotlin `HeartRateSampleStatisticsContent` (day range: raw samples, baseline
/// fed from the daily summaries).
Widget heartRateSampleStatisticsContent({
  required List<HeartRateSample> samples,
  required List<HeartRateSample> previousSamples,
  required List<HeartRateSummary> baselineSummaries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
}) {
  final values = samples.map((s) => s.beatsPerMinute.toDouble()).toList();
  final previousValues =
      previousSamples.map((s) => s.beatsPerMinute.toDouble()).toList();
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: unitFormatter.heartRate(_average(values).round()),
    low: unitFormatter.heartRate(
        values.isEmpty ? 0 : values.reduce((a, b) => a < b ? a : b).round()),
    high: unitFormatter.heartRate(
        values.isEmpty ? 0 : values.reduce((a, b) => a > b ? a : b).round()),
    readings: samples.length,
    comparison: previousValues.isEmpty
        ? null
        : periodComparison(_average(values), _average(previousValues)),
    selectedRange: selectedRange,
    comparisonValueFormatter: (value) => unitFormatter.heartRate(value.round()),
    icon: Icons.favorite_outline,
    accentColor: AppColors.heart,
    period: period,
    baselineCurrentValue: _average(values),
    baselineValues: [
      for (final summary in baselineSummaries)
        BaselineValue(date: summary.date, value: summary.avgBpm.toDouble()),
    ],
  );
}

/// Kotlin `HeartRateSummaryStatisticsContent` (period range: daily summaries).
Widget heartRateSummaryStatisticsContent({
  required List<HeartRateSummary> summaries,
  required List<HeartRateSummary> previousSummaries,
  required List<HeartRateSummary> baselineSummaries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
}) {
  final average = _average(summaries.map((s) => s.avgBpm.toDouble()));
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: unitFormatter.heartRate(average.round()),
    low: unitFormatter.heartRate(summaries.isEmpty
        ? 0
        : summaries.map((s) => s.minBpm).reduce((a, b) => a < b ? a : b)),
    high: unitFormatter.heartRate(summaries.isEmpty
        ? 0
        : summaries.map((s) => s.maxBpm).reduce((a, b) => a > b ? a : b)),
    readings: summaries.length,
    comparison: previousSummaries.isEmpty
        ? null
        : periodComparison(
            average,
            _average(previousSummaries.map((s) => s.avgBpm.toDouble())),
          ),
    selectedRange: selectedRange,
    comparisonValueFormatter: (value) => unitFormatter.heartRate(value.round()),
    icon: Icons.favorite_outline,
    accentColor: AppColors.heart,
    countInLoggedDays: true,
    period: period,
    baselineCurrentValue: average,
    baselineValues: [
      for (final summary in baselineSummaries)
        BaselineValue(date: summary.date, value: summary.avgBpm.toDouble()),
    ],
  );
}

/// Kotlin `RestingHeartRateStatisticsContent`.
Widget restingHeartRateStatisticsContent({
  required List<DailyRestingHR> entries,
  required List<DailyRestingHR> previousEntries,
  required List<DailyRestingHR> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
}) {
  final average = _average(entries.map((e) => e.bpm.toDouble()));
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: unitFormatter.heartRate(average.round()),
    low: unitFormatter.heartRate(entries.isEmpty
        ? 0
        : entries.map((e) => e.bpm).reduce((a, b) => a < b ? a : b)),
    high: unitFormatter.heartRate(entries.isEmpty
        ? 0
        : entries.map((e) => e.bpm).reduce((a, b) => a > b ? a : b)),
    readings: entries.length,
    comparison: previousEntries.isEmpty
        ? null
        : periodComparison(
            average,
            _average(previousEntries.map((e) => e.bpm.toDouble())),
          ),
    selectedRange: selectedRange,
    comparisonValueFormatter: (value) => unitFormatter.heartRate(value.round()),
    icon: Icons.favorite_border,
    accentColor: AppColors.heart,
    countInLoggedDays: true,
    period: period,
    baselineCurrentValue: average,
    baselineValues: [
      for (final entry in baselineEntries)
        BaselineValue(date: entry.date, value: entry.bpm.toDouble()),
    ],
  );
}

/// Kotlin `HrvStatisticsContent`.
Widget hrvStatisticsContent({
  required List<DailyHrv> entries,
  required List<DailyHrv> previousEntries,
  required List<DailyHrv> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
}) {
  final average = _average(entries.map((e) => e.rmssdMs));
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: unitFormatter.hrv(average),
    low: unitFormatter.hrv(entries.isEmpty
        ? 0
        : entries.map((e) => e.rmssdMs).reduce((a, b) => a < b ? a : b)),
    high: unitFormatter.hrv(entries.isEmpty
        ? 0
        : entries.map((e) => e.rmssdMs).reduce((a, b) => a > b ? a : b)),
    readings: entries.length,
    comparison: previousEntries.isEmpty
        ? null
        : periodComparison(
            average,
            _average(previousEntries.map((e) => e.rmssdMs)),
          ),
    selectedRange: selectedRange,
    comparisonValueFormatter: unitFormatter.hrv,
    icon: Icons.favorite_border,
    accentColor: AppColors.heart,
    countInLoggedDays: true,
    period: period,
    baselineCurrentValue: average,
    baselineValues: [
      for (final entry in baselineEntries)
        BaselineValue(date: entry.date, value: entry.rmssdMs),
    ],
  );
}

/// The shared shape of the single-value vitals statistics (Kotlin
/// `SpO2StatisticsContent`, `Vo2MaxStatisticsContent`, …).
Widget _vitalStatisticsContent<T>({
  required List<T> entries,
  required List<T> previousEntries,
  required List<T> baselineEntries,
  required double Function(T) value,
  required DateTime Function(T) time,
  required DisplayValue Function(double) format,
  required IconData icon,
  required Color accentColor,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
}) {
  final values = entries.map(value).toList();
  final previousValues = previousEntries.map(value).toList();
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: format(_average(values)),
    low: format(values.isEmpty ? 0 : values.reduce((a, b) => a < b ? a : b)),
    high: format(values.isEmpty ? 0 : values.reduce((a, b) => a > b ? a : b)),
    readings: entries.length,
    comparison: previousValues.isEmpty
        ? null
        : periodComparison(_average(values), _average(previousValues)),
    selectedRange: selectedRange,
    comparisonValueFormatter: format,
    icon: icon,
    accentColor: accentColor,
    period: period,
    baselineCurrentValue: _average(values),
    baselineValues: [
      for (final entry in baselineEntries)
        BaselineValue(date: instantToLocalDate(time(entry)), value: value(entry)),
    ],
  );
}

/// Kotlin `SpO2StatisticsContent`.
Widget spO2StatisticsContent({
  required List<SpO2Entry> entries,
  required List<SpO2Entry> previousEntries,
  required List<SpO2Entry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) =>
    _vitalStatisticsContent<SpO2Entry>(
      entries: entries,
      previousEntries: previousEntries,
      baselineEntries: baselineEntries,
      value: (e) => e.percent,
      time: (e) => e.time,
      format: unitFormatter.percent,
      icon: Icons.favorite_border,
      accentColor: accentColor,
      period: period,
      selectedRange: selectedRange,
      unitFormatter: unitFormatter,
    );

/// Kotlin `Vo2MaxStatisticsContent`.
Widget vo2MaxStatisticsContent({
  required List<Vo2MaxEntry> entries,
  required List<Vo2MaxEntry> previousEntries,
  required List<Vo2MaxEntry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) =>
    _vitalStatisticsContent<Vo2MaxEntry>(
      entries: entries,
      previousEntries: previousEntries,
      baselineEntries: baselineEntries,
      value: (e) => e.vo2MaxMlPerKgPerMin,
      time: (e) => e.time,
      format: unitFormatter.vo2Max,
      icon: Icons.speed_outlined,
      accentColor: accentColor,
      period: period,
      selectedRange: selectedRange,
      unitFormatter: unitFormatter,
    );

/// Kotlin `RespiratoryRateStatisticsContent`.
Widget respiratoryRateStatisticsContent({
  required List<RespiratoryRateEntry> entries,
  required List<RespiratoryRateEntry> previousEntries,
  required List<RespiratoryRateEntry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) =>
    _vitalStatisticsContent<RespiratoryRateEntry>(
      entries: entries,
      previousEntries: previousEntries,
      baselineEntries: baselineEntries,
      value: (e) => e.breathsPerMinute,
      time: (e) => e.time,
      format: unitFormatter.respiratoryRate,
      icon: Icons.favorite_outline,
      accentColor: accentColor,
      period: period,
      selectedRange: selectedRange,
      unitFormatter: unitFormatter,
    );

/// Kotlin `BodyTemperatureStatisticsContent`.
Widget bodyTemperatureStatisticsContent({
  required List<BodyTempEntry> entries,
  required List<BodyTempEntry> previousEntries,
  required List<BodyTempEntry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) =>
    _vitalStatisticsContent<BodyTempEntry>(
      entries: entries,
      previousEntries: previousEntries,
      baselineEntries: baselineEntries,
      value: (e) => e.temperatureCelsius,
      time: (e) => e.time,
      format: unitFormatter.temperature,
      icon: Icons.device_thermostat_outlined,
      accentColor: accentColor,
      period: period,
      selectedRange: selectedRange,
      unitFormatter: unitFormatter,
    );

/// Kotlin `BloodGlucoseStatisticsContent`.
Widget bloodGlucoseStatisticsContent({
  required List<BloodGlucoseEntry> entries,
  required List<BloodGlucoseEntry> previousEntries,
  required List<BloodGlucoseEntry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) =>
    _vitalStatisticsContent<BloodGlucoseEntry>(
      entries: entries,
      previousEntries: previousEntries,
      baselineEntries: baselineEntries,
      value: (e) => e.millimolesPerLiter,
      time: (e) => e.time,
      format: unitFormatter.bloodGlucose,
      icon: Icons.favorite_outline,
      accentColor: accentColor,
      period: period,
      selectedRange: selectedRange,
      unitFormatter: unitFormatter,
    );

/// Kotlin `SkinTemperatureStatisticsContent`: statistics over the
/// delta-from-baseline values, formatted as temperature deltas. Entries with no
/// delta are excluded from the math (but still count as readings), and the
/// whole grid is omitted when no entry carries a delta.
Widget skinTemperatureStatisticsContent({
  required List<SkinTemperatureEntry> entries,
  required List<SkinTemperatureEntry> previousEntries,
  required List<SkinTemperatureEntry> baselineEntries,
  required DatePeriod period,
  required TimeRange selectedRange,
  required UnitFormatter unitFormatter,
  required Color accentColor,
}) {
  final values = [
    for (final entry in entries)
      if (entry.averageDeltaCelsius != null) entry.averageDeltaCelsius!,
  ];
  if (values.isEmpty) return const SizedBox.shrink();
  final previousValues = [
    for (final entry in previousEntries)
      if (entry.averageDeltaCelsius != null) entry.averageDeltaCelsius!,
  ];
  return HeartNumericStatisticsContent(
    unitFormatter: unitFormatter,
    average: unitFormatter.temperatureDelta(_average(values)),
    low: unitFormatter
        .temperatureDelta(values.reduce((a, b) => a < b ? a : b)),
    high: unitFormatter
        .temperatureDelta(values.reduce((a, b) => a > b ? a : b)),
    readings: entries.length,
    comparison: previousValues.isEmpty
        ? null
        : periodComparison(_average(values), _average(previousValues)),
    selectedRange: selectedRange,
    comparisonValueFormatter: unitFormatter.temperatureDelta,
    icon: Icons.device_thermostat_outlined,
    accentColor: accentColor,
    period: period,
    baselineCurrentValue: _average(values),
    baselineValues: [
      for (final entry in baselineEntries)
        if (entry.averageDeltaCelsius != null)
          BaselineValue(
            date: instantToLocalDate(entry.time),
            value: entry.averageDeltaCelsius!,
          ),
    ],
  );
}

/// Kotlin `SkinTemperatureEntry.skinTemperatureValue`: the delta when present,
/// otherwise the absolute baseline, otherwise blank.
String skinTemperatureValueText(
  SkinTemperatureEntry entry,
  UnitFormatter unitFormatter,
) {
  final delta = entry.averageDeltaCelsius;
  if (delta != null) return unitFormatter.temperatureDelta(delta).text;
  final baseline = entry.baselineCelsius;
  if (baseline != null) return unitFormatter.temperature(baseline).text;
  return '';
}

// ── Entry lists ───────────────────────────────────────────────────────────────

/// Kotlin `VitalsReadingRow` (`HeartVitalsRows.kt`): a reading with its
/// timestamp and source; manual OpenVitals entries can be edited (pencil) and
/// deleted (end-to-start swipe).
class VitalsReadingRow extends StatelessWidget {
  const VitalsReadingRow({
    super.key,
    required this.label,
    required this.source,
    required this.time,
    this.onEdit,
    this.onDelete,
    this.dismissKey,
  });

  final String label;
  final String source;
  final DateTime time;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  /// Uniquely identifies the row for the swipe-to-delete dismissible.
  final Key? dismissKey;

  @override
  Widget build(BuildContext context) {
    final content = _content(context);
    final delete = onDelete;
    if (delete == null) return content;
    return SwipeToDeleteEntryRow(
      key: dismissKey ?? ValueKey('vitals-reading-$time-$label'),
      onDelete: delete,
      child: content,
    );
  }

  Widget _content(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final local = time.toLocal();
    final dateText = DateFormat.yMMMd(locale).format(local);
    final timeText = DateFormat.jm(locale).format(local);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: theme.textTheme.titleSmall),
                  Text(
                    '$dateText · $timeText',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            SourceChip(source: source),
            if (onEdit != null) ...[
              const SizedBox(width: 4),
              IconButton(
                onPressed: onEdit,
                tooltip: l10n.cdEditEntry,
                icon: Icon(
                  Icons.edit_outlined,
                  size: 20,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Kotlin `HeartEntryListContent`: the paginated raw-reading list, newest
/// first. Manual OpenVitals entries expose edit + swipe-delete.
class HeartEntryListContent<T> extends StatelessWidget {
  const HeartEntryListContent({
    super.key,
    required this.entries,
    required this.value,
    required this.source,
    required this.time,
    this.titleDate,
    this.editable,
    this.onEdit,
    this.onDelete,
    this.entryKey,
  });

  final List<T> entries;
  final String Function(T) value;
  final String Function(T) source;
  final DateTime Function(T) time;
  final LocalDate? titleDate;
  final bool Function(T)? editable;
  final void Function(T)? onEdit;
  final void Function(T)? onDelete;

  /// A stable identity for swipe-to-delete rows (e.g. the entry id).
  final Object Function(T)? entryKey;

  @override
  Widget build(BuildContext context) {
    if (entries.isEmpty) return const SizedBox.shrink();
    final sorted = [...entries]..sort((a, b) => time(b).compareTo(time(a)));
    return PaginatedEntryList<T>(
      title: heartEntryListTitle(context, titleDate),
      entries: sorted,
      rowBuilder: (context, entry) {
        final isEditable = editable?.call(entry) ?? false;
        final edit = onEdit;
        final delete = onDelete;
        return VitalsReadingRow(
          label: value(entry),
          source: source(entry),
          time: time(entry),
          onEdit: isEditable && edit != null ? () => edit(entry) : null,
          onDelete: isEditable && delete != null ? () => delete(entry) : null,
          dismissKey: ValueKey(
            'heart-entry-${entryKey?.call(entry) ?? '${time(entry)}-${value(entry)}'}',
          ),
        );
      },
    );
  }
}

/// Kotlin `HeartDailyEntryRow`: one card per tracked day.
class HeartDailyEntryRow extends StatelessWidget {
  const HeartDailyEntryRow({
    super.key,
    required this.date,
    required this.value,
    required this.accentColor,
  });

  final LocalDate date;
  final String value;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Text(
                DateFormat.yMMMd(locale)
                    .format(DateTime(date.year, date.month, date.day)),
                style: theme.textTheme.bodyMedium,
              ),
            ),
            Text(
              value,
              style: theme.textTheme.titleMedium?.copyWith(color: accentColor),
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `HeartDailyEntryListContent`: paginated one-value-per-day list,
/// newest first.
class HeartDailyEntryListContent<T> extends StatelessWidget {
  const HeartDailyEntryListContent({
    super.key,
    required this.entries,
    required this.date,
    required this.value,
    required this.accentColor,
    this.titleDate,
  });

  final List<T> entries;
  final LocalDate Function(T) date;
  final String Function(T) value;
  final Color accentColor;
  final LocalDate? titleDate;

  @override
  Widget build(BuildContext context) {
    if (entries.isEmpty) return const SizedBox.shrink();
    final sorted = [...entries]..sort((a, b) => date(b).compareTo(date(a)));
    return PaginatedEntryList<T>(
      title: heartEntryListTitle(context, titleDate),
      entries: sorted,
      rowBuilder: (context, entry) => HeartDailyEntryRow(
        date: date(entry),
        value: value(entry),
        accentColor: accentColor,
      ),
    );
  }
}
