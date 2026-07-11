import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/sleep_models.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/components/loading_state.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'sleep_cards.dart';
import 'sleep_detail_notifier.dart';
import '../../ui/components/section_padding.dart';

/// Single sleep-session detail pushed over the shell (`/sleep_detail/:sleepId`).
/// Port of the Kotlin `SleepDetailScreen` + the detail cards in `SleepCards.kt`:
/// a plain list of summary card, stage-breakdown card, session-details card and
/// per-stage event rows.
class SleepDetailScreen extends ConsumerStatefulWidget {
  const SleepDetailScreen({super.key, required this.sleepId});

  final String sleepId;

  @override
  ConsumerState<SleepDetailScreen> createState() => _SleepDetailScreenState();
}

class _SleepDetailScreenState extends ConsumerState<SleepDetailScreen> {
  late final NotifierProvider<SleepDetailNotifier, SleepDetailState> _provider =
      NotifierProvider.autoDispose<SleepDetailNotifier, SleepDetailState>(
    () => SleepDetailNotifier(widget.sleepId),
  );

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(_provider);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.screenSleepDetail)),
      body: _body(context, l10n, state, formatter),
    );
  }

  Widget _body(
    BuildContext context,
    AppLocalizations l10n,
    SleepDetailState state,
    UnitFormatter formatter,
  ) {
    if (state.isLoading) return const FullScreenLoading();
    final error = state.error;
    if (error != null) return ErrorMessage(_errorText(l10n, error));
    final session = state.session;
    if (session == null) return ErrorMessage(l10n.unknownError);

    final stages = [...session.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: SleepSummaryCard(session: session, formatter: formatter),
        ),
        sectionPadded(SleepStageBreakdownCard(session: session, formatter: formatter)),
        sectionPadded(SleepSessionDetailsCard(session: session)),
        if (stages.isNotEmpty) ...[
          sectionPadded(
            SleepSectionCard(
              title: l10n.detailStageEvents,
              child: Text(
                l10n.summaryRecordedStages(formatter.count(stages.length)),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
              ),
            ),
          ),
          for (final stage in stages)
            sectionPadded(SleepStageEventRow(stage: stage, formatter: formatter)),
        ],
        const SizedBox(height: 16),
      ],
    );
  }
}


String _errorText(AppLocalizations l10n, ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => l10n.unknownError,
      ScreenErrorMissingArgument() => l10n.unknownError,
      ScreenErrorPermissionDenied() => l10n.unknownError,
      ScreenErrorHealthConnectUnavailable() => l10n.unknownError,
    };

/// The per-type stage label, port of the Kotlin `sleepStageLabel` (unlike the
/// grouped labels in `sleep_cards.dart`, each Health Connect type keeps its own
/// name here — "Awake in bed" and "Sleeping" stay distinct).
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

// ── Summary card ─────────────────────────────────────────────────────────────

/// Port of the Kotlin `SleepSummaryCard`: bed icon + title, end date, source
/// chip, then the duration headline and the full start-end range.
class SleepSummaryCard extends StatelessWidget {
  const SleepSummaryCard({
    super.key,
    required this.session,
    required this.formatter,
  });

