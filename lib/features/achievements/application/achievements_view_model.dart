import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import 'achievements_display.dart';

// The stats, the badge progress and the display they are folded into are the
// screen's data model; it renders them straight out of here.
export 'achievements_display.dart';

part 'achievements_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `AchievementsUiState`: the loading/error
/// flags and the precomputed [AchievementsDisplay] the screen renders.
///
/// The badge list and the aggregate stats are read through the display, so a
/// screen that has not loaded yet still answers with the empty legacy window
/// the hand-written state used to build in its constructor.
@freezed
abstract class AchievementsState with _$AchievementsState {
  const AchievementsState._();

  const factory AchievementsState({
    @Default(true) bool isLoading,
    AchievementsDisplay? display,
    ScreenError? error,
  }) = _AchievementsState;

  List<AchievementProgress> get badges =>
      display?.badges ?? const <AchievementProgress>[];
  AchievementStats get stats => display?.stats ?? emptyAchievementStats();

  int get unlockedCount => display?.unlockedCount ?? 0;
  int get totalCount => display?.totalCount ?? 0;
  double get completionRatio => display?.completionRatio ?? 0.0;
  bool get hasActivityHistory => display?.hasActivityHistory ?? false;
  bool get hasFloorHistory => display?.hasFloorHistory ?? false;
}

/// The Riverpod port of the Kotlin `AchievementsViewModel`. Loads the full
/// legacy daily-steps history from the [ActivityRepository] and evaluates each
/// badge's earned/progress state from it — at load time, into
/// [AchievementsState.display]; the screen derives nothing.
class AchievementsViewModel extends Notifier<AchievementsState> {
  int _generation = 0;

  @override
  AchievementsState build() {
    // Kick off the first load (matching the Kotlin `init { load() }`).
    Future.microtask(load);
    return const AchievementsState();
  }

  Future<void> load() async {
    final generation = ++_generation;
    state = state.copyWith(isLoading: true, error: null);
    // How far back a lifetime badge counts is the use case's business; what the
    // badges make of the history is this screen's.
    final result = await ref.read(loadAchievementHistoryUseCaseProvider)();
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          display: buildAchievementsDisplay(
            value.days,
            value.start,
            value.end,
          ),
          error: null,
        );
      case Err(:final failure):
        state = state.copyWith(
          isLoading: false,
          error: failure.toScreenError(fallback: 'Unable to load data.'),
        );
    }
  }

  Future<void> refresh() => load();
}

/// The state provider for the achievements screen.
final achievementsProvider =
    NotifierProvider<AchievementsViewModel, AchievementsState>(
  AchievementsViewModel.new,
);
