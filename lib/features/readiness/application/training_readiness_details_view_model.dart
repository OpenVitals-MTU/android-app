import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/daily_readiness.dart';
import '../../../domain/model/dashboard_query.dart';
import '../../../domain/model/refresh_mode.dart';
import 'daily_readiness_view_model.dart' show dailyReadinessMetrics;
import 'training_readiness_display.dart';

// The screen renders the display and the factor-kind set straight out of here.
export 'training_readiness_display.dart';

part 'training_readiness_details_view_model.freezed.dart';

/// The state of the training-readiness detail screen: the day it is showing, the
/// loaded [DailyReadinessInsight], and the precomputed
/// [TrainingReadinessDisplay] the cards render.
@freezed
abstract class TrainingReadinessDetailsState
    with _$TrainingReadinessDetailsState {
  const TrainingReadinessDetailsState._();

  const factory TrainingReadinessDetailsState({
    required LocalDate selectedDate,
    @Default(true) bool isLoading,
    ScreenError? error,
    DailyReadinessInsight? insight,
    TrainingReadinessDisplay? display,
  }) = _TrainingReadinessDetailsState;

  bool get canGoForward => selectedDate.isBefore(LocalDate.now());
}

/// The view-model behind the training-readiness detail. Loads the same
/// [DashboardData] the daily-readiness screen does (via [LoadDashboardDayUseCase]
/// over [dailyReadinessMetrics]) for an arbitrary day, derives the readiness
/// insight with [calculateDailyReadiness], and precomputes the display.
///
/// Replaces the ad-hoc `FutureProvider.family` + `initState` the screen used to
/// load through: day navigation, pull-to-refresh and the staleness guard now
/// live where every other screen keeps them. A monotonic [_generation] guard
/// drops a slow load that a newer day has overtaken.
class TrainingReadinessDetailsViewModel
    extends Notifier<TrainingReadinessDetailsState> {
  int _generation = 0;

  @override
  TrainingReadinessDetailsState build() =>
      TrainingReadinessDetailsState(selectedDate: LocalDate.now());

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

    // LoadDashboardDayUseCase still throws rather than returning a Result, so
    // this is the one bridge the seam reversal cannot remove from here (the
    // sibling DailyReadinessViewModel has the same one).
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
      final insight = calculateDailyReadiness(data, goals: goals);
      state = state.copyWith(
        isLoading: false,
        insight: insight,
        display: buildTrainingReadinessDisplay(insight),
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

/// The training-readiness detail state provider (manually declared, no codegen).
final trainingReadinessDetailsProvider = NotifierProvider<
    TrainingReadinessDetailsViewModel, TrainingReadinessDetailsState>(
  TrainingReadinessDetailsViewModel.new,
);