  final SleepData session;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();
    final title = session.title?.trim().isNotEmpty == true
        ? session.title!
        : l10n.detailSleepSession;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.bed_outlined, color: AppColors.sleep),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              title,
                              style: theme.textTheme.titleLarge
                                  ?.copyWith(fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ),
                      Text(
                        DateFormat.yMMMd(locale)
                            .format(session.endTime.toLocal()),
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                SourceChip(source: session.source),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              formatter.duration(session.durationMs),
              style: theme.textTheme.headlineMedium
                  ?.copyWith(color: AppColors.sleep),
            ),
            Text(
              '${_mediumDateTime(locale, session.startTime)} - '
              '${_mediumDateTime(locale, session.endTime)}',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Stage breakdown card ─────────────────────────────────────────────────────

/// Port of the Kotlin `SleepStageBreakdownCard`: the per-lane stage timeline
/// chart plus the per-stage totals (duration · share).
class SleepStageBreakdownCard extends StatelessWidget {
  const SleepStageBreakdownCard({
    super.key,
    required this.session,
    required this.formatter,
  });

  final SleepData session;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    if (session.stages.isEmpty) {
      return SleepSectionCard(
        title: l10n.detailStages,
        child: Text(
          l10n.messageNoStages,
          style: theme.textTheme.bodyMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      );
    }

    final orderedStages = [...session.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    final stageTotalMs = orderedStages.fold<int>(
      0,
      (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
    );
    final totals = _stageTotals(orderedStages);

    return SleepSectionCard(
      title: l10n.detailStages,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SleepStagesLaneChart(
            stages: orderedStages,
            formatter: formatter,
            timelineStart: session.startTime,
            timelineEnd: session.endTime,
          ),
          const SizedBox(height: 12),
          for (final total in totals)
            _DetailRow(
              label: localizedSleepStageLabel(l10n, total.$1),
              value: '${formatter.duration(total.$2)} · '
                  '${formatter.decimal(stageTotalMs > 0 ? total.$2 * 100.0 / stageTotalMs : 0.0, 0)}%',
            ),
        ],
      ),
    );
  }
}

List<(int, int)> _stageTotals(List<SleepStage> stages) {
  final totals = <int, int>{};
  for (final stage in stages) {
    totals[stage.stageType] = (totals[stage.stageType] ?? 0) + stage.durationMs;
  }
  final entries = totals.entries.map((e) => (e.key, e.value)).toList()
    ..sort((a, b) => b.$2.compareTo(a.$2));
  return entries;
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
  });

  final List<SleepStage> stages;
  final UnitFormatter formatter;
  final DateTime timelineStart;
  final DateTime timelineEnd;

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
                        '${localizedSleepStageLabel(l10n, lane.labelStageType)} - '
                        '${formatter.duration(laneDurationMs(lane))}',
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
/// metadata as label/value rows.
class SleepSessionDetailsCard extends StatelessWidget {
  const SleepSessionDetailsCard({super.key, required this.session});

  final SleepData session;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();
    final device = session.device;
    final notAvailable = l10n.notAvailable;

    return SleepSectionCard(
      title: l10n.detailSessionDetails,
      child: Column(
        children: [
          _DetailRow(
            label: l10n.detailStarted,
            value: _mediumDateTime(locale, session.startTime),
          ),
          _DetailRow(
            label: l10n.detailEnded,
            value: _mediumDateTime(locale, session.endTime),
          ),
          _DetailRow(
            label: l10n.detailStartZone,
            value: _zoneOffsetId(session.startZoneOffset) ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailEndZone,
            value: _zoneOffsetId(session.endZoneOffset) ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailRecording,
            value: _recordingMethodLabel(l10n, session.recordingMethod),
          ),
          _DetailRow(label: l10n.detailSourcePackage, value: session.source),
          _DetailRow(
            label: l10n.detailDeviceType,
            value: _deviceTypeLabel(l10n, device?.type),
          ),
          _DetailRow(
            label: l10n.detailDeviceMaker,
            value: device?.manufacturer ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailDeviceModel,
            value: device?.model ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailLastModified,
            value: session.lastModifiedTime != null
                ? _mediumDateTime(locale, session.lastModifiedTime!)
                : notAvailable,
          ),
          _DetailRow(label: l10n.detailRecordId, value: session.id),
          _DetailRow(
            label: l10n.detailClientRecordId,
            value: session.clientRecordId ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailClientVersion,
            value: session.clientRecordVersion?.toString() ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailTitle,
            value: session.title?.trim().isNotEmpty == true
                ? session.title!
                : notAvailable,
          ),
          _DetailRow(
            label: l10n.detailNotes,
            value: session.notes?.trim().isNotEmpty == true
                ? session.notes!
                : notAvailable,
          ),
        ],
      ),
    );
  }
}

// ── Stage event row ──────────────────────────────────────────────────────────

