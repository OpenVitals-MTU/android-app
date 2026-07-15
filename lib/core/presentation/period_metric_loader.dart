import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../di/providers.dart';
import '../../domain/model/refresh_mode.dart';
import '../period/period_load_query.dart';
import '../period/period_selection.dart';
import '../result/result.dart';
import 'screen_error.dart';

/// Shared load orchestration for the period-metric detail view-models.
///
/// Every metric-detail view-model (`heart_vitals`, `calories`, `sleep`, `body`,
/// …) used to copy-paste the same ~30-line `load`: a monotonic generation guard,
/// a `PeriodLoadQuery` build off `prefs.weekPeriodMode`, the use-case await, the
/// `ref.mounted && generation` staleness check, and the Ok/Err dispatch. This
/// mixin owns that once.
///
/// It also owns the fix for the range-switch loading flash: it computes a
/// [navigated] flag — the selection differs from the one already in [state], so a
/// same-window pull-to-refresh is NOT a navigation — and hands it to
/// [onLoadStart], which nulls the stale display on a navigation (so the screen
/// shows its loading skeleton instead of the previous window's chart, which
/// pinch-zooming would draw broken) while a refresh keeps it.
///
/// The state transitions stay per-view-model: `S` is an opaque freezed type with
/// no shared `copyWith`, so [onLoadStart]/[onLoadSuccess]/[onLoadError] are the
/// hooks that build the next `S`.
mixin PeriodMetricLoader<S, V> on Notifier<S> {
  int _generation = 0;

  /// The selection currently reflected in [state] (its `selectedRange` /
  /// `selectedDate`). Used to tell a navigation from a same-window refresh.
  PeriodSelection selectionOf(S state);

  /// The state at the start of a load: apply [selection], set `isLoading: true`
  /// and clear the error. When [navigated], ALSO null the stale model/display so
  /// the screen shows its loading skeleton rather than the previous window's data.
  /// Always-applied fields (goals, modes, week-period mode) belong here too,
  /// outside the [navigated] gate.
  S onLoadStart(S state, PeriodSelection selection, {required bool navigated});

  /// Runs the feature use-case for [query]. The view-model closes over its
  /// `metric`/mode/flags; a use-case without a [refreshMode] simply ignores it.
  Future<Result<V>> fetch(PeriodLoadQuery query, RefreshMode refreshMode);

  /// The state after a successful load (set model + built display,
  /// `isLoading: false`). [query] is passed so a display that needs the resolved
  /// window (`query.windows.current`) need not recompute it.
  S onLoadSuccess(S state, V value, PeriodLoadQuery query);

  /// The state after a failed load (set error, `isLoading: false`).
  S onLoadError(S state, ScreenError error);

  /// Fallback message when the failure carries none. Override per feature.
  String get loadErrorFallback => 'Unable to load data.';

  /// The single load path; each view-model's public `load(...)` delegates here.
  Future<void> runLoad(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final current = selectionOf(state);
    final navigated = selection.selectedRange != current.selectedRange ||
        selection.selectedDate != current.selectedDate;

    state = onLoadStart(state, selection, navigated: navigated);

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: ref.read(preferencesRepositoryProvider).weekPeriodMode,
    );

    final result = await fetch(query, refreshMode);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = onLoadSuccess(state, value, query);
      case Err(:final failure):
        state = onLoadError(
          state,
          failure.toScreenError(fallback: loadErrorFallback),
        );
    }
  }
}
