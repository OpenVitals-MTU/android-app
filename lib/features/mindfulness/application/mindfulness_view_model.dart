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
import '../../../domain/query/mindfulness_period_data.dart';
import 'mindfulness_display.dart';

part 'mindfulness_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `MindfulnessUiState`: the selection the
/// scaffold drives, the loaded [MindfulnessPeriodData] payload, the precomputed
/// [MindfulnessDisplay], and loading/error flags.
@freezed
abstract class MindfulnessMetricState with _$MindfulnessMetricState {
  const MindfulnessMetricState._();

  const factory MindfulnessMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    MindfulnessPeriodData? data,
    MindfulnessDisplay? display,
  }) = _MindfulnessMetricState;
}

/// The Riverpod port of the Kotlin `MindfulnessViewModel` read path. A manual
/// [Notifier] (no codegen): the owning [MetricDetailScaffold] drives every load
/// through [load] (once on first frame, then on each selection change) and
/// pull-to-refresh through [refresh]. A monotonic [_generation] guard drops
/// stale results.
///
/// The display model is built here, at load time — the screen renders
/// [MindfulnessMetricState.display] and derives nothing (Kotlin
/// `MindfulnessDisplayState` discipline).
class MindfulnessViewModel extends Notifier<MindfulnessMetricState>
    with PeriodMetricLoader<MindfulnessMetricState, MindfulnessPeriodData> {
  @override
  MindfulnessMetricState build() =>
      MindfulnessMetricState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  PeriodSelection selectionOf(MindfulnessMetricState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  MindfulnessMetricState onLoadStart(
    MindfulnessMetricState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
    );
    return navigated ? next.copyWith(data: null, display: null) : next;
  }

  @override
  Future<Result<MindfulnessPeriodData>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      ref.read(loadMindfulnessPeriodUseCaseProvider)(query,
          refreshMode: refreshMode);

  @override
  MindfulnessMetricState onLoadSuccess(
    MindfulnessMetricState state,
    MindfulnessPeriodData value,
    PeriodLoadQuery query,
  ) =>
      state.copyWith(
        isLoading: false,
        data: value,
        display: buildMindfulnessDisplay(value),
        error: null,
      );

  @override
  MindfulnessMetricState onLoadError(
    MindfulnessMetricState state,
    ScreenError error,
  ) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// The state provider for the mindfulness period detail screen.
final mindfulnessProvider =
    NotifierProvider<MindfulnessViewModel, MindfulnessMetricState>(
  MindfulnessViewModel.new,
);
