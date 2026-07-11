import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/period_comparison.dart';
import '../../domain/insights/personal_baseline.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/vitals_models.dart';
import '../../domain/usecase/load_heart_period_use_case.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/line_chart.dart';
import '../../ui/components/data_source_education_item.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/paginated_entry_list.dart';
import '../../ui/theme/app_colors.dart';
import 'heart_metric.dart';
import 'heart_metric_cards.dart';
import 'heart_metric_notifier.dart';
import 'heart_metric_ordered_sections.dart';
import 'heart_metric_shared_sections.dart';

/// Called when a manual OpenVitals measurement entry should be edited/deleted.
typedef VitalsMeasurementCallback = void Function(
  VitalsMeasurementType type,
  String entryId,
);

/// The per-metric detail content of the heart + vitals screens, ported from the
/// Kotlin `HeartMetricContent.kt`, `HeartVitalDetailContent.kt` and
/// `VitalsBloodPressureContent.kt`. Every metric renders through the
/// user-reorderable [heartChartMetricSections] skeleton.
class HeartMetricContentView extends StatelessWidget {
  const HeartMetricContentView({
    super.key,
    required this.metric,
    required this.state,
    required this.formatter,
    required this.period,
    required this.weekPeriodMode,
    required this.onDecreaseHighHeartRateThreshold,
    required this.onIncreaseHighHeartRateThreshold,
    required this.onDecreaseLowHeartRateThreshold,
    required this.onIncreaseLowHeartRateThreshold,
    required this.onEditVitalsMeasurement,
    required this.onDeleteVitalsMeasurement,
  });

  final HeartMetric metric;
  final HeartMetricState state;
  final UnitFormatter formatter;
  final DatePeriod period;

  /// Kept in step with the period navigator's own mode, so a rolling month reads
  /// "Last 30 days" in every chart summary too, not "This month".
  final WeekPeriodMode weekPeriodMode;
  final VoidCallback onDecreaseHighHeartRateThreshold;
  final VoidCallback onIncreaseHighHeartRateThreshold;
  final VoidCallback onDecreaseLowHeartRateThreshold;
  final VoidCallback onIncreaseLowHeartRateThreshold;
  final VitalsMeasurementCallback onEditVitalsMeasurement;
  final VitalsMeasurementCallback onDeleteVitalsMeasurement;

