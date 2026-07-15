import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_calculations.dart';
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
import '../../../domain/usecase/load_nutrition_period_use_case.dart';
import '../presentation/nutrition_metric.dart';
import 'nutrition_display.dart';

part 'nutrition_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `NutritionUiState`, trimmed to the read-only
/// period detail: the scaffold-driven selection, the loaded daily macros +
/// entries, the resolved daily goal for the keyed metric, the precomputed
/// [NutritionDisplay], and loading/error flags.
@freezed
abstract class NutritionState with _$NutritionState {
  const factory NutritionState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(2000.0) double dailyGoal,
    @Default(<DailyMacros>[]) List<DailyMacros> dailyMacros,
    @Default(<DailyMacros>[]) List<DailyMacros> previousDailyMacros,
    @Default(<DailyMacros>[]) List<DailyMacros> baselineDailyMacros,
    @Default(<NutritionEntry>[]) List<NutritionEntry> entries,
    NutritionDisplay? display,
  }) = _NutritionState;

  const NutritionState._();
}

/// The Riverpod port of the Kotlin `NutritionViewModel`, shared across the four
/// keyed nutrition metrics (keyed by [metric]); the overview/nutrient-breakdown
/// screen reuses the [NutritionMetric.caloriesIn] instance for its data.
///
/// A manual [Notifier] (no codegen) matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. A monotonic [_generation] guard drops stale results.
///
/// The display model is built here, at load time — the screens render
/// [NutritionState.display] and derive nothing (the Kotlin
/// `NutritionPresentationMapper` discipline). A nudged daily goal rebuilds it
/// too: the goal card counts the same loaded days against a new target.
class NutritionViewModel extends Notifier<NutritionState>
    with PeriodMetricLoader<NutritionState, NutritionPeriodLoadResult> {
  NutritionViewModel(this.metric);

  final NutritionMetric metric;

  @override
  NutritionState build() => NutritionState(
        selectedDate: LocalDate.now(),
        dailyGoal: ref
            .read(preferencesRepositoryProvider)
            .dailyGoalFor(metric.dailyGoalKey),
      );

  /// Kotlin `NutritionViewModel.increaseDailyGoal` / `decreaseDailyGoal`: step
  /// the goal by its metric's step, clamped to the metric's own bounds, and
  /// persist.
  void _nudgeDailyGoal(double delta) {
    final key = metric.dailyGoalKey;
    final next = key.normalize(state.dailyGoal + delta);
    if (next == state.dailyGoal) return;
    ref.read(preferencesRepositoryProvider).setDailyGoalFor(key, next);
    state = state.copyWith(dailyGoal: next);
    // The goal progress and the goal statistics are part of the display, so a
    // moved goal has to rebuild it — no reload, the same loaded days.
    _rebuildDisplay();
  }

  /// The screen-ready derivation of the loaded period under the current goal.
  /// The one place a [NutritionDisplay] is made. Takes [state] explicitly so the
  /// mixin's [onLoadSuccess] can derive it off the freshly-built state.
  NutritionDisplay _display(NutritionState state) => buildNutritionDisplay(
        nutrient: metric.nutrient,
        goalDirection: metric.dailyGoalKey.direction,
        dailyGoal: state.dailyGoal,
        period: displayPeriodFor(
          state.selectedRange,
          state.selectedDate,
          weekPeriodMode:
              ref.read(preferencesRepositoryProvider).weekPeriodMode,
        ),
        dailyMacros: state.dailyMacros,
        previousDailyMacros: state.previousDailyMacros,
        baselineDailyMacros: state.baselineDailyMacros,
        entries: state.entries,
      );

  void _rebuildDisplay() {
    if (state.display == null) return;
    state = state.copyWith(display: _display(state));
  }

  void increaseDailyGoal() => _nudgeDailyGoal(metric.dailyGoalKey.step);

  void decreaseDailyGoal() => _nudgeDailyGoal(-metric.dailyGoalKey.step);

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  PeriodSelection selectionOf(NutritionState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  NutritionState onLoadStart(
    NutritionState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
      dailyGoal: ref
          .read(preferencesRepositoryProvider)
          .dailyGoalFor(metric.dailyGoalKey),
    );
    return navigated ? next.copyWith(display: null) : next;
  }

  @override
  Future<Result<NutritionPeriodLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      // Three windows, not one: the statistics section needs the previous and
      // baseline macros to compare against — see [LoadNutritionPeriodUseCase].
      ref.read(loadNutritionPeriodUseCaseProvider)(query,
          refreshMode: refreshMode);

  @override
  NutritionState onLoadSuccess(
    NutritionState state,
    NutritionPeriodLoadResult value,
    PeriodLoadQuery query,
  ) {
    final loaded = state.copyWith(
      isLoading: false,
      error: null,
      dailyMacros: value.dailyMacros,
      previousDailyMacros: value.previousDailyMacros,
      baselineDailyMacros: value.baselineDailyMacros,
      entries: value.entries,
    );
    return loaded.copyWith(display: _display(loaded));
  }

  @override
  NutritionState onLoadError(NutritionState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// One [NotifierProvider] per keyed nutrition metric, built eagerly so each
/// metric's state survives across screen rebuilds.
final Map<NutritionMetric, NotifierProvider<NutritionViewModel, NutritionState>>
    _nutritionMetricProviders = {
  for (final metric in NutritionMetric.values)
    metric: NotifierProvider<NutritionViewModel, NutritionState>(
      () => NutritionViewModel(metric),
    ),
};

/// The state provider for [metric]'s period detail screen.
NotifierProvider<NutritionViewModel, NutritionState> nutritionMetricProvider(
  NutritionMetric metric,
) =>
    _nutritionMetricProviders[metric]!;
