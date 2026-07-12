import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../presentation/nutrition_metric.dart';

part 'nutrition_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `NutritionUiState`, trimmed to the read-only
/// period detail: the scaffold-driven selection, the loaded daily macros +
/// entries, the resolved daily goal for the keyed metric, and loading/error
/// flags. The per-nutrient derivations (series, goal progress, statistics) are
/// computed on demand by the screen (the Kotlin `NutritionPresentationMapper`).
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
  }) = _NutritionState;

  const NutritionState._();

  bool get hasData =>
      entries.isNotEmpty ||
      dailyMacros.any((day) =>
          day.energyKcal > 0.0 ||
          day.proteinGrams > 0.0 ||
          day.carbsGrams > 0.0 ||
          day.fatGrams > 0.0 ||
          day.nutrientValues.values.any((value) => value > 0.0));
}

/// The Riverpod port of the Kotlin `NutritionViewModel`, shared across the four
/// keyed nutrition metrics (keyed by [metric]); the overview/nutrient-breakdown
/// screen reuses the [NutritionMetric.caloriesIn] instance for its data.
///
/// A manual [Notifier] (no codegen) matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. A monotonic [_generation] guard drops stale results.
class NutritionViewModel extends Notifier<NutritionState> {
  NutritionViewModel(this.metric);

  final NutritionMetric metric;
  int _generation = 0;

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
  }

  void increaseDailyGoal() => _nudgeDailyGoal(metric.dailyGoalKey.step);

  void decreaseDailyGoal() => _nudgeDailyGoal(-metric.dailyGoalKey.step);

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadNutritionPeriod = ref.read(loadNutritionPeriodUseCaseProvider);
    final goal = prefs.dailyGoalFor(metric.dailyGoalKey);

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
      dailyGoal: goal,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    try {
      // Three windows, not one: the statistics section needs the previous and
      // baseline macros to compare against — see [LoadNutritionPeriodUseCase].
      final result = await loadNutritionPeriod(query, refreshMode: refreshMode);
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: null,
        dailyMacros: result.dailyMacros,
        previousDailyMacros: result.previousDailyMacros,
        baselineDailyMacros: result.baselineDailyMacros,
        entries: result.entries,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
      );
    }
  }

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
