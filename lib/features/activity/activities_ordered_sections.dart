import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/cardio_load.dart';
import '../../domain/insights/cross_metric_insights.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/insights/data_confidence.dart';
import '../../domain/insights/metric_interpretations.dart';
import '../../domain/insights/period_comparison.dart';
import '../../domain/insights/personal_baseline.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/preferences/metric_detail_section_id.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/charts/sparkline_chart.dart';
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
import '../../ui/components/swipe_to_delete_entry_row.dart';
import '../../ui/theme/app_colors.dart';
import 'activities_notifier.dart';
import 'exercise_labels.dart';
import '../../ui/components/section_padding.dart';
import '../../ui/components/accent_icon_chip.dart';

/// Health Connect `Metadata.RECORDING_METHOD_MANUAL_ENTRY`.
const int _recordingMethodManualEntry = 3;

final DateFormat _rowTimeFormat = DateFormat('EEE d MMM · HH:mm');

/// The reorderable body of the activities aggregate screen — a port of Kotlin
/// `renderActivitiesOrderedContent` (`ActivitiesMetricOrderedSections.kt`).
/// Renders every section in the user's persisted order, gated by the same
/// visibility rules Kotlin uses.
class ActivitiesOrderedSections extends ConsumerWidget {
  const ActivitiesOrderedSections({
    super.key,
    required this.state,
    required this.period,
    required this.daySelection,
  });

  final ActivitiesState state;
  final DatePeriod period;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final formatter = ref.watch(unitFormatterProvider);
    final notifier = ref.read(activitiesNotifierProvider.notifier);
    final weekPeriodMode = ref.watch(weekPeriodModeProvider);

    final workouts = state.workouts;
    final sortedDays = [...state.overviewDays]
      ..sort((a, b) => a.date.compareTo(b.date));
    final overviewTotals =
        sortedDays.isEmpty ? null : _overviewTotals(sortedDays);
    final selectedDay = daySelection.selectedDate;

    final goalValues = workoutDailyGoalValues(workouts);
    final goalProgress = dailyGoalProgress(
      goalValues,
      period,
      state.dailyGoalMinutes,
      activitiesGoalKey.direction,
    );

    final hasGuideline = workouts.isNotEmpty &&
        workoutGuidelineProgress(_guidelineMinutes(workouts)) != null;
    final hasCrossMetric = workouts.isNotEmpty &&
        crossMetricInsight(
              [
                for (final v in goalValues)
                  CrossMetricValue(date: v.date, value: v.value),
              ],
              [
                for (final r in state.crossDailyRestingHR)
                  CrossMetricValue(date: r.date, value: r.bpm.toDouble()),
              ],
            ) !=
            null;

