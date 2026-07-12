import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import 'hydration_display.dart';

part 'hydration_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `HydrationUiState`, trimmed to the read-only
/// period detail: the scaffold-driven selection, the loaded daily totals +
/// entries, the resolved daily goal, the precomputed [HydrationDisplay], and
/// loading/error flags. The reminder-config and quick-add/edit fields are
/// Phase 6 concerns and are omitted.
@freezed
abstract class HydrationState with _$HydrationState {
  const factory HydrationState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(2.0) double dailyGoalLiters,
    @Default(<DailyHydration>[]) List<DailyHydration> dailyHydration,
    @Default(<HydrationEntry>[]) List<HydrationEntry> entries,
    HydrationDisplay? display,
  }) = _HydrationState;

  const HydrationState._();
}

/// The Riverpod port of the Kotlin `HydrationViewModel`.
///
/// A manual [Notifier] (no codegen) matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. Each pass loads the period totals + entries through
/// [LoadHydrationPeriodUseCase], reads the daily goal, and builds the display:
/// the period summary, the chart series, the drink breakdown and the beverage
/// history (Kotlin `HydrationPresentationMapper`). A monotonic [_generation]
/// guard drops stale results.
///
/// The display model is built here, at load time — the screen renders
/// [HydrationState.display] and derives nothing.
class HydrationViewModel extends Notifier<HydrationState> {
  int _generation = 0;

  @override
  HydrationState build() => HydrationState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadHydrationPeriod = ref.read(loadHydrationPeriodUseCaseProvider);
    // The daily goal is persisted configuration, not a health read, and it is
    // applied to the state *before* the load starts — so a goal just changed in
    // settings shows on the goal card at once, not a round-trip later. That is
    // why the read is synchronous (see [ReadHydrationDailyGoalUseCase]).
    final goal = ref.read(readHydrationDailyGoalUseCaseProvider)();

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
      dailyGoalLiters: goal,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    // The hydration/nutrition join that puts the drink names back onto the
    // entries is domain work, and lives in the use case.
    final result = await loadHydrationPeriod(query, refreshMode: refreshMode);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          error: null,
          dailyHydration: value.dailyHydration,
          entries: value.entries,
          display: buildHydrationDisplay(
            value.dailyHydration,
            value.entries,
            dailyGoalLiters: goal,
          ),
        );
      case Err(:final failure):
        state = state.copyWith(
          isLoading: false,
          error: failure.toScreenError(fallback: 'Unable to load data.'),
        );
    }
  }

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// The hydration screen's state provider. A manually-declared [NotifierProvider]
/// (no codegen), matching the activity template.
final hydrationProvider =
    NotifierProvider<HydrationViewModel, HydrationState>(HydrationViewModel.new);
