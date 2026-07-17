import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/activity_detail_display.dart';
import '../application/activity_detail_view_model.dart';
import 'activity_heart_rate_chart_card.dart';
import 'activity_metric_relevance.dart';
import 'activity_session_metric_chart_cards.dart';
import 'activity_splits_card.dart';
import 'exercise_labels.dart';
import '../maps/route_map_view.dart';
import '../../../ui/components/section_padding.dart';

final DateFormat _dateTimeFormat = DateFormat('EEE d MMM yyyy · HH:mm');

/// Single-activity detail pushed over the shell (`/activity_detail/:activityId`),
/// ported from the Kotlin `ActivityDetailScreen` + `ActivityDetailCards`.
///
/// Each screen instance owns an auto-dispose [ActivityDetailViewModel] bound to
/// its [activityId], so stacked detail routes stay independent.
class ActivityDetailScreen extends ConsumerStatefulWidget {
  const ActivityDetailScreen({super.key, required this.activityId});

  final String activityId;

  @override
  ConsumerState<ActivityDetailScreen> createState() =>
      _ActivityDetailScreenState();
}

class _ActivityDetailScreenState extends ConsumerState<ActivityDetailScreen> {
  late final NotifierProvider<ActivityDetailViewModel, ActivityDetailState>
      _provider = NotifierProvider.autoDispose<ActivityDetailViewModel,
          ActivityDetailState>(
    () => ActivityDetailViewModel(widget.activityId),
  );

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(_provider);
    final formatter = ref.watch(unitFormatterProvider);
    final workout = state.workout;
    final title = workout == null
        ? 'Activity'
        : (workout.title?.trim().isNotEmpty == true
            ? workout.title!
            : exerciseTypeLabel(workout.exerciseType));

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: _body(state, formatter),
    );
  }

  Widget _body(ActivityDetailState state, UnitFormatter formatter) {
    if (state.isLoading && state.workout == null) {
      return const FullScreenLoading();
    }
    final error = state.error;
    if (error != null && state.workout == null) {
      return ErrorMessage(_errorText(error));
    }
    final workout = state.workout;
    final display = state.display;
    if (workout == null || display == null) {
      return const ErrorMessage('Activity not found.');
    }

    return RefreshIndicator(
      onRefresh: ref.read(_provider.notifier).refresh,
      child: ListView(
        padding: screenScrollPadding(context),
        children: [
          sectionPadded(_WorkoutSummaryCard(workout: workout, formatter: formatter)),
          sectionPadded(_MetricsCard(
            workout: workout,
            display: display,
            formatter: formatter,
          )),
          // Hidden entirely when there is nothing to split (a strength session).
          if (state.splits.isNotEmpty)
            sectionPadded(
              ActivitySplitsCard(
                splits: state.splits,
                formatter: formatter,
                splitDistanceMeters:
                    ref.watch(activitySplitDistanceMetersProvider),
                slowestPaceSeconds: display.slowestSplitPaceSeconds,
                fastestPaceSeconds: display.fastestSplitPaceSeconds,
              ),
            ),
          if (state.heartRateSamples.isNotEmpty)
            sectionPadded(
              ActivityHeartRateChartCard(
                samples: state.heartRateSamples,
                sessionStart: workout.startTime,
                sessionEnd: workout.endTime,
                unitFormatter: formatter,
              ),
            ),
          if (state.speedSamples.isNotEmpty)
            sectionPadded(
              ActivitySpeedChartCard(
                samples: state.speedSamples,
                sessionStart: workout.startTime,
                sessionEnd: workout.endTime,
                unitFormatter: formatter,
              ),
            )
          // No SpeedRecord — which is most watches. The splits know how far each
          // segment went and how long it took, so speed is rebuilt from them and
          // drawn as a step, one flat run per split. The two are exclusive by
          // construction: the display builds no trace when speed was recorded.
          else if (display.splitSpeedTrace case final trace?)
            sectionPadded(
              ActivitySplitSpeedChartCard(
                trace: trace,
                source: state.splits.source,
                splitDistanceMeters:
                    ref.watch(activitySplitDistanceMetersProvider),
                sessionStart: workout.startTime,
                sessionEnd: workout.endTime,
                unitFormatter: formatter,
              ),
            ),
          // The climb. It comes from the route's altitudes rather than from a
          // record of its own, because Health Connect has none — so an activity
          // recorded without GPS, or with a GPS that dropped its altitude, shows
          // the total it climbed and no profile. Decided at load time.
          if (display.elevationSamples.isNotEmpty)
            sectionPadded(
              ActivityElevationChartCard(
                samples: display.elevationSamples,
                sessionStart: workout.startTime,
                sessionEnd: workout.endTime,
                unitFormatter: formatter,
              ),
            ),
          // One card per cadence kind that actually recorded something: a ride
          // yields cycling samples, a run yields step samples, and neither has to
          // be inferred from the exercise type. Which kinds those are is decided
          // at load time, not here.
          for (final kind in display.cadenceKinds)
            sectionPadded(
              ActivityCadenceChartCard(
                samples: state.cadenceSamples,
                kind: kind,
                sessionStart: workout.startTime,
                sessionEnd: workout.endTime,
                unitFormatter: formatter,
              ),
            ),
          sectionPadded(_SessionDetailsCard(workout: workout)),
          if (workout.route.status == ExerciseRouteStatus.data &&
              workout.route.points.isNotEmpty)
            sectionPadded(_RouteMapCard(
              route: workout.route,
              distanceMeters: display.routeDistanceMeters,
            )),
          // Only a workout this app wrote can be edited or deleted; Health
          // Connect refuses to touch another app's records (Kotlin gated the
          // same two buttons on `isOpenVitalsEntry`).
          if (workout.isOpenVitalsEntry && workout.id.isNotEmpty)
            sectionPadded(_ActivityActions(
              isDeleting: state.isDeleting,
              onEdit: () => context
                  .push(AppRoutes.activityEntryEditLocation(workout.id)),
              onDelete: _deleteActivity,
            )),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Future<void> _deleteActivity() async {
    final messenger = ScaffoldMessenger.of(context);
    await ref.read(_provider.notifier).deleteActivity(() {
      if (mounted) context.pop();
    });
    // On success the screen has popped; only a failure leaves it mounted, and
    // then the error is worth a line the user cannot miss.
    if (!mounted) return;
    final error = ref.read(_provider).error;
    if (error != null) {
      messenger.showSnackBar(SnackBar(content: Text(_errorText(error))));
    }
  }
}

/// The edit / delete actions at the foot of an owned activity — Edit filled,
/// Delete outlined in the error color, both disabled mid-delete. Port of the
/// Kotlin `ActivityDetailContent` action buttons (no confirmation dialog).
class _ActivityActions extends StatelessWidget {
  const _ActivityActions({
    required this.isDeleting,
    required this.onEdit,
    required this.onDelete,
  });

  final bool isDeleting;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: isDeleting ? null : onEdit,
            icon: const Icon(Icons.edit_outlined),
            label: Text(l10n.actionEdit),
          ),
        ),
        const SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: isDeleting ? null : onDelete,
            icon: const Icon(Icons.delete_outline),
            label: Text(l10n.actionDelete),
            style: OutlinedButton.styleFrom(foregroundColor: scheme.error),
          ),
        ),
      ],
    );
  }
}


