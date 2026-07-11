import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/insights/stress_tracking.dart';
import '../../domain/model/dashboard_data.dart';
import '../../domain/model/dashboard_query.dart';
import '../../domain/model/refresh_mode.dart';

part 'recovery_notifier.freezed.dart';

/// The metrics loaded for a physiological-stress estimate: the HRV / heart-rate
/// signals plus the sleep, hydration, nutrition, temperature, load, and
/// mindfulness context [calculatePhysiologicalStress] reads.
const Set<DashboardMetric> recoveryStressMetrics = <DashboardMetric>{
  DashboardMetric.avgHeartRate,
  DashboardMetric.restingHeartRate,
  DashboardMetric.hrv,
  DashboardMetric.sleep,
  DashboardMetric.workout,
  DashboardMetric.hydration,
  DashboardMetric.caloriesIn,
  DashboardMetric.protein,
  DashboardMetric.carbs,
  DashboardMetric.fat,
  DashboardMetric.bodyTemperature,
  DashboardMetric.skinTemperature,
  DashboardMetric.weeklyCardioLoad,
  DashboardMetric.intensityMinutes,
  DashboardMetric.mindfulness,
};

/// The selected-day recovery / physiological-stress state (the Flutter port's
/// analogue of the Kotlin `RecoveryViewModel`, re-pointed at
/// `calculatePhysiologicalStress` — the estimate the Stress detail renders).
@freezed
abstract class RecoveryState with _$RecoveryState {
  const RecoveryState._();

  const factory RecoveryState({
    required LocalDate selectedDate,
    @Default(true) bool isLoading,
    ScreenError? error,
    DashboardData? data,
    PhysiologicalStressEstimate? stress,
  }) = _RecoveryState;

  bool get canGoForward => selectedDate.isBefore(LocalDate.now());
}

/// Loads a [DashboardData] for the selected day (via [LoadDashboardDayUseCase])
/// and derives the physiological-stress estimate with
/// [calculatePhysiologicalStress]. A monotonic [_generation] guard drops stale
/// loads.
class RecoveryNotifier extends Notifier<RecoveryState> {
  int _generation = 0;

  @override
  RecoveryState build() =>
      RecoveryState(selectedDate: LocalDate.now());

  Future<void> load(
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final clamped = date.coerceAtMost(LocalDate.now());
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadDashboardDayUseCaseProvider);

    state = state.copyWith(
      selectedDate: clamped,
      isLoading: true,
      error: null,
    );

    try {
      final data = await useCase(
        DashboardQuery(
          date: clamped,
          sleepRangeMode: prefs.sleepRangeMode,
          activityWeekMode: prefs.activityWeekMode,
          visibleMetrics: recoveryStressMetrics,
          refreshMode: refreshMode,
        ),
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        data: data,
        stress: calculatePhysiologicalStress(data),
        error: null,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unknown error'),
      );
    }
  }

  void previousDay() => load(state.selectedDate.minusDays(1));

  void nextDay() {
    final next = state.selectedDate.plusDays(1);
    if (!next.isAfter(LocalDate.now())) load(next);
  }

  void selectDate(LocalDate date) => load(date);

  Future<void> refresh() =>
      load(state.selectedDate, refreshMode: RefreshMode.force);
}

/// The recovery / stress state provider (manually declared, no codegen).
final recoveryNotifierProvider =
    NotifierProvider<RecoveryNotifier, RecoveryState>(RecoveryNotifier.new);
