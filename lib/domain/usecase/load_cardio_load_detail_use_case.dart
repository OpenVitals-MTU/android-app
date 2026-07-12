import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../insights/cardio_load.dart';
import '../model/activity_models.dart';
import '../model/heart_models.dart';

/// How far back the overview loads to establish a resting/HR baseline (Kotlin
/// `ActivityOverviewLookbackDays`).
const int cardioLoadLookbackDays = 30;

/// Today's cardio load, plus the day's step totals the detail screen shows
/// beside it.
class CardioLoadDetailLoadResult {
  const CardioLoadDetailLoadResult({
    required this.estimate,
    required this.steps,
    required this.activeCaloriesKcal,
  });

  final CardioLoadEstimate estimate;
  final int steps;
  final double? activeCaloriesKcal;
}

/// Computes today's cardio-load estimate.
///
/// Cardio load is a score for *one* day that cannot be computed from that day
/// alone: [calculateCardioLoad] needs a resting-HR baseline and an observed
/// maximum heart rate to place today's effort against, and a single day of
/// samples gives neither. So all four reads span a
/// [cardioLoadLookbackDays]-day window across two repositories, and only the
/// day's slice of them is scored — the rest is there to be the yardstick.
///
/// The workouts contribute their time windows, clipped to the day: a run that
/// crossed midnight must only lend the minutes that fell on this side of it.
class LoadCardioLoadDetailUseCase {
  const LoadCardioLoadDetailUseCase(
    this._activityRepository,
    this._heartRepository,
  );

  final ActivityRepository _activityRepository;
  final HeartRepository _heartRepository;

  Future<CardioLoadDetailLoadResult> call(LocalDate today) async {
    final start = today.minusDays(cardioLoadLookbackDays - 1);

    final results = await (
      _activityRepository.loadDailySteps(start, today),
      _activityRepository.loadWorkouts(start, today),
      _heartRepository.loadHeartRateSamples(start, today),
      _heartRepository.loadDailyRestingHR(start, today),
    ).wait;

    final dailySteps = results.$1;
    final workouts = results.$2;
    final samples = results.$3.orThrow();
    final restingHr = results.$4.orThrow();

    final todaySteps = dailySteps.where((s) => s.date == today).fold<DailySteps?>(
          null,
          (previous, element) => element,
        );

    return CardioLoadDetailLoadResult(
      estimate: _estimateFor(
        today: today,
        dailySteps: dailySteps,
        workouts: workouts,
        samples: samples,
        restingHr: restingHr,
      ),
      steps: todaySteps?.steps ?? 0,
      activeCaloriesKcal: todaySteps?.activeCaloriesKcal,
    );
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
