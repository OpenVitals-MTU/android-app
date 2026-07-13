import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../ui/charts/chart_bar_row.dart';
import '../../../ui/theme/chart_tokens.dart';
import '../../../ui/charts/time_axis.dart';
import '../../../ui/charts/chart_empty_state.dart';
import '../../../ui/charts/metric_line_plot.dart';
import '../../../ui/charts/chart_skeleton.dart';
import '../../../ui/components/loading_state.dart';

import '../../../l10n/app_localizations.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/ov_card.dart';
import '../application/caffeine_display.dart';
import '../application/caffeine_view_model.dart';
import '../../../ui/components/section_padding.dart';

/// The caffeine analytics screen, ported from the Kotlin `CaffeineScreen`.
///
/// A bespoke read-only analytics screen (not the Day/Week/Month/Year scaffold):
/// it renders the active-caffeine pharmacokinetic curve, the current-level and
/// bedtime-safety guidance cards, and the source/item/category/time-of-day
/// distributions for the selected analytics range — all of them precomputed by
/// the view-model into a [CaffeineDisplay]. The Kotlin setup card, entry detail
/// sheet and science/reference cards are omitted (drink logging + profile
/// editing are Phase 6).
class CaffeineScreen extends ConsumerWidget {
  const CaffeineScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(caffeineProvider);
    final notifier = ref.read(caffeineProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Caffeine')),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readNutrition},
        showInlineSyncBanner: false,
        child: RefreshIndicator(
          onRefresh: notifier.refresh,
          child: Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 920),
              child: ListView(
                padding: const EdgeInsets.symmetric(vertical: 8),
                children: _content(context, state, notifier, formatter),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

List<Widget> _content(
  BuildContext context,
  CaffeineState state,
  CaffeineViewModel notifier,
  UnitFormatter formatter,
) {
  final error = _resolveError(state.error);
  // Precomputed at load time by the view-model; empty until the first one lands.
  final display = state.display ?? const CaffeineDisplay();
  final home = display.home;
  final analytics = display.analytics;

  return [
    if (error != null) ErrorMessage(error),
    if (state.isLoading && home.insights.curvePoints.isEmpty)
      const ChartSkeleton(shape: ChartSkeletonShape.bars, height: kChartHeightPeriodBar),
    const SectionHeader('Caffeine dashboard'),
    sectionPadded(_CaffeineOverviewCard(home: home, formatter: formatter)),
    sectionPadded(CaffeineCurveCard(home: home, formatter: formatter)),
    const SectionHeader('Sleep impact'),
    sectionPadded(_CaffeineSleepImpactCard(home: home, formatter: formatter)),
    const SectionHeader('Analytics'),
    sectionPadded(
      _AnalyticsRangePicker(
        selected: state.analyticsRange,
        onSelect: notifier.selectAnalyticsRange,
      ),
    ),
    sectionPadded(
      _CaffeineAnalyticsSummaryCard(analytics: analytics, formatter: formatter),
    ),
    sectionPadded(
      CaffeineDistributionCard(
        title: 'Sources',
        bars: analytics.sourceBars,
        formatter: formatter,
      ),
    ),
    sectionPadded(
      CaffeineDistributionCard(
        title: 'Items',
        bars: analytics.itemBars,
        formatter: formatter,
      ),
    ),
    sectionPadded(
      CaffeineDistributionCard(
        title: 'Inferred categories',
        bars: analytics.categoryBars,
        formatter: formatter,
      ),
    ),
    sectionPadded(
      CaffeineTimeBucketsCard(analytics: analytics, formatter: formatter),
    ),
    const SizedBox(height: 16),
  ];
}


// ── Current level + today overview ──────────────────────────────────────────

class _CaffeineOverviewCard extends StatelessWidget {
  const _CaffeineOverviewCard({required this.home, required this.formatter});

  final CaffeineHomeDisplay home;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final insights = home.insights;
    final timeToSafe = insights.timeToThresholdMinutes;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Active caffeine now', style: theme.textTheme.titleSmall),
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                _formatMg(insights.currentMg, formatter),
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.primary,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: _CaffeineSleepStatusBanner(
                home: home,
                formatter: formatter,
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                      title: 'Consumed today',
                      value: _formatMg(insights.todayTotalMg, formatter),
                      icon: Icons.local_drink_outlined,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _MiniStat(
                      title: 'Time to safe',
                      value: timeToSafe != null
                          ? _formatDurationMinutes(timeToSafe)
                          : 'N/A',
                      icon: Icons.query_stats_outlined,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CaffeineSleepStatusBanner extends StatelessWidget {
  const _CaffeineSleepStatusBanner({
    required this.home,
    required this.formatter,
  });

  final CaffeineHomeDisplay home;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final insights = home.insights;
    final status = home.sleepImpactStatus;
    final Color color;
    final IconData icon;
    final String title;
    switch (status) {
      case CaffeineSleepImpactStatus.unlikely:
        color = scheme.primary;
        icon = Icons.check_circle_outline;
        title = 'Sleep impact unlikely';
      case CaffeineSleepImpactStatus.elevatedNow:
        color = scheme.tertiary;
        icon = Icons.query_stats_outlined;
        title = 'Elevated right now';
      case CaffeineSleepImpactStatus.mayAffectSleep:
        color = scheme.error;
        icon = Icons.warning_amber_outlined;
        title = 'May affect sleep';
    }
    final body = switch (status) {
      CaffeineSleepImpactStatus.unlikely =>
        'Active caffeine (${_formatMg(insights.currentMg, formatter)}) is below '
            'your sleep threshold (${_formatMg(insights.sleepThresholdMg.toDouble(), formatter)}).',
      CaffeineSleepImpactStatus.elevatedNow =>
        'You have ${_formatMg(insights.currentMg, formatter)} active. '
            'Projected at bedtime (${insights.bedtime}): '
            '${_formatMg(insights.bedtimeMg, formatter)}.',
      CaffeineSleepImpactStatus.mayAffectSleep =>
        'Projected ${_formatMg(insights.bedtimeMg, formatter)} at bedtime '
            '(${insights.bedtime}) is above your threshold '
            '(${_formatMg(insights.sleepThresholdMg.toDouble(), formatter)}).',
    };

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: const BorderRadius.all(Radius.circular(8)),
      ),
      padding: const EdgeInsets.all(12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.titleSmall?.copyWith(color: color),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Text(
                    body,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: scheme.onSurfaceVariant),
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

// ── Active-caffeine curve ───────────────────────────────────────────────────

/// Public so the chart goldens can photograph it on its own.
///
/// It draws one of the app's nine hand-rolled "labelled proportional bar" copies
/// (and, for the curve card, one of its three line renderings). Those are about to
/// be consolidated, and a picture of each — taken BEFORE — is what proves the
/// consolidation changed nothing it did not mean to.
@visibleForTesting
class CaffeineCurveCard extends StatelessWidget {
  const CaffeineCurveCard({super.key,required this.home, required this.formatter});

  final CaffeineHomeDisplay home;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final insights = home.insights;
    final points = insights.curvePoints;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.caffeineCurveTitle, style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            if (points.length < 2)
              ChartEmptyState(
                message: l10n.caffeineCurveEmpty,
                height: kChartHeightDay,
              )
            else ...[
              // The shared plot, at last. What this card gains by giving up its own
              // painter: a Y AXIS — you could not read a value off this chart,
              // there was no scale on it at all — GRIDLINES, and a curve that is
              // actually a curve. The decay of caffeine in a body is smooth, and it
              // was drawn with `lineTo`: the smooth thing rendered as a run of
              // straight cuts.
              //
              // It also picks up the 64px label gutter every other chart reserves,
              // so it shifts sideways. That is a real change, and the golden calls
              // it one rather than letting it pass as a repaint.
              MetricLinePlot(
                points: [
                  for (final point in points)
                    MetricLinePlotPoint(
                      xFraction: _curveFraction(point.time, points),
                      value: point.valueMg,
                    ),
                ],
                minValue: 0,
                maxValue: home.curveMaxMg,
                accentColor: scheme.primary,
                valueFormatter: (value) => _formatMg(value, formatter),
                // The line you are trying to be under by bedtime. Dashed, because
                // it is a rule, not a reading.
                guides: [
                  (
                    value: insights.sleepThresholdMg.toDouble(),
                    color: scheme.error.withValues(alpha: 0.45),
                  ),
                ],
                // Every drink, on the baseline: each sawtooth in the curve now sits
                // directly above the act that caused it.
                markers: [
                  for (final time in home.curveEntryTimes)
                    (
                      xFraction: _curveFraction(time, points),
                      color: scheme.tertiary,
                    ),
                ],
              ),
              const SizedBox(height: 8),
              // And a TIME axis. There was none: a caffeine curve across a whole
              // day, with no way to tell when any of it happened.
              TimeAxisLabels(
                start: points.first.time,
                end: points.last.time,
                inset: kChartPlotInset,
              ),
            ],
            const SizedBox(height: 8),
            Text(
              l10n.caffeineThresholdLine(
                _formatMg(insights.sleepThresholdMg.toDouble(), formatter),
              ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

/// Where a moment sits across the curve's own window, as a fraction. The window
/// is the curve — its first sample to its last — and not the calendar day.
double _curveFraction(DateTime time, List<CaffeinePoint> points) {
  final start = points.first.time.millisecondsSinceEpoch;
  final end = points.last.time.millisecondsSinceEpoch;
  if (end <= start) return 0;
  return ((time.millisecondsSinceEpoch - start) / (end - start))
      .clamp(0.0, 1.0);
}

// ── Bedtime / sleep impact ──────────────────────────────────────────────────

class _CaffeineSleepImpactCard extends StatelessWidget {
  const _CaffeineSleepImpactCard({
    required this.home,
    required this.formatter,
  });

  final CaffeineHomeDisplay home;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final insights = home.insights;
    final safe = home.bedtimeIsSafe;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.bedtime_outlined,
                    color: scheme.primary, size: 20),
                const SizedBox(width: 8),
                Text('Bedtime forecast', style: theme.textTheme.titleSmall),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                _formatMg(insights.bedtimeMg, formatter),
                style: theme.textTheme.headlineMedium?.copyWith(
                  color: safe ? scheme.primary : scheme.error,
                ),
              ),
            ),
            Text(
              'Projected active caffeine at ${insights.bedtime} '
              '(threshold ${_formatMg(insights.sleepThresholdMg.toDouble(), formatter)}).',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                      title: 'Safe nights',
                      value:
                          '${insights.safeNights}/${insights.totalNights}',
                      icon: Icons.nights_stay_outlined,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _MiniStat(
                      title: 'Safe streak',
                      value: formatter.count(insights.safeSleepStreak),
                      icon: Icons.bedtime_outlined,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Analytics ───────────────────────────────────────────────────────────────

class _AnalyticsRangePicker extends StatelessWidget {
  const _AnalyticsRangePicker({required this.selected, required this.onSelect});

  final CaffeineAnalyticsRange selected;
  final ValueChanged<CaffeineAnalyticsRange> onSelect;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final range in CaffeineAnalyticsRange.values)
          ChoiceChip(
            label: Text(range.label),
            selected: range == selected,
            onSelected: (_) => onSelect(range),
          ),
      ],
    );
  }
}

class _CaffeineAnalyticsSummaryCard extends StatelessWidget {
  const _CaffeineAnalyticsSummaryCard({
    required this.analytics,
    required this.formatter,
  });

  final CaffeineAnalyticsDisplay analytics;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final insights = analytics.insights;
    final topSource = analytics.topSourceLabel ?? 'N/A';
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _formatMg(insights.periodTotalMg, formatter),
              style: theme.textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: scheme.primary,
              ),
            ),
            Text(
              'Total caffeine this period',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                      title: 'Daily average',
                      value: _formatMg(insights.periodAverageMg, formatter),
                      icon: Icons.query_stats_outlined,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _MiniStat(
                      title: 'Safe nights',
                      value:
                          '${insights.safeNights}/${insights.totalNights}',
                      icon: Icons.nights_stay_outlined,
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                      title: 'Top source',
                      value: topSource,
                      icon: Icons.local_drink_outlined,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _MiniStat(
                      title: 'Sleep threshold',
                      value: _formatMg(
                          insights.sleepThresholdMg.toDouble(), formatter),
                      icon: Icons.bedtime_outlined,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Public so the chart goldens can photograph it on its own.
///
/// It draws one of the app's nine hand-rolled "labelled proportional bar" copies
/// (and, for the curve card, one of its three line renderings). Those are about to
/// be consolidated, and a picture of each — taken BEFORE — is what proves the
/// consolidation changed nothing it did not mean to.
@visibleForTesting
class CaffeineDistributionCard extends StatelessWidget {
  const CaffeineDistributionCard({
    super.key,
    required this.title,
    required this.bars,
    required this.formatter,
  });

  final String title;
  final List<CaffeineBar> bars;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleSmall),
            if (bars.isEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'No data',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              )
            else
              for (final bar in bars)
                _DistributionRow(
                  label: bar.label,
                  value: _formatMg(bar.valueMg, formatter),
                  fraction: bar.fraction,
                  color: scheme.primary,
                ),
          ],
        ),
      ),
    );
  }
}

/// Public so the chart goldens can photograph it on its own.
///
/// It draws one of the app's nine hand-rolled "labelled proportional bar" copies
/// (and, for the curve card, one of its three line renderings). Those are about to
/// be consolidated, and a picture of each — taken BEFORE — is what proves the
/// consolidation changed nothing it did not mean to.
@visibleForTesting
class CaffeineTimeBucketsCard extends StatelessWidget {
  const CaffeineTimeBucketsCard({
    super.key,
    required this.analytics,
    required this.formatter,
  });

  final CaffeineAnalyticsDisplay analytics;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Time of day', style: theme.textTheme.titleSmall),
            for (final bar in analytics.timeBucketBars)
              _DistributionRow(
                label: _bucketLabel(bar.bucket),
                value: _formatMg(bar.valueMg, formatter),
                fraction: bar.fraction,
                color: scheme.primary,
              ),
          ],
        ),
      ),
    );
  }
}

