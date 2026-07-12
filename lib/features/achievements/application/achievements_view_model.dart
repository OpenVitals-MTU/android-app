import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/activity_models.dart';
import '../presentation/achievement_catalog.dart';

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

/// The Riverpod port of the Kotlin `AchievementsUiState`.
class AchievementsState {
  AchievementsState({
    this.isLoading = true,
    this.badges = const <AchievementProgress>[],
    AchievementStats? stats,
    this.error,
  }) : stats = stats ??
            AchievementStats(
              startDate: LocalDate(2009, 1, 1),
              endDate: LocalDate.now(),
            );

  final bool isLoading;
  final List<AchievementProgress> badges;
  final AchievementStats stats;
  final ScreenError? error;

  int get unlockedCount => badges.where((b) => b.isUnlocked).length;
  int get totalCount => badges.length;
  double get completionRatio =>
      totalCount == 0 ? 0.0 : unlockedCount / totalCount;
  bool get hasActivityHistory => stats.trackedDays > 0;
  bool get hasFloorHistory => stats.hasFloorData;

  AchievementsState copyWith({
    bool? isLoading,
    List<AchievementProgress>? badges,
    AchievementStats? stats,
    ScreenError? error,
  }) =>
      AchievementsState(
        isLoading: isLoading ?? this.isLoading,
        badges: badges ?? this.badges,
        stats: stats ?? this.stats,
        error: error,
      );
}

/// The Riverpod port of the Kotlin `AchievementsViewModel`. Loads the full
/// legacy daily-steps history from the [ActivityRepository] and evaluates each
/// badge's earned/progress state from it.
class AchievementsViewModel extends Notifier<AchievementsState> {
  int _generation = 0;

  @override
  AchievementsState build() {
    // Kick off the first load (matching the Kotlin `init { load() }`).
    Future.microtask(load);
    return AchievementsState();
  }

  Future<void> load() async {
    final generation = ++_generation;
    state = state.copyWith(isLoading: true, error: null);
    try {
      // How far back a lifetime badge counts is the use case's business; what
      // the badges make of the history is this screen's.
      final history =
          (await ref.read(loadAchievementHistoryUseCaseProvider)()).orThrow();
      if (!ref.mounted || generation != _generation) return;
      state = _evaluate(history.days, history.start, history.end);
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
      );
    }
  }

  Future<void> refresh() => load();
}

/// Builds the achievement state from the daily-steps history. Port of Kotlin
/// `List<DailySteps>.toAchievementState`.
AchievementsState _evaluate(
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
  return AchievementsState(isLoading: false, badges: badges, stats: stats);
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

/// The state provider for the achievements screen.
final achievementsProvider =
    NotifierProvider<AchievementsViewModel, AchievementsState>(
  AchievementsViewModel.new,
);
