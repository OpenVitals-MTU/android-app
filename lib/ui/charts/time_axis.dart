import 'package:flutter/material.dart';

import 'chart_viewport.dart';
import 'package:intl/intl.dart';

import '../theme/chart_tokens.dart';

/// A row of CLOCK times under a chart: when it started, when it ended, and the
/// moment halfway between.
///
/// The library's other session row, [SessionAxisLabels], writes ELAPSED time —
/// "0:00, 20:00, 40:00" — which is what you want under a workout, where the
/// question is how far into it you were. Under a night's sleep the question is
/// what time it was, and "3:10 AM" answers it where "3:55" does not. So the
/// hypnogram wrote its own row, and that is fair; what is not fair is that it then
/// owned the idea, and nothing else could use it.
///
/// Two rows, each with a reason. The rule the library actually cares about is the
/// one about [inset]: an axis row that ignores the plot's left gutter describes a
/// chart that is not there.
class TimeAxisLabels extends StatelessWidget {
  const TimeAxisLabels({
    super.key,
    required this.start,
    required this.end,
    this.inset = 0,
    this.viewport = ChartViewport.full,
  });

  final DateTime start;
  final DateTime end;

  /// The slice of the span on show, when the chart above has been pinched. At full zoom
  /// the arithmetic gives back exactly the three labels it always drew.
  final ChartViewport viewport;

  /// How far the plot above starts from the card's edge. Zero for a chart that
  /// draws no y-axis label column — the hypnogram labels its lanes instead.
  final double inset;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toString();
    final timeFormat = DateFormat.jm(locale);
    final spanMs = end.difference(start).inMilliseconds;

    /// Where a label sits, in real time, for a point [fraction] across the PLOT.
    DateTime at(double fraction) => start.add(
          Duration(
            milliseconds: (viewport.dataFraction(fraction) * spanMs).round(),
          ),
        );

    return Row(
      children: [
        if (inset > 0) SizedBox(width: inset),
        Expanded(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              for (final time in [at(0), at(0.5), at(1)])
                Text(
                  timeFormat.format(time.toLocal()),
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

/// The lane track a hypnogram's stage segments ride in.
///
/// Not a token on its own — it is [ChartTokens.track] at the alpha a lane wants,
/// and it is here so the hypnogram stops naming a colour of its own.
Color sleepLaneTrackColor(BuildContext context) =>
    ChartTokens.read(context).track.withValues(alpha: 0.38);
