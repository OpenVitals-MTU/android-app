import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/presentation/elapsed_format.dart';
import 'chart_axis.dart';

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
  List<String> get elapsedLabels => [
        formatRecordingElapsed(Duration.zero),
        formatRecordingElapsed(duration ~/ 4),
        formatRecordingElapsed(duration ~/ 2),
        formatRecordingElapsed(duration * 3 ~/ 4),
        formatRecordingElapsed(duration),
      ];

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
  });

  final SessionAxis axis;
  final double inset;

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
              for (final label in axis.elapsedLabels)
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
