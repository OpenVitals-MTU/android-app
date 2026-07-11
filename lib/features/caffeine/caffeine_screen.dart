import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/caffeine_models.dart';
import '../../health/health_permissions.dart';
import '../../state/app_providers.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/loading_state.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/ov_card.dart';
import 'caffeine_notifier.dart';
import '../../ui/components/section_padding.dart';

/// The caffeine analytics screen, ported from the Kotlin `CaffeineScreen`.
///
/// A bespoke read-only analytics screen (not the Day/Week/Month/Year scaffold):
/// it renders the active-caffeine pharmacokinetic curve, the current-level and
/// bedtime-safety guidance cards, and the source/item/category/time-of-day
/// distributions for the selected analytics range. The Kotlin setup card, entry
/// detail sheet and science/reference cards are omitted (drink logging + profile
/// editing are Phase 6).
class CaffeineScreen extends ConsumerWidget {
  const CaffeineScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(caffeineNotifierProvider);
    final notifier = ref.read(caffeineNotifierProvider.notifier);
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
  CaffeineNotifier notifier,
  UnitFormatter formatter,
) {
  final error = _resolveError(state.error);
  final home = state.homeDisplay;
  final analytics = state.analyticsDisplay;

  return [
    if (error != null) ErrorMessage(error),
    if (state.isLoading && home.curvePoints.isEmpty)
      const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      ),
    const SectionHeader('Caffeine dashboard'),
    sectionPadded(_CaffeineOverviewCard(insights: home, formatter: formatter)),
    sectionPadded(_CaffeineCurveCard(insights: home, formatter: formatter)),
    const SectionHeader('Sleep impact'),
    sectionPadded(_CaffeineSleepImpactCard(insights: home, formatter: formatter)),
    const SectionHeader('Analytics'),
    sectionPadded(
      _AnalyticsRangePicker(
        selected: state.analyticsRange,
        onSelect: notifier.selectAnalyticsRange,
      ),
    ),
    sectionPadded(
      _CaffeineAnalyticsSummaryCard(insights: analytics, formatter: formatter),
    ),
    sectionPadded(
      _CaffeineDistributionCard(
        title: 'Sources',
        slices: analytics.sourceTotals,
        formatter: formatter,
      ),
    ),
    sectionPadded(
      _CaffeineDistributionCard(
        title: 'Items',
        slices: analytics.itemTotals,
        formatter: formatter,
      ),
    ),
    sectionPadded(
      _CaffeineDistributionCard(
        title: 'Inferred categories',
        slices: analytics.categoryTotals,
        formatter: formatter,
      ),
    ),
    sectionPadded(_CaffeineTimeBucketsCard(insights: analytics, formatter: formatter)),
    const SizedBox(height: 16),
  ];
}


// ── Current level + today overview ──────────────────────────────────────────

