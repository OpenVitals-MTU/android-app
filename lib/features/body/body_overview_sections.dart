import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/metric_interpretations.dart';
import '../../domain/model/body_models.dart';
import '../../domain/query/body_period_data.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/chart_axis.dart';
import '../../ui/charts/metric_line_plot.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_interpretation_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/swipe_to_delete_entry_row.dart';
import '../../ui/theme/app_colors.dart';
import 'body_summary.dart';

/// Ports of the Kotlin `BodyMetricSharedSections.kt` data helpers and the row /
/// card widgets the aggregate `/body` overview renders
/// (`BodyMetricOrderedSections.kt`, `BodyRows.kt`).

// ── Per-metric chart/statistics data (Kotlin `BodyMetricData`) ───────────────

@immutable
class BodyMetricData {
  const BodyMetricData({
    required this.title,
    required this.latest,
    required this.values,
    this.dayValues = const <(DateTime, double)>[],
    required this.color,
    required this.icon,
    required this.format,
  });

  final String title;

  /// The latest formatted value, or null when the metric has no reading.
  final DisplayValue? latest;

  /// Daily latest values feeding the period chart.
  final List<PeriodChartValue> values;

  /// Raw (time, value) samples feeding the DAY-range intraday line.
  final List<(DateTime, double)> dayValues;
  final Color color;
  final IconData icon;
  final DisplayValue Function(double) format;

  /// Kotlin `hasTrackedValues`: the metric earns a trend chart only when the
  /// period actually has values.
  bool get hasTrackedValues => values.isNotEmpty;
}

/// Kotlin `bodyMetricData`: the nine overview metrics, in the Kotlin order
/// (weight, height, BMI, FFMI, body fat, lean mass, bone mass, body water
/// mass, BMR).
List<BodyMetricData> bodyMetricData(
  BodyPeriodData data,
  BodySummary summary,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  DisplayValue plain(double value) =>
      DisplayValue(formatter.decimal(value, 1), '');
  return [
    BodyMetricData(
      title: l10n.metricWeight,
      latest: summary.latestWeightKg?.let(formatter.weight),
      values: dailyLatestValues(
        data.weightEntries,
        (e) => e.time,
        (e) => e.weightKg,
      ),
      dayValues: [for (final e in data.weightEntries) (e.time, e.weightKg)],
      color: AppColors.weight,
      icon: Icons.monitor_weight_outlined,
      format: formatter.weight,
    ),
    BodyMetricData(
      title: l10n.metricHeight,
      latest: summary.latestHeightCm?.let(formatter.height),
      values: dailyLatestValues(
        data.heightEntries,
        (e) => e.time,
        (e) => e.heightCm,
      ),
      dayValues: [for (final e in data.heightEntries) (e.time, e.heightCm)],
      color: AppColors.weight,
      icon: Icons.straighten,
      format: formatter.height,
    ),
    BodyMetricData(
      title: l10n.metricBmi,
      latest: summary.bmi?.let(plain),
      values: _bmiHistoryValues(data.weightEntries, summary.heightCm),
      dayValues: _bmiDayValues(data.weightEntries, summary.heightCm),
      color: AppColors.weight,
      icon: Icons.monitor_weight_outlined,
      format: plain,
    ),
    BodyMetricData(
      title: l10n.metricFfmi,
      latest: summary.adjustedFfmi?.let(plain),
      values: const <PeriodChartValue>[],
      color: AppColors.bodyFat,
      icon: Icons.fitness_center,
      format: plain,
    ),
    BodyMetricData(
      title: l10n.metricBodyFat,
      latest: summary.latestBodyFatPercent?.let(formatter.percent),
      values: dailyLatestValues(
        data.bodyFatEntries,
        (e) => e.time,
        (e) => e.percent,
      ),
      dayValues: [for (final e in data.bodyFatEntries) (e.time, e.percent)],
      color: AppColors.bodyFat,
      icon: Icons.monitor_weight_outlined,
      format: formatter.percent,
    ),
    BodyMetricData(
      title: l10n.metricLeanMass,
      latest: summary.latestLeanMassKg?.let(formatter.bodyMass),
      values: dailyLatestValues(
        data.leanMassEntries,
        (e) => e.time,
        (e) => e.massKg,
      ),
      dayValues: [for (final e in data.leanMassEntries) (e.time, e.massKg)],
      color: AppColors.weight,
      icon: Icons.monitor_weight_outlined,
      format: formatter.bodyMass,
    ),
    BodyMetricData(
      title: l10n.metricBoneMass,
      latest:
          summary.latestBoneMassKg?.let((v) => formatter.bodyMass(v, decimals: 2)),
      values: dailyLatestValues(
        data.boneMassEntries,
        (e) => e.time,
        (e) => e.massKg,
      ),
      dayValues: [for (final e in data.boneMassEntries) (e.time, e.massKg)],
      color: AppColors.weight,
      icon: Icons.monitor_weight_outlined,
      format: (v) => formatter.bodyMass(v, decimals: 2),
    ),
    BodyMetricData(
      title: l10n.metricBodyWaterMass,
      latest: summary.latestBodyWaterMassKg
          ?.let((v) => formatter.bodyMass(v, decimals: 2)),
      values: dailyLatestValues(
        data.bodyWaterMassEntries,
        (e) => e.time,
        (e) => e.massKg,
      ),
      dayValues: [
        for (final e in data.bodyWaterMassEntries) (e.time, e.massKg),
      ],
      color: AppColors.weight,
      icon: Icons.local_drink_outlined,
      format: (v) => formatter.bodyMass(v, decimals: 2),
    ),
    BodyMetricData(
      title: l10n.metricBmr,
      latest: summary.latestBmrKcal?.let(formatter.energy),
      values: dailyLatestValues(
        data.bmrEntries,
        (e) => e.time,
        (e) => e.kcalPerDay,
      ),
      dayValues: [for (final e in data.bmrEntries) (e.time, e.kcalPerDay)],
      color: AppColors.calories,
      icon: Icons.local_fire_department_outlined,
      format: formatter.energy,
    ),
  ];
}

