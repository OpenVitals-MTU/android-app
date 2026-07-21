import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/cross_metric_insights.dart';
import '../../../domain/insights/data_confidence.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/model/sleep_daily_summary.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/cross_metric_insight_card.dart';
import '../../../ui/components/daily_goal_components.dart';
import '../../../ui/components/data_confidence_card.dart';
import '../../../ui/components/insight_cards.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_interpretation_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/paginated_entry_list.dart';
import '../../../ui/components/period_comparison_stat.dart';
import '../../../ui/components/personal_baseline_stat.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/sleep_display.dart';
import '../../../ui/components/section_padding.dart';

/// Port of the Kotlin `SleepMetricOrderedSections.kt` section bodies. The screen
/// composes these; each one renders a slice of the precomputed [SleepDisplay]
/// and derives nothing of its own.

const IconData kSleepIcon = Icons.bed_outlined;

/// Kotlin `sleepHoursDisplay`.
DisplayValue sleepHoursDisplay(double hours, UnitFormatter formatter) =>
    DisplayValue(formatter.duration((hours * 3600000).round()), '');

/// Kotlin `SleepStatisticsSectionContent`: goal statistics, the period grid, the
/// sleep-target reading, then the sleep-vs-HRV correlation.
class SleepStatisticsSectionContent extends StatelessWidget {
  const SleepStatisticsSectionContent({
    super.key,
    required this.display,
    required this.selectedRange,
    required this.formatter,
  });

  final SleepDisplay display;
  final TimeRange selectedRange;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final goalProgress = display.goalProgress;

    DisplayValue hours(double value) => sleepHoursDisplay(value, formatter);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(DailyGoalStatistics(
          progress: goalProgress,
          averageGap: hours(goalProgress.averageGapToGoal),
          unitFormatter: formatter,
          icon: kSleepIcon,
          accentColor: AppColors.sleep,
        )),
        sectionPadded(InsightStatGrid(
          stats: [
            InsightStat(
              title: l10n.statTotal,
              value: hours(display.totalHours).value,
              unit: '',
              icon: kSleepIcon,
              accentColor: AppColors.sleep,
            ),
            // Daily average and longest sleep are a single night's duration over
            // again in the day view — they only mean something across a period.
            if (!display.isDay)
              InsightStat(
                title: l10n.statDailyAverage,
                value: hours(display.averageHours).value,
                unit: '',
                icon: Icons.star_outline,
                accentColor: AppColors.sleep,
              ),
            if (!display.isDay)
              InsightStat(
                title: l10n.statLongestSleep,
                value: hours(display.longestHours).value,
                unit: '',
                icon: Icons.calendar_month_outlined,
                accentColor: AppColors.sleep,
              ),
            InsightStat(
              title: l10n.statNightsLogged,
              value: formatter.count(display.nights.length),
              unit: l10n.unitNights,
              icon: Icons.check_circle_outline,
              accentColor: AppColors.sleep,
            ),
            // Sleep compares averages, not totals: a 5-night week is not worse
            // than a 7-night one just because it has fewer nights.
            previousPeriodInsightStat(
              comparison: display.periodComparison,
              selectedRange: selectedRange,
              unitFormatter: formatter,
              valueFormatter: hours,
              accentColor: AppColors.sleep,
              l10n: l10n,
            ),
            ...personalBaselineInsightStats(
              insight: display.baselineInsight,
              unitFormatter: formatter,
              valueFormatter: hours,
              accentColor: AppColors.sleep,
              l10n: l10n,
            ),
          ],
        )),
        SleepTargetContextSection(
          interpretation: display.targetInterpretation,
          formatter: formatter,
        ),
        SleepHrvInsightSection(insight: display.hrvInsight),
      ],
    );
  }
}

/// Kotlin `SleepTargetContextSectionContent`. Renders nothing without nights —
/// the display hands in a null interpretation for exactly that.
class SleepTargetContextSection extends StatelessWidget {
  const SleepTargetContextSection({
    super.key,
    required this.interpretation,
    required this.formatter,
  });

  final SleepTargetInterpretation? interpretation;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final interpretation = this.interpretation;
    if (interpretation == null) return const SizedBox.shrink();

    final average = sleepHoursDisplay(interpretation.averageHours, formatter).text;
    final target = sleepHoursDisplay(interpretation.targetHours, formatter).text;
    final gap = sleepHoursDisplay(interpretation.gapHours, formatter).text;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionMetricContext),
        sectionPadded(MetricInterpretationCard(
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
/// pair with an HRV reading for a correlation to mean anything — which is what a
/// null [insight] says.
class SleepHrvInsightSection extends StatelessWidget {
  const SleepHrvInsightSection({super.key, required this.insight});

  final CrossMetricInsight? insight;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final insight = this.insight;
    if (insight == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionCrossMetricInsights),
        sectionPadded(CrossMetricInsightCard(
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
/// sleep knows its sources and how many nights were typed in by hand — all of
/// which the display has already counted.
class SleepDataConfidenceSection extends StatelessWidget {
  const SleepDataConfidenceSection({super.key, required this.confidence});

  final DataConfidence confidence;

  @override
  Widget build(BuildContext context) {
    return sectionPadded(DataConfidenceCard(
      confidence: confidence,
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

/// Kotlin's `PaginatedEntryList` of sleep sessions. [sessions] arrives newest
/// night first — the display sorted it.
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
    return PaginatedEntryList<SleepData>(
      title: title,
      entries: sessions,
      rowBuilder: (context, session) => SleepSessionItem(
        session: session,
        formatter: formatter,
        // A merged night maps to no single record, so it is not openable; a
        // single-session night keeps its real id and stays tappable.
        onTap: session.id.startsWith(mergedNightIdPrefix)
            ? null
            : () => onOpenSession(session.id),
      ),
    );
  }
}
