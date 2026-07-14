import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../ui/charts/chart_bar_row.dart';
import '../../../ui/components/loading_state.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import 'achievement_catalog.dart';
import '../application/achievements_view_model.dart';
import '../../../ui/components/section_padding.dart';

/// Achievements badge grid, ported from the Kotlin `AchievementsScreen` +
/// `AchievementsContent`. Shows a legacy-progress summary, an aggregate stats
/// row, category filter chips, and the earned/locked badge list with per-badge
/// progress.
class AchievementsScreen extends ConsumerStatefulWidget {
  const AchievementsScreen({super.key});

  @override
  ConsumerState<AchievementsScreen> createState() => _AchievementsScreenState();
}

class _AchievementsScreenState extends ConsumerState<AchievementsScreen> {
  AchievementCategory? _selectedCategory;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(achievementsProvider);
    final notifier = ref.read(achievementsProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);

    final Widget body;
    if (state.isLoading && state.badges.isEmpty) {
      body = const FullScreenLoading();
    } else {
      // The category filter is precomputed per chip in the display; the screen
      // picks a list, it does not fold one.
      final filtered = state.display?.badgesFor(_selectedCategory) ??
          const <AchievementProgress>[];
      body = Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 920),
          child: ListView(
            padding: screenScrollPadding(context),
            children: [
              sectionPadded(
                AchievementSummaryCard(
                  state: state,
                  formatter: formatter,
                  onRefresh: notifier.refresh,
                ),
              ),
              const SizedBox(height: 8),
              _AchievementStatsRow(stats: state.stats, formatter: formatter),
              const SizedBox(height: 8),
              _AchievementFilters(
                selected: _selectedCategory,
                onSelect: (category) =>
                    setState(() => _selectedCategory = category),
              ),
              if (state.error != null)
                sectionPadded(_MessageCard(
                  title: 'Unable to load achievements',
                  body: _errorText(state.error!),
                ))
              else if (!state.hasActivityHistory)
                sectionPadded(const _MessageCard(
                  title: 'No activity history yet',
                  body: 'Start recording steps to unlock achievements.',
                )),
              if (state.hasActivityHistory && !state.hasFloorHistory)
                sectionPadded(const _MessageCard(
                  title: 'No floor data',
                  body: 'Floor-climbing badges need a device that tracks floors.',
                )),
              for (final progress in filtered)
                sectionPadded(AchievementBadgeCard(
                  progress: progress,
                  formatter: formatter,
                )),
              const SizedBox(height: 16),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Achievements')),
      body: body,
    );
  }
}

String _errorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Not found.',
      ScreenErrorMissingArgument() => 'Something went wrong.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };

String _formatDate(LocalDate date) =>
    DateFormat('d MMM yyyy').format(DateTime(date.year, date.month, date.day));

/// Public so the chart goldens can photograph it on its own.
///
/// It draws one of the app's nine hand-rolled "labelled proportional bar" copies
/// (and, for the curve card, one of its three line renderings). Those are about to
/// be consolidated, and a picture of each — taken BEFORE — is what proves the
/// consolidation changed nothing it did not mean to.
@visibleForTesting
class AchievementSummaryCard extends StatelessWidget {
  const AchievementSummaryCard({
    super.key,
    required this.state,
    required this.formatter,
    required this.onRefresh,
  });

  final AchievementsState state;
  final UnitFormatter formatter;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                _IconBadge(
                  icon: Icons.workspace_premium,
                  color: theme.colorScheme.primary,
                  isUnlocked: true,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Legacy achievements',
                        style: theme.textTheme.titleLarge
                            ?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        '${state.unlockedCount} of ${state.totalCount} unlocked',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: onRefresh,
                  icon: const Icon(Icons.refresh),
                  tooltip: 'Refresh',
                ),
              ],
            ),
            const SizedBox(height: 12),
            ChartBarRow(
              fraction: state.completionRatio,
              color: theme.colorScheme.primary,
              // This one bar took its colours from LinearProgressIndicator's M3
              // defaults rather than naming them, and that track is
              // `secondaryContainer` — not the `surfaceContainerHighest` every
              // other bar in the app uses. Named, not changed: retinting it is a
              // design decision with a golden diff behind it, not a side effect of
              // deleting a widget.
              trackColor: theme.colorScheme.secondaryContainer,
              height: 8,
              radius: 8,
            ),
            const SizedBox(height: 12),
            Text(
              '${_formatDate(state.stats.startDate)} – '
              '${_formatDate(state.stats.endDate)} · '
              '${formatter.count(state.stats.trackedDays)} tracked days',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AchievementStatsRow extends StatelessWidget {
  const _AchievementStatsRow({required this.stats, required this.formatter});

  final AchievementStats stats;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final cards = <(String, String)>[
      ('Tracked days', formatter.count(stats.trackedDays)),
      ('Best steps', formatter.count(stats.maxDailySteps)),
      ('Total distance', formatter.distance(stats.totalDistanceMeters).text),
      ('Best floors', formatter.count(stats.maxDailyFloors)),
      ('Total floors', formatter.count(stats.totalFloors)),
    ];
    return SizedBox(
      height: 78,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: cards.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) =>
            _AchievementStatCard(label: cards[index].$1, value: cards[index].$2),
      ),
    );
  }
}

