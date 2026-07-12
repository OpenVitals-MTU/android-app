import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/body_models.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/query/body_period_data.dart';

part 'body_metric_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `BodyUiState`, trimmed to the selection the
/// scaffold drives plus the single [BodyPeriodData] payload (which the Kotlin
/// view-model loads once — via `BodyPeriodMetric.ALL` — and shares across every
/// body metric). Per-metric derivations are computed on demand by the screen.
@freezed
abstract class BodyMetricState with _$BodyMetricState {
  const BodyMetricState._();

  const factory BodyMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.month) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    BodyPeriodData? data,
  }) = _BodyMetricState;
}

/// The Riverpod port of the Kotlin `BodyViewModel`. A single shared [Notifier]
/// (matching the Kotlin single view-model) backs the aggregate `/body` screen:
/// one `BodyPeriodMetric.all` load batches all eight body metrics, exactly as
/// the Kotlin `load()` does, so the aggregate never issues per-metric loads.
/// The owning
/// [MetricDetailScaffold] drives loads through [load] and pull-to-refresh through
/// [refresh]; a monotonic [_generation] guard drops stale results.
class BodyMetricNotifier extends Notifier<BodyMetricState> {
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

    try {
      final data = await loadBodyPeriod(query, refreshMode: refreshMode);
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
    state = state.copyWith(
      data: _withDeletedEntry(data, type, entryId),
      error: null,
    );
    try {
      await ref.read(deleteBodyMeasurementEntryUseCaseProvider)(type, entryId);
      if (!ref.mounted) return;
      await load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = previous.copyWith(
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
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
final bodyMetricNotifierProvider =
    NotifierProvider<BodyMetricNotifier, BodyMetricState>(
  BodyMetricNotifier.new,
);
