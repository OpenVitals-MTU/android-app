/// Ordered sections of a metric detail screen (user-reorderable).
enum MetricDetailSectionId {
  periodChart,
  intradayChart,
  selectedDayEntries,
  dailyGoal,
  statistics,
  metricContext,
  crossMetricInsights,
  dataConfidence,
  entries,
  activitySummary,
  activityWeekOverview,
  activityKeyMetrics,
  vitalsHeartSection,
  vitalsCardiovascularSection,
  vitalsRespiratorySection,
}

/// The default section order. The stored/persisted identifiers use the original
/// Kotlin `SCREAMING_SNAKE_CASE` enum names for forward/backward compatibility.
const List<MetricDetailSectionId> defaultMetricDetailSectionOrder = [
  MetricDetailSectionId.activitySummary,
  MetricDetailSectionId.activityWeekOverview,
  MetricDetailSectionId.activityKeyMetrics,
  MetricDetailSectionId.periodChart,
  MetricDetailSectionId.intradayChart,
  MetricDetailSectionId.selectedDayEntries,
  MetricDetailSectionId.dailyGoal,
  MetricDetailSectionId.statistics,
  MetricDetailSectionId.metricContext,
  MetricDetailSectionId.crossMetricInsights,
  MetricDetailSectionId.dataConfidence,
  MetricDetailSectionId.entries,
  MetricDetailSectionId.vitalsHeartSection,
  MetricDetailSectionId.vitalsCardiovascularSection,
  MetricDetailSectionId.vitalsRespiratorySection,
];

const Map<MetricDetailSectionId, String> _storageNames = {
  MetricDetailSectionId.periodChart: 'PERIOD_CHART',
  MetricDetailSectionId.intradayChart: 'INTRADAY_CHART',
  MetricDetailSectionId.selectedDayEntries: 'SELECTED_DAY_ENTRIES',
  MetricDetailSectionId.dailyGoal: 'DAILY_GOAL',
  MetricDetailSectionId.statistics: 'STATISTICS',
  MetricDetailSectionId.metricContext: 'METRIC_CONTEXT',
  MetricDetailSectionId.crossMetricInsights: 'CROSS_METRIC_INSIGHTS',
  MetricDetailSectionId.dataConfidence: 'DATA_CONFIDENCE',
  MetricDetailSectionId.entries: 'ENTRIES',
  MetricDetailSectionId.activitySummary: 'ACTIVITY_SUMMARY',
  MetricDetailSectionId.activityWeekOverview: 'ACTIVITY_WEEK_OVERVIEW',
  MetricDetailSectionId.activityKeyMetrics: 'ACTIVITY_KEY_METRICS',
  MetricDetailSectionId.vitalsHeartSection: 'VITALS_HEART_SECTION',
  MetricDetailSectionId.vitalsCardiovascularSection:
      'VITALS_CARDIOVASCULAR_SECTION',
  MetricDetailSectionId.vitalsRespiratorySection: 'VITALS_RESPIRATORY_SECTION',
};

extension MetricDetailSectionIdStorage on MetricDetailSectionId {
  String get storageName => _storageNames[this]!;
}

MetricDetailSectionId? _sectionIdFromStorage(String storedId) {
  for (final entry in _storageNames.entries) {
    if (entry.value == storedId) return entry.key;
  }
  return null;
}

List<MetricDetailSectionId> metricDetailSectionOrderFromStored(
  List<String>? storedIds,
) {
  if (storedIds == null) return defaultMetricDetailSectionOrder;

  final parsedIds = <MetricDetailSectionId>[];
  for (final storedId in storedIds) {
    final section = _sectionIdFromStorage(storedId);
    if (section != null && !parsedIds.contains(section)) {
      parsedIds.add(section);
    }
  }

  if (parsedIds.isEmpty) return defaultMetricDetailSectionOrder;

  final merged = List<MetricDetailSectionId>.from(parsedIds);
  for (final sectionId in defaultMetricDetailSectionOrder) {
    if (!merged.contains(sectionId)) {
      merged.add(sectionId);
    }
  }
  return merged;
}