class _CaffeineOverviewCard extends StatelessWidget {
  const _CaffeineOverviewCard({required this.insights, required this.formatter});

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
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
                insights: insights,
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

enum _SleepImpactStatus { unlikely, elevatedNow, mayAffectSleep }

_SleepImpactStatus _sleepImpactStatus(CaffeineInsights insights) {
  final threshold = insights.sleepThresholdMg.toDouble();
  if (insights.bedtimeMg > threshold) return _SleepImpactStatus.mayAffectSleep;
  if (insights.currentMg > threshold) return _SleepImpactStatus.elevatedNow;
  return _SleepImpactStatus.unlikely;
}

class _CaffeineSleepStatusBanner extends StatelessWidget {
  const _CaffeineSleepStatusBanner({
    required this.insights,
    required this.formatter,
  });

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final status = _sleepImpactStatus(insights);
    final Color color;
    final IconData icon;
    final String title;
    switch (status) {
      case _SleepImpactStatus.unlikely:
        color = scheme.primary;
        icon = Icons.check_circle_outline;
        title = 'Sleep impact unlikely';
      case _SleepImpactStatus.elevatedNow:
        color = scheme.tertiary;
        icon = Icons.query_stats_outlined;
        title = 'Elevated right now';
      case _SleepImpactStatus.mayAffectSleep:
        color = scheme.error;
        icon = Icons.warning_amber_outlined;
        title = 'May affect sleep';
    }
    final body = switch (status) {
      _SleepImpactStatus.unlikely =>
        'Active caffeine (${_formatMg(insights.currentMg, formatter)}) is below '
            'your sleep threshold (${_formatMg(insights.sleepThresholdMg.toDouble(), formatter)}).',
      _SleepImpactStatus.elevatedNow =>
        'You have ${_formatMg(insights.currentMg, formatter)} active. '
            'Projected at bedtime (${insights.bedtime}): '
            '${_formatMg(insights.bedtimeMg, formatter)}.',
      _SleepImpactStatus.mayAffectSleep =>
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

class _CaffeineCurveCard extends StatelessWidget {
  const _CaffeineCurveCard({required this.insights, required this.formatter});

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final points = insights.curvePoints;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Active caffeine over time',
                style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            SizedBox(
              height: 180,
              width: double.infinity,
              child: points.length < 2
                  ? Center(
                      child: Text(
                        'No caffeine to model.',
                        style: theme.textTheme.bodyMedium
                            ?.copyWith(color: scheme.onSurfaceVariant),
                      ),
                    )
                  : CustomPaint(
                      painter: _CaffeineCurvePainter(
                        points: points,
                        thresholdMg: insights.sleepThresholdMg.toDouble(),
                        entryTimes: insights.entryInsights
                            .map((insight) => insight.entry.startTime)
                            .toList(),
                        lineColor: scheme.primary,
                        thresholdColor: scheme.error,
                        markerColor: scheme.tertiary,
                      ),
                    ),
            ),
            const SizedBox(height: 8),
            Text(
              'Dashed line: sleep threshold '
              '(${_formatMg(insights.sleepThresholdMg.toDouble(), formatter)})',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _CaffeineCurvePainter extends CustomPainter {
  _CaffeineCurvePainter({
    required this.points,
    required this.thresholdMg,
    required this.entryTimes,
    required this.lineColor,
    required this.thresholdColor,
    required this.markerColor,
  });

  final List<CaffeinePoint> points;
  final double thresholdMg;
  final List<DateTime> entryTimes;
  final Color lineColor;
  final Color thresholdColor;
  final Color markerColor;

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2) return;
    final startMillis = points.first.time.millisecondsSinceEpoch;
    final endMillis = points.last.time.millisecondsSinceEpoch
        .clamp(startMillis + 1, 1 << 62);
    var maxValue = thresholdMg < 1.0 ? 1.0 : thresholdMg;
    for (final point in points) {
      if (point.valueMg > maxValue) maxValue = point.valueMg;
    }

    double xFor(int millis) =>
        (millis - startMillis) / (endMillis - startMillis) * size.width;
    double yFor(double value) =>
        size.height - (value / maxValue).clamp(0.0, 1.0) * size.height;

    // Threshold guide line.
    final thresholdY = yFor(thresholdMg);
    final thresholdPaint = Paint()
      ..color = thresholdColor.withValues(alpha: 0.45)
      ..strokeWidth = 2;
    const dash = 6.0;
    for (var x = 0.0; x < size.width; x += dash * 2) {
      canvas.drawLine(
        Offset(x, thresholdY),
        Offset((x + dash).clamp(0.0, size.width), thresholdY),
        thresholdPaint,
      );
    }

    // Active-caffeine curve.
    final path = Path();
    for (var i = 0; i < points.length; i++) {
      final offset = Offset(
        xFor(points[i].time.millisecondsSinceEpoch),
        yFor(points[i].valueMg),
      );
      if (i == 0) {
        path.moveTo(offset.dx, offset.dy);
      } else {
        path.lineTo(offset.dx, offset.dy);
      }
    }
    canvas.drawPath(
      path,
      Paint()
        ..color = lineColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );

    // Entry markers along the bottom.
    final markerPaint = Paint()..color = markerColor;
    for (final time in entryTimes) {
      final x = xFor(time.millisecondsSinceEpoch);
      if (x >= 0 && x <= size.width) {
        canvas.drawCircle(Offset(x, size.height - 4), 4, markerPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _CaffeineCurvePainter oldDelegate) => true;
}

// ── Bedtime / sleep impact ──────────────────────────────────────────────────

class _CaffeineSleepImpactCard extends StatelessWidget {
  const _CaffeineSleepImpactCard({
    required this.insights,
    required this.formatter,
  });

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final safe = insights.bedtimeMg <= insights.sleepThresholdMg;
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
    required this.insights,
    required this.formatter,
  });

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final topSource =
        insights.sourceTotals.isNotEmpty ? insights.sourceTotals.first.label : 'N/A';
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

class _CaffeineDistributionCard extends StatelessWidget {
  const _CaffeineDistributionCard({
    required this.title,
    required this.slices,
    required this.formatter,
  });

  final String title;
  final List<CaffeineDistributionSlice> slices;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final visible = slices.take(6).toList();
    var max = 1.0;
    for (final slice in visible) {
      if (slice.valueMg > max) max = slice.valueMg;
    }
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleSmall),
            if (visible.isEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  'No data',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              )
            else
              for (final slice in visible)
                _DistributionRow(
                  label: slice.label,
                  value: _formatMg(slice.valueMg, formatter),
                  fraction: slice.valueMg / max,
                  color: scheme.primary,
                ),
          ],
        ),
      ),
    );
  }
}

class _CaffeineTimeBucketsCard extends StatelessWidget {
  const _CaffeineTimeBucketsCard({
    required this.insights,
    required this.formatter,
  });

  final CaffeineInsights insights;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final buckets = insights.timeBuckets;
    var max = 1.0;
    for (final bucket in buckets) {
      if (bucket.valueMg > max) max = bucket.valueMg;
    }
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Time of day', style: theme.textTheme.titleSmall),
            for (final bucket in buckets)
              _DistributionRow(
                label: _bucketLabel(bucket.bucket),
                value: _formatMg(bucket.valueMg, formatter),
                fraction: bucket.valueMg / max,
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
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(label, style: theme.textTheme.bodyMedium),
              ),
              Text(
                value,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: const BorderRadius.all(Radius.circular(3)),
            child: LinearProgressIndicator(
              value: fraction.clamp(0.0, 1.0),
              minHeight: 6,
              backgroundColor: scheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(color),
            ),
          ),
        ],
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
