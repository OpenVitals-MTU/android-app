import 'package:flutter/material.dart';

import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/insights/daily_goals.dart';
import '../../l10n/app_localizations.dart';
import 'insight_cards.dart';
import 'ov_card.dart';

/// Port of the Kotlin `DailyGoalComponents.kt`.

/// Kotlin `DailyGoalCard`: the current goal, how many tracked days met it, and
/// the − / + steppers that move the goal by its metric's step size.
class DailyGoalCard extends StatelessWidget {
  const DailyGoalCard({
    super.key,
    required this.goal,
    required this.progress,
    required this.icon,
    required this.accentColor,
    required this.onDecreaseGoal,
    required this.onIncreaseGoal,
  });

  final DisplayValue goal;
  final DailyGoalProgress progress;
  final IconData icon;
  final Color accentColor;
  final VoidCallback onDecreaseGoal;
  final VoidCallback onIncreaseGoal;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 22, color: accentColor),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(l10n.dailyGoal, style: theme.textTheme.titleSmall),
                        Text(
                          l10n.goalProgress(
                              progress.goalMetDays, progress.trackedDays),
                          style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                ),
                IconButton(
                  onPressed: onDecreaseGoal,
                  tooltip: l10n.cdDecreaseDailyGoal,
                  icon: const Icon(Icons.remove),
                ),
                IconButton(
                  onPressed: onIncreaseGoal,
                  tooltip: l10n.cdIncreaseDailyGoal,
                  icon: const Icon(Icons.add),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  goal.value,
                  style: theme.textTheme.headlineMedium
                      ?.copyWith(color: theme.colorScheme.onSurface),
                ),
                if (goal.unit.trim().isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(left: 6, bottom: 3),
                    child: Text(
                      goal.unit,
                      style: theme.textTheme.bodyLarge
                          ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `DailyGoalStatistics`: the five goal-oriented stat cards.
class DailyGoalStatistics extends StatelessWidget {
  const DailyGoalStatistics({
    super.key,
    required this.progress,
    required this.averageGap,
    required this.unitFormatter,
    required this.icon,
    required this.accentColor,
  });

  final DailyGoalProgress progress;
  final DisplayValue averageGap;
  final UnitFormatter unitFormatter;
  final IconData icon;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return InsightStatGrid(
      stats: [
        InsightStat(
          title: l10n.statGoalsMet,
          value: unitFormatter.count(progress.goalMetDays),
          unit: l10n.unitDays,
          icon: Icons.check_circle_outline,
          accentColor: accentColor,
        ),
        InsightStat(
          title: l10n.statSuccessRate,
          value: unitFormatter.count(progress.successRatePercent),
          unit: l10n.unitPercentSymbol,
          icon: Icons.star_outline,
          accentColor: accentColor,
        ),
        InsightStat(
          title: l10n.statGoalStreak,
          value: unitFormatter.count(progress.currentStreakDays),
          unit: l10n.unitDays,
          icon: Icons.local_fire_department_outlined,
          accentColor: accentColor,
        ),
        InsightStat(
          title: l10n.statLongestGoalStreak,
          value: unitFormatter.count(progress.longestStreakDays),
          unit: l10n.unitDays,
          icon: Icons.calendar_month_outlined,
          accentColor: accentColor,
        ),
        InsightStat(
          title: l10n.statAverageGap,
          value: averageGap.value,
          unit: averageGap.unit,
          icon: icon,
          accentColor: accentColor,
        ),
      ],
    );
  }
}
