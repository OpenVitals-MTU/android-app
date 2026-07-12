import 'package:flutter/material.dart';

import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';

/// Port of the Kotlin `HeartMetricOrderedSections.kt`: the reorderable section
/// skeleton every heart + vitals metric renders through.
///
/// Visibility rules mirror `renderChartMetricSections`:
/// - the intraday chart only shows on the DAY range;
/// - the period chart shows on other ranges (or always when there is no
///   intraday chart);
/// - selected-day entries only show while a chart day is pinned;
/// - data confidence is hidden for single-day periods;
/// - the context insight only shows when there is no highlight card. Kotlin
///   registers it under `DAILY_GOAL`; here it uses `METRIC_CONTEXT` so the two
///   cannot collide in the section map and stay independently reorderable.
Widget heartChartMetricSections({
  required TimeRange selectedRange,
  required DatePeriod period,
  required LocalDate? selectedDate,
  Widget? intradayChart,
  Widget? periodChart,
  Widget? highlightCard,
  Widget? selectedDayEntries,
  Widget? dataConfidence,
  Widget? contextInsight,
  Widget? statistics,
  Widget? entries,
}) =>
    OrderedMetricDetailSections(
      sections: [
        if (intradayChart != null)
          MetricDetailSection(
            MetricDetailSectionId.intradayChart,
            visible: selectedRange == TimeRange.day,
            intradayChart,
          ),
        if (periodChart != null)
          MetricDetailSection(
            MetricDetailSectionId.periodChart,
            visible: selectedRange != TimeRange.day || intradayChart == null,
            periodChart,
          ),
        if (highlightCard != null)
          MetricDetailSection(MetricDetailSectionId.dailyGoal, highlightCard),
        if (selectedDayEntries != null)
          MetricDetailSection(
            MetricDetailSectionId.selectedDayEntries,
            visible: selectedDate != null,
            selectedDayEntries,
          ),
        if (dataConfidence != null)
          MetricDetailSection(
            MetricDetailSectionId.dataConfidence,
            visible: period.start != period.end,
            dataConfidence,
          ),
        if (contextInsight != null)
          MetricDetailSection(
            MetricDetailSectionId.metricContext,
            visible: highlightCard == null,
            contextInsight,
          ),
        if (statistics != null)
          MetricDetailSection(MetricDetailSectionId.statistics, statistics),
        if (entries != null)
          MetricDetailSection(MetricDetailSectionId.entries, entries),
      ],
    );