    return OrderedMetricDetailSections(
      sections: [
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          visible: workouts.isNotEmpty ||
              sortedDays.isNotEmpty ||
              state.plannedWorkouts.isNotEmpty ||
              !state.isLoading,
          _summarySection(
              context, ref, l10n, formatter, notifier, sortedDays, weekPeriodMode),
        ),
        MetricDetailSection(
          MetricDetailSectionId.activityKeyMetrics,
          visible: sortedDays.isNotEmpty && overviewTotals != null,
          overviewTotals == null
              ? const SizedBox.shrink()
              : _keyMetricsSection(context, l10n, formatter, sortedDays,
                  overviewTotals, weekPeriodMode),
        ),
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: workouts.isNotEmpty,
          sectionPadded(_periodChart(formatter, weekPeriodMode)),
        ),
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null && workouts.isNotEmpty,
          selectedDay == null
              ? const SizedBox.shrink()
              : sectionPadded(_selectedDayEntries(
                  context, ref, notifier, formatter, selectedDay)),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          visible: workouts.isNotEmpty,
          sectionPadded(DailyGoalCard(
            goal: formatter.minutes(state.dailyGoalMinutes.round()),
            progress: goalProgress,
            icon: Icons.directions_run,
            accentColor: AppColors.workout,
            onDecreaseGoal: notifier.decreaseDailyGoal,
            onIncreaseGoal: notifier.increaseDailyGoal,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          visible: workouts.isNotEmpty,
          _statisticsSection(l10n, formatter, goalProgress, goalValues),
        ),
        MetricDetailSection(
          MetricDetailSectionId.metricContext,
          visible: hasGuideline,
          _guidelineSection(l10n, formatter),
        ),
        MetricDetailSection(
          MetricDetailSectionId.crossMetricInsights,
          visible: hasCrossMetric,
          _crossMetricSection(l10n, goalValues),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: workouts.isNotEmpty && period.start != period.end,
          sectionPadded(DataConfidenceCard(
            confidence: dataConfidence(
              period,
              [for (final w in workouts) instantToLocalDate(w.startTime)],
              workouts.length,
              sources: [for (final w in workouts) w.source],
              manualEntryCount: workouts
                  .where((w) => w.recordingMethod == _recordingMethodManualEntry)
                  .length,
            ),
            accentColor: AppColors.workout,
          )),
        ),
      ],
    );
  }

  // ── ACTIVITY_SUMMARY ────────────────────────────────────────────────

  Widget _summarySection(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    UnitFormatter formatter,
    ActivitiesNotifier notifier,
    List<ActivityOverviewDay> sortedDays,
    WeekPeriodMode weekPeriodMode,
  ) {
    final showFilter = state.availableActivityTypes.isNotEmpty ||
        state.selectedActivityType != null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (showFilter)
          sectionPadded(_ActivityTypeFilter(
            selectedActivityType: state.selectedActivityType,
            availableActivityTypes: state.availableActivityTypes,
            onSelect: notifier.selectActivityType,
          )),
        if (state.workouts.isNotEmpty || sortedDays.isNotEmpty || !state.isLoading)
          sectionPadded(_ActivityPeriodSummaryCard(
            state: state,
            period: period,
            days: sortedDays,
            formatter: formatter,
            weekPeriodMode: weekPeriodMode,
            showEmptyState: !state.isLoading,
            onOpen: (id) => context.push(AppRoutes.activityDetailLocation(id)),
            onDelete: notifier.deleteActivityEntry,
          )),
        if (state.activityTypeAggregates.isNotEmpty)
          sectionPadded(_ActivityTypeAggregateCard(
            aggregates: state.activityTypeAggregates,
            formatter: formatter,
          )),
        if (state.plannedWorkouts.isNotEmpty)
          sectionPadded(_PlannedWorkoutCard(
            plannedWorkouts: state.plannedWorkouts,
            formatter: formatter,
            onStart: (id) =>
                context.push(AppRoutes.activityEntryLocation(planId: id)),
          )),
      ],
    );
  }

  // ── ACTIVITY_KEY_METRICS ────────────────────────────────────────────

  Widget _keyMetricsSection(
    BuildContext context,
    AppLocalizations l10n,
    UnitFormatter formatter,
    List<ActivityOverviewDay> days,
    _OverviewTotals totals,
    WeekPeriodMode weekPeriodMode,
  ) {
    final title = periodTitle(
      l10n,
      state.selectedRange,
      period,
      weekPeriodMode: weekPeriodMode,
    );
    final buckets = _buckets(days, state.selectedRange);
    final bucketLabels = _bucketLabels(
      buckets,
      state.selectedRange,
      Localizations.localeOf(context).toString(),
    );
    final estimated = days
        .any((d) => d.energyBurnedSource == CaloriesBurnedSource.estimatedActiveAndBmr);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(SectionHeader(l10n.activitiesKeyMetrics)),
        _MetricCard(
          title: l10n.metricCardioLoad,
          value: totals.hasCardioLoad
              ? DisplayValue(formatter.count(totals.cardioLoad), '')
              : DisplayValue(l10n.noData, ''),
          subtitle:
              '$title / ${_cardioConfidenceLabel(l10n, totals.cardioLoadConfidence)}',
          icon: Icons.favorite,
          accentColor: AppColors.heart,
          series: _series(buckets, (d) =>
              d.cardioLoadConfidence == CardioLoadConfidence.noData
                  ? null
                  : d.cardioLoad.toDouble()),
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.cardioLoadDetail),
        ),
        _MetricCard(
          title: l10n.metricEnergyBurned,
          value: totals.hasEnergyBurned
              ? formatter.energy(totals.energyBurnedKcal)
              : DisplayValue(l10n.noData, ''),
          subtitle: estimated ? l10n.caloriesEstimatedActiveBmr : title,
          icon: Icons.local_fire_department,
          accentColor: AppColors.calories,
          series: _series(buckets, (d) =>
              d.energyBurnedSource == CaloriesBurnedSource.noData
                  ? null
                  : d.energyBurnedKcal),
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.calories),
        ),
        _MetricCard(
          title: l10n.metricSteps,
          value: DisplayValue(formatter.count(totals.steps), ''),
          subtitle: title,
          icon: Icons.directions_walk,
          accentColor: AppColors.steps,
          series: _series(buckets, (d) => d.steps.toDouble()),
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.metricLocation('STEPS')),
        ),
        _MetricCard(
          title: l10n.metricDistance,
          value: formatter.distance(totals.distanceMeters),
          subtitle: title,
          icon: Icons.straighten,
          accentColor: AppColors.distance,
          series: _series(buckets, (d) => d.distanceMeters),
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.metricLocation('DISTANCE')),
        ),
        _MetricCard(
          title: l10n.metricHrv,
          value: totals.hrvRmssdMs == null
              ? DisplayValue(l10n.noData, '')
              : formatter.hrv(totals.hrvRmssdMs!),
          subtitle: '$title / ${l10n.statAverage}',
          icon: Icons.favorite_border,
          accentColor: AppColors.heart,
          series: _series(buckets, (d) => d.hrvRmssdMs, average: true),
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.metricLocation('HRV')),
        ),
      ],
    );
  }

  // ── PERIOD_CHART ────────────────────────────────────────────────────

  Widget _periodChart(UnitFormatter formatter, WeekPeriodMode weekPeriodMode) {
    final byDate = <LocalDate, double>{};
    for (final w in state.workouts) {
      final date = instantToLocalDate(w.startTime);
      byDate[date] =
          (byDate[date] ?? 0.0) + math.max(0, w.durationMs).toDouble() / 60000.0;
    }
    final totalMs =
        state.workouts.fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
    return MetricBarChart(
      title: '',
      values: [
        for (final entry in byDate.entries)
          PeriodChartValue(entry.key, entry.value),
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.workout,
      summaryValue: formatter.duration(totalMs),
      weekPeriodMode: weekPeriodMode,
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.minutes(value.round()).text,
    );
  }

  // ── SELECTED_DAY_ENTRIES ────────────────────────────────────────────

  Widget _selectedDayEntries(
    BuildContext context,
    WidgetRef ref,
    ActivitiesNotifier notifier,
    UnitFormatter formatter,
    LocalDate selectedDay,
  ) {
    final locale = Localizations.localeOf(context).toLanguageTag();
    final entries = [
      for (final w in state.workouts)
        if (instantToLocalDate(w.startTime) == selectedDay) w,
    ];
    return PaginatedEntryList<ExerciseData>(
      title: DateFormat.yMMMd(locale).format(
        DateTime(selectedDay.year, selectedDay.month, selectedDay.day),
      ),
      entries: entries,
      rowBuilder: (context, workout) => _WorkoutRow(
        workout: workout,
        formatter: formatter,
        onTap: () =>
            context.push(AppRoutes.activityDetailLocation(workout.id)),
        onDelete: workout.isOpenVitalsEntry && workout.id.isNotEmpty
            ? () => notifier.deleteActivityEntry(workout.id)
            : null,
      ),
    );
  }

  // ── STATISTICS ──────────────────────────────────────────────────────

  Widget _statisticsSection(
    AppLocalizations l10n,
    UnitFormatter formatter,
    DailyGoalProgress goalProgress,
    List<DailyGoalValue> goalValues,
  ) {
    final workouts = state.workouts;
    final totalMs =
        workouts.fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
    final averageMs = workouts.isEmpty ? 0 : totalMs ~/ workouts.length;
    final longestMs = workouts.isEmpty
        ? 0
        : workouts
            .map((w) => math.max(0, w.durationMs))
            .reduce((a, b) => a > b ? a : b);
    final previousTotalMs = state.previousWorkouts
        .fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
    final dailyMinutes = [for (final v in goalValues) v.value];
    final currentAverage = dailyMinutes.isEmpty
        ? 0.0
        : dailyMinutes.reduce((a, b) => a + b) / dailyMinutes.length;
    final baselineValues = [
      for (final v in workoutDailyGoalValues(state.baselineWorkouts))
        BaselineValue(date: v.date, value: v.value),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(SectionHeader(l10n.sectionStatistics)),
        sectionPadded(DailyGoalStatistics(
          progress: goalProgress,
          averageGap: formatter.minutes(goalProgress.averageGapToGoal.round()),
          unitFormatter: formatter,
          icon: Icons.directions_run,
          accentColor: AppColors.workout,
        )),
        sectionPadded(InsightStatGrid(
          stats: [
            InsightStat(
              title: l10n.statTotal,
              value: formatter.duration(totalMs),
              unit: '',
              icon: Icons.directions_run,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.sectionActivities,
              value: formatter.count(workouts.length),
              unit: '',
              icon: Icons.check_circle_outline,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.statAverageDuration,
              value: formatter.duration(averageMs),
              unit: '',
              icon: Icons.star_outline,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.statLongestWorkout,
              value: formatter.duration(longestMs),
              unit: '',
              icon: Icons.calendar_month_outlined,
              accentColor: AppColors.workout,
            ),
            previousPeriodInsightStat(
              comparison: periodComparison(
                totalMs.toDouble(),
                previousTotalMs.toDouble(),
              ),
              selectedRange: state.selectedRange,
              unitFormatter: formatter,
              valueFormatter: (value) =>
                  DisplayValue(formatter.duration(value.round()), ''),
              accentColor: AppColors.workout,
              l10n: l10n,
            ),
            ...personalBaselineInsightStats(
              insight: personalBaselineInsight(
                currentAverage,
                baselineValues,
                period.start.minusDays(1),
              ),
              unitFormatter: formatter,
              valueFormatter: (value) => formatter.minutes(value.round()),
              accentColor: AppColors.workout,
              l10n: l10n,
            ),
          ],
        )),
      ],
    );
  }

  // ── METRIC_CONTEXT (HHS guideline) ─────────────────────────────────

  Widget _guidelineSection(AppLocalizations l10n, UnitFormatter formatter) {
    final useWeeklyAverage = state.selectedRange == TimeRange.month ||
        state.selectedRange == TimeRange.year;
    final progress = workoutGuidelineProgress(_guidelineMinutes(state.workouts));
    if (progress == null) return const SizedBox.shrink();
    final status = switch (progress.status) {
      WorkoutGuidelineStatus.noLoggedMinutes => l10n.interpretationWorkoutNone,
      WorkoutGuidelineStatus.belowReference => l10n.interpretationWorkoutBelow,
      WorkoutGuidelineStatus.approachingReference =>
        l10n.interpretationWorkoutApproaching,
      WorkoutGuidelineStatus.meetsReference => l10n.interpretationWorkoutMet,
    };
    final minutesText = formatter.minutes(progress.loggedMinutes.round()).text;
    final percentText =
        formatter.percent(progress.percentOfReference, decimals: 0).text;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(SectionHeader(l10n.sectionMetricContext)),
        sectionPadded(MetricInterpretationCard(
          title: l10n.interpretationWorkoutTitle,
          status: status,
          body: useWeeklyAverage
              ? l10n.interpretationWorkoutBodyWeeklyAverage(
                  minutesText, percentText)
              : l10n.interpretationWorkoutBody(minutesText, percentText),
          source: l10n.interpretationWorkoutSource,
          icon: Icons.directions_run,
          accentColor: AppColors.workout,
          severity: progress.severity,
        )),
      ],
    );
  }

  // ── CROSS_METRIC_INSIGHTS (resting HR) ─────────────────────────────

  Widget _crossMetricSection(
    AppLocalizations l10n,
    List<DailyGoalValue> goalValues,
  ) {
    final insight = crossMetricInsight(
      [for (final v in goalValues) CrossMetricValue(date: v.date, value: v.value)],
      [
        for (final r in state.crossDailyRestingHR)
          CrossMetricValue(date: r.date, value: r.bpm.toDouble()),
      ],
    );
    if (insight == null) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        sectionPadded(SectionHeader(l10n.sectionCrossMetricInsights)),
        sectionPadded(CrossMetricInsightCard(
          insight: insight,
          title: l10n.crossWorkoutRestingHrTitle,
          positiveMessage: l10n.crossWorkoutRestingHrPositive,
          negativeMessage: l10n.crossWorkoutRestingHrNegative,
          neutralMessage: l10n.crossWorkoutRestingHrNeutral,
          accentColor: AppColors.workout,
        )),
      ],
    );
  }

  double _guidelineMinutes(List<ExerciseData> workouts) {
    final totalMinutes =
        workouts.fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs)) /
            60000.0;
    if (state.selectedRange == TimeRange.month ||
        state.selectedRange == TimeRange.year) {
      return totalMinutes / _weekCount(period);
    }
    return totalMinutes;
  }
}


