import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../data/prefs/preferences_repository.dart';
import '../../di/providers.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/model/vitals_models.dart';
import '../../domain/usecase/load_heart_period_use_case.dart';
import 'heart_metric.dart';
import 'heart_metric_cards.dart';

part 'heart_metric_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `HeartUiState`, trimmed to the selection the
/// scaffold drives, the loaded [HeartPeriodLoadResult] payload, loading/error
/// flags and the heart-rate threshold-check preferences. The Kotlin view-model
/// precomputes a `HeartDisplayState`; here the (cheap) per-metric derivations
/// are computed on demand by the screen.
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
  }) = _HeartMetricState;
}

/// The Riverpod port of the Kotlin `HeartViewModel`, shared across the ten heart
/// + vitals metrics (keyed by [metric]).
///
/// A manual [Notifier] (no codegen), matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] and pull-to-refresh
/// through [refresh]. A monotonic [_generation] guard drops stale results.
class HeartMetricNotifier extends Notifier<HeartMetricState> {
  HeartMetricNotifier(this.metric);

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

    try {
      final result = await useCase(
        query,
        metric.loadRequest,
        refreshMode: refreshMode,
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, result: result, error: null);
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
    try {
      await ref.read(deleteVitalsMeasurementEntryUseCaseProvider)(type, entryId);
      if (!ref.mounted) return;
      final result = state.result;
      if (result == null) return;
      state = state.copyWith(
        result: _withDeletedVitalsEntry(result, type, entryId),
        error: null,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        error: throwableToScreenError(error, fallback: 'Unable to delete entry.'),
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
        NotifierProvider<HeartMetricNotifier, HeartMetricState>>
    _heartMetricProviders = {
  for (final metric in HeartMetric.values)
    metric: NotifierProvider<HeartMetricNotifier, HeartMetricState>(
      () => HeartMetricNotifier(metric),
    ),
};

/// The state provider for [metric]'s period detail screen.
NotifierProvider<HeartMetricNotifier, HeartMetricState> heartMetricProvider(
  HeartMetric metric,
) =>
    _heartMetricProviders[metric]!;
