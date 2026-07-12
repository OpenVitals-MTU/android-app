import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import 'heart_metric.dart';
import 'heart_metric_content.dart';
import 'heart_metric_view_model.dart';

/// The shared period-detail screen for the ten heart + vitals metrics, ported
/// from the Kotlin `HeartMetricScreen`. Each route-facing screen (`HeartRate
/// Screen`, `BloodPressureScreen`, …) is a thin wrapper that fixes [metric].
/// Wrapped in [HealthConnectGate] with the metric's read permission and driven
/// by [MetricDetailScaffold] (HEART range key). Every metric's content renders
/// through the user-reorderable ordered sections; the app-bar toggle mirrors
/// the Kotlin `onSectionEditStateChanged` affordance.
class HeartMetricScreen extends ConsumerWidget {
  const HeartMetricScreen({super.key, required this.metric});

  final HeartMetric metric;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = heartMetricProvider(metric);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(metric.title),
        actions: [
          // Kotlin hoists this toggle into the host app bar through
          // `onSectionEditStateChanged`; the same affordance, wired locally.
          IconButton(
            onPressed:
                ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? AppLocalizations.of(context).cdFinishMetricSectionEditing
                : AppLocalizations.of(context).cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {metric.readPermission},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          // The Kotlin `HeartViewModel` keys every heart/vitals metric's
          // remembered range on `PeriodRangePreferenceKey.HEART`.
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) => [
            HeartMetricContentView(
              metric: metric,
              state: state,
              formatter: formatter,
              period: period,
              weekPeriodMode: weekMode,
              onDecreaseHighHeartRateThreshold:
                  notifier.decreaseHighHeartRateThreshold,
              onIncreaseHighHeartRateThreshold:
                  notifier.increaseHighHeartRateThreshold,
              onDecreaseLowHeartRateThreshold:
                  notifier.decreaseLowHeartRateThreshold,
              onIncreaseLowHeartRateThreshold:
                  notifier.increaseLowHeartRateThreshold,
              onEditVitalsMeasurement: (type, entryId) => context.push(
                AppRoutes.vitalsMeasurementEntryEditLocation(
                  type.storageName,
                  entryId,
                ),
              ),
              onDeleteVitalsMeasurement: notifier.deleteVitalsMeasurementEntry,
            ),
          ],
        ),
      ),
    );
  }
}

// ── Route-facing per-metric wrappers (Kotlin `HeartRateScreen`, …). ──────────

class HeartRateScreen extends StatelessWidget {
  const HeartRateScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.averageHeartRate);
}

class RestingHeartRateScreen extends StatelessWidget {
  const RestingHeartRateScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.restingHeartRate);
}

class HrvScreen extends StatelessWidget {
  const HrvScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.hrv);
}
