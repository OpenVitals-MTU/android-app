import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_day_chart.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_interpretation_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/swipe_to_delete_entry_row.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/body_display.dart';
import '../../../ui/components/section_padding.dart';

/// Ports of the Kotlin `BodyMetricSharedSections.kt` presentation helpers and the
/// row / card widgets the aggregate `/body` overview renders
/// (`BodyMetricOrderedSections.kt`, `BodyRows.kt`).
///
/// The series, the latest values and the reading list arrive precomputed on the
/// [BodyDisplay]; what is left here is the title, the accent, the icon and the
/// unit formatting — which is what presentation is.

// ── Per-metric chart/statistics data (Kotlin `BodyMetricData`) ───────────────

/// One metric's precomputed series, dressed for the screen: its localized title,
/// its accent, its icon and the formatter that turns its storage-unit values into
/// text.
@immutable
class BodyMetricData {
  const BodyMetricData({
    required this.series,
    required this.title,
    required this.color,
    required this.icon,
    required this.format,
  });

  final BodyMetricSeries series;
  final String title;
  final Color color;
  final IconData icon;
  final DisplayValue Function(double) format;

  /// Daily latest values feeding the period chart.
  List<PeriodChartValue> get values => series.values;

  /// Raw samples feeding the DAY-range intraday line, oldest first.
  List<DaySample> get daySamples => series.daySamples;

  /// Kotlin `hasTrackedValues`: the metric earns a trend chart only when the
  /// period actually has values.
  bool get hasTrackedValues => series.hasTrackedValues;

  /// The latest formatted value, or null when the metric has no reading.
  DisplayValue? get latest => series.latest?.let(format);
}

/// Dresses every precomputed series in its title, accent, icon and formatter, in
/// the Kotlin order (weight, height, BMI, FFMI, body fat, lean mass, bone mass,
/// body water mass, BMR — the order [BodyDisplay.metrics] is already in).
List<BodyMetricData> bodyMetricData(
  BodyDisplay display,
  UnitFormatter formatter,
  AppLocalizations l10n,
) =>
    [
      for (final series in display.metrics)
        bodyMetricDataFor(series, formatter, l10n),
    ];

/// The same, for the metrics that earned a trend chart.
List<BodyMetricData> trackedBodyMetricData(
  BodyDisplay display,
  UnitFormatter formatter,
  AppLocalizations l10n,
) =>
    [
      for (final series in display.trackedMetrics)
        bodyMetricDataFor(series, formatter, l10n),
    ];

BodyMetricData bodyMetricDataFor(
  BodyMetricSeries series,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  DisplayValue plain(double value) =>
      DisplayValue(formatter.decimal(value, 1), '');
  DisplayValue mass2(double value) => formatter.bodyMass(value, decimals: 2);

  switch (series.kind) {
    case BodyMetricKind.weight:
      return BodyMetricData(
        series: series,
        title: l10n.metricWeight,
        color: AppColors.weight,
        icon: Icons.monitor_weight_outlined,
        format: formatter.weight,
      );
    case BodyMetricKind.height:
      return BodyMetricData(
        series: series,
        title: l10n.metricHeight,
        color: AppColors.weight,
        icon: Icons.straighten,
        format: formatter.height,
      );
    case BodyMetricKind.bmi:
      return BodyMetricData(
        series: series,
        title: l10n.metricBmi,
        color: AppColors.weight,
        icon: Icons.monitor_weight_outlined,
        format: plain,
      );
    case BodyMetricKind.ffmi:
      return BodyMetricData(
        series: series,
        title: l10n.metricFfmi,
        color: AppColors.bodyFat,
        icon: Icons.fitness_center,
        format: plain,
      );
    case BodyMetricKind.bodyFat:
      return BodyMetricData(
        series: series,
        title: l10n.metricBodyFat,
        color: AppColors.bodyFat,
        icon: Icons.monitor_weight_outlined,
        format: formatter.percent,
      );
    case BodyMetricKind.leanMass:
      return BodyMetricData(
        series: series,
        title: l10n.metricLeanMass,
        color: AppColors.weight,
        icon: Icons.monitor_weight_outlined,
        format: formatter.bodyMass,
      );
    case BodyMetricKind.boneMass:
      return BodyMetricData(
        series: series,
        title: l10n.metricBoneMass,
        color: AppColors.weight,
        icon: Icons.monitor_weight_outlined,
        format: mass2,
      );
    case BodyMetricKind.bodyWaterMass:
      return BodyMetricData(
        series: series,
        title: l10n.metricBodyWaterMass,
        color: AppColors.weight,
        icon: Icons.local_drink_outlined,
        format: mass2,
      );
    case BodyMetricKind.bmr:
      return BodyMetricData(
        series: series,
        title: l10n.metricBmr,
        color: AppColors.calories,
        icon: Icons.local_fire_department_outlined,
        format: formatter.energy,
      );
  }
}

