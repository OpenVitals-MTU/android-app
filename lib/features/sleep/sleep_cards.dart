import 'package:flutter/material.dart';

import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/sleep_models.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'sleep_presentation.dart';

/// The stage accent colours, ported from the Kotlin `stageColor(...)`.
Color sleepStageColor(int stageType) {
  switch (stageType) {
    case SleepStage.stageAwake:
      return const Color(0xFFF48FB1);
    case SleepStage.stageLight:
      return const Color(0xFF8AB4F8);
    case SleepStage.stageDeep:
      return const Color(0xFF8E63CE);
    case SleepStage.stageRem:
      return const Color(0xFFB3E5FC);
    case SleepStage.stageAwakeInBed:
      return const Color(0xFFF8A6C6);
    case SleepStage.stageSleeping:
      return const Color(0xFF7EA7F5);
    case SleepStage.stageOutOfBed:
      return const Color(0xFFEF9A9A);
    default:
      return const Color(0xFF90A4AE);
  }
}

String sleepStageLabel(int stageType) {
  switch (stageType) {
    case SleepStage.stageAwake:
    case SleepStage.stageAwakeInBed:
      return 'Awake';
    case SleepStage.stageRem:
      return 'REM';
    case SleepStage.stageLight:
    case SleepStage.stageSleeping:
      return 'Light';
    case SleepStage.stageDeep:
      return 'Deep';
    case SleepStage.stageOutOfBed:
      return 'Out of bed';
    default:
      return 'Sleep';
  }
}

/// A subdued card with a titled header, mirroring the Kotlin `DetailSectionCard`.
class SleepSectionCard extends StatelessWidget {
  const SleepSectionCard({super.key, required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            child,
          ],
        ),
      ),
    );
  }
}

/// The single-night stage timeline (hypnogram-style stacked segments), a trimmed
/// port of the Kotlin `SleepSessionTimelineCard`.
class SleepSessionTimelineCard extends StatelessWidget {
  const SleepSessionTimelineCard({
    super.key,
    required this.session,
    required this.formatter,
    this.timeRangeText,
  });

  final SleepData session;
  final UnitFormatter formatter;
  final String? timeRangeText;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final stages = [...session.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    final totalMs = stages.fold<int>(
      0,
      (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
    );

    return SleepSectionCard(
      title: 'Sleep',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          MetricValueLikeRow(
            value: formatter.duration(session.durationMs),
            label: timeRangeText,
          ),
          const SizedBox(height: 12),
          if (totalMs > 0)
            ClipRRect(
              borderRadius: const BorderRadius.all(Radius.circular(6)),
              child: SizedBox(
                height: 16,
                child: Row(
                  children: [
                    for (final stage in stages)
                      if (stage.durationMs > 0)
                        Expanded(
                          flex: stage.durationMs,
                          child: ColoredBox(
                            color: sleepStageColor(stage.stageType),
                          ),
                        ),
                  ],
                ),
              ),
            )
          else
            Text(
              'No stage data for this night.',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
        ],
      ),
    );
  }
}

/// The value + optional label row used by the timeline card.
class MetricValueLikeRow extends StatelessWidget {
  const MetricValueLikeRow({super.key, required this.value, this.label});

  final String value;
  final String? label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: theme.textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.bold,
            color: theme.colorScheme.onSurface,
          ),
        ),
        if (label != null) ...[
          const SizedBox(height: 4),
          Text(
            label!,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ],
      ],
    );
  }
}

/// "Share of time in bed" stage breakdown, port of the Kotlin
/// `SleepStageShareCard` / `SleepStageBreakdown`. Self-hides with no stage data.
class SleepStageShareCard extends StatelessWidget {
  const SleepStageShareCard({
    super.key,
    required this.summary,
    required this.formatter,
  });

  final SleepOverviewSummary summary;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final rows = <(int, int)>[
      (SleepStage.stageAwake, summary.awakeDurationMs),
      (SleepStage.stageRem, summary.remDurationMs),
      (SleepStage.stageLight, summary.coreDurationMs),
      (SleepStage.stageDeep, summary.deepDurationMs),
    ].where((row) => row.$2 > 0).toList();
    final totalMs = rows.fold<int>(0, (sum, row) => sum + row.$2);
    if (totalMs <= 0) return const SizedBox.shrink();

