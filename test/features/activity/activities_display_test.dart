import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/cardio_load.dart';
import 'package:openvitals/domain/insights/metric_interpretations.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/usecase/load_activities_use_case.dart';
import 'package:openvitals/features/activity/application/activities_display.dart';

/// The derivations the activities screen used to run in its build path — the
/// key-metric totals, the sparkline buckets, the goal/statistics folds — now a
/// pure function the view-model calls once per load, testable with no widget.
ExerciseData _workout(
  DateTime start, {
  String id = 'w',
  int type = 56,
  Duration duration = const Duration(minutes: 30),
}) =>
    ExerciseData(
      id: id,
      title: null,
      exerciseType: type,
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'test',
    );

ActivityOverviewDay _day(
  LocalDate date, {
  int steps = 0,
  double distanceMeters = 0,
  double energyBurnedKcal = 0,
  CaloriesBurnedSource energySource = CaloriesBurnedSource.noData,
  double? hrvRmssdMs,
  int cardioLoad = 0,
  CardioLoadConfidence confidence = CardioLoadConfidence.noData,
  List<ExerciseData> workouts = const <ExerciseData>[],
}) =>
    ActivityOverviewDay(
      date: date,
      steps: steps,
      distanceMeters: distanceMeters,
      activeCaloriesKcal: null,
      energyBurnedKcal: energyBurnedKcal,
      energyBurnedSource: energySource,
      workouts: workouts,
      hrvRmssdMs: hrvRmssdMs,
      cardioLoad: cardioLoad,
      cardioLoadConfidence: confidence,
    );

ActivitiesLoadResult _result({
  List<ExerciseData> workouts = const <ExerciseData>[],
  List<ExerciseData> previousWorkouts = const <ExerciseData>[],
  List<ActivityOverviewDay> overviewDays = const <ActivityOverviewDay>[],
}) =>
    ActivitiesLoadResult(
      workouts: workouts,
      plannedWorkouts: const <PlannedExerciseData>[],
      previousWorkouts: previousWorkouts,
      baselineWorkouts: const <ExerciseData>[],
      overviewDays: overviewDays,
      crossDailyRestingHR: const [],
    );