// ── Rows (Kotlin `BodyReadingRow`) ───────────────────────────────────────────

/// A reading row: date-time + source on the left, the accent-colored "label ·
/// value" and an optional edit affordance on the right. Deletable rows swipe to
/// delete.
class BodyReadingRow extends StatelessWidget {
  const BodyReadingRow({
    super.key,
    required this.reading,
    required this.formatter,
    this.onEdit,
    this.onDelete,
  });

  final BodyReading reading;
  final UnitFormatter formatter;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final value = _readingText(reading, formatter, l10n);

    final content = OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    DateFormat.yMMMd(locale)
                        .add_jm()
                        .format(reading.time.toLocal()),
                    style: theme.textTheme.bodyMedium,
                  ),
                  SourceChip(source: reading.source),
                ],
              ),
            ),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  value,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(color: _readingColor(reading.kind)),
                ),
                if (onEdit != null) ...[
                  const SizedBox(width: 4),
                  IconButton(
                    onPressed: onEdit,
                    tooltip: l10n.cdEditEntry,
                    icon: Icon(
                      Icons.edit_outlined,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );

    if (onDelete == null) return content;
    return SwipeToDeleteEntryRow(
      key: ValueKey(_rowKey(reading, value)),
      onDelete: onDelete!,
      child: content,
    );
  }
}

/// "Label · value" as in Kotlin.
String _readingText(
  BodyReading reading,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  switch (reading.kind) {
    case BodyMetricKind.weight:
      return '${l10n.metricWeight} · ${formatter.weight(reading.value).text}';
    case BodyMetricKind.height:
      return '${l10n.metricHeight} · ${formatter.height(reading.value).text}';
    case BodyMetricKind.bodyFat:
      return '${l10n.metricBodyFat} · ${formatter.percent(reading.value).text}';
    case BodyMetricKind.leanMass:
      return '${l10n.metricLeanMass} · ${formatter.bodyMass(reading.value).text}';
    case BodyMetricKind.bmr:
      return '${l10n.metricBmr} · ${formatter.energy(reading.value).text}';
    case BodyMetricKind.boneMass:
      return '${l10n.metricBoneMass} · '
          '${formatter.bodyMass(reading.value, decimals: 2).text}';
    case BodyMetricKind.bodyWaterMass:
      return '${l10n.metricBodyWaterMass} · '
          '${formatter.bodyMass(reading.value, decimals: 2).text}';
    // Derived metrics have no readings of their own.
    case BodyMetricKind.bmi:
    case BodyMetricKind.ffmi:
      return formatter.decimal(reading.value, 1);
  }
}

Color _readingColor(BodyMetricKind kind) => switch (kind) {
      BodyMetricKind.bodyFat || BodyMetricKind.ffmi => AppColors.bodyFat,
      BodyMetricKind.bmr => AppColors.calories,
      _ => AppColors.weight,
    };

/// A stable identity for the swipe-to-delete [Key].
String _rowKey(BodyReading reading, String value) =>
    '${reading.editType?.storageName ?? value}-${reading.editId}'
    '-${reading.time.microsecondsSinceEpoch}';

// ── BMI / FFMI context cards (Kotlin `BmiContextCardsContent`) ───────────────

