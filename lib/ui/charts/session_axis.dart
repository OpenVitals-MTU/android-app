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
  SessionAxis({
    required this.start,
    required this.end,
    List<SessionPause> pauses = const <SessionPause>[],
  })  : pauses = _normalizePauses(pauses, start, end),
        // A zero-length session would divide by zero in the painter. Sessions of
        // zero duration exist: a recording stopped the instant it started.
        durationMs = math.max(
          end.difference(start).inMilliseconds -
              _pausedMillis(_normalizePauses(pauses, start, end)),
          1,
        );

  final DateTime start;
  final DateTime end;

  /// The stretches the recording was paused for, clipped to the session,
  /// ordered and merged. The axis SKIPS these.
  final List<SessionPause> pauses;

  /// MOVING milliseconds — the session's span less what it was paused for, and
  /// the full width of the axis.
  ///
  /// A pause is not part of the ride, so it gets none of the chart. A 21-minute
  /// bike ride with a 10½-minute pause in it spent more than half the width of
  /// every card on a stretch where nothing was recorded, and the elevation trace
  /// drew a smooth spline across the hole — a climb from -29 m to -10 m that
  /// never happened, because a line joins the fixes either side of a gap
  /// whatever sits between them. Collapsing the pause puts those two fixes next
  /// to each other, where they belong.
  final int durationMs;

  Duration get duration => Duration(milliseconds: durationMs);

  /// Where [time] sits across the session, in `0..1`, counting only moving time.
  double fractionOf(DateTime time) => _movingMillisAt(time) / durationMs;

  /// Moving milliseconds from the start of the session up to [time].
  ///
  /// An instant INSIDE a pause resolves to the moment the pause began: nothing
  /// moved while it ran, so everything recorded during it belongs at the one
  /// point on the axis. (Heart rate keeps sampling through a pause — a strap
  /// does not stop because the ride did — and those samples stack there rather
  /// than stretching the pause back open.)
  int _movingMillisAt(DateTime time) {
    final at = time.millisecondsSinceEpoch;
    final from = start.millisecondsSinceEpoch;
    if (at <= from) return 0;
    var moving = at - from;
    for (final pause in pauses) {
      final pauseStart = pause.start.millisecondsSinceEpoch;
      if (at <= pauseStart) break;
      final pauseEnd = pause.end.millisecondsSinceEpoch;
      moving -= math.min(at, pauseEnd) - pauseStart;
    }
    return moving.clamp(0, durationMs);
  }

  /// The inverse of [fractionOf]: how far into the session that x was. The
  /// scrubber needs it — a finger lands on an x, and the chart has to say when
  /// that was.
  ///
  /// Moving elapsed, matching the axis and the "Moving time" the detail screen
  /// states. Reporting wall-clock here would have the scrubber disagree with the
  /// labels directly under it.
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
      other is SessionAxis &&
      other.start == start &&
      other.end == end &&
      _samePauses(other.pauses, pauses);

  @override
  int get hashCode => Object.hash(start, end, Object.hashAll(pauses));
}

/// One stretch of a session the recording was paused for.
@immutable
class SessionPause {
  const SessionPause(this.start, this.end);

  final DateTime start;
  final DateTime end;

  @override
  bool operator ==(Object other) =>
      other is SessionPause && other.start == start && other.end == end;

  @override
  int get hashCode => Object.hash(start, end);
}

bool _samePauses(List<SessionPause> a, List<SessionPause> b) {
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}

/// [pauses] clipped to `[start, end]`, ordered, and merged where they overlap.
///
/// Merged because the arithmetic below subtracts each in turn: two overlapping
/// pause segments — which a source app is free to write — would otherwise have
/// their shared stretch taken out twice and the axis would run past its own end.
List<SessionPause> _normalizePauses(
  List<SessionPause> pauses,
  DateTime start,
  DateTime end,
) {
  final from = start.millisecondsSinceEpoch;
  final to = end.millisecondsSinceEpoch;
  final clipped = <SessionPause>[];
  for (final pause in pauses) {
    final pauseStart = math.max(from, pause.start.millisecondsSinceEpoch);
    final pauseEnd = math.min(to, pause.end.millisecondsSinceEpoch);
    if (pauseEnd > pauseStart) {
      clipped.add(SessionPause(
        DateTime.fromMillisecondsSinceEpoch(pauseStart, isUtc: true),
        DateTime.fromMillisecondsSinceEpoch(pauseEnd, isUtc: true),
      ));
    }
  }
  if (clipped.length < 2) return List.unmodifiable(clipped);
  clipped.sort((a, b) => a.start.compareTo(b.start));
  final merged = <SessionPause>[clipped.first];
  for (final pause in clipped.skip(1)) {
    final last = merged.last;
    if (!pause.start.isAfter(last.end)) {
      if (pause.end.isAfter(last.end)) {
        merged[merged.length - 1] = SessionPause(last.start, pause.end);
      }
    } else {
      merged.add(pause);
    }
  }
  return List.unmodifiable(merged);
}

int _pausedMillis(List<SessionPause> pauses) {
  var total = 0;
  for (final pause in pauses) {
    total += pause.end.difference(pause.start).inMilliseconds;
  }
  return total;
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
