import 'package:flutter/material.dart';

import '../../core/time/local_date.dart';
import '../../l10n/app_localizations.dart';
import 'chart_axis.dart';
import 'chart_viewport.dart';
import 'metric_line_plot.dart';

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

  /// The inverse of [fractionOf]: what time it was, that far across the day.
  ///
  /// The scrubber needs it — a finger lands on an x, and the chart has to say what
  /// hour that was. Against the whole day, for the same reason [fractionOf] is.
  DateTime timeAt(double fraction) => start.add(
        Duration(milliseconds: (fraction.clamp(0.0, 1.0) * _dayMs).round()),
      );

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
///
/// It carries its own [inset], and that is the point. [MetricLinePlot] hands its
/// left edge to [YAxisChart] for the value labels, so the plot starts
/// [kChartYAxisWidth] + [kChartAxisGap] in from the card. An hour row that does not
/// start there is not describing the chart above it: on a phone that shift lands
/// 12:00 at about 41% of the plot instead of halfway.
///
/// The Kotlin app never got this wrong — every single hour row went through
/// `ChartXAxisWithYAxis`. The port dropped the wrapper on hydration, nutrition and
/// the heart timeline, because remembering to wrap is a thing you can forget. So
/// the row insets itself, and there is nothing left to remember.
///
/// It takes no [DayAxis], because it does not need one: the row is the same five
/// labels for every day. Kotlin's cards wrote "Now" in place of "24:00" on today,
/// which was only ever a patch over the elapsed-scaling bug — the axis really did
/// end at now, because they had squeezed the day into the part that had happened.
/// The axis is the whole day. Midnight is midnight.
///
/// Painters that draw no y axis (the body-energy strip) pass `inset: 0`.
class DayAxisLabels extends StatelessWidget {
  const DayAxisLabels({
    super.key,
    this.inset = kChartPlotInset,
    this.viewport = ChartViewport.full,
  });

  /// How far the plot above starts from the left edge of the card.
  final double inset;

  /// The slice of the day on show, when the chart above has been pinched.
  ///
  /// The row is no longer five fixed labels once a chart can be zoomed: an hour row that
  /// still said `00:00 … 24:00` over a plot showing half past seven to nine would be the
  /// very bug this class was written to kill, back again. The labels are computed from
  /// the visible slice instead — and at full zoom that arithmetic gives back exactly the
  /// same five it always drew.
  final ChartViewport viewport;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        SizedBox(width: inset),
        Expanded(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              for (final label in dayAxisLabelsFor(viewport))
                Text(
                  label,
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
            ],
          ),
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

/// The five evenly-spaced labels under a day chart, for the slice of the day on show.
///
/// Evenly spaced across the PLOT, so each one says what time it is at that point — which
/// at full zoom is `00:00 / 06:00 / 12:00 / 18:00 / 24:00`, exactly as it always was, and
/// zoomed in is the hours actually under the plot.
List<String> dayAxisLabelsFor(ChartViewport viewport) => [
      for (var tick = 0; tick <= 4; tick++)
        _hhmm(viewport.dataFraction(tick / 4) * Duration.minutesPerDay),
    ];

String _hhmm(double minutesIntoDay) {
  final total = minutesIntoDay.round().clamp(0, Duration.minutesPerDay);
  final hours = total ~/ 60;
  final minutes = total % 60;
  return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}';
}