/// Kotlin `dailyLatestValues`: one chart value per tracked day — the value of
/// that day's latest reading.
List<PeriodChartValue> dailyLatestValues<T>(
  List<T> entries,
  DateTime Function(T) time,
  double Function(T) value,
) {
  final latestByDate = <LocalDate, T>{};
  for (final entry in entries) {
    final date = instantToLocalDate(time(entry));
    final current = latestByDate[date];
    if (current == null || time(entry).isAfter(time(current))) {
      latestByDate[date] = entry;
    }
  }
  final values = [
    for (final MapEntry(key: date, value: entry) in latestByDate.entries)
      PeriodChartValue(date, value(entry)),
  ]..sort((a, b) => a.date.compareTo(b.date));
  return values;
}

List<PeriodChartValue> _bmiHistoryValues(
  List<WeightEntry> entries,
  double? heightCm,
) {
  final heightMeters = _heightMeters(heightCm);
  if (heightMeters == null) return const <PeriodChartValue>[];
  return dailyLatestValues(
    entries,
    (e) => e.time,
    (e) => e.weightKg / (heightMeters * heightMeters),
  );
}

List<(DateTime, double)> _bmiDayValues(
  List<WeightEntry> entries,
  double? heightCm,
) {
  final heightMeters = _heightMeters(heightCm);
  if (heightMeters == null) return const <(DateTime, double)>[];
  return [
    for (final e in entries)
      (e.time, e.weightKg / (heightMeters * heightMeters)),
  ];
}

double? _heightMeters(double? heightCm) =>
    (heightCm != null && heightCm > 0.0) ? heightCm / 100.0 : null;

// ── Combined reading list (Kotlin `bodyReadingItems`) ────────────────────────

@immutable
class BodyReadingItem {
  const BodyReadingItem({
    required this.value,
    required this.source,
    required this.time,
    required this.accentColor,
    this.editType,
    this.editId,
  });

  /// "Label · value" as in Kotlin.
  final String value;
  final String source;
  final DateTime time;
  final Color accentColor;

  /// Set when the entry is an editable OpenVitals manual entry (Kotlin
  /// `isOpenVitalsEntry && id.isNotBlank()`); null rows are read-only.
  final BodyMeasurementType? editType;
  final String? editId;

  bool get editable => editType != null && editId != null;

  /// A stable identity for the swipe-to-delete [Key].
  String get rowKey =>
      '${editType?.storageName ?? value}-$editId-${time.microsecondsSinceEpoch}';
}

