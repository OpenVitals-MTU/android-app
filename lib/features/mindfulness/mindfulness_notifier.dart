import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/mindfulness_models.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/query/mindfulness_period_data.dart';

part 'mindfulness_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `MindfulnessUiState`, trimmed to the read
/// detail the period screen consumes: the selection the scaffold drives, the
/// loaded [MindfulnessPeriodData] payload, and loading/error flags.
///
/// The Kotlin view-model precomputes a `MindfulnessDisplayState` (goal progress,
/// baselines, cross-metric sleep insight, reminders). Those are Phase 6 concerns;
/// the primary summary + session list is derived on demand by the screen.
@freezed
abstract class MindfulnessMetricState with _$MindfulnessMetricState {
  const MindfulnessMetricState._();

  const factory MindfulnessMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    MindfulnessPeriodData? data,
  }) = _MindfulnessMetricState;

  List<MindfulnessSession> get sessions => data?.sessions ?? const [];
}

/// The Riverpod port of the Kotlin `MindfulnessViewModel` read path. A manual
/// [Notifier] (no codegen), matching the activity/heart templates: the owning
/// [MetricDetailScaffold] drives every load through [load] (once on first frame,
/// then on each selection change) and pull-to-refresh through [refresh]. A
/// monotonic [_generation] guard drops stale results.
class MindfulnessNotifier extends Notifier<MindfulnessMetricState> {
  int _generation = 0;

  @override
  MindfulnessMetricState build() =>
      MindfulnessMetricState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final repo = ref.read(mindfulnessRepositoryProvider);

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
      final data = await repo.loadMindfulnessPeriod(
        query,
        refreshMode: refreshMode,
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, data: data, error: null);
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

/// The state provider for the mindfulness period detail screen.
final mindfulnessProvider =
    NotifierProvider<MindfulnessNotifier, MindfulnessMetricState>(
  MindfulnessNotifier.new,
);
