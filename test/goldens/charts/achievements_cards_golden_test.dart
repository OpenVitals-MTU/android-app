@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/achievements/application/achievements_view_model.dart';
import 'package:openvitals/features/achievements/presentation/achievement_catalog.dart';
import 'package:openvitals/features/achievements/presentation/achievements_screen.dart';

import '../../support/golden_harness.dart';

/// [AchievementSummaryCard] and [AchievementBadgeCard] — two more of the nine
/// proportional bars, and the two that do the least to make themselves legible.
///
/// Both are a bare [LinearProgressIndicator] behind a [ClipRRect], with no label
/// on the bar itself: everything the bar means is in the text around it. So a
/// consolidation could swap the bar for one that fills the other way, or that
/// rounds a 3% badge down to an empty track, and every assertion in the suite
/// would still hold. These are the pictures that would not.
///
/// The badge card also changes its whole CONTAINER on unlock — a tinted fill, a
/// lit icon, a tick instead of a padlock — so locked and unlocked are two
/// different cards, not one card with a longer bar.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  // The legacy window the screen actually scans, on the golden clock. Never
  // `LocalDate.now()`: `emptyAchievementStats()` reaches for it, and a card that
  // prints today's date draws a different picture every day the suite runs.
  const stats = AchievementStats(
    startDate: LocalDate(2009, 1, 1),
    endDate: LocalDate(2026, 6, 22),
    trackedDays: 2841,
    maxDailySteps: 28450,
    totalDistanceMeters: 9_640_000,
    maxDailyFloors: 62,
    totalFloors: 18400,
    hasFloorData: true,
  );

  const stepBadge = AchievementDefinition(
    id: 'daily_steps_20k',
    name: 'Twenty thousand',
    category: AchievementCategory.dailySteps,
    metric: AchievementMetric.dailySteps,
    target: 20000,
  );
  const distanceBadge = AchievementDefinition(
    id: 'lifetime_distance_10000k',
    name: 'Ten thousand kilometres',
    category: AchievementCategory.lifetimeDistance,
    metric: AchievementMetric.lifetimeDistanceMeters,
    target: 10_000_000,
  );
  const floorBadge = AchievementDefinition(
    id: 'daily_floors_100',
    name: 'Century of stairs',
    category: AchievementCategory.dailyFloors,
    metric: AchievementMetric.dailyFloors,
    target: 100,
  );

  testWidgets('the summary card, part way through the catalogue',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementSummaryCard(
        state: const AchievementsState(
          isLoading: false,
          display: AchievementsDisplay(
            badges: [],
            stats: stats,
            badgesByCategory: {},
            unlockedCount: 14,
            totalCount: 32,
            completionRatio: 14 / 32,
            hasActivityHistory: true,
            hasFloorHistory: true,
          ),
        ),
        formatter: formatter,
        onRefresh: () async {},
      ),
      name: 'achievements_summary',
    );
  });

  testWidgets('the summary card before the first load lands', (tester) async {
    // No display at all: every counter falls back to zero and the bar to an empty
    // track. This is what the screen shows for the first frame of every visit,
    // and it is the state in which a bar that fills the wrong way is invisible.
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementSummaryCard(
        state: const AchievementsState(
          isLoading: false,
          display: AchievementsDisplay(
            badges: [],
            stats: stats,
            badgesByCategory: {},
            unlockedCount: 0,
            totalCount: 32,
            completionRatio: 0,
            hasActivityHistory: false,
            hasFloorHistory: false,
          ),
        ),
        formatter: formatter,
        onRefresh: () async {},
      ),
      name: 'achievements_summary_empty',
    );
  });

  testWidgets('a badge mid-progress — locked, and honest about it',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementBadgeCard(
        progress: const AchievementProgress(
          definition: stepBadge,
          currentValue: 14200,
          progressRatio: 0.71,
          isUnlocked: false,
          timesEarned: 0,
        ),
        formatter: formatter,
      ),
      name: 'achievements_badge_locked',
    );
  });

  testWidgets('a badge barely started', (tester) async {
    // 3% of the way to a hundred floors. The bar has almost nothing to draw, and
    // "almost nothing" and "nothing" have to stay distinguishable — otherwise the
    // card claims you have not started climbing.
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementBadgeCard(
        progress: const AchievementProgress(
          definition: floorBadge,
          currentValue: 3,
          progressRatio: 0.03,
          isUnlocked: false,
          timesEarned: 0,
        ),
        formatter: formatter,
      ),
      name: 'achievements_badge_barely_started',
    );
  });

  testWidgets('a badge earned — the whole card changes, not just the bar',
      (tester) async {
    // Unlocked: the container takes the category's tint, the icon lights up, the
    // padlock becomes a tick, and the bar is full. `progressRatio` is over 1 here
    // on purpose — you can walk past a target, and the card clamps rather than
    // overrunning its own track.
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementBadgeCard(
        progress: AchievementProgress(
          definition: distanceBadge,
          currentValue: 9_640_000,
          progressRatio: 1.24,
          isUnlocked: true,
          timesEarned: 1,
          achievedOn: const LocalDate(2026, 4, 8),
        ),
        formatter: formatter,
      ),
      name: 'achievements_badge_unlocked',
    );
  });

  testWidgets('a badge earned more than once', (tester) async {
    // A repeatable daily badge: the status line counts the times rather than
    // naming a date, which is the only branch of `_statusText` a single unlocked
    // shot would miss.
    await expectChartGoldenBothThemes(
      tester,
      () => AchievementBadgeCard(
        progress: const AchievementProgress(
          definition: stepBadge,
          currentValue: 28450,
          progressRatio: 1.42,
          isUnlocked: true,
          timesEarned: 9,
        ),
        formatter: formatter,
      ),
      name: 'achievements_badge_repeat',
    );
  });
}
