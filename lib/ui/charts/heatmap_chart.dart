import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/time/local_date.dart';
import '../components/ov_card.dart';
import 'bar_chart.dart' show PeriodChartValue;
import 'metric_day_opener.dart';

/// One cell of a calendar heatmap. A null [date] is a layout filler for the
/// leading/trailing days of the first/last week. Port of Kotlin
/// `PeriodHeatmapCell`.
class PeriodHeatmapCell {
  const PeriodHeatmapCell({
    required this.date,
    required this.value,
    required this.isWithinLoadedPeriod,
  });

  final LocalDate? date;
  final double value;
  final bool isWithinLoadedPeriod;
}

PeriodHeatmapCell _emptyCell() =>
    const PeriodHeatmapCell(date: null, value: 0.0, isWithinLoadedPeriod: false);

Map<LocalDate, double> _valuesByDate(List<PeriodChartValue> values) {
  final byDate = <LocalDate, double>{};
  for (final value in values) {
    byDate[value.date] = (byDate[value.date] ?? 0.0) + value.value;
  }
  return byDate;
}

/// The month grid cells (Mon→Sun rows, with leading/trailing fillers). Port of
/// Kotlin `periodMonthHeatmapCells`.
///
/// [rolling] chooses what span the grid covers. A calendar month (the default)
/// draws the whole month of [DatePeriod.start] — the 1st to the last day — with
/// days past the loaded window greyed. A rolling window ("Last 30 days") spans
/// two calendar months, so it draws exactly `[period.start, period.end]` as
/// consecutive weeks; drawing only one month of it left ~20 days blank and hid
/// the other month's half of the window entirely.
List<PeriodHeatmapCell> periodMonthHeatmapCells(
  List<PeriodChartValue> values,
  DatePeriod period, {
  bool rolling = false,
}) {
  final firstDay =
      rolling ? period.start : period.start.withDayOfMonth(1);
  final lastDay =
      rolling ? period.end : firstDay.withDayOfMonth(firstDay.lengthOfMonth);
  final byDate = _valuesByDate(values);

  final leadingEmptyCells = firstDay.dayOfWeek - DateTime.monday;
  final dayCells = <PeriodHeatmapCell>[];
  var date = firstDay;
  while (!date.isAfter(lastDay)) {
    dayCells.add(
      PeriodHeatmapCell(
        date: date,
        value: byDate[date] ?? 0.0,
        isWithinLoadedPeriod:
            !date.isBefore(period.start) && !date.isAfter(period.end),
      ),
    );
    date = date.plusDays(1);
  }

  final totalBeforeTrailing = leadingEmptyCells + dayCells.length;
  final trailingRemainder = totalBeforeTrailing % 7;
  final trailingEmptyCells = trailingRemainder == 0 ? 0 : 7 - trailingRemainder;

  return [
    for (var i = 0; i < leadingEmptyCells; i++) _emptyCell(),
    ...dayCells,
    for (var i = 0; i < trailingEmptyCells; i++) _emptyCell(),
  ];
}

/// The full-year dot grid cells. Port of Kotlin `periodYearHeatmapCells`.
List<PeriodHeatmapCell> periodYearHeatmapCells(
  List<PeriodChartValue> values,
  DatePeriod period,
) {
  final firstDay = period.start.withDayOfYear(1);
  final lastDay = firstDay.withDayOfYear(firstDay.lengthOfYear);
  final byDate = _valuesByDate(values);

  final cells = <PeriodHeatmapCell>[];
  var date = firstDay;
  while (!date.isAfter(lastDay)) {
    cells.add(
      PeriodHeatmapCell(
        date: date,
        value: byDate[date] ?? 0.0,
        isWithinLoadedPeriod: !date.isAfter(period.end),
      ),
    );
    date = date.plusDays(1);
  }
  return cells;
}

Color _heatmapCellColor(
  ColorScheme scheme,
  double value,
  double minPositiveValue,
  double maxValue,
  bool isWithinLoadedPeriod,
  Color accentColor,
) {
  if (!isWithinLoadedPeriod) {
    return scheme.surfaceContainerHighest.withValues(alpha: 0.35);
  }
  if (value <= 0.0) {
    return scheme.surfaceContainerHighest.withValues(alpha: 0.65);
  }
  final fraction = maxValue <= minPositiveValue
      ? 1.0
      : ((value - minPositiveValue) / (maxValue - minPositiveValue))
          .clamp(0.0, 1.0);
  return accentColor.withValues(alpha: 0.25 + 0.75 * fraction);
}

