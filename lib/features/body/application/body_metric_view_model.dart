import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/body_period_data.dart';
import 'body_display.dart';

part 'body_metric_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `BodyUiState`, trimmed to the selection the
/// scaffold drives, the single [BodyPeriodData] payload (which the Kotlin
/// view-model loads once — via `BodyPeriodMetric.ALL` — and shares across every
/// body metric) and the [BodyDisplay] derived from it.
@freezed
abstract class BodyMetricState with _$BodyMetricState {
  const BodyMetricState._();

  const factory BodyMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.month) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    BodyPeriodData? data,
    BodyDisplay? display,
  }) = _BodyMetricState;
}

/// The Riverpod port of the Kotlin `BodyViewModel`. A single shared [Notifier]
/// (matching the Kotlin single view-model) backs the aggregate `/body` screen:
/// one `BodyPeriodMetric.all` load batches all eight body metrics, exactly as
/// the Kotlin `load()` does, so the aggregate never issues per-metric loads.
/// The owning
/// [MetricDetailScaffold] drives loads through [load] and pull-to-refresh through
/// [refresh]; a monotonic [_generation] guard drops stale results.
///
/// The display model is built here, at load time — the screen renders
/// [BodyMetricState.display] and derives nothing. The optimistic delete rebuilds
/// it too: it drops an entry from the loaded data without reloading.
class BodyMetricViewModel extends Notifier<BodyMetricState> {
  int _generation = 0;

  @override
  BodyMetricState build() =>
      BodyMetricState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadBodyPeriod = ref.read(loadBodyPeriodUseCaseProvider);

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

    final result = await loadBodyPeriod(query, refreshMode: refreshMode);
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          data: value,
          display: buildBodyDisplay(value),
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

  /// Port of the Kotlin `BodyViewModel.deleteBodyMeasurementEntry`: remove the
  /// entry optimistically, delete it through the repository, then force-reload
  /// the period; restore the previous state (with an error) on failure.
  Future<void> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String entryId,
  ) async {
    if (entryId.isEmpty) return;
    final data = state.data;
    if (data == null) return;
    if (!_isOpenVitalsEntry(data, type, entryId)) return;

    final previous = state;
    // The entry leaves the list at once — and the display with it, since it is
    // what the list renders.
    final withoutEntry = _withDeletedEntry(data, type, entryId);
    state = state.copyWith(
      data: withoutEntry,
      display: buildBodyDisplay(withoutEntry),
      error: null,
    );

    final deletion =
        await ref.read(deleteBodyMeasurementEntryUseCaseProvider)(type, entryId);
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

  bool _isOpenVitalsEntry(
    BodyPeriodData data,
    BodyMeasurementType type,
    String entryId,
  ) {
    switch (type) {
      case BodyMeasurementType.weight:
        return data.weightEntries
            .any((e) => e.id == entryId && e.isOpenVitalsEntry);
      case BodyMeasurementType.height:
        return data.heightEntries
            .any((e) => e.id == entryId && e.isOpenVitalsEntry);
      case BodyMeasurementType.bodyFat:
        return data.bodyFatEntries
            .any((e) => e.id == entryId && e.isOpenVitalsEntry);
    }
  }

  BodyPeriodData _withDeletedEntry(
    BodyPeriodData data,
    BodyMeasurementType type,
    String entryId,
  ) {
    switch (type) {
      case BodyMeasurementType.weight:
        return data.copyWith(
          weightEntries:
              data.weightEntries.where((e) => e.id != entryId).toList(),
        );
      case BodyMeasurementType.height:
        return data.copyWith(
          heightEntries:
              data.heightEntries.where((e) => e.id != entryId).toList(),
        );
      case BodyMeasurementType.bodyFat:
        return data.copyWith(
          bodyFatEntries:
              data.bodyFatEntries.where((e) => e.id != entryId).toList(),
        );
    }
  }
}

/// The shared body state provider. A manually-declared [NotifierProvider] (no
/// codegen), following the dashboard/heart template.
final bodyMetricProvider =
    NotifierProvider<BodyMetricViewModel, BodyMetricState>(
  BodyMetricViewModel.new,
);
