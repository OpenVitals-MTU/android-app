import 'package:flutter/material.dart';

import '../../../core/period/period_titles.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/line_chart.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/paginated_entry_list.dart';
import '../../../ui/theme/app_colors.dart';
import 'heart_metric.dart';
import 'heart_metric_cards.dart';
import '../application/heart_display.dart';
import '../application/heart_metric_view_model.dart';
import 'heart_metric_ordered_sections.dart';
import 'heart_metric_shared_sections.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/section_padding.dart';
import 'heart_chart_series.dart';

/// Called when a manual OpenVitals measurement entry should be edited/deleted.
typedef VitalsMeasurementCallback = void Function(
  VitalsMeasurementType type,
  String entryId,
);

/// The per-metric detail content of the heart + vitals screens, ported from the
/// Kotlin `HeartMetricContent.kt`, `HeartVitalDetailContent.kt` and
/// `VitalsBloodPressureContent.kt`. Every metric renders through the
/// user-reorderable [heartChartMetricSections] skeleton.
///
/// It derives nothing: the sorted series, the extremes and the statistics all
/// arrive precomputed on [HeartMetricState.display] (Kotlin `HeartDisplayState`).
/// What is left here is layout, theming, l10n and formatting — presentation.
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
    final display = state.display;
    if (display == null) {
      if (state.isLoading) return const SectionLoading();
      return _placeholder();
    }
    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _content(context, display, daySelection),
    );
  }

  Widget _content(
    BuildContext context,
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    switch (metric) {
      case HeartMetric.averageHeartRate:
        return _averageHeartRate(context, display, daySelection);
      case HeartMetric.restingHeartRate:
        return _restingHeartRate(context, display, daySelection);
      case HeartMetric.hrv:
        return _hrv(context, display, daySelection);
      case HeartMetric.bloodPressure:
        return _bloodPressure(context, display);
      case HeartMetric.spo2:
        return _spo2(context, display, daySelection);
      case HeartMetric.vo2Max:
        return _vo2Max(context, display);
      case HeartMetric.respiratoryRate:
        return _respiratoryRate(context, display, daySelection);
      case HeartMetric.bodyTemperature:
        return _bodyTemperature(context, display);
      case HeartMetric.bloodGlucose:
        return _bloodGlucose(context, display, daySelection);
      case HeartMetric.skinTemperature:
        return _skinTemperature(context, display, daySelection);
    }
  }

  // ── Average heart rate (Kotlin `averageHeartRateContent`) ─────────────────

  Widget _averageHeartRate(
    BuildContext context,
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final day = display.heartRateDay;
    if (isDay && day != null) {
      final samples = day.samples;
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: samples.length > 1
            ? sectionPadded(HeartTimelineCard(
                date: state.selectedDate,
                metricName: l10n.metricAverageHeartRate,
                points: [
                  for (final s in samples) (s.time, s.beatsPerMinute.toDouble())
                ],
                averageText: formatter.heartRate(day.averageBpm).text,
                rangeText:
                    '${formatter.heartRate(day.minBpm).text}-${formatter.heartRate(day.maxBpm).text}',
                valueFormatter: (value) =>
                    formatter.heartRate(value.round()).text,
                minValue: day.chartMinValue,
                maxValue: day.chartMaxValue,
              ))
            : null,
        highlightCard: _thresholdChecks(display),
        dataConfidence: HeartRawDataConfidenceContent<HeartRateSample>(
          period: period,
          entries: samples,
          source: (s) => s.source,
          time: (s) => s.time,
          accentColor: AppColors.heart,
        ),
        statistics: heartRateStatisticsContent(
          stats: day.stats,
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
      if (state.isLoading) return const SectionLoading();
      return sectionPadded(const HeartRateEmptyDayCard());
    }

    final periodDisplay = display.heartRatePeriod;
    if (periodDisplay == null) return _emptyOrLoading();

    final summaries = periodDisplay.summaries;
    final selectedDay = daySelection.selectedDate;

    final periodSections = heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: heartRateSeries(summaries, l10n),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryAvgValueRange(
            formatter.heartRate(periodDisplay.averageBpm).text,
            formatter.heartRate(periodDisplay.lowestBpm).text,
            formatter.heartRate(periodDisplay.highestBpm).text,
          ),
        ),
        selectedDate: selectedDay,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.heartRate(value.round()).text,
      )),
      highlightCard: _thresholdChecks(display),
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
      statistics: heartRateStatisticsContent(
        stats: periodDisplay.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        countInLoggedDays: true,
      ),
      entries: PaginatedEntryList<HeartRateSummary>(
        title: l10n.sectionDailyBreakdown,
        entries: periodDisplay.summariesNewestFirst,
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

  Widget _thresholdChecks(HeartDisplay display) =>
      sectionPadded(HeartRateThresholdChecksContent(
        highCheck: display.highHeartRateCheck,
        lowCheck: display.lowHeartRateCheck,
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
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final day = display.restingHeartRateDay;
    if (isDay && day != null) {
      final daySamples = day.samples;
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: daySamples.length > 1
            ? sectionPadded(HeartTimelineCard(
                date: state.selectedDate,
                metricName: l10n.metricRestingHeartRate,
                points: [
                  for (final s in daySamples)
                    (s.time, s.beatsPerMinute.toDouble())
                ],
                averageText: formatter.heartRate(day.restingBpm).text,
                rangeText:
                    '${formatter.heartRate(day.lowBpm).text}-${formatter.heartRate(day.highBpm).text}',
                valueFormatter: (value) =>
                    formatter.heartRate(value.round()).text,
                minValue: day.chartMinValue,
                maxValue: day.chartMaxValue,
              ))
            : null,
        highlightCard: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            sectionPadded(HeartDayValueCard(
              title: metric.title,
              value: formatter.heartRate(day.restingBpm).text,
            )),
            restingHeartRateContextCardContent(day.restingBpm),
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
                sampleCount: day.stats.readings,
                accentColor: AppColors.heart,
              ),
        statistics: HeartNumericStatisticsContent(
          unitFormatter: formatter,
          average: formatter.heartRate(day.restingBpm),
          low: formatter.heartRate(day.lowBpm),
          high: formatter.heartRate(day.highBpm),
          readings: day.stats.readings,
          comparison: day.stats.comparison,
          selectedRange: state.selectedRange,
          comparisonValueFormatter: (value) =>
              formatter.heartRate(value.round()),
          icon: Icons.favorite_border,
          accentColor: AppColors.heart,
          period: period,
          baselineCurrentValue: day.stats.baselineCurrentValue,
          baselineValues: day.stats.baselineValues,
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
                  DailyRestingHR(date: state.selectedDate, bpm: day.restingBpm),
                ],
                date: (e) => e.date,
                value: (e) => formatter.heartRate(e.bpm).text,
                accentColor: AppColors.heart,
              ),
      );
    }

    final periodDisplay = display.restingHeartRatePeriod;
    if (isDay || periodDisplay == null) return _emptyOrLoading();

    final sorted = periodDisplay.entries;
    final selectedDay = daySelection.selectedDate;
    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
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
            formatter.heartRate(periodDisplay.averageBpm).text,
            formatter.heartRate(periodDisplay.lowBpm).text,
            formatter.heartRate(periodDisplay.highBpm).text,
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
      contextInsight:
          restingHeartRateContextCardContent(periodDisplay.averageBpm),
      statistics: restingHeartRateStatisticsContent(
        stats: periodDisplay.stats,
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
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final day = display.hrvDay;
    if (isDay && day != null) {
      final daySamples = day.samples;
      return heartChartMetricSections(
        selectedRange: state.selectedRange,
        period: period,
        selectedDate: null,
        intradayChart: daySamples.length > 1
            ? sectionPadded(HeartTimelineCard(
                date: state.selectedDate,
                metricName: l10n.metricHrv,
                points: [for (final s in daySamples) (s.time, s.rmssdMs)],
                averageText: formatter.hrv(day.hrvMs).text,
                rangeText:
                    '${formatter.hrv(day.lowMs).text}-${formatter.hrv(day.highMs).text}',
                valueFormatter: (value) => formatter.hrv(value).text,
                minValue: day.chartMinValue,
                maxValue: day.chartMaxValue,
              ))
            : null,
        highlightCard: sectionPadded(HeartDayValueCard(
          title: metric.title,
          value: '${formatter.hrv(day.hrvMs).text} RMSSD',
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
                sampleCount: day.stats.readings,
                accentColor: AppColors.heart,
              ),
        statistics: HeartNumericStatisticsContent(
          unitFormatter: formatter,
          average: formatter.hrv(day.hrvMs),
          low: formatter.hrv(day.lowMs),
          high: formatter.hrv(day.highMs),
          readings: day.stats.readings,
          comparison: day.stats.comparison,
          selectedRange: state.selectedRange,
          comparisonValueFormatter: formatter.hrv,
          icon: Icons.favorite_border,
          accentColor: AppColors.heart,
          period: period,
          baselineCurrentValue: day.stats.baselineCurrentValue,
          baselineValues: day.stats.baselineValues,
        ),
        entries: daySamples.isNotEmpty
            ? HeartEntryListContent<HrvSample>(
                entries: daySamples,
                value: (s) => formatter.hrv(s.rmssdMs).text,
                source: (s) => s.source,
                time: (s) => s.time,
              )
            : HeartDailyEntryListContent<DailyHrv>(
                entries: [
                  DailyHrv(date: state.selectedDate, rmssdMs: day.hrvMs),
                ],
                date: (e) => e.date,
                value: (e) => formatter.hrv(e.rmssdMs).text,
                accentColor: AppColors.heart,
              ),
      );
    }

    final periodDisplay = display.hrvPeriod;
    if (isDay || periodDisplay == null) return _emptyOrLoading();

    final sorted = periodDisplay.entries;
    final selectedDay = daySelection.selectedDate;
    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
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
            formatter.hrv(periodDisplay.averageMs).text,
            formatter.hrv(periodDisplay.lowMs).text,
            formatter.hrv(periodDisplay.highMs).text,
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
        stats: periodDisplay.stats,
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

  Widget _bloodPressure(BuildContext context, HeartDisplay display) {
    final l10n = AppLocalizations.of(context);
    final bloodPressure = display.bloodPressure;
    if (bloodPressure == null) return _emptyOrLoading();
    final sorted = bloodPressure.entries;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: bloodPressureSeries(sorted, l10n, state.selectedRange),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryReadings(formatter.count(sorted.length)),
        ),
        valueFormatter: (value) => '${value.round()} mmHg',
      )),
      dataConfidence: HeartRawDataConfidenceContent<BloodPressureEntry>(
        period: period,
        entries: sorted,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight:
          BloodPressureContextCardContent(entry: bloodPressure.latest),
      statistics: BloodPressureStatisticsContent(
        stats: bloodPressure.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
      ),
      entries: HeartEntryListContent<BloodPressureEntry>(
        entries: sorted,
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
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final spO2 = display.spO2;
    if (spO2 == null) return _emptyOrLoading();
    final sorted = spO2.entries;
    final selectedDay = daySelection.selectedDate;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: singleSeries(
          [for (final e in sorted) (e.time, e.percent)],
          metric.accentColor,
          state.selectedRange,
        ),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(
            formatter.percent(spO2.averagePercent).text,
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
                for (final e in sorted)
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
        entries: sorted,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: oxygenSaturationContextCardContent(
        spO2.latest,
        metric.accentColor,
      ),
      statistics: spO2StatisticsContent(
        stats: spO2.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<SpO2Entry>(
        entries: sorted,
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

  Widget _vo2Max(BuildContext context, HeartDisplay display) {
    final l10n = AppLocalizations.of(context);
    final vo2Max = display.vo2Max;
    if (vo2Max == null) return _emptyOrLoading();
    final sorted = vo2Max.entries;
    final latest = vo2Max.latest;
    final latestValue = formatter.vo2Max(latest.vo2MaxMlPerKgPerMin);

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: sorted.length > 1
          ? sectionPadded(MetricLineChart(
              title: metric.title,
              series: singleSeries(
                [for (final e in sorted) (e.time, e.vo2MaxMlPerKgPerMin)],
                metric.accentColor,
                state.selectedRange,
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
      highlightCard: sectionPadded(MetricCard(
        title: metric.title,
        value: latestValue.value,
        unit: latestValue.unit,
        icon: Icons.speed_outlined,
        accentColor: metric.accentColor,
        source: latest.source,
      )),
      dataConfidence: HeartRawDataConfidenceContent<Vo2MaxEntry>(
        period: period,
        entries: sorted,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      statistics: vo2MaxStatisticsContent(
        stats: vo2Max.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<Vo2MaxEntry>(
        entries: sorted,
        value: (e) => formatter.vo2Max(e.vo2MaxMlPerKgPerMin).text,
        source: (e) => e.source,
        time: (e) => e.time,
      ),
    );
  }

  // ── Respiratory rate (Kotlin `respiratoryRateContent`) ─────────────────────

  Widget _respiratoryRate(
    BuildContext context,
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final respiratoryRate = display.respiratoryRate;
    if (respiratoryRate == null) return _emptyOrLoading();
    final entries = respiratoryRate.entries;
    final selectedDay = daySelection.selectedDate;
    final isDay = state.selectedRange == TimeRange.day;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: respiratoryRateSeries(entries, l10n, state.selectedRange,
            color: metric.accentColor, dayLabel: metric.title),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(
            formatter.respiratoryRate(respiratoryRate.periodAverage).text,
          ),
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
        respiratoryRate.periodAverage,
        metric.accentColor,
      ),
      statistics: respiratoryRateStatisticsContent(
        stats: respiratoryRate.stats,
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
              entries: respiratoryRate.daySummariesNewestFirst,
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

  Widget _bodyTemperature(BuildContext context, HeartDisplay display) {
    final l10n = AppLocalizations.of(context);
    final bodyTemperature = display.bodyTemperature;
    if (bodyTemperature == null) return _emptyOrLoading();
    final sorted = bodyTemperature.entries;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: null,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: singleSeries(
          [for (final e in sorted) (e.time, e.temperatureCelsius)],
          metric.accentColor,
          state.selectedRange,
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
        entries: sorted,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      contextInsight: bodyTemperatureContextCardContent(
        bodyTemperature.latest,
        metric.accentColor,
      ),
      statistics: bodyTemperatureStatisticsContent(
        stats: bodyTemperature.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<BodyTempEntry>(
        entries: sorted,
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
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final bloodGlucose = display.bloodGlucose;
    if (bloodGlucose == null) return _emptyOrLoading();
    final sorted = bloodGlucose.entries;
    final selectedDay = daySelection.selectedDate;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: sectionPadded(MetricLineChart(
        title: metric.title,
        series: singleSeries(
          [for (final e in sorted) (e.time, e.millimolesPerLiter)],
          metric.accentColor,
          state.selectedRange,
        ),
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryText: _summary(
          l10n,
          l10n.summaryValueAvg(
            formatter.bloodGlucose(bloodGlucose.averageMmolPerLiter).text,
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
                for (final e in sorted)
                  if (instantToLocalDate(e.time) == selectedDay) e,
              ],
              value: (e) => formatter.bloodGlucose(e.millimolesPerLiter).text,
              source: (e) => e.source,
              time: (e) => e.time,
              titleDate: selectedDay,
            ),
      dataConfidence: HeartRawDataConfidenceContent<BloodGlucoseEntry>(
        period: period,
        entries: sorted,
        source: (e) => e.source,
        time: (e) => e.time,
        accentColor: metric.accentColor,
      ),
      statistics: bloodGlucoseStatisticsContent(
        stats: bloodGlucose.stats,
        period: period,
        selectedRange: state.selectedRange,
        unitFormatter: formatter,
        accentColor: metric.accentColor,
      ),
      entries: HeartEntryListContent<BloodGlucoseEntry>(
        entries: sorted,
        value: (e) => formatter.bloodGlucose(e.millimolesPerLiter).text,
        source: (e) => e.source,
        time: (e) => e.time,
      ),
    );
  }

  // ── Skin temperature (Kotlin `skinTemperatureContent`) ─────────────────────

  Widget _skinTemperature(
    BuildContext context,
    HeartDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final skinTemperature = display.skinTemperature;
    if (skinTemperature == null) return _emptyOrLoading();
    final entries = skinTemperature.entries;
    final chartEntries = skinTemperature.chartEntries;
    final averageDelta = skinTemperature.averageDeltaCelsius;
    final selectedDay = daySelection.selectedDate;

    return heartChartMetricSections(
      selectedRange: state.selectedRange,
      period: period,
      selectedDate: selectedDay,
      periodChart: averageDelta == null
          ? null
          : sectionPadded(MetricLineChart(
              title: metric.title,
              series: singleSeries(
                [
                  for (final e in chartEntries)
                    (e.time, e.averageDeltaCelsius!),
                ],
                metric.accentColor,
                state.selectedRange,
              ),
              selectedRange: state.selectedRange,
              period: period,
              accentColor: metric.accentColor,
              summaryText: _summary(
                l10n,
                l10n.summaryValueAvg(
                  formatter.temperatureDelta(averageDelta).text,
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
        stats: skinTemperature.stats,
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

  Widget _placeholder() => sectionPadded(MetricCardPlaceholder(
        title: metric.title,
        icon: metric.icon,
        accentColor: metric.accentColor,
        message: metric.emptyMessage,
      ));

  Widget _emptyOrLoading() =>
      state.isLoading ? const SectionLoading() : _placeholder();
}
