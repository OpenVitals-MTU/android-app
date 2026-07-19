import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
// `show DateFormat`: intl also exports a `TextDirection` that would shadow the
// dart:ui one this file uses for its label painter.
import 'package:intl/intl.dart' show DateFormat;

import '../../../core/presentation/unit_formatter.dart';
import '../../../ui/theme/chart_colors.dart';
import '../../../ui/theme/chart_tokens.dart';
import '../../../ui/charts/time_axis.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../l10n/app_localizations.dart';

/// How a sleep stage is drawn and named, and the hypnogram that uses both.
///
/// Its own file because BOTH the day card (`sleep_cards.dart`) and the sleep
/// detail screen draw the same chart, and each owned half of what it needs — the
/// colour lived beside the cards, the label beside the detail screen. Sharing the
/// chart between them without this would have been a circular import. (The stage
/// COLOURS have since moved to `ui/theme/chart_colors.dart`, where the rest of
/// the app's data colours live; this file re-exports nothing and simply imports
/// them like everyone else.)

/// The per-type stage label, port of the Kotlin `sleepStageLabel` (unlike the
/// grouped lane labels below, each Health Connect type keeps its own name here —
/// "Awake in bed" and "Sleeping" stay distinct).
String localizedSleepStageLabel(AppLocalizations l10n, int stageType) {
  switch (stageType) {
    case SleepStage.stageAwake:
      return l10n.sleepStageAwake;
    case SleepStage.stageSleeping:
      return l10n.sleepStageSleeping;
    case SleepStage.stageOutOfBed:
      return l10n.sleepStageOutOfBed;
    case SleepStage.stageLight:
      return l10n.sleepStageLight;
    case SleepStage.stageDeep:
      return l10n.sleepStageDeep;
    case SleepStage.stageRem:
      return l10n.sleepStageRem;
    case SleepStage.stageAwakeInBed:
      return l10n.sleepStageAwakeInBed;
    default:
      return l10n.sleepStageUnknown;
  }
}

// Kotlin `SleepStagesLaneChart` geometry (all in logical pixels / dp). The lane
// sizes are tokens now — the chart is still the only thing that draws lanes, but
// a number nobody else can see is a number nobody else can keep in step.
//
// [_labelHeight] and [_laneHeight] are the BASE (text scale 1.0) sizes; the band
// under the label — the track and its air — is [_trackBandHeight], and it is the
// part that stays fixed. When the user runs a larger system font the label text
// grows past its base box, so the lane grows with it (see `build`), keeping the
// track a constant distance below the label instead of letting the label overrun
// the track and draw the hypnogram on top of its own axis.
const double _laneHeight = kSleepLaneHeight;
const double _labelHeight = kSleepLaneLabelHeight;
const double _trackBandHeight = _laneHeight - _labelHeight;
const double _trackCenterOffset = 18;
const double _trackHeight = kSleepLaneTrackHeight;
const double _transitionStroke = kChartTraceStroke;

/// Port of the Kotlin `SleepStagesLaneChart`: one horizontal lane per stage
/// group (Awake / REM / Light / Deep plus any extra type present), each lane
/// showing its label with the lane total and its stage segments positioned on
/// the session timeline, then a start / midpoint / end time axis.
///
/// The stage segments are drawn as a single [Path] spanning every lane, so
/// consecutive segments are joined by a diagonal connector across lanes and the
/// whole shape is filled and stroked with a vertical cross-lane gradient — the
/// Kotlin `sleepPath` + `Brush.verticalGradient`. Flutter has no `PathEffect`,
/// so Kotlin's `cornerPathEffect` on the stroke is approximated with a round
/// stroke join (the rounded segment corners themselves come from `addRRect`).
class SleepStagesLaneChart extends StatelessWidget {
  const SleepStagesLaneChart({
    super.key,
    required this.stages,
    required this.formatter,
    required this.timelineStart,
    required this.timelineEnd,
    this.showInlineLabels = true,
  });

