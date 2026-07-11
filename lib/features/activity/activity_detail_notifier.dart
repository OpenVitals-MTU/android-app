import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../di/providers.dart';
import '../../domain/insights/activity_splits.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/heart_models.dart';
import '../../state/app_providers.dart';

part 'activity_detail_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityDetailUiState` — a single workout
/// plus its in-session heart-rate and speed samples, and the splits derived
/// from them.
@freezed
abstract class ActivityDetailState with _$ActivityDetailState {
  const ActivityDetailState._();

  const factory ActivityDetailState({
    @Default(true) bool isLoading,
    ScreenError? error,
    ExerciseData? workout,
    @Default(<HeartRateSample>[]) List<HeartRateSample> heartRateSamples,
    @Default(<SpeedSample>[]) List<SpeedSample> speedSamples,
    @Default(ActivitySplits.none()) ActivitySplits splits,
  }) = _ActivityDetailState;
}

/// The Riverpod port of the Kotlin `ActivityDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `activityId` (so two stacked detail screens stay independent),
/// and [build] kicks off the first load. The cadence/marker/route-backfill
/// slices the Kotlin loads are out of scope for this batch.
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

      state = ActivityDetailState(
        isLoading: false,
        workout: workout,
        heartRateSamples: heartRateSamples,
        speedSamples: speedSamples,
        splits: computeActivitySplits(
          workout: workout,
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
}
