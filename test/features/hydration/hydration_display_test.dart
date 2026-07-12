import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/features/hydration/application/hydration_display.dart';

/// The derivations the hydration screen used to do in its build path — the
/// summary, the drink breakdown, the goal progress, the day curve — now a pure
/// function the view-model calls once per load, testable with no widget.
HydrationEntry _drink(
  DateTime start,
  double liters, {
  String? displayName,
}) =>
    HydrationEntry(
      id: start.toIso8601String(),
      startTime: start,
      endTime: start.add(const Duration(minutes: 1)),
      liters: liters,
      source: 'test',
      displayName: displayName,
    );

void main() {
  final monday = LocalDate(2026, 3, 2);
  final morning = DateTime(2026, 3, 2, 8);

  test('an empty period derives zeroes, not nulls', () {
    final display = buildHydrationDisplay(
      const [],
      const [],
      dailyGoalLiters: 2.0,
    );

    expect(display.hasData, isFalse);
    expect(display.summary.totalLiters, 0.0);
    expect(display.summary.averageLiters, 0.0);
    expect(display.summary.goalSuccessRatePercent, 0);
    expect(display.chartValues, isEmpty);
    expect(display.cumulativeSamples, isEmpty);
    expect(display.drinkBreakdown, isEmpty);
    // Never zero: the breakdown bars divide by it.
    expect(display.maxDrinkLiters, 1.0);
    expect(display.goalProgress, 0.0);
  });

  test('the summary folds totals, tracked days and the best day', () {
    final display = buildHydrationDisplay(
      [
        DailyHydration(date: monday, liters: 2.5),
        DailyHydration(date: monday.plusDays(1), liters: 0.0),
        DailyHydration(date: monday.plusDays(2), liters: 1.5),
      ],
      const [],
      dailyGoalLiters: 2.0,
    );

    final summary = display.summary;
    expect(display.hasData, isTrue);
    expect(summary.totalLiters, 4.0);
    expect(summary.loggedDays, 3);
    // A day with nothing logged is not a tracked day, and the average is over
    // the tracked ones.
    expect(summary.trackedDays, 2);
    expect(summary.averageLiters, 2.0);
    expect(summary.bestDayLiters, 2.5);
    expect(summary.goalMetDays, 1);
    expect(summary.goalSuccessRatePercent, 50);
  });

  test('the goal streak is the trailing one, the longest is the best one', () {
    final display = buildHydrationDisplay(
      [
        DailyHydration(date: monday, liters: 2.2),
        DailyHydration(date: monday.plusDays(1), liters: 2.4),
        // A missed day breaks the streak…
        DailyHydration(date: monday.plusDays(2), liters: 0.5),
        // …and the trailing streak starts over.
        DailyHydration(date: monday.plusDays(3), liters: 2.1),
      ],
      const [],
      dailyGoalLiters: 2.0,
    );

    expect(display.summary.currentGoalStreakDays, 1);
    expect(display.summary.longestGoalStreakDays, 2);
  });

  test('the drink breakdown sums by name, biggest first, and scales itself', () {
    final display = buildHydrationDisplay(
      [DailyHydration(date: monday, liters: 1.3)],
      [
        _drink(morning, 0.3, displayName: 'Green tea'),
        _drink(morning.add(const Duration(hours: 2)), 0.5,
            displayName: 'Flat white'),
        _drink(morning.add(const Duration(hours: 4)), 0.2,
            displayName: 'Green tea'),
        // No name at all: a bare hydration record from another app.
        _drink(morning.add(const Duration(hours: 6)), 0.3),
        // No volume: a nutrition-only beverage never enters the breakdown.
        _drink(morning.add(const Duration(hours: 7)), 0.0,
            displayName: 'Espresso'),
      ],
      dailyGoalLiters: 2.0,
    );

    expect(display.drinkBreakdown.length, 3);
    expect(display.drinkBreakdown.first.label, 'Green tea');
    expect(display.drinkBreakdown.first.liters, closeTo(0.5, 0.0001));
    expect(display.drinkBreakdown[1].label, 'Flat white');
    // The nameless slice is null-labelled — the screen names it; a package name
    // is never a drink name.
    expect(display.drinkBreakdown.last.label, isNull);
    expect(display.maxDrinkLiters, closeTo(0.5, 0.0001));
    expect(display.topDrinkSlices.length, 3);
  });

  test('the day curve accumulates, in time order, skipping empty drinks', () {
    final display = buildHydrationDisplay(
      [DailyHydration(date: monday, liters: 0.8)],
      [
        _drink(morning.add(const Duration(hours: 5)), 0.5),
        _drink(morning, 0.3),
        _drink(morning.add(const Duration(hours: 2)), 0.0),
      ],
      dailyGoalLiters: 2.0,
    );

    expect(display.cumulativeSamples.map((s) => s.value).toList(),
        [0.3, closeTo(0.8, 0.0001)]);
    expect(display.cumulativeSamples.first.time, morning);
  });

  test('the entry list is newest first', () {
    final display = buildHydrationDisplay(
      const [],
      [
        _drink(morning, 0.3),
        _drink(morning.add(const Duration(hours: 5)), 0.5),
      ],
      dailyGoalLiters: 2.0,
    );

    expect(display.entriesNewestFirst.first.startTime,
        morning.add(const Duration(hours: 5)));
    expect(display.entriesNewestFirst.last.startTime, morning);
  });

  test('goal progress is the daily average against the goal, clamped', () {
    final display = buildHydrationDisplay(
      [DailyHydration(date: monday, liters: 3.0)],
      const [],
      dailyGoalLiters: 2.0,
    );
    expect(display.goalProgress, 1.0);

    final zeroGoal = buildHydrationDisplay(
      [DailyHydration(date: monday, liters: 3.0)],
      const [],
      dailyGoalLiters: 0.0,
    );
    expect(zeroGoal.goalProgress, 0.0);
  });
}