double _weekCount(DatePeriod period) {
  final days = period.end.epochDay - period.start.epochDay + 1;
  return math.max(days / 7.0, 1.0 / 7.0);
}

// ── Overview buckets / series / totals ─────────────────────────────────

class _Bucket {
  const _Bucket(this.date, this.days);
  // The bucket's representative date — its first (earliest) day (Kotlin
  // `ActivityOverviewBucket.date`).
  final LocalDate date;
  final List<ActivityOverviewDay> days;
}

List<_Bucket> _buckets(List<ActivityOverviewDay> days, TimeRange range) {
  final sorted = [...days]..sort((a, b) => a.date.compareTo(b.date));
  final maxBuckets = range == TimeRange.year ? 12 : 7;
  final List<_Bucket> raw;
  if (range == TimeRange.year) {
    final byMonth = <String, List<ActivityOverviewDay>>{};
    for (final day in sorted) {
      final key = '${day.date.year}-${day.date.month}';
      byMonth.putIfAbsent(key, () => <ActivityOverviewDay>[]).add(day);
    }
    raw = [
      for (final group in byMonth.values) _Bucket(group.first.date, group),
    ];
  } else {
    raw = [
      for (final day in sorted) _Bucket(day.date, [day]),
    ];
  }
  if (raw.isEmpty || maxBuckets <= 0) return const <_Bucket>[];
  if (raw.length <= maxBuckets) return raw;
  final chunkSize = math.max(1, (raw.length / maxBuckets).ceil());
  final chunked = <_Bucket>[];
  for (var i = 0; i < raw.length; i += chunkSize) {
    final slice = raw.sublist(i, math.min(i + chunkSize, raw.length));
    chunked.add(_Bucket(
      slice.first.date,
      [for (final b in slice) ...b.days],
    ));
  }
  return chunked;
}

