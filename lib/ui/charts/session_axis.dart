import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/presentation/elapsed_format.dart';
import 'chart_axis.dart';
import 'chart_viewport.dart';

/// Where a moment sits within one recorded session, and the axis that says so.
///
/// [DayAxis]'s counterpart, for a chart whose x axis is a workout rather than a
/// day. The distinction that matters is the same one: a sample is placed against
/// the WHOLE session — its recorded start to its recorded end — not against the
/// samples that happen to exist. A trace that ran out of sensor data at the
/// twenty-minute mark of an hour-long ride should stop a third of the way across,
/// not stretch to the right edge and imply an hour of readings.
///
/// The heart-rate card and the speed/cadence cards each wrote this by hand:
/// `time.difference(start).inMilliseconds.clamp(0, durationMs) / durationMs`.
/// Twice is once too many — see what happened to the day charts.
@immutable
class SessionAxis {
  SessionAxis({required this.start, required this.end})
      // A zero-length session would divide by zero in the painter. Sessions of
      // zero duration exist: a recording stopped the instant it started.
      : durationMs = math.max(end.difference(start).inMilliseconds, 1);

  final DateTime start;
  final DateTime end;
  final int durationMs;

  Duration get duration => Duration(milliseconds: durationMs);

  /// Where [time] sits across the session, in `0..1`.
  double fractionOf(DateTime time) =>
      time.difference(start).inMilliseconds.clamp(0, durationMs) / durationMs;

  /// The inverse of [fractionOf]: how far into the session that x was. The
  /// scrubber needs it — a finger lands on an x, and the chart has to say when
  /// that was.
  Duration elapsedAt(double fraction) =>
      Duration(milliseconds: (fraction.clamp(0.0, 1.0) * durationMs).round());

  /// Elapsed labels at the quarters: `0:00 … 15:00 … 30:00 … 45:00 … 1:00:00`.
  /// Kotlin `sessionElapsedLabels`.
  ///
  /// Computed from the slice of the session ON SHOW, which at full zoom is the whole of
  /// it and gives back exactly the five it always did. A row that still read `0:00 …
  /// 1:00:00` under a plot showing the last ten minutes would be describing a chart that
  /// is not there -- which is the bug [DayAxisLabels] exists to have killed once already.
  List<String> elapsedLabelsFor([ChartViewport viewport = ChartViewport.full]) => [
        for (var tick = 0; tick <= 4; tick++)
          formatRecordingElapsed(
            Duration(
              milliseconds:
                  (viewport.dataFraction(tick / 4) * durationMs).round(),
            ),
          ),
      ];

  List<String> get elapsedLabels => elapsedLabelsFor();

  @override
  bool operator ==(Object other) =>
      other is SessionAxis && other.start == start && other.end == end;

  @override
  int get hashCode => Object.hash(start, end);
}

/// The elapsed-time label row under a session chart.
///
/// Carries the plot's left inset itself, for the same reason [DayAxisLabels] does:
/// a row that starts at the card's edge does not describe a plot that starts 64px
/// in.
class SessionAxisLabels extends StatelessWidget {
  const SessionAxisLabels({
    super.key,
    required this.axis,
    this.inset = kChartPlotInset,
    this.viewport = ChartViewport.full,
  });

  final SessionAxis axis;
  final double inset;

  /// The slice of the session on show, when the chart above has been pinched.
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
              for (final label in axis.elapsedLabelsFor(viewport))
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
