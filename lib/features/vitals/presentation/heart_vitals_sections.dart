import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/period/period_titles.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/charts/line_chart.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/section_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../../heart/presentation/heart_chart_series.dart';
import '../../heart/presentation/heart_metric.dart';
import '../../heart/presentation/heart_metric_cards.dart';
import '../application/heart_vitals_overview_display.dart';
import '../application/heart_vitals_overview_view_model.dart';

/// The three reorderable groups of the heart & vitals overview, ported from the
/// Kotlin `VitalsHeartOverviewSectionContent`, `VitalsCardiovascular…` and
/// `VitalsRespiratoryOverviewSectionContent`.
///
/// Each renders a 2-per-row grid of [MetricCard]s (tapping one opens its
/// `/metric/<routeName>` detail) followed by that group's trend charts. Every
/// number they print comes precomputed off [HeartVitalsOverviewState.display] —
/// these widgets format, lay out and theme, and derive nothing.

// Vitals accent colours, ported from the Kotlin `HeartVitalsPresentation.kt`.
const Color oxygenColor = Color(0xFF00897B);
const Color respiratoryColor = Color(0xFF5E97F6);
const Color temperatureColor = Color(0xFFFF7043);
const Color vo2Color = Color(0xFF7E57C2);
const Color glucoseColor = Color(0xFF8E5D42);

