import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_skeleton.dart';
import '../../../ui/theme/chart_tokens.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../di/data_providers.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../domain/refresh/data_domain.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import 'activity_metric.dart';
import '../application/calories_display.dart';
import '../application/calories_view_model.dart';

/// Calories overview pushed over the shell (`/calories`), ported from the Kotlin
/// `CaloriesScreen`. Shows calories burned and active calories over the selected
/// period plus the latest basal metabolic rate.
class CaloriesScreen extends ConsumerStatefulWidget {
  const CaloriesScreen({super.key});

  @override
  ConsumerState<CaloriesScreen> createState() => _CaloriesScreenState();
}

class _CaloriesScreenState extends ConsumerState<CaloriesScreen> {
  bool _syncKicked = false;

  /// Drain the Changes API into the daily cache, then re-read from it.
  ///
  /// This is what a refresh MEANS on a cache-backed screen. RefreshMode.force
  /// deliberately does not bypass the daily-aggregate mirror (see [RefreshMode]),
  /// so a pull that only forced the load would re-serve the same SQLite rows and
  /// never see a burn another app recorded. Draining first picks those up for the
  /// cost of one incremental poll, instead of Health Connect's 13-24s
  /// TotalCaloriesBurned year aggregate. The service is single-flight, so this is
  /// free when a drain is already running.
  Future<void> _syncHistory() async {
    await ref.read(caloriesHistorySyncServiceProvider).syncAll();
    if (!mounted) return;
    await ref.read(caloriesProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    // Kick the background cache sync only AFTER the first foreground load
    // finishes — never alongside it. Concurrent reads serialize inside Health
    // Connect, so running the sync's slow history read next to the screen's own
    // read turned a ~30s first open into 80s+. Sequenced, the screen loads
    // first (live, once), then the cache fills in the background, and every
    // later open is a ~365-row SQLite read. Guarded to fire once per open.
    ref.listen(caloriesProvider.select((s) => s.isLoading), (prev, next) {
      if (prev == true && next == false && !_syncKicked) {
        _syncKicked = true;
        _syncHistory();
      }
    });
    final state = ref.watch(caloriesProvider);
    final notifier = ref.read(caloriesProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).screenCalories)),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readTotalCalories,
          HcPermissions.readActiveCalories,
        },
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.calories,
          onRefresh: _syncHistory,
          refreshDomains: const {
            DataDomain.calories,
            DataDomain.activities,
            DataDomain.nutrition,
          },
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) => _content(context, state, formatter, period, weekMode),
        ),
      ),
    );
  }
}

List<Widget> _content(
  BuildContext context,
  CaloriesState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final display = state.display;
  if (display == null) {
    if (state.isLoading) {
      return const [
        ChartSkeleton(shape: ChartSkeletonShape.bars, height: kChartHeightPeriodBar),
      ];
    }
    return [
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCardPlaceholder(
          title: AppLocalizations.of(context).screenCalories,
          icon: Icons.local_fire_department_outlined,
          accentColor: Colors.redAccent,
          message: AppLocalizations.of(context).messageNoCaloriesPeriod,
        ),
      ),
    ];
  }

  return [
    ..._metricSection(
      ActivityMetric.caloriesOut,
      display.caloriesOut,
      state,
      formatter,
      period,
      weekPeriodMode,
    ),
    ..._metricSection(
      ActivityMetric.activeCalories,
      display.activeCalories,
      state,
      formatter,
      period,
      weekPeriodMode,
    ),
    if (state.latestBmrKcal != null)
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCard(
          title: AppLocalizations.of(context).settingsCsvImportMetricBasalMetabolicRate,
          value: formatter.energy(state.latestBmrKcal!).value,
          unit: 'kcal/day',
          icon: Icons.bolt_outlined,
          accentColor: Colors.orange,
          subtitle: AppLocalizations.of(context).caloriesBmrSubtitle,
        ),
      ),
  ];
}

List<Widget> _metricSection(
  ActivityMetric metric,
  CaloriesMetricSeries series,
  CaloriesState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final hero = metric.format(formatter, series.total);

  if (!series.hasData) {
    return [
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCardPlaceholder(
          title: metric.title,
          icon: metric.icon,
          accentColor: metric.accentColor,
          message: metric.emptyMessage,
        ),
      ),
    ];
  }

  return [
    Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: MetricCard(
        title: metric.title,
        value: hero.value,
        unit: hero.unit,
        icon: metric.icon,
        accentColor: metric.accentColor,
      ),
    ),
    Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: MetricBarChart(
        title: metric.title,
        values: series.values,
        selectedRange: state.selectedRange,
        period: period,
        accentColor: metric.accentColor,
        summaryValue: hero.text,
        weekPeriodMode: weekPeriodMode,
        valueFormatter: (value) => metric.formatChartValue(formatter, value),
      ),
    ),
  ];
}
