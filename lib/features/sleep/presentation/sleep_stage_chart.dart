import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../ui/theme/chart_colors.dart';
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

// Kotlin `SleepStagesLaneChart` geometry (all in logical pixels / dp).
const double _laneHeight = 72;
const double _labelHeight = 28;
const double _trackCenterOffset = 18;
const double _trackHeight = 26;
const double _transitionStroke = 2;

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
    final locale = Localizations.localeOf(context).toString();
    final orderedStages = stages.where((s) => s.durationMs > 0).toList()
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    if (orderedStages.isEmpty) return const SizedBox.shrink();

    final totalMs =
        timelineEnd.difference(timelineStart).inMilliseconds;
    if (totalMs <= 0) return const SizedBox.shrink();

    final lanes = _sleepStageLanes(orderedStages);
    final timeFormat = DateFormat.jm(locale);
    final midpoint =
        timelineStart.add(Duration(milliseconds: totalMs ~/ 2));
    final trackColor =
        theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.38);

    int laneDurationMs(_SleepStageLane lane) => orderedStages
        .where((stage) => lane.stageTypes.contains(stage.stageType))
        .fold<int>(
          0,
          (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
        );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: double.infinity,
          height: _laneHeight * lanes.length,
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
                  ),
                ),
              ),
              // The per-lane labels sit above each track, matching the Kotlin
              // overlay Column (label height then the remaining lane band).
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (final lane in lanes) ...[
                    SizedBox(
                      height: _labelHeight,
                      width: double.infinity,
                      child: Text(
                        showInlineLabels
                            ? '${localizedSleepStageLabel(l10n, lane.labelStageType)} - '
                                '${formatter.duration(laneDurationMs(lane))}'
                            : localizedSleepStageLabel(l10n, lane.labelStageType),
                        style: theme.textTheme.titleSmall,
                      ),
                    ),
                    const SizedBox(height: _laneHeight - _labelHeight),
                  ],
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            for (final value in [timelineStart, midpoint, timelineEnd])
              Text(
                timeFormat.format(value.toLocal()),
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
          ],
        ),
      ],
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
  });

  final List<SleepStage> stages;
  final List<_SleepStageLane> lanes;
  final DateTime timelineStart;
  final DateTime timelineEnd;
  final Color trackColor;

  static double _laneCenterY(int index) =>
      index * _laneHeight + _labelHeight + _trackCenterOffset;

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
      oldDelegate.trackColor != trackColor;
}

// ── Session details card ─────────────────────────────────────────────────────

/// Port of the Kotlin `SleepSessionDetailsCard`: the full Health Connect record