class _AchievementStatCard extends StatelessWidget {
  const _AchievementStatCard({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      width: 156,
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 4),
              Text(
                value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.w600),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AchievementFilters extends StatelessWidget {
  const _AchievementFilters({required this.selected, required this.onSelect});

  final AchievementCategory? selected;
  final ValueChanged<AchievementCategory?> onSelect;

  @override
  Widget build(BuildContext context) {
    final categories = <AchievementCategory?>[null, ...AchievementCategory.values];
    return SizedBox(
      height: 48,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: categories.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final category = categories[index];
          return Align(
            alignment: Alignment.center,
            child: FilterChip(
              selected: selected == category,
              onSelected: (_) => onSelect(category),
              label: Text(category?.label ?? 'All'),
            ),
          );
        },
      ),
    );
  }
}

/// Public so the chart goldens can photograph it on its own.
///
/// It draws one of the app's nine hand-rolled "labelled proportional bar" copies
/// (and, for the curve card, one of its three line renderings). Those are about to
/// be consolidated, and a picture of each — taken BEFORE — is what proves the
/// consolidation changed nothing it did not mean to.
@visibleForTesting
class AchievementBadgeCard extends StatelessWidget {
  const AchievementBadgeCard({super.key,required this.progress, required this.formatter});

  final AchievementProgress progress;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final category = progress.definition.category;
    final color = category.color;
    final target = _targetDisplay(progress.definition, formatter);
    final current = _currentDisplay(progress, formatter);
    final container = progress.isUnlocked
        ? color.withValues(alpha: 0.12)
        : theme.colorScheme.surfaceContainer;
    return OpenVitalsCard(
      color: container,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _IconBadge(
                  icon: category.icon,
                  color: color,
                  isUnlocked: progress.isUnlocked,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              progress.definition.name,
                              style: theme.textTheme.titleMedium
                                  ?.copyWith(fontWeight: FontWeight.w600),
                            ),
                          ),
                          Icon(
                            progress.isUnlocked
                                ? Icons.check_circle
                                : Icons.lock_outline,
                            size: 20,
                            color: progress.isUnlocked
                                ? color
                                : theme.colorScheme.onSurfaceVariant,
                          ),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _requirementText(progress.definition, target),
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ChartBarRow(
              fraction: progress.progressRatio,
              color: color,
              height: 8,
              radius: 8,
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    '$current / $target',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
                Text(
                  _statusText(progress),
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: progress.isUnlocked
                        ? color
                        : theme.colorScheme.onSurfaceVariant,
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

class _IconBadge extends StatelessWidget {
  const _IconBadge({
    required this.icon,
    required this.color,
    required this.isUnlocked,
  });

  final IconData icon;
  final Color color;
  final bool isUnlocked;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: color.withValues(alpha: isUnlocked ? 0.20 : 0.10),
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Icon(icon, color: color, size: 26),
    );
  }
}

class _MessageCard extends StatelessWidget {
  const _MessageCard({required this.title, this.body});

  final String title;
  final String? body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
            if (body != null && body!.isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(
                body!,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

String _requirementText(AchievementDefinition definition, String target) {
  switch (definition.metric) {
    case AchievementMetric.dailySteps:
      return 'Reach $target steps in a single day';
    case AchievementMetric.lifetimeDistanceMeters:
      return 'Walk $target in total';
    case AchievementMetric.dailyFloors:
      return 'Climb $target floors in a single day';
    case AchievementMetric.lifetimeFloors:
      return 'Climb $target floors in total';
  }
}

String _targetDisplay(AchievementDefinition definition, UnitFormatter f) {
  switch (definition.metric) {
    case AchievementMetric.dailySteps:
    case AchievementMetric.dailyFloors:
    case AchievementMetric.lifetimeFloors:
      return f.count(definition.target.round());
    case AchievementMetric.lifetimeDistanceMeters:
      return f.distance(definition.target).text;
  }
}

String _currentDisplay(AchievementProgress progress, UnitFormatter f) {
  switch (progress.definition.metric) {
    case AchievementMetric.dailySteps:
    case AchievementMetric.dailyFloors:
    case AchievementMetric.lifetimeFloors:
      return f.count(progress.currentValue.round());
    case AchievementMetric.lifetimeDistanceMeters:
      return f.distance(progress.currentValue).text;
  }
}

String _statusText(AchievementProgress progress) {
  if (progress.timesEarned > 1) return 'Earned ${progress.timesEarned}x';
  if (progress.isUnlocked && progress.achievedOn != null) {
    return 'Achieved ${_formatDate(progress.achievedOn!)}';
  }
  if (progress.isUnlocked) return 'Earned';
  return 'Locked';
}

