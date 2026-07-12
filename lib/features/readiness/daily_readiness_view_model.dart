import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../data/prefs/preferences_repository.dart';
import '../../di/providers.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/insights/daily_readiness.dart';
import '../../domain/model/dashboard_data.dart';
import '../../domain/model/dashboard_query.dart';
import '../../domain/model/refresh_mode.dart';

part 'daily_readiness_view_model.freezed.dart';

/// The metrics loaded for a Daily Readiness computation (Kotlin
/// `DailyReadinessMetrics`).
const Set<DashboardMetric> dailyReadinessMetrics = <DashboardMetric>{
  DashboardMetric.sleep,
  DashboardMetric.workout,
  DashboardMetric.avgHeartRate,
  DashboardMetric.restingHeartRate,
  DashboardMetric.hrv,
  DashboardMetric.bodyTemperature,
  DashboardMetric.skinTemperature,
  DashboardMetric.weeklyCardioLoad,
  DashboardMetric.intensityMinutes,
  DashboardMetric.hydration,
  DashboardMetric.caloriesIn,
  DashboardMetric.protein,
  DashboardMetric.carbs,
  DashboardMetric.fat,
  DashboardMetric.mindfulness,
};

/// The Riverpod port of the Kotlin `DailyReadinessUiState`.
@freezed
abstract class DailyReadinessState with _$DailyReadinessState {
  const DailyReadinessState._();

  const factory DailyReadinessState({
    required LocalDate selectedDate,
    @Default(true) bool isLoading,
    ScreenError? error,
    DashboardData? data,
    DailyReadinessInsight? insight,
  }) = _DailyReadinessState;

  bool get canGoForward => selectedDate.isBefore(LocalDate.now());
}

/// The Riverpod port of the Kotlin `DailyReadinessViewModel`. A manual
/// [Notifier] that loads a [DashboardData] for the selected day (via
/// [LoadDashboardDayUseCase]) and derives the readiness insight with
/// [calculateDailyReadiness]. A monotonic [_generation] guard drops stale loads.
class DailyReadinessViewModel extends Notifier<DailyReadinessState> {
  int _generation = 0;

  @override
  DailyReadinessState build() {
    final initial = DailyReadinessState(selectedDate: LocalDate.now());
    Future.microtask(() {
      if (ref.mounted) load(initial.selectedDate);
    });
    return initial;
  }

  Future<void> load(
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final clamped = date.coerceAtMost(LocalDate.now());
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadDashboardDayUseCaseProvider);
    final goals = _goals(prefs);

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
          visibleMetrics: dailyReadinessMetrics,
          refreshMode: refreshMode,
        ),
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        data: data,
        insight: calculateDailyReadiness(data, goals: goals),
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

  DailyReadinessGoalInputs _goals(PreferencesRepository prefs) =>
      DailyReadinessGoalInputs(
        stepsGoal: prefs.dailyGoalFor(MetricDailyGoalKey.steps),
        hydrationLitersGoal: prefs.hydrationDailyGoalLiters,
        activeMinutesGoal:
            prefs.dailyGoalFor(MetricDailyGoalKey.activeCaloriesKcal) / 10.0,
      );
}

/// The Daily Readiness state provider (manually declared, no codegen).
final dailyReadinessProvider =
    NotifierProvider<DailyReadinessViewModel, DailyReadinessState>(
  DailyReadinessViewModel.new,
);
