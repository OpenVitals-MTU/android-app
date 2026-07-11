import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../health/health_permissions.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import '../../ui/theme/app_colors.dart';
import 'activities_notifier.dart';
import 'activities_ordered_sections.dart';

/// The aggregate `/activity` screen (also the `/metric/WORKOUT` dispatch target):
/// a port of the Kotlin `ActivitiesScreen` + `renderActivitiesOrderedContent`.
/// ONE scrolling screen that renders the activity summary, key movement metrics,
/// the workout-minutes chart, statistics, the HHS guideline context, the
/// resting-HR cross insight and data confidence as user-reorderable sections,
/// with an activity-type filter and an app-bar edit toggle.
class ActivitiesScreen extends ConsumerWidget {
  const ActivitiesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(activitiesNotifierProvider);
    final notifier = ref.read(activitiesNotifierProvider.notifier);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.sectionActivities),
        actions: [
          // Kotlin hoists this toggle into the host app bar through
          // `onSectionEditStateChanged`; the same affordance, wired locally.
          IconButton(
            onPressed:
                ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? l10n.cdFinishMetricSectionEditing
                : l10n.cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readExercise},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.activities,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => [
            _ActivitiesContent(state: state, period: period),
          ],
        ),
      ),
    );
  }
}

class _ActivitiesContent extends StatelessWidget {
  const _ActivitiesContent({required this.state, required this.period});

  final ActivitiesState state;
  final DatePeriod period;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final hasAnyData = state.workouts.isNotEmpty ||
        state.plannedWorkouts.isNotEmpty ||
        state.overviewDays.any((d) =>
            d.steps > 0 || d.distanceMeters > 0 || d.energyBurnedKcal > 0);
    if (!hasAnyData) {
      if (state.isLoading) {
        return const Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        );
      }
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: MetricCardPlaceholder(
          title: l10n.sectionActivities,
          icon: Icons.directions_run,
          accentColor: AppColors.workout,
          message: l10n.messageNoActivitiesPeriod,
        ),
      );
    }

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) => ActivitiesOrderedSections(
        state: state,
        period: period,
        daySelection: daySelection,
      ),
    );
  }
}
