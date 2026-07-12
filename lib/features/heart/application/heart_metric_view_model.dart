import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/usecase/load_heart_period_use_case.dart';
import '../presentation/heart_metric.dart';
import '../presentation/heart_metric_cards.dart';
import 'heart_display.dart';

part 'heart_metric_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `HeartUiState`: the selection the scaffold
/// drives, the loaded [HeartPeriodLoadResult] payload, the precomputed
/// [HeartDisplay], loading/error flags and the heart-rate threshold-check
/// preferences.
@freezed
abstract class HeartMetricState with _$HeartMetricState {
  const HeartMetricState._();

  const factory HeartMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    @Default(PreferencesRepository.defaultHighHeartRateThresholdBpm)
    int highHeartRateThresholdBpm,
    @Default(PreferencesRepository.defaultLowHeartRateThresholdBpm)
    int lowHeartRateThresholdBpm,
    ScreenError? error,
    HeartPeriodLoadResult? result,
    HeartDisplay? display,
  }) = _HeartMetricState;
}

/// The Riverpod port of the Kotlin `HeartViewModel`, shared across the ten heart
/// + vitals metrics (keyed by [metric]).
///
/// A manual [Notifier] (no codegen), matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. A monotonic [_generation] guard drops stale results.
///
/// The display model is built here, at load time — the screen renders
/// [HeartMetricState.display] and derives nothing (Kotlin `HeartDisplayState`
/// discipline). Everything that feeds it — a new period, a moved threshold, a
/// deleted manual entry — rebuilds it through [_display].
class HeartMetricViewModel extends Notifier<HeartMetricState> {
  HeartMetricViewModel(this.metric);

  final HeartMetric metric;
  int _generation = 0;

  @override
  HeartMetricState build() {
    final prefs = ref.read(preferencesRepositoryProvider);
    return HeartMetricState(
      selectedDate: LocalDate.now(),
      highHeartRateThresholdBpm: prefs.highHeartRateThresholdBpm,
      lowHeartRateThresholdBpm: prefs.lowHeartRateThresholdBpm,
    );
  }

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadHeartPeriodUseCaseProvider);

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

    final result = await useCase(
      query,
      metric.loadRequest,
      refreshMode: refreshMode,
    );
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          result: value,
          display: _display(value),
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

  /// The screen-ready derivation of [result] under the current selection and
  /// thresholds. The one place a [HeartDisplay] is made.
  HeartDisplay _display(HeartPeriodLoadResult result) => buildHeartDisplay(
        result,
        selectedRange: state.selectedRange,
        highHeartRateThresholdBpm: state.highHeartRateThresholdBpm,
        lowHeartRateThresholdBpm: state.lowHeartRateThresholdBpm,
      );

  // ── Heart-rate threshold steppers (Kotlin `HeartViewModel`) ────────────────

  /// Kotlin `increaseHighHeartRateThreshold`.
  void increaseHighHeartRateThreshold() => _setHighHeartRateThreshold(
      state.highHeartRateThresholdBpm + heartRateThresholdStepBpm);

  /// Kotlin `decreaseHighHeartRateThreshold`.
  void decreaseHighHeartRateThreshold() => _setHighHeartRateThreshold(
      state.highHeartRateThresholdBpm - heartRateThresholdStepBpm);

  /// Kotlin `increaseLowHeartRateThreshold`.
  void increaseLowHeartRateThreshold() => _setLowHeartRateThreshold(
      state.lowHeartRateThresholdBpm + heartRateThresholdStepBpm);

  /// Kotlin `decreaseLowHeartRateThreshold`.
  void decreaseLowHeartRateThreshold() => _setLowHeartRateThreshold(
      state.lowHeartRateThresholdBpm - heartRateThresholdStepBpm);

  void _setHighHeartRateThreshold(int thresholdBpm) {
    final normalized = thresholdBpm
        .clamp(
          state.lowHeartRateThresholdBpm + heartRateThresholdMinimumGapBpm,
          PreferencesRepository.maxHighHeartRateThresholdBpm,
        )
        .clamp(
          PreferencesRepository.minHighHeartRateThresholdBpm,
          PreferencesRepository.maxHighHeartRateThresholdBpm,
        );
    ref.read(preferencesRepositoryProvider).highHeartRateThresholdBpm =
        normalized;
    state = state.copyWith(highHeartRateThresholdBpm: normalized);
    _rebuildDisplay();
  }

  void _setLowHeartRateThreshold(int thresholdBpm) {
    final maxAllowed =
        state.highHeartRateThresholdBpm - heartRateThresholdMinimumGapBpm;
    var normalized = thresholdBpm > maxAllowed ? maxAllowed : thresholdBpm;
    normalized = normalized.clamp(
      PreferencesRepository.minLowHeartRateThresholdBpm,
      PreferencesRepository.maxLowHeartRateThresholdBpm,
    );
    ref.read(preferencesRepositoryProvider).lowHeartRateThresholdBpm =
        normalized;
    state = state.copyWith(lowHeartRateThresholdBpm: normalized);
    _rebuildDisplay();
  }

  /// The threshold checks are part of the display, so a moved threshold has to
  /// rebuild it — the cards count the same loaded samples against a new line.
  void _rebuildDisplay() {
    final result = state.result;
    if (result == null) return;
    state = state.copyWith(display: _display(result));
  }

  // ── Manual vitals entry deletion (Kotlin `deleteVitalsMeasurementEntry`) ───

  /// Deletes a manual OpenVitals measurement and drops it from the loaded
  /// result without a full reload, mirroring the Kotlin
  /// `withDeletedVitalsMeasurementEntry`.
  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String entryId,
  ) async {
    if (entryId.isEmpty) return;
    final deletion =
        await ref.read(deleteVitalsMeasurementEntryUseCaseProvider)(
      type,
      entryId,
    );
    if (!ref.mounted) return;
    switch (deletion) {
      case Ok():
        final result = state.result;
        if (result == null) return;
        final remaining = _withDeletedVitalsEntry(result, type, entryId);
        state = state.copyWith(
          result: remaining,
          display: _display(remaining),
          error: null,
        );
      case Err(:final failure):
        state = state.copyWith(
          error: failure.toScreenError(fallback: 'Unable to delete entry.'),
        );
    }
  }
}

