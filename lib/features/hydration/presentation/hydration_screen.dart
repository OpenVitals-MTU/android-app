import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/paginated_entry_list.dart';
import '../../../ui/theme/app_colors.dart';
import '../../../data/source/health/health_permissions.dart';
import 'hydration_intraday_chart.dart';
import '../application/hydration_view_model.dart';
import '../reminders/hydration_reminder_card.dart';
import '../../../ui/components/section_padding.dart';

/// The hydration period-detail screen, ported from the Kotlin `HydrationScreen`.
///
/// A [MetricDetailScaffold] (HYDRATION range key) rendering the daily hydration
/// bar chart, the goal-progress card, the drink-type breakdown, the period
/// statistics and the reminder settings card. The "+ add drink" action routes to
/// the (existing) add-entry screen; inline quick-add is Phase 6.
class HydrationScreen extends ConsumerWidget {
  const HydrationScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(hydrationProvider);
    final notifier = ref.read(hydrationProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Hydration'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Add drink',
            // TODO(phase6): the Kotlin screen offers inline quick-add of a
            // logged drink here; for now route to the manual add-entry screen.
            onPressed: () => context.push(AppRoutes.hydrationEntry),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readHydration},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.hydration,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) =>
              _content(context, state, formatter, period, weekMode),
        ),
      ),
    );
  }
}

List<Widget> _content(
  BuildContext context,
  HydrationState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  if (!state.hasData) {
    if (state.isLoading) {
      return const [
        Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        ),
      ];
    }
    return [
      sectionPadded(
        MetricCardPlaceholder(
          title: 'Hydration',
          icon: Icons.local_drink_outlined,
          accentColor: AppColors.hydration,
          message: state.entries.isEmpty
              ? 'No hydration logged for this period.'
              : 'No hydration added for this period.',
        ),
      ),
      // Reminders are configurable with no data logged yet — that is exactly
      // when a user wants to switch them on.
      sectionPadded(const HydrationReminderCard()),
      // Kotlin renders its ENTRIES section in the empty branch too: a period can
      // hold nutrition-only beverages (a drink with nutrients but no volume),
      // which log no litres yet still belong in the history.
      if (state.entries.isNotEmpty)
        _HydrationEntriesContent(entries: state.entries, formatter: formatter),
    ];
  }

  final summary = state.summary;
  final total = formatter.hydration(summary.totalLiters);
  final values = [
    for (final day in state.dailyHydration)
      PeriodChartValue(day.date, day.liters),
  ];

  return [
    sectionPadded(
      Row(
        children: [
          Expanded(
            child: MetricCard(
              title: 'Total hydration',
              value: total.value,
              unit: total.unit,
              icon: Icons.local_drink_outlined,
              accentColor: AppColors.hydration,
              subtitle:
                  'Daily average ${formatter.hydration(summary.averageLiters).text}',
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: MetricCard(
              title: 'Logged days',
              value: formatter.count(summary.trackedDays),
              unit: 'days',
              icon: Icons.local_drink_outlined,
              accentColor: AppColors.hydration,
              subtitle: '${formatter.count(summary.loggedDays)} in range',
            ),
          ),
        ],
      ),
    ),
    // A day is not a bar. Kotlin branched here too: the DAY view draws the day's
    // hydration accumulating through its hours, while week/month/year draw one bar
    // per day. Rendering the bar chart for a single day gave one fat bar labelled
    // "Sun 12" — restating a number the two cards above it already show, and
    // throwing away the only thing the day chart is for: WHEN you drank it.
    sectionPadded(
      state.selectedRange == TimeRange.day
          ? HydrationIntradayChartCard(
              selectedDate: state.selectedDate,
              entries: state.entries,
              dailyGoalLiters: state.dailyGoalLiters,
              formatter: formatter,
            )
          : MetricBarChart(
              title: 'Hydration',
              values: values,
              selectedRange: state.selectedRange,
              period: period,
              accentColor: AppColors.hydration,
              summaryValue: total.text,
              weekPeriodMode: weekPeriodMode,
              valueFormatter: (value) => formatter.hydration(value).text,
            ),
    ),
    sectionPadded(_HydrationGoalCard(state: state, formatter: formatter)),
    if (state.drinkBreakdown.isNotEmpty)
      sectionPadded(_HydrationDrinkBreakdownCard(state: state, formatter: formatter)),
    const SectionHeader('Statistics'),
    sectionPadded(_HydrationStatisticsCard(state: state, formatter: formatter)),
    sectionPadded(const HydrationReminderCard()),
    // Kotlin's `MetricDetailSectionId.ENTRIES` — the per-entry beverage history,
    // rendered last. This is the day view's "line info": one row per logged
    // drink, with its name, time, source and volume.
    if (state.entries.isNotEmpty)
      _HydrationEntriesContent(entries: state.entries, formatter: formatter),
  ];
}


class _HydrationGoalCard extends StatelessWidget {
  const _HydrationGoalCard({required this.state, required this.formatter});

