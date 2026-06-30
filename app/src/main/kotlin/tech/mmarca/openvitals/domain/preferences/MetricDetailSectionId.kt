package tech.mmarca.openvitals.domain.preferences

enum class MetricDetailSectionId {
    PERIOD_CHART,
    INTRADAY_CHART,
    SELECTED_DAY_ENTRIES,
    DAILY_GOAL,
    STATISTICS,
    DATA_CONFIDENCE,
    ENTRIES,
    ACTIVITY_SUMMARY,
    VITALS_HEART_SECTION,
    VITALS_CARDIOVASCULAR_SECTION,
    VITALS_RESPIRATORY_SECTION,
}

val DefaultMetricDetailSectionOrder: List<MetricDetailSectionId> = listOf(
    MetricDetailSectionId.PERIOD_CHART,
    MetricDetailSectionId.INTRADAY_CHART,
    MetricDetailSectionId.SELECTED_DAY_ENTRIES,
    MetricDetailSectionId.DAILY_GOAL,
    MetricDetailSectionId.STATISTICS,
    MetricDetailSectionId.DATA_CONFIDENCE,
    MetricDetailSectionId.ENTRIES,
    MetricDetailSectionId.ACTIVITY_SUMMARY,
    MetricDetailSectionId.VITALS_HEART_SECTION,
    MetricDetailSectionId.VITALS_CARDIOVASCULAR_SECTION,
    MetricDetailSectionId.VITALS_RESPIRATORY_SECTION,
)

fun metricDetailSectionOrderFromStored(storedIds: List<String>?): List<MetricDetailSectionId> {
    if (storedIds == null) return DefaultMetricDetailSectionOrder

    val parsedIds = storedIds.mapNotNull { storedId ->
        runCatching { MetricDetailSectionId.valueOf(storedId) }.getOrNull()
    }.distinct()

    if (parsedIds.isEmpty()) return DefaultMetricDetailSectionOrder

    val merged = parsedIds.toMutableList()
    DefaultMetricDetailSectionOrder.forEach { sectionId ->
        if (sectionId !in merged) {
            merged += sectionId
        }
    }
    return merged
}