/// The per-bucket labels under a key-metric sparkline (Kotlin
/// `activityOverviewBucketLabel`): DAY & WEEK → the weekday's initial;
/// MONTH → day-of-month; YEAR → the month's initial. Uses [locale].
List<String> _bucketLabels(
  List<_Bucket> buckets,
  TimeRange range,
  String locale,
) =>
    [for (final bucket in buckets) _bucketLabel(bucket.date, range, locale)];

String _bucketLabel(LocalDate date, TimeRange range, String locale) {
  final dateTime = DateTime(date.year, date.month, date.day);
  switch (range) {
    case TimeRange.day:
    case TimeRange.week:
      return _initial(DateFormat.E(locale).format(dateTime));
    case TimeRange.month:
      return DateFormat.d(locale).format(dateTime);
    case TimeRange.year:
      return _initial(DateFormat.MMM(locale).format(dateTime));
  }
}

String _initial(String text) =>
    text.isEmpty ? '' : String.fromCharCode(text.runes.first);

List<double> _series(
  List<_Bucket> buckets,
  double? Function(ActivityOverviewDay) selector, {
  bool average = false,
}) =>
    [
      for (final bucket in buckets)
        () {
          final values = [
            for (final day in bucket.days)
              if (selector(day) != null) selector(day)!,
          ];
          if (values.isEmpty) return 0.0;
          final sum = values.reduce((a, b) => a + b);
          return average ? sum / values.length : sum;
        }(),
    ];