  @override
  Widget build(BuildContext context) {
    final result = state.result;
    if (result == null) {
      if (state.isLoading) return const _LoadingBlock();
      return _placeholder();
    }
    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _content(context, result, daySelection),
    );
  }

  Widget _content(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    switch (metric) {
      case HeartMetric.averageHeartRate:
        return _averageHeartRate(context, result, daySelection);
      case HeartMetric.restingHeartRate:
        return _restingHeartRate(context, result, daySelection);
      case HeartMetric.hrv:
        return _hrv(context, result, daySelection);
      case HeartMetric.bloodPressure:
        return _bloodPressure(context, result);
      case HeartMetric.spo2:
        return _spo2(context, result, daySelection);
      case HeartMetric.vo2Max:
        return _vo2Max(context, result);
      case HeartMetric.respiratoryRate:
        return _respiratoryRate(context, result, daySelection);
      case HeartMetric.bodyTemperature:
        return _bodyTemperature(context, result);
      case HeartMetric.bloodGlucose:
        return _bloodGlucose(context, result, daySelection);
      case HeartMetric.skinTemperature:
        return _skinTemperature(context, result, daySelection);
    }
  }

  // ── Average heart rate (Kotlin `averageHeartRateContent`) ─────────────────

  Widget _averageHeartRate(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    if (isDay && result.daySamples.isNotEmpty) {
      final samples = [...result.daySamples]
        ..sort((a, b) => a.time.compareTo(b.time));
      final bpm = samples.map((s) => s.beatsPerMinute.toDouble()).toList();
      final minBpm = _min(bpm).round();
      final maxBpm = _max(bpm).round();
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: samples.length > 1
            ? heartPadded(HeartTimelineCard(
                date: state.selectedDate,
                points: [
                  for (final s in samples) (s.time, s.beatsPerMinute.toDouble())
                ],
                averageText: formatter.heartRate(_avg(bpm).round()).text,
                rangeText:
                    '${formatter.heartRate(minBpm).text}-${formatter.heartRate(maxBpm).text}',
                valueFormatter: (value) =>
                    formatter.heartRate(value.round()).text,
                minValue: math.max(30, minBpm - 5).toDouble(),
                maxValue: (maxBpm + 5).toDouble(),
              ))
            : null,
        highlightCard: _thresholdChecks(result),
        dataConfidence: HeartRawDataConfidenceContent<HeartRateSample>(
          period: period,
          entries: samples,
          source: (s) => s.source,
          time: (s) => s.time,
          accentColor: AppColors.heart,
        ),
        statistics: heartRateSampleStatisticsContent(
          samples: samples,
          previousSamples: result.previousDaySamples,
          baselineSummaries: result.baselineDailySummaries,
          period: period,
          selectedRange: state.selectedRange,
          unitFormatter: formatter,
        ),
        entries: HeartEntryListContent<HeartRateSample>(
          entries: samples,
          value: (s) => formatter.heartRate(s.beatsPerMinute).text,
          source: (s) => s.source,
          time: (s) => s.time,
        ),
      );
    }
    if (isDay) {
      if (state.isLoading) return const _LoadingBlock();
      return heartPadded(const HeartRateEmptyDayCard());
    }

    final summaries = [...result.dailySummaries]
      ..sort((a, b) => a.date.compareTo(b.date));
    if (summaries.isEmpty) return _emptyOrLoading();

    final average = _avg(summaries.map((s) => s.avgBpm.toDouble())).round();
    final lowest =
        summaries.map((s) => s.minBpm).reduce((a, b) => a < b ? a : b);
    final highest =
        summaries.map((s) => s.maxBpm).reduce((a, b) => a > b ? a : b);
    final selectedDay = daySelection.selectedDate;

    final periodSections = heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _heartRateSeries(summaries, l10n),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryAvgValueRange(
            formatter.heartRate(average).text,
            formatter.heartRate(lowest).text,
            formatter.heartRate(highest).text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.heartRate(value.round()).text,
      )),
      highlightCard: _thresholdChecks(result),
      selectedDayEntries: selectedDay == null
          ? null
          : PaginatedEntryList<HeartRateSummary>(
              title: heartEntryListTitle(context, selectedDay),
              entries: [
                for (final summary in summaries)
                  if (summary.date == selectedDay) summary,
              ],
              rowBuilder: (context, summary) => HeartRateDayRow(
                summary: summary,
                unitFormatter: formatter,
              ),
            ),
      dataConfidence: HeartAggregateDataConfidenceContent(
        period: period,
        trackedDates: [for (final s in summaries) s.date],
        sampleCount: summaries.length,
        accentColor: AppColors.heart,
      ),
      statistics: heartRateSummaryStatisticsContent(
        summaries: summaries,
        previousSummaries: result.previousDailySummaries,
        baselineSummaries: result.baselineDailySummaries,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
      ),
      entries: PaginatedEntryList<HeartRateSummary>(
        title: l10n.sectionDailyBreakdown,
        entries: [...summaries]..sort((a, b) => b.date.compareTo(a.date)),
        rowBuilder: (context, summary) => HeartRateDayRow(
          summary: summary,
          unitFormatter: formatter,
        ),
      ),
    );
    // Kotlin `averageHeartRateContent` renders `dataSourceEducationItem()` as a
    // bare trailing item after the period sections (HeartMetricContent.kt:219),
    // outside the reorderable section set.
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        periodSections,
        const DataSourceEducationItem(),
      ],
    );
  }

  Widget _thresholdChecks(HeartPeriodLoadResult result) =>
      heartPadded(HeartRateThresholdChecksContent(
        highCheck: heartRateThresholdCheck(
          selectedRange: state.selectedRange,
          type: HeartRateThresholdCheckType.high,
          thresholdBpm: state.highHeartRateThresholdBpm,
          daySamples: result.daySamples,
          dailySummaries: result.dailySummaries,
        ),
        lowCheck: heartRateThresholdCheck(
          selectedRange: state.selectedRange,
          type: HeartRateThresholdCheckType.low,
          thresholdBpm: state.lowHeartRateThresholdBpm,
          daySamples: result.daySamples,
          dailySummaries: result.dailySummaries,
        ),
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        onDecreaseHighThreshold: onDecreaseHighHeartRateThreshold,
        onIncreaseHighThreshold: onIncreaseHighHeartRateThreshold,
        onDecreaseLowThreshold: onDecreaseLowHeartRateThreshold,
        onIncreaseLowThreshold: onIncreaseLowHeartRateThreshold,
      ));

  // ── Resting heart rate (Kotlin `restingHeartRateContent`) ─────────────────

  Widget _restingHeartRate(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final daySamples = [...result.dayRestingSamples]
      ..sort((a, b) => a.time.compareTo(b.time));
    final hasDayResting =
        isDay && (daySamples.isNotEmpty || result.dayRestingBpm != null);
    if (hasDayResting) {
      final restingBpm = result.dayRestingBpm ??
          _avg(daySamples.map((s) => s.beatsPerMinute.toDouble())).round();
      final lowResting = daySamples.isEmpty
          ? restingBpm
          : daySamples.map((s) => s.beatsPerMinute).reduce((a, b) => a < b ? a : b);
      final highResting = daySamples.isEmpty
          ? restingBpm
          : daySamples.map((s) => s.beatsPerMinute).reduce((a, b) => a > b ? a : b);
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: daySamples.length > 1
            ? heartPadded(HeartTimelineCard(
                date: state.selectedDate,
                points: [
                  for (final s in daySamples)
                    (s.time, s.beatsPerMinute.toDouble())
                ],
                averageText: formatter.heartRate(restingBpm).text,
                rangeText:
                    '${formatter.heartRate(lowResting).text}-${formatter.heartRate(highResting).text}',
                valueFormatter: (value) =>
                    formatter.heartRate(value.round()).text,
                minValue: math.max(30, lowResting - 5).toDouble(),
                maxValue: (highResting + 5).toDouble(),
              ))
            : null,
        highlightCard: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            heartPadded(HeartDayValueCard(
              title: metric.title,
              value: formatter.heartRate(restingBpm).text,
            )),
            restingHeartRateContextCardContent(restingBpm),
          ],
        ),
        dataConfidence: daySamples.isNotEmpty
            ? HeartRawDataConfidenceContent<RestingHeartRateSample>(
                period: period,
                entries: daySamples,
                source: (s) => s.source,
                time: (s) => s.time,
                accentColor: AppColors.heart,
              )
            : HeartAggregateDataConfidenceContent(
                period: period,
                trackedDates: [state.selectedDate],
                sampleCount: math.max(daySamples.length, 1),
                accentColor: AppColors.heart,
              ),
        statistics: HeartNumericStatisticsContent(
          unitFormatter: formatter,
          average: formatter.heartRate(restingBpm),
          low: formatter.heartRate(lowResting),
          high: formatter.heartRate(highResting),
          readings: math.max(daySamples.length, 1),
          comparison: result.dayRestingBpm != null &&
                  result.previousDayRestingBpm != null
              ? periodComparison(
                  result.dayRestingBpm!.toDouble(),
                  result.previousDayRestingBpm!.toDouble(),
                )
              : null,
          selectedRange: state.selectedRange,
          comparisonValueFormatter: (value) =>
              formatter.heartRate(value.round()),
          icon: Icons.favorite_border,
          accentColor: AppColors.heart,
          period: period,
          baselineCurrentValue: restingBpm.toDouble(),
          baselineValues: [
            for (final entry in result.baselineDailyRestingHR)
              BaselineValue(date: entry.date, value: entry.bpm.toDouble()),
          ],
        ),
        entries: daySamples.isNotEmpty
            ? HeartEntryListContent<RestingHeartRateSample>(
                entries: daySamples,
                value: (s) => formatter.heartRate(s.beatsPerMinute).text,
                source: (s) => s.source,
                time: (s) => s.time,
              )
            : HeartDailyEntryListContent<DailyRestingHR>(
                entries: [
                  DailyRestingHR(date: state.selectedDate, bpm: restingBpm),
                ],
                date: (e) => e.date,
                value: (e) => formatter.heartRate(e.bpm).text,
                accentColor: AppColors.heart,
              ),
      );
    }

    final sorted = [...result.dailyRestingHR]
      ..sort((a, b) => a.date.compareTo(b.date));
    if (isDay || sorted.isEmpty) return _emptyOrLoading();

    final bpm = sorted.map((e) => e.bpm.toDouble()).toList();
    final average = _avg(bpm).round();
    final selectedDay = daySelection.selectedDate;
    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: [
          MetricLineSeries(
            points: [
              for (final e in sorted)
                MetricLinePoint(date: e.date, value: e.bpm.toDouble()),
            ],
            color: metric.accentColor,
          ),
        ],
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryAvgValueRange(
            formatter.heartRate(average).text,
            formatter.heartRate(_min(bpm).round()).text,
            formatter.heartRate(_max(bpm).round()).text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.heartRate(value.round()).text,
      )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartDailyEntryListContent<DailyRestingHR>(
              entries: [
                for (final e in sorted)
                  if (e.date == selectedDay) e,
              ],
              date: (e) => e.date,
              value: (e) => formatter.heartRate(e.bpm).text,
              accentColor: AppColors.heart,
              titleDate: selectedDay,
            ),
      dataConfidence: HeartAggregateDataConfidenceContent(
        period: period,
        trackedDates: [for (final e in sorted) e.date],
        sampleCount: sorted.length,
        accentColor: AppColors.heart,
      ),
      contextInsight: restingHeartRateContextCardContent(average),
      statistics: restingHeartRateStatisticsContent(
        entries: sorted,
        previousEntries: result.previousDailyRestingHR,
        baselineEntries: result.baselineDailyRestingHR,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
      ),
      entries: HeartDailyEntryListContent<DailyRestingHR>(
        entries: sorted,
        date: (e) => e.date,
        value: (e) => formatter.heartRate(e.bpm).text,
        accentColor: AppColors.heart,
      ),
    );
  }

  // ── HRV (Kotlin `hrvContent`) ──────────────────────────────────────────────

  Widget _hrv(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final daySamples = [...result.dayHrvSamples]
      ..sort((a, b) => a.time.compareTo(b.time));
    final hasDayHrv =
        isDay && (daySamples.isNotEmpty || result.dayHrvMs != null);
    if (hasDayHrv) {
      final hrvMs =
          result.dayHrvMs ?? _avg(daySamples.map((s) => s.rmssdMs));
      final lowHrv = daySamples.isEmpty
          ? hrvMs
          : daySamples.map((s) => s.rmssdMs).reduce((a, b) => a < b ? a : b);
      final highHrv = daySamples.isEmpty
          ? hrvMs
          : daySamples.map((s) => s.rmssdMs).reduce((a, b) => a > b ? a : b);
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: daySamples.length > 1
            ? heartPadded(HeartTimelineCard(
                date: state.selectedDate,
                points: [for (final s in daySamples) (s.time, s.rmssdMs)],
                averageText: formatter.hrv(hrvMs).text,
                rangeText:
                    '${formatter.hrv(lowHrv).text}-${formatter.hrv(highHrv).text}',
                valueFormatter: (value) => formatter.hrv(value).text,
                minValue: math.max(0, lowHrv - 5),
                maxValue: highHrv + 5,
              ))
            : null,
        highlightCard: heartPadded(HeartDayValueCard(
          title: metric.title,
          value: '${formatter.hrv(hrvMs).text} RMSSD',
        )),
        dataConfidence: daySamples.isNotEmpty
            ? HeartRawDataConfidenceContent<HrvSample>(
                period: period,
                entries: daySamples,
                source: (s) => s.source,
                time: (s) => s.time,
                accentColor: AppColors.heart,
              )
            : HeartAggregateDataConfidenceContent(
                period: period,
                trackedDates: [state.selectedDate],
                sampleCount: math.max(daySamples.length, 1),
                accentColor: AppColors.heart,
              ),
        statistics: HeartNumericStatisticsContent(
          unitFormatter: formatter,
          average: formatter.hrv(hrvMs),
          low: formatter.hrv(lowHrv),
          high: formatter.hrv(highHrv),
          readings: math.max(daySamples.length, 1),
          comparison: result.dayHrvMs != null && result.previousDayHrvMs != null
              ? periodComparison(result.dayHrvMs!, result.previousDayHrvMs!)
              : null,
          selectedRange: state.selectedRange,
          comparisonValueFormatter: formatter.hrv,
          icon: Icons.favorite_border,
          accentColor: AppColors.heart,
          period: period,
          baselineCurrentValue: hrvMs,
          baselineValues: [
            for (final entry in result.baselineDailyHrv)
              BaselineValue(date: entry.date, value: entry.rmssdMs),
          ],
        ),
        entries: daySamples.isNotEmpty
            ? HeartEntryListContent<HrvSample>(
                entries: daySamples,
                value: (s) => formatter.hrv(s.rmssdMs).text,
                source: (s) => s.source,
                time: (s) => s.time,
              )
            : HeartDailyEntryListContent<DailyHrv>(
                entries: [DailyHrv(date: state.selectedDate, rmssdMs: hrvMs)],
                date: (e) => e.date,
                value: (e) => formatter.hrv(e.rmssdMs).text,
                accentColor: AppColors.heart,
              ),
      );
    }

    final sorted = [...result.dailyHrv]
      ..sort((a, b) => a.date.compareTo(b.date));
    if (isDay || sorted.isEmpty) return _emptyOrLoading();

    final ms = sorted.map((e) => e.rmssdMs).toList();
    final selectedDay = daySelection.selectedDate;
    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: [
          MetricLineSeries(
            points: [
              for (final e in sorted)
                MetricLinePoint(date: e.date, value: e.rmssdMs),
            ],
            color: metric.accentColor.withValues(alpha: 0.85),
          ),
        ],
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryAvgValueRange(
            formatter.hrv(_avg(ms)).text,
            formatter.hrv(_min(ms)).text,
            formatter.hrv(_max(ms)).text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.hrv(value).text,
      )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartDailyEntryListContent<DailyHrv>(
              entries: [
                for (final e in sorted)
                  if (e.date == selectedDay) e,
              ],
              date: (e) => e.date,
              value: (e) => formatter.hrv(e.rmssdMs).text,
              accentColor: AppColors.heart,
              titleDate: selectedDay,
            ),
      dataConfidence: HeartAggregateDataConfidenceContent(
        period: period,
        trackedDates: [for (final e in sorted) e.date],
        sampleCount: sorted.length,
        accentColor: AppColors.heart,
      ),
      statistics: hrvStatisticsContent(
        entries: sorted,
        previousEntries: result.previousDailyHrv,
        baselineEntries: result.baselineDailyHrv,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
      ),
      entries: HeartDailyEntryListContent<DailyHrv>(
        entries: sorted,
        date: (e) => e.date,
        value: (e) => formatter.hrv(e.rmssdMs).text,
        accentColor: AppColors.heart,
      ),
    );
  }

  // ── Blood pressure (Kotlin `bloodPressureContent`) ─────────────────────────

  Widget _bloodPressure(BuildContext context, HeartPeriodLoadResult result) {
    final l10n = AppLocalizations.of(context);
    final entries = result.bloodPressure;
    if (entries.isEmpty) return _emptyOrLoading();
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    final latest = sorted.last;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _bloodPressureSeries(sorted, l10n),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryReadings(formatter.count(entries.length)),
        ),
        valueFormatter: (value) => '${value.round()} mmHg',
      )),
      dataConfidence: HeartRawDataConfidenceContent<BloodPressureEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: BloodPressureContextCardContent(entry: latest),
      statistics: BloodPressureStatisticsContent(
        entries: entries,
        previousEntries: result.previousBloodPressure,
        baselineEntries: result.baselineBloodPressure,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
      ),
      entries: HeartEntryListContent<BloodPressureEntry>(
        entries: entries,
        value: (e) =>
            formatter.bloodPressure(e.systolicMmHg, e.diastolicMmHg).text,
        source: (e) => e.source,
        time: (e) => e.time,
        editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
        onEdit: (e) => onEditVitalsMeasurement(
            VitalsMeasurementType.bloodPressure, e.id),
        onDelete: (e) => onDeleteVitalsMeasurement(
            VitalsMeasurementType.bloodPressure, e.id),
        entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
      ),
    );
  }

  // ── SpO2 (Kotlin `spO2Content`) ────────────────────────────────────────────

  Widget _spo2(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final entries = result.spO2;
    if (entries.isEmpty) return _emptyOrLoading();
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    final selectedDay = daySelection.selectedDate;
    final latest = sorted.last;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _singleSeries(
          [for (final e in sorted) (e.time, e.percent)],
          metric.accentColor,
        ),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(
            formatter.percent(_avg(entries.map((e) => e.percent))).text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.percent(value).text,
      )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartEntryListContent<SpO2Entry>(
              entries: [
                for (final e in entries)
                  if (instantToLocalDate(e.time) == selectedDay) e,
              ],
              value: (e) => formatter.percent(e.percent).text,
              source: (e) => e.source,
              time: (e) => e.time,
              titleDate: selectedDay,
              editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
              onEdit: (e) =>
                  onEditVitalsMeasurement(VitalsMeasurementType.spo2, e.id),
              onDelete: (e) =>
                  onDeleteVitalsMeasurement(VitalsMeasurementType.spo2, e.id),
              entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
            ),
      dataConfidence: HeartRawDataConfidenceContent<SpO2Entry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: oxygenSaturationContextCardContent(
        latest,
        metric.accentColor,
      ),
      statistics: spO2StatisticsContent(
        entries: entries,
        previousEntries: result.previousSpO2,
        baselineEntries: result.baselineSpO2,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<SpO2Entry>(
        entries: entries,
        value: (e) => formatter.percent(e.percent).text,
        source: (e) => e.source,
        time: (e) => e.time,
        editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
        onEdit: (e) =>
            onEditVitalsMeasurement(VitalsMeasurementType.spo2, e.id),
        onDelete: (e) =>
            onDeleteVitalsMeasurement(VitalsMeasurementType.spo2, e.id),
        entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
      ),
    );
  }

  // ── VO2 max (Kotlin `vo2MaxContent`) ───────────────────────────────────────

  Widget _vo2Max(BuildContext context, HeartPeriodLoadResult result) {
    final l10n = AppLocalizations.of(context);
    final entries = result.vo2Max;
    if (entries.isEmpty) return _emptyOrLoading();
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    final latest = sorted.last;
    final latestValue = formatter.vo2Max(latest.vo2MaxMlPerKgPerMin);

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: entries.length > 1
          ? heartPadded(MetricLineChart(
              title: metric.title,
              series: _singleSeries(
                [for (final e in sorted) (e.time, e.vo2MaxMlPerKgPerMin)],
                metric.accentColor,
              ),
              selectedRange: state.selectedRange,
              period: period,
              accentColor: metric.accentColor,
              summaryText: _summary(
                l10n,
                l10n.summaryReadings(formatter.count(sorted.length)),
              ),
              valueFormatter: (value) => formatter.vo2Max(value).text,
            ))
          : null,
      highlightCard: heartPadded(MetricCard(
        title: metric.title,
        value: latestValue.value,
        unit: latestValue.unit,
        icon: Icons.speed_outlined,
        accentColor: metric.accentColor,
        source: latest.source,
      )),
      dataConfidence: HeartRawDataConfidenceContent<Vo2MaxEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      statistics: vo2MaxStatisticsContent(
        entries: entries,
        previousEntries: result.previousVo2Max,
        baselineEntries: result.baselineVo2Max,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<Vo2MaxEntry>(
        entries: entries,
        value: (e) => formatter.vo2Max(e.vo2MaxMlPerKgPerMin).text,
        source: (e) => e.source,
        time: (e) => e.time,
      ),
    );
  }

  // ── Respiratory rate (Kotlin `respiratoryRateContent`) ─────────────────────

  Widget _respiratoryRate(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final entries = result.respiratoryRate;
    if (entries.isEmpty) return _emptyOrLoading();
    final selectedDay = daySelection.selectedDate;
    final isDay = state.selectedRange == TimeRange.day;
    final daySummaries = respiratoryRateDaySummaries(entries)
      ..sort((a, b) => b.date.compareTo(a.date));
    final periodAverage =
        _avg(daySummaries.map((summary) => summary.average));

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _respiratoryRateSeries(entries, l10n),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(formatter.respiratoryRate(periodAverage).text),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.respiratoryRate(value).text,
      )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartEntryListContent<RespiratoryRateEntry>(
              entries: [
                for (final e in entries)
                  if (instantToLocalDate(e.time) == selectedDay) e,
              ],
              value: (e) => formatter.respiratoryRate(e.breathsPerMinute).text,
              source: (e) => e.source,
              time: (e) => e.time,
              titleDate: selectedDay,
              editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
              onEdit: (e) => onEditVitalsMeasurement(
                  VitalsMeasurementType.respiratoryRate, e.id),
              onDelete: (e) => onDeleteVitalsMeasurement(
                  VitalsMeasurementType.respiratoryRate, e.id),
              entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
            ),
      dataConfidence: HeartRawDataConfidenceContent<RespiratoryRateEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: respiratoryRateContextCardContent(
        _avg(entries.map((e) => e.breathsPerMinute)),
        metric.accentColor,
      ),
      statistics: respiratoryRateStatisticsContent(
        entries: entries,
        previousEntries: result.previousRespiratoryRate,
        baselineEntries: result.baselineRespiratoryRate,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (!isDay)
            PaginatedEntryList<RespiratoryRateDaySummary>(
              title: l10n.sectionRespiratoryRateDailyBreakdown,
              entries: daySummaries,
              rowBuilder: (context, summary) => RespiratoryRateDayRow(
                summary: summary,
                unitFormatter: formatter,
                accentColor: metric.accentColor,
              ),
            ),
          HeartEntryListContent<RespiratoryRateEntry>(
            entries: entries,
            value: (e) => formatter.respiratoryRate(e.breathsPerMinute).text,
            source: (e) => e.source,
            time: (e) => e.time,
            editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
            onEdit: (e) => onEditVitalsMeasurement(
                VitalsMeasurementType.respiratoryRate, e.id),
            onDelete: (e) => onDeleteVitalsMeasurement(
                VitalsMeasurementType.respiratoryRate, e.id),
            entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
          ),
        ],
      ),
    );
  }

  // ── Body temperature (Kotlin `bodyTemperatureContent`) ─────────────────────

  Widget _bodyTemperature(BuildContext context, HeartPeriodLoadResult result) {
    final l10n = AppLocalizations.of(context);
    final entries = result.bodyTemperature;
    if (entries.isEmpty) return _emptyOrLoading();
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    final latest = sorted.last;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _singleSeries(
          [for (final e in sorted) (e.time, e.temperatureCelsius)],
          metric.accentColor,
        ),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryReadings(formatter.count(sorted.length)),
        ),
        valueFormatter: (value) => formatter.temperature(value).text,
      )),
      dataConfidence: HeartRawDataConfidenceContent<BodyTempEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: bodyTemperatureContextCardContent(
        latest,
        metric.accentColor,
      ),
      statistics: bodyTemperatureStatisticsContent(
        entries: entries,
        previousEntries: result.previousBodyTemperature,
        baselineEntries: result.baselineBodyTemperature,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<BodyTempEntry>(
        entries: entries,
        value: (e) => formatter.temperature(e.temperatureCelsius).text,
        source: (e) => e.source,
        time: (e) => e.time,
        editable: (e) => e.isOpenVitalsEntry && e.id.isNotEmpty,
        onEdit: (e) => onEditVitalsMeasurement(
            VitalsMeasurementType.bodyTemperature, e.id),
        onDelete: (e) => onDeleteVitalsMeasurement(
            VitalsMeasurementType.bodyTemperature, e.id),
        entryKey: (e) => e.id.isEmpty ? '${e.time}' : e.id,
      ),
    );
  }

  // ── Blood glucose (Kotlin `bloodGlucoseContent`) ───────────────────────────

  Widget _bloodGlucose(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final entries = result.bloodGlucose;
    if (entries.isEmpty) return _emptyOrLoading();
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    final selectedDay = daySelection.selectedDate;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: heartPadded(MetricLineChart(
        title: metric.title,
        series: _singleSeries(
          [for (final e in sorted) (e.time, e.millimolesPerLiter)],
          metric.accentColor,
        ),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(
            formatter
                .bloodGlucose(_avg(sorted.map((e) => e.millimolesPerLiter)))
                .text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.bloodGlucose(value).text,
      )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartEntryListContent<BloodGlucoseEntry>(
              entries: [
                for (final e in entries)
                  if (instantToLocalDate(e.time) == selectedDay) e,
              ],
              value: (e) => formatter.bloodGlucose(e.millimolesPerLiter).text,
              source: (e) => e.source,
              time: (e) => e.time,
              titleDate: selectedDay,
            ),
      dataConfidence: HeartRawDataConfidenceContent<BloodGlucoseEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      statistics: bloodGlucoseStatisticsContent(
        entries: entries,
        previousEntries: result.previousBloodGlucose,
        baselineEntries: result.baselineBloodGlucose,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<BloodGlucoseEntry>(
        entries: entries,
        value: (e) => formatter.bloodGlucose(e.millimolesPerLiter).text,
        source: (e) => e.source,
        time: (e) => e.time,
      ),
    );
  }

  // ── Skin temperature (Kotlin `skinTemperatureContent`) ─────────────────────

  Widget _skinTemperature(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final entries = result.skinTemperature;
    if (entries.isEmpty) return _emptyOrLoading();
    final chartEntries = [
      for (final e in entries)
        if (e.averageDeltaCelsius != null) e,
    ]..sort((a, b) => a.time.compareTo(b.time));
    final selectedDay = daySelection.selectedDate;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: chartEntries.isEmpty
          ? null
          : heartPadded(MetricLineChart(
              title: metric.title,
              series: _singleSeries(
                [
                  for (final e in chartEntries)
                    (e.time, e.averageDeltaCelsius!),
                ],
                metric.accentColor,
              ),
              selectedRange: state.selectedRange,
              period: period,
              accentColor: metric.accentColor,
              summaryText: _summary(
                l10n,
                l10n.summaryValueAvg(
                  formatter
                      .temperatureDelta(_avg(chartEntries
                          .map((e) => e.averageDeltaCelsius!)))
                      .text,
                ),
              ),
              selectedDate: selectedDay,
              onDateSelected: daySelection.onDateSelected,
              valueFormatter: (value) =>
                  formatter.temperatureDelta(value).text,
            )),
      selectedDayEntries: selectedDay == null
          ? null
          : HeartEntryListContent<SkinTemperatureEntry>(
              entries: [
                for (final e in entries)
                  if (instantToLocalDate(e.time) == selectedDay) e,
              ],
              value: (e) => skinTemperatureValueText(e, formatter),
              source: (e) => e.source,
              time: (e) => e.time,
              titleDate: selectedDay,
            ),
      dataConfidence: HeartRawDataConfidenceContent<SkinTemperatureEntry>(
        period: period,
        entries: entries,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      statistics: skinTemperatureStatisticsContent(
        entries: entries,
        previousEntries: result.previousSkinTemperature,
        baselineEntries: result.baselineSkinTemperature,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<SkinTemperatureEntry>(
        entries: entries,
        value: (e) => skinTemperatureValueText(e, formatter),
        source: (e) => e.source,
        time: (e) => e.time,
      ),
    );
  }

  // ── Building blocks ─────────────────────────────────────────────────────────

  String _summary(AppLocalizations l10n, String extra) => '${periodTitle(
        l10n,
        state.selectedRange,
        period,
        weekPeriodMode: weekPeriodMode,
      )} · $extra';

  Widget _placeholder() => heartPadded(MetricCardPlaceholder(
        title: metric.title,
        icon: metric.icon,
        accentColor: metric.accentColor,
        message: metric.emptyMessage,
      ));

  Widget _emptyOrLoading() =>
      state.isLoading ? const _LoadingBlock() : _placeholder();

  /// Kotlin `heartRateSeries` (`HeartVitalsChartData.kt`): the avg line plus
  /// min/max lines when any day actually has a range.
  List<MetricLineSeries> _heartRateSeries(
    List<HeartRateSummary> summaries,
    AppLocalizations l10n,
  ) {
    final hasRange = summaries.any((s) => s.minBpm != s.maxBpm);
    return [
      MetricLineSeries(
        points: [
          for (final s in summaries)
            MetricLinePoint(date: s.date, value: s.avgBpm.toDouble()),
        ],
        color: AppColors.heart,
        label: l10n.summaryAverage,
      ),
      if (hasRange) ...[
        MetricLineSeries(
          points: [
            for (final s in summaries)
              MetricLinePoint(date: s.date, value: s.minBpm.toDouble()),
          ],
          color: AppColors.heart.withValues(alpha: 0.55),
          label: l10n.statLowest,
        ),
        MetricLineSeries(
          points: [
            for (final s in summaries)
              MetricLinePoint(date: s.date, value: s.maxBpm.toDouble()),
          ],
          color: AppColors.heart.withValues(alpha: 0.9),
          label: l10n.statHighest,
        ),
      ],
    ];
  }

  /// Kotlin `bloodPressureSeries`: systolic (VitalsColor) + diastolic
  /// (HeartColor); raw within a day, daily averages otherwise.
  List<MetricLineSeries> _bloodPressureSeries(
    List<BloodPressureEntry> sorted,
    AppLocalizations l10n,
  ) {
    final isDay = state.selectedRange == TimeRange.day;
    final systolic = [
      for (final e in sorted)
        MetricLinePoint(
          date: instantToLocalDate(e.time),
          value: e.systolicMmHg.toDouble(),
          time: e.time,
        ),
    ];
    final diastolic = [
      for (final e in sorted)
        MetricLinePoint(
          date: instantToLocalDate(e.time),
          value: e.diastolicMmHg.toDouble(),
          time: e.time,
        ),
    ];
    return [
      MetricLineSeries(
        points: isDay ? systolic : dailyAverageLinePoints(systolic),
        color: AppColors.vitals,
        label: l10n.vitalsEntrySystolicLabel,
      ),
      MetricLineSeries(
        points: isDay ? diastolic : dailyAverageLinePoints(diastolic),
        color: AppColors.heart,
        label: l10n.vitalsEntryDiastolicLabel,
      ),
    ];
  }

  /// Kotlin `respiratoryRateSeries`: raw within a day; daily average plus
  /// min/max range series otherwise.
  List<MetricLineSeries> _respiratoryRateSeries(
    List<RespiratoryRateEntry> entries,
    AppLocalizations l10n,
  ) {
    final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
    if (state.selectedRange == TimeRange.day) {
      return _singleSeries(
        [for (final e in sorted) (e.time, e.breathsPerMinute)],
        metric.accentColor,
        label: metric.title,
      );
    }
    final byDate = <LocalDate, List<double>>{};
    for (final e in sorted) {
      byDate
          .putIfAbsent(instantToLocalDate(e.time), () => <double>[])
          .add(e.breathsPerMinute);
    }
    final dates = byDate.keys.toList()..sort((a, b) => a.compareTo(b));
    final average = <MetricLinePoint>[];
    final min = <MetricLinePoint>[];
    final max = <MetricLinePoint>[];
    for (final date in dates) {
      final values = byDate[date]!;
      average.add(MetricLinePoint(date: date, value: _avg(values)));
      min.add(MetricLinePoint(date: date, value: _min(values)));
      max.add(MetricLinePoint(date: date, value: _max(values)));
    }
    final hasRange = [
      for (var i = 0; i < min.length; i++) min[i].value != max[i].value
    ].any((different) => different);
    return [
      MetricLineSeries(
        points: average,
        color: metric.accentColor,
        label: l10n.summaryAverage,
      ),
      if (hasRange) ...[
        MetricLineSeries(
          points: min,
          color: metric.accentColor.withValues(alpha: 0.55),
          label: l10n.statLowest,
        ),
        MetricLineSeries(
          points: max,
          color: AppColors.vitals.withValues(alpha: 0.75),
          label: l10n.statHighest,
        ),
      ],
    ];
  }

  /// Raw points within a day; daily averages otherwise.
  List<MetricLineSeries> _singleSeries(
    List<(DateTime, double)> raw,
    Color color, {
    String? label,
  }) {
    final base = [
      for (final (time, value) in raw)
        MetricLinePoint(
          date: instantToLocalDate(time),
          value: value,
          time: time,
        ),
    ];
    final points = state.selectedRange == TimeRange.day
        ? base
        : dailyAverageLinePoints(base);
    return [MetricLineSeries(points: points, color: color, label: label)];
  }
}

double _avg(Iterable<double> values) {
  final list = values.toList();
  return list.isEmpty ? 0 : list.reduce((a, b) => a + b) / list.length;
}

double _min(Iterable<double> values) => values.reduce((a, b) => a < b ? a : b);

double _max(Iterable<double> values) => values.reduce((a, b) => a > b ? a : b);

class _LoadingBlock extends StatelessWidget {
  const _LoadingBlock();

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      );
}
