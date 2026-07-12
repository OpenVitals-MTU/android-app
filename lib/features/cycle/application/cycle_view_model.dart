import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/cycle_period_data.dart';
import 'cycle_display.dart';

part 'cycle_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `CycleUiState`, trimmed to the selection the
/// scaffold drives, the loaded [CyclePeriodData] payload (menstruation flow,
/// periods, ovulation tests, cervical mucus, basal body temperature, …), the
/// precomputed [CycleDisplay], and loading/error flags.
@freezed
abstract class CycleMetricState with _$CycleMetricState {
  const CycleMetricState._();

  const factory CycleMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.month) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    CyclePeriodData? result,
    CycleDisplay? display,
  }) = _CycleMetricState;

  CycleData get data => result?.data ?? const CycleData();
  Set<String> get missingPermissions =>
      result?.missingPermissions ?? const <String>{};
}

/// The Riverpod port of the Kotlin `CycleViewModel`. A manual [Notifier] (no
/// codegen), matching the activity/heart templates: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. A monotonic [_generation] guard drops stale results.
///
/// The display model is built here, at load time — the screen renders
/// [CycleMetricState.display] and derives nothing (Kotlin `CycleDisplayState`
/// discipline).
class CycleViewModel extends Notifier<CycleMetricState> {
  int _generation = 0;

  @override
  CycleMetricState build() => CycleMetricState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadCyclePeriod = ref.read(loadCyclePeriodUseCaseProvider);

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

    final result = await loadCyclePeriod(query, refreshMode: refreshMode);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          result: value,
          display: buildCycleDisplay(value.data),
          error: null,
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

/// The state provider for the cycle period detail screen.
final cycleProvider = NotifierProvider<CycleViewModel, CycleMetricState>(
  CycleViewModel.new,
);
