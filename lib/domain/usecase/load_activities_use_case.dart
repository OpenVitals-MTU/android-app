import '../../core/period/period_load_query.dart';
import '../../core/period/time_range.dart';
import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/activity_repository.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../insights/cardio_load.dart';
import '../model/activity_models.dart';
import '../model/heart_models.dart';
import '../model/nutrition_models.dart';

/// One day of the activities overview aggregate — the union of steps,
/// energy-burned, HRV, cardio-load and workouts for that date. A plain (non
/// freezed) value type; ported from Kotlin `ActivityOverviewDay`.
class ActivityOverviewDay {
  const ActivityOverviewDay({
    required this.date,
    required this.steps,
    required this.distanceMeters,
    required this.activeCaloriesKcal,
    required this.energyBurnedKcal,
    required this.energyBurnedSource,
    required this.workouts,
    required this.hrvRmssdMs,
    required this.cardioLoad,
    required this.cardioLoadConfidence,
  });

  final LocalDate date;
  final int steps;
  final double distanceMeters;
  final double? activeCaloriesKcal;
  final double energyBurnedKcal;
  final CaloriesBurnedSource energyBurnedSource;
  final List<ExerciseData> workouts;
  final double? hrvRmssdMs;
  final int cardioLoad;
  final CardioLoadConfidence cardioLoadConfidence;

  ActivityOverviewDay withWorkouts(List<ExerciseData> workouts) =>
      ActivityOverviewDay(
        date: date,
        steps: steps,
        distanceMeters: distanceMeters,
        activeCaloriesKcal: activeCaloriesKcal,
        energyBurnedKcal: energyBurnedKcal,
        energyBurnedSource: energyBurnedSource,
        workouts: workouts,
        hrvRmssdMs: hrvRmssdMs,
        cardioLoad: cardioLoad,
        cardioLoadConfidence: cardioLoadConfidence,
      );
}

/// The unfiltered load result, cached so the activity-type filter can re-slice
/// the data without a repository round-trip (Kotlin `ActivitiesLoadResult`).
class ActivitiesLoadResult {
  const ActivitiesLoadResult({
    required this.workouts,
    required this.plannedWorkouts,
    required this.previousWorkouts,
    required this.baselineWorkouts,
    required this.overviewDays,
    required this.crossDailyRestingHR,
  });

  final List<ExerciseData> workouts;
  final List<PlannedExerciseData> plannedWorkouts;
  final List<ExerciseData> previousWorkouts;
  final List<ExerciseData> baselineWorkouts;
  final List<ActivityOverviewDay> overviewDays;
  final List<DailyRestingHR> crossDailyRestingHR;

  /// Every activity type the period saw, recorded or planned. Unsorted — the
  /// screen orders them by their (localizable) label.
  Set<int> activityTypes() => <int>{
        for (final w in workouts) w.exerciseType,
        for (final p in plannedWorkouts) p.exerciseType,
      };

  ActivitiesLoadResult filteredBy(int? type) {
    if (type == null) return this;
    return ActivitiesLoadResult(
      workouts: [for (final w in workouts) if (w.exerciseType == type) w],
      plannedWorkouts: [
        for (final p in plannedWorkouts)
          if (p.exerciseType == type) p,
      ],
      previousWorkouts: [
        for (final w in previousWorkouts)
          if (w.exerciseType == type) w,
      ],
      baselineWorkouts: [
        for (final w in baselineWorkouts)
          if (w.exerciseType == type) w,
      ],
      overviewDays: [
        for (final day in overviewDays)
          day.withWorkouts(
            [for (final w in day.workouts) if (w.exerciseType == type) w],
          ),
      ],
      crossDailyRestingHR: crossDailyRestingHR,
    );
  }

  ActivitiesLoadResult withoutEntry(String id) => ActivitiesLoadResult(
        workouts: [for (final w in workouts) if (w.id != id) w],
        plannedWorkouts: plannedWorkouts,
        previousWorkouts: previousWorkouts,
        baselineWorkouts: baselineWorkouts,
        overviewDays: [
          for (final day in overviewDays)
            day.withWorkouts([for (final w in day.workouts) if (w.id != id) w]),
        ],
        crossDailyRestingHR: crossDailyRestingHR,
      );
}

/// Loads the whole activities overview for one period.
///
/// Nine reads across two repositories, issued together because the overview is
/// one aggregate, not nine cards that happen to share a screen: the per-day
/// cardio-load estimate needs the day's steps, its workouts' time windows, its
/// heart-rate samples AND a resting-HR baseline taken across the *whole* window,
/// so no single repository can answer it.
///
/// Two of the reads are deliberately narrowed:
///
/// - Only the current window pays for the per-session distance/speed aggregates
///   ([ActivityRepository.loadWorkoutsWithMetrics]) — it is the one that renders
///   the activity-type stats card. Previous/baseline stay on the plain read
///   (Kotlin `ActivitiesViewModel` switched exactly this one call site).
/// - A year of instantaneous heart-rate samples is hundreds of thousands of
///   points and no cardio-load chart shows them, so the year range skips that
///   read entirely.
///
/// Nothing degrades here: the overview is the screen.
class LoadActivitiesUseCase {
  const LoadActivitiesUseCase(this._activityRepository, this._heartRepository);

  final ActivityRepository _activityRepository;
  final HeartRepository _heartRepository;

