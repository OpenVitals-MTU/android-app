import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_titles.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/cardio_load.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/charts/sparkline_chart.dart';
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
import '../../../ui/components/swipe_to_delete_entry_row.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/activities_display.dart';
import '../application/activities_view_model.dart';
import 'exercise_labels.dart';
import '../../../ui/components/section_padding.dart';
import '../../../ui/components/accent_icon_chip.dart';

final DateFormat _rowTimeFormat = DateFormat('EEE d MMM · HH:mm');

/// The reorderable body of the activities aggregate screen — a port of Kotlin
/// `renderActivitiesOrderedContent` (`ActivitiesMetricOrderedSections.kt`).
/// Renders every section in the user's persisted order, gated by the same
/// visibility rules Kotlin uses.
///
/// Everything it prints arrives precomputed on [display] (built by
/// [ActivitiesViewModel] at load time): this file sorts, folds, buckets and
/// averages nothing.
class ActivitiesOrderedSections extends ConsumerWidget {
  const ActivitiesOrderedSections({
    super.key,
    required this.state,
    required this.display,
    required this.period,
    required this.daySelection,
  });

  final ActivitiesState state;
  final ActivitiesDisplay display;
  final DatePeriod period;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final formatter = ref.watch(unitFormatterProvider);
    final notifier = ref.read(activitiesProvider.notifier);
    final weekPeriodMode = ref.watch(weekPeriodModeProvider);

    final hasWorkouts = display.workoutCount > 0;
    final selectedDay = daySelection.selectedDate;

