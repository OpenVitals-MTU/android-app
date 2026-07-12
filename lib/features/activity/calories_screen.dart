import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/query/activity_period_data.dart';
import '../../data/source/health/health_permissions.dart';
import '../../state/app_providers.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import 'activity_metric.dart';
import 'calories_notifier.dart';

/// Calories overview pushed over the shell (`/calories`), ported from the Kotlin
/// `CaloriesScreen`. Shows calories burned and active calories over the selected
/// period plus the latest basal metabolic rate.
class CaloriesScreen extends ConsumerWidget {
  const CaloriesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(caloriesNotifierProvider);
    final notifier = ref.read(caloriesNotifierProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Calories')),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readTotalCalories,
          HcPermissions.readActiveCalories,
        },
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.calories,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) => _content(state, formatter, period, weekMode),
        ),
      ),
    );
  }
}

List<Widget> _content(
  CaloriesState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final data = state.data;
  if (data == null) {
    if (state.isLoading) {
      return const [
        Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        ),
      ];
    }
    return const [
      Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCardPlaceholder(
          title: 'Calories',
          icon: Icons.local_fire_department,
          accentColor: Colors.redAccent,
          message: 'No calories data for this period.',
        ),
      ),
    ];
  }

  return [
    ..._metricSection(
      ActivityMetric.caloriesOut,
      data,
      state,
      formatter,
      period,
      weekPeriodMode,
    ),
    ..._metricSection(
      ActivityMetric.activeCalories,
      data,
      state,
      formatter,
      period,
      weekPeriodMode,
    ),
    if (state.latestBmrKcal != null)
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCard(
          title: 'Basal metabolic rate',
          value: formatter.energy(state.latestBmrKcal!).value,
          unit: 'kcal/day',
          icon: Icons.bolt,
          accentColor: Colors.orange,
          subtitle: 'Latest resting energy expenditure',
        ),
      ),
  ];
}

List<Widget> _metricSection(
  ActivityMetric metric,
  ActivityPeriodData data,
  CaloriesState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final values = metric.chartValues(data);
  final hasData = values.any((value) => value.value > 0.0);
  final total = values.fold<double>(0.0, (sum, value) => sum + value.value);
  final hero = metric.format(formatter, total);

  if (!hasData) {
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
        values: values,
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