  final List<SleepStage> stages;
  final UnitFormatter formatter;
  final DateTime timelineStart;
  final DateTime timelineEnd;

  /// Whether a lane's label carries its total ("REM - 1h 59m") or just its name.
  ///
  /// The detail screen wants the totals; the day card does not, because it lists
  /// the same totals underneath. Kotlin's `showInlineLabels`.
  final bool showInlineLabels;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final orderedStages = stages.where((s) => s.durationMs > 0).toList()
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    if (orderedStages.isEmpty) return const SizedBox.shrink();

    final totalMs =
        timelineEnd.difference(timelineStart).inMilliseconds;
    if (totalMs <= 0) return const SizedBox.shrink();

    final lanes = _sleepStageLanes(orderedStages);
    final trackColor = sleepLaneTrackColor(context);

    // The label sits above the track in a band of its own; at a large system font
    // the label text is taller than its base box, so grow the band to fit it and
    // the whole lane with it. Otherwise the label overflows downward onto the
    // track and the graph reads as drawn on top of its labels.
    final labelStyle = theme.textTheme.titleSmall;
    final labelPainter = TextPainter(
      text: TextSpan(text: 'Ag', style: labelStyle),
      textDirection: TextDirection.ltr,
      textScaler: MediaQuery.textScalerOf(context),
    )..layout();
    final labelHeight = math.max(_labelHeight, labelPainter.height);
    final laneHeight = labelHeight + _trackBandHeight;