class _WorkoutSummaryCard extends StatelessWidget {
  const _WorkoutSummaryCard({required this.workout, required this.formatter});

  final ExerciseData workout;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: AppColors.workout.withValues(alpha: 0.15),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    exerciseTypeIcon(workout.exerciseType),
                    color: AppColors.workout,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    exerciseTypeLabel(workout.exerciseType),
                    style: theme.textTheme.titleMedium,
                  ),
                ),
                SourceChip(source: workout.source),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              formatter.duration(workout.durationMs),
              style: theme.textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: AppColors.workout,
              ),
            ),
            Text(
              '${_dateTimeFormat.format(workout.startTime.toLocal())} - '
              '${DateFormat('HH:mm').format(workout.endTime.toLocal())}',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricsCard extends StatelessWidget {
  const _MetricsCard({
    required this.workout,
    required this.display,
    required this.formatter,
  });

  final ExerciseData workout;
  final ActivityDetailDisplay display;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final type = workout.exerciseType;
    final distance = workout.totalDistanceMeters;
    final hasDistance = distance != null && distance > 0;
    final pausedMs = display.pausedDurationMs;

    final rows = <(String, String)>[];

    /// Shows [metric] when it HAS a value, and when it does not, only if its
    /// absence means something for this kind of activity — that is the whole
    /// fix for a bike ride reporting "Wheelchair pushes: Not available".
    /// Recorded data is never hidden: a non-null [value] always gets a row.
    void add(ActivityMetric metric, String label, String? value) {
      if (value == null && !isMetricRelevant(metric, type)) return;
      rows.add((label, value ?? l10n.notAvailable));
    }

    add(
      ActivityMetric.duration,
      l10n.detailDuration,
      formatter.duration(workout.durationMs),
    );
    // Only worth a row once some of the session was actually spent stopped.
    if (pausedMs > 0) {
      add(
        ActivityMetric.movingTime,
        l10n.detailMovingTime,
        formatter.duration(display.movingDurationMs),
      );
    }
    add(
      ActivityMetric.steps,
      l10n.metricSteps,
      workout.steps != null ? formatter.count(workout.steps!) : null,
    );
    add(
      ActivityMetric.distance,
      l10n.metricDistance,
      distance != null ? formatter.distance(distance).text : null,
    );
    // Pace and speed are the SAME fact rendered two ways (distance over duration),
    // so pace always has a value and the show-if-it-has-a-value rule would keep it
    // on every bike ride. Which of the two to render is a presentation choice, not
    // a question about the data, so it is gated on the type outright: a cyclist
    // reads km/h, a runner reads min/km. Not computing it is what hides the row.
    add(
      ActivityMetric.averagePace,
      l10n.metricAveragePace,
      hasDistance && isMetricRelevant(ActivityMetric.averagePace, type)
          ? formatter.averagePace(distance, workout.durationMs)?.text
          : null,
    );
    add(
      ActivityMetric.averageSpeed,
      l10n.metricAverageSpeed,
      hasDistance
          ? formatter.averageSpeed(distance, workout.durationMs).text
          : null,
    );
    // Distinct from "Average speed" above, which this app derives from distance
    // over duration. This is the figure the device itself recorded.
    add(
      ActivityMetric.recordedSpeed,
      l10n.metricRecordedSpeed,
      workout.averageSpeedMetersPerSecond != null
          ? formatter.speed(workout.averageSpeedMetersPerSecond!).text
          : null,
    );
    add(
      ActivityMetric.averageHeartRate,
      l10n.metricAverageHeartRate,
      workout.averageHeartRateBpm != null
          ? formatter.heartRate(workout.averageHeartRateBpm!).text
          : null,
    );
    add(
      ActivityMetric.averagePower,
      l10n.metricAveragePower,
      workout.averagePowerWatts != null
          ? formatter.power(workout.averagePowerWatts!).text
          : null,
    );
    add(
      ActivityMetric.stepsCadence,
      l10n.metricStepsCadence,
      workout.averageStepsCadenceRate != null
          ? formatter.stepsCadence(workout.averageStepsCadenceRate!).text
          : null,
    );
    add(
      ActivityMetric.cyclingCadence,
      l10n.metricCyclingCadence,
      workout.averageCyclingCadenceRpm != null
          ? formatter.cadence(workout.averageCyclingCadenceRpm!).text
          : null,
    );
    add(
      ActivityMetric.caloriesBurned,
      l10n.metricCaloriesBurned,
      switch (workout.totalCaloriesKcal) {
        // Flag an estimate as an estimate, rather than passing our own arithmetic
        // off as something the device measured (Kotlin `calories_estimated_value`).
        final kcal? when workout.totalCaloriesSource ==
                CaloriesBurnedSource.estimatedActiveAndBmr =>
          l10n.caloriesEstimatedValue(formatter.energy(kcal).text),
        final kcal? => formatter.energy(kcal).text,
        null => null,
      },
    );
    add(
      ActivityMetric.activeCalories,
      l10n.metricActiveCalories,
      workout.activeCaloriesKcal != null
          ? formatter.energy(workout.activeCaloriesKcal!).text
          : null,
    );
    add(
      ActivityMetric.floorsClimbed,
      l10n.metricFloorsClimbed,
      workout.floorsClimbed != null
          ? formatter.count(workout.floorsClimbed!)
          : null,
    );
    add(
      ActivityMetric.elevationGained,
      l10n.metricElevationGained,
      workout.elevationGainedMeters != null
          ? formatter.elevation(workout.elevationGainedMeters!).text
          : null,
    );
    add(
      ActivityMetric.wheelchairPushes,
      l10n.metricWheelchairPushes,
      workout.wheelchairPushes != null
          ? formatter.count(workout.wheelchairPushes!)
          : null,
    );

    return _DetailSectionCard(title: l10n.detailMetrics, rows: rows);
  }
}

