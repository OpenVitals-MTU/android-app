import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/query/activity_period_data.dart';

part 'calories_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `CaloriesUiState` — the calories overview
/// (burned + active + BMR) for the selected period.
@freezed
abstract class CaloriesState with _$CaloriesState {
  const CaloriesState._();

  const factory CaloriesState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    ActivityPeriodData? data,
    double? latestBmrKcal,
  }) = _CaloriesState;
}

/// The Riverpod port of the Kotlin `CaloriesViewModel`: loads the activity
/// period (steps + nutrition) alongside the latest basal metabolic rate. Driven
/// by the [MetricDetailScaffold] like the movement metrics.
class CaloriesNotifier extends Notifier<CaloriesState> {
  int _generation = 0;

  @override
  CaloriesState build() => CaloriesState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final activityRepo = ref.read(activityRepositoryProvider);
    final bodyRepo = ref.read(bodyRepositoryProvider);

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    try {
      final results = await (
        activityRepo.loadActivityPeriod(
          query,
          includeSteps: true,
          includeNutrition: true,
          refreshMode: refreshMode,
        ),
        bodyRepo.loadLatestBMR(),
      ).wait;
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        data: results.$1,
        latestBmrKcal: results.$2,
        error: null,
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

final caloriesNotifierProvider =
    NotifierProvider<CaloriesNotifier, CaloriesState>(CaloriesNotifier.new);
