import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/time/local_date.dart';
import '../../../domain/model/activity_models.dart';
import '../presentation/achievement_catalog.dart';

part 'achievements_display.freezed.dart';

/// Aggregate activity statistics over the scanned window. Port of Kotlin
/// `AchievementStats`.
class AchievementStats {
  const AchievementStats({
    required this.startDate,
    required this.endDate,
    this.trackedDays = 0,
    this.maxDailySteps = 0,
    this.totalDistanceMeters = 0.0,
    this.maxDailyFloors = 0,
    this.totalFloors = 0,
    this.hasFloorData = false,
  });

  final LocalDate startDate;
  final LocalDate endDate;
  final int trackedDays;
  final int maxDailySteps;
  final double totalDistanceMeters;
  final int maxDailyFloors;
  final int totalFloors;
  final bool hasFloorData;
}

/// The evaluated progress of one badge. Port of Kotlin `AchievementProgress`.
class AchievementProgress {
  const AchievementProgress({
    required this.definition,
    required this.currentValue,
    required this.progressRatio,
    required this.isUnlocked,
    required this.timesEarned,
    this.achievedOn,
  });

  final AchievementDefinition definition;
  final double currentValue;
  final double progressRatio;
  final bool isUnlocked;
  final int timesEarned;
  final LocalDate? achievedOn;
}

/// The screen-ready derivation of one loaded step history: the aggregate stats,
/// every badge's progress, the completion counters the summary card shows, and
/// the badges grouped by the category the filter chips select.
///
/// Built once per load by [buildAchievementsDisplay] and stored on the state —
/// the view-model precomputes, the widgets only render.
@freezed
abstract class AchievementsDisplay with _$AchievementsDisplay {
  const AchievementsDisplay._();

  const factory AchievementsDisplay({
    required List<AchievementProgress> badges,
    required AchievementStats stats,
    required Map<AchievementCategory, List<AchievementProgress>>
        badgesByCategory,
    required int unlockedCount,
    required int totalCount,
    required double completionRatio,
    required bool hasActivityHistory,
    required bool hasFloorHistory,
  }) = _AchievementsDisplay;

  /// The badge list behind one filter chip. `null` is the "All" chip — the
  /// filter the screen used to recompute with a `where` on every rebuild.
  List<AchievementProgress> badgesFor(AchievementCategory? category) =>
      category == null
          ? badges
          : (badgesByCategory[category] ?? const <AchievementProgress>[]);
}

/// The stats an achievements screen shows before its first load lands: the
/// legacy window, and nothing in it. The default the hand-written
/// `AchievementsState` used to build in its constructor.
AchievementStats emptyAchievementStats() => AchievementStats(
      startDate: LocalDate(2009, 1, 1),
      endDate: LocalDate.now(),
    );

/// Pure derivation from the daily-steps history to its display model. No clock,
/// no I/O — unit-testable with a fixture list. Port of Kotlin
/// `List<DailySteps>.toAchievementState`.
AchievementsDisplay buildAchievementsDisplay(
  List<DailySteps> daysUnsorted,
  LocalDate start,
  LocalDate end,
) {
  final days = [...daysUnsorted]..sort((a, b) => a.date.compareTo(b.date));
  final maxDailySteps =
      days.fold<int>(0, (m, d) => d.steps > m ? d.steps : m);
  final totalDistanceMeters =
      days.fold<double>(0.0, (sum, d) => sum + d.distanceMeters);
  final floorDays = days.where((d) => d.floorsClimbed != null).toList();
  final maxDailyFloors =
      floorDays.fold<int>(0, (m, d) => (d.floorsClimbed ?? 0) > m ? d.floorsClimbed! : m);
  final totalFloors =
      floorDays.fold<int>(0, (sum, d) => sum + (d.floorsClimbed ?? 0));
  final stats = AchievementStats(
    startDate: start,
    endDate: end,
    trackedDays: days
        .where((d) =>
            d.steps > 0 || d.distanceMeters > 0.0 || d.floorsClimbed != null)
        .length,
    maxDailySteps: maxDailySteps,
    totalDistanceMeters: totalDistanceMeters,
    maxDailyFloors: maxDailyFloors,
    totalFloors: totalFloors,
    hasFloorData: floorDays.isNotEmpty,
  );
  final badges = [
    for (final definition in achievementDefinitions)
      _progressFor(definition, days, stats),
  ];
  final unlockedCount = badges.where((b) => b.isUnlocked).length;
  final totalCount = badges.length;
  final badgesByCategory = <AchievementCategory, List<AchievementProgress>>{};
  for (final badge in badges) {
    badgesByCategory
        .putIfAbsent(badge.definition.category, () => <AchievementProgress>[])
        .add(badge);
  }
  return AchievementsDisplay(
    badges: badges,
    stats: stats,
    badgesByCategory: badgesByCategory,
    unlockedCount: unlockedCount,
    totalCount: totalCount,
    completionRatio: totalCount == 0 ? 0.0 : unlockedCount / totalCount,
    hasActivityHistory: stats.trackedDays > 0,
    hasFloorHistory: stats.hasFloorData,
  );
}

/// Port of Kotlin `AchievementDefinition.progressFor`.
AchievementProgress _progressFor(
  AchievementDefinition definition,
  List<DailySteps> days,
  AchievementStats stats,
) {
  final currentValue = switch (definition.metric) {
    AchievementMetric.dailySteps => stats.maxDailySteps.toDouble(),
    AchievementMetric.lifetimeDistanceMeters => stats.totalDistanceMeters,
    AchievementMetric.dailyFloors => stats.maxDailyFloors.toDouble(),
    AchievementMetric.lifetimeFloors => stats.totalFloors.toDouble(),
  };
  final targetRounded = definition.target.round();
  final timesEarned = switch (definition.metric) {
    AchievementMetric.dailySteps =>
      days.where((d) => d.steps >= targetRounded).length,
    AchievementMetric.dailyFloors =>
      days.where((d) => (d.floorsClimbed ?? 0) >= targetRounded).length,
    AchievementMetric.lifetimeDistanceMeters ||
    AchievementMetric.lifetimeFloors =>
      currentValue >= definition.target ? 1 : 0,
  };
  final achievedOn = switch (definition.metric) {
    AchievementMetric.dailySteps => _firstWhereDate(
        days,
        (d) => d.steps >= targetRounded,
      ),
    AchievementMetric.dailyFloors => _firstWhereDate(
        days,
        (d) => (d.floorsClimbed ?? 0) >= targetRounded,
      ),
    AchievementMetric.lifetimeDistanceMeters => _firstCumulativeDate(
        days,
        definition.target,
        (d) => d.distanceMeters,
      ),
    AchievementMetric.lifetimeFloors => _firstCumulativeDate(
        days,
        definition.target,
        (d) => (d.floorsClimbed ?? 0).toDouble(),
      ),
  };
  return AchievementProgress(
    definition: definition,
    currentValue: currentValue,
    progressRatio: definition.target <= 0.0
        ? 0.0
        : (currentValue / definition.target).clamp(0.0, 1.0),
    isUnlocked: currentValue >= definition.target,
    timesEarned: timesEarned,
    achievedOn: achievedOn,
  );
}

LocalDate? _firstWhereDate(
  List<DailySteps> days,
  bool Function(DailySteps) test,
) {
  for (final day in days) {
    if (test(day)) return day.date;
  }
  return null;
}

LocalDate? _firstCumulativeDate(
  List<DailySteps> days,
  double target,
  double Function(DailySteps) value,
) {
  var total = 0.0;
  for (final day in days) {
    total += value(day);
    if (total >= target) return day.date;
  }
  return null;
}