class _OverviewTotals {
  const _OverviewTotals({
    required this.steps,
    required this.distanceMeters,
    required this.energyBurnedKcal,
    required this.hasEnergyBurned,
    required this.cardioLoad,
    required this.hasCardioLoad,
    required this.cardioLoadConfidence,
    required this.hrvRmssdMs,
  });

  final int steps;
  final double distanceMeters;
  final double energyBurnedKcal;
  final bool hasEnergyBurned;
  final int cardioLoad;
  final bool hasCardioLoad;
  final CardioLoadConfidence cardioLoadConfidence;
  final double? hrvRmssdMs;
}

_OverviewTotals _overviewTotals(List<ActivityOverviewDay> days) {
  final hrvValues = [for (final d in days) if (d.hrvRmssdMs != null) d.hrvRmssdMs!];
  final cardioDays = [
    for (final d in days)
      if (d.cardioLoadConfidence != CardioLoadConfidence.noData) d,
  ];
  return _OverviewTotals(
    steps: days.fold<int>(0, (sum, d) => sum + d.steps),
    distanceMeters: days.fold<double>(0, (sum, d) => sum + d.distanceMeters),
    energyBurnedKcal: days.fold<double>(0, (sum, d) => sum + d.energyBurnedKcal),
    hasEnergyBurned:
        days.any((d) => d.energyBurnedSource != CaloriesBurnedSource.noData),
    cardioLoad: cardioDays.fold<int>(0, (sum, d) => sum + d.cardioLoad),
    hasCardioLoad: cardioDays.isNotEmpty,
    cardioLoadConfidence: _aggregateCardioConfidence(cardioDays),
    hrvRmssdMs: hrvValues.isEmpty
        ? null
        : hrvValues.reduce((a, b) => a + b) / hrvValues.length,
  );
}