void main() {
  const monday = LocalDate(2026, 3, 2);
  const sunday = LocalDate(2026, 3, 8);
  final week = DatePeriod(monday, sunday);

  ActivitiesDisplay build(
    ActivitiesLoadResult result, {
    TimeRange range = TimeRange.week,
    DatePeriod? period,
    double dailyGoalMinutes = 30,
    List<int> availableActivityTypes = const <int>[],
    int? selectedActivityType,
  }) =>
      buildActivitiesDisplay(
        result: result,
        availableActivityTypes: availableActivityTypes,
        selectedActivityType: selectedActivityType,
        range: range,
        period: period ?? week,
        dailyGoalMinutes: dailyGoalMinutes,
      );

  group('key-metric totals', () {
    test('folds steps, distance, energy, cardio load and averages HRV', () {
      final display = build(_result(overviewDays: [
        _day(
          monday,
          steps: 9000,
          distanceMeters: 6000,
          energyBurnedKcal: 2200,
          energySource: CaloriesBurnedSource.recordedTotal,
          hrvRmssdMs: 40,
          cardioLoad: 30,
          confidence: CardioLoadConfidence.high,
        ),
        _day(
          const LocalDate(2026, 3, 3),
          steps: 7000,
          distanceMeters: 4000,
          energyBurnedKcal: 1800,
          energySource: CaloriesBurnedSource.estimatedActiveAndBmr,
          hrvRmssdMs: 60,
          cardioLoad: 20,
          confidence: CardioLoadConfidence.medium,
        ),
      ]));

      final totals = display.totals!;
      expect(totals.steps, 16000);
      expect(totals.distanceMeters, 10000);
      expect(totals.energyBurnedKcal, 4000);
      expect(totals.hasEnergyBurned, isTrue);
      expect(totals.cardioLoad, 50);
      expect(totals.hasCardioLoad, isTrue);
      // HRV is the only one that AVERAGES; the rest sum.
      expect(totals.hrvRmssdMs, 50);
      // The weakest day's confidence carries the whole period.
      expect(totals.cardioLoadConfidence, CardioLoadConfidence.medium);
      // Any estimated day makes the energy card say so.
      expect(display.energyEstimated, isTrue);
    });

    test('a day with no cardio-load reading is left out of the sum', () {
      final display = build(_result(overviewDays: [
        _day(monday, cardioLoad: 30, confidence: CardioLoadConfidence.high),
        // Never scored: it must not drag the total or the confidence down.
        _day(const LocalDate(2026, 3, 3), cardioLoad: 99),
      ]));

      final totals = display.totals!;
      expect(totals.cardioLoad, 30);
      expect(totals.cardioLoadConfidence, CardioLoadConfidence.high);
      // ...but its bucket still charts as a zero, not as a hole.
      expect(display.cardioLoadSeries, [30.0, 0.0]);
    });
  });

  group('buckets', () {
    test('a week is one bucket per day, in date order', () {
      final display = build(_result(overviewDays: [
        _day(const LocalDate(2026, 3, 4), steps: 3000),
        _day(monday, steps: 1000),
        _day(const LocalDate(2026, 3, 3), steps: 2000),
      ]));

      expect(display.bucketDates, [
        monday,
        const LocalDate(2026, 3, 3),
        const LocalDate(2026, 3, 4),
      ]);
      expect(display.stepsSeries, [1000.0, 2000.0, 3000.0]);
    });

    test('a year rolls its days up into one bucket per month', () {
      final display = build(
        _result(overviewDays: [
          _day(const LocalDate(2026, 1, 5), steps: 1000, hrvRmssdMs: 30),
          _day(const LocalDate(2026, 1, 20), steps: 2000, hrvRmssdMs: 50),
          _day(const LocalDate(2026, 2, 3), steps: 4000),
        ]),
        range: TimeRange.year,
        period: const DatePeriod(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)),
      );

      expect(display.bucketDates,
          [const LocalDate(2026, 1, 5), const LocalDate(2026, 2, 3)]);
      // Steps sum across the month; HRV averages across the days that have it.
      expect(display.stepsSeries, [3000.0, 4000.0]);
      expect(display.hrvSeries, [40.0, 0.0]);
    });

    test('more days than buckets chunk down to the cap (7)', () {
      final display = build(
        _result(overviewDays: [
          for (var day = 1; day <= 28; day++)
            _day(LocalDate(2026, 3, day), steps: 100),
        ]),
        range: TimeRange.month,
        period: const DatePeriod(LocalDate(2026, 3, 1), LocalDate(2026, 3, 28)),
      );

      expect(display.bucketDates, hasLength(7));
      // 28 days over 7 buckets: four days each, 400 steps a bucket.
      expect(display.stepsSeries, everyElement(400.0));
    });

    test('the week strip marks the days that carry a workout — week only', () {
      final workout = _workout(DateTime(2026, 3, 2, 7));
      final display = build(_result(
        workouts: [workout],
        overviewDays: [
          _day(monday, workouts: [workout]),
          _day(const LocalDate(2026, 3, 3)),
        ],
      ));

      expect(display.stripMarkers, hasLength(2));
      expect(display.stripMarkers.first.workout, workout);
      expect(display.stripMarkers.last.workout, isNull);

      final month = build(
        _result(overviewDays: [
          _day(monday, workouts: [workout]),
        ]),
        range: TimeRange.month,
      );
      expect(month.stripMarkers, isEmpty);
    });
  });

  group('statistics', () {
    test('folds the period total, average, longest and previous total', () {
      final display = build(_result(
        workouts: [
          _workout(DateTime(2026, 3, 2, 7), id: 'a'),
          _workout(DateTime(2026, 3, 3, 7),
              id: 'b', duration: const Duration(minutes: 50)),
        ],
        previousWorkouts: [
          _workout(DateTime(2026, 2, 24, 7),
              id: 'c', duration: const Duration(minutes: 20)),
        ],
      ));

      expect(display.workoutCount, 2);
      expect(display.totalDurationMs, const Duration(minutes: 80).inMilliseconds);
      expect(
          display.averageDurationMs, const Duration(minutes: 40).inMilliseconds);
      expect(
          display.longestDurationMs, const Duration(minutes: 50).inMilliseconds);
      expect(display.periodComparison.previousValue,
          const Duration(minutes: 20).inMilliseconds.toDouble());
      // The bar series is minutes per day, one entry per trained day.
      expect(display.chartValues.map((v) => v.value), [30.0, 50.0]);
      // The selected-day list is a map lookup, not a scan.
      expect(display.workoutsByDay[monday], hasLength(1));
      expect(display.workoutsByDay[const LocalDate(2026, 3, 5)], isNull);
    });

    test('the HHS guideline averages by week on a month or a year', () {
      final workouts = [
        _workout(DateTime(2026, 3, 2, 7), duration: const Duration(minutes: 140)),
      ];
      // A week of 140 logged minutes is 140 against the 150-minute reference.
      final week = build(_result(workouts: workouts));
      expect(week.guidelineUsesWeeklyAverage, isFalse);
      expect(week.guideline!.loggedMinutes, 140);

      // The same 140 minutes over a 28-day month is 35 minutes a week.
      final month = build(
        _result(workouts: workouts),
        range: TimeRange.month,
        period: const DatePeriod(LocalDate(2026, 3, 1), LocalDate(2026, 3, 28)),
      );
      expect(month.guidelineUsesWeeklyAverage, isTrue);
      expect(month.guideline!.loggedMinutes, 35);
    });

    test('the filter options are the union with the selection, by label', () {
      final display = build(
        _result(),
        // Cycling (8) and Running (56); the selection is Yoga (83).
        availableActivityTypes: const [8, 56],
        selectedActivityType: 83,
      );
      // Cycling < Running < Yoga, alphabetically.
      expect(display.filterOptions, [8, 56, 83]);
    });
  });

  test('an empty period derives zeroes and nulls, not a crash', () {
    final display = build(_result());

    expect(display.hasAnyData, isFalse);
    expect(display.hasOverviewDays, isFalse);
    expect(display.totals, isNull);
    expect(display.bucketDates, isEmpty);
    expect(display.stripMarkers, isEmpty);
    expect(display.stepsSeries, isEmpty);
    expect(display.chartValues, isEmpty);
    expect(display.workoutCount, 0);
    expect(display.totalDurationMs, 0);
    expect(display.averageDurationMs, 0);
    expect(display.longestDurationMs, 0);
    expect(display.crossInsight, isNull);
    expect(display.workoutsByDay, isEmpty);
    // A period with no workouts still has a goal to have missed.
    expect(display.goalProgress.goalMetDays, 0);
    expect(display.guideline!.status, WorkoutGuidelineStatus.noLoggedMinutes);
  });
}