class _SessionDetailsCard extends StatelessWidget {
  const _SessionDetailsCard({required this.workout});

  final ExerciseData workout;

  @override
  Widget build(BuildContext context) {
    const na = 'Not available';
    return _DetailSectionCard(
      title: 'Session details',
      rows: [
        ('Type', exerciseTypeLabel(workout.exerciseType)),
        ('Started', _dateTimeFormat.format(workout.startTime.toLocal())),
        ('Ended', _dateTimeFormat.format(workout.endTime.toLocal())),
        ('Source', workout.source),
        ('Record ID', workout.id),
        (
          'Notes',
          workout.notes?.trim().isNotEmpty == true ? workout.notes! : na,
        ),
      ],
    );
  }
}

/// The workout GPS route rendered on a [RouteMapView] (Kotlin `RouteCard` /
/// `OfflineRouteMapOrPreview`). The online raster base map is the default;
/// offline vector packs plug in inside [RouteMapView] (see its TODO).
class _RouteMapCard extends StatelessWidget {
  const _RouteMapCard({required this.route, required this.distanceMeters});

  final ExerciseRouteData route;

  /// Summed at load time; this card only prints it.
  final double distanceMeters;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.map_outlined,
                    color: theme.colorScheme.onSurfaceVariant),
                const SizedBox(width: 12),
                Text('Route', style: theme.textTheme.titleMedium),
                const Spacer(),
                if (distanceMeters > 0)
                  Text(
                    '${(distanceMeters / 1000).toStringAsFixed(2)} km',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            RouteMapView(points: route.points),
          ],
        ),
      ),
    );
  }
}

class _DetailSectionCard extends StatelessWidget {
  const _DetailSectionCard({required this.title, required this.rows});

  final String title;
  final List<(String, String)> rows;

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
            const SizedBox(height: 8),
            for (final row in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      flex: 2,
                      child: Text(
                        row.$1,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                    Expanded(
                      flex: 3,
                      child: Text(
                        row.$2,
                        textAlign: TextAlign.end,
                        style: theme.textTheme.bodyMedium,
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

String _errorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Activity not found.',
      ScreenErrorMissingArgument() => 'Missing activity id.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };
