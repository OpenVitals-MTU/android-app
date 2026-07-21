import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../application/heart_vitals_overview_display.dart';
import '../application/heart_vitals_overview_view_model.dart';
import 'heart_vitals_sections.dart';

/// The combined heart & vitals overview (`/heart_vitals`), a port of the Kotlin
/// `HeartVitalsOverviewScreen` + `VitalsOverviewContent`: one range-driven
/// scrolling screen that renders every heart and vitals metric grouped into the
/// three user-reorderable sections Kotlin uses — `VITALS_HEART_SECTION`,
/// `VITALS_CARDIOVASCULAR_SECTION` and `VITALS_RESPIRATORY_SECTION`.
///
/// It renders [HeartVitalsOverviewState.display], which the view-model derived
/// once at load time; the widgets here format, lay out and reorder, and compute
/// nothing.
class HeartVitalsOverviewScreen extends ConsumerStatefulWidget {
  const HeartVitalsOverviewScreen({super.key});

  @override
  ConsumerState<HeartVitalsOverviewScreen> createState() =>
      _HeartVitalsOverviewScreenState();
}

class _HeartVitalsOverviewScreenState
    extends ConsumerState<HeartVitalsOverviewScreen> {
  bool _syncKicked = false;

  Future<void> _syncHistory() async {
    await ref.read(vitalsHistorySyncServiceProvider).syncAll();
    if (!mounted) return;
    ref.read(heartVitalsOverviewProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    // Kick the daily-aggregate history sync only AFTER the first foreground load
    // finishes — never alongside it. The sync fans out seven 730-day reads;
    // running them next to the screen's own budgeted reads makes Health Connect
    // serialize everything and the 6s per-metric budget times out spuriously
    // (the 30s→80s contention the calories screen documents). Sequenced, the
    // screen loads first (live, once), then the cache fills in the background so
    // long-range charts stuck on "Building history…" pick up the fresh days.
    // Guarded to fire once per open.
    ref.listen(heartVitalsOverviewProvider.select((s) => s.isLoading),
        (prev, next) {
      if (prev == true && next == false && !_syncKicked) {
        _syncKicked = true;
        _syncHistory();
      }
    });
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(heartVitalsOverviewProvider);
    final notifier = ref.read(heartVitalsOverviewProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.screenHeartVitals),
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
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readRestingHeartRate,
          HcPermissions.readHrv,
        },
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          // The Kotlin `HeartViewModel` keys the remembered range on
          // `PeriodRangePreferenceKey.HEART`.
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) => [
            _OverviewContent(
              state: state,
              period: period,
              formatter: formatter,
            ),
          ],
        ),
      ),
    );
  }
}

class _OverviewContent extends StatelessWidget {
  const _OverviewContent({
    required this.state,
    required this.period,
    required this.formatter,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final display = state.display;
    if (display == null && state.isLoading) return const SectionLoading();
    // An empty display is what an empty period derives to: every card falls back
    // to its placeholder, every chart stays away.
    final data = display ?? const HeartVitalsOverviewDisplay();

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _sections(context, data, daySelection),
    );
  }

  Widget _sections(
    BuildContext context,
    HeartVitalsOverviewDisplay display,
    ChartDaySelection daySelection,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OrderedMetricDetailSections(
          sections: [
            MetricDetailSection(
              MetricDetailSectionId.vitalsHeartSection,
              HeartVitalsHeartSection(
                state: state,
                period: period,
                formatter: formatter,
                display: display,
                daySelection: daySelection,
              ),
            ),
            MetricDetailSection(
              MetricDetailSectionId.vitalsCardiovascularSection,
              HeartVitalsCardiovascularSection(
                state: state,
                period: period,
                formatter: formatter,
                display: display,
                daySelection: daySelection,
              ),
            ),
            MetricDetailSection(
              MetricDetailSectionId.vitalsRespiratorySection,
              HeartVitalsRespiratorySection(
                state: state,
                period: period,
                formatter: formatter,
                display: display,
                daySelection: daySelection,
              ),
            ),
          ],
        ),
        // Kotlin `HeartVitalsOverviewScreen` renders `dataSourceEducationItem()`
        // as a bare trailing item after the grouped sections (line 155).
        const DataSourceEducationItem(),
      ],
    );
  }
}