/// Port of the Kotlin `SleepStageEventRow`: one recorded stage with its time
/// range and duration.
class SleepStageEventRow extends StatelessWidget {
  const SleepStageEventRow({
    super.key,
    required this.stage,
    required this.formatter,
  });

  final SleepStage stage;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              localizedSleepStageLabel(l10n, stage.stageType),
              style: theme.textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            _DetailRow(
              label: l10n.detailTime,
              value: _timeRange(locale, stage.startTime, stage.endTime),
            ),
            _DetailRow(
              label: l10n.detailDuration,
              value: formatter.duration(stage.durationMs),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Shared bits ──────────────────────────────────────────────────────────────

/// A label/value line, port of the Kotlin `DetailRow`.
class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: theme.textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}

String _mediumDateTime(String locale, DateTime value) =>
    DateFormat.yMMMd(locale).add_jm().format(value.toLocal());

/// Kotlin `formatTimeRange`: times only inside one day, full date-times across
/// midnight.
String _timeRange(String locale, DateTime start, DateTime end) {
  final localStart = start.toLocal();
  final localEnd = end.toLocal();
  final sameDay = localStart.year == localEnd.year &&
      localStart.month == localEnd.month &&
      localStart.day == localEnd.day;
  if (sameDay) {
    final time = DateFormat.jm(locale);
    return '${time.format(localStart)} - ${time.format(localEnd)}';
  }
  return '${_mediumDateTime(locale, start)} - ${_mediumDateTime(locale, end)}';
}

/// The `ZoneOffset.id`-style rendering of a stored zone offset (`Z`, `+02:00`).
String? _zoneOffsetId(Duration? offset) {
  if (offset == null) return null;
  if (offset == Duration.zero) return 'Z';
  final sign = offset.isNegative ? '-' : '+';
  final absolute = offset.abs();
  final hours = absolute.inHours.toString().padLeft(2, '0');
  final minutes = (absolute.inMinutes % 60).toString().padLeft(2, '0');
  return '$sign$hours:$minutes';
}

// Health Connect `Metadata.RECORDING_METHOD_*` constants.
const int _recordingMethodUnknown = 0;
const int _recordingMethodActivelyRecorded = 1;
const int _recordingMethodAutomaticallyRecorded = 2;
const int _recordingMethodManualEntry = 3;

String _recordingMethodLabel(AppLocalizations l10n, int? method) {
  switch (method) {
    case _recordingMethodActivelyRecorded:
      return l10n.recordingActivelyRecorded;
    case _recordingMethodAutomaticallyRecorded:
      return l10n.recordingAutomaticallyRecorded;
    case _recordingMethodManualEntry:
      return l10n.recordingManualEntry;
    case _recordingMethodUnknown:
      return l10n.recordingUnknown;
    default:
      return l10n.notAvailable;
  }
}

// Health Connect `Device.TYPE_*` constants.
const int _deviceTypeUnknown = 0;
const int _deviceTypeWatch = 1;
const int _deviceTypePhone = 2;
const int _deviceTypeScale = 3;
const int _deviceTypeRing = 4;
const int _deviceTypeHeadMounted = 5;
const int _deviceTypeFitnessBand = 6;
const int _deviceTypeChestStrap = 7;
const int _deviceTypeSmartDisplay = 8;

String _deviceTypeLabel(AppLocalizations l10n, int? type) {
  switch (type) {
    case _deviceTypeWatch:
      return l10n.deviceWatch;
    case _deviceTypePhone:
      return l10n.devicePhone;
    case _deviceTypeScale:
      return l10n.deviceScale;
    case _deviceTypeRing:
      return l10n.deviceRing;
    case _deviceTypeHeadMounted:
      return l10n.deviceHeadMounted;
    case _deviceTypeFitnessBand:
      return l10n.deviceFitnessBand;
    case _deviceTypeChestStrap:
      return l10n.deviceChestStrap;
    case _deviceTypeSmartDisplay:
      return l10n.deviceSmartDisplay;
    case _deviceTypeUnknown:
      return l10n.recordingUnknown;
    default:
      return l10n.notAvailable;
  }
}