class BmiContextCards extends StatelessWidget {
  const BmiContextCards({
    super.key,
    required this.bmi,
    required this.ffmi,
    required this.adjustedFfmi,
    required this.formatter,
  });

  final double? bmi;
  final double? ffmi;
  final double? adjustedFfmi;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final bmiResult = bmi?.let(bmiInterpretation);
    final ffmiResult = adjustedFfmi?.let(ffmiInterpretation);
    if (bmiResult == null && ffmiResult == null) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(SectionHeader(l10n.sectionMetricContext)),
        if (bmiResult != null)
          sectionPadded(MetricInterpretationCard(
            title: l10n.interpretationBmiTitle,
            status: switch (bmiResult.category) {
              BmiCategory.underweight => l10n.interpretationBmiUnderweight,
              BmiCategory.healthy => l10n.interpretationBmiHealthy,
              BmiCategory.overweight => l10n.interpretationBmiOverweight,
              BmiCategory.obesityClass1 => l10n.interpretationBmiObesity1,
              BmiCategory.obesityClass2 => l10n.interpretationBmiObesity2,
              BmiCategory.obesityClass3 => l10n.interpretationBmiObesity3,
            },
            body: l10n.interpretationBmiBody,
            source: l10n.interpretationBmiSource,
            icon: Icons.monitor_weight_outlined,
            accentColor: AppColors.weight,
            severity: bmiResult.severity,
          )),
        if (ffmiResult != null && ffmi != null)
          sectionPadded(MetricInterpretationCard(
            title: l10n.interpretationFfmiTitle,
            status: switch (ffmiResult.category) {
              FfmiCategory.belowAverage => l10n.interpretationFfmiBelowAverage,
              FfmiCategory.average => l10n.interpretationFfmiAverage,
              FfmiCategory.aboveAverage => l10n.interpretationFfmiAboveAverage,
              FfmiCategory.excellent => l10n.interpretationFfmiExcellent,
              FfmiCategory.superior => l10n.interpretationFfmiSuperior,
              FfmiCategory.exceptional => l10n.interpretationFfmiExceptional,
              FfmiCategory.elite => l10n.interpretationFfmiElite,
            },
            body: l10n.interpretationFfmiBody(
              formatter.decimal(ffmi!, 1),
              formatter.decimal(adjustedFfmi!, 1),
            ),
            source: l10n.interpretationFfmiSource,
            icon: Icons.fitness_center,
            accentColor: AppColors.bodyFat,
            severity: ffmiResult.severity,
          )),
      ],
    );
  }
}

// ── DAY-range intraday chart (Kotlin `BodyIntradayMetricChartCard`) ──────────

class BodyIntradayMetricChartCard extends StatelessWidget {
  const BodyIntradayMetricChartCard({
    super.key,
    required this.selectedDate,
    required this.metricData,
    this.now,
  });

  final LocalDate selectedDate;
  final BodyMetricData metricData;

  /// Injectable clock: today's series stops at "now", a past day's runs to midnight.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    // Precomputed by the view-model, oldest first.
    final samples = metricData.daySamples;
    final latest = samples.isEmpty ? null : samples.last.value;

    return MetricDayChart(
      axis: DayAxis(selectedDate, now: now),
      samples: samples,
      // A weight at 08:00 is a fact about 08:00; it says nothing about midnight.
      shape: DaySeriesShape.raw,
      // Weight moves in kilos around 70, not around zero — an axis from 0 would
      // draw every day as the same flat line. Pad the span, but never dip below
      // zero, since none of these metrics can.
      range: ChartRange.padded(
        [for (final sample in samples) sample.value],
        floor: 0,
      ),
      accentColor: metricData.color,
      metricName: metricData.title,
      emptyLabel: l10n.screenBody,
      headlineText:
          latest == null ? l10n.noData : metricData.format(latest).text,
      valueFormatter: (value) => metricData.format(value).text,
    );
  }
}


extension<T> on T {
  R let<R>(R Function(T) block) => block(this);
}
