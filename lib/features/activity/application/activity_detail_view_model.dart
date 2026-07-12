import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/activity_splits.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../state/app_providers.dart';
import 'activity_detail_display.dart';

part 'activity_detail_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityDetailUiState` — a single workout
/// plus its in-session heart-rate, speed and cadence samples, the splits derived
/// from them, and the precomputed [ActivityDetailDisplay] the cards render.
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
    ActivityDetailDisplay? display,
  }) = _ActivityDetailState;
}

/// The Riverpod port of the Kotlin `ActivityDetailViewModel`.
///
/// One instance per detail screen: the screen creates an auto-dispose provider
/// bound to its `activityId` (so two stacked detail screens stay independent),
/// and [build] kicks off the first load. The marker/route-backfill slices the
/// Kotlin loads are still out of scope.
class ActivityDetailViewModel extends Notifier<ActivityDetailState> {
  ActivityDetailViewModel(this.activityId);

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
    final loadActivityDetail = ref.read(loadActivityDetailUseCaseProvider);
    final splitDistanceMeters = ref.read(activitySplitDistanceMetersProvider);
    state = state.copyWith(isLoading: true, error: null);

    // Reassembling the workout — which repositories to ask, in what order, and
    // that Health Connect's own totals outrank the ones derived from samples —
    // is domain knowledge, and lives in the use case. What stays here is the
    // view's business: the generation guard, and turning a result into state.
    final result = await loadActivityDetail(activityId);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        if (value == null) {
          state = const ActivityDetailState(
            isLoading: false,
            error: ScreenErrorNotFound(),
          );
          return;
        }
        // Splits depend on a user preference, so they are cut here rather than in
        // the use case: changing the split distance must re-cut an open screen.
        final splits = computeActivitySplits(
          workout: value.workout,
          heartRateSamples: value.heartRateSamples,
          speedSamples: value.speedSamples,
          splitDistanceMeters: splitDistanceMeters,
        );
        state = ActivityDetailState(
          isLoading: false,
          workout: value.workout,
          heartRateSamples: value.heartRateSamples,
          speedSamples: value.speedSamples,
          cadenceSamples: value.cadenceSamples,
          splits: splits,
          display: buildActivityDetailDisplay(
            workout: value.workout,
            cadenceSamples: value.cadenceSamples,
            // Whether speed was RECORDED decides whether it has to be rebuilt
            // from the splits, so the display needs the trace, not just a flag.
            speedSamples: value.speedSamples,
            splits: splits,
          ),
        );
      case Err(:final failure):
        state = ActivityDetailState(
          isLoading: false,
          error: failure.toScreenError(fallback: 'Unable to load activity.'),
        );
    }
  }
}
