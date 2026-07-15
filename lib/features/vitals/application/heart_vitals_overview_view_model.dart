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
import '../../../domain/usecase/load_heart_period_use_case.dart';
import 'heart_vitals_overview_display.dart';

part 'heart_vitals_overview_view_model.freezed.dart';

/// The combined heart & vitals overview state, port of the slice of the Kotlin
/// `HeartUiState` the overview consumes: the scaffold-driven selection, the
/// loaded [HeartPeriodLoadResult] payload, the precomputed
/// [HeartVitalsOverviewDisplay] and loading/error flags.
@freezed
abstract class HeartVitalsOverviewState with _$HeartVitalsOverviewState {
  const HeartVitalsOverviewState._();

  const factory HeartVitalsOverviewState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,

    /// The loaded period's week mode, carried on the state (as `SleepState` does)
    /// so the section summaries name the window exactly as the period navigator
    /// does — "Last 30 days" on a rolling month, not "This month".
    @Default(WeekPeriodMode.mondayToSunday) WeekPeriodMode weekPeriodMode,
    @Default(true) bool isLoading,
    ScreenError? error,
    HeartPeriodLoadResult? result,
    HeartVitalsOverviewDisplay? display,
  }) = _HeartVitalsOverviewState;
}

/// The Riverpod port of the overview slice of the Kotlin `HeartViewModel`: the
/// owning [MetricDetailScaffold] drives every load through [load] and
/// pull-to-refresh through [refresh]. A monotonic [_generation] guard drops
/// stale results. It always issues the combined heart + vitals load.
///
/// The display model is built here, at load time — the screen renders
/// [HeartVitalsOverviewState.display] and derives nothing (Kotlin
/// `HeartDisplayState` discipline).
class HeartVitalsOverviewViewModel extends Notifier<HeartVitalsOverviewState>
    with PeriodMetricLoader<HeartVitalsOverviewState, HeartPeriodLoadResult> {
  @override
  HeartVitalsOverviewState build() =>
      HeartVitalsOverviewState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  String get loadErrorFallback => 'Unable to load heart & vitals.';

  @override
  PeriodSelection selectionOf(HeartVitalsOverviewState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  HeartVitalsOverviewState onLoadStart(
    HeartVitalsOverviewState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      weekPeriodMode: ref.read(preferencesRepositoryProvider).weekPeriodMode,
      isLoading: true,
      error: null,
    );
    return navigated ? next.copyWith(result: null, display: null) : next;
  }

  @override
  Future<Result<HeartPeriodLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      ref.read(loadHeartPeriodUseCaseProvider)(
        query,
        const HeartPeriodLoadCombined(),
        refreshMode: refreshMode,
      );

  @override
  HeartVitalsOverviewState onLoadSuccess(
    HeartVitalsOverviewState state,
    HeartPeriodLoadResult value,
    PeriodLoadQuery query,
  ) =>
      state.copyWith(
        isLoading: false,
        result: value,
        display: buildHeartVitalsOverviewDisplay(
          value,
          selectedRange: state.selectedRange,
        ),
        error: null,
      );

  @override
  HeartVitalsOverviewState onLoadError(
    HeartVitalsOverviewState state,
    ScreenError error,
  ) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

final heartVitalsOverviewProvider =
    NotifierProvider<HeartVitalsOverviewViewModel, HeartVitalsOverviewState>(
        HeartVitalsOverviewViewModel.new);
