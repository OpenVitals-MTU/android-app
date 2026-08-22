package tech.mmarca.openvitals.domain.model

/**
 * Metrics that have to be read in one pass, because splitting them would either
 * read the same records twice or leave a derived tile with nothing to derive
 * from.
 *
 * Everything NOT listed here loads alone. That is the point: a tile answers as
 * soon as its own read does, instead of waiting on the slowest metric in a
 * batch it happened to be bundled with.
 */
private val CoupledDashboardMetricGroups: List<Set<DashboardMetric>> = listOf(
    // One weight read, one height read and one body-fat read feed five tiles,
    // and BMI and FFMI are computed from them rather than read. Split, the
    // records would be fetched up to three times over and the derived tiles
    // would come up empty.
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
    // The sleep score folds the night's HRV against its 28-day baseline. Loaded
    // apart from HRV, sleep silently scores itself without that term.
    setOf(DashboardMetric.SLEEP, DashboardMetric.HRV),
    // Both fall out of one readDashboardWeeklyTrainingSignals pass — the
    // fourteen-day heart-rate walk is far too expensive to run twice.
    setOf(DashboardMetric.WEEKLY_CARDIO_LOAD, DashboardMetric.INTENSITY_MINUTES),
)

/**
 * Splits the dashboard's metrics into the passes that load them, keeping
 * [orderedMetrics]' order so the tiles the user is looking at are requested
 * first — Health Connect reads are throttled to a couple at a time, so dispatch
 * order is what decides which tile fills in first.
 *
 * A coupled group is narrowed to the metrics actually asked for: the loader
 * already reads weight for a BMI tile whose own weight tile was removed, so
 * there is nothing to gain by loading a metric the dashboard does not show.
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
