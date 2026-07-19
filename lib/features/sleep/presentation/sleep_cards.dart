import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../ui/charts/chart_bar_row.dart';
import '../../../ui/theme/chart_colors.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/metric_card.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/sleep_display.dart';
import 'sleep_stage_chart.dart';

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

/// The night's hypnogram, port of the Kotlin `SleepSessionTimelineCard`: the
/// duration and its source, the time range, and the stage lane chart.
///
/// The Flutter port had shrunk this to a flat proportional bar — every stage
/// squeezed edge-to-edge in reading order, telling you the SHARES a second time
/// and the SHAPE of the night not at all. Kotlin drew (and this draws) one lane
/// per stage, positioned on the real clock, so a night reads as the descent into
/// deep sleep and the climb back out that it actually was.
///
/// [onTap] opens the session's detail screen — the whole card is the target, as
/// in Kotlin, so tapping the chart itself works. It is null when the day holds
/// more than one session: [session] is then a MERGED summary with no id of its
/// own, and there is no single night to open.
class SleepSessionTimelineCard extends StatelessWidget {
  const SleepSessionTimelineCard({
    super.key,
    required this.session,
    required this.selectedDate,
    required this.formatter,
    this.timeRangeText,
    this.onTap,
  });

  final SleepData session;
  final LocalDate selectedDate;
  final UnitFormatter formatter;
  final String? timeRangeText;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    return OpenVitalsCard(
      onTap: onTap,
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
                      Text(
                        formatter.duration(session.durationMs),
                        style: theme.textTheme.headlineMedium
                            ?.copyWith(color: AppColors.sleep),
                      ),
                      Text(
                        // The headline duration is the whole session span (awake
                        // stretches included) — time in bed, matching the overview
                        // tile and the "share of time in bed" card below.
                        l10n.sleepTimeInBed,
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                SourceChip(source: session.source),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              timeRangeText ?? _sessionRangeText(locale, session),
              style: theme.textTheme.bodyMedium,
            ),
            if (sleepSessionHasReliableStages(session)) ...[
              const SizedBox(height: 16),
              SleepStagesLaneChart(
                stages: session.stages,
                formatter: formatter,
                timelineStart: session.startTime,
                timelineEnd: session.endTime,
                // The lane totals are listed again in the "share of time in bed"
                // card right below, so the lanes here carry names only.
                showInlineLabels: false,
              ),
            ] else if (session.stages.isNotEmpty) ...[
              // A device that staged only part of the night would otherwise draw
              // a near-empty hypnogram with a fragment at one edge; say so instead.
              const SizedBox(height: 12),
              Text(
                l10n.messagePartialStages,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
            if (onTap != null) ...[
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Text(
                    l10n.actionDetails,
                    style: theme.textTheme.labelLarge
                        ?.copyWith(color: theme.colorScheme.primary),
                  ),
                  Icon(
                    Icons.chevron_right,
                    size: 20,
                    color: theme.colorScheme.primary,
                  ),
                ],
              ),
            ],
          ],
        ),
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
/// `SleepStageShareCard` / `SleepStageBreakdown`. Self-hides with no stage data
/// — which is what an empty [shares] means; [sleepStageShares] weighed them.
class SleepStageShareCard extends StatelessWidget {
  const SleepStageShareCard({
    super.key,
    required this.shares,
    required this.formatter,
  });

  final List<SleepStageShare> shares;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (shares.isEmpty) return const SizedBox.shrink();

    final trackColor = theme.colorScheme.surfaceContainerHighest;
    return SleepSectionCard(
      title: 'Share of time in bed',
      child: Column(
        children: [
          for (var index = 0; index < shares.length; index++) ...[
            _StageRow(
              label: sleepStageLabel(shares[index].stageType),
              fraction: shares[index].fraction,
              color: sleepStageColor(shares[index].stageType),
              trailing: '${formatter.duration(shares[index].durationMs)} '
                  '(${shares[index].percent}%)',
              trackColor: trackColor,
            ),
            if (index < shares.length - 1) const SizedBox(height: 12),
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
    return ChartBarRow(
      layout: ChartBarRowLayout.inline,
      fraction: fraction,
      color: color,
      trackColor: trackColor,
      labelWidth: 60,
      gap: 12,
      height: 10,
      radius: 5,
      label: Text(label),
      labelStyle: theme.textTheme.bodyMedium,
      trailing: Text(trailing),
      trailingStyle: theme.textTheme.bodyMedium
          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
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
                // The value is the union of the night's session spans — the whole
                // time in bed, awake stretches included — not time asleep, which
                // is why "Sleep efficiency" sits beside it. Name it for what it is.
                title: 'Time in bed',
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

/// The card's own start-end range, used when the day view does not hand one in
/// (it does when several sessions are merged). Kotlin `singleSessionTimeRangeText`.
String _sessionRangeText(String locale, SleepData session) {
  final time = DateFormat.jm(locale);
  return '${time.format(session.startTime.toLocal())} - '
      '${time.format(session.endTime.toLocal())}';
}
