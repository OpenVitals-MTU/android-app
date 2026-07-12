// The badge evaluation is a pure derivation now: a step history in, a display
// out. It used to be reachable only through the notifier (and, before that, only
// through the screen).

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/achievements/application/achievements_display.dart';
import 'package:openvitals/features/achievements/presentation/achievement_catalog.dart';

final LocalDate _start = LocalDate(2024, 1, 1);
final LocalDate _end = LocalDate(2024, 1, 31);

DailySteps _day(
  int day, {
  int steps = 0,
  double distanceMeters = 0,
  int? floorsClimbed,
}) =>
    DailySteps(
      date: LocalDate(2024, 1, day),
      steps: steps,
      distanceMeters: distanceMeters,
      floorsClimbed: floorsClimbed,
    );

AchievementProgress _badge(AchievementsDisplay display, String name) =>
    display.badges.firstWhere((b) => b.definition.name == name);

void main() {
  test('an empty history unlocks nothing and reports no activity', () {
    final display = buildAchievementsDisplay(const <DailySteps>[], _start, _end);

    expect(display.badges, isNotEmpty, reason: 'the catalog still renders');
    expect(display.unlockedCount, 0);
    expect(display.completionRatio, 0.0);
    expect(display.hasActivityHistory, isFalse);
    expect(display.hasFloorHistory, isFalse);
    expect(display.stats.trackedDays, 0);
    expect(display.stats.maxDailySteps, 0);
    expect(display.stats.totalDistanceMeters, 0.0);
    for (final badge in display.badges) {
      expect(badge.isUnlocked, isFalse, reason: badge.definition.name);
      expect(badge.progressRatio, 0.0);
      expect(badge.achievedOn, isNull);
      expect(badge.timesEarned, 0);
    }
  });

  test('the stats aggregate the whole window', () {
    final display = buildAchievementsDisplay(
      [
        _day(3, steps: 12000, distanceMeters: 8000, floorsClimbed: 12),
        _day(1, steps: 5000, distanceMeters: 3000),
        _day(2), // a blank day is not a tracked one
      ],
      _start,
      _end,
    );

    expect(display.stats.trackedDays, 2);
    expect(display.stats.maxDailySteps, 12000);
    expect(display.stats.totalDistanceMeters, 11000.0);
    expect(display.stats.maxDailyFloors, 12);
    expect(display.stats.totalFloors, 12);
    expect(display.stats.hasFloorData, isTrue);
    expect(display.stats.startDate, _start);
    expect(display.stats.endDate, _end);
  });

  test('a badge is earned on the first day that reaches its target', () {
    final display = buildAchievementsDisplay(
      [
        _day(1, steps: 4000),
        _day(2, steps: 12000),
        _day(3, steps: 9000),
      ],
      _start,
      _end,
    );

    // Boat Shoes is the 5k daily-steps badge.
    final boatShoes = _badge(display, 'Boat Shoes');
    expect(boatShoes.isUnlocked, isTrue);
    expect(boatShoes.currentValue, 12000.0);
    expect(boatShoes.progressRatio, 1.0);
    // Earned on both the 12k and the 9k day, first reached on the 12k one.
    expect(boatShoes.timesEarned, 2);
    expect(boatShoes.achievedOn, LocalDate(2024, 1, 2));

    expect(display.unlockedCount, greaterThan(0));
    expect(
      display.completionRatio,
      closeTo(display.unlockedCount / display.totalCount, 1e-9),
    );
  });

  test('a locked badge carries its partial progress, clamped', () {
    final display = buildAchievementsDisplay(
      [_day(1, steps: 5000)],
      _start,
      _end,
    );

    // Sneakers is the 10k daily-steps badge: half way there.
    final sneakers = _badge(display, 'Sneakers');
    expect(sneakers.isUnlocked, isFalse);
    expect(sneakers.progressRatio, closeTo(0.5, 1e-9));
    expect(sneakers.achievedOn, isNull);
  });

  test('the category filter is precomputed per chip', () {
    final display = buildAchievementsDisplay(
      [_day(1, steps: 5000)],
      _start,
      _end,
    );

    // "All" is the whole list; a chip is exactly its category's badges.
    expect(display.badgesFor(null), display.badges);
    for (final category in AchievementCategory.values) {
      final filtered = display.badgesFor(category);
      expect(
        filtered,
        display.badges.where((b) => b.definition.category == category).toList(),
        reason: category.name,
      );
    }
  });

  test('a history with no floor data leaves the floor badges unearned', () {
    final display = buildAchievementsDisplay(
      [_day(1, steps: 20000, distanceMeters: 15000)],
      _start,
      _end,
    );

    expect(display.hasFloorHistory, isFalse);
    expect(display.stats.maxDailyFloors, 0);
    for (final badge in display.badges.where(
      (b) => b.definition.category == AchievementCategory.dailyFloors,
    )) {
      expect(badge.isUnlocked, isFalse);
    }
  });
}