double _minPositive(List<PeriodHeatmapCell> cells) {
  final positives = cells.map((c) => c.value).where((v) => v > 0.0);
  return positives.isEmpty ? 0.0 : positives.reduce((a, b) => a < b ? a : b);
}

double _maxValue(List<PeriodHeatmapCell> cells) {
  final maxCell = cells
      .map((c) => c.value)
      .fold<double>(0.0, (currentMax, v) => v > currentMax ? v : currentMax);
  return maxCell < 1.0 ? 1.0 : maxCell;
}

final DateFormat _weekdayFormat = DateFormat('EEE');
final DateFormat _dayFormat = DateFormat('d');

/// The month calendar heatmap card. Port of Kotlin `PeriodMonthHeatmap`.
class PeriodMonthHeatmap extends StatelessWidget {
  const PeriodMonthHeatmap({
    super.key,
    required this.title,
    required this.values,
    required this.period,
    required this.accentColor,
    required this.summaryText,
    this.selectedDate,
    this.onDateSelected,
    this.rolling = false,
  });

  final String title;
  final List<PeriodChartValue> values;
  final DatePeriod period;
  final Color accentColor;
  final String summaryText;
  final LocalDate? selectedDate;
  final ValueChanged<LocalDate>? onDateSelected;

  /// Whether the period is a rolling window ("Last 30 days") rather than a
  /// calendar month. A rolling window renders exactly its span across the month
  /// boundary; see [periodMonthHeatmapCells].
  final bool rolling;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final cells = periodMonthHeatmapCells(values, period, rolling: rolling);
    final minPositive = _minPositive(cells);
    final maxValue = _maxValue(cells);
    final gridStart = rolling ? period.start : period.start.withDayOfMonth(1);
    final monthStartMonday = gridStart.previousOrSame(DateTime.monday);
    final weekdays = [
      for (var offset = 0; offset < 7; offset++)
        _weekdayFormat.format(_toDateTime(monthStartMonday.plusDays(offset))),
    ];
    final rows = <List<PeriodHeatmapCell>>[];
    for (var i = 0; i < cells.length; i += 7) {
      rows.add(cells.sublist(i, (i + 7).clamp(0, cells.length)));
    }

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _HeatmapHeader(title: title, summaryText: summaryText),
            const SizedBox(height: 16),
            Row(
              children: [
                for (final weekday in weekdays)
                  Expanded(
                    child: Text(
                      weekday,
                      style: theme.textTheme.labelSmall
                          ?.copyWith(color: scheme.onSurfaceVariant),
                      textAlign: TextAlign.center,
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            for (final rowCells in rows) ...[
              Row(
                children: [
                  for (var i = 0; i < rowCells.length; i++) ...[
                    if (i > 0) const SizedBox(width: 8),
                    Expanded(
                      child: _MonthCell(
                        cell: rowCells[i],
                        color: _heatmapCellColor(
                          scheme,
                          rowCells[i].value,
                          minPositive,
                          maxValue,
                          rowCells[i].isWithinLoadedPeriod,
                          accentColor,
                        ),
                        accentColor: accentColor,
                        selected: rowCells[i].date != null &&
                            rowCells[i].date == selectedDate,
                        onDateSelected: onDateSelected,
                      ),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 8),
            ],
            _HeatmapLegend(
              accentColor: accentColor,
              minPositiveValue: minPositive,
              maxValue: maxValue,
            ),
          ],
        ),
      ),
    );
  }
}

class _MonthCell extends StatelessWidget {
  const _MonthCell({
    required this.cell,
    required this.color,
    required this.accentColor,
    required this.selected,
    required this.onDateSelected,
  });

  final PeriodHeatmapCell cell;
  final Color color;
  final Color accentColor;
  final bool selected;
  final ValueChanged<LocalDate>? onDateSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final date = cell.date;
    // Inside a metric-detail scaffold, tapping a day drills into its Day view;
    // otherwise it falls back to the host's pin-a-day callback (or is inert).
    final openDay = MetricDetailDayOpener.maybeOf(context);
    final tappable = date != null &&
        cell.isWithinLoadedPeriod &&
        (openDay != null || onDateSelected != null);
    return AspectRatio(
      aspectRatio: 1,
      child: GestureDetector(
        onTap: tappable
            ? () => (openDay ?? onDateSelected)!(date)
            : null,
        child: Container(
          decoration: BoxDecoration(
            color: color,
            borderRadius: const BorderRadius.all(Radius.circular(8)),
            border: selected ? Border.all(color: accentColor, width: 2) : null,
          ),
          alignment: Alignment.center,
          child: date == null
              ? null
              : Text(
                  _dayFormat.format(_toDateTime(date)),
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: theme.colorScheme.onSurface),
                  textAlign: TextAlign.center,
                ),
        ),
      ),
    );
  }
}

