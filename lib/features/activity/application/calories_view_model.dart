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
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';
import '../../../domain/usecase/load_calories_use_case.dart';
import 'calories_display.dart';

part 'calories_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `CaloriesUiState` — the calories overview
/// (burned + active + BMR) for the selected period, plus the precomputed
/// [CaloriesDisplay] the screen renders.
@freezed
abstract class CaloriesState with _$CaloriesState {
  const CaloriesState._();

  const factory CaloriesState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    ActivityPeriodData? data,
    CaloriesDisplay? display,
    double? latestBmrKcal,
  }) = _CaloriesState;
}

/// The Riverpod port of the Kotlin `CaloriesViewModel`: loads the activity
/// period (steps + nutrition) alongside the latest basal metabolic rate. Driven
/// by the [MetricDetailScaffold] like the movement metrics.
class CaloriesViewModel extends Notifier<CaloriesState>
    with PeriodMetricLoader<CaloriesState, CaloriesLoadResult> {
  @override
  CaloriesState build() => CaloriesState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  PeriodSelection selectionOf(CaloriesState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  CaloriesState onLoadStart(
    CaloriesState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
    );
    // The BMR reading belongs to the old window too — null it so no stale number
    // shows under the skeleton.
    return navigated
        ? next.copyWith(data: null, display: null, latestBmrKcal: null)
        : next;
  }

  @override
  Future<Result<CaloriesLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      ref.read(loadCaloriesUseCaseProvider)(query, refreshMode: refreshMode);

  @override
  CaloriesState onLoadSuccess(
    CaloriesState state,
    CaloriesLoadResult value,
    PeriodLoadQuery query,
  ) =>
      state.copyWith(
        isLoading: false,
        data: value.data,
        display: buildCaloriesDisplay(value.data),
        latestBmrKcal: value.latestBmrKcal,
        error: null,
      );

  @override
  CaloriesState onLoadError(CaloriesState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

final caloriesProvider =
    NotifierProvider<CaloriesViewModel, CaloriesState>(CaloriesViewModel.new);