CardioLoadConfidence _aggregateCardioConfidence(List<ActivityOverviewDay> days) {
  if (days.isEmpty) return CardioLoadConfidence.noData;
  if (days.any((d) => d.cardioLoadConfidence == CardioLoadConfidence.low)) {
    return CardioLoadConfidence.low;
  }
  if (days.any((d) => d.cardioLoadConfidence == CardioLoadConfidence.medium)) {
    return CardioLoadConfidence.medium;
  }
  return CardioLoadConfidence.high;
}

String _cardioConfidenceLabel(
  AppLocalizations l10n,
  CardioLoadConfidence confidence,
) =>
    switch (confidence) {
      CardioLoadConfidence.high => l10n.cardioLoadConfidenceHigh,
      CardioLoadConfidence.medium => l10n.cardioLoadConfidenceMedium,
      CardioLoadConfidence.low => l10n.cardioLoadConfidenceLow,
      CardioLoadConfidence.noData => l10n.cardioLoadConfidenceNoData,
    };

// ── Widgets ────────────────────────────────────────────────────────────

/// Kotlin `ActivityTypeFilterSelector`: an exposed-dropdown `All / type` menu.
class _ActivityTypeFilter extends StatelessWidget {
  const _ActivityTypeFilter({
    required this.selectedActivityType,
    required this.availableActivityTypes,
    required this.onSelect,
  });

  final int? selectedActivityType;
  final List<int> availableActivityTypes;
  final ValueChanged<int?> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final options = <int>{
      ...availableActivityTypes,
      ?selectedActivityType,
    }.toList()
      ..sort((a, b) => exerciseTypeLabel(a).compareTo(exerciseTypeLabel(b)));
    return DropdownButtonFormField<int?>(
      initialValue: selectedActivityType,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: l10n.activitiesFilterActivityTypeLabel,
        border: const OutlineInputBorder(),
      ),
      items: [
        DropdownMenuItem<int?>(
          value: null,
          child: Text(l10n.activitiesFilterAll),
        ),
        for (final type in options)
          DropdownMenuItem<int?>(
            value: type,
            child: Text(exerciseTypeLabel(type)),
          ),
      ],
      onChanged: onSelect,
    );
  }
}

class _ActivityPeriodSummaryCard extends StatefulWidget {
  const _ActivityPeriodSummaryCard({
    required this.state,
    required this.period,
    required this.days,
    required this.formatter,
    required this.weekPeriodMode,
    required this.showEmptyState,
    required this.onOpen,
    required this.onDelete,
  });

  final ActivitiesState state;
  final DatePeriod period;
  final List<ActivityOverviewDay> days;
  final UnitFormatter formatter;
  final WeekPeriodMode weekPeriodMode;
  final bool showEmptyState;
  final ValueChanged<String> onOpen;
  final Future<void> Function(String) onDelete;

  @override
  State<_ActivityPeriodSummaryCard> createState() =>
      _ActivityPeriodSummaryCardState();
}

