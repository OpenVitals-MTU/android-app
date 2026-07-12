import 'package:flutter/material.dart';

import '../../core/time/local_date.dart';
import '../../l10n/app_localizations.dart';

/// Where a moment sits on a chart of ONE day, and the axis that says so.
///
/// This exists because the same twenty lines were written five times, and four of
/// them were wrong in the same way.
///
/// Each intraday card computed its own x-positions by scaling against the time
/// ELAPSED so far — so on a chart opened at 12:49 an event at 09:29 landed at 74%
/// of the width. Underneath it every one of them drew the same fixed axis:
/// `00:00 / 06:00 / 12:00 / 18:00`. The chart said quarter past five. The only
/// thing an intraday chart exists to tell you is WHEN, and it told you the wrong
/// hour — on hydration, on steps, on calories, on body, on nutrition.
///
/// It was only found on hydration, because that is the screen someone happened to
/// look at.
///
/// So the rule lives in one place: **the x axis is the whole day, always.** A
/// point sits at its real hour. Today's series simply stops at [endFraction]
/// instead of running to the right edge, which is honest — the rest of the day has
/// not happened, and a line drawn across it would be a claim about the future.
@immutable
class DayAxis {
  DayAxis(LocalDate date, {DateTime? now})
      : start = DateTime(date.year, date.month, date.day),
        isToday = date == LocalDate.fromDateTime(now ?? DateTime.now()),
        _now = now ?? DateTime.now();

  /// Local midnight — the left edge.
  final DateTime start;

  /// Whether the day is still happening.
  final bool isToday;

  final DateTime _now;

  static const int _dayMs = Duration.millisecondsPerDay;

  /// Where [time] sits across the day, in `0..1`.
  ///
  /// Against the WHOLE day, never against the elapsed part of it. That is the
  /// distinction the old code got wrong, and it is the only reason this class
  /// exists.
  double fractionOf(DateTime time) =>
      time.toLocal().difference(start).inMilliseconds.clamp(0, _dayMs) / _dayMs;

  /// Where the series should stop: now, if the day is still running.
  ///
  /// Not 1.0 for today. A line held out to the right edge at two in the afternoon
  /// draws ten hours that have not happened, and every earlier point then looks
  /// like it came sooner than it did.
  double get endFraction => isToday ? fractionOf(_now) : 1.0;

  @override
  bool operator ==(Object other) =>
      other is DayAxis && other.start == start && other._now == _now;

  @override
  int get hashCode => Object.hash(start, _now);
}

/// The `00:00 … 24:00` label row that goes under an intraday chart.
///
/// Evenly spaced, and TRUE: [DayAxis] spans the whole day, so the 12:00 tick
/// really is halfway across it. That was not so when each card scaled its own x by
/// the elapsed time and then drew this same row underneath.
class DayAxisLabels extends StatelessWidget {
  const DayAxisLabels({super.key, required this.axis});

  final DayAxis axis;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        for (final label in const ['00:00', '06:00', '12:00', '18:00', '24:00'])
          Text(
            label,
            style: theme.textTheme.labelSmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
      ],
    );
  }
}

/// The header a day card shows above its chart: a value, and what day it is.
class DayChartHeader extends StatelessWidget {
  const DayChartHeader({
    super.key,
    required this.axis,
    required this.value,
    required this.metricName,
    required this.accentColor,
    required this.dateText,
  });

  final DayAxis axis;
  final String value;
  final String metricName;
  final Color accentColor;

  /// The formatted day, for a chart that is not today's.
  final String dateText;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: theme.textTheme.headlineMedium?.copyWith(color: accentColor),
        ),
        Text(
          axis.isToday
              ? l10n.summaryToday(metricName)
              : l10n.summaryOnDate(metricName, dateText),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}
