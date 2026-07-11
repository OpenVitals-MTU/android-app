import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/insights/cardio_load.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/heart_models.dart';

part 'cardio_load_detail_notifier.freezed.dart';

/// How far back the overview loads to establish a resting/HR baseline (Kotlin
/// `ActivityOverviewLookbackDays`).
const int _lookbackDays = 30;

/// The Riverpod port of the Kotlin `ActivityOverviewUiState`, reduced to the
/// single day the cardio-load detail renders.
@freezed
abstract class CardioLoadState with _$CardioLoadState {
  const factory CardioLoadState({
    required LocalDate date,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(CardioLoadEstimate.noData) CardioLoadEstimate estimate,
    @Default(0) int steps,
    double? activeCaloriesKcal,
  }) = _CardioLoadState;
}

/// The Riverpod port of the Kotlin `ActivityOverviewViewModel` +
/// `CardioLoadDetailScreen`, computing today's cardio-load estimate via
/// [calculateCardioLoad] over a 30-day heart-rate baseline window.
class CardioLoadNotifier extends Notifier<CardioLoadState> {
  int _generation = 0;

  @override
  CardioLoadState build() {
    Future.microtask(() {
      if (ref.mounted) _load();
    });
    return CardioLoadState(date: LocalDate.now());
  }

  Future<void> refresh() => _load();

  Future<void> _load() async {
    final generation = ++_generation;
    final today = LocalDate.now();
    final start = today.minusDays(_lookbackDays - 1);
    final activityRepo = ref.read(activityRepositoryProvider);
    final heartRepo = ref.read(heartRepositoryProvider);

    state = state.copyWith(isLoading: true, error: null, date: today);

    try {
      final results = await (
        activityRepo.loadDailySteps(start, today),
        activityRepo.loadWorkouts(start, today),
        heartRepo.loadHeartRateSamples(start, today),
        heartRepo.loadDailyRestingHR(start, today),
      ).wait;
      if (!ref.mounted || generation != _generation) return;

      final dailySteps = results.$1;
      final workouts = results.$2;
      final samples = results.$3;
      final restingHr = results.$4;

      final estimate = _estimateFor(
        today: today,
        dailySteps: dailySteps,
        workouts: workouts,
        samples: samples,
        restingHr: restingHr,
      );
      final todaySteps =
          dailySteps.where((s) => s.date == today).fold<DailySteps?>(
                null,
                (previous, element) => element,
              );

      state = state.copyWith(
        isLoading: false,
        estimate: estimate,
        steps: todaySteps?.steps ?? 0,
        activeCaloriesKcal: todaySteps?.activeCaloriesKcal,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load cardio load.',
        ),
      );
    }
  }

  CardioLoadEstimate _estimateFor({
    required LocalDate today,
    required List<DailySteps> dailySteps,
    required List<ExerciseData> workouts,
    required List<HeartRateSample> samples,
    required List<DailyRestingHR> restingHr,
  }) {
    final daySteps = dailySteps.where((s) => s.date == today).fold<DailySteps?>(
          null,
          (previous, element) => element,
        );
    final daySamples = samples
        .where((s) => LocalDate.fromDateTime(s.time.toLocal()) == today)
        .toList();
    final restingByDate = {for (final r in restingHr) r.date: r.bpm};
    final baselineResting = _median(restingHr.map((r) => r.bpm).toList());
    final observedMax = samples.isEmpty
        ? null
        : samples
            .map((s) => s.beatsPerMinute)
            .reduce((a, b) => a > b ? a : b);

    final dayStart = DateTime(today.year, today.month, today.day);
    final dayEnd = dayStart.add(const Duration(days: 1));
    final windows = <CardioLoadTimeWindow>[];
    for (final workout in workouts) {
      final startLocal = workout.startTime.toLocal();
      final endLocal = workout.endTime.toLocal();
      if (!endLocal.isAfter(dayStart) || !startLocal.isBefore(dayEnd)) continue;
      final windowStart = startLocal.isBefore(dayStart) ? dayStart : startLocal;
      final windowEnd = endLocal.isAfter(dayEnd) ? dayEnd : endLocal;
      final window = CardioLoadTimeWindow(start: windowStart, end: windowEnd);
      if (window.durationMinutes > 0.0) windows.add(window);
    }

    return calculateCardioLoad(
      daySteps,
      daySamples,
      restingByDate[today],
      baselineResting,
      observedMax,
      windows,
    );
  }

  int? _median(List<int> values) {
    if (values.isEmpty) return null;
    final sorted = [...values]..sort();
    return sorted[(sorted.length - 1) ~/ 2];
  }
}

final cardioLoadNotifierProvider =
    NotifierProvider<CardioLoadNotifier, CardioLoadState>(
  CardioLoadNotifier.new,
);