HeartPeriodLoadResult _withDeletedVitalsEntry(
  HeartPeriodLoadResult result,
  VitalsMeasurementType type,
  String entryId,
) =>
    HeartPeriodLoadResult(
      daySamples: result.daySamples,
      previousDaySamples: result.previousDaySamples,
      dailySummaries: result.dailySummaries,
      previousDailySummaries: result.previousDailySummaries,
      baselineDailySummaries: result.baselineDailySummaries,
      dayRestingSamples: result.dayRestingSamples,
      dayRestingBpm: result.dayRestingBpm,
      previousDayRestingBpm: result.previousDayRestingBpm,
      dayHrvSamples: result.dayHrvSamples,
      dayHrvMs: result.dayHrvMs,
      previousDayHrvMs: result.previousDayHrvMs,
      dailyRestingHR: result.dailyRestingHR,
      previousDailyRestingHR: result.previousDailyRestingHR,
      baselineDailyRestingHR: result.baselineDailyRestingHR,
      dailyHrv: result.dailyHrv,
      previousDailyHrv: result.previousDailyHrv,
      baselineDailyHrv: result.baselineDailyHrv,
      missingVitalsPermissions: result.missingVitalsPermissions,
      bloodPressure: type == VitalsMeasurementType.bloodPressure
          ? [for (final e in result.bloodPressure) if (e.id != entryId) e]
          : result.bloodPressure,
      previousBloodPressure: result.previousBloodPressure,
      baselineBloodPressure: result.baselineBloodPressure,
      spO2: type == VitalsMeasurementType.spo2
          ? [for (final e in result.spO2) if (e.id != entryId) e]
          : result.spO2,
      previousSpO2: result.previousSpO2,
      baselineSpO2: result.baselineSpO2,
      respiratoryRate: type == VitalsMeasurementType.respiratoryRate
          ? [for (final e in result.respiratoryRate) if (e.id != entryId) e]
          : result.respiratoryRate,
      previousRespiratoryRate: result.previousRespiratoryRate,
      baselineRespiratoryRate: result.baselineRespiratoryRate,
      bodyTemperature: type == VitalsMeasurementType.bodyTemperature
          ? [for (final e in result.bodyTemperature) if (e.id != entryId) e]
          : result.bodyTemperature,
      previousBodyTemperature: result.previousBodyTemperature,
      baselineBodyTemperature: result.baselineBodyTemperature,
      vo2Max: result.vo2Max,
      previousVo2Max: result.previousVo2Max,
      baselineVo2Max: result.baselineVo2Max,
      bloodGlucose: result.bloodGlucose,
      previousBloodGlucose: result.previousBloodGlucose,
      baselineBloodGlucose: result.baselineBloodGlucose,
      skinTemperature: result.skinTemperature,
      previousSkinTemperature: result.previousSkinTemperature,
      baselineSkinTemperature: result.baselineSkinTemperature,
    );

/// One [NotifierProvider] per heart/vitals metric, built eagerly so each metric's
/// state survives across screen rebuilds.
final Map<HeartMetric,
        NotifierProvider<HeartMetricViewModel, HeartMetricState>>
    _heartMetricProviders = {
  for (final metric in HeartMetric.values)
    metric: NotifierProvider<HeartMetricViewModel, HeartMetricState>(
      () => HeartMetricViewModel(metric),
    ),
};

/// The state provider for [metric]'s period detail screen.
NotifierProvider<HeartMetricViewModel, HeartMetricState> heartMetricProvider(
  HeartMetric metric,
) =>
    _heartMetricProviders[metric]!;
