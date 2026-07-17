import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/period_metric_loader.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/usecase/load_hydration_period_use_case.dart';
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
class HydrationViewModel extends Notifier<HydrationState>
    with PeriodMetricLoader<HydrationState, HydrationPeriodLoadResult> {
  @override
  HydrationState build() => HydrationState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  PeriodSelection selectionOf(HydrationState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  HydrationState onLoadStart(
    HydrationState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    // The daily goal is persisted configuration, not a health read, and it is
    // applied to the state *before* the load starts — so a goal just changed in
    // settings shows on the goal card at once, not a round-trip later. That is
    // why the read is synchronous (see [ReadHydrationDailyGoalUseCase]).
    final goal = ref.read(readHydrationDailyGoalUseCaseProvider)();
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
      dailyGoalLiters: goal,
    );
    return navigated ? next.copyWith(display: null) : next;
  }

  @override
  Future<Result<HydrationPeriodLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      // The hydration/nutrition join that puts the drink names back onto the
      // entries is domain work, and lives in the use case.
      ref.read(loadHydrationPeriodUseCaseProvider)(query,
          refreshMode: refreshMode);

  @override
  HydrationState onLoadSuccess(
    HydrationState state,
    HydrationPeriodLoadResult value,
    PeriodLoadQuery query,
  ) =>
      state.copyWith(
        isLoading: false,
        error: null,
        dailyHydration: value.dailyHydration,
        entries: value.entries,
        display: buildHydrationDisplay(
          value.dailyHydration,
          value.entries,
          dailyGoalLiters: state.dailyGoalLiters,
          period: query.windows.current,
          today: LocalDate.now(),
        ),
      );

  @override
  HydrationState onLoadError(HydrationState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );

  /// Port of the Kotlin `HydrationViewModel.deleteHydrationEntry`: remove the
  /// entry optimistically so the row leaves the list at once, delete it through
  /// the repository — a hydration record (which also clears its paired nutrition
  /// record) or a nutrition-only record, by the entry's own type — then
  /// force-reload the period; restore the previous state (with an error) on
  /// failure.
  Future<void> deleteHydrationEntry(String entryId) async {
    if (entryId.isEmpty) return;
    final entry = _entryById(entryId);
    if (entry == null || !entry.isOpenVitalsEntry) return;

    final previous = state;
    final remaining = [
      for (final e in state.entries)
        if (e.id != entryId) e,
    ];
    // Rebuild the display off the trimmed list so the beverage history the
    // screen renders drops the row synchronously — the Dismissible requires it
    // gone before the next frame. The daily totals stay until the force-reload
    // below corrects them, the same as the other metrics.
    final query = PeriodLoadQuery(
      range: state.selectedRange,
      anchorDate: state.selectedDate,
      weekPeriodMode: ref.read(preferencesRepositoryProvider).weekPeriodMode,
    );
    state = state.copyWith(
      entries: remaining,
      error: null,
      display: buildHydrationDisplay(
        state.dailyHydration,
        remaining,
        dailyGoalLiters: state.dailyGoalLiters,
        period: query.windows.current,
        today: LocalDate.now(),
      ),
    );

    final deletion = switch (entry.recordType) {
      HydrationEntryRecordType.nutritionOnly =>
        await ref.read(deleteNutritionEntryUseCaseProvider)(entryId),
      _ => await ref.read(deleteHydrationEntryUseCaseProvider)(entryId),
    };
    if (!ref.mounted) return;
    switch (deletion) {
      case Ok():
        await load(
          PeriodSelection(state.selectedRange, state.selectedDate),
          refreshMode: RefreshMode.force,
        );
      case Err(:final failure):
        state = previous.copyWith(
          error: failure.toScreenError(fallback: 'Unable to load data.'),
        );
    }
  }

  HydrationEntry? _entryById(String entryId) {
    for (final entry in state.entries) {
      if (entry.id == entryId) return entry;
    }
    return null;
  }
}

/// The hydration screen's state provider. A manually-declared [NotifierProvider]
/// (no codegen), matching the activity template.
final hydrationProvider =
    NotifierProvider<HydrationViewModel, HydrationState>(HydrationViewModel.new);
