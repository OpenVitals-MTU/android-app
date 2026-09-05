package tech.mmarca.openvitals.domain.model

/**
 * Metrics that must be read in one pass, or records are read twice or a
 * derived tile has nothing to derive from. Everything else loads alone.
 */
private val CoupledDashboardMetricGroups: List<Set<DashboardMetric>> = listOf(
    // Weight, height and body fat feed five tiles; BMI and FFMI are derived.
    setOf(
        DashboardMetric.WEIGHT,
        DashboardMetric.HEIGHT,
        DashboardMetric.BODY_FAT,
        DashboardMetric.BMI,
        DashboardMetric.FFMI,
    ),
    // readDailyMacros answers all four out of a single nutrition read.
    setOf(
        DashboardMetric.PROTEIN,
        DashboardMetric.CARBS,
        DashboardMetric.FAT,
        DashboardMetric.CAFFEINE,
    ),
    // The sleep score folds HRV against its baseline.
    setOf(DashboardMetric.SLEEP, DashboardMetric.HRV),
    // Both fall out of one weekly training pass, which is too expensive to run twice.
    setOf(DashboardMetric.WEEKLY_CARDIO_LOAD, DashboardMetric.INTENSITY_MINUTES),
)

/**
 * Splits the metrics into load passes in [orderedMetrics]' order, so the
 * visible tiles fill first. A coupled group is narrowed to what is asked for.
 */
fun dashboardMetricLoadGroups(orderedMetrics: List<DashboardMetric>): List<Set<DashboardMetric>> {
    val wanted = orderedMetrics.toSet()
    val groups = mutableListOf<Set<DashboardMetric>>()
    val claimed = mutableSetOf<DashboardMetric>()
    orderedMetrics.forEach { metric ->
        if (metric in claimed) return@forEach
        val group = CoupledDashboardMetricGroups
            .firstOrNull { metric in it }
            ?.intersect(wanted)
            ?: setOf(metric)
        claimed += group
        groups += group
    }
    return groups
}