class _DistributionRow extends StatelessWidget {
  const _DistributionRow({
    required this.label,
    required this.value,
    required this.fraction,
    required this.color,
  });

  final String label;
  final String value;
  final double fraction;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: ChartBarRow(
        fraction: fraction,
        color: color,
        height: 6,
        radius: 3,
        label: Text(label),
        labelStyle: theme.textTheme.bodyMedium,
        trailing: Text(value),
        trailingStyle: theme.textTheme.bodySmall
            ?.copyWith(color: scheme.onSurfaceVariant),
      ),
    );
  }
}

class _MiniStat extends StatelessWidget {
  const _MiniStat({required this.title, required this.value, required this.icon});

  final String title;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final color = scheme.primary;
    return Container(
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: const BorderRadius.all(Radius.circular(8)),
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color, size: 18),
          const SizedBox(height: 8),
          Text(
            value,
            style: theme.textTheme.titleMedium,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          Text(
            title,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

String _bucketLabel(CaffeineTimeOfDayBucket bucket) {
  switch (bucket) {
    case CaffeineTimeOfDayBucket.morning:
      return 'Morning';
    case CaffeineTimeOfDayBucket.afternoon:
      return 'Afternoon';
    case CaffeineTimeOfDayBucket.evening:
      return 'Evening';
    case CaffeineTimeOfDayBucket.night:
      return 'Night';
  }
}

String _formatMg(double value, UnitFormatter formatter) =>
    '${formatter.count(value.round())} mg';

String _formatDurationMinutes(int minutes) {
  final safe = minutes < 0 ? 0 : minutes;
  if (safe < 60) return '$safe min';
  final hours = safe ~/ 60;
  final remaining = safe % 60;
  return remaining == 0 ? '${hours}h' : '${hours}h ${remaining}m';
}

String? _resolveError(ScreenError? error) {
  switch (error) {
    case null:
      return null;
    case ScreenErrorMessage(:final text):
      return text;
    case ScreenErrorNotFound():
      return 'Not found.';
    case ScreenErrorMissingArgument():
      return 'Something went wrong.';
    case ScreenErrorPermissionDenied():
      return 'Permission denied.';
    case ScreenErrorHealthConnectUnavailable():
      return 'Health Connect is unavailable.';
  }
}