    int laneDurationMs(_SleepStageLane lane) => orderedStages
        .where((stage) => lane.stageTypes.contains(stage.stageType))
        .fold<int>(
          0,
          (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
        );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SleepStageScrubber(
          stages: orderedStages,
          timelineStart: timelineStart,
          timelineEnd: timelineEnd,
          child: SizedBox(
            width: double.infinity,
            height: laneHeight * lanes.length,
            child: Stack(
              children: [
                Positioned.fill(
                  child: CustomPaint(
                    painter: _LaneChartPainter(
                      stages: orderedStages,
                      lanes: lanes,
                      timelineStart: timelineStart,
                      timelineEnd: timelineEnd,
                      trackColor: trackColor,
                      labelHeight: labelHeight,
                      laneHeight: laneHeight,
                    ),
                  ),
                ),
                // The per-lane labels sit above each track, matching the Kotlin
                // overlay Column (label height then the remaining lane band). Each
                // gets a scrim in the card's own colour so the hypnogram's segments
                // and connectors are cut out behind the text rather than crossing
                // through it — the labels ride over a busy plot at the left edge.
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    for (final lane in lanes) ...[
                      SizedBox(
                        height: labelHeight,
                        width: double.infinity,
                        child: Align(
                          alignment: Alignment.centerLeft,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: theme.colorScheme.surfaceContainer,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 4, vertical: 1),
                              child: Text(
                                showInlineLabels
                                    ? '${localizedSleepStageLabel(l10n, lane.labelStageType)} - '
                                        '${formatter.duration(laneDurationMs(lane))}'
                                    : localizedSleepStageLabel(
                                        l10n, lane.labelStageType),
                                style: labelStyle,
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: _trackBandHeight),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        TimeAxisLabels(start: timelineStart, end: timelineEnd),
      ],
    );
  }
}

/// Drag across the hypnogram to read the clock time — and the stage you were in —
/// at any moment of the night. The sleep analogue of [ChartScrubber], and it
/// reads CONTINUOUS time rather than snapping to a sample: a stage is a span, not
/// a point, so a crosshair that jumped to a segment's midpoint would detach from
/// a finger tracing a 40-minute stretch of REM. The time under the finger is what
/// a night's chart is asked, exactly as [TimeAxisLabels] answers it at the edges.
///
/// Horizontal-only drag, like [ChartScrubber]: a vertical drag is left to the
/// scrolling screen this chart sits inside, or the page would freeze under the
/// thumb. See that widget for the gesture-arena reasoning.
class SleepStageScrubber extends StatefulWidget {
  const SleepStageScrubber({
    super.key,
    required this.stages,
    required this.timelineStart,
    required this.timelineEnd,
    required this.child,
    this.enabled = true,
  });

  /// The stages on show, in start order — the same list the hypnogram draws.
  final List<SleepStage> stages;
  final DateTime timelineStart;
  final DateTime timelineEnd;

  /// The hypnogram, unchanged. The scrubber draws over it, never inside it.
  final Widget child;
  final bool enabled;

  @override
  State<SleepStageScrubber> createState() => _SleepStageScrubberState();
}

class _SleepStageScrubberState extends State<SleepStageScrubber> {
  /// Where the finger is, 0..1 across the plot, or null when it lifts.
  double? _fraction;

  /// The stage type last under the finger, so a haptic fires on each crossing
  /// rather than every pixel.
  int? _stageType;

  int get _totalMs =>
      widget.timelineEnd.difference(widget.timelineStart).inMilliseconds;

  DateTime _timeAt(double fraction) => widget.timelineStart
      .add(Duration(milliseconds: (fraction * _totalMs).round()));

  /// The stage covering [time], or null in a gap between segments.
  SleepStage? _stageAt(DateTime time) {
    for (final stage in widget.stages) {
      if (stage.durationMs <= 0) continue;
      if (!time.isBefore(stage.startTime) && time.isBefore(stage.endTime)) {
        return stage;
      }
    }
    return null;
  }

  void _scrub(double dx, double width) {
    if (width <= 0 || _totalMs <= 0) return;
    final fraction = (dx / width).clamp(0.0, 1.0);
    final type = _stageAt(_timeAt(fraction))?.stageType;
    if (type != _stageType) HapticFeedback.selectionClick();
    setState(() {
      _fraction = fraction;
      _stageType = type;
    });
  }

  void _lift() {
    if (_fraction == null) return;
    setState(() {
      _fraction = null;
      _stageType = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled || widget.stages.isEmpty || _totalMs <= 0) {
      return widget.child;
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          // HORIZONTAL only — a vertical drag stays with the scrolling screen.
          onHorizontalDragStart: (d) => _scrub(d.localPosition.dx, width),
          onHorizontalDragUpdate: (d) => _scrub(d.localPosition.dx, width),
          onHorizontalDragEnd: (_) => _lift(),
          onHorizontalDragCancel: _lift,
          child: Stack(
            children: [
              widget.child,
              if (_fraction case final fraction?) ...[
                Positioned.fill(
                  child: IgnorePointer(
                    child: CustomPaint(
                      painter: _SleepScrubCrosshairPainter(
                        fraction: fraction,
                        color: ChartTokens.read(context).crosshair,
                      ),
                    ),
                  ),
                ),
                Positioned.fill(
                  child: IgnorePointer(
                    child: _SleepScrubTooltip(
                      fraction: fraction,
                      time: _timeAt(fraction),
                      stage: _stageAt(_timeAt(fraction)),
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}

/// A single vertical line at the finger — this moment of the night.
class _SleepScrubCrosshairPainter extends CustomPainter {
  const _SleepScrubCrosshairPainter({
    required this.fraction,
    required this.color,
  });

  final double fraction;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final x = fraction.clamp(0.0, 1.0) * size.width;
    canvas.drawLine(
      Offset(x, 0),
      Offset(x, size.height),
      Paint()
        ..color = color
        ..strokeWidth = 1,
    );
  }

  @override
  bool shouldRepaint(_SleepScrubCrosshairPainter old) =>
      old.fraction != fraction || old.color != color;
}

/// The clock time under the finger, and the stage active then, floated above the
/// crosshair and kept inside the plot.
class _SleepScrubTooltip extends StatelessWidget {
  const _SleepScrubTooltip({
    required this.fraction,
    required this.time,
    required this.stage,
  });

  final double fraction;
  final DateTime time;
  final SleepStage? stage;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final tokens = ChartTokens.read(context);
    final locale = Localizations.localeOf(context).toString();
    final timeText = DateFormat.jm(locale).format(time.toLocal());
    final stageText =
        stage == null ? null : localizedSleepStageLabel(l10n, stage!.stageType);

    return LayoutBuilder(
      builder: (context, constraints) {
        const width = 132.0;
        final x = fraction.clamp(0.0, 1.0) * constraints.maxWidth;
        // Clamped to the plot: the first and last moments of the night are exactly
        // the ones a finger reaches for, and a tooltip hanging off the card cannot
        // be read.
        final left = (x - width / 2).clamp(0.0, constraints.maxWidth - width);

        return Stack(
          children: [
            Positioned(
              left: left,
              top: 0,
              width: width,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: tokens.tooltipSurface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        timeText,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.labelLarge?.copyWith(
                          color: tokens.onTooltipSurface,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (stageText != null)
                        Text(
                          stageText,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelSmall?.copyWith(
                            color:
                                tokens.onTooltipSurface.withValues(alpha: 0.75),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _SleepStageLane {
  const _SleepStageLane({
    required this.stageTypes,
    required this.labelStageType,
  });

  final Set<int> stageTypes;
  final int labelStageType;
}

const List<_SleepStageLane> _standardSleepStageLanes = [
  _SleepStageLane(
    stageTypes: {
      SleepStage.stageAwake,
      SleepStage.stageAwakeInBed,
      SleepStage.stageOutOfBed,
    },
    labelStageType: SleepStage.stageAwake,
  ),
  _SleepStageLane(
    stageTypes: {SleepStage.stageRem},
    labelStageType: SleepStage.stageRem,
  ),
  _SleepStageLane(
    stageTypes: {SleepStage.stageLight, SleepStage.stageSleeping},
    labelStageType: SleepStage.stageLight,
  ),
  _SleepStageLane(
    stageTypes: {SleepStage.stageDeep},
    labelStageType: SleepStage.stageDeep,
  ),
];

List<_SleepStageLane> _sleepStageLanes(List<SleepStage> stages) {
  final knownTypes = <int>{
    for (final lane in _standardSleepStageLanes) ...lane.stageTypes,
  };
  final extraLanes = <_SleepStageLane>[];
  final seenExtra = <int>{};
  for (final stage in stages) {
    final type = stage.stageType;
    if (!knownTypes.contains(type) && seenExtra.add(type)) {
      extraLanes.add(
        _SleepStageLane(stageTypes: {type}, labelStageType: type),
      );
    }
  }
  return [..._standardSleepStageLanes, ...extraLanes];
}

/// A stage clamped to the visible timeline, tagged with its lane index — the
/// Kotlin `VisibleSleepStage`.
class _VisibleStage {
  const _VisibleStage({
    required this.start,
    required this.end,
    required this.laneIndex,
  });

  final DateTime start;
  final DateTime end;
  final int laneIndex;
}

/// Draws the full multi-lane stage chart: per-lane tracks, then one gradient
/// [Path] over every stage segment with diagonal cross-lane connectors between
/// consecutive stages. Port of the Kotlin `SleepStagesLaneChart` canvas.
class _LaneChartPainter extends CustomPainter {
  const _LaneChartPainter({
    required this.stages,
    required this.lanes,
    required this.timelineStart,
    required this.timelineEnd,
    required this.trackColor,
    required this.labelHeight,
    required this.laneHeight,
  });

  final List<SleepStage> stages;
  final List<_SleepStageLane> lanes;
  final DateTime timelineStart;
  final DateTime timelineEnd;
  final Color trackColor;

  /// The label band and full lane height, grown from their base sizes to fit a
  /// larger system font. Kept in step with the label overlay in `build`.
  final double labelHeight;
  final double laneHeight;

  double _laneCenterY(int index) =>
      index * laneHeight + labelHeight + _trackCenterOffset;

  int _laneIndex(int stageType) {
    for (var i = 0; i < lanes.length; i++) {
      if (lanes[i].stageTypes.contains(stageType)) return i;
    }
    return 0;
  }

  @override
  void paint(Canvas canvas, Size size) {
    final totalMs = timelineEnd.difference(timelineStart).inMilliseconds;
    if (totalMs <= 0) return;
    const trackRadius = _trackHeight / 2;

    double timeX(DateTime value) {
      final elapsed =
          value.difference(timelineStart).inMilliseconds.clamp(0, totalMs);
      return size.width * (elapsed / totalMs);
    }

    // Lane tracks.
    final trackPaint = Paint()..color = trackColor;
    for (var i = 0; i < lanes.length; i++) {
      final centerY = _laneCenterY(i);
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(0, centerY - trackRadius, size.width, _trackHeight),
          const Radius.circular(trackRadius),
        ),
        trackPaint,
      );
    }

    // Stages clamped to the timeline, in start order, tagged with their lane.
    final visible = <_VisibleStage>[];
    for (final stage in stages) {
      final start = stage.startTime.isAfter(timelineStart)
          ? stage.startTime
          : timelineStart;
      final end =
          stage.endTime.isBefore(timelineEnd) ? stage.endTime : timelineEnd;
      if (!start.isBefore(end)) continue;
      visible.add(_VisibleStage(
        start: start,
        end: end,
        laneIndex: _laneIndex(stage.stageType),
      ));
    }
    if (visible.isEmpty) return;

    // Vertical cross-lane gradient: one colour stop per lane, at the fraction
    // of the span between the first and last lane centres.
    final gradientStartY = _laneCenterY(0);
    final lastCenter = _laneCenterY(lanes.length - 1);
    final gradientEndY = lastCenter > gradientStartY ? lastCenter : size.height;
    final List<Color> colors;
    final List<double> stops;
    if (lanes.length == 1) {
      final color = sleepStageColor(lanes.first.labelStageType);
      colors = [color, color];
      stops = const [0.0, 1.0];
    } else {
      colors = [for (final lane in lanes) sleepStageColor(lane.labelStageType)];
      stops = [
        for (var i = 0; i < lanes.length; i++)
          ((_laneCenterY(i) - gradientStartY) /
                  (gradientEndY - gradientStartY))
              .clamp(0.0, 1.0),
      ];
    }
    final shader = ui.Gradient.linear(
      Offset(0, gradientStartY),
      Offset(0, gradientEndY),
      colors,
      stops,
    );

    // One path over every segment, joined across lanes when contiguous.
    final path = Path();
    for (var i = 0; i < visible.length; i++) {
      final stage = visible[i];
      final left = timeX(stage.start);
      final right = timeX(stage.end);
      final width = right - left;
      if (width <= 0) continue;
      final centerY = _laneCenterY(stage.laneIndex);
      final previous = i > 0 ? visible[i - 1] : null;
      if (previous != null && previous.end == stage.start) {
        path.lineTo(left, centerY);
      } else {
        path.moveTo(left, centerY);
      }
      final radius = width / 2 < trackRadius ? width / 2 : trackRadius;
      path.addRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(left, centerY - trackRadius, width, _trackHeight),
          Radius.circular(radius),
        ),
      );
      path.moveTo(right, centerY);
    }

    canvas.drawPath(
      path,
      Paint()
        ..style = PaintingStyle.fill
        ..shader = shader,
    );
    // The cornerPathEffect from Kotlin has no Flutter equivalent; a round
    // stroke join approximates the softened connector corners.
    canvas.drawPath(
      path,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = _transitionStroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..shader = shader,
    );
  }

  @override
  bool shouldRepaint(_LaneChartPainter oldDelegate) =>
      oldDelegate.stages != stages ||
      oldDelegate.lanes != lanes ||
      oldDelegate.timelineStart != timelineStart ||
      oldDelegate.timelineEnd != timelineEnd ||
      oldDelegate.trackColor != trackColor ||
      oldDelegate.labelHeight != labelHeight ||
      oldDelegate.laneHeight != laneHeight;
}

// ── Session details card ─────────────────────────────────────────────────────

/// Port of the Kotlin `SleepSessionDetailsCard`: the full Health Connect record