  Future<ActivitiesLoadResult> call(PeriodLoadQuery query) async {
    final windows = query.windows;
    final current = windows.current;
    final isYear = query.range == TimeRange.year;

    final results = await (
      _activityRepository.loadWorkoutsWithMetrics(current.start, current.end),
      _activityRepository.loadPlannedWorkouts(current.start, current.end),
      _activityRepository.loadWorkouts(
        windows.previous.start,
        windows.previous.end,
      ),
      _activityRepository.loadWorkouts(
        windows.baseline.start,
        windows.baseline.end,
      ),
      _activityRepository.loadDailySteps(current.start, current.end),
      _activityRepository.loadDailyNutrition(current.start, current.end),
      _heartRepository.loadDailyRestingHR(current.start, current.end),
      _heartRepository.loadDailyHRV(current.start, current.end),
      isYear
          ? Future<Result<List<HeartRateSample>>>.value(
              const Ok(<HeartRateSample>[]))
          : _heartRepository.loadHeartRateSamples(current.start, current.end),
    ).wait;

    final crossDailyRestingHR = results.$7.orThrow();
    return ActivitiesLoadResult(
      workouts: results.$1,
      plannedWorkouts: results.$2,
      previousWorkouts: results.$3,
      baselineWorkouts: results.$4,
      overviewDays: _activityOverviewDays(
        start: current.start,
        end: current.end,
        steps: results.$5,
        nutrition: results.$6,
        workouts: results.$1,
        heartRateSamples: results.$9.orThrow(),
        restingHeartRate: crossDailyRestingHR,
        hrv: results.$8.orThrow(),
      ),
      crossDailyRestingHR: crossDailyRestingHR,
    );
  }
}

List<ActivityOverviewDay> _activityOverviewDays({
  required LocalDate start,
  required LocalDate end,
  required List<DailySteps> steps,
  required List<DailyNutrition> nutrition,
  required List<ExerciseData> workouts,
  required List<HeartRateSample> heartRateSamples,
  required List<DailyRestingHR> restingHeartRate,
  required List<DailyHrv> hrv,
}) {
  final stepsByDate = {for (final s in steps) s.date: s};
  final nutritionByDate = {for (final n in nutrition) n.date: n};
  final hrvByDate = {for (final h in hrv) h.date: h};
  final restingByDate = {for (final r in restingHeartRate) r.date: r.bpm};
  final samplesByDate = <LocalDate, List<HeartRateSample>>{};
  for (final sample in heartRateSamples) {
    samplesByDate
        .putIfAbsent(instantToLocalDate(sample.time), () => <HeartRateSample>[])
        .add(sample);
  }
  final baselineResting = _median(restingHeartRate.map((r) => r.bpm).toList());
  final observedMax = heartRateSamples.isEmpty
      ? null
      : heartRateSamples
          .map((s) => s.beatsPerMinute)
          .reduce((a, b) => a > b ? a : b);

  final days = <ActivityOverviewDay>[];
  var date = start;
  while (!date.isAfter(end)) {
    final daySteps = stepsByDate[date];
    final dayNutrition = nutritionByDate[date];
    final dayWorkouts = _overlapping(workouts, date);
    final estimate = calculateCardioLoad(
      daySteps,
      samplesByDate[date] ?? const <HeartRateSample>[],
      restingByDate[date],
      baselineResting,
      observedMax,
      _cardioWindows(dayWorkouts, date),
    );
    days.add(ActivityOverviewDay(
      date: date,
      steps: daySteps?.steps ?? 0,
      distanceMeters: daySteps?.distanceMeters ?? 0.0,
      activeCaloriesKcal: daySteps?.activeCaloriesKcal,
      energyBurnedKcal: dayNutrition?.caloriesBurnedKcal ?? 0.0,
      energyBurnedSource:
          dayNutrition?.caloriesBurnedSource ?? CaloriesBurnedSource.noData,
      workouts: dayWorkouts,
      hrvRmssdMs: hrvByDate[date]?.rmssdMs,
      cardioLoad: estimate.score,
      cardioLoadConfidence: estimate.confidence,
    ));
    date = date.plusDays(1);
  }
  return days;
}

List<ExerciseData> _overlapping(List<ExerciseData> workouts, LocalDate date) {
  final dayStart = DateTime(date.year, date.month, date.day);
  final dayEnd = dayStart.add(const Duration(days: 1));
  final result = [
    for (final w in workouts)
      if (w.endTime.toLocal().isAfter(dayStart) &&
          w.startTime.toLocal().isBefore(dayEnd))
        w,
  ]..sort((a, b) => b.startTime.compareTo(a.startTime));
  return result;
}

List<CardioLoadTimeWindow> _cardioWindows(
  List<ExerciseData> workouts,
  LocalDate date,
) {
  final dayStart = DateTime(date.year, date.month, date.day);
  final dayEnd = dayStart.add(const Duration(days: 1));
  final windows = <CardioLoadTimeWindow>[];
  for (final w in workouts) {
    final startLocal = w.startTime.toLocal();
    final endLocal = w.endTime.toLocal();
    final windowStart = startLocal.isBefore(dayStart) ? dayStart : startLocal;
    final windowEnd = endLocal.isAfter(dayEnd) ? dayEnd : endLocal;
    final window = CardioLoadTimeWindow(start: windowStart, end: windowEnd);
    if (window.durationMinutes > 0.0) windows.add(window);
  }
  return windows;
}

int? _median(List<int> values) {
  if (values.isEmpty) return null;
  final sorted = [...values]..sort();
  return sorted[(sorted.length - 1) ~/ 2];
}
