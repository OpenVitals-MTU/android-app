import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../di/providers.dart';
import '../../domain/insights/activity_splits.dart';
import '../../domain/model/activity_backfill.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/exercise_session_metrics.dart';
import '../../domain/model/heart_models.dart';
import '../../state/app_providers.dart';

part 'activity_detail_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityDetailUiState` — a single workout
/// plus its in-session heart-rate, speed and cadence samples, and the splits
/// derived from them.
@freezed
abstract class ActivityDetailState with _$ActivityDetailState {
  const ActivityDetailState._();

  const factory ActivityDetailState({
    @Default(true) bool isLoading,
    ScreenError? error,
    ExerciseData? workout,
    @Default(<HeartRateSample>[]) List<HeartRateSample> heartRateSamples,
    @Default(<SpeedSample>[]) List<SpeedSample> speedSamples,
    @Default(<ActivityCadenceSample>[])
    List<ActivityCadenceSample> cadenceSamples,
    @Default(ActivitySplits.none()) ActivitySplits splits,
  }) = _ActivityDetailState;
}

/// The Riverpod port of the Kotlin `ActivityDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `activityId` (so two stacked detail screens stay independent),
/// and [build] kicks off the first load. The marker/route-backfill slices the
/// Kotlin loads are still out of scope.
class ActivityDetailNotifier extends Notifier<ActivityDetailState> {
  ActivityDetailNotifier(this.activityId);

  final String activityId;
  int _generation = 0;

  @override
  ActivityDetailState build() {
    // The split distance is a user preference: changing it in settings must
    // re-cut the splits of a detail screen that is already open.
    ref.watch(activitySplitDistanceMetersProvider);
    Future.microtask(() {
      if (ref.mounted) _load();
    });
    return const ActivityDetailState();
  }

  Future<void> refresh() => _load();

  Future<void> _load() async {
    if (activityId.isEmpty) {
      state = const ActivityDetailState(
        isLoading: false,
        error: ScreenErrorMissingArgument(),
      );
      return;
    }

    final generation = ++_generation;
    final repo = ref.read(activityRepositoryProvider);
    final heartRepo = ref.read(heartRepositoryProvider);
    final splitDistanceMeters = ref.read(activitySplitDistanceMetersProvider);
    state = state.copyWith(isLoading: true, error: null);

    try {
      final workout = await repo.loadWorkout(activityId);
      if (!ref.mounted || generation != _generation) return;
      if (workout == null) {
        state = const ActivityDetailState(
          isLoading: false,
          error: ScreenErrorNotFound(),
        );
        return;
      }
      final heartRateSamples = await heartRepo.loadHeartRateSamplesInstant(
        workout.startTime,
        workout.endTime,
      );
      if (!ref.mounted || generation != _generation) return;
      final speedSamples = await _loadSpeedSamples(repo, workout);
      if (!ref.mounted || generation != _generation) return;
      final cadenceSamples = await _loadCadenceSamples(repo, workout);
      if (!ref.mounted || generation != _generation) return;

      // An ExerciseSessionRecord carries almost nothing: a watch writes the walk
      // as a session and its steps, distance, calories and elevation as SEPARATE
      // records over the same span. Read alone, the session has a duration and
      // little else — which is how a recorded walk came to report "Steps: Not
      // available" directly above a chart of its own step cadence. Aggregating
      // the session's window is the only way to get those numbers back.
      //
      // Health Connect first (it is the authority), then the samples, which fill
      // whatever is still missing — the averages, and a distance integrated from
      // speed when the device wrote no DistanceRecord at all.
      final sessionMetrics = await _loadWorkoutMetrics(repo, workout);
      if (!ref.mounted || generation != _generation) return;

      final backfilledWorkout = workout
          .withSessionMetricsBackfilled(sessionMetrics)
          .withSampleBackfilledMetrics(
            heartRateSamples: heartRateSamples,
            speedSamples: speedSamples,
            cadenceSamples: cadenceSamples,
          );

      state = ActivityDetailState(
        isLoading: false,
        workout: backfilledWorkout,
        heartRateSamples: heartRateSamples,
        speedSamples: speedSamples,
        cadenceSamples: cadenceSamples,
        splits: computeActivitySplits(
          workout: backfilledWorkout,
          heartRateSamples: heartRateSamples,
          speedSamples: speedSamples,
          splitDistanceMeters: splitDistanceMeters,
        ),
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = ActivityDetailState(
        isLoading: false,
        error:
            throwableToScreenError(error, fallback: 'Unable to load activity.'),
      );
    }
  }

  /// Speed samples are a NICE-TO-HAVE: they only upgrade a treadmill run's
  /// splits from estimated to integrated. The repository already returns an
  /// empty list when the SPEED permission is missing; this also swallows a
  /// failing read, because losing the splits card is not a reason to fail the
  /// whole activity screen.
  Future<List<SpeedSample>> _loadSpeedSamples(
    ActivityRepository repo,
    ExerciseData workout,
  ) async {
    try {
      return await repo.loadSpeedSamples(workout.startTime, workout.endTime);
    } catch (_) {
      return const <SpeedSample>[];
    }
  }

  /// The session-window aggregate. Every metric inside is already gated on its
  /// own read permission, so a missing grant costs that one number; a failing
  /// read costs all of them, and neither is a reason to fail the screen — the
  /// workout itself loaded fine.
  Future<ExerciseSessionMetrics> _loadWorkoutMetrics(
    ActivityRepository repo,
    ExerciseData workout,
  ) async {
    try {
      return await repo.loadWorkoutMetrics(workout.startTime, workout.endTime);
    } catch (_) {
      return ExerciseSessionMetrics.none;
    }
  }

  /// Cadence samples drive their own chart cards and nothing else, so — like the
  /// speed read above — a failure costs a card, not the screen.
  ///
  /// Health Connect tags each sample with the record it came from, so a ride
  /// yields cycling-kind samples and a run yields steps-kind ones; the screen
  /// renders whichever kinds actually came back rather than guessing from the
  /// exercise type.
  Future<List<ActivityCadenceSample>> _loadCadenceSamples(
    ActivityRepository repo,
    ExerciseData workout,
  ) async {
    try {
      return await repo.loadActivityCadenceSamples(
        workout.startTime,
        workout.endTime,
      );
    } catch (_) {
      return const <ActivityCadenceSample>[];
    }
  }
}