    return OrderedMetricDetailSections(
      sections: [
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          visible: hasWorkouts ||
              display.hasOverviewDays ||
              display.sortedPlannedWorkouts.isNotEmpty ||
              !state.isLoading,
          _summarySection(context, formatter, notifier, weekPeriodMode),
        ),
        MetricDetailSection(
          MetricDetailSectionId.activityKeyMetrics,
          visible: display.hasOverviewDays && display.totals != null,
          display.totals == null
              ? const SizedBox.shrink()
              : _keyMetricsSection(
                  context, l10n, formatter, display.totals!, weekPeriodMode),
        ),
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: hasWorkouts,
          sectionPadded(_periodChart(formatter, weekPeriodMode)),
        ),
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null && hasWorkouts,
          selectedDay == null
              ? const SizedBox.shrink()
              : sectionPadded(_selectedDayEntries(
                  context, notifier, formatter, selectedDay)),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          visible: hasWorkouts,
          sectionPadded(DailyGoalCard(
            goal: formatter.minutes(state.dailyGoalMinutes.round()),
            progress: display.goalProgress,
            icon: Icons.directions_run,
            accentColor: AppColors.workout,
            onDecreaseGoal: notifier.decreaseDailyGoal,
            onIncreaseGoal: notifier.increaseDailyGoal,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          visible: hasWorkouts,
          _statisticsSection(l10n, formatter),
        ),
        MetricDetailSection(
          MetricDetailSectionId.metricContext,
          visible: hasWorkouts && display.guideline != null,
          _guidelineSection(l10n, formatter),
        ),
        MetricDetailSection(
          MetricDetailSectionId.crossMetricInsights,
          visible: hasWorkouts && display.crossInsight != null,
          _crossMetricSection(l10n),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: hasWorkouts && period.start != period.end,
          sectionPadded(DataConfidenceCard(
            confidence: display.dataConfidence,
            accentColor: AppColors.workout,
          )),
        ),
      ],
    );
  }

  // ── ACTIVITY_SUMMARY ────────────────────────────────────────────────

  Widget _summarySection(
    BuildContext context,
    UnitFormatter formatter,
    ActivitiesViewModel notifier,
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
            options: display.filterOptions,
            onSelect: notifier.selectActivityType,
          )),
        if (state.workouts.isNotEmpty ||
            display.hasOverviewDays ||
            !state.isLoading)
          sectionPadded(_ActivityPeriodSummaryCard(
            state: state,
            display: display,
            period: period,
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
        if (display.sortedPlannedWorkouts.isNotEmpty)
          sectionPadded(_PlannedWorkoutCard(
            plannedWorkouts: display.sortedPlannedWorkouts,
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
    ActivityOverviewTotals totals,
    WeekPeriodMode weekPeriodMode,
  ) {
    final title = periodTitle(
      l10n,
      state.selectedRange,
      period,
      weekPeriodMode: weekPeriodMode,
    );
    final bucketLabels = _bucketLabels(
      display.bucketDates,
      state.selectedRange,
      Localizations.localeOf(context).toString(),
    );
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
          series: display.cardioLoadSeries,
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.cardioLoadDetail),
        ),
        _MetricCard(
          title: l10n.metricEnergyBurned,
          value: totals.hasEnergyBurned
              ? formatter.energy(totals.energyBurnedKcal)
              : DisplayValue(l10n.noData, ''),
          subtitle:
              display.energyEstimated ? l10n.caloriesEstimatedActiveBmr : title,
          icon: Icons.local_fire_department,
          accentColor: AppColors.calories,
          series: display.energyBurnedSeries,
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.calories),
        ),
        _MetricCard(
          title: l10n.metricSteps,
          value: DisplayValue(formatter.count(totals.steps), ''),
          subtitle: title,
          icon: Icons.directions_walk,
          accentColor: AppColors.steps,
          series: display.stepsSeries,
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.metricLocation('STEPS')),
        ),
        _MetricCard(
          title: l10n.metricDistance,
          value: formatter.distance(totals.distanceMeters),
          subtitle: title,
          icon: Icons.straighten,
          accentColor: AppColors.distance,
          series: display.distanceSeries,
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
          series: display.hrvSeries,
          bucketLabels: bucketLabels,
          onTap: () => context.push(AppRoutes.metricLocation('HRV')),
        ),
      ],
    );
  }

  // ── PERIOD_CHART ────────────────────────────────────────────────────

  Widget _periodChart(UnitFormatter formatter, WeekPeriodMode weekPeriodMode) =>
      MetricBarChart(
        title: '',
        values: display.chartValues,
        selectedRange: state.selectedRange,
        period: period,
        accentColor: AppColors.workout,
        summaryValue: formatter.duration(display.totalDurationMs),
        weekPeriodMode: weekPeriodMode,
        selectedDate: daySelection.selectedDate,
        onDateSelected: daySelection.onDateSelected,
        valueFormatter: (value) => formatter.minutes(value.round()).text,
      );

  // ── SELECTED_DAY_ENTRIES ────────────────────────────────────────────

  Widget _selectedDayEntries(
    BuildContext context,
    ActivitiesViewModel notifier,
    UnitFormatter formatter,
    LocalDate selectedDay,
  ) {
    final locale = Localizations.localeOf(context).toLanguageTag();
    return PaginatedEntryList<ExerciseData>(
      title: DateFormat.yMMMd(locale).format(
        DateTime(selectedDay.year, selectedDay.month, selectedDay.day),
      ),
      entries: display.workoutsByDay[selectedDay] ?? const <ExerciseData>[],
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

  Widget _statisticsSection(AppLocalizations l10n, UnitFormatter formatter) {
    final goalProgress = display.goalProgress;
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
              value: formatter.duration(display.totalDurationMs),
              unit: '',
              icon: Icons.directions_run,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.sectionActivities,
              value: formatter.count(display.workoutCount),
              unit: '',
              icon: Icons.check_circle_outline,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.statAverageDuration,
              value: formatter.duration(display.averageDurationMs),
              unit: '',
              icon: Icons.star_outline,
              accentColor: AppColors.workout,
            ),
            InsightStat(
              title: l10n.statLongestWorkout,
              value: formatter.duration(display.longestDurationMs),
              unit: '',
              icon: Icons.calendar_month_outlined,
              accentColor: AppColors.workout,
            ),
            previousPeriodInsightStat(
              comparison: display.periodComparison,
              selectedRange: state.selectedRange,
              unitFormatter: formatter,
              valueFormatter: (value) =>
                  DisplayValue(formatter.duration(value.round()), ''),
              accentColor: AppColors.workout,
              l10n: l10n,
            ),
            ...personalBaselineInsightStats(
              insight: display.baselineInsight,
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
    final progress = display.guideline;
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
          body: display.guidelineUsesWeeklyAverage
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

  Widget _crossMetricSection(AppLocalizations l10n) {
    final insight = display.crossInsight;
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
}

// ── Bucket labels (locale formatting; the buckets themselves are precomputed) ──

/// A filled circle carrying the workout's own icon on the days you trained; an
/// empty ring on the days you did not. Port of Kotlin `ActivityOverviewStrip`.
///
/// This was simply never ported — the Flutter activities screen used the buckets
/// for its sparklines and dropped the strip, so the week view lost the one thing
/// that showed WHICH days you were active.
class _ActivityOverviewStrip extends StatelessWidget {
  const _ActivityOverviewStrip({
    required this.markers,
    required this.selectedRange,
  });

  final List<ActivityStripMarker> markers;
  final TimeRange selectedRange;

  /// Kotlin `ActivityOverviewMarkerSize`.
  static const double _markerSize = 38;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final locale = Localizations.localeOf(context).toString();
    return ColoredBox(
      color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.36),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        child: Row(
          children: [
            for (final marker in markers)
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _Marker(
                      key: ValueKey(
                        'activity-day-marker-${marker.date.toString()}-'
                        '${marker.workout == null ? 'rest' : 'active'}',
                      ),
                      workout: marker.workout,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      _bucketLabel(marker.date, selectedRange, locale),
                      maxLines: 1,
                      overflow: TextOverflow.clip,
                      style: theme.textTheme.labelMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: theme.colorScheme.onSurfaceVariant,
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

class _Marker extends StatelessWidget {
  const _Marker({super.key, required this.workout});

  /// Null on a day with no workout — that day gets the empty ring.
  final ExerciseData? workout;

  @override
  Widget build(BuildContext context) {
    final activity = workout;
    if (activity == null) {
      return Container(
        width: _ActivityOverviewStrip._markerSize,
        height: _ActivityOverviewStrip._markerSize,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: AppColors.workout, width: 2),
        ),
      );
    }
    return Container(
      width: _ActivityOverviewStrip._markerSize,
      height: _ActivityOverviewStrip._markerSize,
      decoration: const BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.workout,
      ),
      child: Icon(
        exerciseTypeIcon(activity.exerciseType),
        size: 22,
        color: Colors.white,
      ),
    );
  }
}

/// The per-bucket labels under a key-metric sparkline (Kotlin
/// `activityOverviewBucketLabel`): DAY & WEEK → the weekday's initial;
/// MONTH → day-of-month; YEAR → the month's initial. Uses [locale].
List<String> _bucketLabels(
  List<LocalDate> bucketDates,
  TimeRange range,
  String locale,
) =>
    [for (final date in bucketDates) _bucketLabel(date, range, locale)];

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
    required this.options,
    required this.onSelect,
  });

  final int? selectedActivityType;

  /// Already unioned with the selection and ordered by label (by the view-model).
  final List<int> options;
  final ValueChanged<int?> onSelect;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
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
    required this.display,
    required this.period,
    required this.formatter,
    required this.weekPeriodMode,
    required this.showEmptyState,
    required this.onOpen,
    required this.onDelete,
  });

  final ActivitiesState state;
  final ActivitiesDisplay display;
  final DatePeriod period;
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
    // The week strip: one marker per day, filled with the workout's own icon on
    // the days you trained and left as an empty ring on the days you did not.
    // Week only — a month of 31 rings says nothing, which is why Kotlin gated it
    // on WEEK too (`stripBuckets`); the view-model returns an empty list for
    // every other range.
    final stripMarkers = widget.display.stripMarkers;
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
              if (stripMarkers.isNotEmpty)
                _ActivityOverviewStrip(
                  markers: stripMarkers,
                  selectedRange: widget.state.selectedRange,
                ),
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

  /// Already ordered earliest-first by the view-model.
  final List<PlannedExerciseData> plannedWorkouts;
  final UnitFormatter formatter;
  final ValueChanged<String> onStart;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionPlannedWorkouts),
        OpenVitalsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var i = 0; i < plannedWorkouts.length; i++) ...[
                _plannedRow(context, l10n, plannedWorkouts[i]),
                if (i < plannedWorkouts.length - 1)
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
