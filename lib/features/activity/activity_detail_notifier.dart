import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../di/providers.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/heart_models.dart';

part 'activity_detail_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityDetailUiState` — a single workout
/// plus its in-session heart-rate samples.
@freezed
abstract class ActivityDetailState with _$ActivityDetailState {
  const factory ActivityDetailState({
    @Default(true) bool isLoading,
    ScreenError? error,
    ExerciseData? workout,
    @Default(<HeartRateSample>[]) List<HeartRateSample> heartRateSamples,
  }) = _ActivityDetailState;
}

/// The Riverpod port of the Kotlin `ActivityDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `activityId` (so two stacked detail screens stay independent),
/// and [build] kicks off the first load. The speed/cadence/marker/route-backfill
/// slices the Kotlin loads are out of scope for this batch.
class ActivityDetailNotifier extends Notifier<ActivityDetailState> {
  ActivityDetailNotifier(this.activityId);

  final String activityId;
  int _generation = 0;

  @override
  ActivityDetailState build() {
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
      state = ActivityDetailState(
        isLoading: false,
        workout: workout,
        heartRateSamples: heartRateSamples,
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
}
