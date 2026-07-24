import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/time_range.dart';
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

/// The week of Mon 2 Mar 2026, seen from a point well after it ended — so the
/// whole 7 days count as elapsed.
final _week = DatePeriod(LocalDate(2026, 3, 2), LocalDate(2026, 3, 8));
final _afterTheWeek = LocalDate(2026, 3, 20);

HydrationDisplay _display(
  List<DailyHydration> days,
  List<HydrationEntry> entries, {
  double dailyGoalLiters = 2.0,
  DatePeriod? period,
  LocalDate? today,
}) =>
    buildHydrationDisplay(
      days,
      entries,
      dailyGoalLiters: dailyGoalLiters,
      period: period ?? _week,
      today: today ?? _afterTheWeek,
    );

void main() {
  final monday = LocalDate(2026, 3, 2);
  final morning = DateTime(2026, 3, 2, 8);

  test('an empty period derives zeroes, not nulls', () {
    final display = _display(const [], const []);

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
    final display = _display(
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
    final display = _display(
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

  test('an unmet TODAY does not break the trailing streak', () {
    // Yesterday met the goal; today (0.3 L so far) has not — yet. The day is
    // still in progress, so the streak must survive until today genuinely
    // fails: without the skip it collapsed to 0 at midnight every night.
    final display = _display(
      [
        DailyHydration(date: monday, liters: 2.2),
        DailyHydration(date: monday.plusDays(1), liters: 2.4),
        DailyHydration(date: monday.plusDays(2), liters: 0.3),
      ],
      const [],
      dailyGoalLiters: 2.0,
      period: DatePeriod(monday, monday.plusDays(2)),
      today: monday.plusDays(2),
    );

    expect(display.summary.currentGoalStreakDays, 2);
    // The longest streak is history, not a live countdown — unchanged.
    expect(display.summary.longestGoalStreakDays, 2);
  });

  test('an unmet PAST day still breaks the trailing streak', () {
    final display = _display(
      [
        DailyHydration(date: monday, liters: 2.2),
        DailyHydration(date: monday.plusDays(1), liters: 0.3),
      ],
      const [],
      dailyGoalLiters: 2.0,
      period: DatePeriod(monday, monday.plusDays(1)),
      today: monday.plusDays(5),
    );

    expect(display.summary.currentGoalStreakDays, 0);
  });

  test('the drink breakdown sums by name, biggest first, and scales itself', () {
    final display = _display(
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
    final display = _display(
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
    final display = _display(
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

  test('a single day over the goal is one day of seven, not a full bar', () {
    // This test used to assert goalProgress == 1.0 here, because progress was
    // the average of the LOGGED days over the goal. One day at 3L of a 2L goal
    // filled the bar. That was the bug; the assertion moved with it.
    final display = _display(
      [DailyHydration(date: monday, liters: 3.0)],
      const [],
      dailyGoalLiters: 2.0,
    );
    expect(display.goalProgress, closeTo(1 / 7, 0.001));

    // A goal of zero cannot be met, and cannot divide.
    final zeroGoal = _display(
      [DailyHydration(date: monday, liters: 3.0)],
      const [],
      dailyGoalLiters: 0.0,
    );
    expect(zeroGoal.goalProgress, 0.0);
  });

  group('the goal bar measures the period, not the days you logged', () {
    // It used to divide the average of the days you LOGGED by the goal. So a
    // week in which you logged Monday, hit your goal, and never opened the app
    // again showed a full bar and "1 of 1 days met" — the bar rewarded you for
    // logging less. It now measures the days that actually happened.
    test('one logged day in a seven-day week does not fill the bar', () {
      final display = _display(
        [DailyHydration(date: monday, liters: 2.5)],
        const [],
      );

      expect(display.summary.goalMetDays, 1);
      expect(display.summary.elapsedDays, 7);
      expect(display.goalProgress, closeTo(1 / 7, 0.001));
    });

    test('meeting the goal every day of the week fills it', () {
      final display = _display(
        [
          for (var i = 0; i < 7; i++)
            DailyHydration(date: monday.plusDays(i), liters: 2.5),
        ],
        const [],
      );

      expect(display.goalProgress, 1.0);
    });

    test('a goal you have not had the chance to miss yet does not count', () {
      // Wednesday of the current week: three days have happened, not seven.
      final display = _display(
        [
          DailyHydration(date: monday, liters: 2.5),
          DailyHydration(date: monday.plusDays(1), liters: 2.5),
        ],
        const [],
        today: LocalDate(2026, 3, 4),
      );

      expect(display.summary.elapsedDays, 3);
      expect(display.goalProgress, closeTo(2 / 3, 0.001));
    });
  });
}