/// Kotlin `VitalsHeartOverviewSectionContent`.
class HeartVitalsHeartSection extends StatelessWidget {
  const HeartVitalsHeartSection({
    super.key,
    required this.state,
    required this.period,
    required this.formatter,
    required this.display,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartVitalsOverviewDisplay display;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final heartRate = display.heartRate;
    final resting = display.restingHeartRate;
    final hrv = display.hrv;

    final dayTimeline = heartRate?.dayTimeline;
    final periodChart = heartRate?.periodChart;
    final restingChart = resting?.periodChart;
    final hrvChart = hrv?.periodChart;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionHeart),
        MetricCardGrid(cards: [
          OverviewCard(
            metric: HeartMetric.averageHeartRate,
            title: l10n.metricAverageHeartRate,
            value: heartRate == null
                ? null
                : formatter.heartRate(heartRate.averageBpm),
            icon: Icons.favorite,
            color: AppColors.heart,
            source: heartRate?.source,
          ),
          OverviewCard(
            metric: HeartMetric.restingHeartRate,
            title: l10n.metricRestingHeartRate,
            value: resting == null ? null : formatter.heartRate(resting.bpm),
            icon: Icons.favorite_border,
            color: AppColors.heart,
          ),
          OverviewCard(
            metric: HeartMetric.hrv,
            title: l10n.metricHrv,
            value: hrv == null ? null : formatter.hrv(hrv.ms),
            icon: Icons.speed,
            color: AppColors.heart.withValues(alpha: 0.85),
          ),
        ]),
        // Kotlin `HeartOverviewChartsContent`.
        if (dayTimeline != null)
          sectionPadded(_dayTimeline(context, dayTimeline))
        else if (periodChart != null)
          sectionPadded(_heartRateChart(context, periodChart)),
        if (restingChart != null)
          sectionPadded(_restingChart(context, restingChart)),
        if (hrvChart != null) sectionPadded(_hrvChart(context, hrvChart)),
      ],
    );
  }

  Widget _dayTimeline(BuildContext context, HeartRateDayTimeline timeline) {
    return HeartTimelineCard(
      date: state.selectedDate,
      metricName: AppLocalizations.of(context).metricAverageHeartRate,
      points: [
        for (final s in timeline.samples) (s.time, s.beatsPerMinute.toDouble()),
      ],
      averageText: formatter.heartRate(timeline.averageBpm).text,
      rangeText: '${formatter.heartRate(timeline.minBpm).text}'
          '-${formatter.heartRate(timeline.maxBpm).text}',
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
      minValue: timeline.chartMinValue,
      maxValue: timeline.chartMaxValue,
    );
  }

  Widget _heartRateChart(BuildContext context, HeartRatePeriodChart chart) {
    final l10n = AppLocalizations.of(context);
    return MetricLineChart(
      title: l10n.metricAverageHeartRate,
      series: heartRateSeries(chart.summaries, l10n),
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart,
      summaryText: summaryLine(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.heartRate(chart.averageBpm).text,
          formatter.heartRate(chart.lowestBpm).text,
          formatter.heartRate(chart.highestBpm).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
    );
  }

  Widget _restingChart(
      BuildContext context, RestingHeartRatePeriodChart chart) {
    final l10n = AppLocalizations.of(context);
    return MetricLineChart(
      title: l10n.metricRestingHeartRate,
      series: [
        MetricLineSeries(
          points: [
            for (final e in chart.entries)
              MetricLinePoint(date: e.date, value: e.bpm.toDouble()),
          ],
          color: AppColors.heart,
        ),
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart,
      summaryText: summaryLine(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.heartRate(chart.averageBpm).text,
          formatter.heartRate(chart.lowBpm).text,
          formatter.heartRate(chart.highBpm).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
    );
  }

  Widget _hrvChart(BuildContext context, HrvPeriodChart chart) {
    final l10n = AppLocalizations.of(context);
    return MetricLineChart(
      title: l10n.metricHrv,
      series: [
        MetricLineSeries(
          points: [
            for (final e in chart.entries)
              MetricLinePoint(date: e.date, value: e.rmssdMs),
          ],
          color: AppColors.heart.withValues(alpha: 0.85),
        ),
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart.withValues(alpha: 0.85),
      summaryText: summaryLine(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.hrv(chart.averageMs).text,
          formatter.hrv(chart.lowMs).text,
          formatter.hrv(chart.highMs).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.hrv(value).text,
    );
  }
}

/// Kotlin `VitalsCardiovascularOverviewSectionContent`.
class HeartVitalsCardiovascularSection extends StatelessWidget {
  const HeartVitalsCardiovascularSection({
    super.key,
    required this.state,
    required this.period,
    required this.formatter,
    required this.display,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartVitalsOverviewDisplay display;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final range = state.selectedRange;

    final bloodPressure = display.bloodPressure;
    final spO2 = display.spO2;
    final vo2Max = display.vo2Max;
    final bloodGlucose = display.bloodGlucose;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionCardiovascular),
        MetricCardGrid(cards: [
          OverviewCard(
            metric: HeartMetric.bloodPressure,
            title: l10n.metricBloodPressure,
            value: bloodPressure == null
                ? null
                : formatter.bloodPressure(
                    bloodPressure.latest.systolicMmHg,
                    bloodPressure.latest.diastolicMmHg),
            icon: Icons.favorite,
            color: AppColors.vitals,
            source: bloodPressure?.latest.source,
          ),
          OverviewCard(
            metric: HeartMetric.spo2,
            title: l10n.metricSpo2,
            value:
                spO2 == null ? null : formatter.percent(spO2.latest.percent),
            icon: Icons.favorite,
            color: oxygenColor,
            source: spO2?.latest.source,
          ),
          OverviewCard(
            metric: HeartMetric.vo2Max,
            title: l10n.metricVo2Max,
            value: vo2Max == null
                ? null
                : formatter.vo2Max(vo2Max.latest.vo2MaxMlPerKgPerMin),
            icon: Icons.speed,
            color: vo2Color,
            source: vo2Max?.latest.source,
          ),
          OverviewCard(
            metric: HeartMetric.bloodGlucose,
            title: l10n.metricBloodGlucose,
            value: bloodGlucose == null
                ? null
                : formatter
                    .bloodGlucose(bloodGlucose.latest.millimolesPerLiter),
            icon: Icons.favorite,
            color: glucoseColor,
            source: bloodGlucose?.latest.source,
          ),
        ]),
        // Kotlin `CardiovascularOverviewChartsContent`.
        if (bloodPressure != null && bloodPressure.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricBloodPressure,
            series: bloodPressureSeries(bloodPressure.entries, l10n, range),
            selectedRange: range,
            period: period,
            accentColor: AppColors.vitals,
            summaryText: summaryLine(l10n, state, period,
                l10n.summaryReadings(formatter.count(bloodPressure.readings))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => '${value.round()} mmHg',
          )),
        if (spO2 != null && spO2.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricSpo2,
            series: singleSeries(
              [for (final e in spO2.entries) (e.time, e.percent)],
              oxygenColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: oxygenColor,
            summaryText: summaryLine(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                  formatter.percent(spO2.averagePercent).text),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.percent(value).text,
          )),
        if (vo2Max != null && vo2Max.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricVo2Max,
            series: singleSeries(
              [for (final e in vo2Max.entries) (e.time, e.vo2MaxMlPerKgPerMin)],
              vo2Color,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: vo2Color,
            summaryText: summaryLine(l10n, state, period,
                l10n.summaryReadings(formatter.count(vo2Max.readings))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.vo2Max(value).text,
          )),
        if (bloodGlucose != null && bloodGlucose.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricBloodGlucose,
            series: singleSeries(
              [
                for (final e in bloodGlucose.entries)
                  (e.time, e.millimolesPerLiter),
              ],
              glucoseColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: glucoseColor,
            summaryText: summaryLine(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(formatter
                  .bloodGlucose(bloodGlucose.averageMmolPerLiter)
                  .text),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.bloodGlucose(value).text,
          )),
      ],
    );
  }
}

/// Kotlin `VitalsRespiratoryOverviewSectionContent`.
class HeartVitalsRespiratorySection extends StatelessWidget {
  const HeartVitalsRespiratorySection({
    super.key,
    required this.state,
    required this.period,
    required this.formatter,
    required this.display,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartVitalsOverviewDisplay display;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final range = state.selectedRange;

    final respiratoryRate = display.respiratoryRate;
    final bodyTemperature = display.bodyTemperature;
    final skinTemperature = display.skinTemperature;
    final skinDelta = skinTemperature?.cardDeltaCelsius;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionRespiratory),
        MetricCardGrid(cards: [
          OverviewCard(
            metric: HeartMetric.respiratoryRate,
            title: l10n.metricRespiratoryRate,
            value: respiratoryRate == null
                ? null
                : formatter
                    .respiratoryRate(respiratoryRate.cardBreathsPerMinute),
            icon: Icons.air,
            color: respiratoryColor,
            source: respiratoryRate?.cardSource,
          ),
          OverviewCard(
            metric: HeartMetric.bodyTemperature,
            title: l10n.metricBodyTemp,
            value: bodyTemperature == null
                ? null
                : formatter
                    .temperature(bodyTemperature.latest.temperatureCelsius),
            icon: Icons.device_thermostat,
            color: temperatureColor,
            source: bodyTemperature?.latest.source,
          ),
          OverviewCard(
            metric: HeartMetric.skinTemperature,
            title: l10n.metricSkinTemperature,
            value:
                skinDelta == null ? null : formatter.temperatureDelta(skinDelta),
            icon: Icons.device_thermostat,
            color: temperatureColor,
            source: skinTemperature?.latest.source,
          ),
        ]),
        // Kotlin `RespiratoryOverviewChartsContent`.
        if (respiratoryRate != null && respiratoryRate.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricRespiratoryRate,
            series: respiratoryRateSeries(respiratoryRate.entries, l10n, range,
                color: respiratoryColor, dayLabel: l10n.metricRespiratoryRate),
            selectedRange: range,
            period: period,
            accentColor: respiratoryColor,
            summaryText: summaryLine(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                formatter.respiratoryRate(respiratoryRate.periodAverage).text,
              ),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.respiratoryRate(value).text,
          )),
        if (bodyTemperature != null && bodyTemperature.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricBodyTemp,
            series: singleSeries(
              [
                for (final e in bodyTemperature.entries)
                  (e.time, e.temperatureCelsius),
              ],
              temperatureColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: temperatureColor,
            summaryText: summaryLine(
                l10n,
                state,
                period,
                l10n.summaryReadings(
                    formatter.count(bodyTemperature.readings))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.temperature(value).text,
          )),
        if (skinTemperature != null && skinTemperature.hasChart)
          sectionPadded(MetricLineChart(
            title: l10n.metricSkinTemperature,
            series: singleSeries(
              [
                for (final e in skinTemperature.chartEntries)
                  (e.time, e.averageDeltaCelsius!),
              ],
              temperatureColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: temperatureColor,
            summaryText: summaryLine(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                formatter
                    .temperatureDelta(skinTemperature.averageDeltaCelsius)
                    .text,
              ),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.temperatureDelta(value).text,
          )),
      ],
    );
  }
}

// ── Card grid ────────────────────────────────────────────────────────────────

/// A metric summary card, port of Kotlin `OverviewMetricCardData`.
@immutable
class OverviewCard {
  const OverviewCard({
    required this.metric,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
    this.source,
  });

  final HeartMetric metric;
  final String title;
  final DisplayValue? value;
  final IconData icon;
  final Color color;
  final String? source;
}

/// Kotlin `OverviewMetricRowsContent`: the metric cards laid out two per row,
/// each an [MetricCard]/[MetricCardPlaceholder] that opens its detail screen.
class MetricCardGrid extends StatelessWidget {
  const MetricCardGrid({super.key, required this.cards});

  final List<OverviewCard> cards;

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[];
    for (var i = 0; i < cards.length; i += 2) {
      final first = cards[i];
      final second = i + 1 < cards.length ? cards[i + 1] : null;
      rows.add(Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: _card(context, first)),
            const SizedBox(width: 12),
            Expanded(
              child: second == null
                  ? const SizedBox.shrink()
                  : _card(context, second),
            ),
          ],
        ),
      ));
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: rows);
  }

  Widget _card(BuildContext context, OverviewCard card) {
    final l10n = AppLocalizations.of(context);
    void open() =>
        context.push(AppRoutes.metricLocation(card.metric.routeName));
    final value = card.value;
    if (value == null) {
      return MetricCardPlaceholder(
        title: card.title,
        icon: card.icon,
        accentColor: card.color,
        message: l10n.messageNoReadingsPeriod,
        onTap: open,
      );
    }
    return MetricCard(
      title: card.title,
      value: value.value,
      unit: value.unit,
      icon: card.icon,
      accentColor: card.color,
      source: card.source,
      onTap: open,
    );
  }
}

/// The "This week · avg 68 bpm" line under every chart: the period title the
/// navigator shows, then whatever the chart has to say about it.
String summaryLine(
  AppLocalizations l10n,
  HeartVitalsOverviewState state,
  DatePeriod period,
  String extra,
) =>
    '${periodTitle(
      l10n,
      state.selectedRange,
      period,
      weekPeriodMode: state.weekPeriodMode,
    )} · $extra';
