import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/heart_models.dart';
import '../../state/app_providers.dart';
import '../../ui/components/loading_state.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'activity_detail_notifier.dart';
import 'exercise_labels.dart';
import 'maps/route_geometry.dart';
import 'maps/route_map_view.dart';

final DateFormat _dateTimeFormat = DateFormat('EEE d MMM yyyy · HH:mm');

/// Single-activity detail pushed over the shell (`/activity_detail/:activityId`),
/// ported from the Kotlin `ActivityDetailScreen` + `ActivityDetailCards`.
///
/// Each screen instance owns an auto-dispose [ActivityDetailNotifier] bound to
/// its [activityId], so stacked detail routes stay independent.
class ActivityDetailScreen extends ConsumerStatefulWidget {
  const ActivityDetailScreen({super.key, required this.activityId});

  final String activityId;

  @override
  ConsumerState<ActivityDetailScreen> createState() =>
      _ActivityDetailScreenState();
}

class _ActivityDetailScreenState extends ConsumerState<ActivityDetailScreen> {
  late final NotifierProvider<ActivityDetailNotifier, ActivityDetailState>
      _provider = NotifierProvider.autoDispose<ActivityDetailNotifier,
          ActivityDetailState>(
    () => ActivityDetailNotifier(widget.activityId),
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
    if (workout == null) {
      return const ErrorMessage('Activity not found.');
    }

    return RefreshIndicator(
      onRefresh: ref.read(_provider.notifier).refresh,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          _padded(_WorkoutSummaryCard(workout: workout, formatter: formatter)),
          _padded(_MetricsCard(workout: workout, formatter: formatter)),
          if (state.heartRateSamples.isNotEmpty)
            _padded(_HeartRateCard(samples: state.heartRateSamples)),
          _padded(_SessionDetailsCard(workout: workout)),
          if (workout.route.status == ExerciseRouteStatus.data &&
              workout.route.points.isNotEmpty)
            _padded(_RouteMapCard(route: workout.route)),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

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
  const _MetricsCard({required this.workout, required this.formatter});

  final ExerciseData workout;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    const na = 'Not available';
    final distance = workout.totalDistanceMeters;
    final pace = distance != null && distance > 0
        ? formatter.averagePace(distance, workout.durationMs)?.text
        : null;
    final speed = distance != null && distance > 0
        ? formatter.averageSpeed(distance, workout.durationMs).text
        : null;
    return _DetailSectionCard(
      title: 'Metrics',
      rows: [
        ('Duration', formatter.duration(workout.durationMs)),
        ('Steps', workout.steps != null ? formatter.count(workout.steps!) : na),
        ('Distance', distance != null ? formatter.distance(distance).text : na),
        ('Average pace', pace ?? na),
        ('Average speed', speed ?? na),
        (
          'Average heart rate',
          workout.averageHeartRateBpm != null
              ? formatter.heartRate(workout.averageHeartRateBpm!).text
              : na,
        ),
        (
          'Calories burned',
          workout.totalCaloriesKcal != null
              ? formatter.energy(workout.totalCaloriesKcal!).text
              : na,
        ),
        (
          'Active calories',
          workout.activeCaloriesKcal != null
              ? formatter.energy(workout.activeCaloriesKcal!).text
              : na,
        ),
        (
          'Floors climbed',
          workout.floorsClimbed != null
              ? formatter.count(workout.floorsClimbed!)
              : na,
        ),
        (
          'Elevation gained',
          workout.elevationGainedMeters != null
              ? formatter.elevation(workout.elevationGainedMeters!).text
              : na,
        ),
        (
          'Wheelchair pushes',
          workout.wheelchairPushes != null
              ? formatter.count(workout.wheelchairPushes!)
              : na,
        ),
      ],
    );
  }
}

class _HeartRateCard extends StatelessWidget {
  const _HeartRateCard({required this.samples});

  final List<HeartRateSample> samples;

  @override
  Widget build(BuildContext context) {
    final bpms = samples.map((s) => s.beatsPerMinute).toList();
    final min = bpms.reduce((a, b) => a < b ? a : b);
    final max = bpms.reduce((a, b) => a > b ? a : b);
    final avg = (bpms.reduce((a, b) => a + b) / bpms.length).round();
    return _DetailSectionCard(
      title: 'Heart rate',
      rows: [
        ('Samples', '${samples.length}'),
        ('Average', '$avg bpm'),
        ('Min', '$min bpm'),
        ('Max', '$max bpm'),
      ],
    );
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
  const _RouteMapCard({required this.route});

  final ExerciseRouteData route;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final distanceMeters = routeTotalDistanceMeters(route.points);
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
