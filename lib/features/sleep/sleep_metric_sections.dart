import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/insights/cross_metric_insights.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/insights/data_confidence.dart';
import '../../domain/insights/metric_interpretations.dart';
import '../../domain/insights/period_comparison.dart';
import '../../domain/insights/personal_baseline.dart';
import '../../domain/model/sleep_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/components/cross_metric_insight_card.dart';
import '../../ui/components/daily_goal_components.dart';
import '../../ui/components/data_confidence_card.dart';
import '../../ui/components/insight_cards.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_interpretation_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/paginated_entry_list.dart';
import '../../ui/components/period_comparison_stat.dart';
import '../../ui/components/personal_baseline_stat.dart';
import '../../ui/theme/app_colors.dart';
import 'sleep_presentation.dart';

/// Port of the Kotlin `SleepMetricOrderedSections.kt` section bodies. The screen
/// composes these; each one is a plain widget over a [SleepDisplay].

/// Health Connect's `Metadata.RECORDING_METHOD_MANUAL_ENTRY`.
const int kRecordingMethodManualEntry = 1;

const IconData kSleepIcon = Icons.bed_outlined;

/// Kotlin `sleepHoursDisplay`.
DisplayValue sleepHoursDisplay(double hours, UnitFormatter formatter) =>
    DisplayValue(formatter.duration((hours * 3600000).round()), '');

/// Kotlin `sleepGoalProgress`.
DailyGoalProgress sleepGoalProgress({
  required List<SleepDurationPoint> durationPoints,
  required DatePeriod period,
  required double targetHours,
}) =>
    dailyGoalProgress(
      [
        for (final point in durationPoints)
          DailyGoalValue(date: point.date, value: point.hours),
      ],
      period,
      targetHours,
      MetricDailyGoalKey.sleepHours.direction,
    );

/// The nights that actually recorded sleep. A zero-hour point is a night the
/// period covers but nothing was logged.
List<SleepDurationPoint> sleepNights(List<SleepDurationPoint> points) =>
    [for (final point in points) if (point.hours > 0.0) point];

double _averageHours(List<SleepDurationPoint> nights) => nights.isEmpty
    ? 0.0
    : nights.fold(0.0, (sum, night) => sum + night.hours) / nights.length;

/// Kotlin `SleepStatisticsSectionContent`: goal statistics, the period grid, the
/// sleep-target reading, then the sleep-vs-HRV correlation.
class SleepStatisticsSectionContent extends StatelessWidget {
  const SleepStatisticsSectionContent({
    super.key,
    required this.display,
    required this.period,
    required this.selectedRange,
    required this.formatter,
    required this.goalProgress,
    required this.targetHours,
  });

  final SleepDisplay display;
  final DatePeriod period;
  final TimeRange selectedRange;
  final UnitFormatter formatter;
  final DailyGoalProgress goalProgress;
  final double targetHours;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final nights = sleepNights(display.durationPoints);
    final totalHours = nights.fold(0.0, (sum, night) => sum + night.hours);
    final averageHours = _averageHours(nights);
    final longestHours =
        nights.isEmpty ? 0.0 : nights.map((n) => n.hours).reduce((a, b) => a > b ? a : b);
    final previousAverageHours =
        _averageHours(sleepNights(display.previousDurationPoints));

    DisplayValue hours(double value) => sleepHoursDisplay(value, formatter);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _padded(DailyGoalStatistics(
          progress: goalProgress,
          averageGap: hours(goalProgress.averageGapToGoal),
          unitFormatter: formatter,
          icon: kSleepIcon,
          accentColor: AppColors.sleep,
        )),
        _padded(InsightStatGrid(
          stats: [
            InsightStat(
              title: l10n.statTotal,
              value: hours(totalHours).value,
              unit: '',
              icon: kSleepIcon,
              accentColor: AppColors.sleep,
            ),
            InsightStat(
              title: l10n.statDailyAverage,
              value: hours(averageHours).value,
              unit: '',
              icon: Icons.star_outline,
              accentColor: AppColors.sleep,
            ),
            InsightStat(
              title: l10n.statLongestSleep,
              value: hours(longestHours).value,
              unit: '',
              icon: Icons.calendar_month_outlined,
              accentColor: AppColors.sleep,
            ),
            InsightStat(
              title: l10n.statNightsLogged,
              value: formatter.count(nights.length),
              unit: l10n.unitNights,
              icon: Icons.check_circle_outline,
              accentColor: AppColors.sleep,
            ),
            // Sleep compares averages, not totals: a 5-night week is not worse
            // than a 7-night one just because it has fewer nights.
            previousPeriodInsightStat(
              comparison: periodComparison(averageHours, previousAverageHours),
              selectedRange: selectedRange,
              unitFormatter: formatter,
              valueFormatter: hours,
              accentColor: AppColors.sleep,
              l10n: l10n,
            ),
            ...personalBaselineInsightStats(
              insight: personalBaselineInsight(
                averageHours,
                [
                  for (final point in display.baselineDurationPoints)
                    BaselineValue(date: point.date, value: point.hours),
                ],
                period.start.minusDays(1),
              ),
              unitFormatter: formatter,
              valueFormatter: hours,
              accentColor: AppColors.sleep,
              l10n: l10n,
            ),
          ],
        )),
        SleepTargetContextSection(
          durationPoints: display.durationPoints,
          targetHours: targetHours,
          formatter: formatter,
        ),
        SleepHrvInsightSection(
          durationPoints: display.durationPoints,
          hrvValues: display.crossMetricHrvValues,
        ),
      ],
    );
  }
}

/// Kotlin `SleepTargetContextSectionContent`. Renders nothing without nights.
class SleepTargetContextSection extends StatelessWidget {
  const SleepTargetContextSection({
    super.key,
    required this.durationPoints,
    required this.targetHours,
    required this.formatter,
  });

  final List<SleepDurationPoint> durationPoints;
  final double targetHours;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final nights = sleepNights(durationPoints);
    if (nights.isEmpty) return const SizedBox.shrink();

    final interpretation =
        sleepTargetInterpretation(_averageHours(nights), targetHours);
    if (interpretation == null) return const SizedBox.shrink();

    final average = sleepHoursDisplay(interpretation.averageHours, formatter).text;
    final target = sleepHoursDisplay(interpretation.targetHours, formatter).text;
    final gap = sleepHoursDisplay(interpretation.gapHours, formatter).text;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionMetricContext),
        _padded(MetricInterpretationCard(
          title: l10n.interpretationSleepTitle,
          status: switch (interpretation.status) {
            SleepTargetStatus.belowTarget => l10n.interpretationSleepBelow,
            SleepTargetStatus.nearTarget => l10n.interpretationSleepNear,
            SleepTargetStatus.metTarget => l10n.interpretationSleepMet,
          },
          body: switch (interpretation.status) {
            SleepTargetStatus.belowTarget =>
              l10n.interpretationSleepBelowBody(gap),
            SleepTargetStatus.nearTarget =>
              l10n.interpretationSleepNearBody(average, target),
            SleepTargetStatus.metTarget =>
              l10n.interpretationSleepMetBody(average, target),
          },
          source: l10n.interpretationSleepSource,
          icon: kSleepIcon,
          accentColor: AppColors.sleep,
          severity: interpretation.severity,
        )),
      ],
    );
  }
}

/// Kotlin `SleepHrvInsightSectionContent`. Renders nothing until enough nights
/// pair with an HRV reading for a correlation to mean anything.
class SleepHrvInsightSection extends StatelessWidget {
  const SleepHrvInsightSection({
    super.key,
    required this.durationPoints,
    required this.hrvValues,
  });

  final List<SleepDurationPoint> durationPoints;
  final List<CrossMetricValue> hrvValues;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final insight = crossMetricInsight(
      [
        for (final point in durationPoints)
          CrossMetricValue(date: point.date, value: point.hours),
      ],
      hrvValues,
    );
    if (insight == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionCrossMetricInsights),
        _padded(CrossMetricInsightCard(
          insight: insight,
          title: l10n.crossSleepHrvTitle,
          positiveMessage: l10n.crossSleepHrvPositive,
          negativeMessage: l10n.crossSleepHrvNegative,
          neutralMessage: l10n.crossSleepHrvNeutral,
          accentColor: AppColors.sleep,
        )),
      ],
    );
  }
}

/// Kotlin `SleepDataConfidenceSectionContent`. Unlike the activity metrics,
/// sleep knows its sources and how many nights were typed in by hand.
class SleepDataConfidenceSection extends StatelessWidget {
  const SleepDataConfidenceSection({
    super.key,
    required this.sessions,
    required this.durationPoints,
    required this.period,
  });

  final List<SleepData> sessions;
  final List<SleepDurationPoint> durationPoints;
  final DatePeriod period;

  @override
  Widget build(BuildContext context) {
    return _padded(DataConfidenceCard(
      confidence: dataConfidence(
        period,
        [for (final night in sleepNights(durationPoints)) night.date],
        sessions.length,
        sources: [for (final session in sessions) session.source],
        valueKind: DataValueKind.measured,
        manualEntryCount: sessions
            .where((session) => session.recordingMethod == kRecordingMethodManualEntry)
            .length,
      ),
      accentColor: AppColors.sleep,
    ));
  }
}

/// Kotlin `SleepSessionItem`: one night in the entries list.
class SleepSessionItem extends StatelessWidget {
  const SleepSessionItem({
    super.key,
    required this.session,
    required this.formatter,
    this.onTap,
  });

  final SleepData session;
  final UnitFormatter formatter;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final time = DateFormat.jm(locale);
    final start = session.startTime.toLocal();
    final end = session.endTime.toLocal();

    return OpenVitalsCard(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    DateFormat.yMMMd(locale).format(end),
                    style: theme.textTheme.bodyMedium,
                  ),
                  Text(
                    '${time.format(start)} – ${time.format(end)}',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            Text(
              formatter.duration(
                end.difference(start).inMilliseconds,
              ),
              style:
                  theme.textTheme.titleMedium?.copyWith(color: AppColors.sleep),
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin's `PaginatedEntryList` of sleep sessions, newest night first.
class SleepSessionsSection extends StatelessWidget {
  const SleepSessionsSection({
    super.key,
    required this.title,
    required this.sessions,
    required this.formatter,
    required this.onOpenSession,
  });

  final String title;
  final List<SleepData> sessions;
  final UnitFormatter formatter;
  final ValueChanged<String> onOpenSession;

  @override
  Widget build(BuildContext context) {
    final sorted = [...sessions]
      ..sort((a, b) => b.endTime.compareTo(a.endTime));
    return PaginatedEntryList<SleepData>(
      title: title,
      entries: sorted,
      rowBuilder: (context, session) => SleepSessionItem(
        session: session,
        formatter: formatter,
        onTap: () => onOpenSession(session.id),
      ),
    );
  }
}

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );
