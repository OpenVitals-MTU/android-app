import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/insight_cards.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/paginated_entry_list.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/body_display.dart';
import '../application/body_metric_view_model.dart';
import 'body_overview_sections.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/section_padding.dart';

/// The aggregate `/body` screen, a port of the Kotlin `BodyScreen` +
/// `renderBodyOverviewOrderedContent` (`BodyMetricOrderedSections.kt`): ONE
/// scrolling screen that renders every body-composition metric inline as
/// user-reorderable sections — a statistics grid of all latest values (plus the
/// BMI/FFMI context cards), one trend chart per tracked metric, the pinned
/// day's readings, and the combined entry list with swipe-to-delete and
/// tap-to-edit. No row or tile navigates onward; every `/metric/<body id>`
/// route lands here (Kotlin parity).
class BodyScreen extends ConsumerWidget {
  const BodyScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(bodyMetricProvider);
    final notifier = ref.read(bodyMetricProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.screenBody),
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
        requiredPermissions: {HcPermissions.readWeight},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          // The Kotlin `BodyViewModel` keys the remembered range on
          // `PeriodRangePreferenceKey.BODY` (default MONTH).
          rangePreferenceKey: PeriodRangePreferenceKey.body,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => [
            _BodyOverviewContent(
              state: state,
              period: period,
              formatter: formatter,
              weekPeriodMode: weekMode,
            ),
          ],
        ),
      ),
    );
  }
}

class _BodyOverviewContent extends ConsumerWidget {
  const _BodyOverviewContent({
    required this.state,
    required this.period,
    required this.formatter,
    required this.weekPeriodMode,
  });

  final BodyMetricState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final WeekPeriodMode weekPeriodMode;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final display = state.display;
    if (display == null) {
      if (state.isLoading) return const SectionLoading();
      return _placeholder(l10n);
    }

    // Kotlin `bodyContent`: placeholder when the whole period has no body data.
    if (!display.hasAnyBodyData && !state.isLoading) return _placeholder(l10n);

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) => _sections(
        context,
        ref,
        l10n,
        display,
        daySelection,
      ),
    );
  }

  Widget _sections(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    BodyDisplay display,
    ChartDaySelection daySelection,
  ) {
    final metricsData = bodyMetricData(display, formatter, l10n);
    final trackedMetricsData = trackedBodyMetricData(display, formatter, l10n);
    final summary = display.summary;
    final selectedDay = daySelection.selectedDate;

    return OrderedMetricDetailSections(
      sections: [
        // Kotlin section STATISTICS: the all-metric latest grid + the BMI /
        // FFMI interpretation cards.
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              sectionPadded(SectionHeader(l10n.sectionStatistics)),
              sectionPadded(InsightStatGrid(
                stats: [
                  for (final metric in metricsData)
                    InsightStat(
                      title: metric.title,
                      value: metric.latest?.value ?? l10n.noData,
                      unit: metric.latest?.unit ?? '',
                      icon: metric.icon,
                      accentColor: metric.color,
                    ),
                ],
              )),
              BmiContextCards(
                bmi: summary.bmi,
                ffmi: summary.ffmi,
                adjustedFfmi: summary.adjustedFfmi,
                formatter: formatter,
              ),
            ],
          ),
        ),
        // Kotlin section PERIOD_CHART: one inline trend chart per metric that
        // has values in the period ("tracked").
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: trackedMetricsData.isNotEmpty,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              sectionPadded(SectionHeader(l10n.sectionBodyTrends)),
              for (final metric in trackedMetricsData)
                sectionPadded(_metricChart(metric, daySelection, l10n)),
            ],
          ),
        ),
        // Kotlin section SELECTED_DAY_ENTRIES: readings of the pinned chart day.
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null,
          selectedDay == null
              ? const SizedBox.shrink()
              : Builder(builder: (context) {
                  final locale =
                      Localizations.localeOf(context).toLanguageTag();
                  return PaginatedEntryList<BodyReading>(
                    title: DateFormat.yMMMd(locale).format(DateTime(
                      selectedDay.year,
                      selectedDay.month,
                      selectedDay.day,
                    )),
                    entries: display.readingsByDate[selectedDay] ??
                        const <BodyReading>[],
                    rowBuilder: (context, reading) =>
                        _readingRow(context, ref, reading),
                  );
                }),
        ),
        // Kotlin section ENTRIES: every reading across the metrics, swipe to
        // delete and tap to edit where the entry is an OpenVitals one.
        MetricDetailSection(
          MetricDetailSectionId.entries,
          PaginatedEntryList<BodyReading>(
            title: l10n.sectionEntries,
            entries: display.readingsNewestFirst,
            rowBuilder: (context, reading) => _readingRow(context, ref, reading),
          ),
        ),
      ],
    );
  }

  Widget _metricChart(
    BodyMetricData metric,
    ChartDaySelection daySelection,
    AppLocalizations l10n,
  ) {
    if (state.selectedRange == TimeRange.day) {
      return BodyIntradayMetricChartCard(
        selectedDate: state.selectedDate,
        metricData: metric,
      );
    }
    return MetricBarChart(
      title: metric.title,
      values: metric.values,
      selectedRange: state.selectedRange,
      period: period,
      accentColor: metric.color,
      summaryValue: metric.latest?.text ??
          l10n.summaryEntries('${metric.values.length}'),
      weekPeriodMode: weekPeriodMode,
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => metric.format(value).text,
    );
  }

  Widget _readingRow(
    BuildContext context,
    WidgetRef ref,
    BodyReading reading,
  ) {
    if (!reading.editable) {
      return BodyReadingRow(reading: reading, formatter: formatter);
    }
    final type = reading.editType!;
    final id = reading.editId!;
    return BodyReadingRow(
      reading: reading,
      formatter: formatter,
      onEdit: () => context.push(
        AppRoutes.bodyMeasurementEntryEditLocation(type.storageName, id),
      ),
      onDelete: () => ref
          .read(bodyMetricProvider.notifier)
          .deleteBodyMeasurementEntry(type, id),
    );
  }

  /// Kotlin `renderBodyOverviewPlaceholder`: the empty state is itself an
  /// ordered section (keyed ACTIVITY_SUMMARY) so edit mode still works.
  Widget _placeholder(AppLocalizations l10n) => OrderedMetricDetailSections(
        sections: [
          MetricDetailSection(
            MetricDetailSectionId.activitySummary,
            sectionPadded(MetricCardPlaceholder(
              title: l10n.screenBody,
              icon: Icons.monitor_weight_outlined,
              accentColor: AppColors.weight,
              message: l10n.messageNoReadingsPeriod,
            )),
          ),
        ],
      );
}