/// Kotlin `bodyReadingItems`: every reading across the eight metrics, labelled.
/// Weight / height / body-fat OpenVitals entries carry edit + delete actions.
List<BodyReadingItem> bodyReadingItems(
  BodyPeriodData data,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  BodyMeasurementType? editTypeFor(
    BodyMeasurementType type,
    bool isOpenVitalsEntry,
    String id,
  ) =>
      (isOpenVitalsEntry && id.isNotEmpty) ? type : null;

  return [
    for (final e in data.weightEntries)
      BodyReadingItem(
        value: '${l10n.metricWeight} · ${formatter.weight(e.weightKg).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.weight,
        editType:
            editTypeFor(BodyMeasurementType.weight, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.heightEntries)
      BodyReadingItem(
        value: '${l10n.metricHeight} · ${formatter.height(e.heightCm).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.weight,
        editType:
            editTypeFor(BodyMeasurementType.height, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.bodyFatEntries)
      BodyReadingItem(
        value: '${l10n.metricBodyFat} · ${formatter.percent(e.percent).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.bodyFat,
        editType:
            editTypeFor(BodyMeasurementType.bodyFat, e.isOpenVitalsEntry, e.id),
        editId: (e.isOpenVitalsEntry && e.id.isNotEmpty) ? e.id : null,
      ),
    for (final e in data.leanMassEntries)
      BodyReadingItem(
        value: '${l10n.metricLeanMass} · ${formatter.bodyMass(e.massKg).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.weight,
      ),
    for (final e in data.bmrEntries)
      BodyReadingItem(
        value: '${l10n.metricBmr} · ${formatter.energy(e.kcalPerDay).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.calories,
      ),
    for (final e in data.boneMassEntries)
      BodyReadingItem(
        value:
            '${l10n.metricBoneMass} · ${formatter.bodyMass(e.massKg, decimals: 2).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.weight,
      ),
    for (final e in data.bodyWaterMassEntries)
      BodyReadingItem(
        value:
            '${l10n.metricBodyWaterMass} · ${formatter.bodyMass(e.massKg, decimals: 2).text}',
        source: e.source,
        time: e.time,
        accentColor: AppColors.weight,
      ),
  ];
}

// ── Rows (Kotlin `BodyReadingRow`) ───────────────────────────────────────────

/// A reading row: date-time + source on the left, the accent-colored value and
/// an optional edit affordance on the right. Deletable rows swipe to delete.
class BodyReadingRow extends StatelessWidget {
  const BodyReadingRow({
    super.key,
    required this.item,
    this.onEdit,
    this.onDelete,
  });

  final BodyReadingItem item;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();

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
                        .format(item.time.toLocal()),
                    style: theme.textTheme.bodyMedium,
                  ),
                  SourceChip(source: item.source),
                ],
              ),
            ),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  item.value,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(color: item.accentColor),
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
      key: ValueKey(item.rowKey),
      onDelete: onDelete!,
      child: content,
    );
  }
}

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
        _padded(SectionHeader(l10n.sectionMetricContext)),
        if (bmiResult != null)
          _padded(MetricInterpretationCard(
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
          _padded(MetricInterpretationCard(
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

  /// Injectable clock: today's x axis ends at "now", past days at midnight.
  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();

    final currentTime = now ?? DateTime.now();
    final isToday = selectedDate == LocalDate.now();
    final dayStart =
        DateTime(selectedDate.year, selectedDate.month, selectedDate.day);
    final chartEnd =
        isToday ? currentTime : dayStart.add(const Duration(days: 1));
    final elapsedMillis =
        chartEnd.difference(dayStart).inMilliseconds.clamp(1, 1 << 62);

    final points = [...metricData.dayValues]
      ..sort((a, b) => a.$1.compareTo(b.$1));
    final latest = points.isEmpty ? null : points.last.$2;
    final minValue = points.isEmpty
        ? 0.0
        : points.map((p) => p.$2).reduce((a, b) => a < b ? a : b);
    final maxValue = points.isEmpty
        ? 1.0
        : points.map((p) => p.$2).reduce((a, b) => a > b ? a : b);
    final span = maxValue - minValue;
    final padding =
        (span > 0.0 ? span : (maxValue.abs() < 1.0 ? 1.0 : maxValue.abs())) *
            0.08;
    final unclampedMin = minValue - padding;
    final axisMin = unclampedMin < 0.0 ? 0.0 : unclampedMin;
    final axisMax = maxValue + padding;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              latest?.let((v) => metricData.format(v).text) ?? l10n.noData,
              style: theme.textTheme.headlineMedium
                  ?.copyWith(color: metricData.color),
            ),
            Text(
              isToday
                  ? l10n.summaryToday(metricData.title)
                  : l10n.summaryOnDate(
                      metricData.title,
                      DateFormat.yMMMd(locale).format(dayStart),
                    ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (points.isEmpty)
              Text(
                isToday
                    ? l10n.summaryEmptyToday(l10n.screenBody)
                    : l10n.summaryEmptyDay(l10n.screenBody),
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else ...[
              MetricLinePlot(
                points: [
                  for (final (time, value) in points)
                    MetricLinePlotPoint(
                      xFraction: time
                              .toLocal()
                              .difference(dayStart)
                              .inMilliseconds
                              .clamp(0, elapsedMillis) /
                          elapsedMillis,
                      value: value,
                    ),
                ],
                minValue: axisMin,
                maxValue: axisMax,
                accentColor: metricData.color,
                valueFormatter: (value) => metricData.format(value).text,
              ),
              const SizedBox(height: 8),
              ChartXAxisWithYAxis(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    for (final label in [
                      '00:00',
                      '06:00',
                      '12:00',
                      '18:00',
                      if (isToday) l10n.summaryNow else '24:00',
                    ])
                      Text(
                        label,
                        style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Text(
                l10n.summaryLastUpdate(
                  DateFormat.jm(locale).format(points.last.$1.toLocal()),
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

extension<T> on T {
  R let<R>(R Function(T) block) => block(this);
}