/// The full-year dot heatmap card. Port of Kotlin `PeriodYearHeatmap`.
class PeriodYearHeatmap extends StatelessWidget {
  const PeriodYearHeatmap({
    super.key,
    required this.title,
    required this.values,
    required this.period,
    required this.accentColor,
    required this.summaryText,
  });

  final String title;
  final List<PeriodChartValue> values;
  final DatePeriod period;
  final Color accentColor;
  final String summaryText;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final cells = periodYearHeatmapCells(values, period);
    final minPositive = _minPositive(cells);
    final maxValue = _maxValue(cells);
    final rows = <List<PeriodHeatmapCell>>[];
    for (var i = 0; i < cells.length; i += 20) {
      rows.add(cells.sublist(i, (i + 20).clamp(0, cells.length)));
    }

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _HeatmapHeader(title: title, summaryText: summaryText),
            const SizedBox(height: 16),
            for (final rowCells in rows) ...[
              Row(
                children: [
                  for (var i = 0; i < rowCells.length; i++) ...[
                    if (i > 0) const SizedBox(width: 4),
                    Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        color: _heatmapCellColor(
                          scheme,
                          rowCells[i].value,
                          minPositive,
                          maxValue,
                          rowCells[i].isWithinLoadedPeriod,
                          accentColor,
                        ),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ],
                  if (rowCells.length < 20) const Spacer(),
                ],
              ),
              const SizedBox(height: 4),
            ],
            const SizedBox(height: 8),
            _HeatmapLegend(
              accentColor: accentColor,
              minPositiveValue: minPositive,
              maxValue: maxValue,
            ),
          ],
        ),
      ),
    );
  }
}

class _HeatmapHeader extends StatelessWidget {
  const _HeatmapHeader({required this.title, required this.summaryText});

  final String title;
  final String summaryText;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: theme.textTheme.titleSmall
              ?.copyWith(color: theme.colorScheme.onSurface),
        ),
        const SizedBox(height: 4),
        Text(
          summaryText,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}

class _HeatmapLegend extends StatelessWidget {
  const _HeatmapLegend({
    required this.accentColor,
    required this.minPositiveValue,
    required this.maxValue,
  });

  final Color accentColor;
  final double minPositiveValue;
  final double maxValue;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final labelStyle = theme.textTheme.labelSmall
        ?.copyWith(color: scheme.onSurfaceVariant);
    return Row(
      children: [
        Text('Less', style: labelStyle),
        const Spacer(),
        for (var index = 0; index < 5; index++) ...[
          if (index > 0) const SizedBox(width: 6),
          Container(
            width: 12,
            height: 12,
            decoration: BoxDecoration(
              color: _heatmapCellColor(
                scheme,
                maxValue <= minPositiveValue
                    ? maxValue
                    : minPositiveValue +
                        (maxValue - minPositiveValue) * index / 4.0,
                minPositiveValue,
                maxValue,
                true,
                accentColor,
              ),
              shape: BoxShape.circle,
            ),
          ),
        ],
        const Spacer(),
        Text('More', style: labelStyle),
      ],
    );
  }
}

DateTime _toDateTime(LocalDate date) => DateTime(date.year, date.month, date.day);
