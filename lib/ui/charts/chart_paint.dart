import 'package:flutter/material.dart';

import '../theme/chart_tokens.dart';

/// The paint idioms a chart reaches for, written once.
///
/// Each of these existed two or three times, in painters that could not see each
/// other — the dashed reference line twice (at different dash lengths, because
/// nobody could compare them), the line paint everywhere.

/// A dashed line from [from] to [to].
///
/// Used for a reference the data is measured AGAINST rather than data itself: the
/// caffeine sleep threshold, the sleep-schedule average markers. Dashed, because a
/// solid line of the same weight would read as another series.
///
/// [dash] and [gap] are parameters and not constants only because the two callers
/// disagreed (8/6 and 6/6) and this commit is not allowed to change how anything
/// looks. One of them is wrong; a later one picks.
void drawDashedLine(
  Canvas canvas,
  Offset from,
  Offset to,
  Paint paint, {
  double dash = 8,
  double gap = 6,
}) {
  final delta = to - from;
  final length = delta.distance;
  if (length <= 0 || dash <= 0) return;
  final step = dash + gap;
  final unit = delta / length;
  for (var travelled = 0.0; travelled < length; travelled += step) {
    final end = (travelled + dash).clamp(0.0, length);
    canvas.drawLine(from + unit * travelled, from + unit * end, paint);
  }
}

/// The stroke a data line is drawn with.
Paint chartLinePaint(Color accent, {double strokeWidth = kChartLineStroke}) =>
    Paint()
      ..color = accent
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

/// The wash under a data line.
///
/// A flat alpha today, and a single place for Phase B to make it a gradient — at
/// which point every line chart in the app gains one, and none of them has to
/// know.
Paint chartFillPaint(Color accent, ChartTokens tokens) =>
    Paint()..color = tokens.areaFill(accent);