  final HydrationState state;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final goal = formatter.hydration(state.dailyGoalLiters);
    final summary = state.summary;
    final progress = state.dailyGoalLiters > 0.0
        ? (summary.averageLiters / state.dailyGoalLiters).clamp(0.0, 1.0)
        : 0.0;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.local_drink_outlined,
                    color: AppColors.hydration, size: 22),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Daily goal', style: theme.textTheme.titleSmall),
                      Text(
                        '${summary.goalMetDays} of ${summary.trackedDays} days met',
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: scheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                Text.rich(
                  TextSpan(
                    children: [
                      TextSpan(
                        text: goal.value,
                        style: theme.textTheme.titleLarge,
                      ),
                      TextSpan(
                        text: ' ${goal.unit}',
                        style: theme.textTheme.bodyMedium
                            ?.copyWith(color: scheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: const BorderRadius.all(Radius.circular(6)),
              child: LinearProgressIndicator(
                value: progress.toDouble(),
                minHeight: 10,
                backgroundColor: scheme.surfaceContainerHighest,
                valueColor: const AlwaysStoppedAnimation<Color>(
                  AppColors.hydration,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HydrationDrinkBreakdownCard extends StatelessWidget {
  const _HydrationDrinkBreakdownCard({
    required this.state,
    required this.formatter,
  });

  final HydrationState state;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final slices = state.drinkBreakdown.take(6).toList();
    var max = 0.0;
    for (final slice in slices) {
      if (slice.liters > max) max = slice.liters;
    }
    if (max <= 0.0) max = 1.0;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Drink breakdown', style: theme.textTheme.titleSmall),
            for (final slice in slices)
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            // A drink with no name of its own (another app's
                            // plain water log) is "Beverage" — never its
                            // originating package name.
                            slice.label ??
                                AppLocalizations.of(context)
                                    .hydrationEntryNutritionOnly,
                            style: theme.textTheme.bodyMedium,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          formatter.hydration(slice.liters).text,
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: scheme.onSurfaceVariant),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    ClipRRect(
                      borderRadius: const BorderRadius.all(Radius.circular(3)),
                      child: LinearProgressIndicator(
                        value: (slice.liters / max).clamp(0.0, 1.0),
                        minHeight: 6,
                        backgroundColor: scheme.surfaceContainerHighest,
                        valueColor: const AlwaysStoppedAnimation<Color>(
                          AppColors.hydration,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _HydrationStatisticsCard extends StatelessWidget {
  const _HydrationStatisticsCard({required this.state, required this.formatter});

  final HydrationState state;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final summary = state.summary;
    final rows = <(String, String)>[
      ('Goal streak', '${formatter.count(summary.currentGoalStreakDays)} days'),
      ('Goals met', '${formatter.count(summary.goalMetDays)} days'),
      (
        'Longest goal streak',
        '${formatter.count(summary.longestGoalStreakDays)} days'
      ),
      ('Success rate', '${formatter.count(summary.goalSuccessRatePercent)}%'),
      ('Daily average', formatter.hydration(summary.averageLiters).text),
      ('Total intake', formatter.hydration(summary.totalLiters).text),
      ('Best day', formatter.hydration(summary.bestDayLiters).text),
    ];
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.insights, color: AppColors.hydration, size: 20),
                const SizedBox(width: 8),
                Text(
                  'Statistics',
                  style: theme.textTheme.labelMedium
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              ],
            ),
            const SizedBox(height: 12),
            for (final row in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(row.$1, style: theme.textTheme.bodyMedium),
                    Text(
                      row.$2,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: scheme.onSurface,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Kotlin `HydrationEntriesContent`: the beverage history, newest first, as a
/// paginated list of rows.
///
/// DEVIATION from Kotlin, which makes each row swipe-to-delete and (for an
/// OpenVitals hydration record) edit-tappable. The Dart screen has no delete or
/// edit path yet — the same Phase 6 gap the "+ add drink" action already routes
/// around — so the rows are read-only. Restoring the *information* is what was
/// missing; the affordances follow with the rest of Phase 6.
class _HydrationEntriesContent extends StatelessWidget {
  const _HydrationEntriesContent({
    required this.entries,
    required this.formatter,
  });

  final List<HydrationEntry> entries;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final sorted = [...entries]
      ..sort((a, b) => b.startTime.compareTo(a.startTime));
    return PaginatedEntryList<HydrationEntry>(
      title: AppLocalizations.of(context).sectionEntries,
      entries: sorted,
      rowBuilder: (context, entry) =>
          _HydrationEntryRow(entry: entry, formatter: formatter),
    );
  }
}

/// Kotlin `HydrationEntryRowContent`: one logged beverage — its name, when it
/// was drunk, where it came from, and how much it hydrated.
///
/// A nutrition-only entry (a drink logged with nutrients but no volume) is
/// titled by its name and reports no hydration impact, exactly as in Kotlin.
class _HydrationEntryRow extends StatelessWidget {
  const _HydrationEntryRow({required this.entry, required this.formatter});

  final HydrationEntry entry;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();

    final isNutritionOnly =
        entry.recordType == HydrationEntryRecordType.nutritionOnly;
    final dateText = DateFormat.yMMMd(locale).format(entry.startTime);
    final startText = DateFormat.jm(locale).format(entry.startTime);
    final name = entry.displayName?.trim();
    final hasName = name != null && name.isNotEmpty;

    // Kotlin titles a hydration row by its date, because its hydration entries
    // never carry a name (it only joins the paired nutrition record for the
    // nutrition-only rows). We do have the name — see `hydration_entry_merge` —
    // and a drink's name is what a beverage history is for, so it leads when we
    // have one and falls back to Kotlin's date otherwise.
    final titleText = hasName
        ? name
        : (isNutritionOnly ? l10n.hydrationEntryNutritionOnly : dateText);
    final subtitleText = isNutritionOnly || hasName
        ? '$dateText • $startText'
        : '$startText - ${DateFormat.jm(locale).format(entry.endTime)}';
    final amountText = isNutritionOnly
        ? l10n.hydrationEntryNoHydration
        : formatter.hydration(entry.liters).text;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titleText, style: theme.textTheme.bodyMedium),
                  Text(
                    subtitleText,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 4),
                  SourceChip(source: entry.source),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Text(
              amountText,
              style: theme.textTheme.titleMedium?.copyWith(
                color:
                    isNutritionOnly ? scheme.secondary : AppColors.hydration,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