    final trackColor = theme.colorScheme.surfaceContainerHighest;
    return SleepSectionCard(
      title: 'Share of time in bed',
      child: Column(
        children: [
          for (final row in rows) ...[
            _StageRow(
              label: sleepStageLabel(row.$1),
              fraction: (row.$2 / totalMs).clamp(0.0, 1.0),
              color: sleepStageColor(row.$1),
              trailing:
                  '${formatter.duration(row.$2)} (${((row.$2 / totalMs) * 100).round()}%)',
              trackColor: trackColor,
            ),
            if (row != rows.last) const SizedBox(height: 12),
          ],
        ],
      ),
    );
  }
}

class _StageRow extends StatelessWidget {
  const _StageRow({
    required this.label,
    required this.fraction,
    required this.color,
    required this.trailing,
    required this.trackColor,
  });

  final String label;
  final double fraction;
  final Color color;
  final String trailing;
  final Color trackColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        SizedBox(width: 60, child: Text(label, style: theme.textTheme.bodyMedium)),
        Expanded(
          child: ClipRRect(
            borderRadius: const BorderRadius.all(Radius.circular(5)),
            child: SizedBox(
              height: 10,
              child: Stack(
                children: [
                  Positioned.fill(child: ColoredBox(color: trackColor)),
                  FractionallySizedBox(
                    widthFactor: fraction,
                    child: ColoredBox(color: color),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Text(
          trailing,
          style: theme.textTheme.bodyMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}

/// The overview top cards (sleep score + duration) plus schedule / efficiency
/// tiles, a trimmed port of the Kotlin `SleepOverviewSectionContent`.
class SleepOverviewCard extends StatelessWidget {
  const SleepOverviewCard({
    super.key,
    required this.summary,
    required this.formatter,
    required this.periodTitle,
  });

  final SleepOverviewSummary summary;
  final UnitFormatter formatter;
  final String periodTitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _OverviewTile(
                title: 'Sleep score',
                value: summary.sleepScore?.toString() ?? '--',
                subtitle: periodTitle,
                accent: AppColors.sleep,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _OverviewTile(
                title: 'Sleep duration',
                value: summary.sleepDurationMs > 0
                    ? formatter.duration(summary.sleepDurationMs)
                    : '--',
                subtitle: periodTitle,
                accent: AppColors.sleep,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _OverviewTile(
                title: 'Sleep schedule',
                value: _scheduleLabel(summary.schedule),
                subtitle: periodTitle,
                accent: AppColors.sleep,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _OverviewTile(
                title: 'Sleep efficiency',
                value: summary.sleepEfficiencyPercent != null
                    ? formatter.percent(summary.sleepEfficiencyPercent!,
                            decimals: 0).text
                    : '--',
                subtitle: periodTitle,
                accent: AppColors.sleep,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

String _scheduleLabel(SleepOverviewSchedule? schedule) {
  if (schedule == null) return '--';
  return '${_minuteLabel(schedule.startMinute)} - ${_minuteLabel(schedule.endMinute)}';
}

String _minuteLabel(int minuteOfDay) {
  final hours = (minuteOfDay ~/ 60) % 24;
  final minutes = minuteOfDay % 60;
  return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}';
}

class _OverviewTile extends StatelessWidget {
  const _OverviewTile({
    required this.title,
    required this.value,
    required this.subtitle,
    required this.accent,
  });

  final String title;
  final String value;
  final String subtitle;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.labelMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 8),
            Text(
              value,
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: accent,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

/// The total / average / longest / nights statistics, a trimmed port of the
/// Kotlin `sleepStatistics` insight-stat grid.
class SleepStatisticsCard extends StatelessWidget {
  const SleepStatisticsCard({
    super.key,
    required this.durationPoints,
    required this.formatter,
  });

  final List<SleepDurationPoint> durationPoints;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final nights = durationPoints.where((p) => p.hours > 0.0).toList();
    final totalHours = nights.fold<double>(0, (sum, p) => sum + p.hours);
    final averageHours = nights.isEmpty ? 0.0 : totalHours / nights.length;
    final longestHours =
        nights.isEmpty ? 0.0 : nights.map((p) => p.hours).reduce((a, b) => a > b ? a : b);

    String hours(double value) => formatter.duration((value * 3600000).round());

    return SleepSectionCard(
      title: 'Statistics',
      child: Column(
        children: [
          for (final row in [
            ('Total', hours(totalHours)),
            ('Daily average', hours(averageHours)),
            ('Longest sleep', hours(longestHours)),
            ('Nights logged', '${nights.length}'),
          ])
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(row.$1, style: theme.textTheme.bodyMedium),
                  Text(
                    row.$2,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: theme.colorScheme.onSurface,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