class _ActivityPeriodSummaryCardState
    extends State<_ActivityPeriodSummaryCard> {
  static const int _pageSize = 10;
  int _visibleCount = _pageSize;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final workouts = widget.state.workouts;
    final visibleCount = math.min(_visibleCount, workouts.length);
    final visible = workouts.take(visibleCount).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(periodTitle(
          AppLocalizations.of(context),
          widget.state.selectedRange,
          widget.period,
          weekPeriodMode: widget.weekPeriodMode,
        )),
        OpenVitalsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (workouts.isEmpty)
                if (widget.showEmptyState)
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      AppLocalizations.of(context).messageNoActivitiesPeriod,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  )
                else
                  const SizedBox.shrink()
              else ...[
                for (var i = 0; i < visible.length; i++) ...[
                  _WorkoutRow(
                    workout: visible[i],
                    formatter: widget.formatter,
                    onTap: () => widget.onOpen(visible[i].id),
                    onDelete: visible[i].isOpenVitalsEntry &&
                            visible[i].id.isNotEmpty
                        ? () => widget.onDelete(visible[i].id)
                        : null,
                  ),
                  if (i < visible.length - 1)
                    Divider(
                      height: 1,
                      indent: 72,
                      color: theme.colorScheme.outlineVariant
                          .withValues(alpha: 0.55),
                    ),
                ],
                if (visibleCount < workouts.length)
                  Padding(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: OutlinedButton(
                      onPressed: () => setState(() {
                        _visibleCount = math.min(
                            visibleCount + _pageSize, workouts.length);
                      }),
                      child:
                          Text(AppLocalizations.of(context).actionLoadMoreEntries),
                    ),
                  ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _WorkoutRow extends StatelessWidget {
  const _WorkoutRow({
    required this.workout,
    required this.formatter,
    required this.onTap,
    this.onDelete,
  });

  final ExerciseData workout;
  final UnitFormatter formatter;
  final VoidCallback onTap;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final content = _RowContent(
      workout: workout,
      formatter: formatter,
      onTap: onTap,
    );
    if (onDelete == null) return content;
    return SwipeToDeleteEntryRow(
      key: ValueKey('workout-${workout.id}'),
      onDelete: onDelete!,
      child: content,
    );
  }
}

class _RowContent extends StatelessWidget {
  const _RowContent({
    required this.workout,
    required this.formatter,
    required this.onTap,
  });

  final ExerciseData workout;
  final UnitFormatter formatter;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final title = workout.title?.trim().isNotEmpty == true
        ? workout.title!
        : exerciseTypeLabel(workout.exerciseType);
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            AccentIconChip(
              icon: exerciseTypeIcon(workout.exerciseType),
              color: AppColors.workout,
              iconSize: 22,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: theme.textTheme.titleSmall,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis),
                  Text(
                    _rowTimeFormat.format(workout.startTime.toLocal()),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(formatter.duration(workout.durationMs),
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.bold)),
                Text(
                  AppLocalizations.of(context).detailDuration,
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ActivityTypeAggregateCard extends StatelessWidget {
  const _ActivityTypeAggregateCard({
    required this.aggregates,
    required this.formatter,
  });

  final List<ActivityTypeAggregate> aggregates;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionActivityTypeStats),
        OpenVitalsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var i = 0; i < aggregates.length; i++) ...[
                _aggregateRow(context, l10n, aggregates[i]),
                if (i < aggregates.length - 1)
                  Divider(
                    height: 1,
                    indent: 72,
                    color: theme.colorScheme.outlineVariant
                        .withValues(alpha: 0.55),
                  ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  Widget _aggregateRow(
    BuildContext context,
    AppLocalizations l10n,
    ActivityTypeAggregate aggregate,
  ) {
    final theme = Theme.of(context);
    final noData = DisplayValue(l10n.noData, '');
    final usePace = _prefersPace(aggregate.exerciseType);
    final avgSpeed = aggregate.averageMovingSpeedMetersPerSecond;
    final bestSpeed = aggregate.bestSpeedMetersPerSecond;
    final avgValue = usePace
        ? (formatter.averagePace(
                aggregate.totalDistanceMeters, aggregate.totalMovingDurationMs) ??
            noData)
        : (avgSpeed != null ? formatter.speed(avgSpeed) : noData);
    final bestValue = usePace
        ? (bestSpeed != null && bestSpeed > 0
            ? (formatter.averagePace(1.0, (1000.0 / bestSpeed).round()) ?? noData)
            : noData)
        : (bestSpeed != null ? formatter.speed(bestSpeed) : noData);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              AccentIconChip(
                icon: exerciseTypeIcon(aggregate.exerciseType),
                color: AppColors.workout,
                iconSize: 22,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(exerciseTypeLabel(aggregate.exerciseType),
                        style: theme.textTheme.titleSmall
                            ?.copyWith(fontWeight: FontWeight.w600)),
                    Text(
                      l10n.activityTypeStatsActivityCount(aggregate.count),
                      style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _aggregateMetric(
                  theme,
                  l10n.metricDistance,
                  aggregate.totalDistanceMeters > 0
                      ? formatter.distance(aggregate.totalDistanceMeters)
                      : noData,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _aggregateMetric(theme, l10n.statTime,
                    DisplayValue(formatter.duration(aggregate.totalDurationMs), '')),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _aggregateMetric(
                  theme,
                  usePace
                      ? l10n.statAverageMovingPace
                      : l10n.activityEntryRecordingAverageMovingSpeed,
                  avgValue,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _aggregateMetric(
                  theme,
                  usePace ? l10n.statFastestPace : l10n.statBestSpeed,
                  bestValue,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _aggregateMetric(ThemeData theme, String label, DisplayValue value) =>
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(value.value,
              style: theme.textTheme.titleLarge
                  ?.copyWith(fontWeight: FontWeight.bold),
              maxLines: 1,
              overflow: TextOverflow.ellipsis),
          if (value.unit.isNotEmpty)
            Text(value.unit,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          Text(label,
              style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontWeight: FontWeight.w600),
              maxLines: 1,
              overflow: TextOverflow.ellipsis),
        ],
      );
}

/// EXERCISE_TYPE_* constants that read better as pace than speed.
bool _prefersPace(int exerciseType) =>
    exerciseType == 56 || // running
    exerciseType == 57 || // running treadmill
    exerciseType == 79 || // walking
    exerciseType == 37 || // hiking
    exerciseType == 63; // snowshoeing

class _PlannedWorkoutCard extends StatelessWidget {
  const _PlannedWorkoutCard({
    required this.plannedWorkouts,
    required this.formatter,
    required this.onStart,
  });

  final List<PlannedExerciseData> plannedWorkouts;
  final UnitFormatter formatter;
  final ValueChanged<String> onStart;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final sorted = [...plannedWorkouts]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionPlannedWorkouts),
        OpenVitalsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var i = 0; i < sorted.length; i++) ...[
                _plannedRow(context, l10n, sorted[i]),
                if (i < sorted.length - 1)
                  Divider(
                    height: 1,
                    indent: 72,
                    color: theme.colorScheme.outlineVariant
                        .withValues(alpha: 0.55),
                  ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  Widget _plannedRow(
    BuildContext context,
    AppLocalizations l10n,
    PlannedExerciseData planned,
  ) {
    final theme = Theme.of(context);
    final isActionable = planned.completedExerciseSessionId == null;
    final title = planned.title?.trim().isNotEmpty == true
        ? planned.title!
        : exerciseTypeLabel(planned.exerciseType);
    final row = Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          AccentIconChip(
            icon: planned.completedExerciseSessionId != null
                ? Icons.check_circle_outline
                : exerciseTypeIcon(planned.exerciseType),
            color: AppColors.workout,
            iconSize: 22,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: theme.textTheme.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                Text(
                  planned.hasExplicitTime
                      ? _rowTimeFormat.format(planned.startTime.toLocal())
                      : l10n.sectionPlannedWorkouts,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(formatter.duration(planned.durationMs),
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
              Text(
                planned.completedExerciseSessionId != null
                    ? l10n.plannedWorkoutCompleted
                    : l10n.plannedWorkoutBlocks(planned.blockCount),
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
          if (isActionable) ...[
            const SizedBox(width: 4),
            Icon(Icons.chevron_right,
                color: theme.colorScheme.onSurfaceVariant),
          ],
        ],
      ),
    );
    if (!isActionable) return row;
    return InkWell(onTap: () => onStart(planned.id), child: row);
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.title,
    required this.value,
    required this.subtitle,
    required this.icon,
    required this.accentColor,
    required this.series,
    required this.bucketLabels,
    required this.onTap,
  });

  final String title;
  final DisplayValue value;
  final String subtitle;
  final IconData icon;
  final Color accentColor;
  final List<double> series;
  final List<String> bucketLabels;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(icon, color: accentColor, size: 20),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(title,
                              style: theme.textTheme.titleSmall
                                  ?.copyWith(fontWeight: FontWeight.w600),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.baseline,
                      textBaseline: TextBaseline.alphabetic,
                      children: [
                        Flexible(
                          child: Text(value.value,
                              style: theme.textTheme.headlineSmall
                                  ?.copyWith(fontWeight: FontWeight.bold),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                        if (value.unit.isNotEmpty) ...[
                          const SizedBox(width: 4),
                          Text(value.unit,
                              style: theme.textTheme.titleMedium?.copyWith(
                                  color: theme.colorScheme.onSurfaceVariant)),
                        ],
                      ],
                    ),
                    Text(subtitle,
                        style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                width: 120,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    SizedBox(
                      height: 58,
                      child: SparklineChart(
                        values: series,
                        accentColor: accentColor,
                        singlePointLine: true,
                      ),
                    ),
                    if (bucketLabels.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          for (final label in bucketLabels)
                            Text(
                              label,
                              style: theme.textTheme.labelSmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.clip,
                            ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

